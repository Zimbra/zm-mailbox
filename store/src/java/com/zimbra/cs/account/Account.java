/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.GroupMembership;
import com.zimbra.cs.account.Provisioning.SetPasswordResult;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.names.NameUtil;
import com.zimbra.soap.admin.type.DataSourceType;

/**
 * @author schemers
 */
public class Account extends ZAttrAccount implements GroupedEntry, AliasedEntry {

    public Account(String name, String id, Map<String, Object> attrs, Map<String, Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.ACCOUNT;
    }

    public boolean sameAccount(Account other) {
        if (null == other) {
            return false;
        }
        return (other.getId().equals(getId()));
    }

    public void deleteAccount() throws ServiceException {
        getProvisioning().deleteAccount(this.getId());
    }

    public void rename(String newName) throws ServiceException {
        getProvisioning().renameAccount(this.getId(), newName);
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }

    public void addAlias(String alias) throws ServiceException {
        getProvisioning().addAlias(this, alias, false);
    }
    public void addAlias(String alias, boolean isHiddenAlias) throws ServiceException {
        getProvisioning().addAlias(this, alias, isHiddenAlias);
    }

    public void removeAlias(String alias) throws ServiceException {
        getProvisioning().removeAlias(this, alias);
    }

    public void authAccount(String password, AuthContext.Protocol proto) throws ServiceException {
        getProvisioning().authAccount(this, password, proto);
    }

    public void changePassword(String currentPassword, String newPassword) throws ServiceException {
        getProvisioning().changePassword(this, currentPassword, newPassword);
    }

    public void checkPasswordStrength(String password) throws ServiceException {
        getProvisioning().checkPasswordStrength(this, password);
    }

    public SetPasswordResult setPassword(String password) throws ServiceException {
        return getProvisioning().setPassword(this, password);
    }

    public GroupMembership getAclGroups(boolean adminGroupsOnly) throws ServiceException {
        return getProvisioning().getGroupMembership(this, adminGroupsOnly);
    }

    /**
     * @param zimbraId the zimbraId of the dl we are checking for
     * @return true if this account (or one of the dl it belongs to) is a member of the specified dl.
     * @throws ServiceException on error
     */
    public boolean inDistributionList(String zimbraId) throws ServiceException {
        return getProvisioning().inDistributionList(this, zimbraId);
    }

    /**
     * @return set of all the zimbraId's of lists this account belongs to, including any list in other list.
     * @throws ServiceException on error
     */
    public Set<String> getDistributionLists() throws ServiceException {
        return getProvisioning().getDistributionLists(this);
    }

    /**
     *
     * @param directOnly return only DLs this account is a direct member of
     * @param via if non-null and directOnly is false, this map will containing a mapping from
     *            a DL name to the DL it was a member of, if member was indirect.
     * @return all the DLs
     * @throws ServiceException on error
     */
    public List<DistributionList> getDistributionLists(boolean directOnly, Map<String,String> via)
    throws ServiceException {
        return getProvisioning().getDistributionLists(this, directOnly, via);
    }

    public void preAuthAccount(String accountName, String accountBy, long timestamp, long expires,
            String preAuth, Map<String, Object> authCtxt) throws ServiceException {
        getProvisioning().preAuthAccount(this, accountName, accountBy, timestamp, expires, preAuth, authCtxt);
    }

    public void preAuthAccount(String accountName, String accountBy, long timestamp, long expires,
                                        String preAuth,
                                        boolean admin,
                                        Map<String, Object> authCtxt) throws ServiceException
    {
        getProvisioning().preAuthAccount(this, accountName, accountBy, timestamp, expires, preAuth, admin, authCtxt);
    }


    public Cos getCOS() throws ServiceException {
        return getProvisioning().getCOS(this);
    }

    public DataSource createDataSource(DataSourceType type, String dataSourceName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createDataSource(this, type, dataSourceName, attrs);
    }

    public DataSource createDataSource(DataSourceType type, String dataSourceName, Map<String, Object> attrs, boolean passwdAlreadyEncrypted) throws ServiceException {
        return getProvisioning().createDataSource(this, type, dataSourceName, attrs, passwdAlreadyEncrypted);
    }

    public void modifyDataSource(String dataSourceId, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyDataSource(this, dataSourceId, attrs);
    }

    public void deleteDataSource(String dataSourceId) throws ServiceException {
        getProvisioning().deleteDataSource(this, dataSourceId);
    }

