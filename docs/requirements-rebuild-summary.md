# AI 도서 검색/추천 서비스 요구사항 재정리

## 1. 작성 기준

이 문서는 기존 구현 코드를 기준으로 하지 않고, `docs` 하위 문서의 학습 흐름과 요구사항을 기준으로 새로 정리한 협업용 요구사항 문서다.

읽은 문서 범위:

- `docs/index.md`
- `docs/step-1/*`
- `docs/step-2/*`
- `docs/step-3/*`
- `docs/step-4/*`
- `docs/step-5/*`
- `docs/step-6/*`
- `docs/step-7/*`
- `docs/step-8/*`
- `docs/REFACTORING_NOTES.md`
- `docs/TEMPLATE.md`

핵심 결론:

> 이 프로젝트는 단순 도서 검색 서비스가 아니라, 공공 도서 데이터를 기반으로 키워드 검색에서 벡터 검색, RAG, 피드백, 개인화, Agent 도구 호출까지 확장하는 AI 도서관 서비스다.

---

## 2. 전체 목표

사용자가 자연어로 원하는 책을 찾고, AI가 검색 결과를 근거로 추천 이유를 설명하며, 사용자 피드백을 통해 점점 더 개인화된 추천을 제공하는 시스템을 만든다.

최종 지향점:

```text
사용자 질문
  -> 키워드/벡터/하이브리드 검색
  -> RAG 기반 추천 설명
  -> 피드백 수집
  -> 개인화 재정렬
  -> 필요 시 외부 도서관 API / 대출 가능 여부 조회
```

---

## 3. 기능 요구사항

### 3.1 도서 데이터 적재

공공 도서 CSV 데이터를 DB에 적재해야 한다.

필수 요구사항:

- CSV 파일을 읽어 도서 원천 데이터를 파싱한다.
- 대량 데이터를 배치 단위로 저장한다.
- 도서 엔티티와 CSV DTO를 분리한다.
- 중복 저장을 방지할 기준을 둔다. ISBN이 있으면 ISBN을 우선 후보로 삼는다.
- 파싱 실패, 필수값 누락, 중복 데이터에 대한 처리 정책을 정한다.

주요 도서 필드:

- 도서 ID
- ISBN / ISBN13
- 제목
- 저자
- 출판사
- 출판일
- 표지 이미지 URL
- 설명 / 책 소개
- 분류 / KDC
- 가격 또는 부가 메타데이터

권장 설계:

- 초기 MVP에서는 이벤트 기반 아키텍처를 강제하지 않아도 된다.
- 단, CSV 파싱, 변환, 저장 책임은 분리한다.
- 나중에 임베딩 생성이나 외부 API 보강을 붙일 수 있도록 적재 완료 지점을 명확히 둔다.

---

### 3.2 기본 검색

도서 데이터에 대해 전통적인 키워드 검색을 제공해야 한다.

필수 요구사항:

- 제목 검색
- 저자 검색
- 출판사 검색
- ISBN 정확 검색
- 설명/소개글 키워드 검색
- 페이지네이션
- 검색 결과 DTO 제공
- 상세 조회 API 제공

검색 방식:

- MVP: `LIKE` 또는 DB 기본 검색
- 개선: PostgreSQL Full Text Search 또는 인덱스 적용

주의사항:

- 키워드 검색은 고유명사, 정확한 제목, ISBN 검색에 강하다.
- 오타, 동의어, 추상적인 의도 검색에는 약하다.
- 이 한계를 체감하는 것이 Step 2 벡터 검색의 출발점이다.

---

### 3.3 벡터 검색

사용자의 자연어 질의와 도서 내용을 벡터로 변환하여 의미 기반 검색을 제공해야 한다.

필수 요구사항:

- PostgreSQL에 `pgvector` 확장을 적용한다.
- 도서 테이블에 `embedding vector(1024)` 컬럼을 둔다.
- BGE-M3 기준 1024차원 임베딩을 사용한다.
- 도서 제목, 저자, 설명 등 검색에 중요한 필드를 합쳐 임베딩 텍스트를 만든다.
- HTML 태그, 엔티티, 특수문자, 중복 공백 등을 전처리한다.
- 임베딩이 없는 도서를 배치로 찾아 생성한다.
- 사용자 검색어도 같은 모델로 임베딩한다.
- pgvector 연산자로 유사도 기준 정렬 검색을 수행한다.

