package com.zimbra.cs.ml.feature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;

import com.google.common.base.Joiner;
import com.zimbra.cs.ml.feature.FeatureParam.ParamKey;

public class FeatureParams {

    private Map<ParamKey, Object> paramMap = new HashMap<>();
    private List<FeatureParam<?>> paramList = new ArrayList<>();

    public FeatureParams addParam(FeatureParam<?> param) {
        paramMap.put(param.getKey(), param.getValue());
        paramList.add(param);
        return this;
    }

    public <T> T get(ParamKey key, T defaultVal) {
        return paramMap.containsKey(key) ? (T) paramMap.get(key) : defaultVal;
    }

    public FeatureParams merge(FeatureParams other) {
        paramMap.putAll(other.paramMap);
        return this;
    }

    public List<FeatureParam<?>> getParams() {
        return paramList;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FeatureParams) {
            FeatureParams otherParams = (FeatureParams) other;
            return CollectionUtils.isEqualCollection(paramList, otherParams.paramList);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(paramList.toArray(new FeatureParam[paramList.size()]));
    }

    public int getNumParams() {
        return paramList.size();
    }

    @Override
    public String toString() {
        return String.format("{%s}", Joiner.on(", ").join(paramList));
    }
}