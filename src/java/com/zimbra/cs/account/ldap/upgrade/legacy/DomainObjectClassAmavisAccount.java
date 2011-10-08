/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapProvisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;

public class DomainObjectClassAmavisAccount extends LegacyLdapUpgrade {

    DomainObjectClassAmavisAccount() throws ServiceException {
    }
    
    static class AddDomainObjectClassAmavisAccountVisitor extends LegacyLdapUpgrade.UpgradeVisitor implements NamedEntry.Visitor {
        int mDomainsVisited;
    
        AddDomainObjectClassAmavisAccountVisitor(LdapProv prov, LegacyZimbraLdapContext zlcForMod, boolean verbose) {
            super(prov, zlcForMod, verbose);
        }
        
        public void visit(NamedEntry entry) {
            if (!(entry instanceof Domain)) {
                // should not happen
                System.out.println("Encountered non domain object: " + entry.getName() + ", skipping");
                return;
            }
            
            mDomainsVisited++;
            
            Domain domain = (Domain)entry;
            
            Map<String, Object> attrs = new HashMap<String, Object>(); 
            attrs.put("+" + Provisioning.A_objectClass, "amavisAccount");
            
            try {
                System.out.format("Updating domain %-30s: objectClass=amavisAccount\n",
                                  domain.getName());
                LegacyLdapUpgrade.modifyAttrs(domain, mZlcForMod, attrs);
            } catch (ServiceException e) {
                // log the exception and continue
                System.out.println("Caught ServiceException while modifying domain " + domain.getName());
                e.printStackTrace();
            } catch (NamingException e) {
                // log the exception and continue
                System.out.println("Caught NamingException while modifying domain " + domain.getName());
                e.printStackTrace();
            }
        }
        
        void reportStat() {
            System.out.println();
            System.out.println("Number of domains modified = " + mDomainsVisited);
            System.out.println();
        }
    }
    
    /**
     * bug 32557
     * 
     * Add objectClass=amavisAccount to all existing domains
     * 
     * @throws ServiceException
     */
    void doUpgrade() throws ServiceException {
        
        String query = "(&(objectClass=zimbraDomain)(!(objectClass=amavisAccount)))";
        String bases[] = mProv.getDIT().getSearchBases(Provisioning.SD_DOMAIN_FLAG);
        String attrs[] = new String[] {Provisioning.A_objectClass,
                                       Provisioning.A_zimbraId,
                                       Provisioning.A_zimbraDomainName};
                
        LegacyZimbraLdapContext zlc = null; 
        AddDomainObjectClassAmavisAccountVisitor visitor = null;
        
        try {
            zlc = new LegacyZimbraLdapContext(true);
            
            visitor = new AddDomainObjectClassAmavisAccountVisitor(mProv, zlc,  mVerbose);
            
            for (String base : bases) {
                // should really have one base, but iterate thought the array anyway
                if (mVerbose) {
                    System.out.println("LDAP search base: " + base);
                    System.out.println("LDAP search query: " + query);
                    System.out.println();
                }
                
                ((LegacyLdapProvisioning) mProv).searchObjects(query, attrs, base,
                                    Provisioning.SO_NO_FIXUP_OBJECTCLASS | Provisioning.SO_NO_FIXUP_RETURNATTRS, // turn off fixup for objectclass and return attrs
                                    visitor, 
                                    0,      // return all entries that satisfy filter.
                                    false,  // do not use connection pool, for the OpenLdap bug (see bug 24168) might still be there
                                    true);  // use LDAP master
             
            }
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
            if (visitor != null)
                visitor.reportStat();
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
