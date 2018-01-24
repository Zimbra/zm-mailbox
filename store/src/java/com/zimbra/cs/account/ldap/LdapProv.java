/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapTODO.TODO;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.mime.MimeTypeInfo;

/**
 *
 * @author pshao
 *
 */
public abstract class LdapProv extends Provisioning {

    protected LdapDIT mDIT;
    protected LdapHelper helper;

    protected LdapProv() {
        LdapClient.initialize();
    }

    public static LdapProv getInst() throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        if (prov instanceof LdapProv) {
            return (LdapProv) prov;
        } else {
            throw ServiceException.FAILURE("not an instance of LdapProv", null);
        }
    }

    protected void setDIT() {
        mDIT = new LdapDIT(this);
    }

    public LdapDIT getDIT() {
        return mDIT;
    }

    protected void setHelper(LdapHelper helper) {
        this.helper = helper;
    }

    public LdapHelper getHelper() {
        return helper;
    }

    public abstract int getAccountCacheSize();
    public abstract double getAccountCacheHitRate();
    public abstract int getCosCacheSize();
    public abstract double getCosCacheHitRate();
    public abstract int getDomainCacheSize();
    public abstract double getDomainCacheHitRate();
    public abstract int getServerCacheSize();
    public abstract double getServerCacheHitRate();
    public abstract int getUCServiceCacheSize();
    public abstract double getUCServiceCacheHitRate();
    public abstract int getZimletCacheSize();
    public abstract double getZimletCacheHitRate();
    public abstract int getGroupCacheSize();
    public abstract double getGroupCacheHitRate();
    public abstract int getXMPPCacheSize();
    public abstract double getXMPPCacheHitRate();


    public abstract void waitForLdapServer();
    public abstract void alwaysUseMaster();

    public abstract void dumpLdapSchema(PrintWriter pw) throws ServiceException;

    public abstract void renameDomain(String domainId, String newDomainName)
    throws ServiceException;

    public abstract void searchOCsForSuperClasses(Map<String, Set<String>> ocs);
    public abstract void getAttrsInOCs(String[] ocs, Set<String> attrsInOCs)
    throws ServiceException;

    public abstract List<MimeTypeInfo> getAllMimeTypesByQuery()
    throws ServiceException;
    public abstract List<MimeTypeInfo> getMimeTypesByQuery(String mimeType)
    throws ServiceException;

    public abstract void externalLdapAuth(Domain domain, AuthMech authMech, Account acct,
            String password, Map<String, Object> authCtxt)
    throws ServiceException;


    public abstract void externalLdapAuth(Domain domain, AuthMech authMech, String principal,
            String password, Map<String, Object> authCtxt)
    throws ServiceException;

    /**
     * Authenticate to Zimbra LDAP server with bind DN and password.
     * Used when stored password is not SSHA.
     */
    public abstract void zimbraLdapAuthenticate(Account acct, String password,
            Map<String, Object> authCtxt)
    throws ServiceException;

    public abstract void removeFromCache(Entry entry);

    @TODO  // deprecate
    public abstract void searchLdapOnMaster(String base, String filter,
            String[] returnAttrs, SearchLdapOptions.SearchLdapVisitor visitor)
    throws ServiceException;

    public abstract void searchLdapOnMaster(String base, ZLdapFilter filter,
            String[] returnAttrs, SearchLdapOptions.SearchLdapVisitor visitor)
    throws ServiceException;

    @TODO  // deprecate
    public abstract void searchLdapOnReplica(String base, String filter,
            String[] returnAttrs, SearchLdapOptions.SearchLdapVisitor visitor)
    throws ServiceException;

    public abstract void searchLdapOnReplica(String base, ZLdapFilter filter,
            String[] returnAttrs, SearchLdapOptions.SearchLdapVisitor visitor)
    throws ServiceException;

}
