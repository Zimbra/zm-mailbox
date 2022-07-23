package com.zimbra.cs.store.events;

public interface ZmEventPublisher<T> {
    void publishEvent(T t);
}
