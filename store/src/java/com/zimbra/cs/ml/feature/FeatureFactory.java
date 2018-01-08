package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;

/**
 * Abstract class used for generating {@link Feature} instances for a given message
 *
 * @param <T> The type of Feature this factory returns
 */
public abstract class FeatureFactory<C extends Classifiable, T> {

    /**
     * No-argument constructor used to construct a factory from specs
     */
    public FeatureFactory() {}

    public abstract void setParams(FeatureParams params) throws ServiceException;

    public abstract Feature<T> buildFeature(C item) throws ServiceException;

    protected void missingParam(ParamKey missingKey) throws ServiceException {
        throw ServiceException.INVALID_REQUEST("FeatureFactory " + this.getClass().getSimpleName() + " is missing required param " + missingKey.name(), null);
    }
}
