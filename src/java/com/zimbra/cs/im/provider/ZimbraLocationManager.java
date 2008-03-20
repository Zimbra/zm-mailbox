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

import org.jivesoftware.wildfire.LocationManager;
import org.jivesoftware.wildfire.user.UserNotFoundException;
import org.xmpp.packet.JID;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;

public class ZimbraLocationManager implements LocationManager {
    
    private static final ZimbraLocationManager sInstance = new ZimbraLocationManager();
    
    public static ZimbraLocationManager getInstance() { return sInstance; }
    
    private ZimbraLocationManager() {}
    
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
