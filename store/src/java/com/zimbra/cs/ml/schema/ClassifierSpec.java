package com.zimbra.cs.ml.schema;

/**
 * Specification used to register a new classifier.
 * Mirrors the GraphQL object type defined in zimbra-ml/schema/schema.py.
 */
public class ClassifierSpec {

    private String classifierId;
    private int numSubjectWords;
    private int numBodyWords;
    private int numFeatures;
    private String[] exclusiveClasses;
    private String[] overlappingClasses;
    private String vocabPath;
    private Integer lookupSize; //used for auto lookup table creation from training data
    private Integer lookupDim; //dimensions of embeddings in the automatic lookup table

    public ClassifierSpec(int numSubjectWords, int numBodyWords,
            String[] exclusiveClasses, String[] overlappingClasses) {
        setNumBodyWords(numBodyWords);
        setNumSubjectWords(numSubjectWords);
        setExclusiveClasses(exclusiveClasses);
        setOverlappingClasses(overlappingClasses);
    }

    public String getClassifierId() {
        return classifierId;
    }
    public void setClassifierId(String classifierId) {
        this.classifierId = classifierId;
    }
    public int getNumSubjectWords() {
        return numSubjectWords;
    }
    public void setNumSubjectWords(int numSubjectWords) {
        this.numSubjectWords = numSubjectWords;
    }
    public int getNumBodyWords() {
        return numBodyWords;
    }
    public void setNumBodyWords(int numBodyWords) {
        this.numBodyWords = numBodyWords;
    }
    public int getNumFeatures() {
        return numFeatures;
    }
    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }
    public String[] getExclusiveClasses() {
        return exclusiveClasses;
    }
    public void setExclusiveClasses(String[] exclusiveClasses) {
        if (exclusiveClasses == null) {
            this.exclusiveClasses = new String[0];
        } else {
            this.exclusiveClasses = exclusiveClasses;
        }
    }

    public String[] getOverlappingClasses() {
        return overlappingClasses;
    }

    public void setOverlappingClasses(String[] overlappingClasses) {
        if (overlappingClasses == null) {
            this.overlappingClasses = new String[0];
        } else {
            this.overlappingClasses = overlappingClasses;
        }
    }

    public String getVocabPath() {
        return vocabPath;
    }

    public void setVocabPath(String vocabPath) {
        this.vocabPath = vocabPath;
    }

    public Integer getLookupSize() {
        return lookupSize;
    }

    public void setLookupSize(Integer lookupSize) {
        this.lookupSize = lookupSize;
    }

    public Integer getLookupDim() {
        return lookupDim;
    }

    public void setLookupDim(Integer lookupDim) {
        this.lookupDim = lookupDim;
    }
}
