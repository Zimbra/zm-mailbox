package com.zimbra.cs.listeners;

import com.zimbra.cs.listeners.ListenerUtil.Priority;

public class DomainListenerEntry implements Comparable<DomainListenerEntry> {
    private String listenerName;
    private Priority priority;
    private DomainListener domainListener;

    public DomainListenerEntry(String listenerName, Priority priority, DomainListener domainListener) {
        this.listenerName = listenerName;
        this.priority = priority;
        this.domainListener = domainListener;
    }

    @Override
    public int compareTo(DomainListenerEntry other) {
        if (this.priority.ordinal() < other.priority.ordinal()) {
            return -1;
        } else if (this.priority.ordinal() > other.priority.ordinal()) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getListenerName() {
        return this.listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

    public DomainListener getDomainListener() {
        return this.domainListener;
    }

    public void setDomainListener(DomainListener domainListener) {
        this.domainListener = domainListener;
    }
}