package com.nhnacademy.springailibrarycore.front.controller;

import com.nhnacademy.springailibrarycore.agent.BookSearchAgent;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResult;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class BookSearchController {
    private final BookSearchAgent bookSearchAgent;

    @GetMapping("/")
    public String index(@ModelAttribute("request") BookSearchRequest bookSearchRequest) {

        return "index";
    }

    @PostMapping("/search")
    public String searchBook(@ModelAttribute BookSearchRequest bookSearchRequest,
                             @PageableDefault(size = 24)Pageable pageable,
                             Model model){
        long startedAt = System.nanoTime();
        log.info("[BookSearchController] - 사용자 검색 검색어: {}, 검색 방식: {}", bookSearchRequest.keyword(), bookSearchRequest.searchType());

        BookSearchResult result = bookSearchAgent.searchBooks(pageable, bookSearchRequest);
        double searchTimeSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;

        model.addAttribute("books", result.books().getContent());
        model.addAttribute("page", result.books());
        model.addAttribute("request", bookSearchRequest);
        model.addAttribute("searchTime", String.format("%.3f", searchTimeSeconds));
        model.addAttribute("pageNumbers", createPageNumbers(
                result.books().getNumber(),
                result.books().getTotalPages()
        ));
        return "index";
    }

    private List<Integer> createPageNumbers(int currentPage, int totalPages) {
        if (totalPages == 0) {
            return List.of();
        }

        int startPage = Math.max(0, currentPage - 2);
        int endPage = Math.min(totalPages - 1, currentPage + 2);

        return IntStream.rangeClosed(startPage, endPage)
                .boxed()
                .toList();
    }
}
