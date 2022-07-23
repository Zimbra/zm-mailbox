package com.zimbra.cs.store.events;

public interface ZmEventListener<T> {
    public void onEvent(T t);
}
