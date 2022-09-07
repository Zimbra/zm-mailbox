/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.account;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.AttributeFlag;
import com.zimbra.cs.account.AttributeManager;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.Zimlet;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.admin.AdminAccessControl;
import com.zimbra.cs.service.mail.ModifyProfileImage;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.BuildInfo;
import com.zimbra.cs.zimlet.ZimletPresence;
import com.zimbra.cs.zimlet.ZimletUserProperties;
import com.zimbra.cs.zimlet.ZimletUtil;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.Prop;

/**
 * @since May 26, 2004
 * @author schemers
 */
public class GetInfo extends AccountDocumentHandler  {

    public interface GetInfoExt {
        public void handle(ZimbraSoapContext zsc, Element getInfoResponse);
    }

    private static ArrayList<GetInfoExt> extensions = new ArrayList<GetInfoExt>();

    public static void addExtension(GetInfoExt extension) {
        synchronized (extensions) {
            extensions.add(extension);
        }
    }

    private enum Section {
        MBOX, PREFS, ATTRS, ZIMLETS, PROPS, IDENTS, SIGS, DSRCS, CHILDREN;

        static Section lookup(String value) throws ServiceException {
            try {
                return Section.valueOf(value.toUpperCase().trim());
            } catch (IllegalArgumentException iae) {
                throw ServiceException.INVALID_REQUEST("unknown GetInfo section: " + value.trim(), null);
            }
        }
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);
        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }

        // figure out the subset of data the caller wants (default to all data)
        String secstr = request.getAttribute(AccountConstants.A_SECTIONS, null);
        Set<Section> sections;
        if (!StringUtil.isNullOrEmpty(secstr)) {
            sections = EnumSet.noneOf(Section.class);
            for (String sec : Splitter.on(',').omitEmptyStrings().trimResults().split(secstr)) {
                sections.add(Section.lookup(sec));
            }
        } else {
            sections = EnumSet.allOf(Section.class);
        }

        String rightsStr = request.getAttribute(AccountConstants.A_RIGHTS, null);
        Set<Right> rights = null;
        if (!StringUtil.isNullOrEmpty(rightsStr)) {
            RightManager rightMgr = RightManager.getInstance();
            rights = Sets.newHashSet();
            for (String right : Splitter.on(',').omitEmptyStrings().trimResults().split(rightsStr)) {
                rights.add(rightMgr.getUserRight(right));
            }
        }


        Element response = zsc.createElement(AccountConstants.GET_INFO_RESPONSE);
        response.addAttribute(AccountConstants.E_VERSION, BuildInfo.FULL_VERSION, Element.Disposition.CONTENT);
        response.addAttribute(AccountConstants.E_ID, account.getId(), Element.Disposition.CONTENT);
        response.addAttribute(AccountConstants.E_NAME, account.getUnicodeName(), Element.Disposition.CONTENT);
        int profileId = getProfileId(mbox, octxt);
        if (profileId != 0) {
            response.addAttribute(AccountConstants.E_PROFILE_IMAGE_ID, profileId,
                Element.Disposition.CONTENT);
        }
        try {
            response.addAttribute(AccountConstants.E_CRUMB, zsc.getAuthToken().getCrumb(), Element.Disposition.CONTENT);
        } catch (AuthTokenException e) {
            // shouldn't happen
            ZimbraLog.account.warn("can't generate crumb", e);
        }
        long lifetime = zsc.getAuthToken().getExpires() - System.currentTimeMillis();
        response.addAttribute(AccountConstants.E_LIFETIME, lifetime, Element.Disposition.CONTENT);

        Provisioning prov = Provisioning.getInstance();

        // bug 53770, return if the request is using a delegated authtoken issued to an admin account
        AuthToken authToken = zsc.getAuthToken();
        if (authToken.isDelegatedAuth()) {
            Account admin = prov.get(AccountBy.id, authToken.getAdminAccountId());
            if (admin != null) {
                boolean isAdmin = AdminAccessControl.isAdequateAdminAccount(admin);
                if (isAdmin) {
                    response.addAttribute(AccountConstants.E_ADMIN_DELEGATED, true, Element.Disposition.CONTENT);
                }
            }
        }

        try {
            Server server = prov.getLocalServer();
            if (server != null) {
                response.addAttribute(AccountConstants.A_DOCUMENT_SIZE_LIMIT, server.getFileUploadMaxSize());
                //For ZBUG-1280: checks if atleast one URL is present from zimbraSpellCheckURL.
                response.addAttribute(AccountConstants.A_IS_SPELL_CHECK_AVAILABLE, isSpellCheckServiceAvailable(server));
            }
            Config config = prov.getConfig();
            if (config != null) {
                long maxAttachSize = config.getMtaMaxMessageSize();
                if (maxAttachSize == 0) {
                    maxAttachSize = -1;  /* means unlimited */
                }
                response.addAttribute(AccountConstants.A_ATTACHMENT_SIZE_LIMIT, maxAttachSize);
            }
        } catch (ServiceException e) {
        }

        if (sections.contains(Section.MBOX) && Provisioning.onLocalServer(account)) {
            response.addAttribute(AccountConstants.E_REST, UserServlet.getRestUrl(account), Element.Disposition.CONTENT);
                response.addAttribute(AccountConstants.E_QUOTA_USED, mbox.getSize(), Element.Disposition.CONTENT);
                response.addAttribute(AccountConstants.E_IS_TRACKING_IMAP, mbox.isTrackingImap(), Element.Disposition.CONTENT);

                Session s = (Session) context.get(SoapEngine.ZIMBRA_SESSION);
                if (s instanceof SoapSession) {
                    // we have a valid session; get the stats on this session
                    response.addAttribute(AccountConstants.E_PREVIOUS_SESSION,
                            ((SoapSession) s).getPreviousSessionTime(), Element.Disposition.CONTENT);
                    response.addAttribute(AccountConstants.E_LAST_ACCESS,
                            ((SoapSession) s).getLastWriteAccessTime(), Element.Disposition.CONTENT);
                    response.addAttribute(AccountConstants.E_RECENT_MSGS,
                            ((SoapSession) s).getRecentMessageCount(), Element.Disposition.CONTENT);
                } else {
                    // we have no session; calculate the stats from the mailbox and the other SOAP sessions
                    long lastAccess = mbox.getLastSoapAccessTime();
                    response.addAttribute(AccountConstants.E_PREVIOUS_SESSION, lastAccess, Element.Disposition.CONTENT);
                    response.addAttribute(AccountConstants.E_LAST_ACCESS, lastAccess, Element.Disposition.CONTENT);
                    response.addAttribute(AccountConstants.E_RECENT_MSGS,
                            mbox.getRecentMessageCount(), Element.Disposition.CONTENT);
                }
        }

        doCos(account, response);

        Map<String, Object> attrMap = account.getUnicodeAttrs();
        Locale locale = Provisioning.getInstance().getLocale(account);

        if (sections.contains(Section.PREFS)) {
            Element prefs = response.addUniqueElement(AccountConstants.E_PREFS);
            GetPrefs.doPrefs(account, prefs, attrMap, null);
        }
        if (sections.contains(Section.ATTRS)) {
            Element attrs = response.addUniqueElement(AccountConstants.E_ATTRS);
            doAttrs(account, locale.toString(), attrs, attrMap);
        }
        if (sections.contains(Section.ZIMLETS)) {
            Element zimlets = response.addUniqueElement(AccountConstants.E_ZIMLETS);
            doZimlets(zimlets, account);
        }
        if (sections.contains(Section.PROPS)) {
            Element props = response.addUniqueElement(AccountConstants.E_PROPERTIES);
            doProperties(props, account);
        }
        if (sections.contains(Section.IDENTS)) {
            Element ids = response.addUniqueElement(AccountConstants.E_IDENTITIES);
            doIdentities(ids, account);
        }
        if (sections.contains(Section.SIGS)) {
            Element sigs = response.addUniqueElement(AccountConstants.E_SIGNATURES);
            doSignatures(sigs, account);
        }
        if (sections.contains(Section.DSRCS)) {
            Element ds = response.addUniqueElement(AccountConstants.E_DATA_SOURCES);
            doDataSources(ds, account);
        }
        if (sections.contains(Section.CHILDREN)) {
            Element ca = response.addUniqueElement(AccountConstants.E_CHILD_ACCOUNTS);
            doChildAccounts(ca, account, zsc.getAuthToken());
        }

        if (rights != null && !rights.isEmpty()) {
            Element eRights = response.addUniqueElement(AccountConstants.E_RIGHTS);
            doDiscoverRights(eRights, account, rights);
        }

        GetAccountInfo.addUrls(response, account);

        for (GetInfoExt extension : extensions) {
            extension.handle(zsc, response);
        }

        // we do not have any ldap attrs to define whether pasteitcleaned service is installed and running
        // so check if pasteitcleaned service is installed and running by checking the installed directory and connectivity
        // if yes return pasteitcleanedEnabled = true else return pasteitcleanedEnabled = false
        response.addAttribute("pasteitcleanedEnabled", checkIfPasteitcleanedInstalled(), Element.Disposition.CONTENT);

        return response;
    }

    private static int getProfileId(Mailbox mbox, OperationContext octxt) throws ServiceException {
        String folderName = ModifyProfileImage.IMAGE_FOLDER_PREFIX + mbox.getAccountId();
        int folderId;
        int imageId = 0;
        try {
            Folder imgFolder = mbox.getFolderByName(octxt, Mailbox.ID_FOLDER_ROOT, folderName);
            folderId = imgFolder.getId();
            TypedIdList ids = mbox.getItemIds(octxt, folderId);
            List<Integer> idList = ids.getAllIds();
            MailItem[] itemList = mbox.getItemById(octxt, idList, MailItem.Type.DOCUMENT);
            for (MailItem item : itemList) {
                CustomMetadata customData = item
                    .getCustomData(ModifyProfileImage.IMAGE_CUSTOM_DATA_SECTION);
                if (customData.containsKey("p") && customData.get("p").equals("1")) {
                    imageId = item.getId();
                    break;
                }
            }
        } catch (ServiceException exception) {
            if (MailServiceException.NO_SUCH_FOLDER.equals(exception.getCode())) {
                ZimbraLog.account.debug("Profile image folder doesn't exist");
            } else {
                ZimbraLog.account.error("can't get profile image id : %s", exception.getMessage());
                ZimbraLog.account.debug("can't get profile image id", exception);
            }
        }
        return imageId;
    }

    static void doCos(Account acct, Element response) throws ServiceException {
        Cos cos = Provisioning.getInstance().getCOS(acct);
        if (cos != null) {
            Element eCos = response.addUniqueElement(AccountConstants.E_COS);
            eCos.addAttribute(AccountConstants.A_ID, cos.getId());
            eCos.addAttribute(AccountConstants.A_NAME, cos.getName());
        }
    }

    static void doAttrs(Account acct, String locale, Element response, Map<String,Object> attrsMap)
            throws ServiceException {
        AttributeManager attrMgr = AttributeManager.getInstance();

        Set<String> attrList = attrMgr.getAttrsWithFlag(AttributeFlag.accountInfo);

        Set<String> acctAttrs = attrMgr.getAllAttrsInClass(AttributeClass.account);
        Set<String> domainAttrs = attrMgr.getAllAttrsInClass(AttributeClass.domain);
        Set<String> serverAttrs = attrMgr.getAllAttrsInClass(AttributeClass.server);
        Set<String> configAttrs = attrMgr.getAllAttrsInClass(AttributeClass.globalConfig);

        Provisioning prov = Provisioning.getInstance();
        Domain domain = prov.getDomain(acct);
        Server server = acct.getServer();
        Config config = prov.getConfig();

        for (String key : attrList) {
            Object value = null;
            if (Provisioning.A_zimbraLocale.equals(key)) {
                value = locale;
            } else if (Provisioning.A_zimbraAttachmentsBlocked.equals(key)) {
                // leave this a special case for now, until we have enough incidences to make it a pattern
                value = config.isAttachmentsBlocked() || acct.isAttachmentsBlocked() ?
                        ProvisioningConstants.TRUE : ProvisioningConstants.FALSE;
            } else if (Provisioning.A_zimbraMailQuota.equals(key)) {
                // setting effective quota value refer ZBUG-1869 
                value = String.valueOf(AccountUtil.getEffectiveQuota(acct));
            } else if (Provisioning.A_zimbraFeatureDocumentEditingEnabled.equals(key)) { 
                value = AccountUtil.isDocumentEditingEnabled(acct) ? 
                        ProvisioningConstants.TRUE : ProvisioningConstants.FALSE;
            } else {
                value = attrsMap.get(key);

                if (value == null) { // no value on account/cos
                    if (!acctAttrs.contains(key)) { // not an account attr
                        // see if it is on domain, server, or globalconfig
                        if (domainAttrs.contains(key)) {
                            if (domain != null) {
                                value = domain.getMultiAttr(key); // value on domain/global config (domainInherited)
                            }
                        } else if (serverAttrs.contains(key)) {
                            value = server.getMultiAttr(key); // value on server/global config (serverInherited)
                        } else if (configAttrs.contains(key)) {
                            value = config.getMultiAttr(key); // value on global config
                        }
                    }
                }
            }

            ToXML.encodeAttr(response, key, value);
        }
        // ZCS-10678: Include Local config change for zimbraPasswordAllowUsername in getinfo response
        ToXML.encodeAttr(response, "zimbraPasswordAllowUsername",
                Boolean.toString(LC.allow_username_within_password.booleanValue()).toUpperCase());
    }

    private static void doZimlets(Element response, Account acct) {
        try {
            // bug 34517
            ZimletUtil.migrateUserPrefIfNecessary(acct);

            ZimletPresence userZimlets = ZimletUtil.getUserZimlets(acct);
            List<Zimlet> zimletList = ZimletUtil.orderZimletsByPriority(userZimlets.getZimletNamesAsArray());
            zimletList.removeIf(zimlet -> AdminConstants.ZEXTRAS_PACKAGES_LIST.contains(zimlet.getName()));
            int priority = 0;
            for (Zimlet z : zimletList) {
                if (z.isEnabled() && !z.isExtension()) {
                    ZimletUtil.listZimlet(response, z, priority, userZimlets.getPresence(z.getName()));
                }
                priority++;
            }

            // load the zimlets in the dev directory and list them
            ZimletUtil.listDevZimlets(response);
        } catch (ServiceException se) {
            ZimbraLog.account.error("can't get zimlets", se);
        }
    }

    private static void doProperties(Element response, Account acct) {
        ZimletUserProperties zp = ZimletUserProperties.getProperties(acct);
        Set<? extends Prop> props = zp.getAllProperties();
        for (Prop prop : props) {
            Element elem = response.addElement(AccountConstants.E_PROPERTY);
            elem.addAttribute(AccountConstants.A_ZIMLET, prop.getZimlet());
            elem.addAttribute(AccountConstants.A_NAME, prop.getName());
            elem.setText(prop.getValue());
        }
    }

    private static void doIdentities(Element response, Account acct) {
        try {
            for (Identity i : Provisioning.getInstance().getAllIdentities(acct)) {
                ToXML.encodeIdentity(response, i);
            }
        } catch (ServiceException e) {
            ZimbraLog.account.error("can't get identities", e);
        }
    }

    private static void doSignatures(Element response, Account acct) {
        try {
            List<Signature> signatures = Provisioning.getInstance().getAllSignatures(acct);
            for (Signature s : signatures) {
                ToXML.encodeSignature(response, s);
            }
        } catch (ServiceException e) {
            ZimbraLog.account.error("can't get signatures", e);
        }
    }

    private static void doDataSources(Element response, Account acct) {
        try {
            List<DataSource> dataSources = Provisioning.getInstance().getAllDataSources(acct);
            for (DataSource ds : dataSources) {
                if (!ds.isInternal()) {
                    com.zimbra.cs.service.mail.ToXML.encodeDataSource(response, ds);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("Unable to get data sources", e);
        }
    }

    protected void doChildAccounts(Element response, Account acct, AuthToken authToken) throws ServiceException {
        String[] childAccounts = acct.getMultiAttr(Provisioning.A_zimbraChildAccount);
        String[] visibleChildAccounts = acct.getMultiAttr(Provisioning.A_zimbraPrefChildVisibleAccount);

        if (childAccounts.length == 0 && visibleChildAccounts.length == 0) {
            return;
        }
        Provisioning prov = Provisioning.getInstance();
        Set<String> children = new HashSet<String>(childAccounts.length);

        for (String childId : visibleChildAccounts) {
            if (children.contains(childId)) {
                continue;
            }
            Account child = prov.get(Key.AccountBy.id, childId, authToken);
            if (child != null) {
                encodeChildAccount(response, child, true);
            }
            children.add(childId);
        }

        for (String childId : childAccounts) {
            if (children.contains(childId)) {
                continue;
            }
            Account child = prov.get(Key.AccountBy.id, childId, authToken);
            if (child != null) {
                encodeChildAccount(response, child, false);
            }
            children.add(childId);
        }
    }

    protected Element encodeChildAccount(Element parent, Account child, boolean isVisible) {
        Element elem = parent.addElement(AccountConstants.E_CHILD_ACCOUNT);
        elem.addAttribute(AccountConstants.A_ID, child.getId());
        elem.addAttribute(AccountConstants.A_NAME, child.getUnicodeName());
        elem.addAttribute(AccountConstants.A_VISIBLE, isVisible);
        elem.addAttribute(AccountConstants.A_ACTIVE, child.isAccountStatusActive());

        String displayName = child.getAttr(Provisioning.A_displayName);
        if (displayName != null) {
            Element attrsElem = elem.addUniqueElement(AccountConstants.E_ATTRS);
            attrsElem.addKeyValuePair(Provisioning.A_displayName, displayName,
                    AccountConstants.E_ATTR, AccountConstants.A_NAME);
        }
        return elem;
    }

    private void doDiscoverRights(Element eRights, Account account, Set<Right> rights) throws ServiceException {
        DiscoverRights.discoverRights(account, rights, eRights, false);
    }

    /**
     * This check is done for zbug-1280.
     * checks and returns true when zimbraSpellCheckURL is non-empty on a server
     * The spellcheck button is shown only when zimbraSpellCheckURL returns non-empty value.
     */
    public boolean isSpellCheckServiceAvailable(Server server) {
        String[] urls = server.getMultiAttr(Provisioning.A_zimbraSpellCheckURL);
        return !ArrayUtil.isEmpty(urls);
    }

    private boolean checkIfPasteitcleanedInstalled() {
        String libLocation = "/opt/zimbra/common/lib/pasteitcleaned";
        String extLoc = "/opt/zimbra/lib/ext/pasteitcleaned/zimbra-pasteitcleaned.jar";
        HttpURLConnection connection = null;
        try {
            File lib = new File(libLocation);
            File ext = new File(extLoc);
            if (lib.exists() && lib.isDirectory() && lib.canRead()) {
                if (ext.exists() && ext.isFile() && ext.canRead()) {
                    connection = (HttpURLConnection) new URL("http://localhost:5000").openConnection();
                    connection.setConnectTimeout(1000);
                    connection.connect();
                    return true;
                }
            }
            ZimbraLog.account.debug("pasteitcleaned service is not installed or running");
            return false;
        } catch(InvalidPathException | SecurityException | IOException ex) {
            ZimbraLog.account.info("exception occurred : %s", ex.getMessage());
            return false;
        }
    }
}
