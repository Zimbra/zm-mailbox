package com.zimbra.cs.ml.classifier;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.ComputedFeatures;
import com.zimbra.cs.ml.feature.FeatureSet;
import com.zimbra.cs.ml.query.AbstractClassificationQuery;
import com.zimbra.cs.ml.query.TrainClassifierQuery;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassificationResult;
import com.zimbra.cs.ml.schema.TrainingSpec;

public abstract class Classifier<T extends Classifiable> {

    private String id;
    private String classifierLabel;
    private FeatureSet<T> featureSet;
    private ClassifierInfo info = null;
    private String description;
    private ClassifiableType type;

    public static enum ClassifiableType {
        MESSAGE;
    }

    public Classifier(String id, String label, ClassifiableType type, FeatureSet<T> featureSet) {
        this(id, label, type, featureSet, null);
    }

    public Classifier(String id, String label, ClassifiableType type, FeatureSet<T> featureSet, ClassifierInfo info) {
        this.id = id;
        this.classifierLabel = label;
        this.featureSet = featureSet;
        this.info = info;
        this.type = type;
    }

    public String getLabel() {
        return classifierLabel;
    }

    public FeatureSet<T> getFeatureSet() {
        return featureSet;
    }

    public void setClassifierInfo(ClassifierInfo info) {
        this.info = info;
    }

    public ClassifierInfo getInfo() {
        return info;
    }

    public String getId() {
        return id;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public String getDescription() {
        return description;
    }

    public ClassifiableType getType() {
        return type;
    }

    protected abstract AbstractClassificationQuery<T> buildQuery(ComputedFeatures<T> features) throws ServiceException;

    public ClassifierInfo train(TrainingSpec trainingSpec) throws ServiceException {
        trainingSpec.setClassifierId(getId());
        TrainClassifierQuery query = new TrainClassifierQuery(trainingSpec);
        ClassifierInfo info = query.execute();
        this.info = info;
        return info;
    }

    public ClassificationResult classify(T item) throws ServiceException {
        ComputedFeatures<T> features = getFeatureSet().getFeatures(item);
        AbstractClassificationQuery<T> query = buildQuery(features);
        return query.execute();
    }
}
