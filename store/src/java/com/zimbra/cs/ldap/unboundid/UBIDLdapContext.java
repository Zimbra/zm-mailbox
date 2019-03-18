/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import com.google.common.collect.Lists;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.CompareRequest;
import com.unboundid.ldap.sdk.CompareResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.AssertionRequestControl;
import com.unboundid.ldap.sdk.controls.ManageDsaITRequestControl;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.ldap.sdk.schema.Schema;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapOp;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapServerConfig.ZimbraLdapConfig;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapSchema;
import com.zimbra.cs.ldap.ZModificationList;
import com.zimbra.cs.ldap.ZMutableEntry;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.stats.ZimbraPerf;

public class UBIDLdapContext extends ZLdapContext {

    private static boolean initialized = false;

    private static ZimbraLdapConfig replicaConfig;
    private static ZimbraLdapConfig masterConfig;

    private static LDAPConnectionPool replicaConnPool;
    private static LDAPConnectionPool masterConnPool;

    private LDAPConnectionPool connPool;
    private LDAPConnection conn;
    private final boolean isZimbraLdap;
    private DereferencePolicy derefAliasPolicy;

    public static synchronized void init(boolean alwaysUseMaster) throws LdapException {
        assert(!initialized);

        if (initialized) {
            return;
        }

        initialized = true;

        try {
            masterConfig = new ZimbraLdapConfig(LdapServerType.MASTER);
            masterConnPool = LdapConnectionPool.createConnectionPool(
                    LdapConnectionPool.CP_ZIMBRA_MASTER, masterConfig);
        } catch (LdapException e) {
            ZimbraLog.ldap.info("master is down, falling back to replica...");
            replicaConfig = new ZimbraLdapConfig(LdapServerType.REPLICA);
            replicaConnPool = LdapConnectionPool.createConnectionPool(
                    LdapConnectionPool.CP_ZIMBRA_REPLICA, replicaConfig);
            masterConfig = replicaConfig;
            masterConnPool = replicaConnPool;
            ZimbraLog.ldap.info("using replica");
        }

        if (alwaysUseMaster) {
            replicaConfig = masterConfig;
            replicaConnPool = masterConnPool;
        } else {
            if (replicaConfig == null) {
                replicaConfig = new ZimbraLdapConfig(LdapServerType.REPLICA);
            }
            if (replicaConnPool == null) {
                replicaConnPool = LdapConnectionPool.createConnectionPool(
                        LdapConnectionPool.CP_ZIMBRA_REPLICA, replicaConfig);
            }
        }
    }

    // only called from TestLdapConnectivity unittest, so we can run multiple
    // ldap configs (ldap, ldaps, etc) in one VM
    public static synchronized void shutdown() {
        LdapConnectionPool.closeAll();

        initialized = false;

        replicaConfig = null;
        masterConfig = null;

        replicaConnPool = null;
        masterConnPool = null;
    }


    @Override
    public void debug() {
    }

    static synchronized void alwaysUseMaster() {
        replicaConnPool = masterConnPool;
    }

    /*
     * Zimbra LDAP
     */
    public UBIDLdapContext(LdapServerType serverType, LdapUsage usage) throws LdapException {
        super(usage);
        isZimbraLdap = true;
        derefAliasPolicy = DereferencePolicy.NEVER;

        if (serverType.isMaster()) {
            connPool = masterConnPool;
        } else {
            connPool = replicaConnPool;
        }

        conn = getConnection(connPool);
        ZimbraLog.ldap.debug("UBIDLdapContext: Zimbra LDAP host: %s ", conn.getHostPort());
    }

    /*
     * External LDAP
     */
    public UBIDLdapContext(ExternalLdapConfig config, LdapUsage usage) throws LdapException {
        super(usage);
        isZimbraLdap = false;
        setDerefAliasPolicy(config);

        connPool = LdapConnectionPool.getConnPoolByConfig(config);
        conn = getConnection(connPool);
        ZimbraLog.ldap.debug("UBIDLdapContext: External LDAP host: %s ", conn.getHostPort());
    }

    // for unittest
    public LDAPConnection getNative() {
        return conn;
    }

