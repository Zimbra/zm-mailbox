package com.zimbra.cs.ml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.ml.query.CreateClassifierQuery;
import com.zimbra.cs.ml.query.DeleteClassifierQuery;
import com.zimbra.cs.ml.query.GetClassifierQuery;
import com.zimbra.cs.ml.query.ListClassifiersQuery;
import com.zimbra.cs.ml.query.MLQuery;
import com.zimbra.cs.ml.query.MessageClassificationQuery;
import com.zimbra.cs.ml.query.TrainClassifierQuery;
import com.zimbra.cs.ml.schema.ClassifierInfo;
import com.zimbra.cs.ml.schema.ClassificationResult;

public abstract class MachineLearningBackend {

    private static Map<String, String> REGISTERED_FACTORIES = new HashMap<>();
    private static Factory factory;

    private Map<Class<? extends MLQuery<?>>, QueryCallback<?,?>> map;

    public static void registerFactory(String prefix, String clazz) {
        if (REGISTERED_FACTORIES.containsKey(prefix)) {
            ZimbraLog.index.warn(
                    "Replacing EventStore factory class '%s' registered for prefix '%s' with another factory class: '%s'",
                    REGISTERED_FACTORIES.get(prefix), prefix, clazz);
        }
        REGISTERED_FACTORIES.put(prefix, clazz);
    }

    public static Factory getFactory() throws ServiceException {
        if (factory == null) {
            String factoryClassName = null;
            String eventURL = Provisioning.getInstance().getLocalServer().getMachineLearningBackendURL();
            if (eventURL != null) {
                String[] tokens = eventURL.split(":");
                if (tokens != null && tokens.length > 0) {
                    String backendFactoryName = tokens[0];
                    factoryClassName = REGISTERED_FACTORIES.get(backendFactoryName);
                }
            } else {
                throw ServiceException.FAILURE("Machine Learning Backend is not configured", null);
            }
            setFactory(factoryClassName);
        }
        return factory;
    }

    public static void clearFactory() {
        factory = null;
    }

    public static void setFactory(String factoryClassName) throws ServiceException {
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionUtil.findClass(factoryClassName)
                            .asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    throw ServiceException.FAILURE("unable to find Machine Learning Backend Factory class " + factoryClassName, cnfe);
                }
            }
        } catch (ClassCastException cce) {
            ZimbraLog.event.error("unable to initialize Machine Learning Backend Factory class %s", factoryClassName, cce);
        }
        setFactory(factoryClass);
    }

    public static void setFactory(Class<? extends Factory> factoryClass) throws ServiceException {
        if (factoryClass == null) {
            throw ServiceException.FAILURE("Machine Learning Backend Factory class cannot be null", null);
        }
        String className = factoryClass.getName();
        ZimbraLog.search.debug("setting MachineLearningBackend.Factory class %s", className);
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE(String.format("unable to initialize Machine Learning Backend Factory %s", className), e);
        }
    }

    public MachineLearningBackend() {
        map = new HashMap<>();
        registerCallbacks();
    }

    @SuppressWarnings("unchecked")
    public <T extends MLQuery<Q>, Q> Q executeQuery(T query) throws ServiceException {
        QueryCallback<T, Q> callback = (QueryCallback<T, Q>) map.get(query.getClass());
        if (callback == null) {
            throw ServiceException.INVALID_REQUEST("no handler implemented for query " + query.getClass().getName(), null);
        }
        return callback.run(query);
    }

    protected void registerCallbacks() {
        addCallback(CreateClassifierQuery.class, getCreateClassifierCallback());
        addCallback(DeleteClassifierQuery.class, getDeleteClassifierCallback());
        addCallback(ListClassifiersQuery.class, getListClassifiersCallback());
        addCallback(TrainClassifierQuery.class, getTrainClassifierCallback());
        addCallback(MessageClassificationQuery.class, getClassificationCallback());
        addCallback(GetClassifierQuery.class, getGetClassifierCallback());
    }

    protected abstract QueryCallback<CreateClassifierQuery, ClassifierInfo> getCreateClassifierCallback();
    protected abstract QueryCallback<DeleteClassifierQuery, Boolean> getDeleteClassifierCallback();
    protected abstract QueryCallback<ListClassifiersQuery, List<ClassifierInfo>> getListClassifiersCallback();
    protected abstract QueryCallback<TrainClassifierQuery, ClassifierInfo> getTrainClassifierCallback();
    protected abstract QueryCallback<MessageClassificationQuery, ClassificationResult> getClassificationCallback();
    protected abstract QueryCallback<GetClassifierQuery, ClassifierInfo> getGetClassifierCallback();

    protected void addCallback(Class<? extends MLQuery<?>> queryClass, QueryCallback<?, ?> callback) {
        map.put(queryClass, callback);
    }

    public static abstract class QueryCallback<T extends MLQuery<Q>, Q> {

        public abstract Q run(T query) throws ServiceException;
    }

    public static interface Factory {
        public MachineLearningBackend getMachineLearningBackend() throws ServiceException;
    }
}
