package com.zimbra.cs.ml.schema;


/**
 * Information about a persisted training set
 */
public class TrainingSetInfo {

    public String date;
    public int numTrain;
    public int numTest;

    public TrainingSetInfo(String date, int numTrain, int numTest) {
        this.date = date;
        this.numTrain = numTrain;
        this.numTest = numTest;
    }

    public String getDate() {
        return date;
    }

    public int getNumTrain() {
        return numTrain;
    }

    public int getNumTest() {
        return numTest;
    }
}
