package com.zimbra.cs.ml;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ml.callback.ExclusiveClassCallback;
import com.zimbra.cs.ml.callback.OverlappingClassCallback;
import com.zimbra.cs.ml.schema.ClassificationResult;
import com.zimbra.cs.ml.schema.OverlappingClassification;

/**
 * This class reads classification results represented by {@link TextClasses}
 * and invokes callbacks based on the class labels.
 */
public class ClassificationHandler<C extends Classifiable> {

    private ExclusiveClassCallback<C> exclusiveCallback;
    private Map<String, OverlappingClassCallback<C>> overlappingCallbacks;

    public ClassificationHandler() {
        this.overlappingCallbacks = new HashMap<>();
    }

    public boolean hasExclusiveCallback() {
        return exclusiveCallback != null;
    }

    public int getNumOverlappingClassCallbacks() {
        return overlappingCallbacks.size();
    }

    public ExclusiveClassCallback<C> getExclusiveClassCallback() {
        return exclusiveCallback;
    }

    public Map<String, OverlappingClassCallback<C>> getOverlappingClassCallbacks() {
        return overlappingCallbacks;
    }

    public ClassificationHandler(ExclusiveClassCallback<C> exclusiveCallback, Map<String, OverlappingClassCallback<C>> overlappingCallbacks) {
        this.exclusiveCallback = exclusiveCallback;
        this.overlappingCallbacks = new HashMap<>();
        if (overlappingCallbacks != null) {
            this.overlappingCallbacks.putAll(overlappingCallbacks);
        }
    }

    public void setExclusiveClassCallback(ExclusiveClassCallback<C> callback) {
        this.exclusiveCallback = callback;
    }

    public void addOverlappingClassCallback(OverlappingClassCallback<C> callback) {
        overlappingCallbacks.put(callback.getOverlappingClass().toLowerCase(), callback);
    }

    public void handle(C item, ClassificationResult classification) throws ServiceException {
        String exclusiveClass = classification.getExclusiveClass();
        if (exclusiveClass != null && exclusiveCallback != null) {
            ZimbraLog.ml.debug("executing %s for exclusive class [%s]", exclusiveCallback.getClass().getSimpleName(), exclusiveClass);
            exclusiveCallback.handle(item, exclusiveClass);
        }
        OverlappingClassification[] overlapping = classification.getOverlappingClasses();
        if (overlapping != null) {
            for (OverlappingClassification c: overlapping) {
                OverlappingClassCallback<C> callback = overlappingCallbacks.get(c.getLabel().toLowerCase());
                if (callback == null) {
                    ZimbraLog.ml.debug("no callback specified for overlapping class [%s]", c.getLabel());
                } else {
                    float threshold = callback.getThreshold();
                    if (threshold == 0) {
                        ZimbraLog.ml.debug("probability threshold not set for overlapping class [%s]", c.getLabel());
                        continue;
                    } else if (c.getProb() >= threshold) {
                        ZimbraLog.ml.debug("prob %.2f > threshold %.2f, executing %s for overlapping class [%s]", c.getProb(), threshold, callback.getClass().getSimpleName(), callback.getOverlappingClass());
                        callback.handle(item);
                    }
                }
            }
        }
    }
}
