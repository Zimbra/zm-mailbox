package com.zimbra.cs.ml.schema;

/**
 * Information about a registered classifier
 */
public class ClassifierInfo extends ClassifierSpec {

    private int epoch;
    private TrainingSetInfo trainingSet;

    public ClassifierInfo(String classifierId, int numSubjectWords, int numBodyWords,
            String[] exclusiveClasses, String[] overlappingClasses, Integer lookupDim, Integer lookupSize) {
        super(numSubjectWords, numBodyWords, exclusiveClasses, overlappingClasses);
        setClassifierId(classifierId);
        setLookupDim(lookupDim);
        setLookupSize(lookupSize);
    }


    public int getEpoch() {
        return epoch;
    }

    public void setEpoch(int epoch) {
        this.epoch = epoch;
    }


    public TrainingSetInfo getTrainingSet() {
        return trainingSet;
    }


    public void setTrainingSet(TrainingSetInfo trainingSet) {
        this.trainingSet = trainingSet;
    }

    public static ClassifierInfo fromSpec(String id, ClassifierSpec spec) {
        ClassifierInfo info = new ClassifierInfo(id,
                spec.getNumSubjectWords(),
                spec.getNumBodyWords(),
                spec.getExclusiveClasses(),
                spec.getOverlappingClasses(),
                spec.getLookupDim(),
                spec.getLookupSize());
        info.setNumFeatures(spec.getNumFeatures());
        info.setVocabPath(spec.getVocabPath());
        return info;
    }
}
