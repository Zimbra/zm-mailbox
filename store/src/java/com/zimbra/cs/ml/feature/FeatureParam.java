package com.zimbra.cs.ml.feature;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;

public class FeatureParam<T> extends Pair<FeatureParam.ParamKey, T> {

    public FeatureParam(ParamKey key, T value) {
        super(key, value);
    }

    public ParamKey getKey() {
        return getFirst();
    }

    public T getValue() {
        return getSecond();
    }

    @Override
    public String toString() {
        return String.format("%s:%s", getKey(), getValue());
    }


    public static enum ParamKey {
        FREQUENCY_TYPE("tp"),
        TIME_RANGE("tr"),
        METRIC_INITIALIZER("i"),
        RECIPIENT_TYPE("rt"),
        GLOBAL_RATIO("g"),
        NUMERATOR("n"),
        DENOMINATOR("d"),
        FROM_EVENT("fr"),
        TO_EVENT("to");

        private String abbrev;

        private ParamKey(String abbrev) {
            this.abbrev = abbrev;
        }

        public String getAbbrev() {
            return abbrev;
        }

        public static ParamKey of(String name) throws ServiceException {
            for (ParamKey key: ParamKey.values()) {
                if (key.name().equalsIgnoreCase(name) || key.abbrev.equalsIgnoreCase(name)) {
                    return key;
                }
            }
            throw ServiceException.INVALID_REQUEST(name + " is not a valid ParamKey", null);
        }
    }
}