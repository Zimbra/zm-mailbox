package com.zimbra.cs.ml;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.ml.classifier.Classifier;
import com.zimbra.cs.ml.classifier.ClassifierManager;

public abstract class ClassificationTaskConfigProvider {

    /**
     * Provides a mapping of classification tasks to the labels of classifiers to use for each task
     */
    public abstract Map<String, TaskConfig> getConfigMap();

    /**
     * Assign a classifier to a task with a given overlapping class threshold
     */
    @SuppressWarnings("unchecked")
    public <C extends Classifiable> void assignClassifier(String taskName, Classifier<C> classifier, Float threshold) throws ServiceException {
        ClassificationTask<C> task = (ClassificationTask<C>) ClassifierManager.getInstance().getTaskByName(taskName);
        if (!task.supportsClassifier(classifier)) {
            throw ServiceException.FAILURE(String.format("classifier \"%s\" cannot be assigned to task \"%s\"", classifier.getLabel(), taskName), null);
        }
        assign(taskName, classifier.getLabel(), threshold);
    }

    /**
     * Assign a classifier to a task
     */
    public <C extends Classifiable> void assignClassifier(String taskName, Classifier<C> classifier) throws ServiceException {
        assignClassifier(taskName, classifier, null);
    }

    protected abstract void assign(String taskName, String classifierLabel, Float threshold) throws ServiceException;

    /**
     * Remove the classifier assigned to the given task
     */
    public abstract void clearAssignment(String taskName) throws ServiceException;

    public static class TaskConfig extends Pair<String, Float> {

        public TaskConfig(String classifierLabel) {
            this(classifierLabel, null);
        }

        public TaskConfig(String classifierLabel, Float threshold) {
            super(classifierLabel, threshold);
        }

        public String getClassifierLabel() {
            return getFirst();
        }

        public Float getThreshold() {
            return getSecond();
        }
    }
}