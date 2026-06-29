package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.PopularBooksSearchRequest;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PopularBookSearchAgent {
    private final LibraryNaruService libraryNaruService;

    public List<NaruBookInfo> searchPopularBook(PopularBooksSearchRequest request) {
        return libraryNaruService.getPopularBooks(request);
    }
}
