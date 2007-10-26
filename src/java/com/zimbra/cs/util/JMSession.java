/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.util;


import java.util.Properties;

import javax.mail.Session;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;

/**
 * @author schemers
 */
public class JMSession {
    private static Session mSession;
    
    private static Log mLog = LogFactory.getLog(JMSession.class);
    
    private static SmtpConfig sSmtpConfig;
    
    static {
        try {
            sSmtpConfig = new SmtpConfig();
            String timeout = sSmtpConfig.getTimeoutMS()+"";
            Properties props = new Properties();
            props.setProperty("mail.mime.address.strict", "false");
            props.setProperty("mail.smtp.host", sSmtpConfig.getHostname());
            props.setProperty("mail.smtp.port", sSmtpConfig.getPort()+"");
            props.setProperty("mail.smtp.connectiontimeout", timeout);
            props.setProperty("mail.smtp.timeout", timeout);
            props.setProperty("mail.smtp.localhost", LC.zimbra_server_hostname.value());
            
            props.setProperty("mail.smtp.sendpartial", Boolean.toString(sSmtpConfig.getSendPartial()));
            mSession = Session.getInstance(props);
            if (LC.javamail_smtp_debug.booleanValue())
            	mSession.setDebug(true);
            
            // Assume that most malformed base64 errors occur due to incorrect delimiters,
            // as opposed to errors in the data itself.  See bug 11213 for more details.
            System.setProperty("mail.mime.base64.ignoreerrors", "true");
            
            mLog.info("SMTP Server: "+sSmtpConfig.getHostname());
        } catch (ServiceException e) {
            mLog.fatal("unable to initialize Java Mail session", e);
            // TODO: System.exit? For now mSession will be null and something else will croak
        }

    }
    
    public static SmtpConfig getSmtpConfig() {
    	return sSmtpConfig;
    }
    
    public static Session getSession() {
        return mSession;
    }
}
