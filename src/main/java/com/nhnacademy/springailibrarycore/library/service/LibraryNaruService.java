package com.nhnacademy.springailibrarycore.library.service;

import com.nhnacademy.springailibrarycore.config.Data4LibraryApiProperties;
import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse;
import com.nhnacademy.springailibrarycore.library.dto.LibrarySearchResponse.LibraryInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruResponse;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruWrapper;
import com.nhnacademy.springailibrarycore.library.dto.request.*;
import com.nhnacademy.springailibrarycore.library.dto.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * 도서관 정보나루 (data4library.kr) 오픈 API 연동을 담당하는 비즈니스 서비스
 * 모든 API 요청에 대해 JSON 데이터 포맷을 강제
 * 공통 제네릭 클래스({@link NaruResponse}, {@link NaruWrapper})를 통해 언래핑
 *
 */
@Service
@Slf4j
public class LibraryNaruService {

    private final RestClient restClient;
    private final Data4LibraryApiProperties properties;

    public LibraryNaruService(RestClient.Builder restClientBuilder, Data4LibraryApiProperties properties) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    /**
     * 정보공개 도서관 조회 API를 호출하여 도서관 목록을 반환합니다.

     * 요청 DTO에{@link LibrarySearchRequest} libCode가 있으면 해당 도서관의 정보만 단일 조회하여 반환
     * libCode가 없으면 region(시도) 및 dtl_region}(시군구) 필터 조건을 기반으로 전체 목록 조회 수행
     * 지역 조건이 모두 생략되면 전국 도서관을 대상으로 페이지 번호와 페이지 크기에 맞춰 페이징 조회
     *
     * @param request 도서관 검색 조건 DTO (libCode, region, dtl_region 등)
     * @return 주소, 연락처, 운영시간, 좌표, 단행본수가 포함된 도서관 정보 리스트
     */
    public List<LibraryInfo> getLibraries(LibrarySearchRequest request) {
        var typeRef = new ParameterizedTypeReference<NaruResponse<NaruWrapper<LibraryInfo>>>() {};
        NaruResponse<NaruWrapper<LibraryInfo>> response = fetchFromApi("libSrch", request.toQueryParams(), typeRef);

        if (response == null || response.response() == null) {
            return List.of();
        }
        return response.response().getItems().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }

    /**
     * 전국 도서관 인기대출 도서 조회 API를 호출하여 도서 리스트를 반환
     * startDt와 endDt 기간 설정이 생략된 경우, 요청 시점 기준 최근 한 달간의 대출 데이터를 기본 범위로 자동 지정
     * gender}(성별), age(연령대), region}(지역), addCode(ISBN 부가기호), kdc(주제분류) 등의 코드는 세미콜론(;)을 구분자로 사용하여 다중 선택 가능 (예: "11;31" 입력 시 서울과 경기 지역 조건 다중 검색)</li>
     * 연령 조건의 경우 상세 연령 범위 (from_age ~ to_age})를 지정하거나 대표 연령대 코드 age, (예: 20) 중 하나를 선택해 적용 가능
     *
     * @param request 인기대출 도서 조건 DTO
     * @return 대출 순위 및 대출 횟수가 포함된 통합 도서 정보 리스트
     */
    public List<NaruBookInfo> getPopularBooks(PopularBooksSearchRequest request) {
        var typeRef = new ParameterizedTypeReference<NaruResponse<NaruWrapper<NaruBookInfo>>>() {};
        NaruResponse<NaruWrapper<NaruBookInfo>> response = fetchFromApi("loanItemSrch", request.toQueryParams(), typeRef);

        if (response == null || response.response() == null) {
            return List.of();
        }
        return response.response().getItems().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }

    /**
     * 특정 도서와 함께 대출될 확률이 높은 마니아 추천 도서 조회 API를 호출합니다.
     * 입력받은 isbn13 도서의 대출이력 기반 조건부 확률 분석을 통해 추천을 생성
     * isbn13은 세미콜론(;)으로 구분하여 최대 5개까지 다중 지정 가능, 5개 초과 시 처음 5개 도서 조건으로만 결과 제공
     * 추천 도서는 조건부 확률이 가장 높은 순으로 최대 200건까지 조회
     *
     * @param request 추천 도서 검색 조건 DTO (isbn13, type="mania")
     * @return 추천 순번이 매겨진 도서 목록
     */
    public List<NaruBookInfo> getManiaRecommendations(ManiaRecommendationRequest request) {
        var typeRef = new ParameterizedTypeReference<NaruResponse<NaruWrapper<NaruBookInfo>>>() {};
        NaruResponse<NaruWrapper<NaruBookInfo>> response = fetchFromApi("recommandList", request.toQueryParams(), typeRef);

        if (response == null || response.response() == null) {
            return List.of();
        }
        return response.response().getItems().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }

