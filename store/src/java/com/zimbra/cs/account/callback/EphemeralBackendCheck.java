package com.zimbra.cs.account.callback;

import java.util.Map;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.EphemeralStore.Factory;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.extension.ExtensionUtil;

public class EphemeralBackendCheck extends AttributeCallback {

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
                Factory factory = EphemeralStore.getFactory(backend);
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

    private void checkMigration(String URL) throws ServiceException {
        /*
         * The only scenario that explicitly disallows changing the backend URL is if the migration is currently
         * marked is IN_PROGRESS.
         */
        MigrationInfo info = AttributeMigration.getMigrationInfo();
        Status curStatus = info.getStatus();
        String migrationURL = info.getURL();
        if (curStatus == Status.IN_PROGRESS) {
            throw ServiceException.FAILURE("attribute migration currently in progress, cannot change backend URL", null);
        } else if (curStatus == Status.NONE) {
          //no record of migration running
            ZimbraLog.ephemeral.warn("no record of an attribute migration exists; data may not have been migrated to this backend");
        } else if (curStatus == Status.FAILED) {
            ZimbraLog.ephemeral.warn("attribute migration did not succeed; data may not have been migrated to this backend");
        } else if (!URL.equals(migrationURL)) {
            //ephemeral backend is changed to one that does not match the migration target
            ZimbraLog.ephemeral.warn("new backend does not match current migration URL %s; data may not have been migrated to this backend", migrationURL);
        }
    }

    private void resetMigrationInfo(String newURL) throws ServiceException {
         // Clear out the migration info if the backend URL we migrated to matches the URL used for migration.
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
    }
}
