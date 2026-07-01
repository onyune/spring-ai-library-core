package com.nhnacademy.springailibrarycore.telegram.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 에이전트의 툴 실행 결과들을 스레드 로컬(ThreadLocal)에 보관하는 컨텍스트 홀더입니다.
 * <p>
 * Tomcat 스레드 풀 환경에서 메모리 누수나 데이터 오염을 예방하기 위해,
 * 요청 처리 완료 시 반드시 {@code clear()}를 호출해야 합니다.
 */
public class ToolResultHolder {

    private static final ThreadLocal<List<Object>> holder = ThreadLocal.withInitial(ArrayList::new);

    public static void add(Object result) {
        if (result != null) {
            holder.get().add(result);
        }
    }

    public static List<Object> getResults() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
