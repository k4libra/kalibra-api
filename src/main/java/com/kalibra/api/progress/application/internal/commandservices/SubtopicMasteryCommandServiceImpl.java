package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.SubtopicMasteryCommandService;
import org.springframework.stereotype.Service;

@Service
public class SubtopicMasteryCommandServiceImpl
        implements SubtopicMasteryCommandService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;

    public SubtopicMasteryCommandServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository) {
        this.subtopicMasteryRepository = subtopicMasteryRepository;
    }

    @Override
    public SubtopicMastery handle(
            UpdateSubtopicMasteryCommand command) {

        var mastery = subtopicMasteryRepository
                .findByHolderIdAndSubtopicId(
                        command.holderId(),
                        command.subtopicId()
                )
                .map(existing -> {
                    existing.apply(command);
                    return existing;
                })
                .orElseGet(() ->
                        SubtopicMastery.firstEstimate(command)
                );

        return subtopicMasteryRepository.save(mastery);
    }
}