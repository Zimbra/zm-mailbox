package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;

public class Bug59720 extends LegacyLdapUpgrade {
	Bug59720() throws ServiceException {
		
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
    
    private void doEntry(LegacyZimbraLdapContext zlc, Cos cos) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraFilterSleepInterval;
        String oldValue = "100ms";  
        String newValue = "1ms";
        
        String curVal = cos.getAttr(attrName);
        System.out.print("Checking cos [" + cos.getName() + "]: " + "current value of " + attrName + " is " + curVal);
        
        
        if (newValue.equals(curVal)) {
            System.out.println(" => not updating ");
            return;
        }
        
        Map<String, Object> attrValues = new HashMap<String, Object>();
        attrValues.put(attrName, newValue);   
        try {
            System.out.println(" => updating to " + newValue);
            LegacyLdapUpgrade.modifyAttrs(cos, zlc, attrValues);
        } catch (ServiceException e) {
            // log the exception and continue
            System.out.println("Caught ServiceException while modifying " + cos.getName());
            e.printStackTrace();
        } catch (NamingException e) {
            // log the exception and continue
            System.out.println("Caught NamingException while modifying " + cos.getName());
            e.printStackTrace();
        }

    }

    private void doAllCos(LegacyZimbraLdapContext zlc) throws ServiceException {
        List<Cos> coses = mProv.getAllCos();
        
        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos);
        }
    }
}
