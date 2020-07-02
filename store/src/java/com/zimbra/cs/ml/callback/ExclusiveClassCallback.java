package com.zimbra.cs.ml.callback;

import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ml.Classifiable;

public abstract class ExclusiveClassCallback<C extends Classifiable> extends ClassificationCallback<C> {

    private Set<String> exclusiveClasses;

    public ExclusiveClassCallback(String... exclusiveClasses) {
        this(Sets.newHashSet(exclusiveClasses));
    }

    public ExclusiveClassCallback(Set<String> exclusiveClasses) {
        this.exclusiveClasses = exclusiveClasses;
    }

    public Set<String> getExclusiveClasses() {
        return exclusiveClasses;
    }

    public abstract void handle(C item, String exclusiveClassLabel) throws ServiceException;
}