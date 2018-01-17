package com.zimbra.cs.ml.schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing training data to be be passed to a classifier.
 */
public class TrainingData {

    private List<TrainingDocument> trainingDocs;

    public TrainingData() {
        this.trainingDocs = new ArrayList<>();
    }

    public void addDoc(TrainingDocument doc) {
        trainingDocs.add(doc);
    }

    public List<TrainingDocument> getTrainingDocs() {
        return trainingDocs;
    }

    /**
     * Class encompassing a training input document.
     * Note that the training API expects the text, exclusive targets, and overlapping targets
     * to each be in their own lists, but this format is more convenient for constructing
     * the training data.
     */
    public static class TrainingDocument {
        private MessageClassificationInput input;
        private String exclusiveTarget;
        private String[] overlappingTargets;

        public TrainingDocument(MessageClassificationInput input, String exclusiveTarget, String[] overlappingTargets) {
            this.input = input;
            this.exclusiveTarget = exclusiveTarget;
            this.overlappingTargets = overlappingTargets;
        }

        public MessageClassificationInput getInput() {
            return input;
        }

        public String[] getOverlappingTargets() {
            return overlappingTargets;
        }

        public String getExclusiveTarget() {
            return exclusiveTarget;
        }
    }
}
