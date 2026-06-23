# AI 도서 검색 및 다중 에이전트 시스템 최종 통합 설계서

본 문서는 **도서 검색(Keyword, Vector, Hybrid, RAG, AUTO), RabbitMQ 및 SSE 기반 비동기 리뷰 요약 에이전트, ThreadLocal을 완전히 배제한 토큰 절약형 추천 에이전트, 도서관 정보 에이전트 및 챗 에이전트** 등 전체 아키텍처를 총망라한 최종 상세 설계서입니다.

---

## 1. 아키텍처 및 시스템 흐름도 (System Architecture)

### 1.1 전체 아키텍처 개요
```
1) 실시간 서비스 요청 흐름 (동기 / 챗 에이전트 및 검색)
[사용자 요청 (searchType 포함)]
       │
       ├─> keyword ──> KeywordSearchStrategy 실행
       ├─> vector ──> VectorSearchStrategy 실행
       ├─> hybrid ──> HybridSearchStrategy 실행
       ├─> rag ───> RagSearchStrategy 실행
       │
       └─> auto ───> [BookSearchAgent (도서 검색 에이전트)]
                          │
                          ▼ (사용자 질문 분석: Keyword vs RAG 판별)
                    ┌─────┴────────────────┐
                    ▼ (Keyword 판별)       ▼ (RAG 판별)
             [Keyword Tool 실행]     [RAG Tool 실행]
                    │                      │
                    └───────────┬──────────┘
                                ▼
                        [최종 검색 결과 반환]

2) 비동기 리뷰 요약 이벤트 흐름 (비동기 / RabbitMQ & SSE)
[도서 상세 페이지 진입]
       │ (리뷰 요약 캐시 Miss)
       ▼
[ReviewStatus: PROCESSING 상태 즉시 리턴] ──> Client: SSE 구독 연결 (/api/reviews/connect/{bookId})
       │
       └─> [ReviewEventPublisher] ──> [RabbitMQ (book.review.queue)] 
                                            │
                                            ▼
                                      [ReviewEventConsumer] ──> [ReviewAgent 호출]
                                                                     │
                                                                     ▼ (AI Map-Reduce 요약)
                                                                [요약 완료 & Redis 캐싱 (1일)]
                                                                     │
                                                                     ▼ (SSE 푸시)
                                                                [ReviewSseSender] ──> [Client 화면 업데이트]
```

---

## 2. 전체 패키지 및 클래스 명세 (Full Class Specifications)

```
com.nhnacademy.library
 ├─ core.book
 │   ├─ domain
 │   │   └─ Book (Entity - 도서 메타데이터 및 pgvector 임베딩)
 │   ├─ dto
 │   │   ├─ BookSearchRequest (Record - 검색 조건 필드)
 │   │   ├─ BookSearchResponse (Record - 최종 도서 데이터 응답 DTO)
 │   │   ├─ RecommendContext (Record - 추천 에이전트 데이터 교환용)
 │   │   └─ BookRecommendReason (Record - LLM 구조체 응답 파싱용)
 │   ├─ repository
 │   │   ├─ BookRepository (Spring Data JPA)
 │   │   └─ search
 │   │       ├─ KeywordBookSearchRepository (QueryDSL 동적 쿼리)
 │   │       └─ VectorBookSearchRepository (pgvector 코사인 유사도 검색)
 │   └─ service
 │       ├─ embedding
 │       │   └─ BookEmbeddingService (도서 소개글의 벡터 임베딩 추출)
 │       ├─ search
 │       │   ├─ RrfFusionService (RRF 기반 순위 병합 연산)
 │       │   └─ strategy
 │       │       ├─ SearchStrategy (검색 전략 인터페이스)
 │       │       ├─ KeywordSearchStrategy (키워드 검색 전략)
 │       │       ├─ VectorSearchStrategy (벡터 검색 전략)
 │       │       ├─ HybridSearchStrategy (RRF 하이브리드 검색 전략)
 │       │       └─ RagSearchStrategy (RAG 추천 후보 도서 선별 전략)
 │       └─ agent
 │           ├─ ChatAgent (챗 에이전트 - 사용자 메시지 분기 및 병렬 툴 제어)
 │           ├─ BookSearchAgent (도서 검색 에이전트 - AUTO 모드 분석 및 툴 호출)
 │           ├─ BookRecommendationAgent (도서 추천 에이전트 - 추천 사유 집필)
 │           ├─ LibraryInfoAgent (도서관 정보 에이전트 - 외부 API 코디네이터)
 │           └─ AgentTools (에이전트가 실행할 기능들을 담은 공용 @Tool 클래스)
 ├─ core.review
 │   ├─ domain
 │   │   ├─ Review (Entity - 사용자 리뷰 데이터)
 │   │   └─ ReviewStatus (Enum - PROCESSING, DONE, ERROR)
 │   ├─ dto
 │   │   └─ ReviewSummaryResponse (Record - 리뷰 요약 데이터 송신 DTO)
 │   ├─ repository
 │   │   └─ ReviewRepository (Spring Data JPA)
 │   ├─ controller
 │   │   └─ ReviewSseController (SSE 커넥션 생성 웹 컨트롤러)
 │   ├─ event
 │   │   ├─ ReviewEventPublisher (RabbitMQ 이벤트 메일박스 발행자)
 │   │   └─ ReviewEventConsumer (RabbitMQ 수신 및 에이전트 핸들러)
 │   └─ service
 │       ├─ ReviewSseSender (SSE 커넥션 매니저 및 실시간 데이터 송신기)
 │       └─ ReviewAgent (리뷰 에이전트 - 크롤링 및 Map-Reduce 요약)
 └─ core.config
     ├─ QueryDslConfig (QueryDSL JPAQueryFactory 설정)
     ├─ RedisConfig (Redis 커넥션, CacheManager, RedisVectorStore 설정)
     └─ RabbitMqConfig (RabbitMQ Exchange, Queue, Binding 설정)
```

