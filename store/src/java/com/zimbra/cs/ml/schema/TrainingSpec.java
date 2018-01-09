package com.zimbra.cs.ml.schema;

/**
 * All information needed to train a classifier
 */
public class TrainingSpec {

    private String classifierId;
    private TrainingData trainingData;
    private TrainingData testData;
    private int epochs;
    private float learningRate;
    private float percentHoldout;
    private boolean persist = false;

    public TrainingSpec(TrainingData trainingData) {
        this.trainingData = trainingData;
    }

    public String getClassifierId() {
        return classifierId;
    }

    public void setClassifierId(String classifierId) {
        this.classifierId = classifierId;
    }

    public TrainingData getTrainingData() {
        return trainingData;
    }

    public int getEpochs() {
        return epochs;
    }

    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }

    public void setTestData(TrainingData testData) {
        this.testData = testData;
    }

    public TrainingData getTestData() {
        return testData;
    }

    public float getPercentHoldout() {
        return percentHoldout;
    }


    public void setPercentHoldout(float percentHoldout) {
        this.percentHoldout = percentHoldout;
    }

    public float getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(float learningRate) {
        this.learningRate = learningRate;
    }

    public boolean isPersist() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

}
