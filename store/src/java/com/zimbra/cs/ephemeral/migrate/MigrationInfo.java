package com.zimbra.cs.ephemeral.migrate;

import java.util.Date;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public abstract class MigrationInfo {

    private static Factory factory;

    public static enum Status { NONE, IN_PROGRESS, COMPLETED, FAILED }

    protected MigrationInfo.Status status = Status.NONE;
    protected String URL;
    protected Date dateStarted;

    public void beginMigration() throws ServiceException {
        setStatus(Status.IN_PROGRESS);
        dateStarted = new Date();
        save();
    }

    public void endMigration() throws ServiceException {
        status = Status.COMPLETED;
        save();
    }

    public void migrationFailed() throws ServiceException {
        status = Status.FAILED;
        save();
    }

    public void setStatus(MigrationInfo.Status status) { this.status = status; }
    public MigrationInfo.Status getStatus() { return status; }
    public void setURL(String URL) { this.URL = URL; }
    public String getURL() { return URL; }
    public Date getDate() { return dateStarted; }

    /**
     * Clear all migration data
     * @throws ServiceException
     */
    public void clearData() throws ServiceException {
        this.status = Status.NONE;
        this.URL = null;
        this.dateStarted = null;
        clearSavedData();
    }

    /**
     * Reset the saved migration data
     * @throws ServiceException
     */
    protected abstract void clearSavedData() throws ServiceException;

    /**
     * Save the current state of migration
     * @throws ServiceException
     */
    public abstract void save() throws ServiceException;

    public static Factory getFactory() {
        if (factory == null) {
            setFactory(ZimbraMigrationInfo.Factory.class);
        }
        return factory;
    }

    public static void setFactory(Class<? extends Factory> factoryClass) {

        Factory factory;
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ZimbraLog.ephemeral.error("unable to instantiate Factory %s, using ZimbraAttributeInfo", factoryClass.getName(), e);
            factory = new ZimbraMigrationInfo.Factory();
        }
        MigrationInfo.factory = factory;
    }

    public static interface Factory {
        public MigrationInfo getInfo() throws ServiceException;
    }

}