package com.zimbra.cs.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.classifier.Classifier;

/**
 * This class is a bridge between the mailbox and the classification system.
 * It provides a mapping of {@link Classifier} instances to corresponding {@link ClassificationHandler} instances
 * that lets us execute the known {@link ClassificationTask} definitions.
 * This abstracts away the details of which classifiers are invoked, as there may not be a one-to-one
 * correspondence between tasks and classifiers.
 */
public class ClassificationExecutionContext<C extends Classifiable> {
    private Map<String, ClassifierUsageInfo<C>> classifierMap;

    public ClassificationExecutionContext() {
        classifierMap = new HashMap<>();
    }

    /**
     * Link a {@link ClassificationTask} to a {@link Classifier}
     */
    public void addClassifierForTask(ClassificationTask<C> task, Classifier<C> classifier) {
        ClassifierUsageInfo<C> usageInfo = classifierMap.get(classifier.getId());
        if (usageInfo == null) {
            usageInfo = new ClassifierUsageInfo<>(classifier);
            classifierMap.put(classifier.getId(), usageInfo);
        }
        usageInfo.addTask(task);
    }

    /**
     * Invoke each classifier and handle the classification output accordingly.
     */
    public void execute(C item) throws ServiceException {
        for (ClassifierUsageInfo<C> info: classifierMap.values()) {
            ClassificationHandler<C> handler = info.getHandler();
            handler.handle(item, info.getClassifier().classify(item));
        }
    }

    /**
     * Get the {@link ClassificationHandler} for the given classifier
     */
    public ClassificationHandler<C> getHandler(Classifier<C> classifier) {
        ClassifierUsageInfo<C> info = classifierMap.get(classifier.getId());
        return info == null ? null : info.getHandler();
    }

    /**
     * Return a list of all {@link ClassifierUsageInfo} for this execution context
     */
    public List<ClassifierUsageInfo<C>> getInfo() {
        return new ArrayList<ClassifierUsageInfo<C>>(classifierMap.values());
    }

    /**
     * Return the {@link ClassifierUsageInfo} for the given classifier for this execution context
     */
    public ClassifierUsageInfo<C> getClassifierUsage(Classifier<C> classifier) {
        return classifierMap.containsKey(classifier.getId()) ? classifierMap.get(classifier.getId()) : new ClassifierUsageInfo<C>(classifier);
    }

    /**
     * Returns false if this execution context does not result in any tasks being processed
     */
    public boolean hasResolvedTasks() {
        return !classifierMap.isEmpty();
    }

    public static class ClassifierUsageInfo<C extends Classifiable> {
        private Classifier<C> classifier;
        private ClassificationHandler<C> handler;
        private List<ClassificationTask<C>> tasks;

        ClassifierUsageInfo(Classifier<C> classifier) {
            this.classifier = classifier;
            this.handler = new ClassificationHandler<>();
            this.tasks = new ArrayList<>();
        }

        public Classifier<C> getClassifier() {
            return classifier;
        }

        public ClassificationHandler<C> getHandler() {
            return handler;
        }

        public void addTask(ClassificationTask<C> task) {
            tasks.add(task);
        }

        public List<ClassificationTask<C>> getTasks() {
            return tasks;
        }
    }
}