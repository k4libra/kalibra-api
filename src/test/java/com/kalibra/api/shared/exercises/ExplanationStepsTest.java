package com.kalibra.api.shared.exercises;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplanationStepsTest {
    @Test
    void shouldSplitLegacySentencesParagraphsAndPreserveCode() {
        var steps = ExplanationSteps.resolve(null, "Primero resta. Luego divide.\n\nComprueba:\n```java\nvar x = 2;\nassert x == 2;\n```\nEl resultado es dos.");
        assertThat(steps).extracting(ExplanationStep::kind).containsExactly("TEXT", "TEXT", "TEXT", "CODE", "TEXT");
        assertThat(steps.get(3).detail()).isEqualTo("var x = 2;\nassert x == 2;");
    }

    @Test
    void shouldPreferStructuredStepsAndHandleEmptyLegacyExplanations() {
        var step = new ExplanationStep("Fórmula", "x = 2", "FORMULA");
        assertThat(ExplanationSteps.resolve(List.of(step), "Old. Explanation.")).containsExactly(step);
        assertThat(ExplanationSteps.resolve(null, null)).isEmpty();
        assertThat(ExplanationSteps.resolve(List.of(), "  ")).isEmpty();
    }
}
