package com.zimbra.cs.account.ldap.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;

public class ZimbraGalLdapAttrMap_ZimbraContactHiddenAttributes_externalCalendarResource extends LdapUpgrade {

    ZimbraGalLdapAttrMap_ZimbraContactHiddenAttributes_externalCalendarResource() throws ServiceException {
    }

    @Override
    void doUpgrade() throws ServiceException {
        upgrade_zimbraGalLdapAttrMap();
        upgrade_zimbraContactHiddenAttributes();
    }
    
    void upgrade_zimbraGalLdapAttrMap() throws ServiceException {
        
        String attrName = Provisioning.A_zimbraGalLdapAttrMap;
        
        Config config = mProv.getConfig();
        
        System.out.println();
        System.out.println("Checking " + config.getLabel() + " for " + attrName);
        
        String oldCalResType = "zimbraCalResType=zimbraCalResType";
        String newCalResType = "zimbraCalResType,msExchResourceSearchProperties=zimbraCalResType";
        
        String oldCalResLocationDisplayName = "zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName";
        String newCalResLocationDisplayName = "zimbraCalResLocationDisplayName,displayName=zimbraCalResLocationDisplayName";
        
        String zimbraCalResBuilding = "zimbraCalResBuilding=zimbraCalResBuilding";
        String zimbraCalResCapacity = "zimbraCalResCapacity,msExchResourceCapacity=zimbraCalResCapacity";
        String zimbraCalResFloor = "zimbraCalResFloor=zimbraCalResFloor";
        String zimbraCalResSite = "zimbraCalResSite=zimbraCalResSite";
        String zimbraCalResContactEmail = "zimbraCalResContactEmail=zimbraCalResContactEmail";
        String zimbraAccountCalendarUserType = "msExchResourceSearchProperties=zimbraAccountCalendarUserType";
        
        String[] curValues = config.getMultiAttr(attrName);
        
        Map<String, Object> attrs = new HashMap<String, Object>(); 
        for (String curValue : curValues) {
            replaceIfNeeded(attrs, attrName, curValue, oldCalResType, newCalResType);
            replaceIfNeeded(attrs, attrName, curValue, oldCalResLocationDisplayName, newCalResLocationDisplayName);
        }

        addValue(attrs, attrName, zimbraCalResBuilding);
        addValue(attrs, attrName, zimbraCalResCapacity);
        addValue(attrs, attrName, zimbraCalResFloor);
        addValue(attrs, attrName, zimbraCalResSite);
        addValue(attrs, attrName, zimbraCalResContactEmail);
        addValue(attrs, attrName, zimbraAccountCalendarUserType);
        
        if (attrs.size() > 0) {
            System.out.println("Modifying " + attrName + " on " + config.getLabel());
            mProv.modifyAttrs(config, attrs);
        }
    }

    private void upgrade_zimbraContactHiddenAttributes(Entry entry, String entryName) throws ServiceException {
        
        String attrName = Provisioning.A_zimbraContactHiddenAttributes;
        
        System.out.println();
        System.out.println("Checking " + entryName + " for " + attrName);
        
        String curValue = entry.getAttr(attrName);
        
        // remove zimbraCalResType,zimbraCalResLocationDisplayName,zimbraCalResCapacity,zimbraCalResContactEmail 
        String newHiddenAttrs = "";
        if (curValue != null) {
            String[] curHiddenAttrs = curValue.split(",");
            boolean first = true;
            for (String hiddenAttr : curHiddenAttrs) {
                if (!Provisioning.A_zimbraCalResType.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResLocationDisplayName.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResCapacity.equalsIgnoreCase(hiddenAttr) &&
                    !Provisioning.A_zimbraCalResContactEmail.equalsIgnoreCase(hiddenAttr)) {
                    if (!first)
                        newHiddenAttrs += ",";
                    else
                        first = false;
                    newHiddenAttrs += hiddenAttr;
                }
            }
        }
        if (newHiddenAttrs.length() == 0)
            newHiddenAttrs = "dn,zimbraAccountCalendarUserType,vcardUID,vcardURL,vcardXProps";
        
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraContactHiddenAttributes, newHiddenAttrs);
        
        if (attrs.size() > 0) {
            System.out.println("Modifying " + attrName + " on " + entryName + " from " + curValue + " to " + newHiddenAttrs);
            mProv.modifyAttrs(entry, attrs);
        }
    }
    
    private void upgrade_zimbraContactHiddenAttributes() throws ServiceException {
        Config config = mProv.getConfig();
        upgrade_zimbraContactHiddenAttributes(config, config.getLabel());
        
        List<Server> servers = mProv.getAllServers();
        
        for (Server server : servers)
            upgrade_zimbraContactHiddenAttributes(server, "server " + server.getLabel());
    }
    
    private void replaceIfNeeded(Map<String, Object> attrs, String attrName, String curValue, String oldValue, String newValue) {
        if (curValue.equalsIgnoreCase(oldValue)) {
            System.out.println("    removing value: " + oldValue);
            System.out.println("    adding value: " + newValue);
            
            StringUtil.addToMultiMap(attrs, "-" + attrName, oldValue);
            StringUtil.addToMultiMap(attrs, "+" + attrName, newValue);
        }
    }
    
    private void addValue(Map<String, Object> attrs, String attrName, String value) {
        System.out.println("    adding value: " + value);
        StringUtil.addToMultiMap(attrs, "+" + attrName, value);
    }
    
}
