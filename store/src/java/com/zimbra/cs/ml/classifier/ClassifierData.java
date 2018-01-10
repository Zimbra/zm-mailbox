package com.zimbra.cs.ml.classifier;

import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.classifier.Classifier.ClassifiableType;
import com.zimbra.cs.ml.feature.FeatureSet;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassifierSpec;

/**
 * Class encompassing all data needed to register a classifier
 */
public class ClassifierData<T extends Classifiable> {
    private String label;
    private String description;
    private FeatureSet<T> featureSet;
    private ClassifierSpec spec;
    private ClassifiableType type;

    public ClassifierData(ClassifiableType type, String label, ClassifierSpec spec, FeatureSet<T> featureSet) {
        this(type, label, spec, featureSet, null);
    }

    public ClassifierData(ClassifiableType type, String label, ClassifierSpec spec, FeatureSet<T> featureSet, String description) {
        this.type = type;
        this.label = label;
        this.description = description;
        this.featureSet = featureSet;
        this.spec = spec;
    }

    public String getLabel() {
        return label;
    }

    public ClassifierSpec getSpec() {
        return spec;
    }

    @SuppressWarnings("unchecked")
    public Classifier<T> create(ClassifierInfo info) {
        Classifier<T> classifier;
        switch (type) {
        case MESSAGE:
        default:
            classifier = (Classifier<T>) new MessageClassifier(info.getClassifierId(), label, (FeatureSet<Message>) featureSet);
        }
        if (description != null) {
            classifier.setDescription(description);
        }
        classifier.setClassifierInfo(info);
        return classifier;
    }
}