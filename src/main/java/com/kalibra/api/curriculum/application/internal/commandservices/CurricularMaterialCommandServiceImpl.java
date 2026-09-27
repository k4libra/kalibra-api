package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalCurricularExtractionService;
import com.kalibra.api.curriculum.application.internal.outboundservices.storage.MaterialStorageService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.commands.IngestCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.commands.UploadCurricularMaterialCommand;
import com.kalibra.api.curriculum.domain.model.events.MaterialIngested;
import com.kalibra.api.curriculum.domain.model.events.MaterialIngestionFailed;
import com.kalibra.api.curriculum.domain.model.events.MaterialUploaded;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.services.CurricularMaterialCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CurricularMaterialCommandServiceImpl implements CurricularMaterialCommandService {

    private static final Logger log = LoggerFactory.getLogger(CurricularMaterialCommandServiceImpl.class);
    private static final String UNREADABLE_CONTENT = "The material content could not be extracted";

    private final CurricularMaterialRepository curricularMaterialRepository;
    private final CourseRepository courseRepository;
    private final MaterialStorageService materialStorageService;
    private final ExternalCurricularExtractionService externalCurricularExtractionService;
    private final ApplicationEventPublisher eventPublisher;

    public CurricularMaterialCommandServiceImpl(CurricularMaterialRepository curricularMaterialRepository,
                                                 CourseRepository courseRepository,
                                                 MaterialStorageService materialStorageService,
                                                 ExternalCurricularExtractionService externalCurricularExtractionService,
                                                 ApplicationEventPublisher eventPublisher) {
        this.curricularMaterialRepository = curricularMaterialRepository;
        this.courseRepository = courseRepository;
        this.materialStorageService = materialStorageService;
        this.externalCurricularExtractionService = externalCurricularExtractionService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CurricularMaterial handle(UploadCurricularMaterialCommand command) {
        var course = courseRepository.findByIdAndHolderId(command.courseId(), command.holderId())
                .orElseThrow(() -> new CourseNotOwnedByTeacherException(command.courseId().value()));
        for (var subtopicId : command.subtopicIds()) {
            if (!course.hasSubtopic(subtopicId)) {
                throw new IllegalArgumentException("Subtopic does not belong to the course: " + subtopicId.value());
            }
        }
        var storageReference = materialStorageService.store(command.fileName(), command.content());
        var material = CurricularMaterial.register(command, storageReference);
        var saved = curricularMaterialRepository.save(material);
        eventPublisher.publishEvent(new MaterialUploaded(saved.getId().value()));
        return saved;
    }

    // REQUIRES_NEW: this runs from an AFTER_COMMIT listener, where joining the finished
    // transaction would silently drop the writes.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(IngestCurricularMaterialCommand command) {
        var material = curricularMaterialRepository.findById(command.materialId())
                .filter(CurricularMaterial::isPendingIngestion)
                .orElse(null);
        if (material == null) {
            return;
        }
        Optional<CurricularContent> content;
        try {
            content = externalCurricularExtractionService.extract(material);
        } catch (RuntimeException engineUnavailable) {
            log.warn("Curricular material {} stays pending ingestion: {}",
                    material.getId().value(), engineUnavailable.getMessage());
            return;
        }
        if (content.isPresent()) {
            material.markReady(content.get());
            curricularMaterialRepository.save(material);
            eventPublisher.publishEvent(new MaterialIngested(material.getId().value(), material.getCourseId().value()));
        } else {
            material.markFailed(UNREADABLE_CONTENT);
            curricularMaterialRepository.save(material);
            eventPublisher.publishEvent(new MaterialIngestionFailed(material.getId().value(), UNREADABLE_CONTENT));
        }
    }
}
