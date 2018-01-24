package com.zimbra.cs.ephemeral.migrate;

import com.zimbra.common.service.ServiceException;

public class InMemoryMigrationInfo extends MigrationInfo {

    @Override
    protected void clearSavedData() throws ServiceException {
        //nothing to do here, since data is not persisted outside the singleton
    }

    @Override
    public void save() throws ServiceException {
        //nothing to do here, since data is not persisted outside the singleton
    }

    public static class Factory implements MigrationInfo.Factory {

        private MigrationInfo singleton;

        @Override
        public MigrationInfo getInfo() throws ServiceException {
            synchronized (Factory.class) {
                if (singleton == null) {
                    singleton = new InMemoryMigrationInfo();
                }
            }
            return singleton;
        }
    }
}