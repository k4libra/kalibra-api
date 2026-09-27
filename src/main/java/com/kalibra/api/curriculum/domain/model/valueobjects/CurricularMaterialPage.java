package com.kalibra.api.curriculum.domain.model.valueobjects;

import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;

import java.util.List;

public record CurricularMaterialPage(List<CurricularMaterial> items, int page, int size, long totalElements, int totalPages) {

    public CurricularMaterialPage {
        items = List.copyOf(items);
    }
}
