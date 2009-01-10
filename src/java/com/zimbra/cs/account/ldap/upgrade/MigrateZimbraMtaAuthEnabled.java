package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCardinality;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeInfo;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.ZAttrProvisioning;
import com.zimbra.cs.account.ldap.ZimbraLdapContext;
import com.zimbra.cs.util.BuildInfo;

public class MigrateZimbraMtaAuthEnabled extends LdapUpgrade {

    MigrateZimbraMtaAuthEnabled(String bug, boolean verbose) throws ServiceException {
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
    
    private void doEntry(ZimbraLdapContext zlc, Entry entry, String entryName, AttributeClass klass) throws ServiceException {
        
        System.out.println();
        System.out.println("------------------------------");
        System.out.println("Checking " + entryName + ": ");
        
        StringBuilder msg = new StringBuilder();
        try {
            Map<String, Object> attrValues = new HashMap<String, Object>();
            
            String zimbraMtaAuthEnabled      = entry.getAttr(Provisioning.A_zimbraMtaAuthEnabled, false);
            String zimbraMtaTlsAuthOnly      = entry.getAttr(Provisioning.A_zimbraMtaTlsAuthOnly, false);
            
            String zimbraMtaTlsSecurityLevel = entry.getAttr(Provisioning.A_zimbraMtaTlsSecurityLevel, false);
            String zimbraMtaSaslAuthEnable   = entry.getAttr(Provisioning.A_zimbraMtaSaslAuthEnable, false);
            
            // set it only if it does not already have a value
            if (zimbraMtaTlsSecurityLevel == null) {
                ZAttrProvisioning.MtaTlsSecurityLevel value;
                if (Provisioning.TRUE.equals(zimbraMtaAuthEnabled) && 
                    Provisioning.TRUE.equals(zimbraMtaTlsAuthOnly))
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.may;
                else
                    value = ZAttrProvisioning.MtaTlsSecurityLevel.none;
                
                attrValues.put(Provisioning.A_zimbraMtaTlsSecurityLevel, value.toString());
            }
            
            // set it only if it does not already have a value
            if (zimbraMtaSaslAuthEnable == null) {
                if (zimbraMtaAuthEnabled != null)
                    attrValues.put(Provisioning.A_zimbraMtaSaslAuthEnable, zimbraMtaAuthEnabled);
            }
            
            if (!attrValues.isEmpty()) {
                boolean first = true;
                for (Map.Entry<String, Object> attr : attrValues.entrySet()) {
                    if (!first)
                        msg.append(", ");
                    msg.append(attr.getKey() + "=>" + (String)attr.getValue());
                    first = false;
                }
                
                System.out.println("Updating " + entryName + ": " + msg.toString());
                LdapUpgrade.modifyAttrs(entry, zlc, attrValues);
            }
        } catch (ServiceException e) {
            // log the exception and continue
            System.out.println("Caught ServiceException while modifying " + entryName + ": " + msg.toString());
            e.printStackTrace();
        } catch (NamingException e) {
            // log the exception and continue
            System.out.println("Caught NamingException while modifying " + entryName + ": " + msg.toString());
            e.printStackTrace();
        }
    }

    private void doGlobalConfig(ZimbraLdapContext zlc) throws ServiceException {
        Config config = mProv.getConfig();
        doEntry(zlc, config, "global config", AttributeClass.globalConfig);
    }
    
    private void doAllServers(ZimbraLdapContext zlc) throws ServiceException {
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            doEntry(zlc, server, "server " + server.getName(), AttributeClass.server);
    }
}
