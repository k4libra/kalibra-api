package com.kalibra.api.curriculum.interfaces.rest.resources;

import java.util.List;

public record CurricularMaterialPageResource(
        List<CurricularMaterialResource> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) { }
