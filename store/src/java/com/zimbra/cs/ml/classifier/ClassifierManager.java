package com.zimbra.cs.ml.classifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.ClassificationExecutionContext;
import com.zimbra.cs.ml.ClassificationExecutionContext.ClassifierUsageInfo;
import com.zimbra.cs.ml.ClassificationHandler;
import com.zimbra.cs.ml.ClassificationTask;
import com.zimbra.cs.ml.ClassificationTaskConfigProvider;
import com.zimbra.cs.ml.ClassificationTaskConfigProvider.TaskConfig;
import com.zimbra.cs.ml.LdapClassificationTaskConfigProvider;
import com.zimbra.cs.ml.callback.ClassificationCallback;
import com.zimbra.cs.ml.callback.ExclusiveClassCallback;
import com.zimbra.cs.ml.callback.OverlappingClassCallback;
import com.zimbra.cs.ml.query.CreateClassifierQuery;
import com.zimbra.cs.ml.query.DeleteClassifierQuery;
import com.zimbra.cs.ml.query.ListClassifiersQuery;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassifierSpec;

/**
 * Primary entry point into the classifier system
 * @author iraykin
 */
public class ClassifierManager {

    private ClassifierRegistry registry;
    private Map<String, ClassificationTask<?>> taskNameMap;

    private static ClassifierManager instance = null;

    //type-safe map of classification execution contexts keyed by their parameterized types
    private Map<Class<? extends Classifiable>, ClassificationExecutionContext<? extends Classifiable>> executionContextCache;

    private ClassifierManager() throws ServiceException {
        this(new LdapClassifierRegistry());
    }

    private ClassifierManager(ClassifierRegistry registry) {
        this.registry = registry;
        this.taskNameMap = new HashMap<>();
        this.executionContextCache = new HashMap<>();
    }

    public void setRegistry(ClassifierRegistry registry) {
        this.registry  = registry;
    }

    public static synchronized ClassifierManager getInstance() throws ServiceException {
        if (instance == null) {
            instance = new ClassifierManager();
        }
        return instance;

    }

    private <T extends Classifiable> void storeExecutionContext(Class<T> key, ClassificationExecutionContext<T> context) {
        executionContextCache.put(key, context);
    }

    @SuppressWarnings("unchecked")
    public <T extends Classifiable> ClassificationExecutionContext<T> getExecutionContext(T item) throws ServiceException {
        Class<T> klass = (Class<T>) item.getClass();
        ClassificationExecutionContext<T> context = (ClassificationExecutionContext<T>) executionContextCache.get(klass);
        if (context == null) {
            context = resolveConfig(new LdapClassificationTaskConfigProvider());
            storeExecutionContext(klass, context);
        }
        return context;
    }

    public <T extends Classifiable> void clearExecutionContextCache() {
        executionContextCache.clear();
    }

    /**
     * Register a known {@link MachineLearningTask} with the classifier system.
     */
    public <T extends Classifiable> void registerClassificationTask(ClassificationTask<T> task) throws ServiceException {
        String taskName = task.getTaskName();
        if (taskNameMap.containsKey(taskName)) {
            throw ServiceException.FAILURE(String.format("Classification task with name \"%s\" already exists", taskName), null);
        }
        taskNameMap.put(taskName, task);
    }

    /**
     * Delete a given classification task and clear its task assignment from the given {@link ClassificationTaskConfigProvider}
     */
    public void deleteClassificationTask(String taskName, ClassificationTaskConfigProvider taskConfig) throws ServiceException {
        taskNameMap.remove(taskName);
        taskConfig.clearAssignment(taskName);
    }

    /**
     * Return a {@link ClassificationExecutionContext} for the given classification configuration.
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    <T extends Classifiable> ClassificationExecutionContext<T> resolveConfig(ClassificationTaskConfigProvider configProvider) throws ServiceException {
        //TODO: this is OK while only Message implements Classifiable, this method uses unsafe casts.
        //Need to figure out how to get the execution context for a specific Classifiable type.
        Set<String> resolvedTasks = new HashSet<>();
        ClassificationExecutionContext<T> context = new ClassificationExecutionContext<T>();
        Map<String, TaskConfig> configMap = configProvider.getConfigMap();
        for (Map.Entry<String, TaskConfig> configEntry: configMap.entrySet()) {
            String taskName = configEntry.getKey();
            TaskConfig config = configEntry.getValue();
            String classifierLabel = config.getClassifierLabel();
            Classifier<T> classifier = null;
            if (!taskNameMap.containsKey(taskName)) {
                ZimbraLog.ml.warn("classification config references non-existent classification task \"%s\"", taskName);
                continue;
            }
            try {
                classifier = (Classifier<T>) getClassifierByLabel(classifierLabel);
            } catch (ServiceException e) {
                ZimbraLog.ml.warn("unable to locate classifier \"%s\" to use for classification task '%s'", classifierLabel, taskName);
                continue;
            }
            ClassificationTask<T> task = (ClassificationTask<T>) taskNameMap.get(taskName);
            context.addClassifierForTask(task, classifier);
            try {
                ClassificationCallback<?> callback = task.resolveCallback(classifier);
                ClassificationHandler<T> handler = context.getHandler(classifier);
                if (callback instanceof OverlappingClassCallback<?>) {
                    OverlappingClassCallback<T> ocCallback = (OverlappingClassCallback<T>) callback;
                    Float threshold = config.getThreshold();
                    if (threshold == null) {
                        ZimbraLog.ml.error("probability threshold not specified for classification task \"%s\" for classifier '%s'", taskName, classifier.getLabel());
                        continue;
                    }
                    ocCallback.setThreshold(threshold);
                    handler.addOverlappingClassCallback(ocCallback);
                } else if (callback instanceof ExclusiveClassCallback<?>) {
                    if (handler.hasExclusiveCallback()) {
                        ZimbraLog.ml.error("classifier resolution found multiple exclusive class callbacks for classifier \"%s\"", classifierLabel);
                    }
                    handler.setExclusiveClassCallback((ExclusiveClassCallback<T>) callback);
                }
                resolvedTasks.add(task.getTaskName());
            } catch (ServiceException e) {
                ZimbraLog.ml.error("unable to resolve callback for classification task '%s' with classifier '%s'", taskName, classifierLabel, e);
            }
        }
        Set<String> unresolved = Sets.difference(taskNameMap.keySet(), resolvedTasks);
        if (unresolved.size() == 1) {
            ZimbraLog.ml.warn("classification task %s is not resolved", unresolved.iterator().next());
        } else if (unresolved.size() > 1) {
            ZimbraLog.ml.warn("classification tasks %s are not resolved", Joiner.on(", ").join(unresolved));
        }
        return context;
    }

    /**
     * Retrieve known classification tasks
     */
    public List<ClassificationTask<?>> getAllTasks() {
        return new ArrayList<>(taskNameMap.values());
    }