    public List<DataSource> getAllDataSources() throws ServiceException {
        return getProvisioning().getAllDataSources(this);
    }

    public DataSource get(Key.DataSourceBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public DataSource getDataSourceByName(String name) throws ServiceException {
        return get(Key.DataSourceBy.name, name);
    }

    public DataSource getDataSourceById(String id) throws ServiceException {
        return get(Key.DataSourceBy.id, id);
    }

    public Identity createIdentity(String identityName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createIdentity(this, identityName, attrs);
    }

    public void modifyIdentity(String identityName, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyIdentity(this, identityName, attrs);
    }

    public void deleteIdentity(String identityName) throws ServiceException {
        getProvisioning().deleteIdentity(this, identityName);
    }

    public List<Identity> getAllIdentities() throws ServiceException {
        return getProvisioning().getAllIdentities(this);
    }

    public Identity get(Key.IdentityBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public Identity getIdentityByName(String name) throws ServiceException {
        return get(Key.IdentityBy.name, name);
    }

    public Identity getIdentityById(String id) throws ServiceException {
        return get(Key.IdentityBy.id, id);
    }
    public Identity getDefaultIdentity() throws ServiceException {
        return getProvisioning().getDefaultIdentity(this);
    }

    public Signature createSignature(String signatureName, Map<String, Object> attrs) throws ServiceException {
        return getProvisioning().createSignature(this, signatureName, attrs);
    }

    public void modifySignature(String signatureId, Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifySignature(this, signatureId, attrs);
    }

    public void deleteSignature(String signatureId) throws ServiceException {
        getProvisioning().deleteSignature(this, signatureId);
    }

    public List<Signature> getAllSignatures() throws ServiceException {
        return getProvisioning().getAllSignatures(this);
    }

    public Signature get(Key.SignatureBy keyType, String key) throws ServiceException {
        return getProvisioning().get(this, keyType, key);
    }

    public Signature getSignatureByName(String key) throws ServiceException {
        return get(Key.SignatureBy.name, key);
    }

    public Signature getSignatureById(String key) throws ServiceException {
        return get(Key.SignatureBy.id, key);
    }

    public boolean isAccountStatusActive() {
        return Provisioning.ACCOUNT_STATUS_ACTIVE.equals(getAccountStatus(getProvisioning()));
    }

    public boolean isAccountExternal() throws ServiceException {
        Server server = getServer();
        if (server == null)
            return true;

        return !server.mailTransportMatches(getAttr(Provisioning.A_zimbraMailTransport));
    }

    public Server getServer() throws ServiceException {
        String serverName = getAttr(Provisioning.A_zimbraMailHost);
        return (serverName == null ? null : getProvisioning().get(Key.ServerBy.name, serverName));
    }

    public String getServerName() throws ServiceException {
        Server server = getServer();
        // all accounts don't necessarily have a server.
        // accounts under the config tree don't have a home server nor an email address
        if (server != null) {
            return server.getName();
        } else {
            return null;
        }
    }


    public String getAccountStatus(Provisioning prov) {

        String domainStatus = null;
        String accountStatus = getAttr(Provisioning.A_zimbraAccountStatus);

        boolean isAdmin = getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false);
        boolean isDomainAdmin = getBooleanAttr(Provisioning.A_zimbraIsDomainAdminAccount, false);
        isAdmin = (isAdmin && !isDomainAdmin);
        if (isAdmin)
            return accountStatus;


        if (mDomain != null) {
            try {
                Domain domain = prov.getDomain(this);
                if (domain != null) {
                    domainStatus = domain.getDomainStatusAsString();
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to get domain for account " + getName(), e);
                return accountStatus;
            }
        }

        if (domainStatus == null || domainStatus.equals(Provisioning.DOMAIN_STATUS_ACTIVE))
            return accountStatus;
        else if (domainStatus.equals(Provisioning.DOMAIN_STATUS_LOCKED)) {
            if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_MAINTENANCE) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_PENDING) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_LOCKED;
        } else if (domainStatus.equals(Provisioning.DOMAIN_STATUS_MAINTENANCE) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SUSPENDED) ||
                   domainStatus.equals(Provisioning.DOMAIN_STATUS_SHUTDOWN)) {
            if (accountStatus.equals(Provisioning.ACCOUNT_STATUS_PENDING) ||
                accountStatus.equals(Provisioning.ACCOUNT_STATUS_CLOSED))
                return accountStatus;
            else
                return Provisioning.ACCOUNT_STATUS_MAINTENANCE;
        } else {
            // domainStatus is Provisioning.DOMAIN_STATUS_CLOSED
            return Provisioning.ACCOUNT_STATUS_CLOSED;
        }
    }

    // needed when when cal resource is loaded as an account and we need to know if
    // it actually is a cal resource.
    public boolean isCalendarResource() {
        return getAttr(Provisioning.A_zimbraCalResType) != null;
    }

    /**
     *
     * @param prov
     * @param acct
     * @param at
     * @return     true if the the validity checking is OK (either disabled or passed checking)
     *             false otherwise
     */
    public boolean checkAuthTokenValidityValue(AuthToken at) throws ServiceException {
        if (!getProvisioning().getConfig().isAuthTokenValidityValueEnabled())
            return true;

        int acctValue = getAuthTokenValidityValue();
        int authTokenValue = at.getValidityValue();

        if (acctValue == authTokenValue)
            return true;

        ZimbraLog.account.debug("checkAuthTokenValidityValue: validity value on account = " + acctValue +
                ", validity value on auth token = " + authTokenValue);

        if (acctValue < authTokenValue) {
            /* bug 46287
             *
             * If the validity value in the auth token is higher(i.e. newer) than the
             * zimbraAuthTokenValidityValue on the account, the password had probably
             * changed on another server.   (Note: ChangePassword does *not* get proxied
             * to the home server, so even if this is the home server of the account, our
             * zimbraAuthTokenValidityValue on the account can be behind.)
             *
             * If this is the case, we reload the account from LDAP replica.
             * Note: for this reload, we reload from replica just like a regular caching reload.
             * If the replica is slow(which is a system error to be corrected, and is orthogonal
             * to the problem we are trying to solve), so be it - it is a generic/documented behavior.
             *
             * To defend against LDAP fluke (slow replica, someone directly modifies LDAP, etc)
             * so that we don't stuck in a situation repeatedly pounding LDAP, we remember the
             * highest validity value for which we have done the reload.   Do the reload only when
             * we have not yet reloaded the account during its life in cache for a validity value as
             * high as the one in this auth token.
             *
             * e.g. - req 1
             *        - account.zimbraAuthTokenValidityValue is 1
             *        - autoToken.ValidityValue is 3
             *        - we have not reloaded, so go ahead reload the account
             *        - after the reload account.zimbraAuthTokenValidityValue is still not 3 (e.g is 1 or 2)
             *          (should not happen, but people can modify LDAP data outside zimbra or
             *           there could be a slow replica, etc)
             *
             *        - record that we have reloaded for validity value 3
             *        - reject the auth token
             *
             *      - req 2
             *        - account.zimbraAuthTokenValidityValue is still 1
             *        - autoToken.ValidityValue is 3
             *        - we do *not* reload again this time, since we already tried.
             *        - reject the auth token
             *
             *      - req 3
             *        - account.zimbraAuthTokenValidityValue is still 1
             *        - autoToken.ValidityValue is 4
             *        - the highest validity value we've tried reloading for is 3, so do
             *          the reload again.
             *        - reloaded account.zimbraAuthTokenValidityValue is 4
             *        - remember that we have reloaded for validity value 4
             *        - accept the auth token
             */
            Integer highestReloadedFor = (Integer)getCachedData(EntryCacheDataKey.ACCOUNT_VALIDITY_VALUE_HIGHEST_RELOAD.getKeyName());
            boolean willReload = (highestReloadedFor == null) || (highestReloadedFor < authTokenValue);

            ZimbraLog.account.debug("checkAuthTokenValidityValue: highest validity value reloaded for = " + highestReloadedFor +
                    ", will reload = " + willReload);

            if (willReload) {
                ZimbraLog.account.debug("checkAuthTokenValidityValue: reloading account " + getName() + " for validity value " + authTokenValue);
                getProvisioning().reload(this, false); // reload from replica
                setCachedData(EntryCacheDataKey.ACCOUNT_VALIDITY_VALUE_HIGHEST_RELOAD.getKeyName(),
                    Integer.valueOf(authTokenValue));

                // validate the value again
                acctValue = getAuthTokenValidityValue();
                ZimbraLog.account.debug("checkAuthTokenValidityValue: validity value on account after reload = " + acctValue);
                return (acctValue == authTokenValue);
            }
        }

        return false;
    }

    public void setAccountDefaults(boolean setSecondaryDefaults) throws ServiceException {

        Cos cos = getProvisioning().getCOS(this); // will set cos if not set yet

        Map<String, Object> defaults = null;
        if (cos != null) {
            defaults = cos.getAccountDefaults();
            if (cos.getId() != null && this.getCOSId() == null) {
                defaults.put(Provisioning.A_zimbraCOSId, cos.getId());
            }
        }

        if (!setSecondaryDefaults) {
            // set only primary defaults
            setDefaults(defaults);
        } else {
            // set primary and secondary defaults
            Map<String, Object> secondaryDefaults = null;
            Domain domain = getProvisioning().getDomain(this);
            if (domain != null)
                secondaryDefaults = domain.getAccountDefaults();
            setDefaults(defaults, secondaryDefaults);
        }

    }

    @Override
    public String[] getAllAddrsAsGroupMember() throws ServiceException {
        if (isIsExternalVirtualAccount()) {
            return new String[]{getExternalUserMailAddress()};
        } else {
            String aliases[] = getMailAlias();
            List<String> addrs = Lists.newArrayListWithExpectedSize(aliases.length + 1);
            String myName = getName();
            addrs.add(myName);
            for (String alias : aliases) {
                if (!alias.equals(myName)) {
                    addrs.add(alias);
                }
            }
            return addrs.toArray(new String[0]);
        }

    }

    @Override
    public String[] getAliases() throws ServiceException {
        return getMailAlias();
    }

    @Override
    public boolean isAddrOfEntry(String addr) {
        addr = addr.toLowerCase();
        if (getName().equals(addr)) {
            return true;
        } else {
            Set<String> aliases = getMultiAttrSet(Provisioning.A_zimbraMailAlias);
            return aliases.contains(addr);
        }
    }

    @Override
    public Set<String> getAllAddrsSet() {
        Set<String> addrs = Sets.newHashSet();
        addrs.add(getName());
        addrs.addAll(getMultiAttrSet(Provisioning.A_zimbraMailAlias));
        return Collections.unmodifiableSet(addrs);
    }

    public UCService getUCService() throws ServiceException {
        String ucServiceId = getAttr(Provisioning.A_zimbraUCServiceId);
        return (ucServiceId == null ? null : getProvisioning().get(Key.UCServiceBy.id, ucServiceId));
    }

    @Override
    public String getUCUsername() {
        String ucUsername = super.getUCUsername();
        if (ucUsername == null) {
            try {
                NameUtil.EmailAddress emailAddr = new NameUtil.EmailAddress(getName());
                ucUsername = emailAddr.getLocalPart();
            } catch (ServiceException e) {
                ZimbraLog.account.warn("ignoring exception while getting localpart of primary email address", e);
            }
        }
        return ucUsername;
    }

    public String getDecryptedUCPassword() throws ServiceException {
        String encryptedPassword = getUCPassword();
        if (encryptedPassword == null) {
            return null;
        } else {
            return DataSource.decryptData(getId(), encryptedPassword);
        }
    }

    public void changeUCPassword(String newPlainPassword) throws ServiceException {
        String encryptedPassword = null;

        if (newPlainPassword != null) {
            encryptedPassword = encrypytUCPassword(getId(), newPlainPassword);
        }

        setUCPassword(encryptedPassword);
    }

    public static String encrypytUCPassword(String acctId, String plainPassword)
    throws ServiceException {
        return DataSource.encryptData(acctId, plainPassword);
    }

    public void cleanExpiredTokens() throws ServiceException {
        purgeAuthTokens();
    }

    public void cleanExpiredJWTokens() throws ServiceException {
        purgeInvalidJWTokens();
    }

    /**
     * Updates the values of the following attributes:
     * - userPassword
     * - zimbraAuthTokens
     * - zimbraAuthTokenValidityValue
     *
     * @throws ServiceException
     */
    public void refreshUserCredentials() throws ServiceException {
        getProvisioning().refreshUserCredentials(this);
    }

    /**
     * Returns whether username is allowed within password.
     * @return true if username is allowed within password; otherwise, false.
     */
    public boolean getAllowUsernameWithinPassword() {
        return LC.allow_username_within_password.booleanValue();
    }
}
