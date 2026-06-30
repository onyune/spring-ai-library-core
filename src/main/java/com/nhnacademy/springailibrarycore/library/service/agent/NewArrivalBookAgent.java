package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.request.NewArrivalBookRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.NewArrivalBooksResponse;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NewArrivalBookAgent {

    private final LibraryNaruService libraryNaruService;

    public NewArrivalBooksResponse.ResponseData searchNewArrivalBook(NewArrivalBookRequest request){
        return  libraryNaruService.getNewArrivalBooks(request);
    }
}
