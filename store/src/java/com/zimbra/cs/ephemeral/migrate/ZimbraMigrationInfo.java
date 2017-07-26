package com.zimbra.cs.ephemeral.migrate;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
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
        URL = tokens[0];
        status = Status.valueOf(tokens[1]);
        if (tokens.length == 3) {
            dateStarted = LdapDateUtil.parseGeneralizedTime(tokens[2]);
        }
    }

    private String encode() {
        String dateStr = dateStarted == null ? "" : LdapDateUtil.toGeneralizedTime(dateStarted);
        return String.format("%s|%s|%s", URL == null ? "" : URL, status.name(), dateStr);
    }

    @Override
    protected void clearSavedData() throws ServiceException {
        config.unsetAttributeMigrationInfo();
        AttributeMigration.clearConfigCacheOnAllServers(true);
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