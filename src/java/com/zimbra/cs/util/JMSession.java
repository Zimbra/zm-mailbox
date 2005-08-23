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
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.util;


import java.util.Properties;

import javax.mail.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.service.ServiceException;

/**
 * @author schemers
 */
public class JMSession {
    private static javax.mail.Session mSession;
    
    private static Log mLog = LogFactory.getLog(JMSession.class);
    
    static {
        try {
            SmtpConfig config = new SmtpConfig();
            String timeout = config.getTimeoutMS()+"";
            Properties props = new Properties();
            props.setProperty("mail.mime.address.strict", "false");
            props.setProperty("mail.smtp.host", config.getHostname());
            props.setProperty("mail.smtp.port", config.getPort()+"");
            props.setProperty("mail.smtp.connectiontimeout", timeout);
            props.setProperty("mail.smtp.timeout", timeout);
            mSession = Session.getInstance(props);
            mLog.info("SMTP Server: "+config.getHostname());
        } catch (ServiceException e) {
            mLog.fatal("unable to initialize Java Mail session", e);
            // TODO: System.exit? For now mSession will be null and something else will croak
        }

    }
    
    public static Session getSession() {
        return mSession;
    }
}
