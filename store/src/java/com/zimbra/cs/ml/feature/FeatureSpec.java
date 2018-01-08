package com.zimbra.cs.ml.feature;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BEncoding;
import com.zimbra.common.util.BEncoding.BEncodingException;
import com.zimbra.cs.event.Event.EventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyEventType;
import com.zimbra.cs.event.analytics.contact.ContactAnalytics.ContactFrequencyTimeRange;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.ml.Classifiable;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;
import com.zimbra.cs.ml.feature.NumRecipientsFeatureFactory.RecipientCountType;

/**
 * Provides the ability to concisely define a feature.
 * FeatureSpecs can be encoded/decoded as strings for easy persistence.
 */
public class FeatureSpec<T extends Classifiable> {

    private static final String KEY_FEATURE = "f";

    /**
     * Enum of known feature factories.
     * Only features registered here can be initialized with the FeatureSpec mechanism.
     */
    public static enum KnownFeature {
        IS_PART_OF_CONVERSATION("conv", ConversationFeatureFactory.class),
        RECIPIENT_FIELD("r", RecipientFieldFeatureFactory.class),
        NUM_RECIPIENTS("nr", RecipientFieldFeatureFactory.class),
        SENT_FREQUENCY("sf", ContactFrequencyFeatureFactory.class, new FeatureParam<ContactFrequencyEventType>(ParamKey.FREQUENCY_TYPE, ContactFrequencyEventType.SENT)),
        RECEIVED_FREQUENCY("rf", ContactFrequencyFeatureFactory.class, new FeatureParam<ContactFrequencyEventType>(ParamKey.FREQUENCY_TYPE, ContactFrequencyEventType.RECEIVED)),
        COMBINED_FREQUENCY("cf", ContactFrequencyFeatureFactory.class, new FeatureParam<ContactFrequencyEventType>(ParamKey.FREQUENCY_TYPE, ContactFrequencyEventType.COMBINED)),
        TIME_DELTA("td", EventTimeDeltaFeatureFactory.class),
        EVENT_RATIO("er", EventRatioFeatureFactory.class);

        private String abbrev;
        private Class<? extends FeatureFactory<?, ?>> featureFactoryClass;
        private FeatureParams featureParams;

        private KnownFeature(String abbrev, Class<? extends FeatureFactory<Message, ?>> klass, FeatureParam<?>... initParams) {
            this.abbrev = abbrev;
            this.featureFactoryClass = klass;
            this.featureParams = new FeatureParams();
            for (FeatureParam<?> param: initParams) {
                featureParams.addParam(param);
            }
        }

        public void checkTypeCompatability(Class<? extends Classifiable> klass) throws ServiceException {

        }

        @SuppressWarnings("unchecked")
        public <T extends Classifiable> Class<? extends FeatureFactory<T, ?>> getFeatureFactoryClass() {
            return (Class<? extends FeatureFactory<T, ?>>) featureFactoryClass;
        }

        public FeatureParams getParams() {
            return featureParams;
        }

        public String getAbbrev() {
            return abbrev;
        }

        public static KnownFeature of(String name) throws ServiceException {
            for (KnownFeature type: values()) {
                if (type.name().equalsIgnoreCase(name) || type.abbrev.equalsIgnoreCase(name)) {
                    return type;
                }
            }
            throw ServiceException.INVALID_REQUEST(name + " is not a known feature type", null);
        }
    }
    private KnownFeature feature;
    private FeatureParams otherParams = new FeatureParams();

    public FeatureSpec(KnownFeature feature) {
        this.feature = feature;
    }

    public FeatureSpec<T> addParam(FeatureParam<? >param) {
        otherParams.addParam(param);
        return this;
    }

    public KnownFeature getFeature() {
        return feature;
    }

    public FeatureParams getParams() {
        return otherParams;
    }

    /**
     * Return a FeatureFactory instance based on this spec.
     */
    public FeatureFactory<T, ?> buildFactory() throws ServiceException {
        KnownFeature featureType = getFeature();
        Class<? extends FeatureFactory<T, ?>> factoryClass = featureType.getFeatureFactoryClass();
        FeatureFactory<T, ?> factory;
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw ServiceException.FAILURE("unable to initialize class " + factoryClass.getName(), e);
        }
        FeatureParams params = feature.getParams();
        params.merge(otherParams);
        factory.setParams(params);
        return factory;
    }

    public String encode() {
        Map<Object, Object> map = new HashMap<>();
        map.put(KEY_FEATURE, feature.getAbbrev());
        for (FeatureParam<?> param: otherParams.getParams()) {
            map.put(param.getKey().getAbbrev(), param.getValue());
        }
        return BEncoding.encode(map);
    }

    public FeatureSpec(String encoded) throws ServiceException {
        decode(encoded);
    }

    private void decode(String encoded) throws ServiceException {
        Map<String, Object> map;
        try {
             map = BEncoding.decode(encoded);
        } catch (BEncodingException e) {
            throw ServiceException.FAILURE(encoded + " is not a valid FeatureSet encoding", e);
        }

        for (Map.Entry<String, Object> entry: map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (key.equals(KEY_FEATURE)) {
                feature = KnownFeature.of((String) val);
            } else {
                try {
                    FeatureParam<?> param;
                    ParamKey paramKey = ParamKey.of(key);
                    switch (paramKey) {
                    case FREQUENCY_TYPE:
                        param = new FeatureParam<ContactFrequencyEventType>(paramKey, ContactFrequencyEventType.valueOf((String) val));
                        break;
                    case TIME_RANGE:
                        param = new FeatureParam<ContactFrequencyTimeRange>(paramKey, ContactFrequencyTimeRange.valueOf((String) val));
                        break;
                    case RECIPIENT_TYPE:
                        param = new FeatureParam<RecipientCountType>(paramKey, RecipientCountType.of((String) val));
                        break;
                    case FROM_EVENT:
                    case TO_EVENT:
                    case NUMERATOR:
                    case DENOMINATOR:
                        param = new FeatureParam<EventType>(paramKey, EventType.of((String) val));
                        break;
                    default:
                        param = new FeatureParam<String>(paramKey, (String) val);
                    }
                    addParam(param);
                } catch (IllegalArgumentException e) {
                    throw ServiceException.FAILURE((String) val + " is not a valid value for ParamKey " + key, null);
                }
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FeatureSpec) {
            FeatureSpec<?> otherSpec = (FeatureSpec<?>) other;
            if (getFeature() != otherSpec.getFeature()) {
                return false;
            } else if (getParams().equals(otherSpec.getParams())) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(feature.name(), otherParams);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("feature", feature)
                .add("params", otherParams).toString();
    }
}