/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Config;

public class MailHost implements AttributeCallback {

    /**
     * check to make sure zimbraMailHost points to a valid server zimbraServiceHostname
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraMailHost+" is a single-valued attribute", null);
        
        String mailHost = (String) value;

        List servers = Provisioning.getInstance().getAllServers();
        for (Iterator it=servers.iterator(); it.hasNext(); ) {
            Server server = (Server) it.next();
            String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
            if (mailHost.equalsIgnoreCase(serviceName)) {
            	// Set zimbraMailTransport attr whenever zimbraMailHost is modified.
            	int lmtpPort = server.getIntAttr(Provisioning.A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
            	String transport = "lmtp:" + mailHost + ":" + lmtpPort;
            	attrsToModify.put(Provisioning.A_zimbraMailTransport, transport);
                return;
            }
        }
        
        throw ServiceException.INVALID_REQUEST("specified "+Provisioning.A_zimbraMailHost+" does not correspond to a valid server service hostname: "+mailHost, null);
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
