# 🚀 피드백 기반 개인화 추천 연동 및 팀 협업 방안

## 1. 개요 및 아키텍처 방향성

본 문서는 사용자의 피드백(👍/👎) 데이터를 활용하여 도서 검색 결과를 개인화하는 시스템의 아키텍처 설계와, 이를 구현하기 위한 Telegram/Feedback 팀과 Search/Personalization 팀 간의 역할 분담(R&R)을 정의합니다.

### 💡 왜 모듈을 분리하고 2-Step으로 구성하는가?

1. **안전한 2-Step 분리 구조**
   - 검색 로직과 피드백/개인화 로직을 강하게 결합하지 않습니다.
   - 만약 개인화 로직이나 피드백 서버에 장애가 발생하더라도, 기존 검색(1단계) 결과가 그대로 반환되므로 서비스 전체 장애를 방지할 수 있습니다.
2. **검색어 우선 원칙 (엉뚱한 결과 방지)**
   - 평소 '소설'에만 좋아요를 누른 사용자가 "자바 스프링"을 검색했을 때 소설책이 나오는 치명적인 오류를 방지해야 합니다.
   - 1단계(기본 검색)에서 오직 "자바 스프링"과 관련된 도서 목록만 먼저 추출합니다.
   - 개인화 가중치를 낮게(예: `α=0.3`) 설정하여, **개인화는 필터링된 유효한 후보군 내에서 미세한 순위만 조정**하는 역할만 수행합니다.
3. **느슨한 결합 (API 기반 연동)**
   - Telegram/Feedback 모듈은 피드백 데이터를 적재하고 제공하는 역할만 수행합니다.
   - Search 모듈은 제공받은 데이터를 바탕으로 벡터 연산을 수행하는 역할만 수행합니다.
   - 서로의 내부 DB 스키마나 비즈니스 로직을 몰라도, 약속된 인터페이스(DTO)만 맞추면 병렬 개발이 가능합니다.

---

## 2. 모듈별 역할 및 구현 목표 (R&R)

### 📱 Telegram / Feedback 모듈 측 (피드백 데이터 생산자)
사용자의 인터랙션을 담당하며 피드백을 수집하고, 결과를 예쁘게 포맷팅하여 사용자에게 보여주는 역할을 맡습니다.

#### 주요 Task
1. **피드백 데이터 적재**
   - 사용자가 인라인 키보드(👍/👎)를 클릭하면 `SearchFeedback` 엔티티(테이블)에 데이터를 안전하게 저장합니다.
2. **조회용 Internal API (또는 Service 메서드) 제공**
   - Search 모듈에서 특정 유저의 취향을 분석할 수 있도록, **최근 'GOOD' 피드백을 누른 도서 ID 목록**을 반환하는 메서드를 제공합니다.
   - *예시 메서드:* `List<Long> getRecentLikedBookIds(Long chatId)` (최신순 20개 정도면 충분)
3. **개인화 UI 노출 (검색 결과 포맷팅)**
   - Search 모듈이 돌려준 검색 결과(`BookSearchResponse`) 안에 `personalizationScore`(개인화 점수)가 존재하는지 확인합니다.
   - 값이 존재하면 텔레그램 메시지에 개인화 안내 문구를 추가합니다. 
     - *예: "✨ 사용자님의 취향을 분석한 맞춤 추천이 포함되어 있습니다."*
   - 각 도서 정보 옆에 기존 서치 유사도(%) 외에 선호도(%) 지표를 함께 렌더링해 줍니다.

### 🔍 Search / Personalization 모듈 측 (피드백 데이터 소비자)
전달받은 도서 ID들을 기반으로 벡터 연산을 수행하고, 검색 결과의 최종 점수를 재산정하는 역할을 맡습니다.

#### 주요 Task
1. **사용자 선호도 벡터 생성 및 캐싱**
   - Telegram 쪽 API(`FeedbackService`)를 호출하여 특정 유저(`chatId`)가 'GOOD'을 누른 도서 ID 리스트를 가져옵니다.
   - 해당 도서들의 임베딩(1024차원)을 Vector DB에서 조회한 후 평균값을 계산하여 **'사용자 선호도 벡터'**를 도출합니다.
   - 연산 비용 절감을 위해 이 선호도 벡터를 24시간 동안 Redis 등에 캐싱합니다. (새로운 피드백 추가 시 캐시 무효화 고려)
