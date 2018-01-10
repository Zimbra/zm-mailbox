package com.zimbra.cs.ml.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.Classifiable;

public abstract class OverlappingClassCallback<C extends Classifiable> extends ClassificationCallback<C> {

    private float threshold;
    private String overlappingClass;

    public OverlappingClassCallback(String label) {
        this(label, 0);
    }

    public OverlappingClassCallback(String overlappingClass, float threshold) {
        this.overlappingClass = overlappingClass;
        this.threshold = threshold;
    }

    public String getOverlappingClass() {
        return overlappingClass;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getThreshold() {
        return threshold;
    }

    public abstract void handle(C item) throws ServiceException;
}