    private LDAPConnection getConnection(LDAPConnectionPool pool)
    throws LdapException {
        try {

           // TODO: this is for backward compatibility with the legacy code - remvoe or enhance
            // TODO: should have a timer for each connection pool
            long start = 0;
            if (isZimbraLdap) {
                start = ZimbraPerf.STOPWATCH_LDAP_DC.start();
            }

            LDAPConnection connection = UBIDLdapOperation.GET_CONNECTION.execute(this, pool);

            if (isZimbraLdap) {
                ZimbraPerf.STOPWATCH_LDAP_DC.stop(start);
            }

            LdapConnectionPool.debugCheckOut(pool, connection);
            return connection;
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get connection", e);
        }
    }

    public LDAPConnectionPool getConnectionPool() {
        String connPoolName = conn.getConnectionPoolName();
        return LdapConnectionPool.getConnPoolByName(connPoolName);
    }

    public String getConnectionName() {
        return conn.getConnectionName();
    }

    LDAPConnection getConn() {
        return conn;
    }

    private String getConnectedHostname() {
        if (conn != null) {
            return conn.getConnectedAddress() + ":" + conn.getConnectedPort();
        }
        return "";
    }

    private LdapException mapToLdapException(String message, LDAPException e) {
        message = message + ": ldap host="+ getConnectedHostname();
        if (ResultCode.TIMEOUT == e.getResultCode()) {
            this.closeContext(true); //force connection to be terminated
        }
        if (isZimbraLdap) {
            return UBIDLdapException.mapToLdapException(message, e);
        } else {
            // need more precise mapping for external LDAP exceptions so we
            // can report config error better
            return UBIDLdapException.mapToExternalLdapException(message, e);
        }
    }

    private void setDerefAliasPolicy(ExternalLdapConfig config) {

        String derefPolicy = config.getDerefAliasPolicy();

        if (derefPolicy == null) {
            derefAliasPolicy = DereferencePolicy.NEVER;
        } if ("always".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.ALWAYS;
        } else if ("never".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.NEVER;
        } else if ("finding".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.FINDING;
        } else if ("searching".equalsIgnoreCase(derefPolicy)) {
            derefAliasPolicy = DereferencePolicy.SEARCHING;
        } else {
            ZimbraLog.ldap.warn("invalid deref alias policy: " + derefPolicy +
                    ", default to never");
            derefAliasPolicy = DereferencePolicy.NEVER;
        }
    }

    @Override
    public void closeContext(boolean forceClose) {

        LdapConnectionPool.debugCheckIn(connPool, conn);

        UBIDLogger.beforeOp(LdapOp.REL_CONN, conn);
        // NOTE: do NOT call conn.close() because it will unbind from the server
        //       and close the connection and defunt the connection from the pool.
        //       Just release it to the pool.
        //
        if (forceClose) {
            connPool.releaseDefunctConnection(conn);
        } else {
            connPool.releaseConnection(conn);
        }

        conn = null;
        // TODO check out this creature releaseConnectionAfterException
    }

    @Override
    public void createEntry(ZMutableEntry entry) throws ServiceException {
        try {
            UBIDLdapOperation.CREATE_ENTRY.execute(this, ((UBIDMutableEntry) entry).getNative());
        } catch (LDAPException e) {
            throw mapToLdapException("unable to create entry", e);
        }
    }

    @Override
    public void createEntry(String dn, String objectClass, String[] attrs)
    throws ServiceException {
        UBIDMutableEntry entry = new UBIDMutableEntry();
        entry.setDN(dn);

        entry.setAttr(LdapConstants.ATTR_objectClass, objectClass);

        for (int i=0; i < attrs.length; i += 2) {
            entry.setAttr(attrs[i], attrs[i+1]);
        }

        createEntry(entry);
    }

    @Override
    public void createEntry(String dn, String[] objectClasses, String[] attrs)
    throws ServiceException {
        UBIDMutableEntry entry = new UBIDMutableEntry();
        entry.setDN(dn);

        Set<String> ocs = new HashSet<String>(Arrays.asList(objectClasses));
        entry.addAttr(LdapConstants.ATTR_objectClass, ocs);

        for (int i=0; i < attrs.length; i += 2) {
            entry.setAttr(attrs[i], attrs[i+1]);
        }

        createEntry(entry);
    }

