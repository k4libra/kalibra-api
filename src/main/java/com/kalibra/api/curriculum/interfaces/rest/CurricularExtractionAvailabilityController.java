package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetCurricularExtractionAvailabilityQuery;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.CurricularExtractionAvailabilityResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.CurricularMaterialAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Courses", description = "Courses and curricular material managed by teachers")
@RequestMapping("/api/v1/curricular-extraction-availability")
public class CurricularExtractionAvailabilityController {

    private final CurricularMaterialQueryService queryService;
    private final CurricularMaterialAssembler assembler;

    public CurricularExtractionAvailabilityController(CurricularMaterialQueryService queryService,
                                                      CurricularMaterialAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get material upload availability")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current material upload availability"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CurricularExtractionAvailabilityResource> getAvailability() {
        var availability = queryService.handle(new GetCurricularExtractionAvailabilityQuery());
        return ResponseEntity.ok(assembler.toResource(availability));
    }
}
