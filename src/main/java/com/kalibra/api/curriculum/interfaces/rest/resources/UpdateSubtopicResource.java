package com.kalibra.api.curriculum.interfaces.rest.resources;

import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSubtopicResource(@Pattern(regexp = "(?s).*\\S.*") @Size(max = 120) String name,
        @Min(1) Integer displayOrder, SubtopicStatus status) { }
