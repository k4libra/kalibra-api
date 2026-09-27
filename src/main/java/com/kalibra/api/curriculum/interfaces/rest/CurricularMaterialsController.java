package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetCurricularMaterialsByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.Pagination;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.CurricularMaterialPageResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CurricularMaterialResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.UploadCurricularMaterialResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.CurricularMaterialAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
@Tag(name = "Courses", description = "Material uploaded by the teacher for the subtopics of a course")
@RequestMapping("/api/v1/courses/{id}/curricular-materials")
public class CurricularMaterialsController {

    private final CurricularMaterialCommandService commandService;
    private final CurricularMaterialQueryService queryService;
    private final CurricularMaterialAssembler assembler;

    public CurricularMaterialsController(CurricularMaterialCommandService commandService,
                                         CurricularMaterialQueryService queryService,
                                         CurricularMaterialAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Upload curricular material",
            description = "Uploads a PDF, PNG or JPEG file for one or more subtopics of one of my courses. "
                    + "It starts as PENDING_INGESTION and becomes READY or INGESTION_ERROR once processed.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Material registered, pending ingestion"),
            @ApiResponse(responseCode = "400",
                    description = "Empty file, missing fields or a subtopic that does not belong to the course", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "413", description = "File larger than the allowed size", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "415", description = "Unsupported format", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schemaProperties = {
                    @SchemaProperty(name = "file", schema = @Schema(type = "string", format = "binary")),
                    @SchemaProperty(name = "subtopicIds",
                            array = @ArraySchema(schema = @Schema(type = "string", format = "uuid"), minItems = 1)),
                    @SchemaProperty(name = "fileName", schema = @Schema(type = "string", maxLength = 255)),
                    @SchemaProperty(name = "format", schema = @Schema(type = "string", allowableValues = {"PDF", "PNG", "JPEG"}))
            }))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CurricularMaterialResource> upload(@PathVariable("id") UUID courseId,
                                                             @Parameter(hidden = true)
                                                             @Valid @ModelAttribute UploadCurricularMaterialResource resource,
                                                             @Parameter(hidden = true) @RequestPart("file") MultipartFile file,
                                                             Authentication authentication) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException failure) {
            throw new UncheckedIOException("The uploaded file could not be read", failure);
        }
        var command = assembler.toCommand(resource, courseId, authentication.getName(), content);
        return new ResponseEntity<>(assembler.toResource(commandService.handle(command)), HttpStatus.CREATED);
    }

    @Operation(summary = "List the curricular material of my course",
            description = "Paginated, newest first, with the processing status of each material.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of materials"),
            @ApiResponse(responseCode = "400", description = "page below 0 or size outside 1..100", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<CurricularMaterialPageResource> getAll(@PathVariable("id") UUID courseId,
                                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_SIZE) int size,
                                                                 Authentication authentication) {
        var query = new GetCurricularMaterialsByCourseQuery(authentication.getName(), new CourseId(courseId), Pagination.of(page, size));
        return ResponseEntity.ok(assembler.toResource(queryService.handle(query)));
    }
}
