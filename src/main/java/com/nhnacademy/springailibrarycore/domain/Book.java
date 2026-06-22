package com.nhnacademy.springailibrarycore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "books",
        indexes = {
            @Index(name = "idx_book_isbn", columnList = "isbn", unique = true)
        }
)

@Getter
@NoArgsConstructor
@ToString
public class Book {

    /**
     * DB Primary Key (시스템 자동 생성 식별자)
     */
    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "book_sequence_generator"
    )

    @SequenceGenerator(
            name = "book_sequence_generator",
            sequenceName = "book_sequence",
            allocationSize = 1000
    )
    @Column(name = "id")
    private Long id;

    /**
     * 13자리 국제표준도서번호
     * [CSV 매칭: ISBN_THIRTEEN_NO]
     */
    @Column(name = "isbn", length = 13, unique = true, nullable = false)
    private String isbn;

    /**
     * 원본 데이터 일련번호
     * [CSV 매칭: SEQ_NO]
     */
    @Column(name = "source_sequence_no")
    private Long sourceSequenceNo;

    /**
     * 권 명 (도서의 부속 권 제목)
     * [CSV 매칭: VLM_NM]
     */
    @Column(name = "volume_title")
    private String volumeTitle;

    /**
     * 도서 명 (제목)
     * [CSV 매칭: TITLE_NM]
     */
    @Column(name = "title", length = 500, nullable = false)
    private String title;

    /**
     * 저자 명
     * [CSV 매칭: AUTHR_NM]
     */
    @Column(name = "author_name", length = 1000)
    private String authorName;

    /**
     * 출판사 명
     * [CSV 매칭: PUBLISHER_NM]
     */
    @Column(name = "publisher_name")
    private String publisherName;

    /**
     * 최초 발행일 (출판일)
     * [CSV 매칭: PBLICTE_DE]
     */
    @Column(name = "first_publish_date")
    private LocalDate firstPublishDate;

    /**
     * 부가기호 명 (도서관 분류/유형 기호)
     * [CSV 매칭: ADTION_SMBL_NM]
     */
    @Column(name = "additional_symbol_name", length = 50)
    private String additionalSymbolName;

    /**
     * 가격 값
     * [CSV 매칭: PRC_VALUE]
     */
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 도서 표지 이미지 URL
     * [CSV 매칭: IMAGE_URL]
     */
    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /**
     * 도서 소개 내용
     * [CSV 매칭: BOOK_INTRCN_CN]
     */
    @Column(name = "book_content", columnDefinition = "TEXT")
    private String bookContent;

    /**
     * 한국십진분류법(KDC) 명칭
     * [CSV 매칭: KDC_NM]
     */
    @Column(name = "kdc_name", length = 50)
    private String kdcName;

    /**
     * 부제목 명
     * [CSV 매칭: TITLE_SBST_NM]
     */
    @Column(name = "subtitle", length = 500)
    private String subtitle;

    /**
     * 저자 소개/설명 정보
     * [CSV 매칭: AUTHR_SBST_NM]
     */
    @Column(name = "author_substance_name", columnDefinition = "TEXT")
    private String authorSubstanceName;

    /**
     * 개정판/재판 발행일
     * [CSV 매칭: TWO_PBLICTE_DE]
     */
    @Column(name = "edition_publish_date")
    private LocalDate editionPublishDate;

    /**
     * 인터넷 서점 도서 존재 여부 (판매 여부)
     * [CSV 매칭: INTNT_BOOKST_BOOK_EXST_AT]
     */
    @Column(name = "internet_bookstore_book_exists", length = 1)
    private String internetBookstoreBookExists;

    /**
     * 포털 사이트 도서 존재 여부 (등록 여부)
     * [CSV 매칭: PORTAL_SITE_BOOK_EXST_AT]
     */
    @Column(name = "portal_site_book_exists", length = 1)
    private String portalSiteBookExists;

    /**
     * ISBN 번호 (통합/구형 ISBN 등)
     * [CSV 매칭: ISBN_NO]
     */
    @Column(name = "isbn_no", length = 10)
    private String isbnNo;

    /**
     * 엔티티 생성 일시 (시스템 자동 설정)
     */
    @Column(name="created_at",updatable = false,nullable = false)
    private OffsetDateTime createdAt;

    /**
     * 엔티티 수정 일시 (시스템 자동 설정)
     */
    @Column(name="updated_at")
    private OffsetDateTime updatedAt;

    /**
     * RAG 검색용 도서 임베딩 벡터 값 (1024차원)
     */
    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private float[] embedding;

    public void updateEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Book(
            Long sourceSequenceNo,
            String isbn,
            String volumeTitle,
            String title,
            String authorName,
            String publisherName,
            LocalDate firstPublishDate,
            String additionalSymbolName,
            BigDecimal price,
            String imageUrl,
            String bookContent,
            String kdcName,
            String subtitle,
            String authorSubstanceName,
            LocalDate editionPublishDate,
            String internetBookstoreBookExists,
            String portalSiteBookExists,
            String isbnNo
    ) {
        this.sourceSequenceNo = sourceSequenceNo;
        this.isbn = isbn;
        this.volumeTitle = volumeTitle;
        this.title = title;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.firstPublishDate = firstPublishDate;
        this.additionalSymbolName = additionalSymbolName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
        this.kdcName = kdcName;
        this.subtitle = subtitle;
        this.authorSubstanceName = authorSubstanceName;
        this.editionPublishDate = editionPublishDate;
        this.internetBookstoreBookExists = internetBookstoreBookExists;
        this.portalSiteBookExists = portalSiteBookExists;
        this.isbnNo = isbnNo;
    }

}
