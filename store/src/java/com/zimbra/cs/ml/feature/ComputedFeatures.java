package com.zimbra.cs.ml.feature;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.ml.Classifiable;

/**
 * Class representing features computed for a given classifiable item
 */
public class ComputedFeatures<T extends Classifiable> {

    private T item;
    private List<Feature<?>> features;

    public ComputedFeatures(T item) {
        this.item = item;
        this.features = new ArrayList<>();
    }

    public void add(Feature<?> feature) {
        features.add(feature);
    }

    public List<Feature<?>> getFeatures() {
        return features;
    }

    public T getItem() {
        return item;
    }
}