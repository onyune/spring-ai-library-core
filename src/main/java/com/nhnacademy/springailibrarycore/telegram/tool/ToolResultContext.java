package com.nhnacademy.springailibrarycore.telegram.tool;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * HTTP 요청 수명 주기와 매핑되어 툴 실행의 날것(Raw) 데이터를 보관하는 컨텍스트 클래스입니다.
 * 스프링이 요청 완료 시 자동으로 스레드 맵에서 리소스를 해제하므로 메모리 누수에 완전히 안전합니다.
 */
@Component
@RequestScope
public class ToolResultContext {

    private final List<Object> results = new ArrayList<>();

    /**
     * 실행된 툴의 결과를 추가합니다.
     */
    public void addResult(Object result) {
        if (result != null) {
            this.results.add(result);
        }
    }

    /**
     * 임시 저장된 툴 실행 결과 리스트를 반환합니다.
     * 방어적 복사(List.copyOf)를 통해 불변성을 유지합니다.
     */
    public List<Object> getResults() {
        return List.copyOf(this.results);
    }
}
