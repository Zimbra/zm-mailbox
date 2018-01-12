package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.Event;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.EventDifferenceMetric.EventDifferenceParams;
import com.zimbra.cs.event.analytics.EventMetric.MetricType;
import com.zimbra.cs.event.analytics.RatioMetric;
import com.zimbra.cs.event.analytics.RatioMetric.RatioIncrement;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Factory that constructs a float MetricFeature for the ratio of read emails for the message sender
 */
public class EventRatioFeatureFactory extends MetricFeature.Factory<RatioMetric, Double, RatioIncrement> {

    private EventType numerator;
    private EventType denominator;

    public EventRatioFeatureFactory(EventType numerator, EventType denominator) throws ServiceException {
        setParams(new FeatureParams()
        .addParam(new FeatureParam<>(ParamKey.NUMERATOR, numerator))
        .addParam(new FeatureParam<>(ParamKey.DENOMINATOR, denominator)));
    }

    public EventRatioFeatureFactory() {}

    @Override
    public Feature<Double> buildFeature(Message msg) {
        EventDifferenceParams params = new EventDifferenceParams(numerator, denominator, msg.getSender());
        params.setInitializer(initializer);
        return new MetricFeature<>(KnownFeature.EVENT_RATIO, msg.getAccountId(), MetricType.EVENT_RATIO, params);
    }

    @Override
    public void setMetricParams(FeatureParams params) throws ServiceException {
        numerator = params.get(ParamKey.NUMERATOR, null);
        denominator = params.get(ParamKey.DENOMINATOR, null);
        if (numerator == null || denominator == null) {
            throw ServiceException.FAILURE("EventRatioFeatureFactory must have NUMERATOR and DENOMINATOR params", null);
        }
        if (numerator.getUniqueOn() != Event.UniqueOn.MESSAGE || denominator.getUniqueOn() != Event.UniqueOn.MESSAGE) {
            throw ServiceException.FAILURE("EventRatioFeatureFactory event types must have MESSAGE uniqueness scope", null);
        }
    }
}
