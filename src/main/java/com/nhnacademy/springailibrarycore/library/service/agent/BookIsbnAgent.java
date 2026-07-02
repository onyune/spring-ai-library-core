package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.BookSearchRequest;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookIsbnAgent {

    private final BookRepository bookRepository;
    private final LibraryNaruService libraryNaruService;

    public String getIsbn(String bookTitle) {
        if(bookTitle == null || bookTitle.isBlank()) {
            throw new IllegalArgumentException("도서 제목을 입력해주세요.");
        }

        String trimTitle = bookTitle.trim();

        // 입력받은 값이 이미 하이픈을 제외했을 때 10자리 혹은 13자리 숫자(ISBN)인 경우, 조회 없이 즉시 반환
        String cleanIsbn = trimTitle.replaceAll("-", "");
        if (cleanIsbn.matches("\\d{10}|\\d{13}")) {
            log.info("[BookIsbnAgent] 입력된 값 자체가 유효한 ISBN 식별자입니다: {}", cleanIsbn);
            return cleanIsbn;
        }

        Optional<Book> dbBook  = bookRepository.findFirstByTitleContaining(trimTitle);

        if(dbBook.isPresent() && dbBook.get().getIsbn() != null && !dbBook.get().getIsbn().isBlank()) {
            log.info("[BookIsbnAgent] 내부 DB에서 ISBN 조회 성공: {} -> {}", trimTitle, dbBook.get().getIsbn());
            return dbBook.get().getIsbn();
        }

        log.info("[BookIsbnAgent] DB 미스로 인해 외부 정보나루 API 검색을 시도합니다: {}", trimTitle);
        try {
            BookSearchRequest apiRequest = BookSearchRequest.builder()
                    .title(trimTitle)
                    .pageSize(1)
                    .build();

            List<NaruBookInfo> apiResults = libraryNaruService.searchBooks(apiRequest);

            if(apiResults != null && !apiResults.isEmpty()) {
                NaruBookInfo firstBook = apiResults.getFirst();

                String isbn = (firstBook.isbn13()) != null && !firstBook.isbn13().isBlank()
                ? firstBook.isbn13() : firstBook.isbn();

                if(isbn != null && isbn.isBlank()) {
                    log.info("[BookIsbnAgent] 외부 API에서 ISBN 조회 성공: {} -> {}", trimTitle, isbn);
                    return isbn;
                }
            }
        } catch (Exception e) {
            log.error("[BookIsbnAgent] 외부 API 조회 중 예외 발생: {}", trimTitle, e);
        }
        log.warn("[BookIsbnAgent] 해당 도서의 ISBN을 찾을 수 없습니다: {}", trimTitle);
        return null;
    }
}
