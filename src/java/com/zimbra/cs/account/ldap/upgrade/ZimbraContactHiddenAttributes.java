package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class ZimbraContactHiddenAttributes extends LdapUpgrade {

    ZimbraContactHiddenAttributes() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        ZimbraLdapContext zlc = new ZimbraLdapContext(true);
        try {
            doGlobalConfig(zlc);
            doAllServers(zlc);
        } finally {
            ZimbraLdapContext.closeContext(zlc);
        }
    }
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraContactHiddenAttributes;
        
        System.out.println();
        System.out.println("Checking " + entryName);
        
        String oldValue = "dn,zimbraAccountCalendarUserType,zimbraCalResType,zimbraCalResLocationDisplayName,zimbraCalResCapacity,zimbraCalResContactEmail";
        String newValue = "dn,zimbraAccountCalendarUserType,zimbraCalResType,zimbraCalResLocationDisplayName,zimbraCalResCapacity,zimbraCalResContactEmail,vcardUID,vcardURL,vcardXProps";
        
        String curValue = entry.getAttr(attrName);
        
        boolean needsUpdate = curValue == null || (oldValue.equals(curValue) && !newValue.equals(curValue));
        
        if (needsUpdate) {
            System.out.println("    Modifying " + attrName + " on " + entryName + 
                    " from [" + curValue + "] to [" + newValue + "]");
            
            Map<String, Object> attr = new HashMap<String, Object>();
            attr.put(attrName, newValue);
            mProv.modifyAttrs(entry, attr);
        } else {
            System.out.println("    " + attrName + " already has an effective value: [" + curValue + "] on entry " + entryName + " - skipping"); 
        }
    }
    
    private void doGlobalConfig(ZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config");
    }
    
    private void doAllServers(ZimbraLdapContext zlc) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            doEntry(zlc, server, "server " + server.getName());
    }
}
