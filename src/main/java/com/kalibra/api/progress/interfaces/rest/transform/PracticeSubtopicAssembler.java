package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeSubtopicResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PracticeSubtopicAssembler {

    PracticeSubtopicResource toResource(
            PracticeSubtopicView view
    );
}