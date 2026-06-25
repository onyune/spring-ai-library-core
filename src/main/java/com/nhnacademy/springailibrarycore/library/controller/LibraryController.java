package com.nhnacademy.springailibrarycore.library.controller;

import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse;
import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.LibrarySearchRequest;
import com.nhnacademy.springailibrarycore.library.dto.request.PopularBooksSearchRequest;
import com.nhnacademy.springailibrarycore.library.dto.request.ManiaRecommendationRequest;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
@RequiredArgsConstructor
@Slf4j
public class LibraryController {

    private final LibraryNaruService libraryNaruService;

    @GetMapping
    public ResponseEntity<List<LibraryInfo>> getLibraries(LibrarySearchRequest request) {
        log.info("[LibraryController] 도서관 목록 조회 요청: {}", request);
        List<LibraryInfo> libraries = libraryNaruService.getLibraries(request);
        return ResponseEntity.ok(libraries);
    }

    @GetMapping("/popular")
    public ResponseEntity<List<NaruBookInfo>> getPopularBooks(PopularBooksSearchRequest request) {
        log.info("[LibraryController] 인기 대출 도서 조회 요청: {}", request);
        List<NaruBookInfo> popularBooks = libraryNaruService.getPopularBooks(request);
        return ResponseEntity.ok(popularBooks);
    }

    @GetMapping("/recommendations/mania")
    public ResponseEntity<List<NaruBookInfo>> getManiaRecommendations(ManiaRecommendationRequest request) {
        log.info("[LibraryController] 마니아 추천 도서 조회 요청: {}", request);
        List<NaruBookInfo> recommendations = libraryNaruService.getManiaRecommendations(request);
        return ResponseEntity.ok(recommendations);
    }
}
