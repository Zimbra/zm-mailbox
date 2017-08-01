package com.zimbra.cs.account.callback;

import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.MigrationStateHelper.Reason;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.extension.ExtensionUtil;

public class EphemeralBackendCheck extends AttributeCallback {

    private static MigrationStateHelper.Factory helperFactory;
    static {
        setHelperFactory(new ZimbraMigrationStateHelper.Factory());
    }

    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        if (attrName.equalsIgnoreCase(Provisioning.A_zimbraEphemeralBackendURL)) {
            String url = (String) attrValue;
            String[] tokens = url.split(":");
            if (tokens != null && tokens.length > 0) {
                String backend = tokens[0];
                if (backend.equalsIgnoreCase("ldap")) {
                    checkMigration(url);
                    savePreviousUrl(context, entry);
                    EphemeralStore.clearFactory();
                    return;
                }
                EphemeralStore.Factory factory = EphemeralStore.getFactory(backend);
                if (factory == null) {
                    // Probably called from zmprov in LDAP mode, so need to setup any Ephemeral Store extensions
                    Level savedEphem = ZimbraLog.ephemeral.getLevel();
                    Level savedExten = ZimbraLog.extensions.getLevel();
                    try {
                        // suppress logging in zmprov output
                        ZimbraLog.ephemeral.setLevel(Level.error);
                        ZimbraLog.extensions.setLevel(Level.error);
                        ExtensionUtil.initAllMatching(new EphemeralStore.EphemeralStoreMatcher(backend));
                    } finally {
                        ZimbraLog.ephemeral.setLevel(savedEphem);
                        ZimbraLog.extensions.setLevel(savedExten);
                    }
                    factory = EphemeralStore.getFactory(backend);
                }
                if (factory == null) {
                    throw ServiceException.FAILURE(String.format(
                            "unable to modify %s; no factory found for backend '%s'", attrName, backend), null);
                }
                checkMigration(url);
                try {
                    factory.test(url);
                    EphemeralStore.clearFactory();
                } catch (ServiceException e) {
                    throw ServiceException.FAILURE(String.format("cannot set zimbraEphemeralBackendURL to %s", url), e);
                }
            } else {
                throw ServiceException.FAILURE(String.format(
                        "unable to modify %s; no ephemeral backend specified", attrName), null);
            }
            savePreviousUrl(context, entry);
        }
    }

    public static void setHelperFactory(MigrationStateHelper.Factory factory) {
        helperFactory = factory;
    }


    private void checkMigration(String URL) throws ServiceException {
        MigrationStateHelper helper = helperFactory.getHelper();
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL(URL);
    }

    private void resetMigrationInfo(String newURL) throws ServiceException {
        // Clear out the migration info if the backend URL we migrated to matches the URL used for migration,
        // even if the previous migration was not successful
        MigrationInfo info = AttributeMigration.getMigrationInfo();
        String migrationURL = info.getURL();
        if (newURL.equals(migrationURL)) {
            info.clearData();
        }
    }

    private void savePreviousUrl(CallbackContext context, Entry entry) {
        String prevUrl = ((Config) entry).getEphemeralBackendURL();
        if (!Strings.isNullOrEmpty(prevUrl)) {
            context.setData(DataKey.PREV_EPHEMERAL_BACKEND_URL, prevUrl);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        String prevUrl = context.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL);
        Config config = (Config) entry;
        try {
            config.setPreviousEphemeralBackendURL(prevUrl);
        } catch (ServiceException e) {
            ZimbraLog.ephemeral.error("unable to set zimbraPreviousEphemeralBackendURL to %s", prevUrl, e);
        }
        try {
            resetMigrationInfo(config.getEphemeralBackendURL());
        } catch (ServiceException e) {
            ZimbraLog.ephemeral.error("unable to reset attribute migration info", e);
        }
        AttributeMigration.clearConfigCacheOnAllServers(false);
    }


    /**
     * Class encapsulating the rules governing whether the backend URL can be changed given
     * what is known about the current state of ephemeral attribute migration
     */
    @VisibleForTesting
    public static class EphemeralBackendMigrationRules {
        private MigrationStateHelper helper;

        public EphemeralBackendMigrationRules(MigrationStateHelper helper) {
            this.helper = helper;
        }


        public void checkCanChangeURL(String newURL) throws ServiceException {
            /*
             * The only scenario that explicitly disallows changing the backend URL is if the migration is currently
             * marked is IN_PROGRESS.
             */
            helper.setURL(newURL);
            MigrationInfo info = AttributeMigration.getMigrationInfo();
            Status curStatus = info.getStatus();
            String migrationURL = info.getURL();
            if (curStatus == Status.IN_PROGRESS) {
                helper.deny();
            } else if (curStatus == Status.NONE) {
                helper.warn(Reason.NO_MIGRATION);
            } else if (curStatus == Status.FAILED && newURL.equals(migrationURL)) {
                helper.warn(Reason.MIGRATION_ERROR);
            } else if (!newURL.equals(migrationURL)) {
                helper.warn(Reason.URL_MISMATCH);
            } else {
                helper.allow();
            }
        }
    }

    /**
     * Helper interface specifying how the outcomes of checkMigration() are handled
     */
    public static abstract class MigrationStateHelper {

        public static enum Reason {NO_MIGRATION, MIGRATION_ERROR, URL_MISMATCH }

        protected String URL;

        private void setURL(String newURL) {
            URL = newURL;
        }

        public abstract void deny() throws ServiceException;
        public abstract void warn(Reason reason);
        public abstract void allow();

        public static interface Factory {
            public MigrationStateHelper getHelper() throws ServiceException;
        }
    }

    public static class ZimbraMigrationStateHelper extends MigrationStateHelper {

        private MigrationInfo info;

        public ZimbraMigrationStateHelper() throws ServiceException {
            this.info = AttributeMigration.getMigrationInfo();
        }

        @Override
        public void allow() {
            ZimbraLog.ephemeral.info("Successfully changed backend URL to %s", URL);
        }

        @Override
        public void deny() throws ServiceException {
            String started = info.getDateStr("MM/dd/yyyy HH:mm:ss");
            throw ServiceException.FAILURE(String.format("Cannot change the backend URL; attribute migration to %s is currently in progress (started %s)", info.getURL(), started), null);
        }

        @Override
        public void warn(Reason reason) {
            switch(reason) {
            case MIGRATION_ERROR:
                ZimbraLog.ephemeral.warn("Previous attribute migration to %s did not succeed; data may not have been migrated to this backend", URL);
                break;
            case NO_MIGRATION:
                ZimbraLog.ephemeral.warn("No record of an attribute migration exists; data may not have been migrated to backend at %s", URL);
                break;
            case URL_MISMATCH:
                ZimbraLog.ephemeral.warn("Provided URL %s does not match current migration URL %s; data may not have been migrated to this backend.\n"
                        + "Ephemeral data will be forwarded to %s until migration info is reset or the backend is changed to match it", URL, info.getURL(), info.getURL());

            }
        }

        public static class Factory implements MigrationStateHelper.Factory {

            @Override
            public MigrationStateHelper getHelper() throws ServiceException {
                return new ZimbraMigrationStateHelper();
            }
        }

    }
}
