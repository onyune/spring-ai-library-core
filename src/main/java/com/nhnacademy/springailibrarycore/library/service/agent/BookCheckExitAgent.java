package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.BookExistRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.BookExistResponse.ResultInfo;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookCheckExitAgent {
    private final LibraryNaruService libraryNaruService;

    public ResultInfo checkBook(BookExistRequest request) {
        return libraryNaruService.checkBookExists(request);
    }

}
