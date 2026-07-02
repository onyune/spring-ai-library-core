# 검색 및 RAG 시스템 성능 개선(Optimization) 계획

본 문서는 Spring AI Library Core 프로젝트의 검색(Vector/Hybrid) 및 RAG 시스템 고도화를 위한 아키텍처 및 성능 개선 계획을 담고 있습니다. 기존 논의된 아이디어를 확장하고, 인프라 및 애플리케이션 레벨의 추가 개선 방안을 포함합니다.

---

## 1. 검색 전략 다층 캐싱(Multi-Level Caching) 아키텍처 완성 ✅

### 1.1 시맨틱 캐시(Semantic Cache) `SearchType` 분리 및 확대 적용 (완료)
* **개선 사항**: 기존에는 단순 벡터 유사도만으로 캐시를 반환하여 RAG, Vector, Hybrid 전략의 서로 다른 결과값이 충돌하는 문제가 있었습니다. 
* **구조**: `BookSearchCache` 엔티티에 `SearchType` 컬럼을 추가하고, `SemanticCacheService`에서 이를 기준으로 분리 조회 및 저장하도록 개선했습니다.
* **확대 적용**: 이제 `RagSearchStrategy`뿐만 아니라 `VectorSearchStrategy`와 `HybridSearchStrategy`에서도 시맨틱 캐시(L3)를 활용하여 LLM/DB 호출 없이 빠르게 응답합니다.

### 1.2 임베딩(Embedding) API 캐싱 (완료)
* **개선 사항**: `EmbeddingSubAgent.getEmbedding()`에 `@Cacheable(value = CACHE_EMBEDDING)`을 적용하여 사용자 질문(`text`)에 대한 임베딩(`float[]`) 결과를 Redis에 캐싱합니다. 
* **구조**: 텍스트-벡터 변환 결과를 재사용함으로써, 동일 질문 시 발생하는 외부 OpenAI/로컬 모델 API 호출 비용과 Latency를 획기적으로 절약합니다.

### 1.3 L1(Caffeine) -> L2(Redis) -> L3(Semantic) 다층 캐싱 파이프라인 (완료)
* **개선 사항**: 기존 단일 Redis 캐시를 넘어 로컬 메모리와 원격 캐시를 혼합한 `MultiLevelCacheManager`를 직접 구현 및 적용했습니다.
* **구조 및 동작 흐름**:
  1. **L1 캐시 (Caffeine - Local Memory)**: `@Cacheable` 인터셉터를 통해 가장 먼저 검사됩니다. Exact Match 시 네트워크 I/O조차 발생하지 않는 초고속 응답을 제공합니다. (TTL: 10분, 최대 1000개)
  2. **L2 캐시 (Redis - Distributed Exact Match)**: L1에 데이터가 없으면 분산 환경 공유를 위한 Redis를 검사합니다. Hit 발생 시 자동으로 L1(Caffeine)에도 결과를 동기화(Put)하여 다음 요청부터는 L1에서 응답하도록 설계되었습니다. (TTL: 3시간)
  3. **L3 캐시 (pgvector - Semantic Cache)**: L1/L2(Exact Match)에서 모두 Miss가 났을 때(조사나 띄어쓰기가 약간 다를 때), 각 검색 전략 로직 내에서 `SemanticCacheService`를 호출해 벡터 유사도(Cosine Distance) 기반으로 이전 결과를 복원합니다.
----

## 2. 하이브리드 검색 병렬화 (Parallelization)

### 2.1 비동기 병렬 처리 (기존 논의) 
* **문제점**: `HybridSearchStrategy`에서 키워드 검색(RDB)과 벡터 검색(pgvector 등)이 순차적으로 실행되어 두 I/O 대기 시간의 합만큼 지연됨.
* **해결 방안**: 두 쿼리를 병렬로 던진 후, `rrfFusionSubAgent`에서 두 결과가 모두 도착했을 때 융합. 응답 시간을 "가장 느린 검색 하나"의 시간으로 단축.

### 🔥 2.2 추가 성능 개선: Java 21 Virtual Threads 도입
* RAG 및 하이브리드 검색은 철저한 **I/O Bound**(DB 쿼리, LLM API 호출 등) 작업입니다. 
* 기존의 OS 스레드 기반 스레드풀 대신 **Java 21의 가상 스레드(Virtual Threads)**를 사용하면, 스레드 블로킹으로 인한 컨텍스트 스위칭 오버헤드가 제거되어 수천 개의 병렬 검색 요청도 적은 메모리로 우아하게 처리할 수 있습니다.

