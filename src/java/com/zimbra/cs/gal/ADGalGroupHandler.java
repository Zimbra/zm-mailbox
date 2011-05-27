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

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapHelper;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class ADGalGroupHandler extends GalGroupHandler {

    private static final String MAIL_ATTR = "mail";
    
    @Override
    public boolean isGroup(IAttributes ldapAttrs) {
        try {
            List<String> objectclass = ldapAttrs.getMultiAttrStringAsList(
                    Provisioning.A_objectClass, IAttributes.CheckBinary.NOCHECK);
            return objectclass.contains("group");
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("unable to get attribute " + Provisioning.A_objectClass, e);
        }
        return false;
    }

    @Override
    public String[] getMembers(ILdapContext ldapContext, String searchBase, String entryDN, IAttributes ldapAttrs) {
        if (ZimbraLog.gal.isDebugEnabled()) {
            try {
                ZimbraLog.gal.debug("Fetching members for group " + ldapAttrs.getAttrString(MAIL_ATTR));
            } catch (ServiceException e) {
                ZimbraLog.gal.debug("unable to get email address of group " + entryDN, e);
            }
        }
        
        SearchADGroupMembers searcher = new SearchADGroupMembers();
        TreeSet<String> result = searcher.searchLdap(ldapContext, searchBase, entryDN);
        return result.toArray(new String[result.size()]);
    }
    
    
    private static class SearchADGroupMembers extends SearchLdapVisitor {

        TreeSet<String> result = new TreeSet<String>();
        
        SearchADGroupMembers() {
            super(false);
        }
        
        @Override
        public void visit(String dn, IAttributes ldapAttrs) {
            String email;
            try {
                email = ldapAttrs.getAttrString(MAIL_ATTR);
                if (email != null) {
                    result.add(email);
                }
            } catch (ServiceException e) {
                // swallow exceptions and continue
                ZimbraLog.gal.warn("unable to get attribute " + MAIL_ATTR + " from search result", e);
            }
        }
        
        private TreeSet<String> searchLdap(ILdapContext zlc, String searchBase, String dnOfGroup) {
            
            String query = "(memberof=" + dnOfGroup + ")";
            String[] returnAttrs = new String[]{MAIL_ATTR};
            
            try {
                LdapHelper ldapHelper = LdapProv.getInst().getHelper();
                SearchLdapOptions searchOptions = new SearchLdapOptions(searchBase, query, 
                        returnAttrs, SearchLdapOptions.SIZE_UNLIMITED, null, 
                        ZSearchScope.SEARCH_SCOPE_SUBTREE, this);
                ldapHelper.searchLdap(zlc, searchOptions);
            } catch (ServiceException e) {
                // log and continue
                ZimbraLog.gal.warn("unable to search group members", e);
            }
                        
            return result;
        }
    }
    
}
