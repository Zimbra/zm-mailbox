package com.zimbra.cs.ml.query;

import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.TrainingSpec;

/**
 * Query to train a classifier
 */
public class TrainClassifierQuery extends MLQuery<ClassifierInfo> {

    private TrainingSpec trainingSpec;

    public TrainClassifierQuery(TrainingSpec trainingSpec) {
        this.trainingSpec = trainingSpec;
    }

    public TrainingSpec getTrainingSpec() {
        return trainingSpec;
    }
}