### 🔥 2.3 추가 성능 개선: 타임아웃 및 폴백(Fallback) 메커니즘
* 벡터 검색 DB에 장애가 발생하거나 병목이 생겼을 때, `CompletableFuture.orTimeout()`을 활용해 특정 시간(예: 1초) 내에 벡터 결과가 오지 않으면, 기다리지 않고 즉시 키워드 검색 결과만으로 폴백(Fallback)하여 응답을 반환해야 서비스 가용성(Availability)이 유지됩니다.

### ✅ 하이브리드 검색 병렬화 구현 완료 내역
* **적용**: `HybridSearchStrategy.java` 내의 `executeParallelSearchAndFuse` 메서드로 분리 구현 완료.
* **Java 21 Virtual Threads 활성화**: `application.properties`에 `spring.threads.virtual.enabled=true`를 추가하여 전역 활성화하고, 전략 클래스 내부에서 `Executors.newVirtualThreadPerTaskExecutor()`를 직접 생성하여 전용 비동기 풀로 사용.
* **병렬 실행 및 시간 단축**: 키워드 검색과 벡터 검색을 `CompletableFuture.supplyAsync`로 병렬 실행하여 I/O 대기 시간을 `A+B`에서 `Max(A, B)`로 획기적으로 단축.
* **안전장치(2초 Timeout & Fallback)**: 벡터 검색에 `.orTimeout(2, TimeUnit.SECONDS)`를 체이닝. 2초 초과 시 빈 리스트를 반환(`.exceptionally`)하도록 Fallback을 구현하여, 벡터 DB나 임베딩 API 장애 시에도 "키워드 검색 결과만으로" 무중단 정상 응답을 보장함.

---

## 3. RAG Reranker 및 LLM 활용 최적화 - 6번에 개선사항 작성

### 3.1 LLM as a Reranker (기존 논의)
* **문제점**: DB 검색으로 상위 5개를 미리 확정하고 LLM은 사유만 작성하여, LLM의 추론 능력이 검색 품질에 영향을 주지 못함.
* **해결 방안**: 후보군을 15~20개로 확대하여 LLM에게 전달하고, 시스템 프롬프트에 '절대적 점수 기준표'를 제공. LLM이 직접 사용자의 질문 의도에 맞는 Top 5를 선택(Reranking)하고 사유를 작성하도록 책임 부여.

### 🔥 3.2 추가 성능 개선: Cross-Encoder를 활용한 2-Stage Reranking
* DB에서 100개를 뽑아 바로 무거운 LLM(GPT-4 등)에 20개를 넘기면, 프롬프트 토큰이 기하급수적으로 커져 API 비용과 응답 시간이 매우 증가합니다.
* **로컬/경량화된 Cross-Encoder 모델**(Cohere Rerank API 사용)을 중간에 삽입하여 `100개 -> 10개`로 1차 필터링을 한 후, 10개만 LLM에 넘겨서 Top 5를 고르게 하면 비용과 속도, 퀄리티를 모두 잡을 수 있습니다.

### 🔥 3.3 추가 성능 개선: 스트리밍 응답 (Server-Sent Events, SSE) - 이건 추후에 다시...
* LLM이 추천 사유를 모두 작성할 때까지 프론트엔드가 대기하면 체감 지연(Perceived Latency)이 너무 큽니다.
* RAG 결과를 반환할 때 LLM의 사유 생성을 **토큰 단위로 SSE 스트리밍**하여 화면에 즉시 타자 치듯 보여주면, 사용자는 로딩 대기 시간이 거의 없다고 느끼게 됩니다.

---

## 4. 기타 시스템 전반(System-wide) 성능 개선 포인트

### 🔥 4.1 트랜잭션과 외부 API(LLM) 호출의 분리 (Connection Pool 고갈 방지)
* LLM API 호출은 수 초 단위의 긴 시간이 소요됩니다. 만약 `@Transactional`이 걸린 메서드 안에서 외부 API(LLM)를 호출하면, LLM 응답을 기다리는 수 초 동안 **DB 커넥션을 계속 점유**하게 됩니다.
* 트래픽이 몰리면 DB 커넥션 풀(HikariCP)이 순식간에 고갈되어 전체 서비스가 멈출 수 있습니다. 반드시 트랜잭션 범위 밖에서 LLM 호출이 일어나도록 `Service` 레이어를 철저하게 분리/설계해야 합니다.

### 🔥 4.2 pgvector 인덱스 최적화 (HNSW)
* 시맨틱 캐시나 벡터 도서 테이블에 `vector` 데이터만 넣고 인덱스를 생성하지 않으면 데이터가 쌓일수록 Full Scan이 발생하여 치명적인 성능 저하가 발생합니다.
* 반드시 벡터 컬럼에 **HNSW (Hierarchical Navigable Small World)** 또는 **IVFFlat** 인덱스(가급적 HNSW 권장)가 올바르게 적용되어 있는지 확인해야 합니다.

---

## 5. 키워드 검색 고도화 (Full Text Search & NLP 전처리) ✅

