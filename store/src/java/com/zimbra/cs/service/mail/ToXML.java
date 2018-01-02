/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.CalendarUtil;
import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.ItemIdentifier;
import com.zimbra.common.mime.ContentDisposition;
import com.zimbra.common.mime.ContentType;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.QuotedTextUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.TruncatingWriter;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.zmime.ZInternetHeader;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.cs.account.accesscontrol.ZimbraACE;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.gal.GalGroup.GroupInfo;
import com.zimbra.cs.gal.GalGroup;
import com.zimbra.cs.gal.GalGroupInfoProvider;
import com.zimbra.cs.gal.GalGroupMembers.ContactDLMembers;
import com.zimbra.cs.html.BrowserDefang;
import com.zimbra.cs.html.DefangFactory;
import com.zimbra.cs.html.DefangFilter;
import com.zimbra.cs.html.HtmlDefang;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SearchParams.ExpandResults;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.AlarmData;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.Chat;
import com.zimbra.cs.mailbox.Comment;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Contact.Attachment;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Link;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.CustomMetadata;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.Note;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.OperationContextData;
import com.zimbra.cs.mailbox.RetentionPolicyManager;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.SenderList;
import com.zimbra.cs.mailbox.SmartFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.WikiItem;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteChanges;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedAddress;
import com.zimbra.cs.mime.handler.TextEnrichedHandler;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.smime.SmimeHandler;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.mail.type.AlarmDataInfo;
import com.zimbra.soap.mail.type.CalendarReply;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;
import com.zimbra.soap.mail.type.XParam;
import com.zimbra.soap.mail.type.XProp;
import com.zimbra.soap.type.MsgContent;
import com.zimbra.soap.type.WantRecipsSetting;

/**
 * Class containing static methods for encoding various MailItem-derived objects into XML.
 *
 * @since 2004. 9. 15.
 */
public final class ToXML {
    private static final Log LOG = LogFactory.getLog(ToXML.class);

    public static enum OutputParticipants {
        PUT_SENDERS(0),
        PUT_RECIPIENTS(1),
        PUT_BOTH(2);

        private int value;
        OutputParticipants(int value) {
            this.value = value;
        }

        public int getIntValue() {
            return value;
        }

        public static OutputParticipants fromJaxb(WantRecipsSetting jaxb) {
            jaxb = WantRecipsSetting.usefulValue(jaxb);
            if (WantRecipsSetting.PUT_RECIPIENTS.equals(jaxb)) {
                return PUT_RECIPIENTS;
            } else if (WantRecipsSetting.PUT_BOTH.equals(jaxb)) {
                return PUT_BOTH;
            }
            return PUT_SENDERS;
        }
    }

    // we usually don't want to return conflict info.  modseq/imapuid excluded for backwards compatibility.
    public static final int NOTIFY_FIELDS = Change.ALL_FIELDS & ~Change.CONFLICT & ~Change.MODSEQ & ~Change.IMAP_UID;

    // no construction
    private ToXML() {
    }

    public static Element encodeItem(Element parent, ItemIdFormatter ifmt, OperationContext octxt, MailItem item,
            int fields) throws ServiceException {
        if (item instanceof Folder) {
            return encodeFolder(parent, ifmt, octxt, (Folder) item, fields);
        } else if (item instanceof SmartFolder) {
            return encodeSmartFolder(parent, ifmt, octxt, (SmartFolder) item, fields);
        } else if (item instanceof Tag) {
            return encodeTag(parent, ifmt, octxt, (Tag) item, fields);
        } else if (item instanceof Note) {
            return encodeNote(parent, ifmt, octxt, (Note) item, fields);
        } else if (item instanceof Contact) {
            return encodeContact(parent, ifmt, octxt, (Contact) item, false, null, fields);
        } else if (item instanceof CalendarItem) {
            return encodeCalendarItemSummary(parent, ifmt, octxt, (CalendarItem) item, fields, true);
        } else if (item instanceof Conversation) {
            return encodeConversationSummary(parent, ifmt, octxt, (Conversation) item, null,
                    OutputParticipants.PUT_SENDERS, fields, false);
        } else if (item instanceof WikiItem) {
            return encodeWiki(parent, ifmt, octxt, (WikiItem) item, fields);
        } else if (item instanceof Document) {
            return encodeDocument(parent, ifmt, octxt, (Document) item, fields);
        } else if (item instanceof Message) {
            OutputParticipants output = fields == NOTIFY_FIELDS ?
                    OutputParticipants.PUT_BOTH : OutputParticipants.PUT_SENDERS;
            return encodeMessageSummary(parent, ifmt, octxt, (Message) item, output, fields);
        } else if (item instanceof Comment) {
            return encodeComment(parent, ifmt, octxt, (Comment) item, fields);
        } else {
            return null;
        }
    }

    private static boolean needToOutput(int fields, int fieldMask) {
        return ((fields & fieldMask) > 0);
    }

    public static boolean hasFullAccess(Mailbox mbox, OperationContext octxt) throws ServiceException {
        if (octxt == null || !octxt.isDelegatedRequest(mbox)) {
            return true;
        } else {
            return AccessManager.getInstance().canAccessAccount(octxt.getAuthenticatedUser(), mbox.getAccount(), octxt.isUsingAdminPrivileges());
        }
    }

    public static Element encodeMailbox(Element parent, OperationContext octxt, Mailbox mbox) {
        return encodeMailbox(parent, octxt, mbox, NOTIFY_FIELDS);
    }

    public static Element encodeMailbox(Element parent, OperationContext octxt, Mailbox mbox, int fields) {
        Element elem = parent.addNonUniqueElement(MailConstants.E_MAILBOX);
        if (octxt.isDelegatedRequest(mbox)) {
            elem.addAttribute(HeaderConstants.A_ACCOUNT_ID, mbox.getAccountId());
        }
        if (needToOutput(fields, Change.SIZE)) {
            elem.addAttribute(MailConstants.A_SIZE, mbox.getSize());
        }
        return elem;
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Folder folder)
    throws ServiceException {
        return encodeFolder(parent, ifmt, octxt, folder, NOTIFY_FIELDS);
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Folder folder, int fields)
    throws ServiceException {
        return encodeFolder(parent, ifmt, octxt, folder, fields, false);
    }

