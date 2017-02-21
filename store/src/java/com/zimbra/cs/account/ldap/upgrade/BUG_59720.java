/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapServerType;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.ZLdapContext;

public class BUG_59720 extends UpgradeOp {

    
    @Override
    void doUpgrade() throws ServiceException {
        ZLdapContext zlc = LdapClient.getContext(LdapServerType.MASTER, LdapUsage.UPGRADE);
        try {
            doAllCos(zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    private void doEntry(ZLdapContext zlc, Cos cos) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraFilterSleepInterval;
        String oldValue = "100ms";  
        String newValue = "1ms";
        
        String curVal = cos.getAttr(attrName);
        printer.print("Checking cos [" + cos.getName() + "]: " + "current value of " + attrName + " is " + curVal);
        
        if (newValue.equals(curVal)) {
            printer.println(" => not updating ");
            return;
        }
        
        Map<String, Object> attrValues = new HashMap<String, Object>();
        attrValues.put(attrName, newValue);   
        try {
            printer.println(" => updating to " + newValue);
            modifyAttrs(zlc, cos, attrValues);
        } catch (ServiceException e) {
            // log the exception and continue
            printer.println("Caught ServiceException while modifying " + cos.getName());
            printer.printStackTrace(e);
        }

    }

    private void doAllCos(ZLdapContext zlc) throws ServiceException {
        List<Cos> coses = prov.getAllCos();
        
        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos);
        }
    }

}
