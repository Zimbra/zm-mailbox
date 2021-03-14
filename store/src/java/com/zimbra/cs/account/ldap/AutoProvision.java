/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import com.sun.mail.smtp.SMTPMessage;
import com.zimbra.common.account.ZAttrProvisioning.AutoProvMode;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZMimeBodyPart;
import com.zimbra.common.zmime.ZMimeMultipart;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DirectoryEntryVisitor;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.LdapClient;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.ldap.LdapException.LdapInvalidAttrValueException;
import com.zimbra.cs.ldap.LdapException.LdapSizeLimitExceededException;
import com.zimbra.cs.ldap.LdapServerConfig.ExternalLdapConfig;
import com.zimbra.cs.ldap.LdapUsage;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.ldap.SearchLdapOptions;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.SearchLdapOptions.StopIteratingException;
import com.zimbra.cs.ldap.ZAttributes;
import com.zimbra.cs.ldap.ZLdapContext;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.cs.ldap.ZLdapFilterFactory.FilterId;
import com.zimbra.cs.ldap.ZSearchResultEntry;
import com.zimbra.cs.ldap.ZSearchScope;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;

public abstract class AutoProvision {

    protected LdapProv prov;
    protected Domain domain;

    protected AutoProvision(LdapProv prov, Domain domain) {
        this.prov = prov;
        this.domain = domain;
    }

    abstract Account handle() throws ServiceException;