    public static Element encodeFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Folder folder, int fields, boolean exposeAclAccessKey)
    throws ServiceException {
        if (folder instanceof SearchFolder) {
            return encodeSearchFolder(parent, ifmt, (SearchFolder) folder, fields);
        } else if (folder instanceof Mountpoint) {
            return encodeMountpoint(parent, ifmt, octxt, (Mountpoint) folder, fields);
        }
        Element elem = parent.addNonUniqueElement(MailConstants.E_FOLDER);
        encodeFolderCommon(elem, ifmt, folder, fields);
        if (needToOutput(fields, Change.SIZE)) {
            int deleted = folder.getDeletedCount();
            elem.addAttribute(MailConstants.A_NUM, folder.getItemCount() - deleted);
            if (deleted > 0) {
                elem.addAttribute(MailConstants.A_IMAP_NUM, folder.getItemCount());
            }
            elem.addAttribute(MailConstants.A_SIZE, folder.getTotalSize());
            elem.addAttribute(MailConstants.A_IMAP_MODSEQ, folder.getImapMODSEQ());
            elem.addAttribute(MailConstants.A_IMAP_UIDNEXT, folder.getImapUIDNEXT());
        }

        if (needToOutput(fields, Change.URL)) {
            String url = folder.getUrl();
            if (!url.isEmpty() || fields != NOTIFY_FIELDS) {
                // Note: in this case, a url on a folder object
                // is not a url to the folder, but the url to another item that's
                // external of the mail system. In most cases this is a 'synced' folder
                // that is either RSS or a remote calendar object
                elem.addAttribute(MailConstants.A_URL, HttpUtil.sanitizeURL(url));
            }
        }

        Mailbox mbox = folder.getMailbox();
        boolean remote = octxt != null && octxt.isDelegatedRequest(mbox);

        boolean canAdminister = !remote;
        boolean canDelete = canAdminister;
        if (remote) {
            // return effective permissions only for remote folders
            String perms = encodeEffectivePermissions(folder, octxt);
            elem.addAttribute(MailConstants.A_RIGHTS, perms);
            canAdminister = perms != null && perms.indexOf(ACL.ABBR_ADMIN) != -1;
            // Need to know retention policy if grantees can delete from a folder so clients can warn
            // them when they try to delete something within the retention period
            canDelete = canAdminister || (perms != null && perms.indexOf(ACL.ABBR_DELETE) != -1);
        }

        if (canAdminister) {
            // return full ACLs for folders we have admin rights on
            if (needToOutput(fields, Change.ACL)) {
                if (fields != NOTIFY_FIELDS || folder.isTagged(Flag.FlagInfo.NO_INHERIT)) {
                    encodeACL(octxt, elem, folder.getEffectiveACL(), exposeAclAccessKey);
                }
            }
        }
        if (canDelete) {
            if (needToOutput(fields, Change.RETENTION_POLICY)) {
                RetentionPolicy rp = folder.getRetentionPolicy();
                if (fields != NOTIFY_FIELDS || rp.isSet()) {
                    // Only output retention policy if it's being modified, or if we're returning all
                    // folder data and policy is set.
                    encodeRetentionPolicy(elem, RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(folder.getAccount(), rp));
                }
            }
        }
        return elem;
    }

    private static Element encodeRetentionPolicy(Element parent, RetentionPolicy policy) {
        Element retPol = parent.addNonUniqueElement(MailConstants.E_RETENTION_POLICY);
        if (policy.isSet()) {
            Element keepEl = retPol.addNonUniqueElement(MailConstants.E_KEEP);
            encodePolicyList(keepEl, policy.getKeepPolicy());
            Element purgeEl = retPol.addNonUniqueElement(MailConstants.E_PURGE);
            encodePolicyList(purgeEl, policy.getPurgePolicy());
        }
        return retPol;
    }

    private static void encodePolicyList(Element parent, List<Policy> list) {
        if (list != null) {
            for (Policy p : list) {
                Element elem = parent.addNonUniqueElement(MailConstants.E_POLICY);
                elem.addAttribute(MailConstants.A_ID, p.getId());
                elem.addAttribute(MailConstants.A_NAME, p.getName());
                elem.addAttribute(MailConstants.A_LIFETIME, p.getLifetime());
                elem.addAttribute(MailConstants.A_RETENTION_POLICY_TYPE, p.getType().toString());
            }
        }
    }

    public static String encodeEffectivePermissions(Folder folder, OperationContext octxt) {
        try {
            short perms = folder.getMailbox().getEffectivePermissions(octxt, folder.getId(), MailItem.Type.FOLDER);
            return ACL.rightsToString(perms);
        } catch (ServiceException e) {
            LOG.warn("ignoring exception while fetching effective permissions for folder %d", folder.getId(), e);
            return null;
        }
    }

    // encode mailbox ACL
    public static Element encodeACL(OperationContext octxt, Element parent, ACL acl, boolean exposeAclAccessKey) {
        Element eACL = parent.addUniqueElement(MailConstants.E_ACL);
        if (acl == null) {
            return eACL;
        }

        if (acl.getInternalGrantExpiry() != 0) {
            eACL.addAttribute(MailConstants.A_INTERNAL_GRANT_EXPIRY, acl.getInternalGrantExpiry());
        }
        if (acl.getGuestGrantExpiry() != 0) {
            eACL.addAttribute(MailConstants.A_GUEST_GRANT_EXPIRY, acl.getGuestGrantExpiry());
        }

        boolean needDispName = OperationContextData.getNeedGranteeName(octxt);
        for (ACL.Grant grant : acl.getGrants()) {
            String name = null;
            byte granteeType = grant.getGranteeType();

            if (needDispName) {
                //
                // Get name of the grantee
                //

                // 1. try getting the name from the Grant, the name is set on the Grant
                //    if we are in the path of proxying sharing in ZD
                name = grant.getGranteeName();
                if (name == null) {
                    // 2. (for bug 35079), see if the name is already resolved in the in the OperationContextData
                    OperationContextData.GranteeNames granteeNames = OperationContextData.getGranteeNames(octxt);
                    if (granteeNames != null) {
                        name = granteeNames.getNameById(grant.getGranteeId(), granteeType);
                    }
                    // 3. fallback to the old way to lookup the name using the Provisioning interface,
                    //    this *may* lead to a LDAP search if the id is not in cache
                    if (name == null) {
                        NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(grant.getGranteeId(), granteeType);
                        if (nentry != null) {
                            name = nentry.getName();
                        }
                    }
                }
            }

            Element eGrant = eACL.addNonUniqueElement(MailConstants.E_GRANT);
            eGrant.addAttribute(MailConstants.A_ZIMBRA_ID, grant.getGranteeId())
                .addAttribute(MailConstants.A_GRANT_TYPE, ACL.typeToString(granteeType))
                .addAttribute(MailConstants.A_RIGHTS, ACL.rightsToString(grant.getGrantedRights()));
            if (grant.getExpiry() != 0) {
                eGrant.addAttribute(MailConstants.A_EXPIRY, grant.getExpiry());
            }

            if (needDispName) {
                // Note: use == instead of equals, the if returns true if and only if INVALID_GRANT and name
                // refer to the same object
                if (OperationContextData.GranteeNames.INVALID_GRANT == name) {
                    eGrant.addAttribute(MailConstants.A_INVALID, true);
                    eGrant.addAttribute(MailConstants.A_DISPLAY, OperationContextData.GranteeNames.EMPTY_NAME);
                } else {
                    eGrant.addAttribute(MailConstants.A_DISPLAY, name);
                }
            }

            if (granteeType == ACL.GRANTEE_KEY) {
                if (exposeAclAccessKey) {
                    eGrant.addAttribute(MailConstants.A_ACCESSKEY, grant.getPassword());
                }
            } else {
                eGrant.addAttribute(MailConstants.A_PASSWORD, grant.getPassword());
            }
        }
        return eACL;
    }

    /*
     * Delete this method in bug 66989
     */
    // encode account ACE
    public static Element encodeACE(Element parent, ZimbraACE ace) {
        Element eACE = parent.addNonUniqueElement(MailConstants.E_ACE)
                .addAttribute(MailConstants.A_ZIMBRA_ID, ace.getGrantee())
                .addAttribute(MailConstants.A_GRANT_TYPE, ace.getGranteeType().getCode())
                .addAttribute(MailConstants.A_RIGHT, ace.getRight().getName())
                .addAttribute(MailConstants.A_DISPLAY, ace.getGranteeDisplayName());

        if (ace.getGranteeType() == GranteeType.GT_KEY) {
            eACE.addAttribute(MailConstants.A_ACCESSKEY, ace.getSecret());
        } else if (ace.getGranteeType() == GranteeType.GT_GUEST) {
            eACE.addAttribute(MailConstants.A_PASSWORD, ace.getSecret());
        }
        if (ace.deny()) {
            eACE.addAttribute(MailConstants.A_DENY, ace.deny());
        }
        return eACE;
    }

    private static Element encodeFolderCommon(Element elem, ItemIdFormatter ifmt, Folder folder, int fields)
            throws ServiceException {
        int folderId = folder.getId();
        elem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(folder));
        elem.addAttribute(MailConstants.A_UUID, folder.getUuid());
        elem.addAttribute(MailConstants.A_DELETABLE, folder.isDeletable());

        if (folderId != Mailbox.ID_FOLDER_ROOT) {
            boolean encodedPath = false;
            if (needToOutput(fields, Change.NAME)) {
                String name = folder.getName();
                if (!Strings.isNullOrEmpty(name)) {
                    elem.addAttribute(MailConstants.A_NAME, name);
                }
                elem.addAttribute(MailConstants.A_ABS_FOLDER_PATH, folder.getPath());
                encodedPath = true;
            }
            if (needToOutput(fields, Change.FOLDER)) {
                elem.addAttribute(MailConstants.A_FOLDER, ifmt.formatItemId(new ItemId(folder.getMailbox(), folder.getFolderId())));
                elem.addAttribute(MailConstants.A_FOLDER_UUID, folder.getFolderUuid());
                if (!encodedPath) {
                    elem.addAttribute(MailConstants.A_ABS_FOLDER_PATH, folder.getPath());
                }
            }
        }

        if (needToOutput(fields, Change.FLAGS)) {
            String flags = folder.getMailbox().getItemFlagString(folder); //so that offline override can filter flags
            if (fields != NOTIFY_FIELDS || !flags.isEmpty()) {
                elem.addAttribute(MailConstants.A_FLAGS, flags);
            }
        }
        encodeColor(elem, folder, fields);
        if (needToOutput(fields, Change.UNREAD)) {
            int unread = folder.getUnreadCount();
            if (unread > 0 || fields != NOTIFY_FIELDS) {
                int deletedUnread = folder.getDeletedUnreadCount();
                elem.addAttribute(MailConstants.A_UNREAD, unread - deletedUnread);
                if (deletedUnread > 0) {
                    elem.addAttribute(MailConstants.A_IMAP_UNREAD, unread);
                }
            }
        }
        if (needToOutput(fields, Change.VIEW)) {
            MailItem.Type view = folder.getDefaultView();
            if (view != MailItem.Type.UNKNOWN) {
                elem.addAttribute(MailConstants.A_DEFAULT_VIEW, view.toString());
            }
        }
        if (needToOutput(fields, Change.CONTENT)) {
            elem.addAttribute(MailConstants.A_REVISION, folder.getSavedSequence());
            // this attribute ("ms") *normally* goes with MODIFIED_CONFLICT, but we need it
            //   serialized in this case as well in order to make dav ctag caching work
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            elem.addAttribute(MailConstants.A_CHANGE_DATE, folder.getChangeDate() / 1000);
            if (!needToOutput(fields, Change.CONTENT)) {  /* will have already added this */
                elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, folder.getModifiedSequence());
            }
            elem.addAttribute(MailConstants.A_METADATA_VERSION, folder.getMetadataVersion());
        }
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(elem, folder, fields);
            elem.addAttribute(MailConstants.A_WEB_OFFLINE_SYNC_DAYS, folder.getWebOfflineSyncDays());
        }
        if (needToOutput(fields, Change.DISABLE_ACTIVESYNC)) {
            elem.addAttribute(MailConstants.A_ACTIVESYNC_DISABLED, folder.isActiveSyncDisabled());
        }
        return elem;
    }

    public static Element encodeSearchFolder(Element parent, ItemIdFormatter ifmt, SearchFolder search)
            throws ServiceException {
        return encodeSearchFolder(parent, ifmt, search, NOTIFY_FIELDS);
    }

    public static Element encodeSearchFolder(Element parent, ItemIdFormatter ifmt, SearchFolder search, int fields)
            throws ServiceException {
        Element elem = parent.addNonUniqueElement(MailConstants.E_SEARCH);
        encodeFolderCommon(elem, ifmt, search, fields);
        if (needToOutput(fields, Change.QUERY)) {
            elem.addAttribute(MailConstants.A_QUERY, search.getQuery());
            elem.addAttribute(MailConstants.A_SORTBY, search.getSortField());
            elem.addAttribute(MailConstants.A_SEARCH_TYPES, search.getReturnTypes());
        }
        return elem;
    }

    public static Element encodeMountpoint(Element parent, ItemIdFormatter ifmt, OperationContext octx,
            Mountpoint mpt) throws ServiceException {
        return encodeMountpoint(parent, ifmt, octx, mpt, NOTIFY_FIELDS);
    }

    public static Element encodeMountpoint(Element parent, ItemIdFormatter ifmt, OperationContext octx,
            Mountpoint mpt, int fields) throws ServiceException {

        Element el = parent.addNonUniqueElement(MailConstants.E_MOUNT);
        encodeFolderCommon(el, ifmt, mpt, fields);
        if (needToOutput(fields, Change.CONTENT)) {
            el.addAttribute(MailConstants.A_ZIMBRA_ID, mpt.getOwnerId());
            el.addAttribute(MailConstants.A_REMOTE_ID, mpt.getRemoteId());
            el.addAttribute(MailConstants.A_REMOTE_UUID, mpt.getRemoteUuid());
            NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(mpt.getOwnerId(), ACL.GRANTEE_USER);
            el.addAttribute(MailConstants.A_OWNER_NAME, nentry == null ? null : nentry.getName());
            if (mpt.getDefaultView() != MailItem.Type.UNKNOWN) {
                el.addAttribute(MailConstants.A_DEFAULT_VIEW, mpt.getDefaultView().toString());
            }
        }
        if (needToOutput(fields, Change.SHAREDREM)) {
            el.addAttribute(MailConstants.A_REMINDER, false);  // feature backed out (bug 57437)
        }
        return el;
    }

    private static String getRestUrl(String ownerName, String folderPath) {
        if (ownerName == null || folderPath == null) {
            return null;
        }
        Provisioning prov = Provisioning.getInstance();
        Account targetAccount;
        try {
            targetAccount = prov.get(AccountBy.name, ownerName);
            if (targetAccount == null) {
                // Remote owner account has been deleted.
                return null;
            }
            Server targetServer = prov.getServer(targetAccount);
            Domain targetDomain = prov.getDomain(targetAccount);
            return URLUtil.getPublicURLForDomain(targetServer, targetDomain, UserServlet.SERVLET_PATH +
                    HttpUtil.urlEscape(UserServlet.getAccountPath(targetAccount) + folderPath) , true);
        } catch (ServiceException e) {
            ZimbraLog.soap.warn("unable to create rest url for mountpoint", e);
            return null;
        }
    }

    public static void transferMountpointContents(Element elem, Element mptTarget) {
        // transfer folder counts to the serialized mountpoint from the serialized target folder
        transferLongAttribute(elem, mptTarget, MailConstants.A_UNREAD);
        transferLongAttribute(elem, mptTarget, MailConstants.A_NUM);
        transferLongAttribute(elem, mptTarget, MailConstants.A_SIZE);
        elem.addAttribute(MailConstants.A_OWNER_FOLDER_NAME, mptTarget.getAttribute(MailConstants.A_NAME, null));
        String ownerName = elem.getAttribute(MailConstants.A_OWNER_NAME, null);
        String ownerFolderPath = mptTarget.getAttribute(MailConstants.A_ABS_FOLDER_PATH, null);
        // construct rest url based on owner name and folder name.
        elem.addAttribute(MailConstants.A_REST_URL, getRestUrl(ownerName, ownerFolderPath));
        elem.addAttribute(MailConstants.A_URL, mptTarget.getAttribute(MailConstants.A_URL, null));
        elem.addAttribute(MailConstants.A_RIGHTS, mptTarget.getAttribute(MailConstants.A_RIGHTS, null));
        if (mptTarget.getAttribute(MailConstants.A_FLAGS, "").indexOf("u") != -1) {
            elem.addAttribute(MailConstants.A_FLAGS, "u" + elem.getAttribute(MailConstants.A_FLAGS, "").replace("u", ""));
        }
        // transfer ACL and child folders to the serialized mountpoint from the serialized remote folder
        for (Element child : mptTarget.listElements()) {
            String name = child.getName();
            // See ZimbraSoap JAXB classes Mountpoint / Folder etc to determine which elements are unique/non-unique
            if (name.equals(MailConstants.E_FOLDER) || name.equals(MailConstants.E_SEARCH) ||
                    name.equals(MailConstants.E_MOUNT) || name.equals(MailConstants.E_RETENTION_POLICY) ||
                    name.equals(MailConstants.E_METADATA)) {
                elem.addNonUniqueElement(child.clone());
            } else {
                elem.addUniqueElement(child.clone());
            }
        }
    }

    private static void transferLongAttribute(Element to, Element from, String attrName) {
        try {
            long remote = from.getAttributeLong(attrName, -1L);
            if (remote >= 0) {
                to.addAttribute(attrName, remote);
            }
        } catch (ServiceException e) {
            ZimbraLog.session.warn("exception reading long attr from remote folder: %s", attrName, e);
        } catch (Element.ContainerException e) {
            ZimbraLog.session.warn("exception adding remote folder attr to serialized mountpoint: %s", attrName, e);
        }
    }

    public static Element encodeRestUrl(Element el, MailItem item) {
        try {
            return el.addAttribute(MailConstants.A_REST_URL, UserServlet.getRestUrl(item));
        } catch (ServiceException e) {
            LOG.error("cannot generate REST url", e);
            return el;
        }
    }

    public static void recordItemTags(Element elem, MailItem item, OperationContext octxt) throws ServiceException {
        recordItemTags(elem, item, octxt, NOTIFY_FIELDS);
    }

    public static void recordItemTags(Element elem, MailItem item, OperationContext octxt, int fields) throws ServiceException {
        if (needToOutput(fields, Change.FLAGS | Change.UNREAD)) {
            String flags = item.getFlagString();
            if (fields != NOTIFY_FIELDS || !flags.isEmpty()) {
                elem.addAttribute(MailConstants.A_FLAGS, flags);
            }
        }
        if (needToOutput(fields, Change.TAGS)) {
            String[] tags = item.getTags();
            if (!ArrayUtil.isEmpty(tags) || fields != NOTIFY_FIELDS) {
                elem.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(tags));
                if (hasFullAccess(item.getMailbox(), octxt)) {
                    elem.addAttribute(MailConstants.A_TAGS, TagUtil.getTagIdString(item));
                }
            }
        }
    }

    public static void encodeAllCustomMetadata(Element elem, MailItem item, int fields) {
        List<String> sections = item.getCustomDataSections();
        if (sections == null || sections.isEmpty()) {
            // if we care specifically about custom metadata and there is none, let the caller know
            if (fields != NOTIFY_FIELDS) {
                elem.addNonUniqueElement(MailConstants.E_METADATA);
            }
            return;
        }

        for (String section : sections) {
            try {
                encodeCustomMetadata(elem, item.getCustomData(section));
            } catch (ServiceException e) {
                ZimbraLog.soap.warn("could not deserialize custom metadata; skipping (section '%s', item %s)",
                        section, new ItemId(item));
            }
        }
    }

    public static Element encodeCustomMetadata(Element elem, CustomMetadata custom) {
        if (custom == null) {
            return null;
        }
        Element serialized = elem.addNonUniqueElement(MailConstants.E_METADATA);
        serialized.addAttribute(MailConstants.A_SECTION, custom.getSectionKey());
        for (Map.Entry<String, String> entry : custom.entrySet()) {
            serialized.addKeyValuePair(entry.getKey(), entry.getValue());
        }
        return serialized;
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Contact contact,
            boolean summary, Collection<String> attrFilter)
    throws ServiceException {
        return encodeContact(parent, ifmt, octxt, contact,
                (ContactGroup)null, (Collection<String>)null /* memberAttrFilter */, summary,
                attrFilter, NOTIFY_FIELDS, null, false /* returnHiddenAttrs */,
                GetContacts.NO_LIMIT_MAX_MEMBERS, true /* returnCertInfo */);
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Contact contact,
            boolean summary, Collection<String> attrFilter, int fields)
    throws ServiceException {
        return encodeContact(parent, ifmt, octxt, contact,
                (ContactGroup)null, (Collection<String>)null /* memberAttrFilter */, summary,
                attrFilter, fields, (String)null /* migratedDlist */, false /* returnHiddenAttrs */,
                GetContacts.NO_LIMIT_MAX_MEMBERS, true /* returnCertInfo */);
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Contact contact,
            ContactGroup contactGroup, Collection<String> memberAttrFilter, boolean summary,
            Collection<String> attrFilter, int fields, String migratedDlist,
            boolean returnHiddenAttrs, long maxMembers, boolean returnCertInfo)
    throws ServiceException {
        return encodeContact(parent, ifmt, octxt, contact, contactGroup, memberAttrFilter, summary, attrFilter,
                fields, migratedDlist, returnHiddenAttrs, maxMembers, returnCertInfo, (Set<String>) null);
    }

    public static Element encodeContact(Element parent, ItemIdFormatter ifmt,
        OperationContext octxt, Contact contact, ContactGroup contactGroup,
        Collection<String> memberAttrFilter, boolean summary, Collection<String> attrFilter,
        int fields, String migratedDlist, boolean returnHiddenAttrs, long maxMembers,
        boolean returnCertInfo, Set<String> memberOfIds) throws ServiceException {
        return encodeContact(parent, ifmt, octxt, contact, contactGroup, memberAttrFilter, summary,
            attrFilter, fields, migratedDlist, returnHiddenAttrs, maxMembers, returnCertInfo,
            memberOfIds, null, null);
    }

    /**
     * @param memberOfIds set of IDs of contact groups this contact is a member of
     * @param mRequestedAcct requested account
     * @param mAuthedAcct authenticated account
     */
    public static Element encodeContact(Element parent, ItemIdFormatter ifmt,
        OperationContext octxt, Contact contact, ContactGroup contactGroup,
        Collection<String> memberAttrFilter, boolean summary, Collection<String> attrFilter,
        int fields, String migratedDlist, boolean returnHiddenAttrs, long maxMembers,
        boolean returnCertInfo, Set<String> memberOfIds, Account mRequestedAcct,
        Account mAuthedAcct)
    throws ServiceException {
        Element el = parent.addNonUniqueElement(MailConstants.E_CONTACT);
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(contact));
        if (needToOutput(fields, Change.FOLDER)) {
            el.addAttribute(MailConstants.A_FOLDER,
                ifmt.formatItemId(new ItemId(contact.getMailbox().getAccountId(), contact.getFolderId())));
        }
        recordItemTags(el, contact, octxt, fields);
        if (needToOutput(fields, Change.CONFLICT)) {
            el.addAttribute(MailConstants.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            el.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, contact.getModifiedSequence());
            el.addAttribute(MailConstants.A_DATE, contact.getDate());
            el.addAttribute(MailConstants.A_REVISION, contact.getSavedSequence());
        } else {
            if (needToOutput(fields, Change.CONTENT)) {
                el.addAttribute(MailConstants.A_DATE, contact.getDate());
                el.addAttribute(MailConstants.A_REVISION, contact.getSavedSequence());
            }
            if (needToOutput(fields, Change.MODSEQ)) {
                el.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, contact.getModifiedSequence());
            }
        }
        if (needToOutput(fields, Change.IMAP_UID)) {
            el.addAttribute(MailConstants.A_IMAP_UID, contact.getImapUid());
        }
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(el, contact, fields);
        }
        if (!needToOutput(fields, Change.CONTENT)) {
            if (summary) {
                try {
                    el.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
                } catch (ServiceException e) {
                }
                el.addAttribute(ContactConstants.A_email, contact.get(ContactConstants.A_email));
                el.addAttribute(ContactConstants.A_email2, contact.get(ContactConstants.A_email2));
                el.addAttribute(ContactConstants.A_email3, contact.get(ContactConstants.A_email3));

                String type = contact.get(ContactConstants.A_type);
                el.addAttribute(ContactConstants.A_type, type);

                // send back date with summary via search results
                el.addAttribute(MailConstants.A_CHANGE_DATE, contact.getChangeDate() / 1000);
            }

            // stop here if we're not returning the actual contact content
            return el;
        }
        try {
            el.addAttribute(MailConstants.A_FILE_AS_STR, contact.getFileAsString());
        } catch (ServiceException e) {
        }

        List<Attachment> attachments = contact.getAttachments();

        // encode contact group members (not derefed) if we don't have a
        // already derefed contactGroup, and we don't have a migrated dlist
        boolean encodeContactGroupMembersBasic = (contactGroup == null) && (migratedDlist == null);

        if (attrFilter != null) {
            for (String name : attrFilter) {
                // XXX: How to distinguish between a non-existent attribute and
                //      an existing attribute with null or empty string value?
                String value = contact.get(name);
                if (!Strings.isNullOrEmpty(value)) {
                    encodeContactAttr(el, name, value, contact, encodeContactGroupMembersBasic, octxt, returnCertInfo);
                } else if (attachments != null) {
                    for (Attachment attach : attachments) {
                        if (attach.getName().equals(name)) {
                            encodeContactAttachment(el, attach);
                        }
                    }
                }
            }
        } else {
            Map<String, String> contactFields = returnHiddenAttrs ?
                    contact.getAllFields() : contact.getFields();
            for (Map.Entry<String, String> me : contactFields.entrySet()) {
                String name = me.getKey();
                String value = me.getValue();
                // don't put returnHiddenAttrs in one of the && condition because member
                // can be configured as a hidden or non-hidden field
                if (ContactConstants.A_member.equals(name)) {
                    String email = contactFields.get("email");
                    if (!hasDLViewRight(email, mRequestedAcct, mAuthedAcct)) {
                        continue;
                    }
                    if (maxMembers != GetContacts.NO_LIMIT_MAX_MEMBERS) {
                        // there is a limit on the max number of members to return
                        ContactDLMembers dlMembers = new ContactDLMembers(contact);
                        if (dlMembers.getTotal() > maxMembers) {
                            el.addAttribute(MailConstants.A_TOO_MANY_MEMBERS, true);
                            continue;
                        }
                    }
                }
                if (name != null && !name.trim().isEmpty() && !Strings.isNullOrEmpty(value)) {
                    encodeContactAttr(el, name, value, contact, encodeContactGroupMembersBasic, octxt, returnCertInfo);
                }
            }
            if (attachments != null) {
                for (Attachment attach : attachments) {
                    encodeContactAttachment(el, attach);
                }
            }
        }

        if (migratedDlist != null) {
            encodeContactAttr(el, ContactConstants.A_dlist, migratedDlist, contact, false, octxt, returnCertInfo);
        } else if (contactGroup != null) {
            encodeContactGroup(el, contactGroup, memberAttrFilter, ifmt, octxt, summary, fields);
        }

        if (memberOfIds != null && !memberOfIds.isEmpty()) {
            List<String> memIds = Lists.newArrayListWithExpectedSize(memberOfIds.size());
            String authAcctId = null;
            if ((octxt != null) && (octxt.getmAuthTokenAccountId() != null)) {
                authAcctId = octxt.getmAuthTokenAccountId();
            }
            for (String memberOfId : memberOfIds) {
                ItemIdentifier ident = ItemIdentifier.fromOwnerAndRemoteId(authAcctId, memberOfId);
                memIds.add(ident.toString(authAcctId));
            }
            el.addAttribute(MailConstants.E_CONTACT_MEMBER_OF, Joiner.on(',').join(memIds),
                    Element.Disposition.CONTENT);
        }
        return el;
    }

    public static boolean hasDLViewRight(String email, Account mRequestedAcct,
        Account mAuthedAcct) {
        boolean canExpand = true;
        if (mRequestedAcct != null && mAuthedAcct != null) {
            GalGroup.GroupInfo groupInfo = GalGroupInfoProvider.getInstance().getGroupInfo(email,
                true, mRequestedAcct, mAuthedAcct);
            if (groupInfo != null) {
                canExpand = (GalGroup.GroupInfo.CAN_EXPAND == groupInfo);
            }
        }
        return canExpand;
    }

    private static void encodeContactAttr(Element elem, String name, String value,
            Contact contact, boolean encodeContactGroupMembers, OperationContext octxt, boolean returnCertInfo) {
        if (Contact.isMultiValueAttr(value)) {
            try {
                for (String v : Contact.parseMultiValueAttr(value)) {
                    if (Contact.isUrlField(name)) {
                        v = DefangFilter.sanitize(v, true);
                    } else if (Contact.isSMIMECertField(name)) {
                        encodeCertificate(octxt, elem, name, v, contact.getEmailAddresses(), returnCertInfo);
                        continue;
                    }
                    elem.addKeyValuePair(name, v);
                }
                return;
            } catch (JSONException e) {}
        } else if (ContactConstants.A_groupMember.equals(name)) {
            if (encodeContactGroupMembers) {
                try {
                    ContactGroup contactGroup = ContactGroup.init(contact, false);
                    if (contactGroup != null) {
                        for (ContactGroup.Member member : contactGroup.getMembers(true)) {
                            Element eMember = elem.addNonUniqueElement(MailConstants.E_CONTACT_GROUP_MEMBER);
                            encodeContactGroupMemberBasic(eMember, member);
                        }
                    }
                } catch (ServiceException e) {
                    ZimbraLog.contact.warn("unable to init contact group", e);
                }
            }
        } else {
            if (Contact.isUrlField(name)) {
                value = DefangFilter.sanitize(value, true);
            } else if (Contact.isSMIMECertField(name)) {
                encodeCertificate(octxt, elem, name, value, contact.getEmailAddresses(), returnCertInfo);
                return;
            }
            elem.addKeyValuePair(name, value);
        }
    }

    private static void encodeCertificate(OperationContext octxt, Element elem, String name, String value, List<String> emailAddresses, boolean returnCertInfo) {
        Account account = octxt.getAuthenticatedUser();
        elem.addKeyValuePair(name, value);
        if (SmimeHandler.getHandler() != null && returnCertInfo) {
            SmimeHandler.getHandler().encodeCertificate(account, elem, value, octxt.getmResponseProtocol(), emailAddresses);
        }
    }

    private static void encodeContactGroupMemberBasic(Element eMember, ContactGroup.Member member) {
        eMember.addAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_TYPE, member.getType().getSoapEncoded());
        eMember.addAttribute(MailConstants.A_CONTACT_GROUP_MEMBER_VALUE, member.getValue());
    }

    private static void encodeContactGroup(Element elem, ContactGroup contactGroup,
            Collection<String> memberAttrFilter, ItemIdFormatter ifmt,
            OperationContext octxt, boolean summary, int fields)
    throws ServiceException {
        for (ContactGroup.Member member : contactGroup.getMembers(true)) {
            Element eMember = elem.addNonUniqueElement(MailConstants.E_CONTACT_GROUP_MEMBER);
            encodeContactGroupMemberBasic(eMember, member);
            Object derefedMember = member.getDerefedObj();
            if (derefedMember != null) {
                if (derefedMember instanceof String) {
                    // inline member, do nothing
                } else if (derefedMember instanceof Contact) {
                    // only expand one level for now.
                    // If this member is a group, do not create/pass down a ContactGroup object from the member.
                    encodeContact(eMember, ifmt, octxt, (Contact) derefedMember, summary, memberAttrFilter, fields);
                } else if (derefedMember instanceof GalContact) {
                    encodeGalContact(eMember, (GalContact) derefedMember, memberAttrFilter);
                } else if (derefedMember instanceof Element) {
                    // proxied GAL or Contact entry
                    Element eContact = (Element) derefedMember;
                    if (memberAttrFilter != null) {
                        for (Element eAttr : eContact.listElements()) {
                            String name = eAttr.getAttribute(MailConstants.A_ATTRIBUTE_NAME, null);
                            if (!memberAttrFilter.contains(name)) {
                                eAttr.detach();
                            }
                        }
                    }
                    eMember.addNonUniqueElement(eContact);
                }
            }
        }
    }

    private static void encodeContactAttachment(Element elem, Attachment attach) {
        Element.KeyValuePair kvp = elem.addKeyValuePair(attach.getName(), null);
        kvp.addAttribute(MailConstants.A_PART,
                attach.getPartName()).addAttribute(MailConstants.A_CONTENT_TYPE, attach.getContentType());
        kvp.addAttribute(MailConstants.A_SIZE,
                attach.getSize()).addAttribute(MailConstants.A_CONTENT_FILENAME, attach.getFilename());
    }

    public static Element encodeNote(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Note note) throws ServiceException {
        return encodeNote(parent, ifmt, octxt, note, NOTIFY_FIELDS);
    }

    public static Element encodeNote(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Note note, int fields) throws ServiceException {
        Element el = parent.addNonUniqueElement(MailConstants.E_NOTE);
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(note));
        if (needToOutput(fields, Change.CONTENT) && note.getSavedSequence() != 0) {
            el.addAttribute(MailConstants.A_REVISION, note.getSavedSequence());
        }
        if (needToOutput(fields, Change.FOLDER)) {
            el.addAttribute(MailConstants.A_FOLDER,
                    ifmt.formatItemId(new ItemId(note.getMailbox().getAccountId(), note.getFolderId())));
        }
        if (needToOutput(fields, Change.DATE)) {
            el.addAttribute(MailConstants.A_DATE, note.getDate());
        }
        recordItemTags(el, note, octxt, fields);
        if (needToOutput(fields, Change.POSITION)) {
            el.addAttribute(MailConstants.A_BOUNDS, note.getBounds().toString());
        }
        encodeColor(el, note, fields);
        if (needToOutput(fields, Change.CONTENT)) {
            el.addAttribute(MailConstants.E_CONTENT, note.getText(), Element.Disposition.CONTENT);
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            el.addAttribute(MailConstants.A_CHANGE_DATE, note.getChangeDate() / 1000);
            el.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, note.getModifiedSequence());
        }
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(el, note, fields);
        }
        return el;
    }

    public static Element encodeTag(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Tag tag)
    throws ServiceException {
        return encodeTag(parent, ifmt, octxt, tag, NOTIFY_FIELDS);
    }

    public static Element encodeSmartFolder(Element parent, ItemIdFormatter ifmt, OperationContext octxt, SmartFolder smartFolder, int fields)
    throws ServiceException {
        Element el = parent.addNonUniqueElement(MailConstants.E_SMART_FOLDER);
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(smartFolder));
        el.addAttribute(MailConstants.A_NAME, smartFolder.getName());
        if (needToOutput(fields, Change.UNREAD)) {
            int unreadCount = smartFolder.getUnreadCount();
            if (unreadCount > 0 || fields != NOTIFY_FIELDS) {
                el.addAttribute(MailConstants.A_UNREAD, unreadCount);
            }
        }
        if (needToOutput(fields, Change.SIZE)) {
            long num = smartFolder.getItemCount();
            if (num > 0 || fields != NOTIFY_FIELDS) {
                el.addAttribute(MailConstants.A_NUM, num);
            }
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            el.addAttribute(MailConstants.A_DATE, smartFolder.getDate());
            el.addAttribute(MailConstants.A_REVISION, smartFolder.getSavedSequence());
            el.addAttribute(MailConstants.A_CHANGE_DATE, smartFolder.getChangeDate() / 1000);
            el.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, smartFolder.getModifiedSequence());
        }
        boolean remote = octxt != null && octxt.isDelegatedRequest(smartFolder.getMailbox());
        boolean canAdminister = !remote;
        if (canAdminister) {
            if (needToOutput(fields, Change.RETENTION_POLICY)) {
                RetentionPolicy rp = smartFolder.getRetentionPolicy();
                if (fields != NOTIFY_FIELDS || rp.isSet()) {
                    // Only output retention policy if it's being modified, or if we're returning all
                    // folder data and policy is set.
                    encodeRetentionPolicy(el, RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(smartFolder.getAccount(), rp));
                }
            }
        }
        return el;
    }

    public static Element encodeTag(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Tag tag, int fields)
    throws ServiceException {
        Element el = parent.addNonUniqueElement(MailConstants.E_TAG);
        // FIXME: eventually remove tag ID from serialization
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(tag));
        // always encode the name now that we're switching away from IDs
        el.addAttribute(MailConstants.A_NAME, tag.getName());
        encodeColor(el, tag, fields);
        if (needToOutput(fields, Change.UNREAD)) {
            int unreadCount = tag.getUnreadCount();
            if (unreadCount > 0 || fields != NOTIFY_FIELDS) {
                el.addAttribute(MailConstants.A_UNREAD, unreadCount);
            }
        }
        if (needToOutput(fields, Change.SIZE)) {
            long num = tag.getItemCount();
            if (num > 0 || fields != NOTIFY_FIELDS) {
                el.addAttribute(MailConstants.A_NUM, num);
            }
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            el.addAttribute(MailConstants.A_DATE, tag.getDate());
            el.addAttribute(MailConstants.A_REVISION, tag.getSavedSequence());
            el.addAttribute(MailConstants.A_CHANGE_DATE, tag.getChangeDate() / 1000);
            el.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, tag.getModifiedSequence());
        }
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(el, tag, fields);
        }
        boolean remote = octxt != null && octxt.isDelegatedRequest(tag.getMailbox());
        boolean canAdminister = !remote;
        if (canAdminister) {
            if (needToOutput(fields, Change.RETENTION_POLICY)) {
                RetentionPolicy rp = tag.getRetentionPolicy();
                if (fields != NOTIFY_FIELDS || rp.isSet()) {
                    // Only output retention policy if it's being modified, or if we're returning all
                    // folder data and policy is set.
                    encodeRetentionPolicy(el, RetentionPolicyManager.getInstance().getCompleteRetentionPolicy(tag.getAccount(), rp));
                }
            }
        }
        return el;
    }

    public static Element encodeColor(Element el, MailItem item, int fields) {
        if (needToOutput(fields, Change.COLOR)) {
            Color color = item.getRgbColor();
            if (color.hasMapping()) {
                byte mappedColor = color.getMappedColor();
                if (mappedColor != MailItem.DEFAULT_COLOR || fields != NOTIFY_FIELDS) {
                    el.addAttribute(MailConstants.A_COLOR, mappedColor);
                }
            } else {
                el.addAttribute(MailConstants.A_RGB, color.toString());
            }
        }
        return el;
    }

    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Conversation conv, SearchParams params) throws ServiceException {
        Mailbox mbox = conv.getMailbox();
        List<Message> msgs = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_ASC, -1);
        return encodeConversation(parent, ifmt, octxt, conv, msgs, params);
    }

    public static Element encodeConversation(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Conversation conv, List<Message> msgs, SearchParams params) throws ServiceException {
        int fields = NOTIFY_FIELDS;
        Element c = encodeConversationSummary(parent, ifmt, octxt, conv, msgs, null, OutputParticipants.PUT_BOTH, fields, true);
        if (msgs.isEmpty()) {
            return c;
        }
        c.addAttribute(MailConstants.E_SUBJECT, msgs.get(0).getSubject(), Element.Disposition.CONTENT);

        ExpandResults expand = params.getInlineRule();
        for (Message msg : msgs) {
            if (msg.isTagged(Flag.FlagInfo.DELETED)) {
                continue;
            }
            if (expand == ExpandResults.FIRST || expand == ExpandResults.ALL || expand.matches(msg)) {
                encodeMessageAsMP(c, ifmt, octxt, msg, null, params.getMaxInlinedLength(), params.getWantHtml(),
                        params.getNeuterImages(), params.getInlinedHeaders(), true, params.getWantExpandGroupInfo(),
                        LC.mime_encode_missing_blob.booleanValue(), params.getWantContent(), NOTIFY_FIELDS);
                if (expand == ExpandResults.FIRST) {
                    expand = ExpandResults.NONE;
                }
            } else {
                Element m = c.addNonUniqueElement(MailConstants.E_MSG);
                m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
                m.addAttribute(MailConstants.A_DATE, msg.getDate());
                m.addAttribute(MailConstants.A_SIZE, msg.getSize());
                m.addAttribute(MailConstants.A_SUBJECT, msg.getSubject(), Element.Disposition.CONTENT);;
                m.addAttribute(MailConstants.A_FOLDER,
                    ifmt.formatItemId(new ItemId(msg.getMailbox().getAccountId(), msg.getFolderId())));
                recordItemTags(m, msg, octxt, fields);
                m.addAttribute(MailConstants.E_FRAG, msg.getFragment(), Element.Disposition.CONTENT);
                encodeEmail(m, msg.getSender(), EmailType.FROM);
            }
        }
        return c;
    }

    private static class ToRecipsList {
        private static final int MAX_COUNT = 8;

        private final List<ParsedAddress> aggregatedRecipients = Lists.newArrayListWithCapacity(MAX_COUNT);
        private boolean mIsElided = false;

        public ToRecipsList()  {}

        public ToRecipsList add(Message msg) {
            InternetAddress toRecips[] = Mime.parseAddressHeader(msg.getRecipients());
            String sender = msg.getSender();
            if (sender == null || sender.trim().equals("")) {
                return this;
            }

            for (InternetAddress ia : toRecips) {
                ParsedAddress pa = new ParsedAddress(ia).parse();
                aggregatedRecipients.remove(pa);
                aggregatedRecipients.add(0, pa);
            }
            while (aggregatedRecipients.size() > MAX_COUNT) {
                aggregatedRecipients.remove(MAX_COUNT);
                mIsElided = true;
            }
            return this;
        }

        public boolean isElided()  { return mIsElided; }

        public List<ParsedAddress> getLastAddresses() {
            if (aggregatedRecipients == null || aggregatedRecipients.isEmpty())
                return Collections.emptyList();
            List<ParsedAddress> addrs = Lists.newArrayList(aggregatedRecipients);
            if (addrs.size() > 1) {
                Collections.reverse(addrs);
            }
            return addrs;
        }
    }

    /**
     * This version lets you specify the Date and Fragment -- we use this when sending Query Results back to the client,
     * the conversation date returned and fragment correspond to those of the matched message.
     */
    public static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, int fields)
    throws ServiceException {
        return encodeConversationSummary(parent, ifmt, octxt, conv, null, OutputParticipants.PUT_SENDERS, fields, true);
    }

    public static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, Message msgHit,
            OutputParticipants output)
    throws ServiceException {
        return encodeConversationSummary(parent, ifmt, octxt, conv, msgHit, output, NOTIFY_FIELDS, true);
    }

    private static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Conversation conv, Message msgHit, OutputParticipants output, int fields, boolean alwaysSerialize)
            throws ServiceException {
        List<Message> msgs = null;
        Mailbox mbox = conv.getMailbox();
        // if the caller might not be able to see all the messages (due to rights or \Deleted),
        //   need to calculate some fields from the visible messages
        boolean isDelegatedNonAccessible = false;
        if (octxt != null && octxt.isDelegatedRequest(mbox)) {
            isDelegatedNonAccessible = !AccessManager.getInstance().canAccessAccount(octxt.getAuthenticatedUser(),
                    conv.getAccount(), octxt.isUsingAdminPrivileges());
        }
        if (isDelegatedNonAccessible || conv.isTagged(Flag.FlagInfo.DELETED)) {
            msgs = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_ASC, -1);
        }
        return encodeConversationSummary(parent, ifmt, octxt, conv, msgs, msgHit, output, fields, alwaysSerialize);
    }

    private static Element encodeConversationSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Conversation conv, List<Message> msgs, Message msgHit, OutputParticipants output, int fields, boolean alwaysSerialize)
            throws ServiceException {
        boolean addFirstHitRecips  = msgHit != null && (output == OutputParticipants.PUT_RECIPIENTS);
        boolean addAggregatedRecips  = !addFirstHitRecips && (output == OutputParticipants.PUT_BOTH);
        boolean addSenders = (output == OutputParticipants.PUT_BOTH || output == OutputParticipants.PUT_SENDERS)
                                && needToOutput(fields, Change.SENDERS);

        Mailbox mbox = conv.getMailbox();

        boolean noneVisible = msgs != null && msgs.isEmpty();
        Element c = noneVisible && !alwaysSerialize ? null : encodeConversationCommon(parent, ifmt, octxt, conv, msgs, fields);
        if (noneVisible || c == null) {
            return c;
        }
        if (needToOutput(fields, Change.DATE)) {
            c.addAttribute(MailConstants.A_DATE, msgHit != null ? msgHit.getDate() : conv.getDate());
        }
        if (needToOutput(fields, Change.SUBJECT)) {
            c.addAttribute(MailConstants.E_SUBJECT, msgHit != null ? msgHit.getSubject() : conv.getSubject(), Element.Disposition.CONTENT);
        }
        if (needToOutput(fields, Change.FLAGS | Change.UNREAD)) {
            c.addAttribute(MailConstants.A_UNREAD, conv.getUnreadCount());
        }
        List<Message> msgsByConv = null;
        if (fields == NOTIFY_FIELDS && msgHit != null) {
            /*
             * bug: 75104
             * we need to encode fragment of the first message in the conv instead of the first hit
             */
            if (msgHit.inTrash() || msgHit.inSpam()) {
                msgsByConv = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_DESC, 1);
            } else {
                msgsByConv = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_DESC, -1, true);
            }
            c.addAttribute(MailConstants.E_FRAG, msgsByConv.isEmpty() == false ? msgsByConv.get(0).getFragment() : msgHit.getFragment(), Element.Disposition.CONTENT);
        }
        if (addFirstHitRecips && msgHit != null) {
            addEmails(c, Mime.parseAddressHeader(msgHit.getRecipients()), EmailType.TO);
        }
        boolean elided = false;
        if (addSenders) {
            SenderList sl;
            try {
                if (msgs != null) {
                    sl = new SenderList();
                    for (Message msg : msgs) {
                        if (!msg.isTagged(Flag.FlagInfo.DELETED)) {
                            sl.add(msg);
                        }
                    }
                } else {
                    sl = mbox.getConversationSenderList(conv.getId());
                }
            } catch (SenderList.RefreshException slre) {
                ZimbraLog.soap.warn("out-of-order messages returned for conversation " + conv.getId());
                return c;
            } catch (ServiceException e) {
                return c;
            }

            elided = elided || sl.isElided();
            for (ParsedAddress pa : sl.getLastAddresses()) {
                encodeEmail(c, pa, EmailType.FROM);
            }
        }
        if (addAggregatedRecips) {
            ToRecipsList aggregatedToRecips;
            aggregatedToRecips = new ToRecipsList();
            if (msgs != null) {
                for (Message msg : msgs) {
                    if (!msg.isTagged(Flag.FlagInfo.DELETED)) {
                        aggregatedToRecips.add(msg);
                    }
                }
            } else {
                if (msgsByConv == null) {
                    msgsByConv = mbox.getMessagesByConversation(octxt, conv.getId(), SortBy.DATE_DESC, -1, true);
                }
                if (msgsByConv != null) {
                    for (Message msg : msgsByConv) {
                        if (!msg.isTagged(Flag.FlagInfo.DELETED)) {
                            aggregatedToRecips.add(msg);
                        }
                    }
                }
            }
            elided = elided || aggregatedToRecips.isElided();
            for (ParsedAddress pa : aggregatedToRecips.getLastAddresses()) {
                encodeEmail(c, pa, EmailType.TO);
            }
        }
        if (elided) {
            c.addAttribute(MailConstants.A_ELIDED, true);
        }

        if (needToOutput(fields, Change.CONFLICT)) {
            c.addAttribute(MailConstants.A_CHANGE_DATE, conv.getChangeDate() / 1000);
            c.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, conv.getModifiedSequence());
        }

        return c;
    }

    private static Element encodeConversationCommon(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Conversation conv, List<Message> msgs, int fields)
    throws ServiceException {
        Element c = parent.addNonUniqueElement(MailConstants.E_CONV);
        c.addAttribute(MailConstants.A_ID, ifmt.formatItemId(conv));

        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(c, conv, fields);
        }
        if (needToOutput(fields, Change.CHILDREN | Change.SIZE)) {
            int count = conv.getMessageCount(), nondeleted = count;
            if (msgs != null) {
                count = nondeleted = msgs.size();
                for (Message msg : msgs) {
                    if (msg.isTagged(Flag.FlagInfo.DELETED)) {
                        nondeleted--;
                    }
                }
            }
            c.addAttribute(MailConstants.A_UNREAD, conv.getUnreadCount());
            c.addAttribute(MailConstants.A_NUM, nondeleted);
            if (count != nondeleted) {
                c.addAttribute(MailConstants.A_TOTAL_SIZE, count);
            }
        }

        if (msgs == null) {
            recordItemTags(c, conv, octxt, fields);
        } else if (needToOutput(fields, Change.FLAGS | Change.UNREAD | Change.TAGS)) {
            int flags = 0;
            Set<String> tags = Sets.newHashSet();
            Set<Integer> tagIds = Sets.newTreeSet();
            for (Message msg : msgs) {
                if (!msg.isTagged(Flag.FlagInfo.DELETED)) {
                    flags |= msg.getFlagBitmask();
                    tags.addAll(Arrays.asList(msg.getTags()));
                    tagIds.addAll(TagUtil.getTagIds(msg));
                }
            }
            if (needToOutput(fields, Change.FLAGS | Change.UNREAD)) {
                if (fields != NOTIFY_FIELDS || flags != 0) {
                    c.addAttribute(MailConstants.A_FLAGS, Flag.toString(flags));
                }
            }
            if (needToOutput(fields, Change.TAGS)) {
                if (fields != NOTIFY_FIELDS || !tags.isEmpty()) {
                    c.addAttribute(MailConstants.A_TAG_NAMES, TagUtil.encodeTags(tags));
                    if (hasFullAccess(conv.getMailbox(), octxt)) {
                        c.addAttribute(MailConstants.A_TAGS, Joiner.on(',').join(tagIds));
                    }
                }
            }
        }

        return c;
    }

    public static Element encodeMessageAsMP(Element parent, ItemIdFormatter ifmt,
            OperationContext octxt, Message msg, String part, int maxSize, boolean wantHTML,
            boolean neuter, Set<String> headers, boolean serializeType, boolean wantExpandGroupInfo,
            boolean encodeMissingBlobs)
    throws ServiceException {
        return encodeMessageAsMP(parent, ifmt, octxt, msg, part, maxSize, wantHTML, neuter,
            headers, serializeType, wantExpandGroupInfo, encodeMissingBlobs, MsgContent.full, NOTIFY_FIELDS);
    }

    public static Element encodeMessageAsMP(Element parent, ItemIdFormatter ifmt,
            OperationContext octxt, Message msg, String part, int maxSize, boolean wantHTML,
            boolean neuter, Set<String> headers, boolean serializeType, boolean wantExpandGroupInfo,
            boolean encodeMissingBlobs, MsgContent wantContent)
    throws ServiceException {
        return encodeMessageAsMP(parent, ifmt, octxt, msg, part, maxSize, wantHTML, neuter,
            headers, serializeType, wantExpandGroupInfo, encodeMissingBlobs, wantContent, NOTIFY_FIELDS);
    }

    /** Encodes a Message object into <m> element with <mp> elements for
     *  message body.
     * @param parent  The Element to add the new <tt>&lt;m></tt> to.
     * @param ifmt    The formatter to sue when serializing item ids.
     * @param msg     The Message to serialize.
     * @param part    If non-null, serialize this message/rfc822 subpart of
     *                the Message instead of the Message itself.
     * @param maxSize TODO
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @param fields  Bitmask of fields to output in the response.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    public static Element encodeMessageAsMP(Element parent, ItemIdFormatter ifmt,
            OperationContext octxt, Message msg, String part, int maxSize, boolean wantHTML,
            boolean neuter, Set<String> headers, boolean serializeType, boolean wantExpandGroupInfo,
            boolean encodeMissingBlobs, MsgContent wantContent, int fields)
    throws ServiceException {
        Mailbox mbox = msg.getMailbox();
        int changeId = msg.getSavedSequence();
        while (true) {
            try {
                return encodeMessageAsMPHelper(false /* bestEffort */, parent, ifmt, octxt, msg, part, maxSize,
                        wantHTML, neuter, headers, serializeType, wantExpandGroupInfo, encodeMissingBlobs,
                        wantContent, fields);
            } catch (ServiceException e) {
                // problem writing the message structure to the response
                //   (this case generally means that the blob backing the MimeMessage disappeared halfway through)
                try {
                    msg = mbox.getMessageById(octxt, msg.getId());
                    if (msg.getSavedSequence() != changeId) {
                        // if a draft was re-saved and we failed because the old blob was deleted
                        //   out from under us, just fetch the new MimeMessage and try again
                        changeId = msg.getSavedSequence();
                        ZimbraLog.soap.info("caught message content change while serializing; will retry");
                        continue;
                    }
                } catch (NoSuchItemException nsie) {
                    // the message has been deleted, so don't include draft data in the response
                    throw nsie;
                }
                // We weren't able to write the message structure and it's not clear what went wrong.
                // best we can do now is send back what we got and apologize.
                ZimbraLog.soap.warn("could not serialize full message structure in response", e);
                return encodeMessageAsMPHelper(true /* bestEffort */, parent, ifmt, octxt, msg, part, maxSize,
                        wantHTML, neuter, headers, serializeType, wantExpandGroupInfo, encodeMissingBlobs,
                        wantContent, fields);
            }
        }
    }

    /** Encodes a Message object into <m> element with <mp> elements for
     *  message body.
     * @param parent  The Element to add the new <tt>&lt;m></tt> to.
     * @param ifmt    The formatter to sue when serializing item ids.
     * @param msg     The Message to serialize.
     * @param part    If non-null, serialize this message/rfc822 subpart of
     *                the Message instead of the Message itself.
     * @param maxSize TODO
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @param bestEffort  If <tt>true</tt>, errors serializing part content
     *                    are swallowed.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    private static Element encodeMessageAsMPHelper(boolean bestEffort, Element parent, ItemIdFormatter ifmt,
            OperationContext octxt, Message msg, String part, int maxSize, boolean wantHTML,
            boolean neuter, Set<String> headers, boolean serializeType, boolean wantExpandGroupInfo,
            boolean encodeMissingBlobs, MsgContent wantContent, int fields)
    throws ServiceException {
        Element m = null;
        boolean success = false;
        try {
            boolean wholeMessage = part == null || part.trim().isEmpty();
            m = encodeMsgCommonAndIdInfo(parent, ifmt, octxt, msg, part, serializeType, fields);
            MimeMessage mm = null;
            try {
                String requestedAccountId = octxt.getmRequestedAccountId();
                String authtokenAccountId = octxt.getmAuthTokenAccountId();
                boolean isDecryptionNotAllowed = StringUtils.isNotEmpty(authtokenAccountId) && !authtokenAccountId.equalsIgnoreCase(requestedAccountId);
                if (isDecryptionNotAllowed && Mime.isEncrypted(msg.getMimeMessage(false).getContentType())) {
                    mm = msg.getMimeMessage(false);
                } else {
                    mm = msg.getMimeMessage();
                }
            } catch (MailServiceException e) {
                if (encodeMissingBlobs && MailServiceException.NO_SUCH_BLOB.equals(e.getCode())) {
                    ZimbraLog.mailbox.error("Unable to get blob while encoding message", e);
                    encodeEmail(m, msg.getSender(), EmailType.FROM);
                    encodeEmail(m, msg.getSender(), EmailType.SENDER);
                    if (msg.getRecipients() != null) {
                        addEmails(m, Mime.parseAddressHeader(msg.getRecipients()), EmailType.TO);
                    }
                    m.addAttribute(MailConstants.A_SUBJECT, msg.getSubject());
                    Element mimePart = m.addNonUniqueElement(MailConstants.E_MIMEPART);
                    mimePart.addAttribute(MailConstants.A_PART, 1);
                    mimePart.addAttribute(MailConstants.A_BODY, true);
                    mimePart.addAttribute(MailConstants.A_CONTENT_TYPE, MimeConstants.CT_TEXT_PLAIN);

                    String errMsg = L10nUtil.getMessage(L10nUtil.MsgKey.errMissingBlob,
                                    msg.getAccount().getLocale(), ifmt.formatItemId(msg));
                    m.addAttribute(MailConstants.E_FRAG, errMsg, Element.Disposition.CONTENT);
                    mimePart.addAttribute(MailConstants.E_CONTENT, errMsg, Element.Disposition.CONTENT);
                    success = true; //not really success, but mark as such so the element is appended correctly
                    return m;
                }
                throw e;
            }
            if (!wholeMessage) {
                MimePart mp = Mime.getMimePart(mm, part);
                if (mp == null) {
                    throw MailServiceException.NO_SUCH_PART(part);
                }
                Object content = Mime.getMessageContent(mp);
                if (!(content instanceof MimeMessage)) {
                    throw MailServiceException.NO_SUCH_PART(part);
                }
                mm = (MimeMessage) content;
            } else {
                part = "";
            }

            // Add fragment before emails to maintain consistent ordering
            // of elements with encodeConversation - need to do this to
            // overcome JAXB issues.

            String fragment = msg.getFragment();
            if (fragment != null && !fragment.isEmpty()) {
                m.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);
            }

            addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
            addEmails(m, Mime.parseAddressHeader(mm, "Sender"), EmailType.SENDER);
            addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
            addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
            addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
            addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);
            addEmails(m, Mime.parseAddressHeader(mm.getHeader("Resent-From", null), false), EmailType.RESENT_FROM);
            // read-receipts only get sent by the mailbox's owner
            if (!(octxt.isDelegatedRequest(msg.getMailbox()) && octxt.isOnBehalfOfRequest(msg.getMailbox()))) {
                addEmails(m, Mime.parseAddressHeader(mm, "Disposition-Notification-To"), EmailType.READ_RECEIPT);
            }

            String calIntendedFor = msg.getCalendarIntendedFor();
            m.addAttribute(MailConstants.A_CAL_INTENDED_FOR, calIntendedFor);

            String subject = Mime.getSubject(mm);
            if (subject != null) {
                m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject),
                        Element.Disposition.CONTENT);
            }

            String messageID = mm.getMessageID();
            if (messageID != null && !messageID.trim().isEmpty()) {
                m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID),
                        Element.Disposition.CONTENT);
            }

            if (wholeMessage && msg.isDraft()) {
                if (!msg.getDraftOrigId().isEmpty()) {
                    ItemId origId = new ItemId(msg.getDraftOrigId(), msg.getMailbox().getAccountId());
                    m.addAttribute(MailConstants.A_ORIG_ID, ifmt.formatItemId(origId));
                }
                if (!msg.getDraftReplyType().isEmpty()) {
                    m.addAttribute(MailConstants.A_REPLY_TYPE, msg.getDraftReplyType());
                }
                if (!msg.getDraftIdentityId().isEmpty()) {
                    m.addAttribute(MailConstants.A_IDENTITY_ID, msg.getDraftIdentityId());
                }
                if (!msg.getDraftAccountId().isEmpty()) {
                    m.addAttribute(MailConstants.A_FOR_ACCOUNT, msg.getDraftAccountId());
                }
                String inReplyTo = mm.getHeader("In-Reply-To", null);
                if (inReplyTo != null && !inReplyTo.isEmpty()) {
                    m.addAttribute(MailConstants.E_IN_REPLY_TO, StringUtil.stripControlCharacters(inReplyTo),
                            Element.Disposition.CONTENT);
                }
                if (msg.getDraftAutoSendTime() != 0) {
                    m.addAttribute(MailConstants.A_AUTO_SEND_TIME, msg.getDraftAutoSendTime());
                }
            }

            if (!wholeMessage) {
                m.addAttribute(MailConstants.A_SIZE, mm.getSize());
            }

            Date sent = mm.getSentDate();
            if (sent != null) {
                m.addAttribute(MailConstants.A_SENT_DATE, sent.getTime());
            }

            Calendar resent = DateUtil.parseRFC2822DateAsCalendar(mm.getHeader("Resent-Date", null));
            if (resent != null) {
                m.addAttribute(MailConstants.A_RESENT_DATE, resent.getTimeInMillis());
            }

            if (msg.isInvite() && msg.hasCalendarItemInfos()) {
                encodeInvitesForMessage(m, ifmt, octxt, msg, NOTIFY_FIELDS, neuter);
            }

            if (headers != null) {
                for (String name : headers) {
                    String[] values = mm.getHeader(name);
                    if (values != null) {
                        for (int i = 0; i < values.length; i++) {
                            m.addKeyValuePair(name, values[i], MailConstants.A_HEADER, MailConstants.A_ATTRIBUTE_NAME);
                        }
                    }
                }
            }

            List<MPartInfo> parts = Mime.getParts(mm, getDefaultCharset(msg));
            if (parts != null && !parts.isEmpty()) {
                Set<MPartInfo> bodies = Mime.getBody(parts, wantHTML);
                addParts(m, parts.get(0), bodies, part, maxSize, neuter, false, getDefaultCharset(msg), bestEffort, wantContent);
            }

            if (wantExpandGroupInfo) {
                ZimbraLog.gal.trace("want expand group info");
                Account authedAcct = octxt.getAuthenticatedUser();
                Account requestedAcct = msg.getMailbox().getAccount();
                encodeAddrsWithGroupInfo(m, requestedAcct, authedAcct);
            } else {
                ZimbraLog.gal.trace("do not want expand group info");
            }

            success = true;
            // update crypto flags - isSigned/isEncrypted
            if (SmimeHandler.getHandler() != null) {
                if (!wholeMessage) {
                    // check content type of attachment message for encryption flag
                    SmimeHandler.getHandler().updateCryptoFlags(msg, m, mm, mm);
                } else {
                    MimeMessage originalMimeMessage = msg.getMimeMessage(false);
                    SmimeHandler.getHandler().updateCryptoFlags(msg, m, originalMimeMessage, mm);
                }
            }

            // if the mime it is signed
            if (Mime.isMultipartSigned(mm.getContentType())
                || Mime.isPKCS7Signed(mm.getContentType())) {
                ZimbraLog.mailbox.debug(
                    "The message is signed. Forwarding it to SmimeHandler for signature verification.");
                if (SmimeHandler.getHandler() != null) {
                    SmimeHandler.getHandler().verifyMessageSignature(msg, m, mm, octxt);
                } 
           } else {
                // if the original mime message was PKCS7-signed and it was
                // decoded and stored in cache as plain mime
                if ((mm instanceof Mime.FixedMimeMessage)
                    && ((Mime.FixedMimeMessage) mm).isPKCS7Signed()) {
                    if (SmimeHandler.getHandler() != null) {
                        SmimeHandler.getHandler().addPKCS7SignedMessageSignatureDetails(
                            msg.getMailbox().getAccount(), m, mm, octxt.getmResponseProtocol());
                    }
                }
            }
            return m;
        } catch (IOException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } catch (MessagingException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } finally {
            // don't leave turds around if we're going to retry
            if (!success && !bestEffort && m != null) {
                m.detach();
            }
        }
    }

    private static Element encodeMsgCommonAndIdInfo(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Message msg, String part, boolean serializeType, int fields)
                    throws ServiceException {
        Element m = null;
        boolean wholeMessage = part == null || part.trim().isEmpty();
        if (wholeMessage) {
            m = encodeMessageCommon(parent, ifmt, octxt, msg, fields, serializeType);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
        } else {
            m = parent.addNonUniqueElement(MailConstants.E_MSG);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
            m.addAttribute(MailConstants.A_PART, part);
        }
        if (needToOutput(fields, Change.IMAP_UID)) {
            m.addAttribute(MailConstants.A_IMAP_UID, msg.getImapUid());
        }
        return m;
    }

    /**
     * Encodes the basic search / sync fields onto an existing calendar item element
     */
    public static void setCalendarItemFields(Element calItemElem, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem calItem, int fields, boolean encodeInvites, boolean includeContent, boolean neuter)
    throws ServiceException {
        recordItemTags(calItemElem, calItem, octxt, fields);

        calItemElem.addAttribute(MailConstants.A_UID, calItem.getUid());
        calItemElem.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem));
        calItemElem.addAttribute(MailConstants.A_FOLDER,
            ifmt.formatItemId(new ItemId(calItem.getMailbox().getAccountId(), calItem.getFolderId())));

        if (needToOutput(fields, Change.CONTENT) && calItem.getSavedSequence() != 0) {
            calItemElem.addAttribute(MailConstants.A_REVISION, calItem.getSavedSequence());
        }
        if (needToOutput(fields, Change.SIZE)) {
            calItemElem.addAttribute(MailConstants.A_SIZE, calItem.getSize());
        }
        if (needToOutput(fields, Change.DATE)) {
            calItemElem.addAttribute(MailConstants.A_DATE, calItem.getDate());
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            calItemElem.addAttribute(MailConstants.A_CHANGE_DATE, calItem.getChangeDate() / 1000);
            calItemElem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, calItem.getModifiedSequence());
        }

        boolean doDetails = needToOutput(fields, Change.CONTENT) && encodeInvites;
        boolean isPublic = true;
        boolean hasSeries = false;
        boolean hasExceptions = false;
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            if (inv.isRecurrence()) {
                hasSeries = true;
            } else if (inv.hasRecurId()) {
                hasExceptions = true;
            }
            if (!inv.isPublic()) {
                isPublic = false;
            }
            if (doDetails) {
                encodeInvite(calItemElem, ifmt, octxt, calItem, inv, includeContent, neuter);
            }
        }
        if (doDetails) {
            if (isPublic || allowPrivateAccess(octxt, calItem)) {
                encodeCalendarReplies(calItemElem, calItem);
            }
            encodeAlarmTimes(calItemElem, calItem);
        }
        if (hasExceptions && !hasSeries) {
            calItemElem.addAttribute(MailConstants.A_CAL_ORPHAN, true);
        }
    }

    public static void setCalendarItemFields(Element calItemElem, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem calItem, int fields, boolean encodeInvites, boolean neuter) throws ServiceException {
        setCalendarItemFields(calItemElem, ifmt, octxt, calItem, fields, encodeInvites, false, neuter);
    }

    public static Element encodeInvite(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem cal, Invite inv, boolean neuter)
    throws ServiceException {
        return encodeInvite(parent, ifmt, octxt, cal, inv, false, neuter);
    }

    public static Element encodeInvite(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem cal, Invite inv, boolean includeContent, boolean neuter)
    throws ServiceException {
        Element ie = parent.addNonUniqueElement(MailConstants.E_INVITE);
        setCalendarItemType(ie, cal.getType());
        encodeTimeZoneMap(ie, cal.getTimeZoneMap());

        //encodeAlarmTimes(ie, cal);

        ie.addAttribute(MailConstants.A_CAL_SEQUENCE, inv.getSeqNo());
        ie.addAttribute(MailConstants.A_ID, inv.getMailItemId());
        ie.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, inv.getComponentNum());
        if (inv.hasRecurId()) {
            ie.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, inv.getRecurId().toString());
        }
        encodeInviteComponent(ie, ifmt, octxt, cal, (ItemId) null, inv, NOTIFY_FIELDS, neuter);

        if (includeContent && (inv.isPublic() || allowPrivateAccess(octxt, cal))) {
            int invId = inv.getMailItemId();
            MimeMessage mm = cal.getSubpartMessage(invId);
            if (mm != null) {
                List<MPartInfo> parts;
                try {
                    parts = Mime.getParts(mm, getDefaultCharset(cal));
                } catch (IOException ex) {
                    throw ServiceException.FAILURE(ex.getMessage(), ex);
                } catch (MessagingException ex) {
                    throw ServiceException.FAILURE(ex.getMessage(), ex);
                }
                if (parts != null && !parts.isEmpty()) {
                    addParts(ie, parts.get(0), null, "", -1, false, true, getDefaultCharset(cal), true);
                }
            }
        }

        return ie;
    }

    /**
     * Encode the metadata for a calendar item.
     *
     * The content for the calendar item is a big multipart/digest containing each
     * invite in the calendar item as a sub-mimepart -- it can be retreived from the content
     * servlet:
     *    http://servername/service/content/get?id=<calItemId>
     *
     * The client can ALSO request just the content for each individual invite using a
     * compound item-id request:
     *    http://servername/service/content/get?id=<calItemId>-<invite-mail-item-id>
     *
     * DO NOT use the raw invite-mail-item-id to fetch the content: since the invite is a
     * standard mail-message it can be deleted by the user at any time!
     */
    public static Element encodeCalendarItemSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem calItem, int fields, boolean includeInvites, boolean includeContent) throws ServiceException {
        final int MAX_RETRIES = LC.calendar_item_get_max_retries.intValue();
        Element elem = null;
        Mailbox mbox = calItem.getMailbox();
        int changeId = calItem.getSavedSequence();
        int numTries = 0;
        while (numTries < MAX_RETRIES) {
            numTries++;
            if (calItem instanceof Appointment) {
                elem = parent.addNonUniqueElement(MailConstants.E_APPOINTMENT);
            } else {
                elem = parent.addNonUniqueElement(MailConstants.E_TASK);
            }
            try {
                setCalendarItemFields(elem, ifmt, octxt, calItem, fields, includeInvites, includeContent, true);

                if (needToOutput(fields, Change.METADATA)) {
                    encodeAllCustomMetadata(elem, calItem, fields);
                }
                return elem;
            } catch (ServiceException e) {
                // Problem writing the structure to the response
                //   (this case generally means that the blob backing the Calendar item disappeared halfway through)
                elem.detach();
                elem = null;
                try {
                    calItem = mbox.getCalendarItemById(octxt, calItem.getId());
                    if (calItem.getSavedSequence() != changeId) {
                        // just fetch the new item and try again
                        changeId = calItem.getSavedSequence();
                        ZimbraLog.soap.info("caught calendar item content change while serializing; will retry");
                        continue;
                    }
                } catch (NoSuchItemException nsie) {
                    // the message has been deleted, so don't include data in the response
                    throw nsie;
                }
                // Were unable to write calendar item structure & not clear what went wrong. Try again a few times
                // in case it was related to underlying modifications
                ZimbraLog.soap.warn("Could not serialize full calendar item structure in response", e);
            }
        }
        // Still failing after several retries...
        throw NoSuchItemException.NO_SUCH_CALITEM("Problem encoding calendar item.  Maybe corrupt");
    }

    public static Element encodeCalendarItemSummary(Element parent, ItemIdFormatter ifmt,
                                                    OperationContext octxt, CalendarItem calItem,
                                                    int fields, boolean includeInvites)
    throws ServiceException {
        return encodeCalendarItemSummary(parent, ifmt, octxt, calItem, fields, includeInvites, false);
    }

    public static void encodeCalendarReplies(Element parent, CalendarItem calItem, Invite inv, String recurIdZ) {
        List<CalendarItem.ReplyInfo> replies = calItem.getReplyInfo(inv, recurIdZ);
        encodeCalendarReplies(parent, replies);
    }

    public static void encodeCalendarReplies(Element parent, CalendarItem calItem) {
        List<CalendarItem.ReplyInfo> replies = calItem.getAllReplies();
        encodeCalendarReplies(parent, replies);
    }

    private static void encodeCalendarReplies(Element parent, List<CalendarItem.ReplyInfo> replies) {
        if (replies.isEmpty()) {
            return;
        }
        List<CalendarReply> jaxbReplies = Lists.newArrayList();
        for (CalendarItem.ReplyInfo repInfo : replies) {
            jaxbReplies.add(repInfo.toJAXB());
        }
        CalendarReply.encodeCalendarReplyList(parent, jaxbReplies);
    }

    private static String getDefaultCharset(MailItem item) throws ServiceException {
        Account acct = (item == null ? null : item.getAccount());
        return acct == null ? null : acct.getAttr(Provisioning.A_zimbraPrefMailDefaultCharset, null);
    }

    /** Encodes an Invite stored within a calendar item object into <m> element
     *  with <mp> elements.
     * @param parent  The Element to add the new <tt>&lt;m></tt> to.
     * @param ifmt    The SOAP request's context.
     * @param calItem The calendar item to serialize.
     * @param iid     The requested item; the contained subpart will be used to
     *                pick the Invite out of the calendar item's blob & metadata.
     * @param part    If non-null, we'll serialuize this message/rfc822 subpart
     *                of the specified Message instead of the Message itself.
     * @param maxSize The maximum amount of content to inline (<=0 is unlimited).
     * @param wantHTML  <tt>true</tt> to prefer HTML parts as the "body",
     *                  <tt>false</tt> to prefer text/plain parts.
     * @param neuter  Whether to rename "src" attributes on HTML <img> tags.
     * @param headers Extra message headers to include in the returned element.
     * @param serializeType If <tt>false</tt>, always serializes as an
     *                      <tt>&lt;m></tt> element.
     * @return The newly-created <tt>&lt;m></tt> Element, which has already
     *         been added as a child to the passed-in <tt>parent</tt>.
     * @throws ServiceException */
    public static Element encodeInviteAsMP(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem calItem, String recurIdZ, ItemId iid, String part, int maxSize, boolean wantHTML,
            boolean neuter, Set<String> headers, boolean serializeType, boolean wantExpandGroupInfo)
    throws ServiceException {
        int invId = iid.getSubpartId();
        Invite[] invites = calItem.getInvites(invId);
        boolean isPublic = calItem.isPublic();
        boolean showAll = isPublic || allowPrivateAccess(octxt, calItem);

        boolean wholeMessage = (part == null || part.trim().isEmpty());

        Element m;
        if (wholeMessage) {
            // We want to return the MODIFIED_CONFLICT fields to enable conflict detection on modify.
            int fields = NOTIFY_FIELDS | Change.CONFLICT;
            m = encodeMessageCommon(parent, ifmt, octxt, calItem, fields, serializeType);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem, invId));
        } else {
            m = parent.addNonUniqueElement(MailConstants.E_MSG);
            m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(calItem, invId));
            m.addAttribute(MailConstants.A_PART, part);
        }

        try {
            MimeMessage mm = calItem.getSubpartMessage(invId);
            if (mm != null) {
                if (!wholeMessage) {
                    MimePart mp = Mime.getMimePart(mm, part);
                    if (mp == null) {
                        throw MailServiceException.NO_SUCH_PART(part);
                    }
                    Object content = Mime.getMessageContent(mp);
                    if (!(content instanceof MimeMessage)) {
                        throw MailServiceException.NO_SUCH_PART(part);
                    }
                    mm = (MimeMessage) content;
                } else {
                    part = "";
                }
                if (showAll) {
                    addEmails(m, Mime.parseAddressHeader(mm, "From"), EmailType.FROM);
                    addEmails(m, Mime.parseAddressHeader(mm, "Sender"), EmailType.SENDER);
                    addEmails(m, Mime.parseAddressHeader(mm, "Reply-To"), EmailType.REPLY_TO);
                    addEmails(m, Mime.parseAddressHeader(mm, "To"), EmailType.TO);
                    addEmails(m, Mime.parseAddressHeader(mm, "Cc"), EmailType.CC);
                    addEmails(m, Mime.parseAddressHeader(mm, "Bcc"), EmailType.BCC);

                    String subject = Mime.getSubject(mm);
                    if (subject != null) {
                        m.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(subject),
                                Element.Disposition.CONTENT);
                    }
                    String messageID = mm.getMessageID();
                    if (messageID != null && !messageID.trim().isEmpty()) {
                        m.addAttribute(MailConstants.E_MSG_ID_HDR, StringUtil.stripControlCharacters(messageID),
                                Element.Disposition.CONTENT);
                    }
                    if (!wholeMessage) {
                        m.addAttribute(MailConstants.A_SIZE, mm.getSize());
                    }
                    java.util.Date sent = mm.getSentDate();
                    if (sent != null) {
                        m.addAttribute(MailConstants.A_SENT_DATE, sent.getTime());
                    }
                }
            }

            Element invElt = m.addNonUniqueElement(MailConstants.E_INVITE);
            setCalendarItemType(invElt, calItem.getType());
            encodeTimeZoneMap(invElt, calItem.getTimeZoneMap());
            if (invites.length > 0) {
                if (showAll) {
                    encodeCalendarReplies(invElt, calItem, invites[0], recurIdZ);
                }
                for (Invite inv : invites) {
                    encodeInviteComponent(invElt, ifmt, octxt, calItem, (ItemId) null, inv, NOTIFY_FIELDS, neuter);
                }
            }

            //encodeAlarmTimes(invElt, calItem);

            if (mm != null && showAll) {
                if (headers != null) {
                    for (String name : headers) {
                        String[] values = mm.getHeader(name);
                        if (values == null) {
                            continue;
                        }
                        for (int i = 0; i < values.length; i++) {
                            m.addKeyValuePair(name, values[i], MailConstants.A_HEADER, MailConstants.A_ATTRIBUTE_NAME);
                        }
                    }
                }

                List<MPartInfo> parts = Mime.getParts(mm, getDefaultCharset(calItem));
                if (parts != null && !parts.isEmpty()) {
                    Set<MPartInfo> bodies = Mime.getBody(parts, wantHTML);
                    addParts(m, parts.get(0), bodies, part, maxSize, neuter, true, getDefaultCharset(calItem), true);
                }
            }

            if (wantExpandGroupInfo) {
                Account authedAcct = octxt.getAuthenticatedUser();
                Account requestedAcct = calItem.getMailbox().getAccount();
                encodeAddrsWithGroupInfo(m, requestedAcct, authedAcct);
            }
        } catch (IOException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        } catch (MessagingException ex) {
            throw ServiceException.FAILURE(ex.getMessage(), ex);
        }
        return m;
    }


    private static final int    MAX_INLINE_MSG_SIZE = 40000;
    private static final String CONTENT_SERVLET_URI = "/service/content/get?id=";
    private static final String PART_PARAM_STRING   = "&part=";

    public static Element encodeMessageAsMIME(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Message msg, String part, boolean mustInline, boolean mustNotInline, boolean serializeType,
            int fields)
    throws ServiceException {
        boolean wholeMessage = (part == null || part.trim().isEmpty());
        Element m = encodeMsgCommonAndIdInfo(parent, ifmt, octxt, msg, part, serializeType, fields);
        Element content = m.addUniqueElement(MailConstants.E_CONTENT);
        long size = msg.getSize() + 2048;
        if (!wholeMessage) {
            content.addAttribute(MailConstants.A_URL,
                    CONTENT_SERVLET_URI + ifmt.formatItemId(msg) + PART_PARAM_STRING + part);
        } else if (mustNotInline || (!mustInline && size > MAX_INLINE_MSG_SIZE)) {
            content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
        } else {
            try {
                byte[] raw = msg.getContent();
                if (!ByteUtil.isASCII(raw)) {
                    if (!mustInline) {
                        content.addAttribute(MailConstants.A_URL, CONTENT_SERVLET_URI + ifmt.formatItemId(msg));
                    } else {
                        // Assume the data is utf-8.
                        content.setText(new String(raw, "utf-8"));
                    }
                } else {
                    content.setText(new String(raw, "US-ASCII"));
                }
            } catch (IOException ex) {
                throw ServiceException.FAILURE(ex.getMessage(), ex);
            }
        }

        return m;
    }

    public static Element encodeMessageSummary(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Message msg, OutputParticipants output, int fields) throws ServiceException {
        Element el = encodeMessageCommon(parent, ifmt, octxt, msg, fields, true);
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(msg));
        if (!needToOutput(fields, Change.CONTENT)) {
            return el;
        }
        boolean addRecips = (output == OutputParticipants.PUT_RECIPIENTS || output == OutputParticipants.PUT_BOTH);
        boolean addSenders = (output == OutputParticipants.PUT_BOTH || output == OutputParticipants.PUT_SENDERS);
        if (addRecips) {
            addEmails(el, Mime.parseAddressHeader(msg.getRecipients()), EmailType.TO);
        }
        if (addSenders) {
            encodeEmail(el, msg.getSender(), EmailType.FROM);
        }
        el.addAttribute(MailConstants.E_SUBJECT, StringUtil.stripControlCharacters(msg.getSubject()),
                Element.Disposition.CONTENT);

        // fragment has already been sanitized...
        String fragment = msg.getFragment();
        if (!fragment.isEmpty()) {
            el.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);
        }
        if (msg.isInvite() && msg.hasCalendarItemInfos()) {
            try {
                encodeInvitesForMessage(el, ifmt, octxt, msg, fields, true);
            } catch (ServiceException ex) {
                LOG.debug("Caught exception while encoding Invites for msg " + msg.getId(), ex);
            }
        }

        if (msg.isDraft() && msg.getDraftAutoSendTime() != 0) {
            el.addAttribute(MailConstants.A_AUTO_SEND_TIME, msg.getDraftAutoSendTime());
        }
        return el;
    }

    private static Element encodeMessageCommon(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            MailItem item, int fields, boolean serializeType) throws ServiceException {
        String name = serializeType && item instanceof Chat ? MailConstants.E_CHAT : MailConstants.E_MSG;
        Element elem = parent.addNonUniqueElement(name);
        // DO NOT encode the item-id here, as some Invite-Messages-In-CalendarItems have special item-id's
        if (needToOutput(fields, Change.SIZE)) {
            elem.addAttribute(MailConstants.A_SIZE, item.getSize());
        }
        if (needToOutput(fields, Change.DATE)) {
            elem.addAttribute(MailConstants.A_DATE, item.getDate());
        }
        if (needToOutput(fields, Change.FOLDER)) {
            elem.addAttribute(MailConstants.A_FOLDER,
                    ifmt.formatItemId(new ItemId(item.getMailbox().getAccountId(), item.getFolderId())));
        }
        if (item instanceof Message) {
            Message msg = (Message) item;
            if (needToOutput(fields, Change.PARENT) &&
                    (fields != NOTIFY_FIELDS || msg.getConversationId() != -1)) {
                elem.addAttribute(MailConstants.A_CONV_ID, ifmt.formatItemId(msg.getConversationId()));
            }
        }
        recordItemTags(elem, item, octxt, fields);
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(elem, item, fields);
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            elem.addAttribute(MailConstants.A_REVISION, item.getSavedSequence());
            elem.addAttribute(MailConstants.A_CHANGE_DATE, item.getChangeDate() / 1000);
            elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, item.getModifiedSequence());
        } else {
            if (needToOutput(fields, Change.MODSEQ) && item.getModifiedSequence() > 0) {
                elem.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, item.getModifiedSequence());
            }
            if (needToOutput(fields, Change.CONTENT) && item.getSavedSequence() != 0) {
                elem.addAttribute(MailConstants.A_REVISION, item.getSavedSequence());
            }
        }
        if (needToOutput(fields, Change.IMAP_UID)) {
            elem.addAttribute(MailConstants.A_IMAP_UID, item.getImapUid());
        }
        return elem;
    }

    private static void encodeTimeZoneMap(Element parent, TimeZoneMap tzmap) {
        assert(tzmap != null);
        for (Iterator<ICalTimeZone> iter = tzmap.tzIterator(); iter.hasNext(); ) {
            ICalTimeZone tz = iter.next();
            Element e = parent.addNonUniqueElement(MailConstants.E_CAL_TZ);
            e.addAttribute(MailConstants.A_ID, tz.getID());
            e.addAttribute(MailConstants.A_CAL_TZ_STDOFFSET, tz.getStandardOffset() / 60 / 1000);
            e.addAttribute(MailConstants.A_CAL_TZ_STDNAME, tz.getStandardTzname());

            if (tz.useDaylightTime()) {
                SimpleOnset standard = tz.getStandardOnset();
                SimpleOnset daylight = tz.getDaylightOnset();
                if (standard != null && daylight != null) {
                    e.addAttribute(MailConstants.A_CAL_TZ_DAYOFFSET, tz.getDaylightOffset() / 60 / 1000);
                    e.addAttribute(MailConstants.A_CAL_TZ_DAYNAME, tz.getDaylightTzname());

                    Element std = e.addNonUniqueElement(MailConstants.E_CAL_TZ_STANDARD);
                    int standardWeek = standard.getWeek();
                    if (standardWeek != 0) {
                        std.addAttribute(MailConstants.A_CAL_TZ_WEEK, standardWeek);
                        std.addAttribute(MailConstants.A_CAL_TZ_DAYOFWEEK, standard.getDayOfWeek());
                    } else {
                        std.addAttribute(MailConstants.A_CAL_TZ_DAYOFMONTH, standard.getDayOfMonth());
                    }
                    std.addAttribute(MailConstants.A_CAL_TZ_MONTH, standard.getMonth());
                    std.addAttribute(MailConstants.A_CAL_TZ_HOUR, standard.getHour());
                    std.addAttribute(MailConstants.A_CAL_TZ_MINUTE, standard.getMinute());
                    std.addAttribute(MailConstants.A_CAL_TZ_SECOND, standard.getSecond());

                    Element day = e.addNonUniqueElement(MailConstants.E_CAL_TZ_DAYLIGHT);
                    int daylightWeek = daylight.getWeek();
                    if (daylightWeek != 0) {
                        day.addAttribute(MailConstants.A_CAL_TZ_WEEK, daylightWeek);
                        day.addAttribute(MailConstants.A_CAL_TZ_DAYOFWEEK, daylight.getDayOfWeek());
                    } else {
                        day.addAttribute(MailConstants.A_CAL_TZ_DAYOFMONTH, daylight.getDayOfMonth());
                    }
                    day.addAttribute(MailConstants.A_CAL_TZ_MONTH, daylight.getMonth());
                    day.addAttribute(MailConstants.A_CAL_TZ_HOUR, daylight.getHour());
                    day.addAttribute(MailConstants.A_CAL_TZ_MINUTE, daylight.getMinute());
                    day.addAttribute(MailConstants.A_CAL_TZ_SECOND, daylight.getSecond());
                }
            }
        }
    }

    private static boolean allowPrivateAccess(OperationContext octxt, CalendarItem calItem) throws ServiceException {
        Account authAccount = octxt != null ? octxt.getAuthenticatedUser() : null;
        boolean asAdmin = octxt != null ? octxt.isUsingAdminPrivileges() : false;
        return calItem.allowPrivateAccess(authAccount, asAdmin);
    }

    public static Element encodeInviteComponent(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            CalendarItem calItem /* may be null */, ItemId calId /* may be null */,
            Invite invite, int fields, boolean neuter)
    throws ServiceException {
        boolean allFields = true;

        if (fields != NOTIFY_FIELDS) {
            allFields = false;
            if (!needToOutput(fields, Change.INVITE)) {
                return parent;
            }
        }

        Element e = parent.addNonUniqueElement(MailConstants.E_INVITE_COMPONENT);
        e.addAttribute(MailConstants.A_CAL_METHOD, invite.getMethod());
        e.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, invite.getComponentNum());
        if (invite.hasRsvp()) {
            e.addAttribute(MailConstants.A_CAL_RSVP, invite.getRsvp());
        }

        boolean allowPrivateAccess = calItem != null ? allowPrivateAccess(octxt, calItem) : true;
        if (allFields) {
            if (invite.isPublic() || allowPrivateAccess) {
                String priority = invite.getPriority();
                if (priority != null) {
                    e.addAttribute(MailConstants.A_CAL_PRIORITY, priority);
                }
                e.addAttribute(MailConstants.A_NAME, invite.getName());
                e.addAttribute(MailConstants.A_CAL_LOCATION, invite.getLocation());

                List<String> categories = invite.getCategories();
                if (categories != null) {
                    for (String cat : categories) {
                        e.addNonUniqueElement(MailConstants.E_CAL_CATEGORY).setText(cat);
                    }
                }
                List<String> comments = invite.getComments();
                if (comments != null) {
                    for (String cmt : comments) {
                        e.addNonUniqueElement(MailConstants.E_CAL_COMMENT).setText(cmt);
                    }
                }
                List<String> contacts = invite.getContacts();
                if (contacts != null) {
                    for (String contact : contacts) {
                        e.addNonUniqueElement(MailConstants.E_CAL_CONTACT).setText(contact);
                    }
                }
                Geo geo = invite.getGeo();
                if (geo != null) {
                    geo.toXml(e);
                }
                // Percent Complete (VTODO)
                if (invite.isTodo()) {
                    String pct = invite.getPercentComplete();
                    if (pct != null)
                        e.addAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, pct);
                    long completed = invite.getCompleted();
                    if (completed != 0) {
                        ParsedDateTime c = ParsedDateTime.fromUTCTime(completed);
                        e.addAttribute(MailConstants.A_TASK_COMPLETED, c.getDateTimePartString());
                    }
                }

                // Attendee(s)
                List<ZAttendee> attendees = invite.getAttendees();
                for (ZAttendee at : attendees) {
                    at.toXml(e);
                }

                // Alarms
                Iterator<Alarm> alarmsIter = invite.alarmsIterator();
                while (alarmsIter.hasNext()) {
                    Alarm alarm = alarmsIter.next();
                    alarm.toXml(e);
                }

                // x-prop
                encodeXProps(e, invite.xpropsIterator());

                // fragment
                String fragment = invite.getFragment();
                if (!Strings.isNullOrEmpty(fragment)) {
                    e.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);
                }

                if (!invite.hasBlobPart()) {
                    e.addAttribute(MailConstants.A_CAL_NO_BLOB, true);
                }
                // Description (plain and html)
                String desc = invite.getDescription();
                if (desc != null) {
                    Element descElem = e.addNonUniqueElement(MailConstants.E_CAL_DESCRIPTION);
                    descElem.setText(desc);
                }
                String descHtml = invite.getDescriptionHtml();
                BrowserDefang defanger = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML);
                if (descHtml != null) {
                    try {
                        descHtml = StringUtil.stripControlCharacters(descHtml);
                        descHtml = defanger.defang(descHtml, neuter);
                        Element descHtmlElem = e.addNonUniqueElement(MailConstants.E_CAL_DESC_HTML);
                        descHtmlElem.setText(descHtml);
                    } catch (IOException ex) {
                        ZimbraLog.calendar.warn("Unable to defang HTML for SetAppointmentRequest", ex);
                    }
                }

                if (invite.isEvent()) {
                    if (calItem != null && calItem instanceof Appointment) {
                        Instance inst = Instance.fromInvite(calItem.getId(), invite);
                        Appointment appt = (Appointment) calItem;
                        e.addAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL,
                                appt.getEffectiveFreeBusyActual(invite, inst));
                    }
                    e.addAttribute(MailConstants.A_APPT_FREEBUSY, invite.getFreeBusy());
                    e.addAttribute(MailConstants.A_APPT_TRANSPARENCY, invite.getTransparency());
                }

                // Organizer
                if (invite.hasOrganizer()) {
                    ZOrganizer org = invite.getOrganizer();
                    org.toXml(e);
                }
                e.addAttribute(MailConstants.A_CAL_URL, invite.getUrl());
            }

            if (invite.isOrganizer()) {
                e.addAttribute(MailConstants.A_CAL_ISORG, true);
            }

            boolean isRecurring = false;
            e.addAttribute("x_uid", invite.getUid());
            e.addAttribute(MailConstants.A_UID, invite.getUid());
            e.addAttribute(MailConstants.A_CAL_SEQUENCE, invite.getSeqNo());
            e.addAttribute(MailConstants.A_CAL_DATETIME, invite.getDTStamp()); //zdsync

            String itemId = null;
            if (calId != null) {
                itemId = calId.toString(ifmt);
            } else if (calItem != null) {
                itemId = ifmt.formatItemId(calItem);
            }
            if ((itemId != null) && !("0".equals(itemId))) {
                e.addAttribute(MailConstants.A_CAL_ID /* calItemId */, itemId);
                if (invite.isEvent()) {
                    e.addAttribute(MailConstants.A_APPT_ID_DEPRECATE_ME /* apptId */, itemId);  // for backward compat
                }
                if (calItem != null) {
                    ItemId ciFolderId = new ItemId(calItem.getMailbox(), calItem.getFolderId());
                    e.addAttribute(MailConstants.A_CAL_ITEM_FOLDER /* ciFolder */, ifmt.formatItemId(ciFolderId));
                }
            }

            Recurrence.IRecurrence recur = invite.getRecurrence();
            if (recur != null) {
                isRecurring = true;
                Element recurElt = e.addNonUniqueElement(MailConstants.E_CAL_RECUR);
                recur.toXml(recurElt);
            }

            e.addAttribute(MailConstants.A_CAL_STATUS, invite.getStatus());
            e.addAttribute(MailConstants.A_CAL_CLASS, invite.getClassProp());

            boolean allDay = invite.isAllDayEvent();
            boolean isException = invite.hasRecurId();
            if (isException) {
                e.addAttribute(MailConstants.A_CAL_IS_EXCEPTION, true);
                RecurId rid = invite.getRecurId();
                e.addAttribute(MailConstants.A_CAL_RECURRENCE_ID_Z, rid.getDtZ());
                encodeRecurId(e, rid, allDay);
            }

            boolean forceUTC = DebugConfig.calendarForceUTC && !isRecurring && !isException && !allDay;
            ParsedDateTime dtStart = invite.getStartTime();
            if (dtStart != null) {
                encodeDtStart(e, dtStart, allDay, forceUTC);
            }
            ParsedDateTime dtEnd = invite.getEndTime();
            if (dtEnd != null) {
                encodeDtEnd(e, dtEnd, allDay, invite.isTodo(), forceUTC);
            }
            ParsedDuration dur = invite.getDuration();
            if (dur != null) {
                dur.toXml(e);
            }
            if (allDay) {
                e.addAttribute(MailConstants.A_CAL_ALLDAY, true);
            }
            if (invite.isDraft()) {
                e.addAttribute(MailConstants.A_CAL_DRAFT, true);
            }
            if (invite.isNeverSent()) {
                e.addAttribute(MailConstants.A_CAL_NEVER_SENT, true);
            }
        }

        return e;
    }

    public static List<XParam> jaxbXParams(Iterator<ZParameter> xparamsIterator) {
        List<XParam> xparams = Lists.newArrayList();
        while (xparamsIterator.hasNext()) {
            ZParameter xparam = xparamsIterator.next();
            String paramName = xparam.getName();
            if (paramName == null) {
                continue;
            }
            xparams.add(new XParam(paramName, xparam.getValue()));
        }
        return Collections.unmodifiableList(xparams);
    }

    public static List<XProp> jaxbXProps(Iterator<ZProperty> xpropsIterator) {
        List<XProp> xprops = Lists.newArrayList();
        while (xpropsIterator.hasNext()) {
            ZProperty xprop = xpropsIterator.next();
            String paramName = xprop.getName();
            if (paramName == null) {
                continue;
            }
            XProp xp = new XProp(paramName, xprop.getValue());
            xp.setXParams(jaxbXParams(xprop.parameterIterator()));
            xprops.add(xp);
        }
        return Collections.unmodifiableList(xprops);
    }

    /**
     * Use {@link jaxbXProps} where possible instead of this
     */
    public static void encodeXProps(Element parent, Iterator<ZProperty> xpropsIterator) {
        while (xpropsIterator.hasNext()) {
            ZProperty xprop = xpropsIterator.next();
            String propName = xprop.getName();
            if (propName == null) {
                continue;
            }
            String propValue = xprop.getValue();
            Element propElem = parent.addNonUniqueElement(MailConstants.E_CAL_XPROP);
            propElem.addAttribute(MailConstants.A_NAME, propName);
            if (propValue != null) {
                propElem.addAttribute(MailConstants.A_VALUE, propValue);
            }
            CalendarUtil.encodeXParams(propElem, xprop.parameterIterator());
        }
    }

    private static Element encodeInvitesForMessage(Element parent, ItemIdFormatter ifmt, OperationContext octxt,
            Message msg, int fields, boolean neuter)
    throws ServiceException {
        if (fields != NOTIFY_FIELDS && !needToOutput(fields, Change.INVITE)) {
            return parent;
        }

        Element ie = parent.addNonUniqueElement(MailConstants.E_INVITE);

        Mailbox mbox = msg.getMailbox();

        for (Iterator<Message.CalendarItemInfo> iter = msg.getCalendarItemInfoIterator(); iter.hasNext(); ) {
            Message.CalendarItemInfo info = iter.next();
            CalendarItem calItem = null;
            ICalTok method = ICalTok.REQUEST;
            Invite invCi = info.getInvite();
            if (invCi != null) {
                method = Invite.lookupMethod(invCi.getMethod());
            }
            Invite invite = invCi;
            ItemId calendarItemId = info.getCalendarItemId();
            if (info.calItemCreated()) {
                try {
                    calItem = mbox.getCalendarItemById(octxt, calendarItemId);
                } catch (MailServiceException.NoSuchItemException e) {
                    // Calendar item has been deleted. Bug 84877 - don't include stale references to it in SOAP response
                    calendarItemId = null;
                } catch (ServiceException e) {
                    // eat PERM_DENIED
                    if (e.getCode() != ServiceException.PERM_DENIED) {
                        throw e;
                    }
                    calendarItemId = null;  // If we can't access it.  Don't include a reference to it.
                }
                // Do staleness check for invitation messages.
                if (ICalTok.REQUEST.equals(method) || ICalTok.PUBLISH.equals(method)) {
                    if (calItem != null && calItem.getFolderId() != Mailbox.ID_FOLDER_TRASH) {
                        if (invCi != null) {
                            // See if the messsage's invite is outdated.
                            Invite invCurr = calItem.getInvite(invCi.getRecurId());
                            if (invCurr != null) {
                                if (invCi.getSeqNo() >= invCurr.getSeqNo()) {
                                    // Invite is new or same as what's in the appointment.  Show it.
                                    invite = invCi;
                                } else {
                                    // Outdated.  Don't show it.
                                    invite = null;
                                }
                            } else {
                                // New invite.  Show it.
                                invite = invCi;
                            }
                        } else {
                            // legacy case
                            invite = calItem.getInvite(msg.getId(), info.getComponentNo());
                            // invite == null if the invite was outdated by a newer update
                        }
                    }
                }
            } else {
                // We have an invite that wasn't auto-added.
                if (invCi != null) {
                    if (!Invite.isOrganizerMethod(invCi.getMethod()) || ICalTok.DECLINECOUNTER.equals(method)) {
                        invite = invCi;
                    } else {
                        try {
                            calItem = mbox.getCalendarItemByUid(octxt, invCi.getUid());
                        } catch (MailServiceException.NoSuchItemException e) {
                            // ignore
                        } catch (ServiceException e) {
                            // eat PERM_DENIED
                            if (e.getCode() != ServiceException.PERM_DENIED) {
                                throw e;
                            }
                        }
                        if (calItem != null) {
                            // See if the messsage's invite is outdated.
                            Invite invCurr = calItem.getInvite(invCi.getRecurId());
                            if (invCurr != null) {
                                if (invCi.getSeqNo() >= invCurr.getSeqNo()) {
                                    // Invite is new or same as what's in the appointment.  Show it.
                                    invite = invCi;
                                } else {
                                    // Outdated.  Don't show it.
                                    invite = null;
                                }
                            } else {
                                // New invite.  Show it.
                                invite = invCi;
                            }
                        } else {
                            // Appointment doesn't exist.  The invite in the message should be displayed and the
                            // user can manually add the appointment.
                            invite = invCi;
                        }
                    }
                }
            }

            if (invite != null) {
                setCalendarItemType(ie, invite.getItemType());
                encodeTimeZoneMap(ie, invite.getTimeZoneMap());
                com.zimbra.soap.mail.type.CalendarItemInfo remoteCalendarItem = null;
                if (calItem == null) {
                    remoteCalendarItem = msg.getRemoteCalendarItem(invite);
                    if (remoteCalendarItem != null) {
                        calendarItemId = new ItemId(remoteCalendarItem.getId(), (String)null);
                    }
                }
                encodeInviteComponent(ie, ifmt, octxt, calItem, calendarItemId, invite, fields, neuter);
                ICalTok invMethod = Invite.lookupMethod(invite.getMethod());
                if (ICalTok.REQUEST.equals(invMethod) || ICalTok.PUBLISH.equals(invMethod)) {
                    InviteChanges invChanges = info.getInviteChanges();
                    if (invChanges != null && !invChanges.noChange()) {
                        Element comp = ie.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
                        if (comp != null) {
                            comp.addAttribute(MailConstants.A_CAL_CHANGES, invChanges.toString());
                        }
                    }
                    if (calItem != null) {
                        boolean showAll = invite.isPublic() || allowPrivateAccess(octxt, calItem);
                        if (showAll) {
                            RecurId rid = invite.getRecurId();
                            encodeCalendarReplies(ie, calItem, invite, rid != null ? rid.getDtZ() : null);
                        }
                    } else if (null != remoteCalendarItem){
                        CalendarReply.encodeCalendarReplyList(ie, remoteCalendarItem.getCalendarReplies());
                    }
                }
            }
        }

        return ie;
    }

    private enum VisitPhase { PREVISIT, POSTVISIT }

    private static void addParts(Element root, MPartInfo mpiRoot, Set<MPartInfo> bodies, String prefix, int maxSize,
        boolean neuter, boolean excludeCalendarParts, String defaultCharset, boolean swallowContentExceptions)
