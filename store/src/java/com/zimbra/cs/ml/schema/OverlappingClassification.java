package com.zimbra.cs.ml.schema;

import com.zimbra.common.util.Pair;

public class OverlappingClassification extends Pair<String, Float> {

    public OverlappingClassification(String label, Float prob) {
        super(label, prob);
    }

    public String getLabel() {
        return getFirst();
    }

    public float getProb() {
        return getSecond();
    }
}
