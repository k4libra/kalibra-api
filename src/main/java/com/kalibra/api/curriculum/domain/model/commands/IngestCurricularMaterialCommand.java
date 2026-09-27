package com.kalibra.api.curriculum.domain.model.commands;

import com.kalibra.api.curriculum.domain.model.valueobjects.MaterialId;

public record IngestCurricularMaterialCommand(MaterialId materialId) { }