검색 예시:

```text
"컴퓨터가 어떻게 작동하는지 알고 싶어"
  -> 컴퓨터 구조, 컴퓨터 아키텍처, 시스템 프로그래밍 관련 도서 검색

"우울할 때 읽기 좋은 책"
  -> 감정, 위로, 에세이, 문학 관련 도서 검색
```

주의사항:

- 임베딩 품질은 전처리와 입력 텍스트 구성에 크게 좌우된다.
- 벡터 검색은 추상 질의에 강하지만 정확한 고유명사 검색에는 약할 수 있다.
- 임베딩 생성 API 장애, 비용, 속도 제한을 고려해야 한다.

---

### 3.4 하이브리드 검색

키워드 검색과 벡터 검색을 결합해야 한다.

필수 요구사항:

- 동일 질의에 대해 키워드 검색과 벡터 검색을 모두 수행한다.
- 두 결과를 RRF(Reciprocal Rank Fusion)로 병합한다.
- 도서 ID 기준으로 중복을 제거한다.
- 최종 결과에 RRF 점수와 벡터 유사도를 포함한다.
- 검색 결과는 페이지네이션 가능해야 한다.

RRF 핵심:

```text
RRF score = 1 / (k + rank)
일반적으로 k = 60
```

하이브리드 검색이 필요한 이유:

- 키워드 검색은 정확한 제목, 저자, ISBN에 강하다.
- 벡터 검색은 의미, 동의어, 자연어 의도에 강하다.
- 실제 사용자는 두 유형을 섞어서 질문한다.

MVP 기준:

- 검색 후보군은 각 검색 방식에서 50~100개 정도 가져온다.
- RRF 병합 후 상위 결과를 반환한다.
- 병렬 처리는 성능 개선 단계로 미룰 수 있다.

---

### 3.5 RAG 기반 AI 추천 설명

검색 결과를 LLM에게 context로 제공하고, AI가 추천 이유를 생성해야 한다.

필수 요구사항:

- 사용자 질문을 받는다.
- 하이브리드 검색으로 관련 도서를 찾는다.
- 상위 K개 도서를 context로 변환한다.
- LLM 프롬프트에 역할, 질문, 참고 도서, 답변 규칙을 포함한다.
- LLM은 반드시 검색 결과에 있는 도서만 추천해야 한다.
- 추천 도서별 이유를 생성한다.
- LLM 호출 실패 시 fallback 응답을 제공한다.

프롬프트 규칙:

- AI 역할: 도서관 사서 또는 도서 추천 전문가
- 참고 도서 외 책 추천 금지
- 추천 이유는 도서 제목, 저자, 설명, 리뷰/평점이 있으면 이를 근거로 작성
- 최대 추천 개수 제한
- 출력 형식은 MVP에서는 자연어, 확장 시 JSON DTO를 고려

주의사항:

- RAG 품질은 LLM보다 검색 결과와 context 품질에 더 크게 좌우된다.
- 너무 많은 도서를 넣으면 비용 증가와 노이즈가 발생한다.
- 너무 적게 넣으면 정답 누락이 생긴다.

---

### 3.6 성능 최적화

AI 호출 비용과 응답 속도를 관리해야 한다.

필수 요구사항:

- Retrieval K와 Rerank K를 분리한다.
- 검색 후보군은 넉넉히 가져오되 LLM에는 상위 5~10권만 전달한다.
- RRF 점수 임계값으로 노이즈 결과를 제거한다.
- 도서 설명은 context에 넣기 전 길이를 제한한다.
- 반복 질의에 대해 캐싱 전략을 둔다.

권장 기준:

```text
Retrieval K: 50~100
Rerank K: 5~10
Context: 도서당 설명 200~300자 제한
```

Semantic cache 요구사항:

- 질문 원문과 질문 embedding을 저장한다.
- 새 질문이 들어오면 기존 캐시 질문 embedding과 코사인 유사도를 비교한다.
- 유사도 임계값 이상이면 캐시된 결과를 사용한다.
- TTL을 둔다.
- 자주 검색되는 질문은 warm-up 대상이 될 수 있다.