---

## 3. 핵심 모듈별 상세 설계 (Detailed Module Design)

### 3.1 검색 라우팅 및 전략 (Search Routing & Strategies)

#### ① 요청 `searchType` 분기
- 클라이언트 API의 요청 객체(`BookSearchRequest`)에 포함되어 들어오는 `searchType` 값을 기준으로 서비스 컨트롤러 또는 검색 코디네이터 레이어에서 직접 실행할 전략을 선택합니다.
  - `keyword` ➡️ `KeywordSearchStrategy` 실행
  - `vector` ➡️ `VectorSearchStrategy` 실행
  - `hybrid` ➡️ `HybridSearchStrategy` 실행
  - `rag` ➡️ `RagSearchStrategy` 실행
  - `auto` ➡️ `BookSearchAgent` 호출

#### ② [Agent] `com.nhnacademy.library.core.book.service.agent.BookSearchAgent` (AUTO 모드 전담)
- **역할**: 사용자가 `searchType`을 `auto`로 요청한 경우, 입력 프롬프트를 분석하여 **단순 키워드 검색(Keyword)**을 쓸지 **RAG 추천 설명(RAG)**을 쓸지 LLM의 판단에 따라 결정하고 해당 도구를 호출합니다.
- **판단 규칙**:
  - 사용자 질문에 특정 책 제목, 저자, 출판사 등이 명확히 명시된 경우 ➡️ `KEYWORD` 판별 및 툴 실행
  - 도서들을 서로 비교 분석해 달라고 하거나 구체적인 상황을 나열해 추천 사유를 원하는 경우 ➡️ `RAG` 판별 및 툴 실행
- **핵심 구조**:
  - LLM 호출을 통해 `KEYWORD` 또는 `RAG` 문자열 결과 획득.
  - 획득한 결과에 따라 `@Tool`로 등록된 `KeywordSearchStrategy` 툴 또는 `RagSearchStrategy` 툴을 실행하여 검색을 완료하고 리턴합니다.

#### ③ [Service] `com.nhnacademy.library.core.book.service.search.RrfFusionService`
- **역할**: 상이한 검색 모델에서 반환한 결과 리스트를 순위(Rank) 기반으로 융합(Reciprocal Rank Fusion).
- **RRF 직접 계산 공식 및 구현**:
  $$RRF\_Score(d) = \sum_{m \in M} \frac{1.0}{K + r_m(d)}$$
  - 여기서 $M$은 융합할 검색 시스템 군(Keyword 검색 결과군, Vector 검색 결과군), $r_m(d)$는 각 검색 결과군 내에서의 순위(1-indexed), $K$는 랭킹 완화 상수(기본값: 60).
  - **동작**: 키워드 결과와 벡터 결과를 받아 각각의 `id`를 키로 맵에 담으면서 가중치 점수를 누적 연산하고, 최종적으로 점수가 높은 순으로 도서 목록을 정렬해 리턴합니다.

---

### 3.2 도서 추천 에이전트 (BookRecommendationAgent)

#### ① [Record] `com.nhnacademy.library.core.book.dto.RecommendContext`
- **필드**:
  - `String userQuery`: 사용자의 원래 질문
  - `List<Long> bookIds`: 검색 결과 얻은 상위 후보 도서 ID 목록
  - `List<BookSearchResponse> candidateBooks`: 검색 결과 얻은 도서 메타데이터 원본 목록
