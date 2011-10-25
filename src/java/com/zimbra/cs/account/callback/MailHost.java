/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.util.Config;

public class MailHost extends AttributeCallback {

    /**
     * check to make sure zimbraMailHost points to a valid server zimbraServiceHostname
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        if (StringUtil.isNullOrEmpty((String)value) || 
            attrsToModify.get("-" + Provisioning.A_zimbraMailHost) != null)
            return; // unsetting
        
        String mailHost = (String)value;
        String mailTransport = (String)attrsToModify.get(Provisioning.A_zimbraMailTransport);
        
        /*
         * never allow setting both zimbraMailHost and zimbraMailTransport in the same request
         */
        if (!StringUtil.isNullOrEmpty(mailHost) && !StringUtil.isNullOrEmpty(mailTransport))
            throw ServiceException.INVALID_REQUEST("setting both " + 
                    Provisioning.A_zimbraMailHost + " and " +  Provisioning.A_zimbraMailTransport + 
                    " in the same request is not allowed", null);
        
        Provisioning prov = Provisioning.getInstance();
        
        Server server = prov.get(Key.ServerBy.serviceHostname, mailHost);
        if (server == null)
            throw ServiceException.INVALID_REQUEST("specified " + 
                    Provisioning.A_zimbraMailHost + 
                    " does not correspond to a valid server service hostname: "+mailHost, null);
        else {
            if (!server.hasMailboxService()) {
                throw ServiceException.INVALID_REQUEST("specified " + Provisioning.A_zimbraMailHost + 
                        " does not correspond to a valid server with the mailbox service enabled: " 
                        + mailHost, null);    
            }
            
            /*
             * bug 18419
             * 
             * If zimbraMailHost is modified, see if applying lmtp rule to old 
             * zimbraMailHost value would result in old zimbraMailTransport - 
             * if it would, then replace both zimbraMailHost and set new zimbraMailTransport.  
             * Otherwise error.
             */
            if (entry != null && !isCreate) {
        	
                String oldMailHost = entry.getAttr(Provisioning.A_zimbraMailHost);
                if (oldMailHost != null) {
                    Server oldServer = prov.get(Key.ServerBy.serviceHostname, oldMailHost);
                    if (oldServer != null) {
                	    String curMailTransport = entry.getAttr(Provisioning.A_zimbraMailTransport);
                	    if (!oldServer.mailTransportMatches(curMailTransport)) {
                            throw ServiceException.INVALID_REQUEST(
                                    "current value of zimbraMailHost does not match zimbraMailTransport" +
                                    ", computed mail transport from current zimbraMailHost=" + 
                                    mailTransport(oldServer) + ", current zimbraMailTransport=" + 
                                    curMailTransport, 
                        	        null);
                	    }
                    }
                }
            } else {
                // we are creating the account
            }
            
            // also update mail transport to match the new mail host
            String newMailTransport = mailTransport(server);
            attrsToModify.put(Provisioning.A_zimbraMailTransport, newMailTransport);
        }
    }
    
    private static String mailTransport(Server server) {
        String serviceName = server.getAttr(Provisioning.A_zimbraServiceHostname, null);
        int lmtpPort = server.getIntAttr(Provisioning.A_zimbraLmtpBindPort, Config.D_LMTP_BIND_PORT);
        String transport = "lmtp:" + serviceName + ":" + lmtpPort;
        return transport;
    }


    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
    

}
