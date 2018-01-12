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

public class EventTimeDeltaFeatureFactory  extends MetricFeature.Factory<RatioMetric, Double, RatioIncrement> {

    private boolean globalRatio;
    private EventType fromEvent;
    private EventType toEvent;

    public EventTimeDeltaFeatureFactory() {}

    public EventTimeDeltaFeatureFactory(EventType fromEvent, EventType toEvent) throws ServiceException {
        this(fromEvent, toEvent, false);
    }

    public EventTimeDeltaFeatureFactory(EventType fromEvent, EventType toEvent, boolean globalRatio) throws ServiceException {
        setParams(new FeatureParams()
        .addParam(new FeatureParam<>(ParamKey.FROM_EVENT, fromEvent))
        .addParam(new FeatureParam<>(ParamKey.TO_EVENT, toEvent))
        .addParam(new FeatureParam<>(ParamKey.GLOBAL_RATIO, globalRatio)));
    }

    @Override
    public Feature<Double> buildFeature(Message msg) {
        EventDifferenceParams params = new EventDifferenceParams(fromEvent, toEvent, msg.getSender());
        params.setInitializer(initializer);
        Feature<Double> contactTimeToOpen = new MetricFeature<>(KnownFeature.TIME_DELTA, msg.getAccountId(), MetricType.TIME_DELTA, params);
        if (globalRatio) {
            EventDifferenceParams globalParams = new EventDifferenceParams(fromEvent, toEvent);
            params.setInitializer(initializer);
            Feature<Double> globalTimeToOpen = new MetricFeature<>(KnownFeature.TIME_DELTA, msg.getAccountId(), MetricType.TIME_DELTA, globalParams);
            return new RatioFeature(contactTimeToOpen, globalTimeToOpen);
        } else {
            return contactTimeToOpen;
        }
    }

    @Override
    public void setMetricParams(FeatureParams params) throws ServiceException {
        globalRatio = params.get(ParamKey.GLOBAL_RATIO, false);
        fromEvent = params.get(ParamKey.FROM_EVENT, null);
        toEvent = params.get(ParamKey.TO_EVENT, null);
        if (fromEvent == null || toEvent == null) {
            throw ServiceException.FAILURE("EventTimeDeltaFeatureFactory must have FROM_EVENT and TO_EVENT params", null);
        }
        if (fromEvent.getUniqueOn() != Event.UniqueOn.MESSAGE || toEvent.getUniqueOn() != Event.UniqueOn.MESSAGE) {
            throw ServiceException.FAILURE("EventTimeDeltaFeatureFactory event types must have MESSAGE uniqueness scope", null);
        }
    }
}