    /**
     * 도서 상세 서지정보 및 통계 조회 API를 호출합니다.
     * 특정 도서 isbn13의 상세 정보(책소개, 표지 URL, 권수 등)를 불러옴
     * loaninfoYN="Y" 옵션을 추가하면 최근 90일 기준 도서관 빅데이터 참여 도서관의 성별/지역별/연령대별 상위 1,000위권 대출 순위 분포 및 건수 통계를 추가로 반환
     * displayInfo 파라미터를 추가하여 특정 인구통계 항목(예: regionResult)만 선택해서 수집 가능
     *
     * @param request 상세조회 요청 DTO
     * @return 서지 상세 데이터 및 요약된 인구통계별 대출통계 정보
     */
    public NaruBookDetailResponse.ResponseData getBookDetail(BookDetailRequest request) {
        NaruBookDetailResponse response = fetchFromApi("srchDtlList", request.toQueryParams(), NaruBookDetailResponse.class);
        return (response != null) ? response.response() : null;
    }

    /**
     * 특정 도서의 이용 분석 및 연계 분석 데이터 조회 API를 호출합니다.
     *
     * 특정 도서 isbn13 에 대해 총 대출 건수, 최근 12개월간의 월별 대출 추이 및 순위 변동 이력 조회
     * 최근 30일간 해당 도서를 가장 많이 빌려간 주 이용층 정보(loanGrps, 예: 40대 여성)와 가중치가 적용된 핵심 단어 목록(keywords)을 받아옴
     * 최근 36개월간 동시 대출 빈도를 분석한 동시대출 도서 목록 및 마니아/다독자 조건부 확률 추천 목록(최대 10권씩)도 통합되어 반환
     * </ul>
     * </p>
     *
     * @param request 이용분석 요청 DTO
     * @return 대출이력, 키워드, 동시대출 도서 및 추천 도서 목록이 통합된 상세 분석 데이터
     */
    public NaruBookUsageAnalysisResponse.ResponseData getBookUsageAnalysis(BookUsageAnalysisRequest request) {
        NaruBookUsageAnalysisResponse response = fetchFromApi("usageAnalysisList", request.toQueryParams(), NaruBookUsageAnalysisResponse.class);
        return (response != null) ? response.response() : null;
    }

    /**
     * 특정 도서관 또는 특정 행정 구역 기준 인기대출 도서 조회 API를 호출합니다.
     *
     * 필수 조건: libCode(도서관), region(시도), dtl_region(시군구) 중 최소 하나는 반드시 필수
     * 선택된 도서관/지역 범위 내에서 성별, 연령대, ISBN 부가기호, 주제 대/소분류 필터를 조합하여 대출 랭킹을 집계
     * 지역/도서관 조건 및 코드는 세미콜론(;)을 구분자로 한 다중값 지정이 동일하게 지원, 최대 200건의 목록이 제공\
     *
     * @param request 도서관/지역별 인기 대출 조건 DTO
     * @return 순위 및 도서관 정보가 포함된 도서 목록
     */
    public List<NaruBookInfo> getLibPopularBooks(LibPopularBooksRequest request) {
        LibPopularBooksResponse response = fetchFromApi("loanItemSrchByLib", request.toQueryParams(), LibPopularBooksResponse.class);
        if (response == null || response.response() == null || response.response().docs() == null) {
            return List.of();
        }
        return response.response().docs().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }

    /**
     * 특정 도서관에 대상 도서가 소장되어 있는지와 현재 대출이 가능한지 여부를 조회
     * 대상 도서관 코드(libCode)와 도서 번호(isbn13)를 지정하여 실행
     * 도서의 단순 보유 여부인 hasBook 과 조회일 기준 바로 전날 상태를 반영한 대출 가용성 여부인 loanAvailable이 각각 Y/N 문자로 반환
     *
     * @param request 소장/대출 여부 확인 조건 DTO
     * @return 소장 여부(Y/N) 및 대출 가능 여부(Y/N) 정보
     */
    public BookExistResponse.ResultInfo checkBookExists(BookExistRequest request) {
        BookExistResponse response = fetchFromApi("bookExist", request.toQueryParams(), BookExistResponse.class);
        return (response != null && response.response() != null) ? response.response().result() : null;
    }



