/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Sep 23, 2004
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;

import java.util.Map;

/**
 * @author schemers
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Server extends ZAttrServer {
    
    public Server(String name, String id, Map<String,Object> attrs, Map<String,Object> defaults, Provisioning prov) {
        super(name, id, attrs, defaults, prov);
    }
    
    @Override
    public EntryType getEntryType() {
        return EntryType.SERVER;
    }

    public void deleteServer(String zimbraId) throws ServiceException {
        getProvisioning().deleteServer(getId());
    }

    public void modify(Map<String, Object> attrs) throws ServiceException {
        getProvisioning().modifyAttrs(this, attrs);
    }
    
    /*
     * compare only proto and host, ignore port, because if port on the server was changed we 
     * still want the change to go through.
     */
    public boolean mailTransportMatches(String mailTransport) {
        // if there is no mailTransport, it sure "matches"
        if (mailTransport == null)
            return true;
        
        String serviceName = getAttr(Provisioning.A_zimbraServiceHostname, null);
        
        String[] parts = mailTransport.split(":");
        if (serviceName != null && parts.length == 3) {
            if (parts[0].equalsIgnoreCase("lmtp") && parts[1].equals(serviceName))
                return true;
        }
        
        return false;
    }
    
    public boolean hasMailboxService() {
        return getMultiAttrSet(Provisioning.A_zimbraServiceEnabled).contains(Provisioning.SERVICE_MAILBOX);
    }
    
    public boolean isLocalServer() throws ServiceException {
        Server localServer = getProvisioning().getLocalServer();
        return getId() != null && getId().equals(localServer.getId());
    }

}
