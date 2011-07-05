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
package com.zimbra.cs.account.ldap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.mime.shim.JavaMailMimeBodyPart;
import com.zimbra.common.mime.shim.JavaMailMimeMultipart;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DirectoryEntryVisitor;
import com.zimbra.cs.account.ZAttrProvisioning.AutoProvAuthMech;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapServerConfig;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtilCommon;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.ldap.LdapException.LdapSizeLimitExceededException;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;
import com.zimbra.cs.util.JMSession;

public abstract class AutoProvision {

    protected LdapProv prov;
    protected Domain domain;
    
    protected AutoProvision(LdapProv prov, Domain domain) {
        this.prov = prov;
        this.domain = domain;
    }
    
    abstract Account handle() throws ServiceException;
    
    protected Account createAccount(String acctZimbraName, ZAttributes externalAttrs) 
    throws ServiceException {
        Map<String, Object> zimbraAttrs = mapAttrs(externalAttrs);
        
        ZimbraLog.account.info("auto provision account: " + acctZimbraName);
        
        /*
        // TODO: should we do this?
        String zimbraPassword = RandomPassword.generate();
        zimbraAttrs.put(Provisioning.A_zimbraPasswordMustChange, Provisioning.TRUE);
        */
        String zimbraPassword = null;
        Account acct = prov.createAccount(acctZimbraName, zimbraPassword, zimbraAttrs);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "auto provision Account", "name", acct.getName(), "id", acct.getId()}, zimbraAttrs));  
        
        // send notification email
        sendNotifMessage(acct, zimbraPassword);
        
        return acct;
    }
    
    protected static boolean useLdapAuthSettings(Domain domain) {
        String authMech = domain.getAttr(Provisioning.A_zimbraAuthMech);
        return domain.isAutoProvUseLdapAuthSettings() && 
            (Provisioning.AM_LDAP.equals(authMech)  || Provisioning.AM_AD.equals(authMech));
    }
    
    private static class AutoProvisionAttrMap {
        
        private static AutoProvisionAttrMap getAttrMap(Domain domain) throws ServiceException {
            AutoProvisionAttrMap attrMap = 
                (AutoProvisionAttrMap) domain.getCachedData(EntryCacheDataKey.DOMAIN_AUTO_PROVISION_ATTR_MAP);
            
            if (attrMap == null) {
                attrMap = new AutoProvisionAttrMap(domain);
                domain.setCachedData(EntryCacheDataKey.DOMAIN_AUTO_PROVISION_ATTR_MAP, attrMap);
            }
            
            return attrMap;
        }
        
        private static final String DELIMITER = "=";
        private Map<String, String> attrMap = new HashMap<String, String>();
        
        private AutoProvisionAttrMap(Domain domain) throws ServiceException {
            AttributeManager attrMgr = AttributeManager.getInstance();
            
            // include attrs in schema extension
            Set<String> validAccountAttrs = attrMgr.getAllAttrsInClass(AttributeClass.account);
            
            String[] rules = domain.getAutoProvAttrMap();
            
            for (String rule : rules) {
                String[] parts = rule.split(DELIMITER);
                if (parts.length != 2) {
                    throw AccountServiceException.INVALID_CONFIG("invalid value in " + 
                            Provisioning.A_zimbraAutoProvAttrMap + ": " + rule, null);
                }
                
                String externalAttr = parts[0];
                String zimbraAttr = parts[1];
                
                if (!validAccountAttrs.contains(zimbraAttr)) {
                    throw AccountServiceException.INVALID_CONFIG("invalid value in " + 
                            Provisioning.A_zimbraAutoProvAttrMap + ": " + rule + 
                            ", not a valid zimbra attribute ", null);
                }
                
                attrMap.put(externalAttr, zimbraAttr);
            }
        }
        
        private String getZimbraAttrName(String externalAttrName) {
            return attrMap.get(externalAttrName);
        }
    }
    
    
    /**
     * map external name to zimbra name for the account to be created in Zimbra.
     * 
     * @param externalAttrs
     * @return
     * @throws ServiceException
     */
    protected String mapName(ZAttributes externalAttrs, String loginName) throws ServiceException {
        String localpart = null;
        
        String localpartAttr = domain.getAutoProvAccountNameMap();
        if (localpartAttr != null) {
            localpart = externalAttrs.getAttrString(localpartAttr);
            if (localpart == null) {
                throw ServiceException.FAILURE("AutoProvision: unable to get localpart: " + loginName, null);
            }
        } else {
            if (loginName == null) {
                throw ServiceException.FAILURE("AutoProvision: unable to map acount name, must configure " +
                        Provisioning.A_zimbraAutoProvAccountNameMap, null);
            }
            EmailAddress emailAddr = new EmailAddress(loginName, false);
            localpart = emailAddr.getLocalPart();
        }
        
        return localpart + "@" + domain.getName();
        
    }
    
    protected Map<String, Object> mapAttrs(ZAttributes externalAttrs) throws ServiceException {
        AutoProvisionAttrMap attrMap = AutoProvisionAttrMap.getAttrMap(domain);
        
        Map<String, Object> extAttrs = externalAttrs.getAttrs();
        Map<String, Object> zimbraAttrs = new HashMap<String, Object>();
        
        for (Map.Entry<String, Object> extAttr : extAttrs.entrySet()) {
            String extAttrName = extAttr.getKey();
            Object attrValue = extAttr.getValue();
            
            String zimbraAttrName = attrMap.getZimbraAttrName(extAttrName);
            if (zimbraAttrName != null) {
                if (attrValue instanceof String) {
                    StringUtil.addToMultiMap(zimbraAttrs, zimbraAttrName, (String) attrValue);
                } else if (attrValue instanceof String[]) {
                    for (String value : (String[]) attrValue) {
                        StringUtil.addToMultiMap(zimbraAttrs, zimbraAttrName, value);
                    }
                }
            }
        }
        
        return zimbraAttrs;
    }
    
    protected ZAttributes getExternalAttrsByDn(String dn) throws ServiceException {
        boolean useldapAuthSettings = useLdapAuthSettings(domain);
        
        String url = domain.getAutoProvLdapURL();
        boolean wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
        String adminDN = domain.getAutoProvLdapAdminBindDn();
        String adminPassword = domain.getAutoProvLdapAdminBindPassword();
        
        if (useldapAuthSettings) {
            url = LdapServerConfig.joinURLS(domain.getAuthLdapURL());
            wantStartTLS = domain.isAuthLdapStartTlsEnabled();
            adminDN = domain.getAuthLdapSearchBindDn();
            adminPassword = domain.getAuthLdapSearchBindPassword();
        } else {
            url = domain.getAutoProvLdapURL();
            wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
            adminDN = domain.getAutoProvLdapAdminBindDn();
            adminPassword = domain.getAutoProvLdapAdminBindPassword();
        }
        
        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS, 
                null, adminDN, adminPassword, null, "auto provision account");
        
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION);
            return prov.getHelper().getAttributes(dn, zlc);
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
    
    protected ZAttributes getExternalAttrsByName(AutoProvAuthMech authedByMech, 
            String loginName, String loginPassword) throws ServiceException {
        if ((authedByMech == null || AutoProvAuthMech.LDAP == authedByMech) && useLdapAuthSettings(domain)) {
            return getExternalAttrsViaLdapAuthSettings(loginName, loginPassword);
        } else {
            return getExternalAttrsViaAutoProvSettings(loginName);
        }
    }

    private ZAttributes getExternalAttrsViaAutoProvSettings(String loginName) throws ServiceException {
        String url = domain.getAutoProvLdapURL();
        boolean wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
        String adminDN = domain.getAutoProvLdapAdminBindDn();
        String adminPassword = domain.getAutoProvLdapAdminBindPassword();
        
        // always use the admin bind DN/password, not the user's bind DN/password
        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS, 
                null, adminDN, adminPassword, null, "auto provision account");
        
        ZLdapContext zlc = null;
        
        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION);
            
            String searchFilterTemplate = domain.getAutoProvLdapSearchFilter();
            if (searchFilterTemplate != null) {
                // get attrs by search
                String searchBase = domain.getAutoProvLdapSearchBase();
                if (searchBase == null) {
                    searchBase = LdapConstants.DN_ROOT_DSE;
                }
                String searchFilter = LdapUtilCommon.computeAuthDn(loginName, searchFilterTemplate);
                ZimbraLog.account.debug("AutoProvision: computed search filter" + searchFilter);
                ZSearchResultEntry entry = prov.getHelper().searchForEntry(
                        searchBase, ZLdapFilterFactory.getInstance().fromFilterString(searchFilter), zlc);
                return entry.getAttributes();
            }
            
            String bindDNTemplate = domain.getAutoProvLdapBindDn();
            if (bindDNTemplate != null) {
                // get attrs by external DN template
                String dn = LdapUtilCommon.computeAuthDn(loginName, bindDNTemplate);
                ZimbraLog.account.debug("AutoProvision: computed external DN" + dn);
                return prov.getHelper().getAttributes(dn, zlc);
            }
            
        } finally {
            LdapClient.closeContext(zlc);
        }
        
        throw ServiceException.FAILURE("One of " + Provisioning.A_zimbraAutoProvLdapBindDn + 
                " or " + Provisioning.A_zimbraAutoProvLdapSearchFilter + " must be set", null);
    }

    private ZAttributes getExternalAttrsViaLdapAuthSettings(String loginName, String loginPassword) 
    throws ServiceException {
        String url[] = domain.getAuthLdapURL();
        boolean wantStartTLS = domain.isAuthLdapStartTlsEnabled();
        
        ZLdapContext zlc = null;
        
        try {
            String searchFilter = domain.getAuthLdapSearchFilter();
            if (searchFilter != null) {
                String searchPassword = domain.getAuthLdapSearchBindPassword();
                String searchDn = domain.getAuthLdapSearchBindDn();
                String searchBase = domain.getAuthLdapSearchBase();
                if (searchBase == null) {
                    searchBase = "";
                }
                searchFilter = LdapUtilCommon.computeAuthDn(loginName, searchFilter);
                ZimbraLog.account.debug("AutoProvision: computed search filter" + searchFilter);
                
                ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS, 
                        null, searchDn, searchPassword, null, "auto provision account");
                zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION);
                
                ZSearchResultEntry entry = prov.getHelper().searchForEntry(
                        searchBase, ZLdapFilterFactory.getInstance().fromFilterString(searchFilter), zlc);
                
                return entry.getAttributes();
            }
            
            String bindDNTemplate = domain.getAuthLdapBindDn();
            if (bindDNTemplate != null) {
                if (loginPassword == null) {
                    throw ServiceException.FAILURE("no password, must configure " + 
                            Provisioning.A_zimbraAuthLdapSearchFilter, null);
                }
                
                String dn = LdapUtilCommon.computeAuthDn(loginName, bindDNTemplate);
                ZimbraLog.account.debug("AutoProvision: computed external DN" + dn);
                
                ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS, 
                        null, dn, loginPassword, null, "auto provision account");
                zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION);
                                
                return prov.getHelper().getAttributes(dn, zlc);
            }
        
        } finally {
            LdapClient.closeContext(zlc);
        }
        
        throw ServiceException.FAILURE("One of " + Provisioning.A_zimbraAuthLdapBindDn + 
                " or " + Provisioning.A_zimbraAuthLdapSearchFilter + " must be set", null);
    }

    
    protected void sendNotifMessage(Account acct, String password) throws ServiceException {
        String toAddr = acct.getName();
        
        try {
            SMTPMessage out = new SMTPMessage(JMSession.getSmtpSession());

            String from = domain.getAutoProvNotificationFromAddress();
            if (from == null) {
                from = String.format("Postmaster <postmaster@%s>", domain.getName());
            }
            
            InternetAddress addr = null;
            try {
                addr = new JavaMailInternetAddress(from);
            } catch (AddressException e) {
                // log and try the next one
                ZimbraLog.account.warn("invalid address in " +
                        Provisioning.A_zimbraAutoProvNotificationFromAddress, e);
            }
            
            Address fromAddr = addr;
            Address replyToAddr = addr;

            // From
            out.setFrom(fromAddr);

            // Reply-To
            out.setReplyTo(new Address[]{replyToAddr});

            // To
            
            out.setRecipient(javax.mail.Message.RecipientType.TO, new JavaMailInternetAddress(toAddr));

            // Date
            out.setSentDate(new Date());

            // Subject
            Locale locale = acct.getLocale();
            String subject = L10nUtil.getMessage(MsgKey.accountAutoProvisionedSubject, locale);
            out.setSubject(subject);

            // body
            MimeMultipart mmp = new JavaMailMimeMultipart("alternative");

            // TEXT part (add me first!)
            String text = L10nUtil.getMessage(MsgKey.accountAutoProvisionedBody, locale, acct.getDisplayName());
            MimeBodyPart textPart = new JavaMailMimeBodyPart();
            textPart.setText(text, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            StringBuilder html = new StringBuilder();
            html.append("<h4>\n");
            html.append("<p>" + L10nUtil.getMessage(MsgKey.accountAutoProvisionedBody, locale, acct.getDisplayName()) + "</p>\n");
            html.append("</h4>\n");
            html.append("\n");

            MimeBodyPart htmlPart = new JavaMailMimeBodyPart();
            htmlPart.setDataHandler(new DataHandler(new HtmlPartDataSource(html.toString())));
            mmp.addBodyPart(htmlPart);
            out.setContent(mmp);
            
            // send it
            Transport.send(out);

            // log
            Address[] rcpts = out.getRecipients(javax.mail.Message.RecipientType.TO);
            StringBuilder rcptAddr = new StringBuilder();
            for (Address a : rcpts)
                rcptAddr.append(a.toString());
            ZimbraLog.account.info("auto provision notification sent rcpt='" + rcptAddr + "' Message-ID=" + out.getMessageID());

        } catch (MessagingException e) {
            ZimbraLog.account.warn("send auto provision notification failed rcpt='" + toAddr +"'", e);
        }
    }

    private static abstract class MimePartDataSource implements DataSource {

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
            ByteArrayInputStream in = new ByteArrayInputStream(mBuf);
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }

    private static class HtmlPartDataSource extends MimePartDataSource {
        private static final String CONTENT_TYPE =
            MimeConstants.CT_TEXT_HTML + "; " + MimeConstants.P_CHARSET + "=" + MimeConstants.P_CHARSET_UTF8;
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
    
    static void searchAutoProvDirectory(LdapProv prov, Domain domain, String filter, String name, 
            String[] returnAttrs, int maxResults, final DirectoryEntryVisitor visitor)
    throws ServiceException {
        // use either filter or name, make sure only one is provided
        if ((filter == null) == (name==null)) {
            throw ServiceException.INVALID_REQUEST("exact one of filter or name must be set", null);
        }
        
        boolean useLdapAuthSettings = useLdapAuthSettings(domain);
        
        String url;
        boolean wantStartTLS;
        String adminDN;
        String adminPassword;
        String searchBase;
        String searchFilterTemplate;
        
        if (useLdapAuthSettings) {
            url = LdapServerConfig.joinURLS(domain.getAuthLdapURL());
            wantStartTLS = domain.isAuthLdapStartTlsEnabled();
            adminDN = domain.getAuthLdapSearchBindDn();
            adminPassword = domain.getAuthLdapSearchBindPassword();
            searchBase = domain.getAuthLdapSearchBase();
            searchFilterTemplate = domain.getAuthLdapSearchFilter();
        } else {
            url = domain.getAutoProvLdapURL();
            wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
            adminDN = domain.getAutoProvLdapAdminBindDn();
            adminPassword = domain.getAutoProvLdapAdminBindPassword();
            searchBase = domain.getAutoProvLdapSearchBase();
            searchFilterTemplate = domain.getAutoProvLdapSearchFilter();
        }
        
        if (searchBase == null) {
            searchBase = LdapConstants.DN_ROOT_DSE;
        }
        
        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS, 
                null, adminDN, adminPassword, null, "search auto provision directory");
        
        ZLdapContext zlc = null;
        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION_ADMIN_SEARCH);
            
            String searchFilter = null;
            
            if (name != null) {
                /*
                 * search by name with search filter configured on domain
                 */
                if (searchFilterTemplate == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "search filter template is not set on doamin " + domain.getName(), null);
                }
                searchFilter = LdapUtilCommon.computeAuthDn(name, searchFilterTemplate);
            } else {
                /*
                 * search by the provided filter
                 */
                searchFilter = filter;
            }
            
            SearchLdapVisitor ldapVisitor = new SearchLdapVisitor() {
                @Override
                public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs)
                throws StopIteratingException {
                    visitor.visit(dn, attrs);
                }
            };
            
            SearchLdapOptions searchOptions = new SearchLdapOptions(searchBase, searchFilter, 
                    returnAttrs, maxResults, null, ZSearchScope.SEARCH_SCOPE_SUBTREE, ldapVisitor);
            
            zlc.searchPaged(searchOptions);
        } catch (LdapSizeLimitExceededException e) {
            throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many search results returned", e);    
        } finally {
            LdapClient.closeContext(zlc);
        }
    }
   
}



