package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.event.analytics.ContactFrequencyMetric.ContactFrequencyParams;
import com.zimbra.cs.event.analytics.EventMetric.MetricType;
import com.zimbra.cs.event.analytics.ValueMetric;
import com.zimbra.cs.event.analytics.ValueMetric.IntIncrement;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyEventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.FeatureSpec.KnownFeature;

/**
 * Factory that constructs an integer MetricFeature representing the contact frequency
 * for the message sender for the specified time range and type.
 */
public class ContactFrequencyFeatureFactory extends MetricFeature.Factory<ValueMetric, Integer, IntIncrement> {

    private ContactFrequencyTimeRange timeRange;
    private ContactFrequencyEventType eventType;

    public ContactFrequencyFeatureFactory() {}

    public ContactFrequencyFeatureFactory(ContactFrequencyTimeRange timeRange, ContactFrequencyEventType eventType) throws ServiceException {
        setParams(new FeatureParams()
        .addParam(new FeatureParam<>(ParamKey.TIME_RANGE, timeRange))
        .addParam(new FeatureParam<>(ParamKey.FREQUENCY_TYPE, eventType)));
    }

    private KnownFeature getFeatureType() {
        switch (eventType) {
        case RECEIVED:
            return KnownFeature.RECEIVED_FREQUENCY;
        case SENT:
            return KnownFeature.SENT_FREQUENCY;
        case COMBINED:
        default:
            return KnownFeature.COMBINED_FREQUENCY;
        }
    }

    @Override
    public Feature<Integer> buildFeature(Message msg) {
        ContactFrequencyParams params = new ContactFrequencyParams(msg.getSender(), timeRange, eventType);
        params.setInitializer(initializer);
        return new MetricFeature<>(getFeatureType(), msg.getAccountId(), MetricType.CONTACT_FREQUENCY, params);
    }

    @Override
    public void setMetricParams(FeatureParams params) throws ServiceException {
        ContactFrequencyTimeRange timeRange = params.get(ParamKey.TIME_RANGE, (ContactFrequencyTimeRange) null);
        ContactFrequencyEventType eventType = params.get(ParamKey.FREQUENCY_TYPE, (ContactFrequencyEventType) null);
        if (timeRange == null ){
            missingParam(ParamKey.TIME_RANGE);
        } else {
            this.timeRange = timeRange;
        }
        if (eventType == null) {
            missingParam(ParamKey.FREQUENCY_TYPE);
        } else {
            this.eventType = eventType;
        }
    }
}
