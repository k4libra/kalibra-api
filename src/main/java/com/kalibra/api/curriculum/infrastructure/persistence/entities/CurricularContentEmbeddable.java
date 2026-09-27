package com.kalibra.api.curriculum.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class CurricularContentEmbeddable {

    @Column(name = "normalized_text", columnDefinition = "text")
    private String normalizedText;

    @Column(name = "page_count")
    private Integer pageCount;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public CurricularContentEmbeddable() {
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}
