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

    /* (non-Javadoc)
     * @see com.zimbra.cs.account.Domain#getAllAccounts()
     */
    public List getAllAccounts() throws ServiceException {
        return searchAccounts("(objectclass=liquidAccount)", null, null, true);
    }
    
    
    public ArrayList searchAccounts(String query, String returnAttrs[], String sortAttr, boolean sortAscending) throws ServiceException
    {
        return mProv.searchAccounts(query, returnAttrs, sortAttr, sortAscending, "ou=people,"+getDN());
    }
    
    /*
	 * (non-Javadoc)
	 * 
	 * @see com.zimbra.cs.account.Provisioning#searchGal(java.lang.String)
	 */
    public List searchGal(String n) throws ServiceException {
        // escape user-supplied string
        n = LdapUtil.escapeSearchFilterArg(n);

        String mode = getAttr(Provisioning.A_zimbraGalMode);
        int maxResults = getIntAttr(Provisioning.A_zimbraGalMaxResults, DEFAULT_GAL_MAX_RESULTS);
                
        if (mode == null || mode.equals(Provisioning.GM_ZIMBRA))
            return searchZimbraGal(n, maxResults);
        
        List results = null; 
        if (mode.equals(Provisioning.GM_LDAP)) {
            results = searchLdapGal(n, maxResults);
        } else if (mode.equals(Provisioning.GM_BOTH)) {
            results = searchZimbraGal(n, maxResults/2);
            results.addAll(searchLdapGal(n, maxResults/2));
        } else {
            results = searchZimbraGal(n, maxResults);
        }
        return results;
    }
    
    private static final String ZIMBRA_DEF = "liquid";

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
    private List searchZimbraGal(String n, int maxResults) throws ServiceException {
        String queryExpr = getFilterDef(ZIMBRA_DEF);
        ArrayList result = new ArrayList();
        if (queryExpr == null)
            return result;

        Map vars = new HashMap();
        vars.put("s", n);
        String query = LdapProvisioning.expandStr(queryExpr, vars);
        
        Map galAttrMap = getGalAttrMap();
        String[] galAttrList = getGalAttrList();
        
        SearchControls sc = new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, galAttrList, true, false);

        DirContext ctxt = null;
        NamingEnumeration ne = null;
        try {
            ctxt = LdapUtil.getDirContext();
            ne = ctxt.search("ou=people,"+getDN(), query, sc);
            while (ne.hasMore()) {
                SearchResult sr = (SearchResult) ne.next();
                Context srctxt = null;
                try {
                    srctxt = (Context) sr.getObject();
                    String dn = srctxt.getNameInNamespace();
                    result.add(new LdapGalContact(dn, sr.getAttributes(), galAttrList, galAttrMap));
                } finally {
                    if (srctxt != null)
                        srctxt.close();
                }
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

    private List searchLdapGal(String n, int maxResults) throws ServiceException {
        String url = getAttr(Provisioning.A_zimbraGalLdapURL);
        String bindDn = getAttr(Provisioning.A_zimbraGalLdapBindDn);
        String bindPassword = getAttr(Provisioning.A_zimbraGalLdapBindPassword);
        String searchBase = getAttr(Provisioning.A_zimbraGalLdapSearchBase, "");
        String filter = getAttr(Provisioning.A_zimbraGalLdapFilter);
        Map galAttrMap = getGalAttrMap();
        String[] galAttrList = getGalAttrList();
        try {
            return LdapUtil.searchLdapGal(url, bindDn, bindPassword, searchBase, filter, n, maxResults, galAttrList, galAttrMap);
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
