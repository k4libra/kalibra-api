package com.kalibra.api.curriculum.domain.model.entities;

public class ExerciseOption {

    private String key;
    private String text;
    private boolean correct;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public ExerciseOption() {
    }

    public ExerciseOption(String key, String text, boolean correct) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Exercise option key cannot be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Exercise option text cannot be blank");
        }
        this.key = key.trim().toUpperCase();
        this.text = text;
        this.correct = correct;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
