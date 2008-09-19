/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im.provider;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.wildfire.LocationManager;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.XMPPComponent;
import com.zimbra.cs.account.Provisioning.AccountBy;

/**
 * Manages whole-cloud location of users, components
 */
public class ZimbraLocationManager implements LocationManager {
    
    private CloudComponentIdentifier fromXMPPComponent(XMPPComponent in) throws ServiceException {
        CloudComponentIdentifier toRet = new CloudComponentIdentifier();
        
        toRet.serverId = in.getServerId();
        toRet.serviceDomain = in.getName();
        toRet.serviceName = in.getShortName();
        toRet.category = in.getComponentCategory();
        toRet.type = in.getComponentType();
        toRet.features = in.getComponentFeatures();
        return toRet;
    }
    
    static class CloudComponentIdentifier extends ComponentIdentifier {
        public String serverId;
    }
    
    private List<XMPPComponent> getCloudComponents() throws ServiceException {
        return Provisioning.getInstance().getAllXMPPComponents();
    }
    
    public List<ComponentIdentifier> getAllServerComponents() throws ServiceException {
        List<ComponentIdentifier> toRet = new ArrayList<ComponentIdentifier>();
        
        String serverId = Provisioning.getInstance().getLocalServer().getId();
        for (XMPPComponent component : getCloudComponents()) {
            try {
                toRet.add(fromXMPPComponent(component));
            } catch (ServiceException ex) {
                ZimbraLog.im.warn("Exception in XMPP Component configuration data for: "+component, ex);
            }
        }
        return toRet;
    }
    
    public List<ComponentIdentifier> getRemoteServerComponents() throws ServiceException {
        List<ComponentIdentifier> toRet = new ArrayList<ComponentIdentifier>();
        
        String serverId = Provisioning.getInstance().getLocalServer().getId();
        for (XMPPComponent component : getCloudComponents()) {
            if (!serverId.equals(component.getServerId())) {
                try {
                    toRet.add(fromXMPPComponent(component));
                } catch (ServiceException ex) {
                    ZimbraLog.im.warn("Exception in XMPP Component configuration data for: "+component, ex);
                }
            }
        }
        return toRet;
    }
    
    public List<ComponentIdentifier> getThisServerComponents(String componentType) throws ServiceException {
        List<ComponentIdentifier> toRet = new ArrayList<ComponentIdentifier>();
        
        String serverId = Provisioning.getInstance().getLocalServer().getId();
        for (XMPPComponent component : getCloudComponents()) {
            if (serverId.equals(component.getServerId())) {
                if (componentType==null || componentType.equals(component.getComponentType())) {
                    try {
                        toRet.add(fromXMPPComponent(component));
                    } catch (ServiceException ex) {
                        ZimbraLog.im.warn("Exception in XMPP Component configuration data for: "+component, ex);
                    }
                }
            }
        }
        return toRet;
    }

    public boolean isCloudComponent(String domain) {
        try {
            if (domain == null || domain.length() < 3) {
                // minimum length of a valid domain is e.g. "a.b"
                return false;
            }
            for (XMPPComponent component : getCloudComponents()) {
                if (domain.equals(component.getName()))
                    return true;
            }
        } catch (ServiceException ex) {
            ex.printStackTrace();
            ZimbraLog.im.warn("Ignoring exception in ZimbraLocationManager.isCloudComponent("+domain+")", ex);
        }
        return false;
    }
    
    public String getServerForComponent(String domain) {
        if (domain == null || domain.length() < 3) {
            // minimum length of a valid domain is e.g. "a.b"
            return null;
        }
        try {
            for (XMPPComponent component : getCloudComponents()) {
                if (domain.equals(component.getName())) {
                    return component.getServer().getName();
                }
            }
        } catch (ServiceException ex) {
            ZimbraLog.im.warn("Ignoring exception in ZimbraLocationManager.getServerForComponent("+domain+")", ex);
        }
        return null;
    }

    private static final ZimbraLocationManager sInstance = new ZimbraLocationManager();
    
    public static ZimbraLocationManager getInstance() { return sInstance; }
    
    private ZimbraLocationManager() { }
    
    public boolean isLocal(String username) throws UserNotFoundException {
        return isLocal(new JID(username));
    }
    
    public boolean isLocal(JID jid) throws UserNotFoundException {
        if (jid.getNode() == null || jid.getDomain() == null)
            return false;
        
        try {
            String node = jid.getNode();
            node = JID.unescapeNode(node);
            Account acct = Provisioning.getInstance().get(AccountBy.name, node+"@"+jid.getDomain());
            if (acct == null)
                throw new UserNotFoundException("Unable to find user: "+jid.toString());
            return (Provisioning.onLocalServer(acct));
        } catch (ServiceException ex) {
            return false;
        }

    }

    public boolean isRemote(JID jid) throws UserNotFoundException {
        return !isLocal(jid);
    }
}
