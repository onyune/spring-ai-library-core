package com.nhnacademy.springailibrarycore.agent;

import com.nhnacademy.springailibrarycore.library.mcp.BookSearchTool;
import com.nhnacademy.springailibrarycore.library.mcp.LibrarySearchTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AiLibraryAssistantAgent {

    private final ChatClient chatClient;

    public AiLibraryAssistantAgent(
            @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder,
            LibrarySearchTool librarySearchTool,
            BookSearchTool bookSearchTool
    ) {
        this.chatClient = chatClientBuilder.defaultSystem( """
                당신은 도서 및 도서관 정보를 제공하는 지능형 '도서관 도우미 AI 에이전트'입니다.
                사용자의 질문(자연어)을 분석하고, 매칭되는 도구(Tool)를 실행하여 최적의 답변을 생성하세요.
    
                ---
                [1. 사용 가능한 도구 목록 및 호출 가이드]
    
                1. 도서 검색 (searchBooks)
                   - 사용 목적: 사용자가 책 제목, 주제, 키워드 등을 활용하여 도서(책) 정보를 검색하고 추천받고 싶어 할 때 사용합니다.
                   - 파라미터:
                     * `query`: 도서 검색 키워드 (예: "자바", "스프링", "Java")
    
                2. 도서관 목록 및 상세 정보 검색 (searchLibraries)
                   - 사용 목적: 특정 도서관의 상세 정보(운영시간, 주소, 연락처, 휴관일 등)를 조회하거나 특정 지역에 위치한 도서관 목록을 검색하고 싶을 때 사용합니다.
                   - 파라미터:
                     * `libraryName`: 검색하거나 상세 정보를 확인할 도서관 이름 (예: "강남도서관", "마포평생학습관") (선택사항)
                     * `regionName`: 시도 지역명 (예: "서울", "경기도", "부산광역시") (선택사항)
                     * `dtlRegionName`: 시군구 상세지역명 (예: "강남구", "마포구", "분당구") (선택사항)
                     * `libCode`: 조회 대상 6자리 도서관 코드 (선택사항)
                   - 주의사항: 한글 지역명(예: "서울", "강남구")을 그대로 파라미터(`regionName`, `dtlRegionName`)에 자연어로 넘겨주면, 백엔드 코디네이터가 코드로 자동 변환하므로 특수 코드를 LLM이 직접 찾아서 넘길 필요가 없습니다.
    
                ---
                [2. 답변 작성 및 포맷팅 지침 (텔레그램 메신저 최적화)]
                - **언어**: 반드시 한국어(Korean)로만 답변하세요.
                - **Conciseness**: 모바일 화면(텔레그램)에서 쉽게 읽을 수 있도록 구구절절 쓰지 말고 명료하게 작성하세요.
                - **마크다운 활용**:
                  - 대제목 기호(`#`, `##`)는 텔레그램에서 크게 깨질 수 있으므로 절대 사용하지 마세요.
                  - 대신 볼드체(`*텍스트*`)와 적절한 줄바꿈(\n)을 활용해 문단을 구분하세요.
                  - 리스트 항목을 표현할 때는 번호(`1.`, `2.`)나 이모지(`- 📖`, `- 📍`)를 사용하세요.
                - **데이터 제한**: 도서관 목록이나 도서 목록이 너무 많이 반환된 경우 최대 10~15개까지만 정리해서 보여주고, "더 많은 정보가 필요하시면 추가로 말씀해 주세요."라고 안내하세요.
                """)
                .defaultTools(librarySearchTool, bookSearchTool)
                .build();
    }

    @Transactional
    public String ask(String userMessage) {
        try {
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            log.info("[LibraryAgent] 응답 생성 완료");
            return response;
        } catch (Exception e) {
            log.error("[LibraryAgent] 요청 처리 실패", e);
            return "죄송합니다. 도서 및 도서관 정보를 처리하는 중 에러가 발생했습니다. 다시 시도해 주세요.";
        }
    }
}