---

### 3.7 리뷰와 리뷰 요약

도서 리뷰를 저장하고, AI가 요약한 리뷰 정보를 추천에 활용한다.

필수 요구사항:

- 도서별 리뷰를 저장한다.
- 리뷰 평점, 리뷰 개수, 요약 정보를 관리한다.
- 리뷰가 추가되면 요약 갱신 대상이 된다.
- 리뷰 요약은 RAG context에 포함될 수 있다.

확장 요구사항:

- 리뷰 요약은 비동기 처리한다.
- RabbitMQ 같은 메시지 큐를 사용할 수 있다.
- 누적 요약(Incremental Summary)을 사용해 비용을 줄인다.
- 일정 조건에서 전체 재요약(Full Rebuild)을 수행한다.

MVP 판단:

- 리뷰 요약은 검색/RAG MVP 이후 기능으로 둔다.
- 단, 도서 추천 품질을 높이는 중요한 확장 요구다.

---

### 3.8 Telegram Bot UI

Telegram Bot을 통해 자연어 검색 UI와 피드백 수집 UI를 제공한다.

필수 요구사항:

- 사용자가 Bot에 자연어 질의를 보낸다.
- Bot은 검색 또는 RAG 결과를 메시지로 응답한다.
- 검색 결과에는 피드백 버튼을 붙인다.
- Inline Keyboard callback을 처리한다.

피드백 callback 형식:

```text
fb:{bookId}:{type}
예: fb:12345:GOOD
```

주의사항:

- Telegram `callback_data`는 64 bytes 제한이 있다.
- 긴 검색어는 callback에 직접 넣지 말고 서버 메모리나 DB에 매핑한다.
- Callback Query는 빠르게 응답해야 한다.

---

### 3.9 사용자 피드백

검색 결과에 대한 사용자 만족도를 저장해야 한다.

필수 요구사항:

- 사용자 식별자: Telegram `chatId`
- 검색어
- 도서 ID
- 피드백 타입: `GOOD`, `BAD`
- 생성 시각
- 중복 피드백 방지 정책
- 관리자용 통계 API

권장 인덱스:

- `chat_id`
- `created_at`
- `(query, book_id)`
- `book_id`

피드백 활용:

- 검색 품질 모니터링
- 도서별 만족도 계산
- 개인화 추천의 학습 데이터
- 관리자 통계

---

### 3.10 개인화 추천

사용자 피드백을 기반으로 검색 결과를 재정렬해야 한다.

필수 요구사항:

- 사용자의 `GOOD` 피드백 도서 ID를 조회한다.
- 해당 도서들의 embedding을 가져온다.
- embedding 평균으로 사용자 선호 벡터를 만든다.
- 검색 결과 각 도서와 사용자 선호 벡터의 코사인 유사도를 계산한다.
- 기존 RRF 점수와 개인화 점수를 조합한다.

점수 계산 예시:

```text
finalScore = rrfScore + (preferenceSimilarity * 0.3)
```

콜드 스타트 정책:

- GOOD 피드백이 최소 3개 미만이면 개인화를 적용하지 않는다.
- 신규 사용자는 일반 하이브리드 검색 결과를 제공한다.
- 전체 인기 도서나 최근 인기 도서를 fallback으로 사용할 수 있다.

성능 요구:

- 사용자 선호 벡터는 캐싱한다.
- 피드백이 추가되면 해당 사용자의 선호 벡터 캐시를 무효화한다.
- 피드백이 많아지면 비동기 계산 또는 배치 계산을 고려한다.

제약:

- 로그인 시스템이 없다면 Telegram `chatId` 기반 단기 개인화로 한정한다.
- 개인정보 보호를 위해 chatId 해싱 또는 최소 보관 정책을 고려한다.

---

### 3.11 외부 도서관 API 연동

도서관정보나루 API를 통해 내부 DB에 없는 정보 또는 대출 가능 정보를 조회한다.

주요 API 요구:

- `srchBooks`: 도서 검색
- `libSrch`: 도서관 검색
- `libSrchByBook`: 특정 도서를 소장한 도서관 조회
- `bookExist`: 도서관별 소장 및 대출 가능 여부 확인
- `srchDtlList`: 도서 상세 조회
- `loanItemSrch`: 인기 대출 도서 조회
- `hotTrend`: 대출 급상승 도서 조회
- `recommandList`: 추천 도서 조회

