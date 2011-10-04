package com.zimbra.cs.service.account;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;

public abstract class DistributionListDocumentHandler extends AccountDocumentHandler {

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        try {
            /* TODO
            Group group = getGroup(request, Provisioning.getInstance());
            
            if (!Provisioning.onLocalServer(group)) {
                return proxyRequest(request, context, getServer(group));
            }
            */
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // if something went wrong proxying the request, just execute it locally
            if (ServiceException.PROXY_ERROR.equals(e.getCode()))
                return null;
            // but if it's a real error, it's a real error
            throw e;
        }
    }
    
    private Server getServer(Group group) throws ServiceException {
        String hostname = group.getAttr(Provisioning.A_zimbraMailHost);
        if (hostname == null) {
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(""), "");
        }
        Server server = Provisioning.getInstance().get(Key.ServerBy.name, hostname);
        if (server == null) {
            throw ServiceException.PROXY_ERROR(AccountServiceException.NO_SUCH_SERVER(hostname), "");
        }
        return server;
    }

    protected boolean isOwner(Account acct, Group group) throws ServiceException {
        return AccessManager.getInstance().canAccessGroup(acct, group);
    }
    
    static boolean isMember(Provisioning prov, Account acct, Group group) 
    throws ServiceException {
        boolean isMember = false;
        List<Group> groups = prov.getGroups(acct, false, null); // all groups the account is a member of
        for (Group inGroup : groups) {
            if (inGroup.getId().equalsIgnoreCase(group.getId())) {
                isMember = true;
                break;
            }
        }
        return isMember;
    }

    protected Group getGroup(Element request, Account acct, Provisioning prov) 
    throws ServiceException {
        Group group = getGroup(request, prov);
        
        if (!isOwner(acct, group)) {
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient rights to access this distribution list");
        }
        
        return group;
    }

    protected Group getGroup(Element request, Provisioning prov) 
    throws ServiceException {
        Element d = request.getElement(AccountConstants.E_DL);
        String key = d.getAttribute(AccountConstants.A_BY);
        String value = d.getText();
        
        // temporary fix for the caching bug
        // Group group = prov.getGroupBasic(Key.DistributionListBy.fromString(key), value);
        Group group = prov.getGroup(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        return group;
    }
    
    protected abstract static class NotificationSender {
        protected Provisioning prov;
        protected Group group;
        protected Account requestingAcct;  // user who requested to subscribe/un-subscribe
        protected DistributionListSubscribeOp op;
        
        protected NotificationSender(Provisioning prov, Group group, Account requestingAcct, 
                DistributionListSubscribeOp op) {
            this.prov = prov;
            this.group = group;
            this.requestingAcct = requestingAcct;
            this.op = op;
        }

        protected abstract MimeMultipart buildMailContent(Locale locale)
        throws MessagingException;
        
        protected Locale getLocale(Account acct) throws ServiceException {
            return acct.getLocale();
        }
        
        protected void buildContentAndSend(SMTPMessage out, Locale locale, String logTTxt)
        throws MessagingException {
        
            MimeMultipart mmp = buildMailContent(locale);
            out.setContent(mmp);
            Transport.send(out);
        
            // log
            Address[] rcpts = out.getRecipients(javax.mail.Message.RecipientType.TO);
            StringBuilder rcptAddr = new StringBuilder();
            for (Address a : rcpts) {
                rcptAddr.append(a.toString() + ", ");
            }
            ZimbraLog.account.info(logTTxt + ": rcpt='" + rcptAddr + 
                    "' Message-ID=" + out.getMessageID());
        }
        
        protected static abstract class MimePartDataSource implements DataSource {

            private String mText;
            private byte[] mBuf = null;

            public MimePartDataSource(String text) {
                mText = text;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                synchronized(this) {
                    if (mBuf == null) {
                        ByteArrayOutputStream buf = new ByteArrayOutputStream();
                        OutputStreamWriter wout =
                            new OutputStreamWriter(buf, MimeConstants.P_CHARSET_UTF8);
                        String text = mText;
                        wout.write(text);
                        wout.flush();
                        mBuf = buf.toByteArray();
                    }
                }
                return new ByteArrayInputStream(mBuf);
            }

            @Override
            public OutputStream getOutputStream() {
                throw new UnsupportedOperationException();
            }
        }
        
        protected static class HtmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_TEXT_HTML + "; " + 
                MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "HtmlDataSource";

            HtmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }
        
        protected static class XmlPartDataSource extends MimePartDataSource {
            private static final String CONTENT_TYPE =
                MimeConstants.CT_XML_ZIMBRA_DL_SUBSCRIPTION + "; " + 
                MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
            private static final String NAME = "XmlDataSource";

            XmlPartDataSource(String text) {
                super(text);
            }

            @Override
            public String getContentType() {
                return CONTENT_TYPE;
            }

            @Override
            public String getName() {
                return NAME;
            }
        }
    }

}