2. **2-Step 재정렬 알고리즘 적용**
   - 1차적으로 기존 하이브리드 검색(키워드+벡터) 결과를 가져옵니다.
   - 후보 도서들의 임베딩과 유저의 '선호도 벡터'를 비교하여 **코사인 유사도**를 구합니다.
   - `기존 검색 점수(RRF) + (유사도 * 0.3)` 공식을 적용해 최종 점수를 계산하고 순위를 재정렬합니다.
3. **Response DTO 결과 매핑**
   - Telegram 모듈이 UI를 그릴 수 있도록 `BookSearchResponse` DTO에 `personalizationScore` 필드를 추가하고 값을 채워 반환합니다.
   - 피드백이 3개 미만인 신규(콜드스타트) 유저의 경우 개인화를 건너뛰고 해당 필드에 `null`을 담아 보냅니다.

---

## 3. 팀 간 API 계약 (Contract) 정의

양 팀이 의존성 없이 병렬로 개발을 진행하기 위해 아래의 인터페이스(DTO) 스펙을 합의합니다.

### 3.1. Feedback 제공 인터페이스 (Feedback -> Search)
Search 모듈이 Feedback 모듈에게 유저 데이터를 요청할 때 사용할 메서드입니다.

```java
public interface FeedbackInternalService {
    /**
     * 특정 유저(chatId)가 좋아요(GOOD)를 누른 최근 도서 ID 목록을 반환합니다.
     * 콜드스타트 방지 및 최신 취향 반영을 위해 최근 N개(예: 20개)로 제한합니다.
     */
    List<Long> getRecentLikedBookIds(Long chatId);
}
```

### 3.2. 검색 결과 응답 DTO (Search -> Telegram)
Search 모듈이 텔레그램으로 결과를 돌려줄 때 사용하는 DTO의 변경점입니다.

```java
@Getter
@Setter
public class BookSearchResponse {
    private Long id;
    private String title;
    private String author;
    
    // ... 기존 필드들 ...
    
    private Double score;                  // 기존: RRF 기반 기본 검색 점수 (또는 2-step 최종 재정렬 점수)
    
    // ✨ 추가된 필드: 개인화 유사도 점수
    // 이 값이 null이 아니라면 Telegram 모듈은 "✨ 개인화 추천" UI를 노출합니다.
    private Double personalizationScore;   
}
```

---

## 4. 예상 워크플로우 시나리오

1. **유저 A**가 "자바 스프링"을 검색합니다.
2. `Telegram 모듈`이 `Search 모듈`에 쿼리와 `chatId`를 전달합니다.
3. `Search 모듈`은 `Feedback 모듈`에 `getRecentLikedBookIds(chatId)`를 호출합니다.
4. `Search 모듈`은 반환받은 도서 ID로 선호도 벡터를 만들고, "자바 스프링" 검색 결과에 가중치(0.3)를 반영하여 순위를 재정렬합니다.
5. `Search 모듈`은 `personalizationScore`가 포함된 결과를 `Telegram 모듈`에 반환합니다.
6. `Telegram 모듈`은 점수를 확인하고 사용자에게 "맞춤 추천" 메시지와 함께 결과를 보여줍니다.

---

### 💡 왜 도서 ID 리스트가 아닌 `chatId`만 먼저 전달할까? (Pull 방식 설계 의도)

Telegram 모듈에서 미리 피드백 데이터를 조회해서 넘겨주지 않고, **`chatId`만 넘긴 뒤 Search 모듈이 필요할 때 직접 데이터를 조회하도록 설계**한 데에는 다음과 같은 이유가 있습니다.

1. **캐싱(Caching) 효율성과 API 통신 최소화**
   - Search 모듈은 연산 비용을 줄이기 위해 유저의 선호도 벡터를 자체적으로 캐싱(예: Redis)합니다.
   - `chatId`만 전달받으면, Search 모듈은 내부에 캐시가 유효한지 먼저 확인하고 **캐시가 만료되었거나 없을 때만 Feedback 모듈을 호출**할 수 있습니다. 매번 불필요한 내부망 통신과 DB 조회를 방지합니다.
2. **역할과 책임의 분리 (의존성 최소화)**
   - 개인화 검색을 위해 "어떤 취향 데이터가 필요한지" 판단하는 것은 전적으로 Search 모듈의 몫입니다. 
   - Telegram 모듈(UI/Controller 계층)이 데이터 준비 로직까지 떠안는 것보다, 단순히 검색 요청자(`chatId`)만 알려주고 Search 모듈이 주도적으로 필요한 재료를 가져오는(Pull) 것이 결합도를 낮춥니다.
