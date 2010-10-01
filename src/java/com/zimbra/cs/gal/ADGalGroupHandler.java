/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.gal;

import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.account.ldap.LdapUtil.SearchLdapVisitor;

public class ADGalGroupHandler extends GalGroupHandler {

    private static final String[] sEmptyMembers = new String[0];
    private static final String MAIL_ATTR = "mail";
    
    @Override
    public boolean isGroup(SearchResult sr) {
        Attributes ldapAttrs = sr.getAttributes();
        Attribute objectclass = ldapAttrs.get(Provisioning.A_objectClass);
        return objectclass.contains("group");
    }

    @Override
    public String[] getMembers(ZimbraLdapContext zlc, SearchResult sr) {
        String dn = sr.getNameInNamespace();
        if (ZimbraLog.gal.isDebugEnabled()) {
            try {
                ZimbraLog.gal.debug("Fetching members for group " + LdapUtil.getAttrString(sr.getAttributes(), MAIL_ATTR));
            } catch (NamingException e) {
                ZimbraLog.gal.debug("unable to get email address of group " + dn, e);
            }
        }
        TreeSet<String> result = searchLdap(zlc, dn);
        return result.toArray(new String[result.size()]);
    }
    
    private TreeSet<String> searchLdap(ZimbraLdapContext zlc, String dnOfGroup) {
        TreeSet result = new TreeSet<String>();
        
        int maxResults = 0; // no limit
        String base = "dc=vmware,dc=com"; //TODO
        String query = "(memberof=" + dnOfGroup + ")";
        String[] returnAttrs = new String[]{MAIL_ATTR};
        
        try {
            SearchControls searchControls =
                new SearchControls(SearchControls.SUBTREE_SCOPE, maxResults, 0, returnAttrs, false, false);

            //Set the page size and initialize the cookie that we pass back in subsequent pages
            int pageSize = LdapUtil.adjustPageSize(maxResults, 1000);
            byte[] cookie = null;

            NamingEnumeration ne = null;
            
            try {
                do {
                    zlc.setPagedControl(pageSize, cookie, true);

                    ne = zlc.searchDir(base, query, searchControls);
                    while (ne != null && ne.hasMore()) {
                        SearchResult sr = (SearchResult) ne.nextElement();
                        // String dn = sr.getNameInNamespace();
                        Attributes attrs = sr.getAttributes();
                        String email = LdapUtil.getAttrString(attrs, MAIL_ATTR);
                        if (email != null)
                            result.add(email);
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } finally {
                if (ne != null) ne.close();
            }
        } catch (NamingException e) {
            // log and continue
            ZimbraLog.gal.warn("unable to search group members", e);
        } catch (IOException e) {
            // log and continue
            ZimbraLog.gal.warn("unable to search group members", e);
        } finally {
            // we didn't open this connection, we don't close it
        }
        
        return result;
    }
    
}
