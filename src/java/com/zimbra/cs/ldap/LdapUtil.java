/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.ldap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalParams;
import com.zimbra.cs.account.ldap.LdapGalMapRules;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.gal.GalSearchConfig;
import com.zimbra.cs.ldap.LdapTODO.*;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class LdapUtil {
    
    //
    // Escape rdn value defined in:
    // http://www.ietf.org/rfc/rfc2253.txt?number=2253
    //
    @TODO   // see impl in LegacyLdapUtil, do we need to for unboundid?
    public static String escapeRDNValue(String rdn) {
        /*
        LdapTODO.TODO();
        return null;
        */
        return rdn;
    }
    
    @TODO
    public static String unescapeRDNValue(String rdn) {
        LdapTODO.TODO();
        return null;
    }
    
    public static String formatMultipleMatchedEntries(ZSearchResultEntry first, ZSearchResultEnumeration rest) 
    throws LdapException {
        StringBuffer dups = new StringBuffer();
        dups.append("[" + first.getDN() + "] ");
        while (rest.hasMore()) {
            ZSearchResultEntry dup = rest.next();
            dups.append("[" + dup.getDN() + "] ");
        }
        
        return new String(dups);
    }
    
    public static void searchLdapOnMaster(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, true, visitor);
    }

    public static void searchLdapOnReplica(String base, String query, String[] returnAttrs, 
            SearchLdapVisitor visitor) throws ServiceException {
        searchZimbraLdap(base, query, returnAttrs, false, visitor);
    }
      
    private static void searchZimbraLdap(String base, String query, String[] returnAttrs, 
            boolean useMaster, SearchLdapVisitor visitor) throws ServiceException {
        
        SearchLdapOptions searchOptions = new SearchLdapOptions(base, query, returnAttrs, null, visitor);
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getContext(LdapServerType.get(useMaster));
            zlc.searchPaged(searchOptions);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    @TODO // see impl in LegacyLdapUtil
    public static void searchGal(ZLdapContext zlc,
            GalSearchConfig.GalType galType,
            int pageSize,
            String base, 
            String query, 
            int maxResults,
            LdapGalMapRules rules,
            String token,
            SearchGalResult result) throws ServiceException {
        LdapTODO.TODO();
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static SearchGalResult searchLdapGal(
            GalParams.ExternalGalParams galParams, 
            GalOp galOp,
            String n,
            int maxResults,
            LdapGalMapRules rules,
            String token,
            GalContact.Visitor visitor) throws ServiceException {
        LdapTODO.TODO();
        return null;
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static void ldapAuthenticate(String urls[], boolean requireStartTLS, String principal, String password) {
        LdapTODO.TODO();
    }
    
    @TODO // see impl in LegacyLdapUtil
    public static void ldapAuthenticate(String url[], boolean requireStartTLS, String password, String searchBase, 
            String searchFilter, String searchDn, String searchPassword) throws ServiceException {
        LdapTODO.TODO();
    }
}
