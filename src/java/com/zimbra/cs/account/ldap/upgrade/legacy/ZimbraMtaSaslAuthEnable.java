package com.zimbra.cs.account.ldap.upgrade.legacy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.legacy.LegacyZimbraLdapContext;
import com.zimbra.cs.ldap.LdapConstants;

public class ZimbraMtaSaslAuthEnable extends LegacyLdapUpgrade {
    
    ZimbraMtaSaslAuthEnable() throws ServiceException {
    }
    
    @Override
    void doUpgrade() throws ServiceException {
        LegacyZimbraLdapContext zlc = new LegacyZimbraLdapContext(true);
        try {
            doGlobalConfig(zlc);
            doAllServers(zlc);
        } finally {
            LegacyZimbraLdapContext.closeContext(zlc);
        }

    }
    
    private void doEntry(LegacyZimbraLdapContext zlc, Entry entry, String entryName) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraMtaSaslAuthEnable;
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Checking " + attrName + " on " + entryName);
        
        String curValue = entry.getAttr(attrName, false);
        if (curValue != null) {
            String newValue =LdapConstants.LDAP_FALSE.equals(curValue) ? "no" : "yes";
            if (!curValue.equals(newValue)) {
                System.out.println("    Changing " + attrName + " on " + entryName + " from " + curValue + " to " + newValue);
                
                Map<String, Object> attr = new HashMap<String, Object>();
                attr.put(attrName, newValue);
                try {
                    LegacyLdapUpgrade.modifyAttrs(entry, zlc, attr);
                } catch (NamingException e) {
                    // log the exception and continue
                    System.out.println("Caught NamingException while modifying " + entryName + " attribute " + attr);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void doGlobalConfig(LegacyZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config");
    }
    
    private void doAllServers(LegacyZimbraLdapContext zlc) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            doEntry(zlc, server, "server " + server.getName());
    }
}