3. **미래의 확장성 (Breaking Change 방지)**
   - 추후 추천 알고리즘이 고도화되어 '싫어요(👎) 누른 책', '최근 조회한 책' 등 다른 데이터가 추가로 필요해져도, `chatId`만 넘기는 구조에서는 Telegram 모듈의 코드나 API 스펙을 수정할 필요가 없습니다. Search 모듈 안에서 필요한 API 호출만 늘리면 되기 때문입니다.


### 1. 캐싱 및 API 호출 전담 서비스 만들기

이 서비스는  chatId 로 요청이 들어오면 먼저 Redis(또는 로컬 캐시)를 뒤져보고, 없으면 Telegram 모듈(API)을 찔러서 값을 가져온 뒤 캐시에 저장합니다.

    import org.springframework.cache.annotation.Cacheable;                                                                                                                                                                                                                                                                                 
    import org.springframework.stereotype.Service;                                                                                                                                                                                                                                                                                         
    import java.util.List;                                                                                                                                                                                                                                                                                                                 
    import lombok.RequiredArgsConstructor;                                                                                                                                                                                                                                                                                                 
                                                                                                                                                                                                                                                                                                                                           
    @Service                                                                                                                                                                                                                                                                                                                               
    @RequiredArgsConstructor                                                                                                                                                                                                                                                                                                               
    public class UserPreferenceService {                                                                                                                                                                                                                                                                                                   
                                                                                                                                                                                                                                                                                                                                           
        // 외부 API 호출용 Feign Client 또는 내부 Service 의존성                                                                                                                                                                                                                                                                           
        private final TelegramFeedbackClient telegramFeedbackClient;                                                                                                                                                                                                                                                                       
                                                                                                                                                                                                                                                                                                                                           
        /**                                                                                                                                                                                                                                                                                                                                
         * @Cacheable:                                                                                                                                                                                                                                                                                                                     
         * 1. 'likedBookIds'라는 캐시 공간에서 chatId를 키로 조회                                                                                                                                                                                                                                                                          
         * 2. 캐시에 데이터가 있으면 즉시 반환 (메서드 실행 안 됨)                                                                                                                                                                                                                                                                         
         * 3. 없으면 메서드를 실행(API 호출)하고, 그 리턴값을 캐시에 자동 저장!                                                                                                                                                                                                                                                            
         */                                                                                                                                                                                                                                                                                                                                
        @Cacheable(cacheNames = "likedBookIds", key = "#chatId")                                                                                                                                                                                                                                                                           
        public List<Long> getRecentLikedBookIds(Long chatId) {                                                                                                                                                                                                                                                                             
            // 캐시에 없을 때만 이 로그가 찍히고 API가 호출됩니다.                                                                                                                                                                                                                                                                         
            log.info("캐시 미스! Telegram API로부터 좋아요 목록을 조회합니다. chatId: {}", chatId);                                                                                                                                                                                                                                        
                                                                                                                                                                                                                                                                                                                                           
            // Telegram API 호출 (이전에 만든 FeedbackLikedBooksResponse 등을 활용)                                                                                                                                                                                                                                                        
            FeedbackLikedBooksResponse response = telegramFeedbackClient.getLikedBooks(chatId);                                                                                                                                                                                                                                            
                                                                                                                                                                                                                                                                                                                                           
            // 도서 ID 리스트 20개 추출해서 반환                                                                                                                                                                                                                                                                                           
            return response.getBookIds();                                                                                                                                                                                                                                                                                                  
        }                                                                                                                                                                                                                                                                                                                                  
    }                                                                                                                                                                                                                                                                                                                                      

(※ Redis를 사용하신다면 TTL 설정을 통해 24시간 후 자동 만료되도록 설정해주시면 됩니다.)

### 2.  BookRecommendationHandler  구현