필수 고려사항:

- API 인증키 관리
- 요청 파라미터 검증
- 지역 코드는 한글이 아니라 API 코드로 전달
- 응답 지연에 대한 timeout
- API 장애 시 fallback
- 외부 API 결과 캐싱
- 응답 파싱 책임 분리

설계 권장:

- 거대한 단일 API Client를 만들지 않는다.
- 도메인별 Client로 분리한다.
  - BookSearchClient
  - LibrarySearchClient
  - LoanInfoClient
  - BookDetailClient
  - PopularTrendClient
  - RecommendationClient
  - BookExistClient

---

### 3.12 LLM Agent / Function Calling

LLM이 사용자의 질문을 해석하고 필요한 기능을 도구처럼 호출해야 한다.

필수 시나리오:

```text
사용자: "자바의 정석 책 정보랑 리뷰, 빌릴 수 있는 도서관 알려줘"

Agent:
  1. 도서 검색 Function 호출
  2. 리뷰 조회 Function 호출
  3. 대출 가능 여부 Function 호출
  4. 결과를 종합해 한국어 응답 생성
```

주요 Function:

- 도서 검색
- 내부 DB 검색
- 리뷰 조회
- 도서 상세 조회
- 도서관 검색
- 도서 소장 여부 확인
- 대출 가능 도서관 조회
- 인기 도서 조회
- 대출 급상승 도서 조회
- 연관 추천 도서 조회

MVP 판단:

- Function Calling은 검색/RAG/피드백/개인화가 안정화된 후 확장한다.
- 4인 팀 기준으로는 별도 담당자가 외부 API와 함께 맡는 것이 좋다.

---

### 3.13 n8n 자동화

n8n 연동은 필수 MVP가 아니라 운영 자동화 보너스 요구사항이다.

가능한 기능:

- Spring AI API를 HTTP Request 노드로 호출
- Webhook으로 외부 이벤트 수신
- 검색 결과 Slack 알림
- 리뷰 요약 자동화
- 신간 도서 수집 자동화
- 검색 로그 저장
- 워크플로우 실행 내역 모니터링

MVP 판단:

- 1차 구현 범위에서는 제외한다.
- 발표나 고급 확장 기능으로 남긴다.

---

## 4. 비기능 요구사항

### 4.1 성능

- 일반 키워드 검색은 빠르게 응답해야 한다.
- RAG 응답은 LLM 호출 때문에 느릴 수 있으므로 timeout과 fallback이 필요하다.
- 벡터 검색은 pgvector 인덱스 도입을 고려한다.
- 하이브리드 검색은 후보 수를 제한한다.
- 외부 API 호출은 timeout, retry, cache 정책이 있어야 한다.

### 4.2 비용

- LLM에 전달하는 context 길이를 제한한다.
- Top-K를 조절한다.
- semantic cache를 둔다.
- 리뷰 요약은 누적 요약 또는 비동기 처리로 비용을 줄인다.

### 4.3 품질

- RAG는 검색 결과에 없는 책을 추천하지 않아야 한다.
- 추천 이유는 실제 도서 정보와 리뷰를 근거로 해야 한다.
- 검색 결과 없음, 임베딩 없음, API 실패 상황의 사용자 메시지를 명확히 한다.

### 4.4 보안

- API Key, Telegram Token은 환경 변수로 관리한다.
- 사용자 식별자인 chatId는 필요 최소한으로 저장한다.
- 관리자 API는 인증을 고려한다.

### 4.5 테스트

필수 테스트:

- CSV 파싱 테스트
- 검색 조건 테스트
- 벡터 유사도 계산 테스트
- 하이브리드 RRF 병합 테스트
- RAG context 생성 테스트
- 피드백 저장/중복 방지 테스트
- 개인화 재정렬 테스트
- 외부 API Client mock 테스트

---

## 5. MVP 범위

4인 팀이 처음부터 끝까지 완성 가능한 1차 MVP는 아래로 제한한다.

### MVP 필수

