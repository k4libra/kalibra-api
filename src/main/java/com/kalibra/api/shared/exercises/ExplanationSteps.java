package com.kalibra.api.shared.exercises;

import com.kalibra.api.shared.contracts.curriculum.ExplanationStep;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ExplanationSteps {

    private static final Pattern CODE = Pattern.compile("```[^\\r\\n`]*\\R([\\s\\S]*?)(?:```|$)");

    private ExplanationSteps() {
    }

    public static List<ExplanationStep> resolve(List<ExplanationStep> steps, String explanation) {
        if (steps != null && !steps.isEmpty()) {
            return List.copyOf(steps);
        }
        if (explanation == null || explanation.isBlank()) {
            return List.of();
        }
        var result = new ArrayList<ExplanationStep>();
        var matcher = CODE.matcher(explanation);
        var start = 0;
        while (matcher.find()) {
            addText(result, explanation.substring(start, matcher.start()));
            var code = matcher.group(1).strip();
            if (!code.isBlank()) {
                result.add(new ExplanationStep("Código", code, "CODE"));
            }
            start = matcher.end();
        }
        addText(result, explanation.substring(start));
        return List.copyOf(result);
    }

    private static void addText(List<ExplanationStep> result, String text) {
        for (var detail : text.strip().split("\\R\\s*\\R|(?<=[.!?])\\s+")) {
            if (!detail.isBlank()) {
                result.add(new ExplanationStep("Paso " + (result.size() + 1), detail.strip(), "TEXT"));
            }
        }
    }
}
