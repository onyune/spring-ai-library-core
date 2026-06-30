package com.nhnacademy.springailibrarycore.library.service.agent;


import com.nhnacademy.springailibrarycore.library.dto.request.LibPopularBooksRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.LibPopularBooksResponse;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class LibPopularBookAgent {
    private final LibraryNaruService libraryNaruService;

    public LibPopularBooksResponse.ResponseData searchLibPopularBook(LibPopularBooksRequest request) {
        return libraryNaruService.getLibPopularBooks(request);
    }
}
