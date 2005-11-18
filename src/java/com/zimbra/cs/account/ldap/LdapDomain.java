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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.SizeLimitExceededException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LdapDomain extends LdapNamedEntry implements Domain {

    private static final int DEFAULT_GAL_MAX_RESULTS = 100;

    private static final String DATA_GAL_ATTR_MAP = "GAL_ATTRS_MAP";
    private static final String DATA_GAL_ATTR_LIST = "GAL_ATTR_LIST";

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
    
    public List getAllDistributionLists() throws ServiceException {
        return searchAccounts("(objectClass=zimbraDistributionList)", null, null, true, Provisioning.SA_DISTRIBUTION_LIST_FLAG);
    }

    public ArrayList searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending, int flags) throws ServiceException
    {
        return mProv.searchAccounts(query, returnAttrs, sortAttr, sortAscending, "ou=people,"+getDN(), flags);
    }
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
	 */
    public SearchGalResult searchGal(String n, String token) throws ServiceException {
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        String mode = getAttr(Provisioning.A_zimbraGalMode);
        int maxResults = token != null ? 0 : getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
        
        if (mode == null || mode.equals(Provisioning.GM_ZIMBRA))
            return searchZimbraGal(n, maxResults, token);
        
        SearchGalResult results = null; 
        if (mode.equals(Provisioning.GM_LDAP)) {
            results = searchLdapGal(n, maxResults, token);
        } else if (mode.equals(Provisioning.GM_BOTH)) {
            String tokens[] = null;
            if (token != null) {
                tokens = token.split(":");
                if (tokens.length != 2) tokens = null;
            }
            if (tokens == null) tokens = new String[] {null, null}; 
                
            results = searchZimbraGal(n, maxResults/2, tokens[0]);
            SearchGalResult ldapResults = searchLdapGal(n, maxResults/2, tokens[1]);            
            results.matches.addAll(ldapResults.matches);
            if (results.token != null) {
                results.token = results.token + ":" + ldapResults.token;
            }
        } else {
            results = searchZimbraGal(n, maxResults, token);
        }
        if (results == null) results = new SearchGalResult();
        if (results.matches == null) results.matches = new ArrayList();

        return results;
    }
    
    private static final String ZIMBRA_DEF = "zimbra";

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
        ArrayList list = new ArrayList(attrs.length);
        HashMap map = new HashMap();
        LdapUtil.initGalAttrs(attrs, list, map);
        setCachedData(DATA_GAL_ATTR_MAP, map);
        String[] attr_list = (String[]) list.toArray(new String[list.size()]);
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

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
     */
    private SearchGalResult searchZimbraGal(String n, int maxResults, String token) throws ServiceException {
        String queryExpr = getFilterDef(ZIMBRA_DEF);
        SearchGalResult result = new SearchGalResult();
        result.matches = new ArrayList();
        if (queryExpr == null)
            return result;

        if (token != null) n = "";

        Map vars = new HashMap();
        vars.put("s", n);
        String query = LdapProvisioning.expandStr(queryExpr, vars);
        if (token != null) {
            if (token.equals(""))
                query = query.replaceAll("\\*\\*", "*");
            else {
                String arg = LdapUtil.escapeSearchFilterArg(token);
                //query = "(&(modifyTimeStamp>="+arg+")"+query.replaceAll("\\*\\*", "*")+")";
                query = "(&(|(modifyTimeStamp>="+arg+")(createTimeStamp>="+arg+"))"+query.replaceAll("\\*\\*", "*")+")";
            }
        }
        Map galAttrMap = getGalAttrMap();
        String[] galAttrList = getGalAttrList();

        SearchControls sc = new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, galAttrList, true, false);

        result.token = null;
        DirContext ctxt = null;
        NamingEnumeration ne = null;
        try {
            ctxt = LdapUtil.getDirContext();
            ne = ctxt.search("ou=people,"+getDN(), query, sc);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                Context srctxt = null;

                String dn = sr.getNameInNamespace();
                LdapGalContact lgc = new LdapGalContact(dn, sr.getAttributes(), galAttrList, galAttrMap);
                String mts = (String) lgc.getAttrs().get("modifyTimeStamp");
                if (result.token == null || (mts !=null && (mts.compareTo(result.token) > 0))) result.token = mts;                    
                String cts = (String) lgc.getAttrs().get("createTimeStamp");                    
                if (result.token == null || (cts !=null && (cts.compareTo(result.token) > 0))) result.token = cts;                    
                result.matches.add(lgc);
            }
            ne.close();
            ne = null;
        } catch (SizeLimitExceededException sle) {
            // ignore
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to search GAL", e);
        } finally {
            LdapUtil.closeEnumContext(ne);
            LdapUtil.closeContext(ctxt);
        }
        //Collections.sort(result);
        return result;
    }

    private SearchGalResult searchLdapGal(String n, int maxResults, String token) throws ServiceException {
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

    public Map getAttrs() throws ServiceException {
        return getAttrs(true);
    }

    public Map getAttrs(boolean applyConfig) throws ServiceException {
        HashMap attrs = new HashMap();
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
