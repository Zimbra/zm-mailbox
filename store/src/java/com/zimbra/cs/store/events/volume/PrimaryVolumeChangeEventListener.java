package com.zimbra.cs.store.events.volume;

import com.zimbra.common.localconfig.ConfigException;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.cs.store.events.ZmEventListener;
import org.dom4j.DocumentException;

import static com.zimbra.common.localconfig.LC.zimbra_class_store;

public class PrimaryVolumeChangeEventListener implements ZmEventListener<PrimaryVolChangedEvent> {
    @Override
    public void onEvent(PrimaryVolChangedEvent primaryVolChangedEvent) {
//        String storeManagerClass = primaryVolChangedEvent.getVolume().getStoreType();
        String storeManagerClass = "some_class_path";
        try {
            LocalConfig localConfig = new LocalConfig(null);
            localConfig.set(zimbra_class_store.key(), storeManagerClass);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }

    }
}
