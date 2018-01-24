package com.zimbra.cs.ephemeral.migrate;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapDateUtil;

/**
 * The primary implementation of MigrationInfo is a singleton, as it is backed by
 * the zimbraAttributeMigrationInfo LDAP attribute
 */
public class ZimbraMigrationInfo extends MigrationInfo {

    private Config config;

    public ZimbraMigrationInfo() throws ServiceException {
        config = Provisioning.getInstance().getConfig();
        decode(config.getAttributeMigrationInfo());
    }

    private void decode(String encoded) {
        if (Strings.isNullOrEmpty(encoded)) {
            //no migration info present
            return;
        }
        String[] tokens = encoded.split("\\|");
        if (tokens.length == 3) {
            try {
                status = Status.valueOf(tokens[0]);
            } catch (IllegalArgumentException e) {
                ZimbraLog.ephemeral.error("invalid migration status '%s', defaulting to NONE", tokens[0]);
            }
            if (tokens[1].length() > 0) {
                try {
                    dateStarted = LdapDateUtil.parseGeneralizedTime(tokens[1]);
                } catch (NumberFormatException e) {
                    ZimbraLog.ephemeral.error("invalid migration start time: %s", tokens[1]);
                }
            }
            URL = tokens[2];
        } else {
            ZimbraLog.ephemeral.error("invalid zimbraAttributeMigrationInfo value: %s", encoded);
        }
    }

    private String encode() {
        String dateStr = dateStarted == null ? "" : LdapDateUtil.toGeneralizedTime(dateStarted);
        return String.format("%s|%s|%s", URL == null ? "" : status.name(), dateStr, URL);
    }

    @Override
    protected void clearSavedData() throws ServiceException {
        config.unsetAttributeMigrationInfo();
        AttributeMigration.clearConfigCacheOnAllServers(true, false);
    }

    @Override
    public void save() throws ServiceException {
        config.setAttributeMigrationInfo(encode());
    }

    public static class Factory implements MigrationInfo.Factory {

        @Override
        public MigrationInfo getInfo() throws ServiceException {
            return new ZimbraMigrationInfo();
        }
    }
}