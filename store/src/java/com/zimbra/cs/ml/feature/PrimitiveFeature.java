package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * A feature for which the value is passed in through the constructor
 */
public class PrimitiveFeature<S> extends Feature<S> {

    private S value;

    public PrimitiveFeature(KnownFeature featureType, S value) {
        super(featureType);
        this.value = value;
    }

    @Override
    public S getFeatureValue() throws ServiceException {
        return value;
    }
}