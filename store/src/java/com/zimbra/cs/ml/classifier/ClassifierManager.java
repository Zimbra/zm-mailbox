package com.zimbra.cs.ml.classifier;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ml.Classifiable;
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

    private static ClassifierManager instance = null;

    private ClassifierManager() throws ServiceException {
        this(new LdapClassifierRegistry());
    }

    private ClassifierManager(ClassifierRegistry registry) {
        this.registry = registry;
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
        new DeleteClassifierQuery(classifierId).execute();
        Classifier<T> deleted = registry.delete(classifierId);
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
