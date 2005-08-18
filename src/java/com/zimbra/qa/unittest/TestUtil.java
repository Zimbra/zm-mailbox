package com.zimbra.qa.unittest;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.client.LmcSession;
import com.zimbra.cs.client.soap.LmcAuthRequest;
import com.zimbra.cs.client.soap.LmcAuthResponse;
import com.zimbra.cs.client.soap.LmcSoapClientException;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.soap.SoapFaultException;

/**
 * @author bburtin
 */
public class TestUtil {
    public static Account getAccount(String userName)
    throws ServiceException {
        String address = getAddress(userName);
        Account account = Provisioning.getInstance().getAccountByName(address);
        if (account == null) {
            throw new IllegalArgumentException("Could not find account for '" + address + "'");
        }
        return account;
    }

    public static String getDomain()
    throws ServiceException {
        Config config = Provisioning.getInstance().getConfig();
        String domain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
        assert(domain != null && domain.length() > 0);
        return domain;
    }
    
    public static String getAddress(String userName)
    throws ServiceException {
        return userName + "@" + getDomain();
    }
    
    public static String getSoapUrl() {
        return "http://localhost:7070/service/soap/";
    }
    
    public static String getAdminSoapUrl() {
        return "https://localhost:7071/service/admin/soap/";
    }
    
    public static LmcSession getSoapSession(String userName)
    throws ServiceException, LmcSoapClientException, IOException, SoapFaultException
    {
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress(userName));
        auth.setPassword("test123");
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getSoapUrl());
        return authResp.getSession();
    }

    public static LmcSession getAdminSoapSession()
    throws Exception
    {
        // Authenticate
        LmcAuthRequest auth = new LmcAuthRequest();
        auth.setUsername(getAddress("admin"));
        auth.setPassword("test123");
        LmcAuthResponse authResp = (LmcAuthResponse) auth.invoke(getAdminSoapUrl());
        return authResp.getSession();
    }
    
    private static String[] MESSAGE_TEMPLATE_LINES = {
        "From: Jeff Spiccoli <jspiccoli@${DOMAIN}>",
        "To: Test User 1 <user1@${DOMAIN}>",
        "Subject: ${SUBJECT}",
        "Date: Mon, 28 Mar 2005 10:21:10 -0700",
        "X-Zimbra-Received: Mon, 28 Mar 2005 10:21:1${MESSAGE_NUM} -0700",
        "Content-Type: text/plain",
        "",
        "Dude,",
        "",
        "All I need are some tasty waves, a cool buzz, and I'm fine.",
        "",
        "Jeff",
        "",
        "(${SUBJECT} ${MESSAGE_NUM})"
    };
    
    private static String MESSAGE_TEMPLATE = StringUtil.join("\n", MESSAGE_TEMPLATE_LINES); 

    public static Message insertMessage(Mailbox mbox, int messageNum, String subject)
    throws Exception {
        Map vars = new HashMap();
        vars.put("MESSAGE_NUM", new Integer(messageNum));
        vars.put("SUBJECT", subject);
        vars.put("DOMAIN", getDomain());
        String message = StringUtil.fillTemplate(new StringReader(MESSAGE_TEMPLATE), vars);
        ParsedMessage pm = new ParsedMessage(message.getBytes(), System.currentTimeMillis(), false);
        pm.analyze();
        return mbox.addMessage(null, pm, Mailbox.ID_FOLDER_INBOX, Flag.FLAG_UNREAD, null);
    }
    
}
