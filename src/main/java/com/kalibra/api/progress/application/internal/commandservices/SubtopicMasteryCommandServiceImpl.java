package com.kalibra.api.progress.application.internal.commandservices;

import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.commands.UpdateSubtopicMasteryCommand;
import com.kalibra.api.progress.domain.model.events.MasteryUpdated;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.SubtopicMasteryCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SubtopicMasteryCommandServiceImpl
        implements SubtopicMasteryCommandService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate requiresNew;

    public SubtopicMasteryCommandServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository,
            ApplicationEventPublisher eventPublisher,
            PlatformTransactionManager transactionManager) {
        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.eventPublisher = eventPublisher;
        // REQUIRES_NEW: this runs from an AFTER_COMMIT listener, where joining the finished
        // transaction would silently drop the writes.
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Two first answers of the same student can race to create the mastery; the one that
    // loses the unique (holder, subtopic) constraint retries and applies over the winner.
    @Override
    public SubtopicMastery handle(
            UpdateSubtopicMasteryCommand command) {
        try {
            return requiresNew.execute(status -> update(command));
        } catch (DataIntegrityViolationException concurrentFirstEstimate) {
            return requiresNew.execute(status -> update(command));
        }
    }

    private SubtopicMastery update(UpdateSubtopicMasteryCommand command) {
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
        var saved = subtopicMasteryRepository.save(mastery);
        eventPublisher.publishEvent(new MasteryUpdated(
                saved.getHolderId(),
                saved.getSubtopicId().value(),
                saved.getCurrentEstimate().value()
        ));
        return saved;
    }
}
