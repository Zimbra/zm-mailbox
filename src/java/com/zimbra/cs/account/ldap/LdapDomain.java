/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntrySearchFilter;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class LdapDomain extends LdapNamedEntry implements Domain {

    private static final int DEFAULT_GAL_MAX_RESULTS = 100;

    private static final String DATA_GAL_ATTR_MAP = "GAL_ATTRS_MAP";
    private static final String DATA_GAL_ATTR_LIST = "GAL_ATTR_LIST";

    private static final String GAL_FILTER_ZIMBRA_ACCOUNTS = "zimbraAccounts";
    private static final String GAL_FILTER_ZIMBRA_CALENDAR_RESOURCES = "zimbraResources";
    
    private static final String GAL_FILTER_ZIMBRA_ACCOUNT_AUTO_COMPLETE = "zimbraAccountAutoComplete";
    private static final String GAL_FILTER_ZIMBRA_CALENDAR_RESOURCE_AUTO_COMPLETE = "zimbraResourceAutoComplete";    

    private LdapProvisioning mProv;
   
    LdapDomain(String dn, Attributes attrs, LdapProvisioning prov) {
        super(dn, attrs);
        mProv = prov;
    }

    public String getName() {
        return getAttr(Provisioning.A_zimbraDomainName);
    }

    public String getId() {
        return getAttr(Provisioning.A_zimbraId);
    }
    //public abstract int numberOfAccounts();

    public List getAllAccounts() throws ServiceException {
        return searchAccounts("(objectclass=zimbraAccount)", null, null, true, Provisioning.SA_ACCOUNT_FLAG);
    }

    public void getAllAccounts(NamedEntry.Visitor visitor) throws ServiceException {
        mProv.searchObjects("(objectclass=zimbraAccount)", null, "ou=people,"+getDN(), Provisioning.SA_ACCOUNT_FLAG, visitor);
    }

    public List getAllCalendarResources() throws ServiceException {
        return searchCalendarResources(
                LdapEntrySearchFilter.sCalendarResourcesFilter,
                null, null, true);
    }

    public void getAllCalendarResources(NamedEntry.Visitor visitor)
    throws ServiceException {
        mProv.searchObjects("(objectclass=zimbraCalendarResource)",
                             null, "ou=people," + getDN(),
                             Provisioning.SA_CALENDAR_RESOURCE_FLAG,
                             visitor);
    }

    public List getAllDistributionLists() throws ServiceException {
        return searchAccounts("(objectClass=zimbraDistributionList)", null, null, true, Provisioning.SA_DISTRIBUTION_LIST_FLAG);
    }

    public List searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException
    {
        return mProv.searchObjects(query, returnAttrs, sortAttr, sortAscending, "ou=people,"+getDN(), flags);
    }

    public List searchCalendarResources(
        EntrySearchFilter filter,
        String returnAttrs[],
        String sortAttr,
        boolean sortAscending)
    throws ServiceException {
        return mProv.searchCalendarResources(filter, returnAttrs,
                                             sortAttr, sortAscending,
                                             "ou=people," + getDN());
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
	 */
    public SearchGalResult searchGal(String n,
                                     Provisioning.GAL_SEARCH_TYPE type,
                                     String token)
    throws ServiceException {
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        int maxResults = token != null ? 0 : getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
        if (type == Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE)
            return searchResourcesGal(n, maxResults, token, false);

        String mode = getAttr(Provisioning.A_zimbraGalMode);
        SearchGalResult results = null;
        if (mode == null || mode.equals(Provisioning.GM_ZIMBRA)) {
            results = searchZimbraGal(n, maxResults, token, false);
        } else if (mode.equals(Provisioning.GM_LDAP)) {
            results = searchLdapGal(n, maxResults, token);
        } else if (mode.equals(Provisioning.GM_BOTH)) {
            String tokens[] = null;
            if (token != null) {
                tokens = token.split(":");
                if (tokens.length != 2) tokens = null;
            }
            if (tokens == null) tokens = new String[] {null, null}; 
                
            results = searchZimbraGal(n, maxResults/2, tokens[0], false);
            SearchGalResult ldapResults = searchLdapGal(n, maxResults/2, tokens[1]);
            if (ldapResults != null) {
                results.matches.addAll(ldapResults.matches);
                results.token = LdapUtil.getLaterTimestamp(results.token, ldapResults.token);
            }
        } else {
            results = searchZimbraGal(n, maxResults, token, false);
        }
        if (results == null) results = new SearchGalResult();
        if (results.matches == null) results.matches = new ArrayList<GalContact>();

        if (type == Provisioning.GAL_SEARCH_TYPE.ALL) {
            SearchGalResult resourceResults = null;
            if (maxResults == 0)
                resourceResults = searchResourcesGal(n, 0, token, false);
            else {
                int room = maxResults - results.matches.size();
                if (room > 0)
                    resourceResults = searchResourcesGal(n, room, token, false);
            }
            if (resourceResults != null) {
                results.matches.addAll(resourceResults.matches);
                results.token = LdapUtil.getLaterTimestamp(results.token, resourceResults.token);
            }
        }
        return results;
    }
    

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
     */
    public SearchGalResult autoCompleteGal(String n, Provisioning.GAL_SEARCH_TYPE type, int max) throws ServiceException 
    {
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        int maxResults = max; //token != null ? 0 : getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
        if (type == Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE)
            return searchResourcesGal(n, maxResults, null, true);

        String mode = getAttr(Provisioning.A_zimbraGalMode);
        SearchGalResult results = null;
        if (mode == null || mode.equals(Provisioning.GM_ZIMBRA)) {
            results = searchZimbraGal(n, maxResults, null, true);
        } else if (mode.equals(Provisioning.GM_LDAP)) {
            results = searchLdapGal(n, maxResults, null);
        } else if (mode.equals(Provisioning.GM_BOTH)) {
//            String tokens[] = null;
            results = searchZimbraGal(n, maxResults/2, null, true);
            SearchGalResult ldapResults = searchLdapGal(n, maxResults/2, null);
            if (ldapResults != null) {
                results.matches.addAll(ldapResults.matches);
                results.token = LdapUtil.getLaterTimestamp(results.token, ldapResults.token);
                results.hadMore = results.hadMore || ldapResults.hadMore;
            }
        } else {
            results = searchZimbraGal(n, maxResults, null, true);
        }
        if (results == null) results = new SearchGalResult();
        if (results.matches == null) results.matches = new ArrayList<GalContact>();

        if (type == Provisioning.GAL_SEARCH_TYPE.ALL) {
            SearchGalResult resourceResults = null;
            if (maxResults == 0)
                resourceResults = searchResourcesGal(n, 0, null, true);
            else {
                int room = maxResults - results.matches.size();
                if (room > 0)
                    resourceResults = searchResourcesGal(n, room, null, true);
            }
            if (resourceResults != null) {
                results.matches.addAll(resourceResults.matches);
                results.token = LdapUtil.getLaterTimestamp(results.token, resourceResults.token);
                results.hadMore = results.hadMore || resourceResults.hadMore;                
            }
        }
        return results;
    }

    public static String getFilterDef(String name) throws ServiceException {
        String queryExprs[] = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraGalLdapFilterDef);
        String fname = name+":";
        String queryExpr = null;
        for (int i=0; i < queryExprs.length; i++) {
            if (queryExprs[i].startsWith(fname)) {
                queryExpr = queryExprs[i].substring(fname.length());
            }
        }
        return queryExpr;
    }

    private synchronized void initGalAttrs() {
        String[] attrs = getMultiAttr(Provisioning.A_zimbraGalLdapAttrMap);
        List<String> list = new ArrayList<String>(attrs.length);
        Map<String, String> map = new HashMap<String, String>();
        LdapUtil.initGalAttrs(attrs, list, map);
        setCachedData(DATA_GAL_ATTR_MAP, map);
        String[] attr_list = list.toArray(new String[list.size()]);
        setCachedData(DATA_GAL_ATTR_LIST, attr_list);
    }

    private synchronized String[] getGalAttrList() {
        String[] attrs = (String[])getCachedData(DATA_GAL_ATTR_LIST);
        if (attrs == null)
            initGalAttrs();
        return (String[]) getCachedData(DATA_GAL_ATTR_LIST);
    }

    private synchronized Map getGalAttrMap() {
        Map map = (Map) getCachedData(DATA_GAL_ATTR_MAP);
        if (map == null)
            initGalAttrs();
        return (Map) getCachedData(DATA_GAL_ATTR_MAP);
    }

    private SearchGalResult searchResourcesGal(String n, int maxResults, String token, boolean autoComplete)
    throws ServiceException {
        return searchZimbraWithNamedFilter(
                autoComplete ? GAL_FILTER_ZIMBRA_CALENDAR_RESOURCE_AUTO_COMPLETE : GAL_FILTER_ZIMBRA_CALENDAR_RESOURCES, n, maxResults, token);
    }

    private SearchGalResult searchZimbraGal(String n, int maxResults, String token, boolean autoComplete)
    throws ServiceException {
        return searchZimbraWithNamedFilter(
                autoComplete ? GAL_FILTER_ZIMBRA_ACCOUNT_AUTO_COMPLETE : GAL_FILTER_ZIMBRA_ACCOUNTS, n, maxResults, token);
    }

    private SearchGalResult searchZimbraWithNamedFilter(
        String filterName,
        String n,
        int maxResults,
        String token)
    throws ServiceException {
        String queryExpr = getFilterDef(filterName);
        String query = null;
        if (queryExpr != null) {
            if (token != null) n = "";
    
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("s", n);
            query = LdapProvisioning.expandStr(queryExpr, vars);
            if (token != null) {
                if (token.equals(""))
                    query = query.replaceAll("\\*\\*", "*");
                else {
                    String arg = LdapUtil.escapeSearchFilterArg(token);
                    //query = "(&(modifyTimeStamp>="+arg+")"+query.replaceAll("\\*\\*", "*")+")";
                    query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+"))"+query.replaceAll("\\*\\*", "*")+")";
                }
            }
        }
        return searchZimbraWithQuery(query, maxResults, token);
    }

    private SearchGalResult searchZimbraWithQuery(String query, int maxResults, String token)
        throws ServiceException 
    {
        SearchGalResult result = new SearchGalResult();
        result.matches = new ArrayList<GalContact>();
        if (query == null)
            return result;

        // filter out hidden entries
        query = "(&("+query+")(!(zimbraHideInGal=TRUE)))";

        Map galAttrMap = getGalAttrMap();
        String[] galAttrList = getGalAttrList();

        SearchControls sc = new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, galAttrList, true, false);

        result.token = token != null ? token : LdapUtil.EARLIEST_SYNC_TOKEN;
        DirContext ctxt = null;
        NamingEnumeration ne = null;
        try {
            ctxt = LdapUtil.getDirContext(false);
            ne = ctxt.search("ou=people,"+getDN(), query, sc);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
//                Context srctxt = null;

                String dn = sr.getNameInNamespace();
                LdapGalContact lgc = new LdapGalContact(dn, sr.getAttributes(), galAttrList, galAttrMap);
                String mts = (String) lgc.getAttrs().get("modifyTimeStamp");
                result.token = LdapUtil.getLaterTimestamp(result.token, mts);
                String cts = (String) lgc.getAttrs().get("createTimeStamp");
                result.token = LdapUtil.getLaterTimestamp(result.token, cts);
                result.matches.add(lgc);
            }
            ne.close();
            ne = null;
        } catch (SizeLimitExceededException sle) {
            result.hadMore = true;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        } finally {
            LdapUtil.closeEnumContext(ne);
            LdapUtil.closeContext(ctxt);
        }
        //Collections.sort(result);
        return result;
    }

    private SearchGalResult searchLdapGal(String n,
                                          int maxResults,
                                          String token)
    throws ServiceException {
        String url[] = getMultiAttr(Provisioning.A_zimbraGalLdapURL);
        String bindDn = getAttr(Provisioning.A_zimbraGalLdapBindDn);
        String bindPassword = getAttr(Provisioning.A_zimbraGalLdapBindPassword);
        String searchBase = getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
        String filter = getAttr(Provisioning.A_zimbraGalLdapFilter);
        Map galAttrMap = getGalAttrMap();
        String[] galAttrList = getGalAttrList();
        try {
            return LdapUtil.searchLdapGal(url, bindDn, bindPassword, searchBase, filter, n, maxResults, galAttrList, galAttrMap, token);
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        }
    }
 
   public String getAttr(String name) {
        String v = super.getAttr(name);
        if (v != null)
            return v;
        try {
            Config c = mProv.getConfig();
            if (!c.isInheritedDomainAttr(name))
                return null;
            else
                return c.getAttr(name);
        } catch (ServiceException e) {
            return null;
        }
    }

    public String[] getMultiAttr(String name) {
        String v[] = super.getMultiAttr(name);
        if (v.length > 0)
            return v;
        try {
            Config c = mProv.getConfig();
            if (!c.isInheritedDomainAttr(name))
                return sEmptyMulti;
            else
                return c.getMultiAttr(name);
        } catch (ServiceException e) {
            return sEmptyMulti;
        }
    }

    public Map<String, Object> getAttrs() throws ServiceException {
        return getAttrs(true);
    }

    public Map<String, Object> getAttrs(boolean applyConfig) throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        try {
            // get all the server attrs
            LdapUtil.getAttrs(mAttrs, attrs, null);
            
            if (!applyConfig)
                return attrs;
            // then enumerate through all inheritable attrs and add them if needed
            Config c = mProv.getConfig();
            String[] inheritable = mProv.getConfig().getMultiAttr(Provisioning.A_zimbraDomainInheritedAttr);
            for (int i=0; i < inheritable.length; i++) {
                Object value = attrs.get(inheritable[i]);
                if (value == null)
                    value = c.getMultiAttr(inheritable[i]);
                if (value != null)
                    attrs.put(inheritable[i], value);
            }
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to get attrs", e);
        }
        return attrs;
    }
}
