package com.kalibra.api.progress.domain.services;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;

public interface SubtopicMasteryCommandService {

    SubtopicMastery handle(UpdateSubtopicMasteryCommand command);
}