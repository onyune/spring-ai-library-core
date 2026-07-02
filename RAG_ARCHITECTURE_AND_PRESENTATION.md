# Spring AI Library Core - RAG 아키텍처 및 발표(보고서) 자료

이 문서는 프로젝트의 RAG(Retrieval-Augmented Generation) 시스템 아키텍처 흐름도와, 이를 기반으로 한 기술 공유(Tech Talk) 발표 슬라이드 대본을 포함하고 있습니다.

---

## 1. RAG 아키텍처 파이프라인 흐름도 (Mermaid)

```mermaid
graph TD
    %% 스타일 정의
    classDef user fill:#f9f,stroke:#333,stroke-width:2px;
    classDef agent fill:#bbf,stroke:#333,stroke-width:2px;
    classDef process fill:#dfd,stroke:#333,stroke-width:2px;
    classDef db fill:#fdd,stroke:#333,stroke-width:2px;
    classDef cache fill:#ffd,stroke:#333,stroke-width:2px;

    User([👤 사용자 질의 입력]):::user --> IntentAgent
    
    subgraph "1. 의도 파악 및 라우팅 (Intent & Routing)"
        IntentAgent[🤖 AutoSearchAgent\n(의도 분류)]:::agent
        Router{Strategy Pattern\nRouter}
        IntentAgent --> Router
    end

    subgraph "2. 다층 캐시 (Multi-Level Caching)"
        L1[Caffeine L1\n(Local Memory)]:::cache
        L2[Redis L2\n(Distributed)]:::cache
        L3[Semantic Cache L3\n(pgvector)]:::cache
        Router -.->|1차 탐색| L1
        L1 -.->|Miss| L2
        L2 -.->|Miss| L3
    end

    subgraph "3. 병렬 검색 (Retrieval with Virtual Threads)"
        VThread((Java 21\nVirtual Threads)):::process
        Router -->|캐시 Miss 시| VThread
        
        Komoran[KOMORAN\n형태소 분석기]:::process
        VThread -->|Keyword Search| Komoran
        Komoran --> FTS[(PostgreSQL\nGIN Index)]:::db
        
        VThread -->|Vector Search| VecDB[(PostgreSQL\npgvector)]:::db
    end

    subgraph "4. 다단 리랭킹 (4-Stage Cascade Reranking)"
        R1[Stage 1: DB RRF 융합\n(후보군 100권 확보)]:::process
        R2[Stage 2: 비즈니스 필터\n(30권으로 압축)]:::process
        R3[Stage 3: Cohere API\nCross-Encoder (10권 선별)]:::process
        R4[Stage 4: LLM Reranker\n(Top 5권 최종 선정)]:::process
        
        FTS --> R1
        VecDB --> R1
        R1 --> R2 --> R3 --> R4
    end

    subgraph "5. 개인화 파이프라인 (Personalization)"
        Profile[최신 피드백 20권\nCentroid Vector 도출]:::process
        CustomQuery[Threshold 무시\n전용 벡터 커스텀 쿼리]:::process
        Router -->|'나한테 맞는 책'| Profile
        Profile --> CustomQuery
        CustomQuery --> VecDB
    end

    subgraph "6. 컨텍스트 주입 및 생성 (Augmentation & Generation)"
        RecAgent[🤖 BookRecommendationAgent\n(컨텍스트 주입 및 생성)]:::agent
        R4 --> RecAgent
    end

    RecAgent --> Output([💡 최종 맞춤형 추천 코멘트 및 도서 반환]):::user
```

---

## 2. 테크 톡(Tech Talk) 발표 슬라이드 및 스크립트

발표 시간: 10~15분 내외
발표 대상: 동료 개발자, 엔지니어 (평가 자리가 아닌 아키텍처 및 기술 고민 공유의 목적)

### 📊 [Slide 1] 타이틀
* **화면 내용**: "Spring AI 기반 엔터프라이즈 도서 검색 및 RAG 시스템 아키텍처" (발표자 이름 및 팀명)
* **발표 스크립트**: "안녕하세요, Spring AI Library Core 프로젝트에서 검색 엔진 아키텍처와 RAG 파이프라인 설계를 담당한 [이름]입니다. 오늘 이 자리에서는 저희 팀이 단순한 API 호출을 넘어서 어떻게 검색의 품질과 성능을 엔터프라이즈급으로 끌어올렸는지, 그 설계 과정과 트러블슈팅 경험을 편안하게 공유하고자 합니다."

### 📊 [Slide 2] 아키텍처 오버뷰: RAG 파이프라인 전체 흐름
* **화면 내용**: 위에서 작성된 Mermaid 다이어그램 축소판 배치
* **발표 스크립트**: "전체 아키텍처 흐름도입니다. 사용자의 자연어 질의가 들어오면, AI 에이전트가 의도를 파악하여 적절한 검색 전략으로 라우팅합니다. 이 과정에서 Java 21의 가상 스레드를 활용한 병렬 검색, 4단계의 리랭킹, 다층 캐싱 기법이 맞물려 돌아가며 최종적으로 환각 없는 추천 결과를 만들어냅니다. 각각의 핵심 구조를 하나씩 살펴보겠습니다."