    protected Account createAccount(String acctZimbraName, ExternalEntry externalEntry,
            String password, AutoProvMode mode)
    throws ServiceException {
        ZAttributes externalAttrs = externalEntry.getAttrs();

        Map<String, Object> zimbraAttrs = mapAttrs(externalAttrs);

        /*
        // TODO: should we do this?
        String zimbraPassword = RandomPassword.generate();
        zimbraAttrs.put(Provisioning.A_zimbraPasswordMustChange, Provisioning.TRUE);
        */

        // if password is provided, use it
        String zimbraPassword = null;
        if (password != null) {
            zimbraPassword = password;
            zimbraAttrs.remove(Provisioning.A_userPassword);
        }

        Account acct = null;

        try {
            acct = prov.createAccount(acctZimbraName, zimbraPassword, zimbraAttrs);
        } catch (ServiceException e) {
            if (AccountServiceException.ACCOUNT_EXISTS.equals(e.getCode())) {
                ZimbraLog.autoprov.debug("account %s already exists", acctZimbraName);
                // the account already exists, that's fine, just return null
                switch (mode) {
                    case EAGER:
                        return null; // that's fine, just return null
                    case LAZY:
                    case MANUAL:
                    default:
                        throw e;
                }
            } else {
                throw e;
            }
        }

        ZimbraLog.autoprov.info("auto provisioned account: " + acctZimbraName);

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "auto provision Account",
                        "name", acct.getName(), "id", acct.getId()}, zimbraAttrs));

        // send notification email
        try {
            sendNotifMessage(acct, zimbraPassword);
        } catch (ServiceException e) {
            // exception during sending notif email should not fail this method
            ZimbraLog.autoprov.warn("unable to send auto provision notification email", e);
        }

        // invoke post create listener if configured
        try {
            AutoProvisionListener listener = AutoProvisionCachedInfo.getInfo(domain).getListener();
            if (listener != null) {
                listener.postCreate(domain, acct, externalEntry.getDN());
            } else {
                //eager mode should configure Listener
                if (mode == AutoProvMode.EAGER) {
                    ZimbraLog.autoprov.warn("EAGER mode should configure " + Provisioning.A_zimbraAutoProvListenerClass);
                }
            }
        } catch (ServiceException e) {
            // exception during the post create listener should not fail this method
            ZimbraLog.autoprov.warn("encountered error in post auto provision listener", e);
        }

        return acct;
    }

    private static class AutoProvisionCachedInfo {

        private static AutoProvisionCachedInfo getInfo(Domain domain) throws ServiceException {
            AutoProvisionCachedInfo attrMap =
                (AutoProvisionCachedInfo) domain.getCachedData(EntryCacheDataKey.DOMAIN_AUTO_PROVISION_DATA);

            if (attrMap == null) {
                attrMap = new AutoProvisionCachedInfo(domain);
                domain.setCachedData(EntryCacheDataKey.DOMAIN_AUTO_PROVISION_DATA, attrMap);
            }

            return attrMap;
        }

        private static final String DELIMITER = "=";
        private final Map<String, String> attrMap = new HashMap<String, String>();
        private final String[] attrsToFetch;
        private AutoProvisionListener listener;

        private AutoProvisionCachedInfo(Domain domain) throws ServiceException {
            AttributeManager attrMgr = AttributeManager.getInstance();

            // include attrs in schema extension
            Set<String> validAccountAttrs = attrMgr.getAllAttrsInClass(AttributeClass.account);

            String[] rules = domain.getAutoProvAttrMap();

            for (String rule : rules) {
                String[] parts = rule.split(DELIMITER);
                if (parts.length != 2) {
                    throw ServiceException.FAILURE("invalid value in " +
                            Provisioning.A_zimbraAutoProvAttrMap + ": " + rule, null);
                }

                String externalAttr = parts[0];
                String zimbraAttr = parts[1];

                if (!validAccountAttrs.contains(zimbraAttr)) {
                    throw ServiceException.FAILURE("invalid value in " +
                            Provisioning.A_zimbraAutoProvAttrMap + ": " + rule +
                            ", not a valid zimbra attribute ", null);
                }

                attrMap.put(externalAttr, zimbraAttr);
            }

            Set<String> attrs = new HashSet<String>();
            attrs.addAll(attrMap.keySet());
            attrs.add(LdapConstants.ATTR_createTimestamp);
            String nameMapAttr = domain.getAutoProvAccountNameMap();
            if (nameMapAttr != null) {
                attrs.add(nameMapAttr);
            }
            attrsToFetch = attrs.toArray(new String[0]);

            // load listener class and instantiate the handler
            String className = domain.getAutoProvListenerClass();
            if (className != null) {
                try {
                    if (className != null) {
                        listener = ExtensionUtil.findClass(className).
                                asSubclass(AutoProvisionListener.class).newInstance();
                    }
                } catch (ClassNotFoundException e) {
                    ZimbraLog.autoprov.warn(
                            "unable to find auto provision listener class " + className, e);
                } catch (InstantiationException e) {
                    ZimbraLog.autoprov.warn(
                            "unable to instantiate auto provision listener object of class "
                            + className, e);
                } catch (IllegalAccessException e) {
                    ZimbraLog.autoprov.warn(
                            "unable to instantiate auto provision listener object of class "
                            + className, e);
                }
            }
        }

        private String getZimbraAttrName(String externalAttrName) {
            return attrMap.get(externalAttrName);
        }

        private String[] getAttrsToFetch() {
            return attrsToFetch;
        }

        private AutoProvisionListener getListener() {
            return listener;
        }
    }

    protected String[] getAttrsToFetch() throws ServiceException {
        return AutoProvisionCachedInfo.getInfo(domain).getAttrsToFetch();
    }

    /**
     * map external name to zimbra name for the account to be created in Zimbra.
     *
     * @param externalAttrs
     * @return
     * @throws ServiceException
     */
    protected String mapName(ZAttributes externalAttrs, String loginName)
    throws ServiceException {
        String localpart = null;

        String localpartAttr = domain.getAutoProvAccountNameMap();
        if (localpartAttr != null) {
            localpart = externalAttrs.getAttrString(localpartAttr);
            if (localpart == null) {
                throw ServiceException.FAILURE(
                        "AutoProvision: unable to get localpart: " + loginName, null);
            }
        } else {
            if (loginName == null) {
                throw ServiceException.FAILURE(
                        "AutoProvision: unable to map account name, must configure " +
                        Provisioning.A_zimbraAutoProvAccountNameMap, null);
            }
            EmailAddress emailAddr = new EmailAddress(loginName, false);
            localpart = emailAddr.getLocalPart();
        }

        return localpart + "@" + domain.getName();

    }

    protected Map<String, Object> mapAttrs(ZAttributes externalAttrs)
    throws ServiceException {
        AutoProvisionCachedInfo attrMap = AutoProvisionCachedInfo.getInfo(domain);

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
        String url = domain.getAutoProvLdapURL();
        boolean wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
        String adminDN = domain.getAutoProvLdapAdminBindDn();
        String adminPassword = domain.getAutoProvLdapAdminBindPassword();

        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS,
                null, adminDN, adminPassword, null, "auto provision account");

        ZLdapContext zlc = null;

        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION);
            return prov.getHelper().getAttributes(zlc, dn, getAttrsToFetch());
        } finally {
            LdapClient.closeContext(zlc);
        }
    }

    protected static class ExternalEntry {
        private final String dn;
        private final ZAttributes attrs;

        ExternalEntry(String dn, ZAttributes attrs) {
            this.dn = dn;
            this.attrs = attrs;
        }

        String getDN() {
            return dn;
        }

        ZAttributes getAttrs() {
            return attrs;
        }
    }

    protected ExternalEntry getExternalAttrsByName(String loginName)
    throws ServiceException {
        String url = domain.getAutoProvLdapURL();
        boolean wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
        String adminDN = domain.getAutoProvLdapAdminBindDn();
        String adminPassword = domain.getAutoProvLdapAdminBindPassword();
        String[] attrs = getAttrsToFetch();

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
                String searchFilter = LdapUtil.computeDn(loginName, searchFilterTemplate);
                ZimbraLog.autoprov.debug("AutoProvision: computed search filter" + searchFilter);
                ZSearchResultEntry entry = prov.getHelper().searchForEntry(
                        searchBase, ZLdapFilterFactory.getInstance().fromFilterString(
                                FilterId.AUTO_PROVISION_SEARCH, searchFilter),
                        zlc, attrs);
                if (entry == null) {
                    throw AccountServiceException.NO_SUCH_EXTERNAL_ENTRY(loginName);
                }
                return new ExternalEntry(entry.getDN(), entry.getAttributes());
            }

            String bindDNTemplate = domain.getAutoProvLdapBindDn();
            if (bindDNTemplate != null) {
                // get attrs by external DN template
                String dn = LdapUtil.computeDn(loginName, bindDNTemplate);
                ZimbraLog.autoprov.debug("AutoProvision: computed external DN" + dn);
                return new ExternalEntry(dn, prov.getHelper().getAttributes(zlc, dn, attrs));
            }

        } finally {
            LdapClient.closeContext(zlc);
        }

        throw ServiceException.FAILURE("One of " + Provisioning.A_zimbraAutoProvLdapBindDn +
                " or " + Provisioning.A_zimbraAutoProvLdapSearchFilter + " must be set", null);
    }

    private String fillTemplate(Account acct, String template) {
        String text = template.replaceAll("\\$\\{ACCOUNT_ADDRESS\\}", acct.getName());
        
        String displayName = acct.getDisplayName();
        if (displayName == null) {
            displayName = "";
        }
        text = text.replaceAll("\\$\\{ACCOUNT_DISPLAY_NAME\\}", displayName);
        return text;
    }

    protected void sendNotifMessage(Account acct, String password)
    throws ServiceException {
        String subject = fillTemplate(acct, domain.getAutoProvNotificationSubject());
        String body = fillTemplate(acct, domain.getAutoProvNotificationBody());
        
        String from = domain.getAutoProvNotificationFromAddress();
        if (from == null) {
            // if From address is configured, notification is not sent.
            // TODO: should we use a seperate boolean control?
            return;
        }

        String toAddr = acct.getName();

        try {
            SMTPMessage out = AccountUtil.getSmtpMessageObj(acct);

            InternetAddress addr = null;
            try {
                addr = new JavaMailInternetAddress(from);
            } catch (AddressException e) {
                // log and try the next one
                ZimbraLog.autoprov.warn("invalid address in " +
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
            out.setSubject(subject);
            
            // NOTIFY=NEVER
            out.setNotifyOptions(SMTPMessage.NOTIFY_NEVER);

            // body
            MimeMultipart mmp = new ZMimeMultipart("alternative");

            // TEXT part (add me first!)
            String text = body;
            MimeBodyPart textPart = new ZMimeBodyPart();
            textPart.setText(text, MimeConstants.P_CHARSET_UTF8);
            mmp.addBodyPart(textPart);

            // HTML part
            StringBuilder html = new StringBuilder();
            html.append("<h4>\n");
            html.append("<p>" + body + "</p>\n");
            html.append("</h4>\n");
            html.append("\n");

            MimeBodyPart htmlPart = new ZMimeBodyPart();
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
            ZimbraLog.autoprov.info("auto provision notification sent rcpt='" +
                    rcptAddr + "' Message-ID=" + out.getMessageID());

        } catch (MessagingException e) {
            ZimbraLog.autoprov.warn("send auto provision notification failed rcpt='" + toAddr +"'", e);
        }
    }

    private static abstract class MimePartDataSource implements DataSource {

        private final String mText;
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

    /*
     * entries are returned in DirectoryEntryVisitor interface.
     */
    static void searchAutoProvDirectory(LdapProv prov, Domain domain,
            String filter, String name, String createTimestampLaterThan,
            String[] returnAttrs, int maxResults, final DirectoryEntryVisitor visitor)
    throws ServiceException {

        SearchLdapVisitor ldapVisitor = new SearchLdapVisitor() {
            @Override
            public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs)
            throws StopIteratingException {
                visitor.visit(dn, attrs);
            }
        };

        searchAutoProvDirectory(prov, domain, filter,  name,  createTimestampLaterThan,
                returnAttrs,  maxResults,  ldapVisitor, false);
    }

    /**
     * Search the external auto provision LDAP source
     *
     * Only one of filter or name can be provided.
     * - if name is provided, the search filter will be zimbraAutoProvLdapSearchFilter
     *   with place holders filled with the name.
     *
     * - if filter is provided, the provided filter will be the search filter.
     *
     * - if neither is provided, the search filter will be zimbraAutoProvLdapSearchFilter
     *   with place holders filled with "*".   If createTimestampLaterThan
     *   is provided, the search filter will be ANDed with (createTimestamp >= {timestamp})
     *
     * @param prov
     * @param domain
     * @param filter
     * @param name
     * @param createTimestampLaterThan
     * @param returnAttrs
     * @param maxResults
     * @param ldapVisitor
     * @param wantPartialResult whether TOO_MANY_SEARCH_RESULTS should be thrown if the
     *                          ldap search encountered LdapSizeLimitExceededException
     *                          Note: regardless of this parameter, the ldapVisitor.visit
     *                          is called for each entry returned from LDAP.
     *                          This behavior is currently hardcoded in
     *                          UBIDLdapContext.searchPaged and has been the legacy behavior.
     *                          We can probably change it into a parameter in SearchLdapOptions.
     * @throws ServiceException
     * @return whether LdapSizeLimitExceededException was hit
     */
    static boolean searchAutoProvDirectory(LdapProv prov, Domain domain,
            String filter, String name, String createTimestampLaterThan,
            String[] returnAttrs, int maxResults, SearchLdapVisitor ldapVisitor,
            boolean wantPartialResult)
    throws ServiceException {
        // use either filter or name, make sure only one is provided
        if ((filter != null) && (name != null)) {
            throw ServiceException.INVALID_REQUEST("only one of filter or name can be provided", null);
        }

        String url = domain.getAutoProvLdapURL();
        boolean wantStartTLS = domain.isAutoProvLdapStartTlsEnabled();
        String adminDN = domain.getAutoProvLdapAdminBindDn();
        String adminPassword = domain.getAutoProvLdapAdminBindPassword();
        String searchBase = domain.getAutoProvLdapSearchBase();
        String searchFilterTemplate = domain.getAutoProvLdapSearchFilter();
        FilterId filterId = FilterId.AUTO_PROVISION_SEARCH;

        if (url == null) {
            throw ServiceException.FAILURE(
                    String.format("missing %s on domain %s", Provisioning.A_zimbraAutoProvLdapURL, domain.getName()), null);
        }
        if (searchBase == null) {
            searchBase = LdapConstants.DN_ROOT_DSE;
        }

        ExternalLdapConfig config = new ExternalLdapConfig(url, wantStartTLS,
                null, adminDN, adminPassword, null, "search auto provision directory");

        boolean hitSizeLimitExceededException = false;
        ZLdapContext zlc = null;
        ZLdapFilter zFilter = null;
        try {
            zlc = LdapClient.getExternalContext(config, LdapUsage.AUTO_PROVISION_ADMIN_SEARCH);

            String searchFilter = null;
            String searchFilterWithoutLastPolling = null;

            if (name != null) {
                if (searchFilterTemplate == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "search filter template is not set on domain " + domain.getName(), null);
                }
                searchFilter = LdapUtil.computeDn(name, searchFilterTemplate);
            } else if (filter != null) {
                searchFilter = filter;
                filterId = FilterId.AUTO_PROVISION_ADMIN_SEARCH;
            } else {
                if (searchFilterTemplate == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "search filter template is not set on domain " + domain.getName(), null);
                }
                searchFilter = LdapUtil.computeDn("*", searchFilterTemplate);
                if (createTimestampLaterThan != null) {
                    searchFilterWithoutLastPolling = searchFilter;
                    // searchFilter = "(&" + searchFilter + "(createTimestamp>=" + createTimestampLaterThan + "))";
                    searchFilter = "(&" + searchFilter +
                            ZLdapFilterFactory.getInstance().createdLaterOrEqual(createTimestampLaterThan).toFilterString() + ")";
                    filterId = FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN;
                }
            }

            zFilter = ZLdapFilterFactory.getInstance().fromFilterString(filterId, searchFilter);
            SearchLdapOptions searchOptions;
            try {
                searchOptions = new SearchLdapOptions(searchBase, zFilter,
                    returnAttrs, maxResults, null, ZSearchScope.SEARCH_SCOPE_SUBTREE, ldapVisitor);
                zlc.searchPaged(searchOptions);
            } catch (LdapInvalidAttrValueException eav) {
                ZimbraLog.autoprov.info("Retrying ldap search query with createTimestamp in seconds.");
                if (searchFilterWithoutLastPolling != null && createTimestampLaterThan != null) {
                    createTimestampLaterThan = createTimestampLaterThan.replaceAll("\\..*Z$", "Z");
                    // searchFilter = "(&" + searchFilter + "(createTimestamp>=" + createTimestampLaterThan + "))";
                    searchFilter = "(&" + searchFilterWithoutLastPolling +
                            ZLdapFilterFactory.getInstance().createdLaterOrEqual(createTimestampLaterThan).toFilterString() + ")";
                    ZimbraLog.autoprov.info("new searchFilter = %s", searchFilter);
                    filterId = FilterId.AUTO_PROVISION_SEARCH_CREATED_LATERTHAN;
                }
                zFilter = ZLdapFilterFactory.getInstance().fromFilterString(filterId, searchFilter);
                searchOptions = new SearchLdapOptions(searchBase, zFilter,
                    returnAttrs, maxResults, null, ZSearchScope.SEARCH_SCOPE_SUBTREE, ldapVisitor);
                zlc.searchPaged(searchOptions);
            }
        } catch (LdapSizeLimitExceededException e) {
            hitSizeLimitExceededException = true;
            if (wantPartialResult) {
                // log at debug level
                ZimbraLog.autoprov.debug(
                        String.format("searchAutoProvDirectory encountered LdapSizeLimitExceededException: " +
                        "base=%s, filter=%s", searchBase, zFilter == null ? "" : zFilter.toFilterString()),
                        e);
            } else {
                throw AccountServiceException.TOO_MANY_SEARCH_RESULTS("too many search results returned", e);
            }
        } finally {
            LdapClient.closeContext(zlc);
        }
        return hitSizeLimitExceededException;
    }

}



