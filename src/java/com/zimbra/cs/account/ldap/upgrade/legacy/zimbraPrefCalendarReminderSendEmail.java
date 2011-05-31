package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapFilter;
import com.zimbra.cs.account.ldap.legacy.LegacyLdapUtil;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;

public class zimbraPrefCalendarReminderSendEmail extends LegacyLdapUpgrade {
    
    zimbraPrefCalendarReminderSendEmail() throws ServiceException {
    }
    
    
    
    @Override
    void doUpgrade() throws ServiceException {
        LegacyZimbraLdapContext zlc = new LegacyZimbraLdapContext(true);
        try {
            doAllCos(zlc);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private void doEntry(LegacyZimbraLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraPrefCalendarReminderSendEmail;
        String oldValue = "FALSE";
        String newValue = "TRUE";
        
        String curVal = entry.getAttr(attrName);
        System.out.print("Checking " + entryName + ": " + "current value of " + attrName + " is " + curVal);
        
        if (curVal != null && !oldValue.equals(curVal)) {
            System.out.println(" => not updating ");
            return;
        }
        
        Map<String, Object> attrValues = new HashMap<String, Object>();
        attrValues.put(attrName, newValue);   
        try {
            System.out.println(" => updating to " + newValue);
            LegacyLdapUpgrade.modifyAttrs(entry, zlc, attrValues);
        } catch (ServiceException e) {
            // log the exception and continue
            System.out.println("Caught ServiceException while modifying " + entryName);
            e.printStackTrace();
        } catch (NamingException e) {
            // log the exception and continue
            System.out.println("Caught NamingException while modifying " + entryName);
            e.printStackTrace();
        }

    }
    
    private void doAllCos(LegacyZimbraLdapContext zlc) throws ServiceException {
        List<Cos> coses = mProv.getAllCos();
        
        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos, name, AttributeClass.cos);
        }
    }
}
