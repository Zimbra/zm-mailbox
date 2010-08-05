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
package com.zimbra.cs.account.ldap;

// use LinkedHashSet to preserve the order and uniqueness of entries, not that the order/uniqueness matters
// LDAP server, just cleaner this way
import java.util.LinkedHashSet;
import java.util.Set;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;

public class LdapObjectClass {
    
    static String ZIMBRA_DEFAULT_PERSON_OC = "organizationalPerson";
    
    private static void addExtraObjectClasses(Set<String> ocs, LdapProvisioning prov, String extraOCAttr) throws ServiceException {
        String[] extraObjectClasses = prov.getConfig().getMultiAttr(extraOCAttr);
        for (String eoc : extraObjectClasses) {
            ocs.add(eoc);
        }
        
        /*
        if (additionalObjectClasses != null) {
            for (int i = 0; i < additionalObjectClasses.length; i++)
                ocs.add(additionalObjectClasses[i]);
        }
        */
    }
    
    public static Set<String> getAccountObjectClasses(LdapProvisioning prov, boolean zimbraDefaultOnly) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(ZIMBRA_DEFAULT_PERSON_OC);
        ocs.add(LdapProvisioning.C_zimbraAccount);
        
        if (!zimbraDefaultOnly)
            addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraAccountExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getAccountObjectClasses(LdapProvisioning prov) throws ServiceException {
        return getAccountObjectClasses(prov, false);
    }
    
    public static Set<String> getCalendarResourceObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraCalendarResource);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraCalendarResourceExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getCosObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraCOS);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraCosExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getDomainObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add("dcObject");
        ocs.add("organization");
        ocs.add(LdapProvisioning.C_zimbraDomain);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraDomainExtraObjectClass);
        return ocs;
    }
    
    public static Set<String> getServerObjectClasses(LdapProvisioning prov) throws ServiceException {
        Set<String> ocs = new LinkedHashSet<String>();
        
        ocs.add(LdapProvisioning.C_zimbraServer);
        
        addExtraObjectClasses(ocs, prov, Provisioning.A_zimbraServerExtraObjectClass);
        return ocs;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ServiceException {
        LdapProvisioning prov = (LdapProvisioning)Provisioning.getInstance();
        Set<String> ocs = LdapObjectClass.getCalendarResourceObjectClasses(prov);

        for (String oc : ocs) {
            System.out.println(oc);
        }
        
    }

}
