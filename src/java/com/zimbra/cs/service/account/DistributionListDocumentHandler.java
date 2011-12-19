/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CacheEntryBy;
import com.zimbra.common.account.Key.ServerBy;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.soap.account.type.DistributionListSubscribeOp;

public abstract class DistributionListDocumentHandler extends AccountDocumentHandler {

    @Override
    protected Element proxyIfNecessary(Element request, Map<String, Object> context) 
    throws ServiceException {
        try {
            Group group = getGroupBasic(request, Provisioning.getInstance());
            
            if (!Provisioning.onLocalServer(group)) {
                Server server = group.getServer();
                if (server == null) {
                    throw ServiceException.PROXY_ERROR(
                            AccountServiceException.NO_SUCH_SERVER(
                            group.getAttr(Provisioning.A_zimbraMailHost)), "");
                }
                return proxyRequest(request, context, server);
            }
            
            return super.proxyIfNecessary(request, context);
        } catch (ServiceException e) {
            // must be able to proxy, we don't want to fallback to local
            throw e;
        }
    }
    
    protected Group getGroupBasic(Element request, Provisioning prov) 
    throws ServiceException {
        Element eDL = request.getElement(AccountConstants.E_DL);
        String key = eDL.getAttribute(AccountConstants.A_BY);
        String value = eDL.getText();
        
        Group group = prov.getGroupBasic(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        return group;
    }
    
    protected static boolean isMember(Provisioning prov, Account acct, Group group) 
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

    /*
     * Centralized callsite for adding/removing group members in DistributionListAction and 
     * SubscribeDistributionList.  The group object passed in is a "basic" group instance, 
     * obtained from Provisioning.getGroupBasic().  Unlink "full" group instances, basic 
     * instances don't contain all attributes, and basic groups are cached in LdapProvisioning.
     * For dynamic groups, there is no difference between basic and full instances of a group.  
     * For static groups, the basic instance does not contain member attribute of the group.   
     * We have to load a full instance, and pass it to Provisiioning.add/removeGroupMembers.
     * 
     * Note: loading full instance of a static group will always make a trip to LDAP,  
     *       these instances are *not* cahed.
     */
    protected static void addGroupMembers(Provisioning prov, Group group, String[] members) 
    throws ServiceException {
        if (!group.isDynamic()) {
            // load full instance of the static group.
            // note: this always cost a LDAP search
            group = prov.getGroup(Key.DistributionListBy.id, group.getId());
        }
        prov.addGroupMembers(group, members);
        
        // flush account from cache for internal members
        flushAccountCache(prov, members);
    }
    
    protected static void removeGroupMembers(Provisioning prov, Group group, String[] members) 
    throws ServiceException {
        if (!group.isDynamic()) {
            // load full instance of the static group.
            // note: this always cost a LDAP search
            group = prov.getGroup(Key.DistributionListBy.id, group.getId());
        }
        prov.removeGroupMembers(group, members);
        
        // flush account from cache for internal members
        flushAccountCache(prov, members);
    }
    
    private static void flushAccountCache(Provisioning prov, String[] members) {
        // List<CacheEntry> localAccts = Lists.newArrayList();
        Map<String /* server name */, List<CacheEntry>> remoteAccts = Maps.newHashMap();
        
        for (String member : members) {
            try {
                Account acct = prov.get(AccountBy.name, member);
                if (acct != null) {
                    if (prov.onLocalServer(acct)) {
                        // localAccts.add(new CacheEntry(CacheEntryBy.id, acct.getId()));
                    } else {
                        Server server = acct.getServer();
                        String serverName = server.getName();
                        List<CacheEntry> acctsOnServer = remoteAccts.get(serverName);
                        if (acctsOnServer == null) {
                            acctsOnServer = Lists.newArrayList();
                            remoteAccts.put(serverName, acctsOnServer);
                        }
                        acctsOnServer.add(new CacheEntry(CacheEntryBy.id, acct.getId()));
                    }
                }
                
                // else, not internal account, skip
                
            } catch (ServiceException e) {
                // log and continue
                ZimbraLog.account.warn("unable to flush account cache", e);
            }
        }
        
        /*
         * No need to flush cache on local server, account membership for static/dynamic 
         * groups are handled in LdapProvisioning
         * 
        // flush accounts from cache on local server
        try {
            prov.flushCache(CacheEntryType.account, localAccts.toArray(new CacheEntry[localAccts.size()]));
        } catch (ServiceException e) {
            // log and continue
            ZimbraLog.account.warn("unable to flush account cache on local server", e);
        }
        */
        
        
        // flush accounts from cache on remote servers
        // if the remote server does not run admin server, to bad - accounts on that 
        // server will have to wait till cache expire to ge tupdate membership
        SoapProvisioning soapProv = new SoapProvisioning();
        String adminUrl = null;
        for (Map.Entry<String, List<CacheEntry>> acctsOnServer : remoteAccts.entrySet()) {
            String serverName = acctsOnServer.getKey();
            List<CacheEntry> accts = acctsOnServer.getValue();
            
            try {
                Server server = prov.get(ServerBy.name, serverName);
                adminUrl = URLUtil.getAdminURL(server, AdminConstants.ADMIN_SERVICE_URI, true);
                soapProv.soapSetURI(adminUrl);
                soapProv.soapZimbraAdminAuthenticate();
                soapProv.flushCache(CacheEntryType.account, accts.toArray(new CacheEntry[accts.size()]));
                
            } catch (ServiceException e) {
                ZimbraLog.account.warn("unable to flush account cache on remote server: " + serverName, e);
            }
        }
    }
    
    protected abstract static class SynchronizedGroupHandler {
        protected Group group;
        
        protected SynchronizedGroupHandler(Group group) {
            this.group = group;
        }
        
        protected abstract void handleRequest() throws ServiceException;
        
        protected void handle() throws ServiceException {
            synchronized (group) {
                handleRequest();
            }
        }
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
