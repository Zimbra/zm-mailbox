package com.zimbra.cs.store.events.volume;

import com.zimbra.cs.store.events.ZmStoreEvent;
import com.zimbra.cs.volume.Volume;

public class PrimaryVolChangedEvent implements ZmStoreEvent {
    private Volume volume;

    public PrimaryVolChangedEvent(Volume volume) {
        this.volume = volume;
    }

    public Volume getVolume() {
        return volume;
    }
}
