package com.nhnacademy.springailibrarycore.front.controller;

import com.nhnacademy.springailibrarycore.book.dto.BookDetailResponse;
import com.nhnacademy.springailibrarycore.book.service.BookService;
import com.nhnacademy.springailibrarycore.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
@Slf4j
public class BookController {

    private final BookService bookService;
    private final ReviewService reviewService;

    /**
     * 특정 도서의 상세 정보와 리뷰 목록(페이징)을 반환하는 입니다.
     *
     * @param id       도서 ID
     * @param pageable 리뷰 목록 페이징 설정 (기본값: 최신등록순 5개)
     * @return BookDetailResponse 도서 상세 및 리뷰 정보 DTO
     */
    @GetMapping("/{id}")
    public String getBookDetail(
            @PathVariable("id") Long id,
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model
    ) {
        log.info("[BookController] 도서 상세 조회 요청: {}, page: {}", id, pageable.getPageNumber());
        BookDetailResponse bookDetail = bookService.getBookDetail(id, pageable);
        model.addAttribute("bookDetail", bookDetail);
        return "book-detail";
    }

    /**
     * 리뷰 작성 기능 (임시 더미 엔드포인트)
     */
    @PostMapping("/{id}/reviews")
    public String writeReviewDummy(
            @PathVariable("id") Long id,
            @RequestParam("rating") int rating,
            @RequestParam("content") String content,
            RedirectAttributes redirectAttributes
    ) {
        log.info("[BookController] 리뷰 등록 요청 (DUMMY): bookId={}, rating={}, content={}", id, rating, content);
        // 실제 저장은 지원하지 않으므로 플래시 속성에 오류/안내 메시지를 담아 리다이렉트
        redirectAttributes.addFlashAttribute("message", "현재 리뷰 등록 기능은 준비 중입니다. 조만간 만나보실 수 있습니다!");
        return "redirect:/books/" + id;
    }
}


