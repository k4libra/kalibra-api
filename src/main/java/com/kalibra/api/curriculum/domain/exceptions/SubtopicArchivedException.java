package com.kalibra.api.curriculum.domain.exceptions;

public class SubtopicArchivedException extends RuntimeException {

    public SubtopicArchivedException() {
        super("Archived subtopics cannot receive material or generate exercises");
    }
}