1. 도서 CSV 적재
2. 도서 기본 검색
3. pgvector 설정
4. 도서 임베딩 생성
5. 벡터 검색
6. 하이브리드 검색
7. RAG 추천 설명
8. 간단한 웹 또는 REST API
9. 피드백 저장
10. 개인화 재정렬 기본형

### MVP 이후 확장

1. Telegram Bot
2. Semantic cache
3. 리뷰 요약
4. 리뷰 정보 RAG 반영
5. 도서관정보나루 API 연동
6. LLM Function Calling Agent
7. n8n 자동화

현실적인 순서:

```text
검색 기반 완성
  -> 벡터/하이브리드
  -> RAG
  -> 피드백
  -> 개인화
  -> Telegram/Agent/외부 API
```

---

## 6. 4인 협업 분담안

### A. 데이터/DB 담당

책임:

- 도서 테이블 설계
- CSV 파싱 및 적재
- 중복 정책
- pgvector 설정
- embedding 컬럼 관리
- DB 인덱스

주요 산출물:

- 도서 ERD
- CSV 적재 기능
- 도서 Repository
- DB migration 또는 schema SQL

---

### B. 검색/랭킹 담당

책임:

- 키워드 검색
- 벡터 검색
- 하이브리드 검색
- RRF 점수 계산
- Top-K / threshold 정책
- 검색 API

주요 산출물:

- SearchRequest / SearchResponse
- KeywordSearchService
- VectorSearchService
- HybridSearchService
- RrfRanker
- 검색 테스트

---

### C. AI/RAG 담당

책임:

- 임베딩 생성 API 연동
- 도서 임베딩 배치
- RAG context 생성
- LLM 호출
- 프롬프트 설계
- AI 추천 응답 파싱
- fallback 처리

주요 산출물:

- EmbeddingService
- BookEmbeddingBatch
- RagRecommendationService
- PromptTemplate
- LLM Client
- RAG 테스트

---

### D. 사용자/피드백/확장 담당

책임:

- REST/Web/Telegram UI
- 피드백 저장
- 관리자 통계 API
- 개인화 선호 벡터 계산
- 개인화 재정렬
- 추후 외부 API/Agent 연동

주요 산출물:

- Feedback Entity/Repository/Service
- Feedback API
- PersonalizationService
- 사용자 검색 화면 또는 Bot
- 통계/관리 API

---

## 7. 추천 신규 패키지 구조

기존 코드와 무관하게 새로 잡는다면 아래처럼 단순하게 시작한다.

```text
com.nhnacademy.library
  ├─ book
  │   ├─ domain
  │   ├─ repository
  │   ├─ service
  │   └─ dto
  ├─ ingest
  │   ├─ csv
  │   └─ batch
  ├─ search
  │   ├─ keyword
  │   ├─ vector
  │   ├─ hybrid
  │   └─ ranking
  ├─ ai
  │   ├─ embedding
  │   ├─ rag
  │   └─ llm
  ├─ feedback
  │   ├─ domain
  │   ├─ repository
  │   └─ service
  ├─ personalization
  ├─ external
  │   └─ librarynaru
  └─ api
```

원칙:

- 검색과 AI를 같은 서비스에 섞지 않는다.
- 임베딩 생성과 벡터 검색을 분리한다.
- RAG는 검색 결과를 소비하는 상위 서비스로 둔다.
- 피드백과 개인화는 검색 결과를 후처리하는 별도 모듈로 둔다.
- 외부 API는 domain별 client로 분리한다.

---

## 8. 우선 설계해야 할 API 초안

### 도서 검색

```http
GET /api/books/search?keyword=자바&type=KEYWORD&page=0&size=10
GET /api/books/search?keyword=초보자를 위한 프로그래밍&type=VECTOR&page=0&size=10
GET /api/books/search?keyword=자바 입문&type=HYBRID&page=0&size=10
```

### RAG 추천

```http
POST /api/recommendations/rag
Content-Type: application/json

{
  "question": "초보자가 읽기 좋은 자바 책 추천해줘",
  "topK": 5
}
```

### 피드백

```http
POST /api/feedback
Content-Type: application/json

{
  "userId": "telegram-chat-id-or-session-id",
  "query": "자바 입문",
  "bookId": 123,
  "type": "GOOD"
}
```