이제 핸들러는 아주 가벼워집니다. 전담 서비스한테 캐싱된 ID 목록을 가져오라고 시키고, 그걸 바탕으로 검색 로직으로 넘기기만 하면 됩니다.

    import com.nhnacademy.springailibrarycore.telegram.domain.AssistantIntent;                                                                                                                                                                                                                                                             
    import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;                                                                                                                                                                                                                                                                    
    import com.nhnacademy.springailibrarycore.personalization.dto.PersonalizedSearchRequest;                                                                                                                                                                                                                                               
    import org.springframework.stereotype.Component;                                                                                                                                                                                                                                                                                       
    import java.util.List;                                                                                                                                                                                                                                                                                                                 
    import lombok.RequiredArgsConstructor;                                                                                                                                                                                                                                                                                                 
                                                                                                                                                                                                                                                                                                                                           
    @Component                                                                                                                                                                                                                                                                                                                             
    @RequiredArgsConstructor                                                                                                                                                                                                                                                                                                               
    public class BookRecommendationHandler implements IntentHandler {                                                                                                                                                                                                                                                                      
                                                                                                                                                                                                                                                                                                                                           
        private final UserPreferenceService userPreferenceService;                                                                                                                                                                                                                                                                         
        private final PersonalizedSearchService personalizedSearchService; // 실제 검색 모듈                                                                                                                                                                                                                                               
                                                                                                                                                                                                                                                                                                                                           
        @Override                                                                                                                                                                                                                                                                                                                          
        public boolean supports(AssistantIntent intent) {                                                                                                                                                                                                                                                                                  
            // Intent가 책 추천일 때 동작                                                                                                                                                                                                                                                                                                  
            return intent == AssistantIntent.BOOK_RECOMMENDATION;                                                                                                                                                                                                                                                                          
        }                                                                                                                                                                                                                                                                                                                                  
                                                                                                                                                                                                                                                                                                                                           
        @Override                                                                                                                                                                                                                                                                                                                          
        public List<AskResponse> handle(String keyword, Long chatId) {                                                                                                                                                                                                                                                                     
                                                                                                                                                                                                                                                                                                                                           
            // 1. 캐시 확인 및 Telegram API 호출 (UserPreferenceService가 다 알아서 해줌)                                                                                                                                                                                                                                                  
            List<Long> recentLikedBookIds = userPreferenceService.getRecentLikedBookIds(chatId);                                                                                                                                                                                                                                           
                                                                                                                                                                                                                                                                                                                                           
            // 2. 도서 ID 목록과 검색어를 묶어서 검색 모듈(Search)로 전달                                                                                                                                                                                                                                                                  
            // (이전에 만든 PersonalizedSearchRequest에 bookIds 필드도 추가해주면 좋습니다)                                                                                                                                                                                                                                                
            PersonalizedSearchRequest request = PersonalizedSearchRequest.builder()                                                                                                                                                                                                                                                        
                    .keyword(keyword)                                                                                                                                                                                                                                                                                                      
                    .chatId(chatId)                                                                                                                                                                                                                                                                                                        
                    .likedBookIds(recentLikedBookIds) // ✨ 획득한 취향 데이터 탑재                                                                                                                                                                                                                                                        
                    .build();                                                                                                                                                                                                                                                                                                              
                                                                                                                                                                                                                                                                                                                                           
            // 3. 개인화 가중치가 반영된 최종 도서 검색 결과 받기                                                                                                                                                                                                                                                                          
            List<BookSearchResponse> searchResults = personalizedSearchService.searchWithPersonalization(request);                                                                                                                                                                                                                         
                                                                                                                                                                                                                                                                                                                                           
            // 4. 결과를 텔레그램 화면용 AskResponse 리스트로 매핑해서 반환                                                                                                                                                                                                                                                                
            return mapToAskResponses(searchResults);                                                                                                                                                                                                                                                                                       
        }                                                                                                                                                                                                                                                                                                                                  
                                                                                                                                                                                                                                                                                                                                           
        // (매핑 헬퍼 메서드 생략)                                                                                                                                                                                                                                                                                                         
    }                                                                                                                                                                                                                                                                                                                                      
    ──────                                                                                                                                                                                                                                                                                                                                 
### 💡 요약

•  @Cacheable  애노테이션을 활용해 깔끔하게 캐시 로직을 분리합니다.                                                                                                                                                                                                                                                                      
•  BookRecommendationHandler 는 의도 파악 후 ① 취향 정보 가져오기 ➡️ ② 맞춤 검색 돌리기 ➡️ ③ UI 렌더링용으로 포맷팅하기라는 본연의 파이프라인 역할만 수행합니다.

(참고로 이 방식을 사용하려면 아까 만들어둔  PersonalizedSearchRequest  DTO에  List<Long> likedBookIds  필드를 하나 추가하는 것이 깔끔할 것 같습니다. 이 구조가 마음에 드시면 필드 추가부터 도와드릴까요?)                                                                                                                                