### 5.1 PostgreSQL FTS(Full Text Search) GIN 인덱스 연동 (완료)
* **개선 사항**: 기존 `LIKE '%키워드%'` 기반의 본문(book_content) 검색은 풀 테이블 스캔(Full Table Scan)을 유발하여 대용량 데이터에서 심각한 성능 저하를 초래합니다.
* **적용 내역**:
  * PostgreSQL의 전문 검색 기능인 `to_tsvector`와 GIN 인덱스를 활용하기 위해, QueryDSL 커스텀 함수 `ts_match_korean`을 구현 및 등록(`FunctionContributor`) 완료했습니다.
  * `KeywordBookSearchRepository`에 적용하여 대용량 본문 검색 성능을 극대화했습니다.

### 5.2 KOMORAN 형태소 분석기 기반 질의 전처리(Query Processing) (완료)
* **개선 사항**: FTS 엔진에 자연어("자바 스프링 찾아줘")를 그대로 넣으면 어휘 불일치(Vocabulary Mismatch)로 매칭률이 0건으로 떨어지는 문제가 있었습니다.
* **적용 내역**:
  * 순수 자바 기반의 가벼운 형태소 분석기 **KOMORAN** 라이브러리를 도입했습니다.
  * `QueryAnalyzerSubAgent`를 생성하여 "책", "관련", "찾아줘" 등 커스텀 불용어(Stop Words) 사전을 바탕으로 핵심 명사(예: "자바 스프링")만 추출하도록 구현했습니다.
  * 추출 로직은 CPU 집약적이므로 `CacheConfig`에 `queryAnalysisCache`를 신규 선언(TTL 24시간)하여 즉각적인 응답 속도를 보장하도록 최적화했습니다.
  * `KeywordSearchStrategy` 가장 앞단에 배치하여, 검색 엔진(DB)으로 들어가기 전 무조건 불용어가 걷히고 정제된 키워드만 전달되도록 완벽히 연결했습니다.

### 5.3 DTO 최적화 및 레거시 파라미터 청소 (완료)
* **적용 내역**: 객체 지향 관점에서 사용하지 않는 더미 필드(`warmUp`)를 `BookSearchRequest` 및 관련된 4개의 Strategy 클래스에서 완벽히 제거하여 DTO의 순수성을 확보하고 불필요한 결합도를 낮췄습니다.

---

## 6. 엔터프라이즈급 다단 리랭킹(Cascade Reranking) 파이프라인 구축 ✅

### 6.1 4단계(4-Stage) 하이브리드 Reranking 아키텍처 완성 (완료)
* **개선 사항**: 기존 RRF(단순 순위 융합)만으로는 사용자 개인화 및 문맥적 유사도를 깊게 판단하지 못하는 한계가 있었으며, LLM에게 많은 후보를 주면 토큰 낭비가 발생했습니다.
* **적용 내역**: 비용과 성능을 완벽하게 타협한 4단계 깔때기(Funnel) 구조를 `RrfBookReranker`와 `BookRecommendationAgent`에 구현했습니다.
  1. **1차망 (DB 하이브리드 검색)**: 넓은 그물망(`candidateSize=100`)을 펼쳐서 100권을 확보 (Recall 극대화). (이 사이즈를 줄이면 최적합한 도서도 찾지 못하게 됨)
  2. **2차망 (비즈니스/개인화 필터)**: 사용자의 선호도 및 글로벌 피드백 점수를 가산하여 30권으로 압축.
  3. **3차망 (Cohere Cross-Encoder)**: 살아남은 30권을 `CohereRerankClient`에 넘겨 딥러닝 기반 문맥 유사도(Semantic Similarity)를 정밀 채점, 최정예 10권으로 압축 (Precision 극대화).
  4. **4차망 (LLM as a Reranker)**: 10권 중 5권을 LLM이 직접 선별하여 추천 사유 작성.

### 6.2 Cohere Rerank API (Cross-Encoder) 연동 (완료)
* **적용 내역**: `CohereRerankClient`를 신규 생성하여 Spring 3 최신 `RestClient` 기반으로 Cohere API(`rerank-multilingual-v3.0`)와 통신하도록 연동했습니다.
* **Fallback 메커니즘**: API 장애 시 원본 순위를 유지하는 안전장치를 마련하고, 통신용 DTO를 자바 `record` 객체로 깔끔하게 구성했습니다.

