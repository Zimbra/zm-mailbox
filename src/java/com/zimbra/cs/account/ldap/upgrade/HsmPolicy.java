package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Constants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;

public class HsmPolicy extends LdapUpgrade {

    HsmPolicy(String bug, boolean verbose) throws ServiceException {
        super(bug, verbose);
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
        
        String oldAttr = Provisioning.A_zimbraHsmAge;
        String newAttr = Provisioning.A_zimbraHsmPolicy;
        
        System.out.println();
        System.out.println("Checking " + entryName);
        
        String oldValue = entry.getAttr(oldAttr, false);
        String newValue = entry.getAttr(newAttr, false);
        if (oldValue != null) {
            if (newValue == null) {
                newValue = String.format("message,document:before:-%dminutes", 
                        entry.getTimeInterval(oldAttr, 0) / Constants.MILLIS_PER_MINUTE);
                
                System.out.println("    Setting " + newAttr + " on " + entryName + 
                        " from " + oldAttr + " value: [" + oldValue + "]" + 
                        " to [" + newValue + "]");
                
                Map<String, Object> attr = new HashMap<String, Object>();
                attr.put(newAttr, newValue);
                mProv.modifyAttrs(entry, attr);
            } else
                System.out.println("    " + newAttr + " already has a value: [" + newValue + "], skipping"); 
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
