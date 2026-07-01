package com.nhnacademy.springailibrarycore.book.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PageImpl 역직렬화 이슈를 방지하기 위해 검색 결과를 캐싱하고 전달할 때 사용하는 DTO 레코드입니다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookSearchPageResult implements Serializable {

    private List<BookSearchResponse> content;
    private long totalElements;

    public static BookSearchPageResult paginate(List<BookSearchResponse> list, org.springframework.data.domain.Pageable pageable) {
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= list.size()) {
            return new BookSearchPageResult(List.of(), list.size());
        }
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new BookSearchPageResult(new ArrayList<>(list.subList(start, end)), list.size());
    }
}
