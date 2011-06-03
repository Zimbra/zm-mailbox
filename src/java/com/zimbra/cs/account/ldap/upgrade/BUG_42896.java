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
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_42896 extends UpgradeOp {

    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }

    }
    
    private void setZimbraMailQuotaConstraint(ZLdapContext zlc, Cos cos) {
        
        String quotaLimitOnCosStr = cos.getAttr(Provisioning.A_zimbraDomainAdminMaxMailQuota);
        
        printer.println("Cos " + cos.getName() + ": " + Provisioning.A_zimbraDomainAdminMaxMailQuota + "=" + quotaLimitOnCosStr);
        
        if (quotaLimitOnCosStr == null) {
            printer.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName());
            return;
        }
        
        long quotaLimitOnCos = Long.parseLong(quotaLimitOnCosStr);
        
        // no quota limitation
        if (quotaLimitOnCos == 0) {
            printer.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName());
            return;
        }
        
        // delegated admin cannot change quota at all 
        // (the right to set zimbraMailQuota had been revoked in the AdminRights upgrade, don't need to set constraint here)
        if (quotaLimitOnCos == -1) {
            printer.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName());
            return;
        }
        
        Set<String> constraints = cos.getMultiAttrSet(Provisioning.A_zimbraConstraint);
        
        for (String constraint : constraints) {
            if (constraint.startsWith(Provisioning.A_zimbraMailQuota)) {
                printer.println("Skip setting constraint for " + Provisioning.A_zimbraMailQuota + " on cos " + cos.getName() + ", it is currently set to " + constraint);
                return;
            }
        }
        
        // there is currently no constraint for zimbraMailQuota, add it
        String value = Provisioning.A_zimbraMailQuota + ":max=" + quotaLimitOnCos;
        constraints.add(value);
        
        Map<String, Object> newValues = new HashMap<String, Object>();
        newValues.put(Provisioning.A_zimbraConstraint, constraints.toArray(new String[constraints.size()]));
        
        try {
            printer.println("Modifying " + Provisioning.A_zimbraConstraint + " on cos " + cos.getName() + ", adding value " + value);
            modifyAttrs(zlc, cos, newValues);
        } catch (ServiceException e) {
            // log the exception and continue
            printer.println("Caught ServiceException while modifying " + Provisioning.A_zimbraConstraint + " attribute ");
            printer.printStackTrace(e);
        }
    }
    
    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();
        
        for (Cos cos : coses) {
            setZimbraMailQuotaConstraint(zlc, cos);
        }
    }

}