### 6.3 LLM 채점 기준표 및 10권 혼합 반환 로직 구현 (완료)
* **프롬프트 초정밀화**: `BookRecommendationAgent`의 시스템 프롬프트에 0점~100점(5단계)의 엄격한 선별 및 평가 기준표를 주입하여, LLM이 10권 중 가장 훌륭한 5권만 선별하도록 제약했습니다.
* **JSON 엔티티(Entity) 유지 및 UI/UX 설계**: 
  * 텔레그램 봇 등 다채널 지원을 위해 SSE 스트리밍 대신 안정적인 JSON(객체) 응답 방식을 확정했습니다.
  * 프론트엔드 화면에는 **AI가 채점한 최상위 5권(추천 코멘트 포함)**을 상단에 노출하고, AI의 선택은 못 받았으나 **Cohere가 선별한 나머지 5권(원본 도서)**을 하단에 이어 붙여, 사용자에게 총 10권의 고품질 도서 목록을 제공하도록 응답 로직을 완성했습니다.

---

## 7. 페이지네이션 캐시 충돌 버그 및 pgvector 안정성 개선 ✅

### 7.1 벡터 검색 시맨틱 캐시 페이징(Pagination) 충돌 해결 (완료)
* **문제점**: 기존 `VectorSearchStrategy`에서 1페이지 요청 시 DB에서 가져온 1페이지 분량(예: 20권)만 캐시에 저장하여, 2페이지 요청 시 캐시에서 동일하게 20권 리스트가 반환됨. 이후 인메모리 `paginate()` 메서드가 20권짜리 리스트에서 21번째 인덱스를 찾으려 시도하다가 예외를 발생시키거나 빈 배열을 반환하는 버그 발생. (UI상 결과는 25개로 뜨지만 2페이지는 백지가 됨)
* **적용 내역**:
  * 타 전략(`RagSearchStrategy`, `HybridSearchStrategy`)과 동일하게, 캐시가 비어있을 때는 **충분한 후보 사이즈(예: 상위 100건)**를 DB에서 조회하여 캐시에 전체를 저장하도록 `VectorSearchStrategy` 코드 수정.
  * 이후 100건의 전체 풀에서 페이징을 처리하도록 로직을 변경하여 다중 페이지 로딩 간의 데이터 유실 및 인덱스 초과 버그를 완벽 해결했습니다.

### 7.2 pgvector 소수점 기수법(Scientific Notation) 파싱 예외 차단 (완료)
* **문제점**: 자바의 `Arrays.toString(float[])` 메서드를 사용해 배열을 문자열로 직렬화할 때, 절대값이 매우 작은 실수(예: `-0.000358`)는 `-3.5842942E-4` 처럼 지수 표기법으로 반환됩니다. 이를 PostgreSQL pgvector가 파싱하다가 구문 오류(Syntax Error)를 일으켜 쿼리가 조용히 실패하거나 빈 결과를 뱉는 심각한 버그를 발견했습니다.
* **적용 내역**: `VectorBookSearchRepository`의 모든 쿼리에서 벡터 문자열 변환 시 `String.format("%.6f", ...)`를 사용하도록 수정. `E-4` 형태가 아닌 순수 소수점 형태로만 캐스팅되도록 안전장치를 구축하여 벡터 쿼리 안정성을 100% 확보했습니다.

---

## 8. 개인화 맞춤형 도서 추천(Personalized Recommendation) 파이프라인 구축 ✅

### 8.1 최근 관심사 기반 사용자 프로파일링 (완료)
* **개선 사항**: 사용자의 "최신 관심사"를 반영해 도서를 추천하는 기능을 추가했습니다. 과거와 현재의 관심사 변화(Concept Drift)를 추적하기 위해, 최신 20개의 도서 피드백에서 벡터들을 가져와 **평균(Centroid) 벡터**를 도출하는 `UserPreferenceVectorService`를 연동했습니다.

### 8.2 개인화 벡터 검색용 커스텀 쿼리 및 임계값(Threshold) 무시 (완료)
* **문제점**: 20권의 다채로운 특징이 섞인 평균 벡터는 어느 한 권의 책과도 코사인 유사도(Cosine Similarity)가 0.90(기존 검색 Threshold)을 넘기 힘들기 때문에 검색 결과가 모두 필터링되는 문제가 발생했습니다.
* **적용 내역**: `VectorBookSearchRepository`에 임계값을 묻지도 따지지도 않고 순수하게 유사도 순으로만 랭킹을 매겨 상위 N건(`LIMIT`)을 가져오는 전용 메서드 `findPersonalizedBooks`를 신규 생성했습니다.

### 8.3 텔레그램 LLM 연동 및 프롬프트 주입 (완료)
* **적용 내역**:
  * 텔레그램 채팅에서 사용자가 "나한테 맞는 책 추천해 줘"라고 할 때 발동하는 `@Tool` 인 `getPersonalizedRecommendation` 메서드를 `BookSearchTool`에 추가했습니다.
  * 뽑아온 도서들에 `aiComment` 필드값으로 `"사용자 선호책 탐색"`을 고정 부여하여 프론트엔드 UI에서도 개인화 결과임을 명시적으로 인지할 수 있도록 하였습니다.
