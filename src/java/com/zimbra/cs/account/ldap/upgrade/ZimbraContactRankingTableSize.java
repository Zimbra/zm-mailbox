package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.util.BuildInfo;

public class ZimbraContactRankingTableSize extends LdapUpgrade {
    
    ZimbraContactRankingTableSize() throws ServiceException {
    }
    
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doAllCos(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraContactRankingTableSize;
        String oldValue = "40";
        String newValue = "200";
        
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
            LdapUpgrade.modifyAttrs(entry, zlc, attrValues);
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
    
    private void doAllCos(ZimbraLdapContext zlc) throws ServiceException {
        List<Cos> coses = mProv.getAllCos();
        
        for (Cos cos : coses) {
            String name = "cos " + cos.getName();
            doEntry(zlc, cos, name, AttributeClass.cos);
        }
    }
    
}
