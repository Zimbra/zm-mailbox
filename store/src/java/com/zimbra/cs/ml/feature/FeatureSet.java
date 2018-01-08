package com.zimbra.cs.ml.feature;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ArrayListMultimap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Class representing features to be used for classification.
 */
public class FeatureSet<T extends Classifiable> {

    private List<FeatureSpec<T>> featureList;
    private ArrayListMultimap<KnownFeature, FeatureSpec<T>> featureMap;

    public FeatureSet() {
        featureMap = ArrayListMultimap.create();
        featureList = new ArrayList<>();
    }

    /**
     * Add a feature specification to this feature set.
     * Features will be generated in the same order that their FeatureSpecs
     * were added using this method.
     */
    public void addFeatureSpec(FeatureSpec<T> spec) throws ServiceException {
        featureMap.put(spec.getFeature(), spec);
        featureList.add(spec);
    }

    private void addFeature(ComputedFeatures<T> features, T item, FeatureFactory<T, ?> factory) {
        try {
            Feature<?> feature = factory.buildFeature(item);
            features.add(feature);
            Object value = feature.getFeatureValue();
            ZimbraLog.ml.debug("generated feature %s with value %s", feature, value);
        } catch (ServiceException e) {
            ZimbraLog.ml.error("Unable to generate feature from factory %s", factory.getClass().getSimpleName(), e);
        }
    }

    @VisibleForTesting
    List<FeatureFactory<T, ?>> buildFactories() throws ServiceException {
        List<FeatureFactory<T, ?>> factories = new ArrayList<FeatureFactory<T, ?>>();
        for (FeatureSpec<T> spec: featureList) {
            factories.add(spec.buildFactory());
        }
        return factories;
    }

    public ComputedFeatures<T> getFeatures(T item) throws ServiceException {
        ComputedFeatures<T> msgFeatures = new ComputedFeatures<T>(item);
        for (FeatureFactory<T, ?> factory: buildFactories()) {
            addFeature(msgFeatures, item, factory);
        }
        return msgFeatures;
    }

    /**
     * Return a list of all FeatureSpecs in this FeatureSet
     */
    public List<FeatureSpec<T>> getAllFeatureSpecs() {
        return featureList;
    }

    /**
     * Return a list of all FeatureSpecs in this FeatureSet
     * that match the given {@link KnownFeature} type
     */
    public List<FeatureSpec<T>> getFeatureSpecs(KnownFeature feature) {
        return featureMap.get(feature);
    }

    public int getNumFeatures() {
        return featureList.size();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("features", featureList).toString();
    }
}