    //이건 안쓸듯
    /**
     * 도서관정보나루 빅데이터 데이터베이스를 기반으로 도서를 검색
     * 도서명(title), 저자명(author), 출판사(publisher), ISBN(isbn13), 키워드(keyword) 조건을 AND 연산으로 조합해 검색
     * keyword는 세미콜론(;)을 구분자로 다중 키워드 조건 조회가 지원됩니다.</li>
     * 검색 결과 정렬 기준(sort: 도서명, 저자명, 출판사, 대출건수 등)과 정렬 순서(order: 오름차순/내림차순) 및 완전 일치 여부(exactMatch)를 추가로 제어 가능
     *
     * @param request 도서 검색 파라미터 DTO
     * @return 검색 일치도와 대출건수가 반영된 도서 리스트
     */
    public List<NaruBookInfo> searchBooks(BookSearchRequest request) {
        var typeRef = new ParameterizedTypeReference<NaruResponse<NaruWrapper<NaruBookInfo>>>() {};
        NaruResponse<NaruWrapper<NaruBookInfo>> response = fetchFromApi("srchBooks", request.toQueryParams(), typeRef);

        if (response == null || response.response() == null) {
            return List.of();
        }
        return response.response().getItems().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }

    /**
     * 특정 월의 대출급상승 도서 정보 분석에서 도출된 이달의 핵심 키워드 목록을 조회합니다.
     *
     * 원하는 연월(month, yyyy-MM)을 인자로 전달 미지정 시에는 조회일 기준 직전월의 분석 데이터를 제공
     * 도서소개글 및 서평 데이터에서 추출한 명사 단어에 TF-IDF 가중치를 적용하여 순위를 매긴 핵심 키워드를 최대 100건 반환
     *
     * @param request 키워드 검색 조건 DTO (month)
     * @return 키워드 단어와 가중치 정보 리스트
     */
    public List<MonthlyKeywordResponse.KeywordInfo> getMonthlyKeywords(MonthlyKeywordRequest request) {
        MonthlyKeywordResponse response = fetchFromApi("monthlyKeywords", request.toQueryParams(), MonthlyKeywordResponse.class);
        if (response == null || response.response() == null || response.response().keywords() == null) {
            return List.of();
        }
        return response.response().keywords().stream()
                .map(MonthlyKeywordResponse.KeywordWrapper::keyword)
                .toList();
    }

    /**
     * 특정 도서관의 최근 등록일 기준 신도서(신착 도서) 목록을 조회합니다.
     * 소속 도서관 코드(libCode)와 검색 연월(searchDt, yyyy-MM)을 사용하여 조회
     * searchDt가 생략되면 조회 당일의 연월이 디폴트로 지정
     * 가장 최근 등록된 신규 도서 리스트를 페이지 규격에 맞게 수집하여 반환
     *
     * @param request 신착도서 조건 DTO
     * @return 등록일과 세부 정보가 매핑된 신간 도서 리스트
     */
    public List<NaruBookInfo> getNewArrivalBooks(NewArrivalBookRequest request) {
        var typeRef = new ParameterizedTypeReference<NaruResponse<NaruWrapper<NaruBookInfo>>>() {};
        NaruResponse<NaruWrapper<NaruBookInfo>> response = fetchFromApi("newArrivalBook", request.toQueryParams(), typeRef);

        if (response == null || response.response() == null) {
            return List.of();
        }
        return response.response().getItems().stream()
                .map(NaruWrapper::getContent)
                .toList();
    }


    /**
     * 제네릭 타입 파싱(TypeReference)이 필요한 공통 리스트 API 처리용 메서드
     */
    private <T> T fetchFromApi(String endPoint, MultiValueMap<String, String> queryParams, ParameterizedTypeReference<T> responseType) {
        String url = UriComponentsBuilder.fromUriString(properties.getUrl() + endPoint)
                .queryParam("authKey", properties.getServiceKey())
                .queryParam("format", "json") // 항상 JSON 응답 강제
                .queryParams(queryParams)
                .build(true)
                .toUriString();

        log.info("[NaruService] Generic API Call: {}", url);

        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(responseType);
        } catch (Exception e) {
            log.error("[NaruService] API Call Failed -> path: {}, params: {}", endPoint, queryParams, e);
            throw new RuntimeException("정보나루 API 통신 중 예외가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 단일 클래스(Class) 매핑이 필요한 독자 구조 API 처리용 메서드
     */
    private <T> T fetchFromApi(String endPoint, MultiValueMap<String, String> queryParams, Class<T> responseType) {
        String url = UriComponentsBuilder.fromUriString(properties.getUrl() + endPoint)
                .queryParam("authKey", properties.getServiceKey())
                .queryParam("format", "json") // 항상 JSON 응답 강제
                .queryParams(queryParams)
                .build(true)
                .toUriString();

        log.info("[NaruService] Standard API Call: {}", url);

        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(responseType);
        } catch (Exception e) {
            log.error("[NaruService] API Call Failed -> path: {}, params: {}", endPoint, queryParams, e);
            throw new RuntimeException("정보나루 API 통신 중 예외가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
