package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetCriticalSubtopicsQuery;
import com.kalibra.api.progress.domain.model.queries.GetInstitutionalIndicatorsQuery;
import com.kalibra.api.progress.domain.services.InstitutionalIndicatorsQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.CriticalSubtopicResource;
import com.kalibra.api.progress.interfaces.rest.resources.InstitutionalIndicatorsResource;
import com.kalibra.api.progress.interfaces.rest.transform.InstitutionalIndicatorsAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Tag(name = "Institutional Indicators", description = "Platform-wide analytics for administrators")
@RequestMapping("/api/v1/institutional-indicators")
public class InstitutionalIndicatorsController {

    private final InstitutionalIndicatorsQueryService queryService;
    private final InstitutionalIndicatorsAssembler assembler;

    public InstitutionalIndicatorsController(InstitutionalIndicatorsQueryService queryService,
                                             InstitutionalIndicatorsAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get the institutional indicators",
            description = "Totals and one summary per course: enrollment, activity, accuracy, mastery evolution and "
                    + "verification approval. With Accept: text/csv: the same per-course aggregates as a file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Institutional indicators"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not an administrator",
                    content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitutionalIndicatorsResource> get() {
        return ResponseEntity.ok(assembler.toResource(queryService.handle(new GetInstitutionalIndicatorsQuery())));
    }

    // Both handlers share one documented operation: OpenAPI has a single GET per path.
    @Operation(hidden = true)
    @GetMapping(produces = IndicatorsController.TEXT_CSV_VALUE)
    public ResponseEntity<String> getAsCsv() {
        var csv = assembler.toCsv(queryService.handle(new GetInstitutionalIndicatorsQuery()));
        var disposition = ContentDisposition.attachment().filename("institutional-indicators.csv").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @Operation(summary = "List the critical subtopics",
            description = "Weakest subtopics across every course with mastery data, lowest group mastery first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Critical subtopics, at most 20"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not an administrator",
                    content = @Content)
    })
    @GetMapping("/critical-subtopics")
    public ResponseEntity<List<CriticalSubtopicResource>> getCriticalSubtopics() {
        var lines = queryService.handle(new GetCriticalSubtopicsQuery());
        return ResponseEntity.ok(lines.stream().map(assembler::toResource).toList());
    }
}
