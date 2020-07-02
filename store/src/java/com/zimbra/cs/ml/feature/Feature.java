package com.zimbra.cs.ml.feature;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;


/**
 * A feature used as input into a classifer
 */
public abstract class Feature<T> {

    private KnownFeature featureType;

    public Feature(KnownFeature featureType) {
        this.featureType = featureType;
    }

    public KnownFeature getFeatureType() {
        return featureType;
    }

    public abstract T getFeatureValue() throws ServiceException;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", featureType)
                .add("class", this.getClass().getSimpleName()).toString();
    }
}