    @Override
    public ZModificationList createModificationList() {
        return new UBIDModificationList();
    }

    @Override
    public void deleteChildren(String dn) throws ServiceException {

        try {
            // use ZLdapFilter instead of just the native Filter so it's
            // convenient for stating
            ZLdapFilter filter = ZLdapFilterFactory.getInstance().anyEntry();
            // Filter filter = Filter.createPresenceFilter(LdapConstants.ATTR_OBJECTCLASS);

            SearchRequest searchRequest = new SearchRequest(dn,
                    SearchScope.ONE,
                    derefAliasPolicy,
                    0,  // size limit
                    0,  // time limit
                    false, // getTypesOnly
                    ((UBIDLdapFilter) filter).getNative()
                    );

            searchRequest.setAttributes("dn");

            SearchResult result = UBIDLdapOperation.SEARCH.execute(this, searchRequest, filter);

            List<SearchResultEntry> entries = result.getSearchEntries();
            for (SearchResultEntry entry : entries) {
                deleteEntry(entry.getDN());
            }
        } catch (LDAPException e) {
            throw mapToLdapException("unable to delete children", e);
        }
    }

    @Override
    public ZAttributes getAttributes(String dn, String[] attrs) throws LdapException {
        try {
            SearchResultEntry entry = UBIDLdapOperation.GET_ENTRY.execute(this, dn, attrs);

            if (entry == null) {
                throw LdapException.ENTRY_NOT_FOUND("entry not found at " + dn, null);
            }
            return new UBIDAttributes(entry);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get attributes", e);
        }
    }

    @Override
    public ZLdapSchema getSchema() throws LdapException {
        try {
            Schema schema;

            if (InMemoryLdapServer.isOn()) {
                schema = InMemoryLdapServer.getSchema(InMemoryLdapServer.ZIMBRA_LDAP_SERVER);
            } else {
                schema = UBIDLdapOperation.GET_SCHEMA.execute(this);
            }
            return new UBIDLdapSchema(schema);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to get schema", e);
        }
    }

    @Override
    public void modifyAttributes(String dn, ZModificationList modList)
    throws LdapException {
        UBIDModificationList modificationList = (UBIDModificationList) modList;
        modifyAttributes(dn, modificationList);
    }