    public ClassificationTask<?> getTaskByName(String taskName) throws ServiceException {
        ClassificationTask<?> task = taskNameMap.get(taskName);
        if (task == null) {
            throw ServiceException.FAILURE(String.format("classification task \"%s\" not found", taskName), null);
        }
        return task;
    }

    /**
     * return a map of registered classifiers keyed by the IDs. Used by the classifier registry
     * to construct full {@link Classifier} instances
     */
    public Map<String, ClassifierInfo> getAllClassifierInfo() throws ServiceException {
        Map<String, ClassifierInfo> infoMap = new HashMap<>();
        ListClassifiersQuery query = new ListClassifiersQuery();
        for (ClassifierInfo info: query.execute()) {
            infoMap.put(info.getClassifierId(), info);
        }
        return infoMap;
    }

    /**
     * Register a new classifier. This initializes the associated {@link ClassifierSpec}
     * on the ML server, and stores information about the classifier in the classifier registry.
     */
    public <T extends Classifiable> Classifier<T> registerClassifier(ClassifierData<T> classifierData) throws ServiceException {
        // 1. Check that the label is not a duplicate
        String label = classifierData.getLabel();
        if (registry.labelExists(label)) {
            throw ServiceException.INVALID_REQUEST("classifier label '" + label + "' already exists", null);
        }
        // 2. try to register the spec with the ML server. This returns an ID.
        CreateClassifierQuery query = new CreateClassifierQuery(classifierData.getSpec());
        ClassifierInfo info = query.execute();
        Classifier<T> classifier = classifierData.create(info);
        // 3. Register the classifier with the registry using this ID.
        registry.register(classifier);
        return classifier;
    }

    /**
     * Return a Classifier for the given classifier ID.
     */
    @SuppressWarnings("unchecked")
    public <T extends Classifiable> Classifier<T> getClassifierById(String classifierId) throws ServiceException {
        Classifier<T> classifier = (Classifier<T>) registry.getById(classifierId);
        if (classifier == null) {
            throw ServiceException.NOT_FOUND("no classifier registered for ID " + classifierId, null);
        }
        return classifier;
    }

    /**
     * Return a Classifier for the given classifier ID.
     */
    public Classifier<?> getClassifierByLabel(String classifierLabel) throws ServiceException {
        Classifier<?> classifier = registry.getByLabel(classifierLabel);
        if (classifier == null) {
            throw ServiceException.NOT_FOUND("no classifier registered for label " + classifierLabel, null);
        }
        return classifier;
    }

    /**
     * Get all known classifiers
     */
    public Map<String, Classifier<?>> getAllClassifiers() throws ServiceException {
        return registry.getAllClassifiers();
    }

    /**
     * Delete a classifier from the ML server by its ID.
     */
    public <T extends Classifiable> Classifier<T> deleteClassifier(String classifierId) throws ServiceException {
        Classifier<T> toDelete = getClassifierById(classifierId);
        //1. Remove any task assignments for this classifier
        ClassificationTaskConfigProvider taskConfig = new LdapClassificationTaskConfigProvider();
        ClassificationExecutionContext<T> context = resolveConfig(taskConfig);
        ClassifierUsageInfo<T> usage = context.getClassifierUsage(toDelete);
        for (ClassificationTask<T> task: usage.getTasks()) {
            ZimbraLog.ml.info("removing classifier %s from task %s", toDelete.getLabel(), task.getTaskName());
            taskConfig.clearAssignment(task.getTaskName());
        }
        //2. Delete the classifier from the ML backend
        new DeleteClassifierQuery(classifierId).execute();
        Classifier<T> deleted = registry.delete(classifierId);
        //3. Remove the classifier from the local registry
        if (deleted == null) {
            ZimbraLog.ml.warn("No classifier with id=%s found in classifier registry", classifierId);
        }
        return deleted;
    }

    /**
     * Check whether a classifier already exists with the given label;
     */
    public boolean labelExists(String label) throws ServiceException {
        return registry.labelExists(label);
    }
}
