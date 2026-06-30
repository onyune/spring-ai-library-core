package com.nhnacademy.springailibrarycore.library.service.agent;

import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.BookExistRequest;
import com.nhnacademy.springailibrarycore.library.dto.request.LibrarySearchRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.BookExistResponse.ResultInfo;
import com.nhnacademy.springailibrarycore.library.service.LibraryNaruService;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MultipleBookLoanCoordinator {
    private final RegionCodeAgent regionCodeAgent;
    private final DetailRegionCodeAgent detailRegionCodeAgent;
    private final BookIsbnAgent bookIsbnAgent;
    private final LibraryNaruService libraryNaruService;
    private final BookCheckExitAgent bookCheckExitAgent;
    private final LibraryCodeAgent libraryCodeAgent;
    private final Executor taskExecutor;

    public MultipleBookLoanCoordinator(RegionCodeAgent regionCodeAgent, DetailRegionCodeAgent detailRegionCodeAgent,
                                       BookIsbnAgent bookIsbnAgent, LibraryNaruService libraryNaruService,
                                       BookCheckExitAgent bookCheckExitAgent, LibraryCodeAgent libraryCodeAgent,
                                       @Qualifier("applicationTaskExecutor")Executor taskExecutor) {
        this.regionCodeAgent = regionCodeAgent;
        this.detailRegionCodeAgent = detailRegionCodeAgent;
        this.bookIsbnAgent = bookIsbnAgent;
        this.libraryNaruService = libraryNaruService;
        this.bookCheckExitAgent = bookCheckExitAgent;
        this.libraryCodeAgent = libraryCodeAgent;
        this.taskExecutor = taskExecutor;
    }


    public String checkMultipleBooksAvailability(List<String> bookTitles, String libName, String regionName, String dtlRegionName) {
        List<LibraryInfo> targetLibraries = List.of();
        if(libName != null && !libName.isBlank()) {
            String resolvedLibCode = libraryCodeAgent.getLibraryCode(libName);

            if(resolvedLibCode != null) {
                targetLibraries = List.of(LibraryInfo.builder()
                        .libCode(resolvedLibCode)
                        .libName(libName.trim())
                        .build());
                log.info("[MultipleBookLoan] 특정 도서관 조회 타겟 설정: {} ({})", libName, resolvedLibCode);
            }
        }

        if(targetLibraries.isEmpty() && regionName != null && !regionName.isBlank()) {
            Integer rCode = null;
            String cleanRegion = regionName.trim();
            if (cleanRegion.matches("\\d{2}")) {
                rCode = Integer.parseInt(cleanRegion);
            } else {
                rCode = regionCodeAgent.getRegionCode(cleanRegion);
            }
            Integer dtlCode = null;
            if (dtlRegionName != null) {
                String cleanDtl = dtlRegionName.trim();
                if (cleanDtl.matches("\\d{5}")) {
                    dtlCode = Integer.parseInt(cleanDtl);
                } else {
                    dtlCode = detailRegionCodeAgent.getDetailRegionCode(cleanDtl, rCode);
                }
            }
            if (rCode != null) {
                log.info("[MultipleBookLoan] 지역 기준 도서관 검색: regionCode={}, dtlRegionCode={}", rCode, dtlCode);
                targetLibraries = libraryNaruService.getLibraries(
                        LibrarySearchRequest.builder()
                                .region(rCode)
                                .dtlRegion(dtlCode)
                                .pageSize(10)
                                .build()
                );
            }
        }
        if (targetLibraries.isEmpty()) {
            return "조회 대상 도서관 명칭 또는 유효한 지역 정보를 입력해주세요.";
        }

        StringBuilder report = new StringBuilder();
        String targetDisplay = (libName != null && !libName.isBlank()) ? libName : (dtlRegionName != null ? (regionName + " " + dtlRegionName) : regionName);
        report.append(String.format("📍 **[%s] 도서별 대출 가능 현황 조회 결과:**\n\n", targetDisplay));

        // 각 책 제목별로 병렬 소장 확인 진행
        for (String title : bookTitles) {
            String trimmedTitle = title.trim();
            report.append(String.format("📖 **[%s]**\n", trimmedTitle));

            String isbn13 = bookIsbnAgent.getIsbn(trimmedTitle);
            if (isbn13 == null || isbn13.isBlank()) {
                report.append("  - ⚠️ 도서 ISBN을 조회할 수 없어 확인 불가\n\n");
                continue;
            }

            // 병렬 조회 (CompletableFuture)
            List<CompletableFuture<String>> futures = targetLibraries.stream()
                    .map(lib -> CompletableFuture.supplyAsync(() -> {
                        try {
                            BookExistRequest existRequest = BookExistRequest.builder()
                                    .libCode(lib.libCode())
                                    .isbn13(isbn13)
                                    .build();

                            ResultInfo result = bookCheckExitAgent.checkBook(existRequest);

                            if (result.isLoanAvailable()) {
                                return String.format("  - 🟢 **%s** (즉시 대출 가능)", lib.libName());
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                        return null;
                    }, taskExecutor))
                    .toList();

            List<String> availableLibs = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            if (availableLibs.isEmpty()) {
                report.append("  - 🔴 현재 대출 가능한 도서관이 없습니다.\n");
            } else {
                availableLibs.forEach(line -> report.append(line).append("\n"));
            }
            report.append("\n");
        }

        return report.toString();
    }
}
