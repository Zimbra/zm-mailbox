package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;

public class RatioFeature extends Feature<Double> {

    private Feature<Double> numerator;
    private Feature<Double> denominator;

    public RatioFeature(Feature<Double> numeratorFeature, Feature<Double> denominatorFeature) {
        super(numeratorFeature.getFeatureType());
        numerator = numeratorFeature;
        denominator = denominatorFeature;
    }

    @Override
    public Double getFeatureValue() throws ServiceException {
        return numerator.getFeatureValue() / denominator.getFeatureValue();
    }


}
