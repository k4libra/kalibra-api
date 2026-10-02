package com.kalibra.api.progress.interfaces.rest.transform;

import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.interfaces.rest.resources.StudentProgressResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StudentProgressAssembler {

    StudentProgressResource toResource(
            StudentProgressReport report
    );
}