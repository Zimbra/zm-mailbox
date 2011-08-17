package com.zimbra.cs.account.ldap.upgrade;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

import javax.naming.NamingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bug63475 extends LdapUpgrade {

    Bug63475() throws ServiceException {
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
        String attrName = Provisioning.A_zimbraMailSSLProxyClientCertPort;
        String oldDefaultValue = "0";
        String newDefaultValue = "3443";
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Checking " + attrName + " on " + entryName);
        
        String curValue = entry.getAttr(attrName, false);
        if (oldDefaultValue.equals(curValue)) {
            System.out.println(
                    "    Changing " + attrName + " on " + entryName + " from " + curValue + " to " + newDefaultValue);

            Map<String, Object> attr = new HashMap<String, Object>();
            attr.put(attrName, newDefaultValue);
            try {
                LdapUpgrade.modifyAttrs(entry, zlc, attr);
            } catch (NamingException e) {
                // log the exception and continue
                System.out.println("Caught NamingException while modifying " + entryName + " attribute " + attr);
                e.printStackTrace();
            }
        } else {
            System.out.println(
                    "    Current value of " + attrName + " on " + entryName + " is " + curValue + " - not changed");
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