- **도입 목적**: 스레드 로컬 안전성(Thread-safety) 확보 및 코루틴/가상 스레드 환경에서의 메모리 누수 방지.

#### ② [Record] `com.nhnacademy.library.core.book.dto.BookRecommendReason`
- **필드**:
  - `Long book_id`: 도서 ID
  - `String reason`: LLM이 집필한 해당 도서 추천 사유
- **역할**: LLM 응답을 받아오기 위한 구조화된 템플릿 DTO.

#### ③ [Agent] `BookRecommendationAgent`
- **역할**: 도서 메타데이터 전문을 LLM에 보내는 대신, **식별 정보(`bookIds`)만 주입하여 추천 사유만을 작성**하게 만듦으로써 입/출력 토큰을 대폭 절감.
- **핵심 로직**:
  1. `RecommendContext.bookIds()`를 시스템 프롬프트의 대상 리스트로 전달.
  2. `ChatClient` 호출 시 출력 포맷을 `List<BookRecommendReason>` 형태로 제약하여 구조화된 JSON 데이터로 획득.
  3. 획득한 사유 목록을 `Map<Long, String>` 형태로 변환하여, 기존 메모리 상의 `candidateBooks` 데이터와 `book_id` 기준으로 조립(Join)하여 최종 완성된 도서 정보를 사용자에게 리턴.

---

### 3.3 리뷰 에이전트 (ReviewAgent) - 비동기 수집 & SSE 푸시

#### ① [Enum] `com.nhnacademy.library.core.review.domain.ReviewStatus`
- **종류**: `PROCESSING` (작업 진행 중), `DONE` (완료), `ERROR` (실패)

#### ② [Record] `com.nhnacademy.library.core.review.dto.ReviewSummaryResponse`
- **필드**:
  - `Long bookId`: 도서 식별 번호
  - `ReviewStatus status`: 현재 상태
  - `String summaryContent`: AI Map-Reduce 기법으로 생성한 최종 리뷰 요약본
  - `String errorMessage`: 실패 시 상세 원인 문구

#### ③ [Controller] `com.nhnacademy.library.core.review.controller.ReviewSseController`
- **역할**: 비동기 리뷰 수집 작업을 대기하는 클라이언트들이 SSE 세션을 연결하기 위한 엔드포인트
- **동작**: `GET /api/reviews/connect/{bookId}` 호출 시 `SseEmitter`를 생성해 `ReviewSseSender`에 보관 및 리턴.

#### ④ [Service] `com.nhnacademy.library.core.review.service.ReviewSseSender`
- **역할**: `bookId` 단위로 대기 중인 `SseEmitter` 세션 관리 및 푸시 메시지 발송.
- **필드**: `private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();`
- **동작**: Consumer로부터 요약 완료를 전달받으면 해당 `bookId` 하위의 모든 세션에 데이터를 전송한 후 세션을 종료(`complete()`) 처리합니다.

#### ⑤ [Event] `ReviewEventConsumer` (RabbitMQ 수신부)
- **동작**:
  1. 큐(`book.review.queue`)로부터 수신된 `bookId`를 매개변수로 `ReviewAgent`의 요약 로직 기동.
  2. 작업 중 에러 발생 시 `ReviewSummaryResponse` 상태를 `ERROR`로 세팅하여 SSE 발송.
  3. 요약 성공 시 **Redis 캐시에 저장 (Key: `review:summary:{bookId}`, Value: DONE 상태의 JSON 응답 데이터, TTL: 1일)**.
  4. 대기 중인 사용자 세션에 `ReviewSseSender`를 통해 완료된 요약본을 실시간으로 푸시 전송.

#### ⑥ [Agent] `ReviewAgent` (Map-Reduce 요약기)
- **역할**: 도서의 모든 리뷰 데이터를 5~10개 단위의 작은 묶음(Chunk)으로 분할하여 1차 요약(Map 단계)을 진행한 후, 각 요약문들을 통합 정제하여 최종 1줄/1단락 요약(Reduce 단계)을 생성합니다.

---

### 3.4 챗 에이전트 (ChatAgent) - 병렬 툴 제어