throws ServiceException {
        addParts(root, mpiRoot, bodies, prefix, maxSize, neuter, excludeCalendarParts, defaultCharset, swallowContentExceptions, MsgContent.full);
    }

    private static void addParts(Element root, MPartInfo mpiRoot, Set<MPartInfo> bodies, String prefix, int maxSize,
            boolean neuter, boolean excludeCalendarParts, String defaultCharset, boolean swallowContentExceptions, MsgContent wantContent)
    throws ServiceException {
        MPartInfo mpi = mpiRoot;
        LinkedList<Pair<Element, LinkedList<MPartInfo>>> queue = new LinkedList<Pair<Element, LinkedList<MPartInfo>>>();
        Pair<Element, LinkedList<MPartInfo>> level = new Pair<Element, LinkedList<MPartInfo>>(root, new LinkedList<MPartInfo>());
        level.getSecond().add(mpi);
        queue.add(level);

        VisitPhase phase = VisitPhase.PREVISIT;
        while (!queue.isEmpty()) {
            level = queue.getLast();
            LinkedList<MPartInfo> parts = level.getSecond();
            if (parts.isEmpty()) {
                queue.removeLast();  phase = VisitPhase.POSTVISIT;  continue;
            }

            mpi = parts.getFirst();
            Element child = addPart(phase, level.getFirst(), root, mpi, bodies, prefix, maxSize, neuter,
                    excludeCalendarParts, defaultCharset, swallowContentExceptions, wantContent);
            if (phase == VisitPhase.PREVISIT && child != null && mpi.hasChildren()) {
                queue.addLast(new Pair<Element, LinkedList<MPartInfo>>(child, new LinkedList<MPartInfo>(mpi.getChildren())));
            } else {
                parts.removeFirst();  phase = VisitPhase.PREVISIT;
            }
        }
    }

    private static Element addPart(VisitPhase phase, Element parent, Element root, MPartInfo mpi,
            Set<MPartInfo> bodies, String prefix, int maxSize, boolean neuter, boolean excludeCalendarParts,
            String defaultCharset, boolean swallowContentExceptions, MsgContent wantContent)
    throws ServiceException {
        if (phase == VisitPhase.POSTVISIT) {
            return null;
        }
        String ctype = StringUtil.stripControlCharacters(mpi.getContentType());

        if (excludeCalendarParts && MimeConstants.CT_TEXT_CALENDAR.equalsIgnoreCase(ctype)) {
            // A true calendar part has "method" parameter in the content type.  Otherwise it's just an attachment
            // that happens to be a .ics file.
            try {
                ContentType ct = new ContentType(mpi.getMimePart().getContentType());
                if (ct.getParameter("method") != null) {
                    return null;
                }
            } catch (MessagingException e) {
            }
        }

        Element el = parent.addNonUniqueElement(MailConstants.E_MIMEPART);
        MimePart mp = mpi.getMimePart();

        String part = mpi.getPartName();
        part = prefix + (prefix.isEmpty() || part.isEmpty() ? "" : ".") + part;
        el.addAttribute(MailConstants.A_PART, part);

        String fname = Mime.getFilename(mp);
        if (MimeConstants.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
            // the <shr> share info goes underneath the top-level <m>
            Element shr = root.addNonUniqueElement(MailConstants.E_SHARE_NOTIFICATION);
            try {
                addContent(shr, mpi, maxSize, defaultCharset);
            } catch (IOException e) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing share XML", e);
                } else {
                    LOG.warn("error writing body part", e);
                }
            } catch (MessagingException e) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing share XML", e);
                }
            }
        } else if (MimeConstants.CT_XML_ZIMBRA_DL_SUBSCRIPTION.equals(ctype)) {
            // the <dlSubs> dl subscription info goes underneath the top-level <m>
            Element dlSubs = root.addNonUniqueElement(MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION);
            try {
                addContent(dlSubs, mpi, maxSize, defaultCharset);
            } catch (IOException e) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing DL subscription", e);
                } else {
                    LOG.warn("error writing body part", e);
                }
            } catch (MessagingException e) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing DL subscription", e);
                }
            }
        } else if (MimeConstants.CT_TEXT_ENRICHED.equals(ctype)) {
            // we'll be replacing text/enriched with text/html
            ctype = MimeConstants.CT_TEXT_HTML;
        } else if (fname != null && (MimeConstants.CT_APPLICATION_OCTET_STREAM.equals(ctype) || MimeConstants.CT_APPLICATION_TNEF.equals(ctype))) {
            String guess = MimeDetect.getMimeDetect().detect(fname);
            if (guess != null) {
                ctype = guess;
            }
        }
        el.addAttribute(MailConstants.A_CONTENT_TYPE, ctype);

        if (mpi.isMultipart()) {
            return el; // none of the below stuff is relevant for a multipart, so just return now...
        }

        // figure out attachment size
        try {
            el.addAttribute(MailConstants.A_SIZE, Mime.getSize(mp));
        } catch (Exception e) { // don't put out size if we get exception
            ZimbraLog.mailbox.warn("Unable to determine MIME part size: %s", e.getMessage());
        }

        // figure out attachment disposition
        try {
            String disp = mp.getHeader("Content-Disposition", null);
            if (disp != null) {
                ContentDisposition cdisp = new ContentDisposition(MimeUtility.decodeText(disp));
                el.addAttribute(MailConstants.A_CONTENT_DISPOSITION, StringUtil.stripControlCharacters(cdisp.getDisposition()));
            }
        } catch (MessagingException e) {
        } catch (UnsupportedEncodingException e) {
        }

        // figure out attachment name
        try {
            if (fname == null && MimeConstants.CT_MESSAGE_RFC822.equals(ctype)) {
                // "filename" for attached messages is the Subject
                Object content = Mime.getMessageContent(mp);
                if (content instanceof MimeMessage) {
                    fname = Mime.getSubject((MimeMessage) content);
                }
            }
            if (!Strings.isNullOrEmpty(fname)) {
                el.addAttribute(MailConstants.A_CONTENT_FILENAME, StringUtil.stripControlCharacters(fname));
            }
        } catch (MessagingException me) {
        } catch (IOException ioe) {
        }

        // figure out content-id (used in displaying attached images)
        String cid = mpi.getContentID();
        if (cid != null) {
            el.addAttribute(MailConstants.A_CONTENT_ID, StringUtil.stripControlCharacters(cid));
        }
        // figure out content-location (used in displaying attached images)
        try {
            String cl = mp.getHeader("Content-Location", null);
            if (cl != null) {
                el.addAttribute(MailConstants.A_CONTENT_LOCATION, StringUtil.stripControlCharacters(cl));
            }
        } catch (MessagingException e) {
        }

        // include the part's content if this is the displayable "memo part",
        // or if it was requested to include all parts
        if (bodies == null || bodies.contains(mpi)) {
            if (bodies != null) {
                el.addAttribute(MailConstants.A_BODY, true);
            }

            try {
                addContent(el, mpi, maxSize, neuter, defaultCharset, wantContent);
            } catch (IOException e) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing part content", e);
                } else {
                    LOG.warn("error writing body part", e);
                }
            } catch (MessagingException me) {
                if (!swallowContentExceptions) {
                    throw ServiceException.FAILURE("error serializing part content", me);
                }
            }
        }

        return el;
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i>
     * @param elt  The element to add the <tt>&lt;content></tt> to.
     * @param mpi  The message part to extract the content from.
     * @param maxSize The maximum amount of content to inline (<=0 is unlimited).
     * @parame defaultCharset  The user's default charset preference.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging */
    private static void addContent(Element elt, MPartInfo mpi, int maxSize, String defaultCharset)
    throws IOException, MessagingException {
        addContent(elt, mpi, maxSize, true, defaultCharset);
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i>
     *
     * @param elt     The element to add the <tt>&lt;content></tt> to.
     * @param mpi     The message part to extract the content from.
     * @param maxSize The maximum number of characters to inline (<=0 is unlimited).
     * @param neuter  Whether to "neuter" image <tt>src</tt> attributes.
     * @parame defaultCharset  The user's default charset preference.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging
     * @see HtmlDefang#defang(String, boolean) */
    private static void addContent(Element elt, MPartInfo mpi, int maxSize, boolean neuter, String defaultCharset)
    throws IOException, MessagingException {
        addContent(elt, mpi, maxSize, neuter, defaultCharset, MsgContent.full);
    }

    /** Adds the decoded text content of a message part to the {@link Element}.
     *  The content is added as the value of a new <tt>&lt;content></tt>
     *  sub-element.  <i>Note: This method will only extract the content of a
     *  <b>text/*</b> or <b>xml/*</b> message part.</i>
     *
     * @param elt     The element to add the <tt>&lt;content></tt> to.
     * @param mpi     The message part to extract the content from.
     * @param maxSize The maximum number of characters to inline (<=0 is unlimited).
     * @param neuter  Whether to "neuter" image <tt>src</tt> attributes.
     * @parame defaultCharset  The user's default charset preference.
     * @throws MessagingException when message parsing or CTE-decoding fails
     * @throws IOException on error during parsing or defanging
     * @see HtmlDefang#defang(String, boolean) */
    private static void addContent(Element elt, MPartInfo mpi, int maxSize, boolean neuter, String defaultCharset, MsgContent wantContent)
    throws IOException, MessagingException {
        // TODO: support other parts
        String ctype = mpi.getContentType();
        if (!ctype.matches(MimeConstants.CT_TEXT_WILD) && !ctype.matches(MimeConstants.CT_XML_WILD)) {
            return;
        }
        MimePart mp = mpi.getMimePart();
        Mime.repairTransferEncoding(mp);

        String data = null;
        String originalContent = null;
        wantContent = wantContent == null ? MsgContent.full : wantContent;
        try {
            if (maxSize <= 0) {
                maxSize = (int) Provisioning.getInstance().getLocalServer().getMailContentMaxSize();
            } else {
                maxSize = Math.min(maxSize, (int) Provisioning.getInstance().getLocalServer().getMailContentMaxSize());
            }
        } catch (ServiceException e) {
            ZimbraLog.soap.warn("Unable to determine max content size", e);
        }

        boolean wasTruncated = false;
        if (ctype.equals(MimeConstants.CT_TEXT_HTML)) {
            String charset = mpi.getContentTypeParameter(MimeConstants.P_CHARSET);
            InputStream stream = null;
            StringWriter sw = new StringWriter();
            TruncatingWriter tw = null;
            Writer out = sw;
            if (maxSize > 0) {
                tw = new TruncatingWriter(sw, maxSize + 1);
                out = tw;
            }
            Reader reader = null;

            try {
                stream = mp.getInputStream();
                if (charset != null && !charset.trim().isEmpty()) {
                    // make sure to feed getTextReader() a full Content-Type header, not just the primary/subtype portion
                    reader = Mime.getTextReader(stream, mp.getContentType(), defaultCharset);
                    BrowserDefang defanger = DefangFactory.getDefanger(mp.getContentType());
                    defanger.defang(reader, neuter, out);
                    data = sw.toString();
                } else {
                    String cte = mp.getEncoding();
                    if (cte != null && !cte.trim().toLowerCase().equals(MimeConstants.ET_7BIT)) {
                        try {
                            DefangFactory.getDefanger(ctype).defang(stream, neuter, out);
                            data = sw.toString();
                        } catch (IOException e) {
                        }
                    }
                    if (data == null) {
                        reader = Mime.getTextReader(stream, mp.getContentType(), defaultCharset);
                        DefangFactory.getDefanger(ctype).defang(reader, neuter, out);
                        data = sw.toString();
                    }
                }
                if (wantContent.equals(MsgContent.original) || wantContent.equals(MsgContent.both)) {
                    originalContent = removeQuotedText(data, true);
                }
            } finally {
                if (tw != null) {
                    wasTruncated = tw.wasTruncated();
                }
                ByteUtil.closeStream(stream);
                Closeables.closeQuietly(reader);
            }
        } else if (ctype.equals(MimeConstants.CT_TEXT_ENRICHED)) {
            // Enriched text handling is a little funky because TextEnrichedHandler
            // doesn't use Reader and Writer.  As a result, we truncate
            // the source before converting to HTML.
            InputStream stream = mp.getInputStream();
            Reader reader = null;
            try {
                reader = Mime.getTextReader(stream, mp.getContentType(), defaultCharset);
                int maxChars = (maxSize > 0 ? maxSize + 1 : -1);
                String enriched = ByteUtil.getContent(reader, maxChars, false);
                if (enriched.length() == maxChars) {
                    // The normal check for truncated data won't work because
                    // the length of the converted text is different than the length
                    // of the source, so set the attr here.
                    wasTruncated = true;
                }
                if (wantContent.equals(MsgContent.original) || wantContent.equals(MsgContent.both)) {
                    originalContent = TextEnrichedHandler.convertToHTML(removeQuotedText(enriched,
                        false));
                }
                data = TextEnrichedHandler.convertToHTML(enriched);
            } finally {
                ByteUtil.closeStream(stream);
                Closeables.closeQuietly(reader);
            }
        } else {
            InputStream stream = mp.getInputStream();
            Reader reader = null;
            try {
                reader = Mime.getTextReader(stream, mp.getContentType(), defaultCharset);
                int maxChars = (maxSize > 0 ? maxSize + 1 : -1);
                data = ByteUtil.getContent(reader, maxChars, false);
                if (wantContent.equals(MsgContent.original) || wantContent.equals(MsgContent.both)) {
                    originalContent = removeQuotedText(data, false);
                }
                if (data.length() == maxChars) {
                    wasTruncated = true;
                }
            } finally {
                ByteUtil.closeStream(stream);
                Closeables.closeQuietly(reader);
            }
        }

        if (data != null && !wantContent.equals(MsgContent.original)) {
            data = StringUtil.stripControlCharacters(data);
            if (wasTruncated) {
                elt.addAttribute(MailConstants.A_TRUNCATED_CONTENT, true);
                if (data.length() > maxSize) {
                    data = data.substring(0, maxSize);
                }
            }
            elt.addAttribute(MailConstants.E_CONTENT, data, Element.Disposition.CONTENT);
        }
        if (originalContent != null && (wantContent.equals(MsgContent.original) || wantContent.equals(MsgContent.both))) {
            originalContent = StringUtil.stripControlCharacters(originalContent);
            elt.addUniqueElement(MailConstants.E_ORIG_CONTENT).setText(originalContent);
        }
        // TODO: CDATA worth the effort?
    }


    /**
     * @param data
     * @return
     */
    private static String removeQuotedText(String data, boolean isHtml) {
        return data != null ? new QuotedTextUtil().getOriginalContent(data, isHtml) : data;
    }

    public enum EmailType {
        NONE(null), FROM("f"), TO("t"), CC("c"), BCC("b"), REPLY_TO("r"), SENDER("s"), READ_RECEIPT("n"), RESENT_FROM("rf");

        private final String rep;
        private EmailType(String c) {
            rep = c;
        }

        @Override
        public String toString() {
            return rep;
        }
    }

    private static void addEmails(Element m, InternetAddress[] recipients, EmailType emailType) {
        for (InternetAddress rcpt : recipients) {
            encodeEmail(m, rcpt, emailType);
        }
    }

    public static Element encodeEmail(Element parent, InternetAddress ia, EmailType type) {
        return encodeEmail(parent, new ParsedAddress(ia).parse(), type);
    }

    public static Element encodeEmail(Element parent, String addr, EmailType type) {
        return encodeEmail(parent, new ParsedAddress(addr).parse(), type);
    }

    public static Element encodeEmail(Element parent, ParsedAddress pa, EmailType type) {
        Element el = parent.addNonUniqueElement(MailConstants.E_EMAIL);
        if(!StringUtil.isNullOrEmpty(pa.emailPart)) {
            pa.emailPart = ZInternetHeader.decode(pa.emailPart);
            pa.emailPart = StringUtil.sanitizeString(pa.emailPart);
        }
        el.addAttribute(MailConstants.A_ADDRESS, IDNUtil.toUnicode(pa.emailPart));
        if(!StringUtil.isNullOrEmpty(pa.firstName)) {
            pa.firstName = ZInternetHeader.decode(pa.firstName);
            pa.firstName = StringUtil.sanitizeString(pa.firstName);
        }
        el.addAttribute(MailConstants.A_DISPLAY, pa.firstName);
        if (!StringUtil.isNullOrEmpty(pa.personalPart)) {
            pa.personalPart = ZInternetHeader.decode(pa.personalPart);
            pa.personalPart = StringUtil.sanitizeString(pa.personalPart);
        }
        el.addAttribute(MailConstants.A_PERSONAL, pa.personalPart);
        el.addAttribute(MailConstants.A_ADDRESS_TYPE, type.toString());
        return el;
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, OperationContext octxt, WikiItem wiki) throws ServiceException {
        return encodeWiki(parent, ifmt, octxt, wiki, NOTIFY_FIELDS);
    }

    public static Element encodeWiki(Element parent, ItemIdFormatter ifmt, OperationContext octxt, WikiItem wiki, int fields)
    throws ServiceException {
        Element elem = parent.addNonUniqueElement(MailConstants.E_WIKIWORD);
        encodeDocumentCommon(elem, ifmt, octxt, wiki, fields);
        return elem;
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Document doc) throws ServiceException {
        return encodeDocument(parent, ifmt, octxt, doc, Change.ALL_FIELDS);
    }

    public static Element encodeDocument(Element parent, ItemIdFormatter ifmt, OperationContext octxt, Document doc,
            int fields) throws ServiceException {
        Element el = parent.addNonUniqueElement(MailConstants.E_DOC);
        encodeDocumentCommon(el, ifmt, octxt, doc, fields);
        if (needToOutput(fields, Change.LOCK)) {
            String lockOwner = doc.getLockOwner();
            if (lockOwner == null) {
                el.addAttribute(MailConstants.A_LOCKOWNER_ID, "");
            } else {
                Account a;
                try {
                    a = Provisioning.getInstance().getAccountById(lockOwner);
                    if (a != null) {
                        el.addAttribute(MailConstants.A_LOCKOWNER_EMAIL, a.getName());
                    } else {
                        ZimbraLog.soap.warn("lock owner not found: %s", lockOwner);
                    }
                } catch (ServiceException e) {
                    ZimbraLog.soap.warn("can't lookup lock owner", e);
                }
                el.addAttribute(MailConstants.A_LOCKOWNER_ID, lockOwner);
                el.addAttribute(MailConstants.A_LOCKTIMESTAMP, doc.getLockTimestamp());
            }
        }
        return el;
    }

    public static Element encodeDocumentCommon(Element m, ItemIdFormatter ifmt, OperationContext octxt, Document doc, int fields)
    throws ServiceException {
        m.addAttribute(MailConstants.A_ID, ifmt.formatItemId(doc));
        m.addAttribute(MailConstants.A_UUID, doc.getUuid());
        if (needToOutput(fields, Change.NAME)) {
            m.addAttribute(MailConstants.A_NAME, doc.getName());
        }
        if (needToOutput(fields, Change.SIZE)) {
            m.addAttribute(MailConstants.A_SIZE, doc.getSize());
        }
        if (needToOutput(fields, Change.DATE)) {
            m.addAttribute(MailConstants.A_DATE, doc.getDate());
        }
        if (needToOutput(fields, Change.FOLDER)) {
            m.addAttribute(MailConstants.A_FOLDER,
                    ifmt.formatItemId(new ItemId(doc.getMailbox().getAccountId(), doc.getFolderId())));
            m.addAttribute(MailConstants.A_FOLDER_UUID, doc.getFolderUuid());
        }
        if (needToOutput(fields, Change.CONFLICT)) {
            m.addAttribute(MailConstants.A_MODIFIED_SEQUENCE, doc.getModifiedSequence());
            m.addAttribute(MailConstants.A_METADATA_VERSION, doc.getMetadataVersion());
            m.addAttribute(MailConstants.A_CHANGE_DATE, (doc.getChangeDate() / 1000));
            m.addAttribute(MailConstants.A_REVISION, doc.getSavedSequence());
        }
        recordItemTags(m, doc, octxt, fields | Change.FLAGS);
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(m, doc, fields);
            String description = doc.getDescription();
            if (!Strings.isNullOrEmpty(description)) {
                m.addAttribute(MailConstants.A_DESC, description);
            }
            m.addAttribute(MailConstants.A_CONTENT_TYPE, doc.getContentType());
            m.addAttribute(MailConstants.A_DESC_ENABLED, doc.isDescriptionEnabled());
        }

        if (needToOutput(fields, Change.CONTENT) || needToOutput(fields, Change.NAME)) {
            try {
                m.addAttribute(MailConstants.A_VERSION, doc.getVersion());
                m.addAttribute(MailConstants.A_LAST_EDITED_BY, doc.getCreator());
                String fragment = doc.getFragment();
                if (!Strings.isNullOrEmpty(fragment)) {
                    m.addAttribute(MailConstants.E_FRAG, fragment, Element.Disposition.CONTENT);
                }
                Document revision = null;
                int v = 1;
                while (revision == null && v <= doc.getVersion()) {
                    revision = (Document) doc.getMailbox().getItemRevision(octxt, doc.getId(), doc.getType(), v++, doc.inDumpster());
                }
                if (revision != null) {
                    m.addAttribute(MailConstants.A_CREATOR, revision.getCreator());
                    m.addAttribute(MailConstants.A_CREATED_DATE, revision.getDate());
                }
            } catch (Exception e) {
                LOG.warn("ignoring exception while fetching revision for document %s", doc.getSubject(), e);
            }
        }

        // return ACLs when they are set
        if (needToOutput(fields, Change.ACL)) {
            if (fields != NOTIFY_FIELDS || doc.isTagged(Flag.FlagInfo.NO_INHERIT)) {
                encodeACL(octxt, m, doc.getEffectiveACL(), false);
            }
        }
        for (ToXMLExtension ext : extensions) {
            ext.encodeDocumentAdditionalAttribute(m, ifmt, octxt, doc, fields);
        }
        return m;
    }

    public static Element encodeDataSource(Element parent, DataSource ds) {
        Element m = parent.addNonUniqueElement(getDsType(ds));
        m.addAttribute(MailConstants.A_ID, ds.getId());
        m.addAttribute(MailConstants.A_NAME, ds.getName());
        m.addAttribute(MailConstants.A_FOLDER, ds.getFolderId());
        m.addAttribute(MailConstants.A_DS_IS_ENABLED, ds.isEnabled());
        m.addAttribute(MailConstants.A_DS_IS_IMPORTONLY, ds.isImportOnly());

        if (ds.getType() == DataSourceType.pop3) {
            m.addAttribute(MailConstants.A_DS_LEAVE_ON_SERVER, ds.leaveOnServer());
        }
        if (ds.getHost() != null) {
            m.addAttribute(MailConstants.A_DS_HOST, ds.getHost());
        }
        if (ds.getPort() != null) {
            m.addAttribute(MailConstants.A_DS_PORT, ds.getPort());
        }
        if (ds.getConnectionType() != null) {
            m.addAttribute(MailConstants.A_DS_CONNECTION_TYPE, ds.getConnectionType().name());
        }
        if (ds.getUsername() != null) {
            m.addAttribute(MailConstants.A_DS_USERNAME, ds.getUsername());
        }
        try {
            if (ds.getPollingInterval() > 0) {
                m.addAttribute(MailConstants.A_DS_POLLING_INTERVAL, ds.getPollingInterval());
            }
        } catch (ServiceException e) {
            LOG.warn("Unable to get polling interval from %s", ds, e);
        }

        if (ds.getAttr(Provisioning.A_zimbraDataSourceSmtpEnabled) != null) {
            m.addAttribute(MailConstants.A_DS_SMTP_ENABLED, ds.isSmtpEnabled());
        }
        if (ds.getSmtpHost() != null) {
            m.addAttribute(MailConstants.A_DS_SMTP_HOST, ds.getSmtpHost());
        }
        if (ds.getSmtpPort() != null) {
            m.addAttribute(MailConstants.A_DS_SMTP_PORT, ds.getSmtpPort());
        }
        if (ds.getAttr(Provisioning.A_zimbraDataSourceSmtpConnectionType) != null) {
            // TODO - Fix hard coded strings
            m.addAttribute(MailConstants.A_DS_SMTP_CONNECTION_TYPE, ds.isSmtpConnectionSecure() ? "ssl" : "cleartext");
        }
        if (ds.getAttr(Provisioning.A_zimbraDataSourceSmtpAuthRequired) != null) {
            m.addAttribute(MailConstants.A_DS_SMTP_AUTH_REQUIRED, ds.isSmtpAuthRequired());
        }
        if (ds.getSmtpUsername() != null) {
            m.addAttribute(MailConstants.A_DS_SMTP_USERNAME, ds.getSmtpUsername());
        }
        if(ds.getDataSourceImportClassName() != null) {
            m.addAttribute(MailConstants.A_DS_IMPORT_CLASS, ds.getDataSourceImportClassName());
        }
        if(ds.getOauthRefreshToken() != null) {
            m.addAttribute(MailConstants.A_DS_REFRESH_TOKEN, ds.getOauthRefreshToken());
        }
        if(ds.getOauthRefreshTokenUrl() != null) {
            m.addAttribute(MailConstants.A_DS_REFRESH_TOKEN_URL, ds.getOauthRefreshTokenUrl());
        }
        m.addAttribute(MailConstants.A_DS_EMAIL_ADDRESS, ds.getEmailAddress());
        m.addAttribute(MailConstants.A_DS_USE_ADDRESS_FOR_FORWARD_REPLY, ds.useAddressForForwardReply());
        m.addAttribute(MailConstants.A_DS_DEFAULT_SIGNATURE, ds.getDefaultSignature());
        m.addAttribute(MailConstants.A_DS_FORWARD_REPLY_SIGNATURE, ds.getForwardReplySignature());
        m.addAttribute(MailConstants.A_DS_FROM_DISPLAY, ds.getFromDisplay());
        m.addAttribute(MailConstants.A_DS_REPLYTO_ADDRESS, ds.getReplyToAddress());
        m.addAttribute(MailConstants.A_DS_REPLYTO_DISPLAY, ds.getReplyToDisplay());

        Date date = ds.getGeneralizedTimeAttr(Provisioning.A_zimbraDataSourceFailingSince, null);
        if (date != null) {
            m.addAttribute(MailConstants.A_DS_FAILING_SINCE, date.getTime() / 1000);
        }

        String lastError = ds.getAttr(Provisioning.A_zimbraDataSourceLastError);
        if (lastError != null) {
            m.addNonUniqueElement(MailConstants.E_DS_LAST_ERROR).setText(lastError);
        }
        return m;
    }

    private static String getDsType(DataSource ds) {
        switch (ds.getType()) {
            case imap:
                return MailConstants.E_DS_IMAP;
            case pop3:
                return MailConstants.E_DS_POP3;
            case caldav:
                return MailConstants.E_DS_CALDAV;
            case yab:
                return MailConstants.E_DS_YAB;
            case rss:
                return MailConstants.E_DS_RSS;
            case gal:
                return MailConstants.E_DS_GAL;
            case cal:
                return MailConstants.E_DS_CAL;
            default:
                return MailConstants.E_DS_UNKNOWN;
        }
    }

    private static void setCalendarItemType(Element elem, MailItem.Type type) {
        elem.addAttribute(MailConstants.A_CAL_ITEM_TYPE, type == MailItem.Type.APPOINTMENT ? "appt" : "task");
    }

    /**
     * @return the next alarm time or Long.MAX_VALUE if there isn't one
     */
    private static long getNextAlarmTime(CalendarItem calItem) {
        AlarmData alarmData = calItem.getAlarmData();
        if (alarmData == null) {
            return Long.MAX_VALUE;
        }
        return alarmData.getNextAt();
    }

    public static void encodeAlarmTimes(Element elem, CalendarItem calItem) {
        long nextAlarm = getNextAlarmTime(calItem);
        if (nextAlarm < Long.MAX_VALUE) {
            elem.addAttribute(MailConstants.A_CAL_NEXT_ALARM, nextAlarm);
        }
    }

    public static AlarmDataInfo alarmDataToJaxb(CalendarItem calItem, AlarmData alarmData) {
        AlarmDataInfo alarm = new AlarmDataInfo();
        long nextAlarm = getNextAlarmTime(calItem);
        if (nextAlarm < Long.MAX_VALUE) {
            alarm.setNextAlarm(nextAlarm);
        }
        long alarmInstStart = alarmData.getNextInstanceStart();
        if (alarmInstStart != 0) {
            alarm.setAlarmInstanceStart(alarmInstStart);
        }
        int alarmInvId = alarmData.getInvId();
        int alarmCompNum = alarmData.getCompNum();
        Invite alarmInv = calItem.getInvite(alarmInvId, alarmCompNum);
        if (alarmInv != null) {
            // Some info on the meeting instance the reminder is for.
            // These allow the UI to display tooltip and issue a Get
            // call on the correct meeting instance.
            alarm.setName(alarmInv.getName());
            alarm.setLocation(alarmInv.getLocation());
            alarm.setInvId(alarmInvId);
            alarm.setComponentNum(alarmCompNum);
        }
        Alarm alarmObj = alarmData.getAlarm();
        if (alarmObj != null) {
            alarm.setAlarm(alarmObj.toJaxb());
        }
        return alarm;
    }

    public static Element encodeAlarmData(Element parent, CalendarItem calItem, AlarmData alarmData) {
        Element alarmElem = parent.addNonUniqueElement(MailConstants.E_CAL_ALARM_DATA);
        encodeAlarmTimes(alarmElem, calItem);
        // Start time of the meeting instance we're reminding about.
        long alarmInstStart = alarmData.getNextInstanceStart();
        if (alarmInstStart != 0) {
            alarmElem.addAttribute(MailConstants.A_CAL_ALARM_INSTANCE_START, alarmInstStart);
        }
        int alarmInvId = alarmData.getInvId();
        int alarmCompNum = alarmData.getCompNum();
        Invite alarmInv = calItem.getInvite(alarmInvId, alarmCompNum);
        if (alarmInv != null) {
            // Some info on the meeting instance the reminder is for.
            // These allow the UI to display tooltip and issue a Get
            // call on the correct meeting instance.
            alarmElem.addAttribute(MailConstants.A_NAME, alarmInv.getName());
            alarmElem.addAttribute(MailConstants.A_CAL_LOCATION, alarmInv.getLocation());
            alarmElem.addAttribute(MailConstants.A_CAL_INV_ID, alarmInvId);
            alarmElem.addAttribute(MailConstants.A_CAL_COMPONENT_NUM, alarmCompNum);
        }
        Alarm alarmObj = alarmData.getAlarm();
        if (alarmObj != null) {
            alarmObj.toXml(alarmElem);
        }
        return alarmElem;
    }

    public static Element encodeFreeBusy(Element parent, FreeBusy fb) {
        Element resp = parent.addNonUniqueElement(MailConstants.E_FREEBUSY_USER);
        resp.addAttribute(MailConstants.A_ID, fb.getName());
        for (Iterator<FreeBusy.Interval> iter = fb.iterator(); iter.hasNext(); ) {
            FreeBusy.Interval cur = iter.next();

            String status = cur.getStatus();
            Element elt;
            if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
                elt = resp.addNonUniqueElement(MailConstants.E_FREEBUSY_FREE);
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
                elt = resp.addNonUniqueElement(MailConstants.E_FREEBUSY_BUSY);
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
                elt = resp.addNonUniqueElement(MailConstants.E_FREEBUSY_BUSY_TENTATIVE);
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
                elt = resp.addNonUniqueElement(MailConstants.E_FREEBUSY_BUSY_UNAVAILABLE);
            } else if (status.equals(IcalXmlStrMap.FBTYPE_NODATA)) {
                elt = resp.addNonUniqueElement(MailConstants.E_FREEBUSY_NODATA);
            } else {
                assert(false);
                elt = null;
            }

            if (elt != null) {
                elt.addAttribute(MailConstants.A_CAL_START_TIME, cur.getStart());
                elt.addAttribute(MailConstants.A_CAL_END_TIME, cur.getEnd());
                if (cur.isDetailsExist()) {
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ID,cur.getId());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_SUBJECT, cur.getSubject());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_LOCATION, cur.getLocation());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ISMEETING, cur.isMeeting());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ISPRIVATE, cur.isPrivate());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ISRECURRING, cur.isRecurring());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ISREMINDERSET, cur.isReminderSet());
                    elt.addAttribute(MailConstants.E_CAL_EVENT_ISEXCEPTION, cur.isException());
                } 
                if(!cur.isHasPermission()) {
                    elt.addAttribute(MailConstants.E_CAL_EVENT_HASPERMISSION, cur.isHasPermission());
                }
                
            }
        }

        return resp;
    }

    private static Element encodeRecurId(Element parent, RecurId recurId, boolean allDay) {
        ParsedDateTime ridDt = recurId.getDt();
        Element ridElem = parent.addNonUniqueElement(MailConstants.E_CAL_EXCEPTION_ID);
        ridElem.addAttribute(MailConstants.A_CAL_DATETIME, ridDt.getDateTimePartString(false));
        if (!allDay) {
            ridElem.addAttribute(MailConstants.A_CAL_TIMEZONE, ridDt.getTZName());
        }
        int rangeType = recurId.getRange();
        if (rangeType != RecurId.RANGE_NONE) {
            ridElem.addAttribute(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, rangeType);
        }
        return ridElem;
    }

    private static Element encodeDtStart(Element parent, ParsedDateTime dtStart, boolean allDay, boolean forceUtc) {
        if (forceUtc) {
            dtStart = (ParsedDateTime) dtStart.clone();
            dtStart.toUTC();
        }
        // fixup for bug 30121
        if (allDay && dtStart.hasTime()) {
            // If this is supposed to be an all-day event but DTSTART has time part, clear the time part.
            dtStart = (ParsedDateTime) dtStart.clone();
            dtStart.setHasTime(false);
        } else if (!allDay && !dtStart.hasTime()){
            // If the event isn't marked all-day but DTSTART has no time part, mark the event all-day.
            allDay = true;
        }
        Element dtStartElem = parent.addNonUniqueElement(MailConstants.E_CAL_START_TIME);
        dtStartElem.addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString(false));
        if (!allDay) {
            String tzName = dtStart.getTZName();
            if (tzName != null) {
                dtStartElem.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
            }
            dtStartElem.addAttribute(MailConstants.A_CAL_DATETIME_UTC, dtStart.getUtcTime());
        }
        return dtStartElem;
    }

    private static Element encodeDtEnd(Element parent, ParsedDateTime dtEnd, boolean allDay, boolean isTodo, boolean forceUtc) {
        if (forceUtc) {
            dtEnd = (ParsedDateTime) dtEnd.clone();
            dtEnd.toUTC();
        }
        // fixup for bug 30121
        if (allDay && dtEnd.hasTime()) {
            // If this is supposed to be an all-day event but DTEND has time part, clear the time part.
            dtEnd = (ParsedDateTime) dtEnd.clone();
            dtEnd.setHasTime(false);
        } else if (!allDay && !dtEnd.hasTime()) {
            // If the event isn't marked all-day but DTEND has no time part, mark the event all-day.
            allDay = true;
        }
        Element dtEndElem = parent.addNonUniqueElement(MailConstants.E_CAL_END_TIME);
        if (!allDay) {
            String tzName = dtEnd.getTZName();
            if (tzName != null) {
                dtEndElem.addAttribute(MailConstants.A_CAL_TIMEZONE, tzName);
            }
            dtEndElem.addAttribute(MailConstants.A_CAL_DATETIME_UTC, dtEnd.getUtcTime());
        } else {
            if (!isTodo) {
                // See CalendarUtils.parseInviteElementCommon, where we parse DTEND
                // for a description of why we add -1d when sending to the client
                dtEnd = dtEnd.add(ParsedDuration.NEGATIVE_ONE_DAY);
            }
        }
        dtEndElem.addAttribute(MailConstants.A_CAL_DATETIME, dtEnd.getDateTimePartString(false));
        return dtEndElem;
    }

    public static void encodeCalendarItemRecur(Element parent, CalendarItem calItem) {
        TimeZoneMap tzmap = calItem.getTimeZoneMap();
        encodeTimeZoneMap(parent, tzmap);
        Invite[] invites = calItem.getInvites();
        for (Invite inv : invites) {
            String elemName;
            if (inv.isCancel()) {
                elemName = MailConstants.E_CAL_CANCEL;
            } else if (inv.hasRecurId()) {
                elemName = MailConstants.E_CAL_EXCEPT;
            } else {
                elemName = MailConstants.E_INVITE_COMPONENT;
            }
            Element compElem = parent.addNonUniqueElement(elemName);
            boolean allDay = inv.isAllDayEvent();
            // RECURRENCE-ID
            if (inv.hasRecurId())
                encodeRecurId(compElem, inv.getRecurId(), allDay);
            if (!inv.isCancel()) {
                // DTSTART
                ParsedDateTime dtStart = inv.getStartTime();
                if (dtStart != null)
                    encodeDtStart(compElem, dtStart, allDay, false);
                // DTEND or DURATION
                ParsedDateTime dtEnd = inv.getEndTime();
                if (dtEnd != null) {
                    encodeDtEnd(compElem, dtEnd, allDay, inv.isTodo(), false);
                } else {
                    ParsedDuration dur = inv.getDuration();
                    if (dur != null)
                        dur.toXml(compElem);
                }

                // recurrence definition
                IRecurrence recurrence = inv.getRecurrence();
                if (recurrence != null) {
                    Element recurElem = compElem.addNonUniqueElement(MailConstants.E_CAL_RECUR);
                    recurrence.toXml(recurElem);
                }
            }
        }
    }

    public static Element encodeGalContact(Element response, GalContact contact) {
        return encodeGalContact(response, contact, null);
    }

    public static Element encodeGalContact(Element response, GalContact contact,
        Collection<String> returnAttrs) {
        return encodeGalContact(response, contact, returnAttrs, true);
    }

    public static Element encodeGalContact(Element response, GalContact contact,
            Collection<String> returnAttrs, boolean canExpand) {
        Element cn = response.addNonUniqueElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            String key = entry.getKey();
            if (ContactConstants.A_member.equals(key)) {
                if (!canExpand) {
                    continue;
                }
            }
            if (returnAttrs == null || returnAttrs.contains(key)) {
                Object value = entry.getValue();
                if (value instanceof String[]) {
                    String sa[] = (String[]) value;
                    for (int i = 0; i < sa.length; i++)
                        cn.addKeyValuePair(key, sa[i], MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
                } else {
                    cn.addKeyValuePair(key, (String) value, MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
                }
            }
        }
        return cn;
    }

    private static void encodeAddrsWithGroupInfo(Element eMsg, Account requestedAcct, Account authedAcct) {
        encodeAddrsWithGroupInfo(eMsg, MailConstants.E_EMAIL, requestedAcct, authedAcct);
        Element eInvite = eMsg.getOptionalElement(MailConstants.E_INVITE);
        if (eInvite != null) {
            Element eComp = eInvite.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
            if (eComp != null) {
                encodeAddrsWithGroupInfo(eComp, MailConstants.E_CAL_ATTENDEE, requestedAcct, authedAcct);
            }
        }
    }

    private static void encodeAddrsWithGroupInfo(Element eParent,
            String emailElem, Account requestedAcct, Account authedAcct) {
        if (requestedAcct.isFeatureGalEnabled()
                && requestedAcct.isFeatureGalAutoCompleteEnabled()
                && requestedAcct.isPrefGalAutoCompleteEnabled()) {
            GalGroupInfoProvider groupInfoProvider = GalGroupInfoProvider.getInstance();

            for (Element eEmail : eParent.listElements(emailElem)) {
                String addr = eEmail.getAttribute(MailConstants.A_ADDRESS, null);
                if (addr != null) {
                    // shortcut the check if the email address is the authed or requested account - it cannot be a group
                    if (addr.equalsIgnoreCase(requestedAcct.getName()) || addr.equalsIgnoreCase(authedAcct.getName()))
                        continue;

                    GroupInfo groupInfo = groupInfoProvider.getGroupInfo(addr, true, requestedAcct, authedAcct);
                    if (GroupInfo.IS_GROUP == groupInfo) {
                        eEmail.addAttribute(MailConstants.A_IS_GROUP, true);
                        eEmail.addAttribute(MailConstants.A_EXP, false);
                    } else if (GroupInfo.CAN_EXPAND == groupInfo) {
                        eEmail.addAttribute(MailConstants.A_IS_GROUP, true);
                        eEmail.addAttribute(MailConstants.A_EXP, true);
                    }
                }
            }
        } else if (ZimbraLog.gal.isTraceEnabled()) {
            ZimbraLog.gal.trace("group info bypassed; account [%s] does not have required attributes isFeatureGalEnabled [%s]"
                    + " isFeatureGalAutoCompleteEnabled [%s] isPrefGalAutoCompleteEnabled [%s]", requestedAcct,
                    requestedAcct.isFeatureGalEnabled(), requestedAcct.isFeatureGalAutoCompleteEnabled(), requestedAcct.isPrefGalAutoCompleteEnabled());
        }
    }

    public static Element encodeComment(Element response, ItemIdFormatter ifmt, OperationContext octxt, Comment comment)
    throws ServiceException {
        return encodeComment(response, ifmt, octxt, comment, NOTIFY_FIELDS);
    }

    public static Element encodeComment(Element response, ItemIdFormatter ifmt, OperationContext octxt, Comment comment,
            int fields) throws ServiceException {
        Element c = response.addNonUniqueElement(MailConstants.E_COMMENT);
        if (needToOutput(fields, Change.PARENT)) {
            c.addAttribute(MailConstants.A_PARENT_ID, ifmt.formatItemId(comment.getParentId()));
        }
        if (needToOutput(fields, Change.SUBJECT)) {
            c.setText(comment.getText());
        }
        c.addAttribute(MailConstants.A_ID, ifmt.formatItemId(comment));
        c.addAttribute(MailConstants.A_UUID, comment.getUuid());
        try {
            Account a = comment.getCreatorAccount();
            if (a != null) {
                c.addAttribute(MailConstants.A_EMAIL, a.getName());
            }
        } catch (ServiceException e) {
        }
        recordItemTags(c, comment, octxt, fields);
        encodeColor(c, comment, fields);
        if (needToOutput(fields, Change.DATE)) {
            c.addAttribute(MailConstants.A_DATE, comment.getDate());
        }
        if (needToOutput(fields, Change.METADATA)) {
            encodeAllCustomMetadata(c, comment, fields);
        }
        return c;
    }

    public static Element encodeLink(Element response, ItemIdFormatter ifmt, Link link, int fields) {
        Element el = response.addNonUniqueElement(MailConstants.E_LINK);
        el.addAttribute(MailConstants.A_ID, ifmt.formatItemId(link));
        el.addAttribute(MailConstants.A_UUID, link.getUuid());

        if (needToOutput(fields, Change.NAME)) {
            String name = link.getName();
            if (!Strings.isNullOrEmpty(name)) {
                el.addAttribute(MailConstants.A_NAME, name);
            }
        }
        if (needToOutput(fields, Change.CONTENT)) {
            el.addAttribute(MailConstants.A_ZIMBRA_ID, link.getOwnerId());
            el.addAttribute(MailConstants.A_REMOTE_ID, link.getRemoteId());
            NamedEntry nentry = FolderAction.lookupGranteeByZimbraId(link.getOwnerId(), ACL.GRANTEE_USER);
            el.addAttribute(MailConstants.A_OWNER_NAME, nentry == null ? null : nentry.getName());
        }
        return el;
    }

    public interface ToXMLExtension {
        Element encodeDocumentAdditionalAttribute(Element elem, ItemIdFormatter ifmt, OperationContext octxt,
                Document doc, int fields);
    }

    private static Set<ToXMLExtension> extensions = new HashSet<ToXMLExtension>();

    public static void addExtension(ToXMLExtension e) {
        extensions.add(e);
    }
}