    private void modifyAttributes(String dn, UBIDModificationList modificationList)
    throws LdapException {
        try {
            LDAPResult result = UBIDLdapOperation.MODIFY_ATTRS.execute(
                    this, dn, modificationList.getModList());
        } catch (LDAPException e) {
            if (e.getResultCode() == ResultCode.NO_SUCH_ATTRIBUTE) {
                if (e.getMessage() != null && e.getMessage().indexOf(':') != -1) {
                    String [] attrs = e.getMessage().split(":");
                    String attr = null;
                    if (attrs.length >= 2) {
                        attr = attrs[1];
                    }
                    if (!StringUtil.isNullOrEmpty(attr)) {
                        attr = attr.trim();
                        Iterator<Modification> iter = modificationList.getModList().iterator();
                        while (iter.hasNext()) {
                            Modification mod = iter.next();
                            if (mod.getAttributeName().equalsIgnoreCase(attr)) {
                                if (mod.getModificationType() == ModificationType.DELETE ||
                                    (mod.getModificationType() == ModificationType.REPLACE && !mod.hasValue())) {
                                    ZimbraLog.ldap.warn("Ignoring delete/modify empty value attribute, reason: %s", e.getMessage());
                                    iter.remove();
                                    if (!modificationList.getModList().isEmpty()) {
                                        modifyAttributes(dn, modificationList);
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            throw mapToLdapException("unable to modify attributes", e);
        }
    }


    @Override
    public boolean testAndModifyAttributes(String dn,
            ZModificationList modList, ZLdapFilter testFilter)
    throws LdapException {
        try {
            ModifyRequest modReq = new ModifyRequest(dn, ((UBIDModificationList)modList).getModList());
            modReq.addControl(new AssertionRequestControl(((UBIDLdapFilter) testFilter).getNative()));

            LDAPResult result = UBIDLdapOperation.TEST_AND_MODIFY_ATTRS.execute(this, modReq);
            return true;
        } catch (LDAPException e) {
            if (e.getResultCode() == ResultCode.ASSERTION_FAILED) {
                // The modification failed because the the filter does not match the target entry
                return false;
            } else {
                // The modification failed for some other reason.
                throw mapToLdapException("unable to test and modify attributes", e);
            }
        }
    }

    @Override
    public void moveChildren(String oldDn, String newDn) throws ServiceException {
        try {
            // use ZLdapFilter instead of just the native Filter so it's
            // convenient for stating
            ZLdapFilter filter = ZLdapFilterFactory.getInstance().anyEntry();
            // Filter filter = Filter.createPresenceFilter(LdapConstants.ATTR_OBJECTCLASS);

            SearchRequest searchRequest = new SearchRequest(oldDn,
                    SearchScope.ONE,
                    derefAliasPolicy,
                    0,  // size limit
                    0,  // time limit
                    false, // getTypesOnly
                    ((UBIDLdapFilter) filter).getNative()
                    );

            searchRequest.setAttributes("dn");

            SearchResult result = UBIDLdapOperation.SEARCH.execute(this, searchRequest, filter);

            List<SearchResultEntry> entries = result.getSearchEntries();
            for (SearchResultEntry entry : entries) {
                DN entryDN = entry.getParsedDN();
                String childDn = entryDN.toNormalizedString();
                String childRdn = entryDN.getRDNString();

                UBIDLdapOperation.MODIFY_DN.execute(this, childDn, childRdn, true, newDn);
            }
        } catch (LDAPException e) {
            throw mapToLdapException("unable to move children", e);
        }
    }

    @Override
    public void renameEntry(String oldDn, String newDn) throws LdapException {
        try {
            DN newDN = new DN(newDn);
            String newRDN = newDN.getRDNString();
            String newSuperiorDN = newDN.getParentString();

            UBIDLdapOperation.MODIFY_DN.execute(this, oldDn, newRDN, true, newSuperiorDN);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to rename entry", e);
        }
    }

    @Override
    public void replaceAttributes(String dn, ZAttributes attrs) throws LdapException {
        Map<String, Object> attrMap = attrs.getAttrs();

        UBIDModificationList modList = new UBIDModificationList();
        modList.replaceAll(attrMap);
        modifyAttributes(dn, modList);
    }


    @Override
    public void searchPaged(SearchLdapOptions searchOptions) throws ServiceException {
        int maxResults = searchOptions.getMaxResults();
        String base = searchOptions.getSearchBase();
        ZLdapFilter filter = searchOptions.getFilter();
        Set<String> binaryAttrs = searchOptions.getBinaryAttrs();
        SearchScope searchScope = ((UBIDSearchScope) searchOptions.getSearchScope()).getNative();
        SearchLdapOptions.SearchLdapVisitor visitor = searchOptions.getVisitor();
        SearchGalResult searchGalResult = searchOptions.getSearchGalResult();
        int pageSize = searchOptions.getResultPageSize();
        int offset = 0;
        boolean pagination = false;
        int limit = 0;
        String prevLastReturnedItemCreateDate = null;
        if (searchGalResult != null) {
            offset = searchGalResult.getLdapMatchCount();
            prevLastReturnedItemCreateDate = searchGalResult.getLdapTimeStamp();
            pagination = searchGalResult.getHadMore();
            limit = searchGalResult.getLimit();
        }

        if (GalOp.sync == searchOptions.getGalOp() && !pagination) {
           limit = 0;
        }
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }

        int pageCount = 0;
        int pageOffset = 0;
        int currentPage = 0;
        int index = 0;
        if (offset > 0) {
            pageCount = offset / pageSize;
            pageOffset = offset % pageSize;
        }
        String newToken = "";

        boolean wantPartialResult = true;  // TODO: this is the legacy behavior, we can make it a param

        try {
            SearchRequest searchRequest = new SearchRequest(base,
                    searchScope,
                    derefAliasPolicy,
                    maxResults,
                    0,
                    false,
                    ((UBIDLdapFilter) filter).getNative());

            searchRequest.setAttributes(searchOptions.getReturnAttrs());

            //Set the page size and initialize the cookie that we pass back in subsequent pages

            ASN1OctetString cookie = null;
            int count = offset;
            do {
                List<Control> controls = Lists.newArrayListWithCapacity(2);
                if (searchOptions.isUseControl()) {
                    controls.add(new SimplePagedResultsControl(pageSize, cookie));
                }
                if (searchOptions.isManageDSAit()) {
                    controls.add(new ManageDsaITRequestControl(false));
                }
                searchRequest.setControls(controls.toArray(new Control[0]));

                SearchResult result = null;
                try {
                    result = UBIDLdapOperation.SEARCH.execute(this, searchRequest, filter);
                } catch (LDAPException e) {
                    if (ResultCode.SIZE_LIMIT_EXCEEDED == e.getResultCode() && wantPartialResult) {
                        // if callsite wants partial result, return them
                        LDAPResult ldapResult = e.toLDAPResult();
                        if (ldapResult instanceof SearchResult) {
                            SearchResult searchResult = (SearchResult) ldapResult;
                            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                                String dn = entry.getDN();
                                UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
                                if (visitor.wantAttrMapOnVisit()) {
                                    visitor.visit(dn, ubidAttrs.getAttrs(binaryAttrs), ubidAttrs);
                                } else {
                                    visitor.visit(dn, ubidAttrs);
                                }
                                newToken = ubidAttrs.getAttrString("whenCreated") != null ? ubidAttrs.getAttrString("whenCreated") : ubidAttrs.getAttrString("createTimeStamp");
                            }
                            if (searchGalResult != null) { 
                                searchGalResult.setLdapTimeStamp(newToken);
                                searchGalResult.setLdapMatchCount(1);
                                searchGalResult.setHadMore(true);
                            }
                        }
                    }
                    // always re-throw
                    throw e;
                }

                List<SearchResultEntry> entries = result.getSearchEntries();
                boolean hasMore = false;
                int resultSize = entries.size();
                if (resultSize > (limit + pageOffset)) {
                    hasMore = true;
                }
                String leCreateDate = null;
                if (currentPage >= pageCount) {
                    leCreateDate = getLastEntryCreationDate(limit+pageOffset, entries);
                    if (prevLastReturnedItemCreateDate != null && !prevLastReturnedItemCreateDate.equals(leCreateDate)) {
                        count = 0;
                    }
                    for (index = pageOffset; index < entries.size() && limit > 0; index++) {
                        SearchResultEntry entry = entries.get(index);
                        String dn = entry.getDN();
                        UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
                        if (visitor.wantAttrMapOnVisit()) {
                            visitor.visit(dn, ubidAttrs.getAttrs(binaryAttrs), ubidAttrs);
                        } else {
                            visitor.visit(dn, ubidAttrs);
                        }
                        limit--;
                        newToken = ubidAttrs.getAttrString("whenCreated") != null ? ubidAttrs.getAttrString("whenCreated") : ubidAttrs.getAttrString("createTimeStamp");
                        if (newToken != null && newToken.equals(leCreateDate)) {
                            count++;
                        }
                    }
                    prevLastReturnedItemCreateDate = leCreateDate;
                    pageOffset = 0;
                }

                cookie = null;
                for (Control c : result.getResponseControls()) {
                    if (c instanceof SimplePagedResultsControl) {
                        cookie = ((SimplePagedResultsControl) c).getCookie();
                    }
                }

                if (searchGalResult != null && (GalOp.sync == searchOptions.getGalOp())) {
                    if(limit == 0 && (((cookie != null) && (cookie.getValueLength() > 0)) || hasMore)) {
                        searchGalResult.setHadMore(true);
                        searchGalResult.setLdapTimeStamp(newToken);
                        searchGalResult.setLdapMatchCount(count);
                    } else if (((cookie != null) && (cookie.getValueLength() == 0))) {
                        searchGalResult.setHadMore(false);
                        searchGalResult.setLdapMatchCount(0);
                    }
                }
                currentPage++;
            } while ((cookie != null) && (cookie.getValueLength() > 0) && limit > 0);
        } catch (SearchLdapOptions.StopIteratingException e) {
            // break out of the loop and close the ne
        } catch (LDAPException e) {
            throw mapToLdapException("unable to search ldap", e);
        }
    }

    private String getLastEntryCreationDate(int limit, List<SearchResultEntry> entries) throws LdapException {
        String leCreateDate = null;
        int size = entries.size();
        ZimbraLog.gal.debug("inside getLastEntryCreationDate()");
        ZimbraLog.gal.debug("Liimit = %d, Size = %d", limit, size);
        SearchResultEntry entry = null;
        if (size != 0) {
            if (limit <= size && limit > 0) {
                entry = entries.get(limit-1);
            } else {
                entry = entries.get(size-1);
            }
            UBIDAttributes ubidAttrs = new UBIDAttributes(entry);
            leCreateDate = ubidAttrs.getAttrString("whenCreated") != null ? ubidAttrs.getAttrString("whenCreated") : ubidAttrs.getAttrString("createTimeStamp");
        }
        return leCreateDate;
     }

    /**
     * Perform an LDAPv3 compare operation, which may be used to determine whether a specified entry contains a given
     * attribute value.  Compare requests include the DN of the target entry, the name of the target attribute,
     * and the value for which to make the determination.
     *
     * @param  dn The DN of the entry in which the comparison is to be performed.  It must not be {@code null}.
     * @param  attributeName   The name of the target attribute for which the comparison is to be performed.
     *         It must not be {@code null}.
     * @param  assertionValue  The assertion value to verify within the entry.  It must not be {@code null}.
     */
    @Override
    public boolean compare(final String dn, final String attributeName, final String assertionValue)
    throws ServiceException {
        CompareRequest compareRequest = new CompareRequest(dn, attributeName, assertionValue);
        CompareResult compareResult;
        try {
            compareResult = UBIDLdapOperation.COMPARE.execute(this, compareRequest);

            // The compare operation didn't throw an exception, so we can try to
            // determine whether the compare matched.
            return compareResult.compareMatched();
        } catch (LDAPException le) {
            ZimbraLog.ldap.debug("Compare failed result code='%s' error message='%s'",
                    le.getResultCode(), le.getDiagnosticMessage(), le);
        }
        return false;
    }

    @Override
    public ZSearchResultEnumeration searchDir(String baseDN, ZLdapFilter filter,
            ZSearchControls searchControls) throws LdapException {

        UBIDSearchControls sc = (UBIDSearchControls)searchControls;

        try {
            SearchRequest searchRequest = new SearchRequest(baseDN,
                    sc.getSearchScope(),
                    derefAliasPolicy,
                    sc.getSizeLimit(),
                    sc.getTimeLimit(),
                    sc.getTypesOnly(),
                    ((UBIDLdapFilter) filter).getNative());

            searchRequest.setAttributes(sc.getReturnAttrs());

            SearchResult result = UBIDLdapOperation.SEARCH.execute(this, searchRequest, filter);

            return new UBIDSearchResultEnumeration(result);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to search ldap", e);
        }
    }

    @Override
    public long countEntries(String baseDN, ZLdapFilter filter,
            ZSearchControls searchControls) throws LdapException {

        UBIDSearchControls sc = (UBIDSearchControls)searchControls;

        try {
            SearchRequest searchRequest = new SearchRequest(baseDN,
                    sc.getSearchScope(),
                    derefAliasPolicy,
                    sc.getSizeLimit(),
                    sc.getTimeLimit(),
                    sc.getTypesOnly(),
                    ((UBIDLdapFilter) filter).getNative());

            NoopSearchControl noopSearchCtl = new NoopSearchControl();
            searchRequest.addControl(noopSearchCtl);

            // searchRequest.setAttributes(sc.getReturnAttrs());
            // no point to request attributes
            if (sc.getReturnAttrs() != null) {
                throw LdapException.INVALID_REQUEST("return attributes are not allowed for countEntries", null);
            }

            SearchResult result = UBIDLdapOperation.SEARCH.execute(this, searchRequest, filter);

            NoopSearchControl control = NoopSearchControl.get(result);
            if (control == null) {
                throw LdapException.LDAP_ERROR("Noop search control is not present in response", null);
            }
            return control.getCount();
        } catch (LDAPException e) {
            throw mapToLdapException("unable to search ldap", e);
        }
    }

    @Override
    public void deleteEntry(String dn) throws LdapException {
        try {
            UBIDLdapOperation.DELETE_ENTRY.execute(this, dn);
        } catch (LDAPException e) {
            throw mapToLdapException("unable delete", e);
        }
    }

    @Override
    public void setPassword(String dn, String newPassword) throws LdapException {
        try {
            UBIDLdapOperation.SET_PASSWORD.execute(this, dn, newPassword);
        } catch (LDAPException e) {
            throw mapToLdapException("unable to change password", e);
        }
    }

    /**
     * authenticate to LDAP server.
     *
     * This is method is called for:
     *   - external LDAP auth
     *   - auth to Zimbra LDAP server when the stored password is not SSHA.
     *
     * @param urls
     * @param wantStartTLS
     * @param bindDN
     * @param password
     * @param note
     * @throws ServiceException
     */
    private static void ldapAuthenticate(LdapServerConfig config,
            String bindDN, String password, LdapUsage usage)
    throws ServiceException {
        /*
         * About dereferencing alias.
         *
         * The legacy JNDI implementation supports specifying deref
         * alias policy during bind, via the "java.naming.ldap.derefAliases"
         * DirContext env property.
         *
         * Doesn't look like unboundid has an obvious way to specify
         * deref alias policy during bind.
         *
         * The LDAP protocol http://tools.ietf.org/html/rfc4511 disallows
         * LDAP server to deref alias during bind anyway.
         *
         * section 4.2
         * ..., it SHALL NOT perform alias dereferencing.
         *
         * Therefore, we do *not* support dereferencing alias during bind anymore.
         *
         */

        boolean succeeded = false;
        LdapServerPool serverPool = new LdapServerPool(config);
        LDAPConnection connection = null;
        BindResult bindResult = null;

        long startTime = UBIDLdapOperation.GENERIC_OP.begin();
        try {
            if (InMemoryLdapServer.isOn()) {
                connection = InMemoryLdapServer.getConnection();
                password = InMemoryLdapServer.Password.treatPassword(password);
            } else {
                connection = serverPool.getServerSet().getConnection();
            }

            if (serverPool.getConnectionType() == LdapConnType.STARTTLS) {
                SSLContext startTLSContext =
                    LdapSSLUtil.createSSLContext(config.sslAllowUntrustedCerts());
                ExtendedResult extendedResult = connection.processExtendedOperation(
                        new StartTLSExtendedRequest(startTLSContext));

                // NOTE:
                // The processExtendedOperation method will only throw an exception
                // if a problem occurs while trying to send the request or read the
                // response.  It will not throw an exception because of a non-success
                // response.
                if (extendedResult.getResultCode() != ResultCode.SUCCESS) {
                    throw ServiceException.FAILURE(
                            "unable to send or receive startTLS extended operation", null);
                }
            }

            bindResult = connection.bind(bindDN, password);
            if (bindResult.getResultCode() != ResultCode.SUCCESS) {
                throw ServiceException.FAILURE("unable to bind", null);
            }
            succeeded = true;
        } catch (LDAPException e) {
            throw UBIDLdapException.mapToExternalLdapException("unable to ldap authenticate", e);
        } finally {
            UBIDLdapOperation.GENERIC_OP.end(LdapOp.OPEN_CONN, usage, startTime,
                    succeeded,
                    bindResult,
                    String.format("conn=[%s], url=[%s], connType=[%s], bindDN=[%s]",
                    connection == null ? "null" : connection.getConnectionID(),
                    serverPool.getRawUrls(),
                    serverPool.getConnectionType().name(),
                    bindDN));

            if (connection != null) {
                UBIDLogger.beforeOp(LdapOp.CLOSE_CONN, connection);
                connection.close();
            }
        }

    }

    static void externalLdapAuthenticate(String[] urls, boolean wantStartTLS,
            String bindDN, String password, String note)
    throws ServiceException {
        ExternalLdapConfig config = new ExternalLdapConfig(urls, wantStartTLS,
                null, bindDN, password, null, note);
        ldapAuthenticate(config, bindDN, password, LdapUsage.LDAP_AUTH_EXTERNAL);
    }

    static void zimbraLdapAuthenticate(String bindDN, String password)
    throws ServiceException {
        ldapAuthenticate(replicaConfig, bindDN, password, LdapUsage.LDAP_AUTH_ZIMBRA);
    }

}
