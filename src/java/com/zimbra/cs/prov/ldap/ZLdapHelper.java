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
package com.zimbra.cs.prov.ldap;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.ILdapContext;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapException;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZSearchControls;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchResultEnumeration;
import com.zimbra.cs.ldap.LdapException.LdapMultipleEntriesMatchedException;
import com.zimbra.cs.ldap.LdapTODO.TODOEXCEPTIONMAPPING;

/**
 * An SDK-neutral LdapHelper.  Based on Z* classes and LdapUtil in the com.zimbra.cs.ldap package.
 * 
 * @author pshao
 *
 */
public class ZLdapHelper extends LdapHelper {
    
    ZLdapHelper(LdapProv ldapProv) {
        super(ldapProv);
    }

    @Override
    public void searchLdap(ILdapContext ldapContext, SearchLdapOptions searchOptions) 
    throws ServiceException {
        
        ZLdapContext zlc = LdapClient.toZLdapContext(getProv(), ldapContext);
        zlc.searchPaged(searchOptions);
    }
    

    @Override
    @TODOEXCEPTIONMAPPING
    public ZSearchResultEntry searchForEntry(String base, String query, ZLdapContext initZlc, 
            boolean useMaster) throws LdapMultipleEntriesMatchedException, ServiceException {
        ZLdapContext zlc = initZlc;
        try {
            if (zlc == null)
                zlc = LdapClient.getContext(LdapServerType.get(useMaster));
            
            ZSearchResultEnumeration ne = zlc.searchDir(base, query, ZSearchControls.SEARCH_CTLS_SUBTREE());
            if (ne.hasMore()) {
                ZSearchResultEntry sr = ne.next();
                if (ne.hasMore()) {
                    String dups = LdapUtil.formatMultipleMatchedEntries(sr, ne);
                    throw LdapException.MULTIPLE_ENTRIES_MATCHED(base, query, dups);
                }
                ne.close();
                return sr;
            }
        /*  all callsites with the following @TODOEXCEPTIONMAPPING pattern can have ease of mind now and remove the 
         * TODOEXCEPTIONMAPPING annotation
         *  
        } catch (NameNotFoundException e) {
            return null;
        } catch (InvalidNameException e) {
            return null;
        } catch (NamingException e) {
            throw ServiceException.FAILURE("unable to lookup account via query: "+query+" message: "+e.getMessage(), e);
        */
        } finally {
            if (initZlc == null)
                LdapClient.closeContext(zlc);
        }
        return null;
    }

}