### 개인화 검색

```http
GET /api/books/search/personalized?userId=12345&keyword=자바&type=HYBRID
```

### Agent 확장

```http
POST /api/assistant/ask
Content-Type: application/json

{
  "userId": "12345",
  "message": "자바의 정석 리뷰랑 빌릴 수 있는 도서관 알려줘"
}
```

---

## 9. 구현 순서

### 1단계: 데이터 기반 만들기

- 도서 테이블 설계
- CSV 적재
- 기본 조회/상세 조회
- 키워드 검색

완료 기준:

- 도서 데이터가 DB에 들어간다.
- 제목/저자/ISBN으로 검색된다.
- 검색 결과와 상세 조회가 API로 제공된다.

### 2단계: 벡터 검색 만들기

- pgvector 설정
- embedding 컬럼 추가
- 임베딩 생성 서비스
- 도서 임베딩 배치
- 사용자 질의 임베딩
- 벡터 유사도 검색

완료 기준:

- 자연어 질의로 의미상 관련 있는 책이 검색된다.

### 3단계: 하이브리드 검색 만들기

- 키워드 검색과 벡터 검색 병합
- RRF 점수 계산
- 중복 제거
- 검색 타입 분기

완료 기준:

- `KEYWORD`, `VECTOR`, `HYBRID` 검색을 선택할 수 있다.
- 하이브리드 결과가 RRF 점수 기준으로 정렬된다.

### 4단계: RAG 추천 만들기

- 검색 결과 Top-K 선정
- context 생성
- 프롬프트 작성
- LLM 호출
- 추천 이유 반환

완료 기준:

- 사용자가 자연어로 질문하면 검색 결과 기반 추천 설명이 나온다.
- 검색 결과에 없는 책을 추천하지 않는다.

### 5단계: 피드백과 개인화

- 피드백 저장
- 도서별/사용자별 통계
- 사용자 선호 벡터 계산
- 개인화 재정렬

완료 기준:

- GOOD 피드백 3개 이상인 사용자는 검색 결과가 취향에 맞게 재정렬된다.

### 6단계: 확장

- Telegram Bot
- Semantic cache
- 리뷰 요약
- 외부 API
- Function Calling Agent
- n8n

---

## 10. 버려도 되는 것과 나중에 할 것

처음부터 하지 않아도 되는 것:

- n8n
- Agent A2A 구조
- 모든 도서관정보나루 API
- RabbitMQ 리뷰 요약
- Semantic cache 고도화
- 관리자 대시보드
- 외부 API 전체 facade/refactoring

처음부터 반드시 해야 하는 것:

- 도서 데이터 품질
- 검색 API 계약
- 임베딩 생성 흐름
- 벡터 검색 정확도 확인
- RRF 병합 기준
- RAG hallucination 방지 규칙
- 피드백 데이터 구조

---

## 11. 팀 작업 규칙 제안

- 먼저 API DTO와 DB 스키마를 합의한다.
- 각 담당자는 자기 모듈의 public interface를 먼저 만든다.
- 검색 담당과 AI 담당은 `SearchResult` 계약을 공유한다.
- 피드백/개인화 담당은 검색 결과 DTO에 필요한 점수 필드를 합의한다.
- 외부 API는 MVP 이후 branch에서 붙인다.
- 테스트 데이터는 작게 시작하고, 임베딩 배치는 일부 데이터로 먼저 검증한다.

공유 계약 예시:

```text
BookSummary
  id
  isbn
  title
  author
  publisher
  description
  imageUrl

SearchResult
  book
  keywordScore
  vectorSimilarity
  rrfScore
  personalizationScore
  finalScore
```

---

## 12. 최종 요약

이 프로젝트의 핵심은 다음 순서를 지키는 것이다.

```text
데이터
  -> 검색
  -> 벡터 의미 검색
  -> 하이브리드 랭킹
  -> RAG 추천 설명
  -> 피드백
  -> 개인화
  -> Agent/외부 API 자동화
```

4인 협업에서는 처음부터 모든 기능을 병렬로 만들면 충돌이 크다. 먼저 데이터 모델과 검색 API 계약을 고정하고, 그 위에 벡터/RAG/피드백/개인화를 얹는 방식이 가장 안전하다.