#### ① [Component] `com.nhnacademy.library.core.book.service.agent.AgentTools`
- **역할**: LLM이 불필요하게 툴을 호출하여 API 비용 및 응답 성능을 해치지 않도록 `@Tool`의 설명(Description)을 매우 구체적이고 제약적으로 작성하여 관리.
- **툴 설명 구성안**:
  - `searchBooks`: *"사용자가 특정 주제나 카테고리의 책 리스트 검색만을 단독으로 원할 때만 호출하세요. 리뷰 요약이나 도서관 소장 여부는 포함되지 않습니다."*
  - `getBookReviewSummary`: *"사용자가 특정 도서의 구체적인 ID를 바탕으로 대중의 평가나 리뷰 평점 요약을 요청할 때만 호출하세요."*
  - `getLibraryInfo`: *"사용자가 특정 도서의 오프라인 도서관 소장 현황이나 대출 가능 여부를 구체적으로 물어볼 때만 호출하세요."*

#### ② [Agent] `ChatAgent` (병렬 툴 호출 연동)
- **역할**: 사용자가 복합 의도(예: *"이 책 리뷰가 어떤지 알려주고 근처 도서관에도 있는지 검색해줘"*)를 요구했을 때, LLM이 반환하는 복수 툴 호출 지시를 자바 스레드에서 병렬 처리하도록 제어.
- **병렬 구현**:
  - `CompletableFuture.supplyAsync()`를 활용해 `ReviewAgent`의 요약 조회와 `LibraryInfoAgent`의 외부 도서관 OpenAPI 호출을 동시 실행함으로써 다중 호출의 지연시간을 가장 오래 걸리는 단일 작업 시간 수준으로 제어합니다.

---

### 3.5 Redis 다중 레이어 캐싱 (Redis Caching Strategy)

#### 1차 캐시: 일반 캐싱 (String Key-Value)
- **적용 기술**: Spring Cache 추상화 (`@Cacheable`) + Redis Cache Manager (`RedisCacheManager`)
- **역할**: 동일 문자열 질문에 대한 추천 및 검색 결과를 JSON 형태로 변환해 Redis에 저장하고, 다음 요청 시 1ms 내에 반환합니다.
- **기본 설정**:
  - TTL: 30분
  - Serialization: `GenericJackson2JsonRedisSerializer`

#### 2차 캐시: 시맨틱 캐싱 (Redis Stack VSS)
- **적용 기술**: Redis Stack (RediSearch 모듈) + Spring AI `RedisVectorStore`
- **역할**: 문자열이 완전히 똑같지 않더라도 **의미가 극도로 유사한 질문** (예: "자바 초보용 책 추천해줘" ↔ "자바 공부 시작하기 좋은 서적 알려줘")이 인입된 경우, 코사인 유사도가 0.98 이상이면 기존 캐싱된 추천 텍스트를 즉시 제공하여 AI 임베딩 모델 및 LLM 호출 비용을 대폭 절감합니다.
- **동작**: 질문 벡터 임베딩을 구한 뒤 `RedisVectorStore.similaritySearch()`를 실행해 매칭을 시도합니다.

---

## 4. 단계별 구현 및 개선 마일스톤

1. **[Milestone 1] RabbitMQ & SSE 기반 비동기 리뷰 시스템 구현**
   - `ReviewStatus` enum 및 `ReviewSummaryResponse` DTO 작성.
   - `ReviewSseController` 및 `ReviewSseSender` 작성하여 SSE 구독 연결 기능 제공.
   - RabbitMQ Config, Queue, Exchange 세팅 및 `ReviewEventPublisher`/`Consumer` 연동.
   - 비동기 처리 완료 후 Redis 캐시(TTL 1일) 적재 및 SSE 전송 테스트 완료.
2. **[Milestone 2] 추천 에이전트 리팩토링 (ThreadLocal 제거)**
   - `BookSearchContextHolder` 등 ThreadLocal과 관련된 모든 리포지토리/서비스 영역 소스코드 제거.
   - `RecommendContext` 및 `BookRecommendReason` DTO 작성.
   - `BookRecommendationAgent`에서 LLM의 프롬프트에 `book_id` 목록만 담아 Structured Output(JSON Array)으로 리턴받는 쿼리 가동.
   - 리턴된 추천 사유 목록과 검색된 책 정보를 자바 코드 단에서 Join하여 합치는 로직 작성 및 통합 테스트 성공.
3. **[Milestone 3] 챗 에이전트 툴 설명 최적화 및 복합 의도 병렬 호출 구현**
   - `AgentTools` 클래스 내 각 `@Tool` 설명 어노테이션 속성을 명확하게 보강하여 LLM 툴 호출 안정성 확보.
   - 다중 툴이 호출되는 경우 `CompletableFuture` 기반의 비동기 병렬 태스크 가동 로직 추가 및 응답 지연 단축 시간 검증.
