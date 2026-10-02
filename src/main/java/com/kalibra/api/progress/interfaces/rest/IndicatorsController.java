package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.ExportCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetCourseIndicatorsQuery;
import com.kalibra.api.progress.domain.model.queries.GetIndicatorGuideQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.CourseIndicatorReportResource;
import com.kalibra.api.progress.interfaces.rest.resources.IndicatorGuideResource;
import com.kalibra.api.progress.interfaces.rest.transform.SubtopicMasteryAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@Tag(name = "Course Indicators", description = "Indicators of a course, calculated from the use of the platform")
@RequestMapping("/api/v1/courses/{id}/indicators")
public class IndicatorsController {

    public static final String TEXT_CSV_VALUE = "text/csv";

    private static final String SUMMARY = "Get or export the indicators of my course";
    private static final String DESCRIPTION = "As JSON (the default): accuracy per student and for the group, exercises solved, "
            + "mastery evolution per subtopic and the approval rate of the verification. Enrolled students without activity are "
            + "listed apart from the group accuracy, subtopics without practice come without variation, and hasActivity is false "
            + "while nothing was solved. With Accept: text/csv: a file with one row per student and subtopic with activity, where "
            + "each student is identified only by an anonymous code, never by name or email.";

    private final SubtopicMasteryQueryService queryService;
    private final SubtopicMasteryAssembler assembler;

    public IndicatorsController(
            SubtopicMasteryQueryService queryService,
            SubtopicMasteryAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    // Both handlers share one documented operation: OpenAPI has a single GET per path.
    @Operation(summary = SUMMARY, description = DESCRIPTION)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicators of the course, as JSON or as a CSV file", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CourseIndicatorReportResource.class)),
                    @Content(mediaType = TEXT_CSV_VALUE, schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher; for CSV, also a course with no indicators to export yet", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CourseIndicatorReportResource> get(
            @PathVariable("id") UUID courseId,
            Authentication authentication) {
        var query = new GetCourseIndicatorsQuery(
                authentication.getName(),
                new CourseId(courseId)
        );
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }

    // Both handlers share one documented operation: OpenAPI has a single GET per path.
    @Operation(summary = SUMMARY, description = DESCRIPTION)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicators of the course, as JSON or as a CSV file", content = {
                    @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CourseIndicatorReportResource.class)),
                    @Content(mediaType = TEXT_CSV_VALUE, schema = @Schema(type = "string"))}),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher; for CSV, also a course with no indicators to export yet", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping(produces = TEXT_CSV_VALUE)
    public ResponseEntity<String> getAsCsv(
            @PathVariable("id") UUID courseId,
            Authentication authentication) {
        var query = new ExportCourseIndicatorsQuery(
                authentication.getName(),
                new CourseId(courseId)
        );
        var export = assembler.toResource(queryService.handle(query));
        var disposition = ContentDisposition.attachment()
                .filename(export.fileName())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(export.content());
    }

    @Operation(summary = "Get the guide to read the indicators",
            description = "For each indicator: what it measures and which result is a good sign, in plain language.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guide of the four indicators"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping("/guide")
    public ResponseEntity<IndicatorGuideResource> getGuide(@PathVariable("id") UUID courseId) {
        return ResponseEntity.ok(assembler.toResource(queryService.handle(new GetIndicatorGuideQuery())));
    }
}
