package com.zimbra.cs.store.events.volume;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.store.events.ZmEventListener;
import com.zimbra.cs.store.events.ZmEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class PrimaryVolChangeEventPublisher implements ZmEventPublisher<PrimaryVolChangedEvent> {

    private static PrimaryVolChangeEventPublisher INSTANCE = new PrimaryVolChangeEventPublisher();
    private List<ZmEventListener> listeners = new ArrayList<>();

    private PrimaryVolChangeEventPublisher() {

    }

    public static PrimaryVolChangeEventPublisher getInstance() {
        return INSTANCE;
    }

    @Override
    public void publishEvent(PrimaryVolChangedEvent primaryVolChangedEvent) {
        ZimbraLog.store.info("publishing event PrimaryVolChangedEvent, listenersCount=" + listeners.size());
        if (!listeners.isEmpty()) {
            listeners.stream().forEach(listener -> listener.onEvent(primaryVolChangedEvent));
        }
    }

    public void registerListener(ZmEventListener eventListener) {
        listeners.add(eventListener);
    }
}
