package com.zimbra.cs.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.ServerBy;

/**
 * Systemwide configuration entry for XMPP component (e.g. conference.mydomain.com) in the cloud
 */
public class XMPPComponent extends NamedEntry implements Comparable {
    private static final String SIMPLE_CLASS_NAME =
        StringUtil.getSimpleClassName(XMPPComponent.class.getName());
    
    public XMPPComponent(String name, String id, Map<String,Object> attrs, Provisioning prov) {
        super(name, id, attrs, null, prov);
    }
    
    public String getComponentCategory() {
        return getAttr(Provisioning.A_zimbraXMPPComponentCategory);
    }
    
    public String getComponentType() {
        return getAttr(Provisioning.A_zimbraXMPPComponentType);
    }
    
    public String getLongName() {
        return getAttr(Provisioning.A_zimbraXMPPComponentName);
    }
    
    public String getClassName() {
        return getAttr(Provisioning.A_zimbraXMPPComponentClassName);
    }
    
    public String getShortName() throws ServiceException {
        String name = getName();
        String domainName = getDomain().getName();
        if (!name.endsWith(domainName)) {
            throw ServiceException.FAILURE("Invalid configuration data: XMPPComponent name, \""+
                                           name+"\" must be a subdomain of domain \""+domainName+
                                           "\"", null);
        }
        String toRet = name.substring(0, (name.length() - domainName.length())-1);
        return toRet;
    }
    
    public List<String> getComponentFeatures() {
        List<String> toRet = null;
        String[] features = this.getMultiAttr(Provisioning.A_zimbraXMPPComponentFeatures);
        if (features != null && features.length > 0) {
            toRet = new ArrayList<String>(features.length);
            for (String s : features)
                toRet.add(s);
        } else {
            toRet = new ArrayList<String>();
        }
        return toRet;
    }
    
    public String getDomainId() {
        return getAttr(Provisioning.A_zimbraDomainId);
    }
    public Domain getDomain() throws ServiceException {
        return Provisioning.getInstance().get(DomainBy.id, getDomainId());
    }
    
    public String getServerId() {
        return getAttr(Provisioning.A_zimbraServerId);
    }
    public Server getServer() throws ServiceException {
        return Provisioning.getInstance().get(ServerBy.id, getServerId());
    }
     
    
    public String toString() {
        List<String> parts = new ArrayList<String>();
        parts.add("name="+getName());
        parts.add("category="+getComponentCategory());
        parts.add("type="+getComponentType());
        parts.add("domainId="+getDomainId());
        try {
            Domain domain = getDomain();
            parts.add("domainName="+domain.getName());
        } catch (ServiceException e) {}
        parts.add("serverId="+getServerId());
        try {
            Server server = getServer();
            parts.add("serverName="+server.getName());
        } catch (ServiceException e) {}
        
        StringBuilder featureStr = new StringBuilder();
        for (String s : getComponentFeatures()) {
            if (featureStr.length() > 0)
                featureStr.append(',');
            featureStr.append(s);
        }
        return String.format("%s: { %s }",
                             SIMPLE_CLASS_NAME, StringUtil.join(", ", parts));
    }
}
