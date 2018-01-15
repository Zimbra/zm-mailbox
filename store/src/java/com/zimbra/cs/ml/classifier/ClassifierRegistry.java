package com.zimbra.cs.ml.classifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.classifier.Classifier.ClassifiableType;
import com.zimbra.cs.ml.feature.FeatureSet;
import com.zimbra.cs.ml.feature.FeatureSpec;
import com.zimbra.cs.ml.schema.ClassifierInfo;

/**
 * Registry of classifiers known to be initialized on the ML server.
 * Subclasses handle persisting/loading classifier info.
 */
public abstract class ClassifierRegistry {

    private static final String KEY_ID = "i";
    private static final String KEY_LABEL = "l";
    private static final String KEY_FEATURES = "f";
    private static final String KEY_DESCRIPTION = "d";
    private static final String KEY_TYPE = "t";

    private Map<String, Classifier<?>> idMap;
    private Map<String, Classifier<?>> labelMap;
    private boolean loaded = false;

    public ClassifierRegistry() {
        idMap = new HashMap<>();
        labelMap = new HashMap<>();
    }

    private synchronized void loadKnownClassifiers() throws ServiceException {
        String[] encodedClassifiers = load();
        ZimbraLog.ml.info("found known %d classifiers in %s", encodedClassifiers.length, this.getClass().getSimpleName());
        Map<String, ClassifierInfo> infoMap = ClassifierManager.getInstance().getAllClassifierInfo();
        ZimbraLog.ml.debug("found %d classifiers registered with ML backend", infoMap.size());
        for (String encoded: encodedClassifiers) {
            Classifier<?> decoded = decode(encoded);
            String id = decoded.getId();
            ClassifierInfo info = infoMap.get(id);
            if (info == null) {
                ZimbraLog.ml.warn("known classifier %s (id=%s) has no entry in ML backend, deleting", decoded.getLabel(), id);
                deRegister(encoded);
            } else {
                ZimbraLog.ml.debug("loaded classifier label=%s id=%s", decoded.getLabel(), id);
                infoMap.remove(id);
                idMap.put(id, decoded);
                labelMap.put(decoded.getLabel(), decoded);
                decoded.setClassifierInfo(info);
            }
        }
        if (!infoMap.isEmpty()) {
            for (String cId: infoMap.keySet()) {
                ZimbraLog.ml.warn("classifier id=%s has no corresponding entry in %s", cId, this.getClass().getSimpleName());
            }
        }
        loaded = true;
    }

    public Map<String, Classifier<?>> getAllClassifiers() throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        return idMap;
    }

    public boolean labelExists(String label) throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        return labelMap.containsKey(label);
    }

    /**
     * return known encoded classifiers
     */
    protected abstract String[] load() throws ServiceException;

    /**
     * Persist the encoded classifier string
     */
    protected abstract void save(String encodedClassifier) throws ServiceException;

    /**
     * Register a classifier
     */
    public void register(Classifier<?> classifier) throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        if (labelExists(classifier.getLabel())) {
            throw ServiceException.FAILURE("label '" + classifier.getLabel() + "' already exists", null);
        }
        idMap.put(classifier.getId(), classifier);
        labelMap.put(classifier.getLabel(), classifier);
        save(encode(classifier));
    }

    protected String encode(Classifier<?> classifier) {
        Map<String, Object> map = new HashMap<>();
        map.put(KEY_ID, classifier.getId());
        map.put(KEY_TYPE, classifier.getType().name());
        map.put(KEY_LABEL, classifier.getLabel());
        map.put(KEY_FEATURES, classifier.getFeatureSet().getAllFeatureSpecs().stream().map(feature -> ((FeatureSpec<?>) feature).encode()).collect(Collectors.toList()));
        map.put(KEY_DESCRIPTION, classifier.getDescription());
        return BEncoding.encode(map);
    }

    protected Classifier<?> decode(String encoded) throws ServiceException {
        Map<String, Object> map;
        try {
            map = BEncoding.decode(encoded);
        } catch (BEncodingException e) {
            throw ServiceException.FAILURE("unable to decode classifier with encoded value %s" + encoded, null);
        }
        if (!map.containsKey(KEY_TYPE)) {
            throw ServiceException.FAILURE("no ClassifiableType value found during decoding classifier", null);

        }
        String typeStr = (String) map.get(KEY_TYPE);
        ClassifiableType type = null;
        try {
            type = ClassifiableType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            throw ServiceException.FAILURE("invalid ClassifiableType value encountered during decoding classifier: " + typeStr, null);
        }
        //this may seem unnecessary now, but it allows us to add classifiers for things other than messages in the future
        switch (type) {
        case MESSAGE:
            return decodeMessageClassifier(map);
        default:
            throw ServiceException.FAILURE(String.format("unknown ClassifiableType %s; only MESSAGE is supported at this time", type), null);
        }
    }

    @SuppressWarnings("unchecked")
    private Classifier<Message> decodeMessageClassifier(Map<String, Object> map) throws ServiceException {
        String label = (String) map.get(KEY_LABEL);
        String id = (String) map.get(KEY_ID);
        String description = map.containsKey(KEY_DESCRIPTION) ? (String) map.get(KEY_DESCRIPTION) : null;
        FeatureSet<Message> featureSet = new FeatureSet<>();
        for (String encodedFeatureSpec: (List<String>) map.get(KEY_FEATURES)) {
            try {
                featureSet.addFeatureSpec(new FeatureSpec<Message>(encodedFeatureSpec));
            } catch (ServiceException e) {
                ZimbraLog.ml.warn("problem decoding feature for classifier %s", label, e);
            }
        }
        Classifier<Message> classifier = new MessageClassifier(id, label, featureSet);
        if (description != null) {
            classifier.setDescription(description);
        }
        return classifier;
    }

    public Classifier<?> getById(String classifierId) throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        if (!idMap.containsKey(classifierId)) {
            throw ServiceException.FAILURE(String.format("no classifier found with id=%s", classifierId), null);
        }
        return idMap.get(classifierId);
    }

    public Classifier<?> getByLabel(String classifierLabel) throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        if (!labelMap.containsKey(classifierLabel)) {
            throw ServiceException.FAILURE(String.format("no classifier found with label=%s", classifierLabel), null);
        }
        return labelMap.get(classifierLabel);
    }

    @SuppressWarnings("unchecked")
    public <T extends Classifiable> Classifier<T> delete(String classifierId) throws ServiceException {
        if (!loaded) {
            loadKnownClassifiers();
        }
        Classifier<T> classifier = (Classifier<T>) idMap.remove(classifierId);
        if (classifier != null) {
            deRegister(encode(classifier));
            labelMap.remove(classifier.getLabel());
        }
        return classifier;
    }

    protected abstract void deRegister(String encoded) throws ServiceException;
}