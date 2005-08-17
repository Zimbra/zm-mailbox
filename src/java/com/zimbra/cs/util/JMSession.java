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