### 📊 [Slide 3] 1단계: 유연한 라우팅과 객체지향 설계 (Strategy Pattern)
* **화면 내용**: `AutoSearchAgent` -> `Vector`, `Keyword`, `Hybrid`, `Rag`, `Personalized` 인터페이스 구현체 다이어그램
* **발표 스크립트**: "첫 번째는 확장에 열려있는 검색 구조 설계입니다. 사용자의 질문이 단순 키워드 검색인지, 책 추천을 원하는 RAG인지 `AutoSearchAgent`가 판단합니다. 이후 디자인 패턴 중 Strategy 패턴을 활용하여 각 검색 로직을 격리시켰습니다. 새로운 검색 방식이 추가되어도 기존 코드를 전혀 건드리지 않고 플러그인처럼 붙일 수 있도록 설계했습니다."

### 📊 [Slide 4] 2단계: Virtual Threads 기반 하이브리드 병렬 검색
* **화면 내용**: `CompletableFuture`를 활용한 병렬 처리 코드 스니펫 및 Timeout 로직
* **발표 스크립트**: "하이브리드 검색의 가장 큰 고민은 키워드 검색과 벡터 검색을 동시에 할 때 응답 시간이 두 배로 걸린다는 점이었습니다. 이를 해결하기 위해 Java 21의 Virtual Threads를 도입해 두 I/O 작업을 완벽히 비동기 병렬로 처리했습니다. 또한, 벡터 DB에 병목이 생겼을 때 전체 서비스가 죽지 않도록 2초 타임아웃을 걸고 키워드 검색 결과만 반환하는 무중단 Fallback 메커니즘을 적용했습니다."

### 📊 [Slide 5] 3단계: 비용과 품질을 모두 잡은 4-Stage Cascade Reranking
* **화면 내용**: 100권 -> 30권 -> 10권 -> 5권으로 좁아지는 깔때기(Funnel) 모형
* **발표 스크립트**: "RAG에서 LLM에게 너무 많은 후보를 주면 토큰 낭비가 심하고 속도가 느려집니다. 그래서 4단계 리랭킹 파이프라인을 구축했습니다. DB에서 넓게 100권을 가져오고, 비즈니스 룰로 30권으로 줄인 뒤, 가벼운 외부 모델인 Cohere Cross-Encoder를 징검다리로 써서 10권으로 압축했습니다. 마지막으로 LLM은 이 정예 10권 중에서만 5권을 선별해 코멘트를 작성하도록 하여, 비용과 속도, 정확도를 모두 타협한 가장 이상적인 구조를 완성했습니다."

### 📊 [Slide 6] 아키텍처 최적화 1: L1-L2-L3 다층 캐싱 시스템
* **화면 내용**: Caffeine(L1) -> Redis(L2) -> pgvector Semantic Cache(L3) 흐름도
* **발표 스크립트**: "성능 최적화의 첫 번째는 단연 캐싱입니다. 저희는 로컬 메모리인 Caffeine과 분산 캐시인 Redis를 엮고, Exact Match가 실패했을 때 띄어쓰기나 조사가 달라도 의미가 같으면 캐시를 태우는 3단계 시맨틱 캐시를 구현했습니다. 개발 도중 페이징 처리 시 캐시 인덱스가 꼬이는 버그가 있었는데, 단건이 아닌 100건의 후보 풀을 통째로 캐싱하고 인메모리에서 페이징을 처리하도록 로직을 변경해 우아하게 해결했습니다."

### 📊 [Slide 7] 아키텍처 최적화 2: 전문 검색(FTS)과 KOMORAN 형태소 분석
* **화면 내용**: 자연어 질의 -> KOMORAN 핵심 명사 추출 -> GIN 인덱스 검색 과정
* **발표 스크립트**: "두 번째는 RDB 풀 스캔 문제 해결입니다. 단순 LIKE 검색 대신 PostgreSQL의 전문 검색(FTS) 기능과 GIN 인덱스를 도입했습니다. 그런데 사용자가 '자바 스프링 찾아줘'라고 쳤을 때 어휘가 불일치하는 문제가 발생했습니다. 이를 해결하기 위해 가벼운 형태소 분석기인 KOMORAN을 앞단에 배치하여, 조사나 동사를 날리고 핵심 명사만 추출해 DB에 전달하는 전처리 파이프라인을 구축했습니다."

### 📊 [Slide 8] 아키텍처 최적화 3: 벡터 예외 차단과 개인화 추천(Personalization)
* **화면 내용**: `E-4` 지수표기법 에러 캡처 화면과 Centroid Vector 연산 개념
* **발표 스크립트**: "마지막으로 기억에 남는 트러블슈팅과 개인화 기능입니다. 자바에서 벡터를 문자열로 직렬화할 때, 절대값이 작은 실수는 `E-4` 같은 지수 표기법으로 나오게 되는데, 이 때문에 pgvector 파서가 터지는 버그가 있었습니다. 포맷팅을 명시적으로 제어(%.6f)하여 이를 잡아냈습니다. 또한, 사용자가 가장 최근에 관심을 보인 20권의 벡터 평균(Centroid)을 내서 개인화 추천을 하는데, 평균 벡터의 특성상 유사도가 임계값(0.9)을 넘기 힘들어 결과가 없는 버그를 커스텀 쿼리로 해결하여 개인화 추천을 완성했습니다."

### 📊 [Slide 9] 결론 및 Q&A
* **화면 내용**: 요약 및 팀원들의 노력 강조
* **발표 스크립트**: "저희는 단순히 'AI API를 쓴다'를 넘어, 그 과정에서 발생하는 병목, 비용, 안정성 문제를 아키텍처 레벨에서 어떻게 풀 것인지 치열하게 고민했습니다. 프론트엔드와 텔레그램 연동을 멋지게 완성해준 팀원들과 함께 개발하면서 많은 것을 배웠습니다. 경청해 주셔서 감사합니다. 질문 받겠습니다."
