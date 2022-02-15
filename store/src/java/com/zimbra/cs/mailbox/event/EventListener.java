/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
package com.zimbra.cs.mailbox.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.share.ShareNotification;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailboxListener;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.PendingModifications;
import com.zimbra.cs.session.PendingModifications.ModificationKey;

public class EventListener extends MailboxListener {

    public static final ImmutableSet<MailboxOperation> EVENTS = ImmutableSet.of(
            MailboxOperation.CreateFolder, MailboxOperation.RenameFolder,
            MailboxOperation.MoveItem, MailboxOperation.CopyItem,
            MailboxOperation.DeleteItem, MailboxOperation.EmptyFolder,
            MailboxOperation.GrantAccess, MailboxOperation.RevokeAccess, MailboxOperation.ExpireAccess,
            MailboxOperation.SaveDocument, MailboxOperation.AddDocumentRevision,
            MailboxOperation.RenameItem, MailboxOperation.PurgeRevision,
            MailboxOperation.RenameItemPath, MailboxOperation.RenameFolderPath,
            MailboxOperation.AlterItemTag, // for marking notification read
            MailboxOperation.LockItem, MailboxOperation.UnlockItem,
            MailboxOperation.CreateComment, MailboxOperation.CreateMountpoint,
            MailboxOperation.CreateMessage
    );

    public static final ImmutableSet<MailboxOperation> FOLDER_EVENTS = ImmutableSet.of(
            MailboxOperation.CreateFolder, MailboxOperation.RenameFolder,
            MailboxOperation.RenameFolderPath, MailboxOperation.RenameItem,
            MailboxOperation.MoveItem,
            MailboxOperation.GrantAccess, MailboxOperation.RevokeAccess, MailboxOperation.ExpireAccess,
            MailboxOperation.LockItem, MailboxOperation.UnlockItem, MailboxOperation.DeleteItem
    );

    public static final ImmutableSet<Type> ITEMTYPES = ImmutableSet.of(
            Type.DOCUMENT, Type.FOLDER, Type.MOUNTPOINT, Type.COMMENT, Type.MESSAGE
    );

    private final EventLogger logger;

    private static Log LOG = LogFactory.getLog(EventListener.class);

    public EventListener() {
        logger = EventLogger.getInstance();
    }

    @Override
    public void notify(ChangeNotification notification) {
        if (notification == null)
            return;

        if ((notification.ctxt != null) && (notification.ctxt.getAuthenticatedUser() == null))
            return;

        if ((notification.ctxt == null) && (notification.mailboxAccount == null))
            return;

        String userAgent = (notification.ctxt != null) ? notification.ctxt.getUserAgent() : null;

        String accountId = (notification.ctxt != null) ?
            notification.ctxt.getAuthenticatedUser().getId() :
            notification.mailboxAccount.getId();

        ItemIdFormatter ifmt = new ItemIdFormatter(accountId);
        MailboxOperation op = notification.op;
        if (notification.mods.modified != null) {
            for (PendingModifications.Change change : notification.mods.modified.values()) {
                if (wantThisChange(change, op)) {
                    handleChange((MailItem)change.what, op, accountId, notification.timestamp, (MailItem)change.preModifyObj, userAgent, getArgs(op, (MailItem)change.what, (MailItem)change.preModifyObj, ifmt));
                }
            }
        }
        if (notification.mods.created != null) {
            for (Map.Entry<ModificationKey, BaseItemInfo> entry: notification.mods.created.entrySet()) {
                if (entry instanceof MailItem) {
                    MailItem mailItem = (MailItem) entry;
                    if (!ITEMTYPES.contains(mailItem.getType()))
                        continue;
                    handleChange(mailItem, op, accountId, mailItem.getDate(), null, userAgent, getArgs(op, mailItem, null, ifmt));
                }
            }
        }
        if (notification.mods.deleted != null) {
            for (PendingModifications.Change change : notification.mods.deleted.values()) {
                if (wantThisChange(change, op)) {
                    MailItem item = (MailItem) change.preModifyObj;
                    if (item != null) {
                        handleChange(item, op, accountId, notification.timestamp, (MailItem)change.preModifyObj, userAgent, getArgs(op, item, (MailItem)change.preModifyObj, ifmt));
                    }
                }
            }
        }
    }

    private static final String ARG_TYPE = "t";
    private static final String ARG_VER = "ver";
    private static final String ARG_OLDNAME = "oldName";
    private static final String ARG_NAME = "filename";
    private static final String ARG_OLDLOCATION = "oldLocation";
    private static final String ARG_NEWLOCATION = "newLocation";
    private static final String ARG_TARGET = "target";
    private static final String ARG_ROLE = "role";
    private static final String ARG_PARENTID = "parentId";
    private static final String ARG_TEXT = "text";
    private static final String ARG_FROM = "from";
    private static final String ARG_SUBJECT = "subject";
    private static final String ARG_SHARED_ITEM_ID = "sharedItemId";
    private static final String ARG_SHARED_ITEM_NAME = "sharedItemName";
    private static final String ARG_SHARED_ITEM_VIEW = "sharedItemView";
    private static final String ARG_SHARE_GRANTEE_ID = "granteeId";
    private static final String ARG_SHARE_PERMISSIONS = "permissions";
    private static final String ARG_OLD_FOLDER_ID = "oldFolderId";
    private static final String ARG_NEW_FOLDER_ID = "newFolderId";
    private static final String ARG_MESSAGE_READ = "msgRead";  // boolean

    private static final String AUTHUSER = "Authuser";
    private static final String PUBLIC = "public";
    private static final String ADMIN = "Admin";
    private static final String RW = "RW";
    private static final String READ = "Read";

    public static boolean wantThisChange(PendingModifications.Change change, MailboxOperation op) {
        if (!EVENTS.contains(op)) {
            return false;
        }
        if (op == MailboxOperation.DeleteItem && change.what instanceof MailItem.Type) {
            return ITEMTYPES.contains(change.what);
        }
        if (!(change.what instanceof MailItem)) {
            return false;
        }
        if (!ITEMTYPES.contains(((MailItem)change.what).getType())) {
            return false;
        }
        if ((change.what instanceof Folder && (change.why & PendingModifications.Change.SIZE) != 0) ||
            (change.why & PendingModifications.Change.CHILDREN) != 0) {
            // not interested in change in children, or change in folder size
            return false;
        }
        if (op == MailboxOperation.AlterItemTag &&
                ((MailItem)change.what).isUnread() == ((MailItem)change.preModifyObj).isUnread()) {
            // only interested in read state changes
            return false;
        }
        return true;
    }

    public static Map<String,String> getArgs(MailboxOperation op, MailItem mailitem, MailItem originalitem, ItemIdFormatter ifmt) {
        HashMap<String,String> args = new HashMap<String,String>();
        switch (op) {
        case CreateFolder:
            args.put(ARG_NAME, mailitem.getName());
            break;
        case SaveDocument:
        case AddDocumentRevision:
            if (mailitem instanceof Document) {
                Document doc = (Document) mailitem;
                args.put(ARG_VER, "" + doc.getVersion());
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        case MoveItem:
            if (originalitem != null) {
                int originalFolderId = originalitem.getFolderId();
                try {
                    Folder f1 = mailitem.getMailbox().getFolderById(null, originalFolderId);
                    if (f1 != null) {
                        args.put(ARG_OLDLOCATION, f1.getName());
                        args.put(ARG_OLD_FOLDER_ID, "" + f1.getId());
                    }
                } catch (ServiceException e) {
                }
            }
            try {
                Folder f2 = mailitem.getMailbox().getFolderById(null, mailitem.getFolderId());
                if (f2 != null) {
                    args.put(ARG_NEWLOCATION, f2.getName());
                    args.put(ARG_NEW_FOLDER_ID, "" + f2.getId());
                }
            } catch (ServiceException e) {
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        case RenameItem:
            if (originalitem != null) {
                args.put(ARG_OLDNAME, originalitem.getName());
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        case GrantAccess:
            if (originalitem != null) {
                ACL acl = originalitem.getACL();
                ACL newAcl = mailitem.getACL();
                for (Grant g : newAcl.getGrants()) {
                    if (acl == null || !containsGrant(acl, g)) {
                        getGrantArgs(g, args);
                    }
                }
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        case RevokeAccess:
        case ExpireAccess:
            if (originalitem != null) {
                ACL acl = originalitem.getACL();
                ACL newAcl = mailitem.getACL();
                for (Grant g : acl.getGrants()) {
                    if (!containsGrant(newAcl, g)) {
                        getGrantArgs(g, args);
                    }
                }
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        case CreateComment:
            try {
                ItemId parentId = new ItemId(mailitem.getMailbox(), mailitem.getParentId());
                args.put(ARG_PARENTID, ifmt.formatItemId(parentId));
                args.put(ARG_TEXT, mailitem.getSubject());
                MailItem parent = mailitem.getMailbox().getItemById(null, mailitem.getParentId(), MailItem.Type.UNKNOWN);
                args.put(ARG_NAME, parent.getName());
            } catch (ServiceException se) {
                LOG.debug(se.getMessage(), se.getCause());
            }
            break;
        case CreateMessage:
            if (!(mailitem instanceof Message))
                break;
            Message msg = (Message) mailitem;
            // return the activity stream args only for share notifications.
            try {
                for (MPartInfo part : Mime.getParts(msg.getMimeMessage())) {
                    String ctype = StringUtil.stripControlCharacters(part.getContentType());
                    if (MimeConstants.CT_XML_ZIMBRA_SHARE.equals(ctype)) {
                        ShareNotification sn = ShareNotification.fromMimePart(part.getMimePart());
                        ItemId iid = new ItemId(sn.getGrantorId(), sn.getItemId());
                        args.put(ARG_SHARED_ITEM_ID, iid.toString());
                        args.put(ARG_SHARED_ITEM_NAME, sn.getItemName());
                        args.put(ARG_SHARED_ITEM_VIEW, sn.getView());
                        args.put(ARG_SHARE_GRANTEE_ID, sn.getGranteeId());
                        args.put(ARG_SHARE_PERMISSIONS, sn.getPermissions());
                        args.put(ARG_FROM, msg.getSender());
                        args.put(ARG_SUBJECT, msg.getSubject());
                        break;
                    }
                }
            } catch (IOException e) {
                ZimbraLog.misc.warn("can't parse share notification", e);
            } catch (MessagingException e) {
                ZimbraLog.misc.warn("can't parse share notification", e);
            } catch (ServiceException e) {
                ZimbraLog.misc.warn("can't parse share notification", e);
            }
            break;
        case Watch:
        case Unwatch:
            args.put(ARG_NAME, mailitem.getName());
            break;
        case AlterItemTag:
            args.put(ARG_MESSAGE_READ, "" + !mailitem.isUnread());
            break;
        case DeleteItem:
            if (originalitem != null) {
                int originalFolderId = originalitem.getFolderId();
                try {
                    Folder f1 = mailitem.getMailbox().getFolderById(null, originalFolderId);
                    if (f1 != null) {
                        args.put(ARG_OLDLOCATION, f1.getName());
                        args.put(ARG_OLD_FOLDER_ID, "" + f1.getId());
                    }
                } catch (ServiceException e) {
                }
            }
            args.put(ARG_NAME, mailitem.getName());
            break;
        }
        args.put(ARG_TYPE, mailitem.getType().toString());
        return args;
    }

    private static void getGrantArgs(Grant g, Map<String,String> args) {
        String role = READ;
        if ((g.getGrantedRights() & ACL.RIGHT_ADMIN) > 0)
            role = ADMIN;
        else if ((g.getGrantedRights() & ACL.RIGHT_WRITE) > 0)
            role = RW;
        Provisioning prov = Provisioning.getInstance();
        NamedEntry entry = null;
        try {
            String granteeId = null;
            if (g.hasGrantee())
                granteeId = g.getGranteeId();
            switch (g.getGranteeType()) {
            case ACL.GRANTEE_USER:     entry = prov.getAccountById(granteeId); break;
            case ACL.GRANTEE_COS:      entry = prov.getCosById(granteeId); break;
            case ACL.GRANTEE_DOMAIN:   entry = prov.getDomainById(granteeId); break;
            case ACL.GRANTEE_GROUP:    entry = prov.get(DistributionListBy.id, granteeId); break;
            case ACL.GRANTEE_KEY:      break;
            case ACL.GRANTEE_GUEST:    args.put(ARG_TARGET, granteeId); break;
            case ACL.GRANTEE_AUTHUSER: args.put(ARG_TARGET, AUTHUSER); break;
            case ACL.GRANTEE_PUBLIC:   args.put(ARG_TARGET, PUBLIC); break;
            }
        } catch (ServiceException e) {
        }
        if (entry != null) {
            args.put(ARG_TARGET, entry.getName());
        }
        args.put(ARG_ROLE, role);
    }

    public static MailboxOperation guessCreateOperation(MailItem item) {
        switch (item.getType()) {
        case DOCUMENT:
            return MailboxOperation.SaveDocument;
        case TAG:
            return MailboxOperation.CreateTag;
        case MESSAGE:
            return MailboxOperation.CreateMessage;
        case APPOINTMENT:
        case TASK:
            return MailboxOperation.SetCalendarItem;
        case FOLDER:
            return MailboxOperation.CreateFolder;
        case COMMENT:
            return MailboxOperation.CreateComment;
        case MOUNTPOINT:
            return MailboxOperation.CreateMountpoint;
        default:
            return MailboxOperation.SaveDocument;
        }
    }

    private static boolean containsGrant(ACL acl, Grant grant) {
        if (acl == null)
            return false;
        if (grant.hasGrantee()) {
            String principal = grant.getGranteeId();
            for (Grant g : acl.getGrants()) {
                if (g.hasGrantee() && g.getGranteeId().equals(principal)) {
                    if (grant.getGrantedRights() == g.getGrantedRights())
                        return true;
                    else
                        return false;
                }
            }
        } else {
            byte granteeType = grant.getGranteeType();
            for (Grant g : acl.getGrants()) {
                if (g.getGranteeType() == granteeType) {
                    if (grant.getGrantedRights() == g.getGrantedRights())
                        return true;
                    else
                        return false;
                }
            }
        }
        return false;
    }

    private int getFolderId(MailItem item, MailItem preModifyObj, MailboxOperation op) {
        if (item.getType() == MailItem.Type.COMMENT) {
            return item.getParentId();
        } else if (op == MailboxOperation.MoveItem && preModifyObj != null) {
            return preModifyObj.getFolderId();
        }
        return item.getFolderId();
    }

    private void handleChange(MailItem item, MailboxOperation op, String accountId, long ts, MailItem preModifyObj, String userAgent, Map<String,String> args) {
        try {
            ItemEventLog log = logger.getLog(item);
            /*
             * We need to normalize the userAgent, otherwise there isn't enough space both in UI and DB to log the raw userAgent
             * Following are some examples of userAgents, regex used and normalizedUserAgents logged in DB
             * Octopus ZimbraWebClient - FF4 (Mac)/prototype     --- "(.*)ZimbraWebClient(.*)"  --- Web UI
             * VMware Octopus (iPad Simulator/iPhone OS/5.1)     --- "(.*)iPad(.*)"             --- ipad
             * Horizon Data (Mac)                                --- "(.*)\\((.*)\\)$"          --- Mac
             */
            String normalizedUserAgent = null;
            if (userAgent != null) {
                String[] userAgentRegex = Provisioning.getInstance().getConfig().getClientTypeRegex();
                for(String regex : userAgentRegex) {
                    Pattern pattern = Pattern.compile(regex.substring(regex.indexOf(":")+1));
                    if (pattern.matcher(userAgent).matches()) {
                        String clientType = regex.substring(0, regex.indexOf(":"));

                        if (StringUtil.equal(clientType, "SyncClient")) {
                            Matcher m = pattern.matcher(userAgent);
                            if (m.find()) {
                                normalizedUserAgent = m.group(2);
                            }
                        } else {
                            normalizedUserAgent = clientType;
                        }
                        break;
                    }
                }
                if (normalizedUserAgent == null)
                    normalizedUserAgent = "Other Client";
            }
            log.addEvent(new MailboxEvent(accountId, op, item.getId(), item.getVersion(), getFolderId(item, preModifyObj, op), ts, normalizedUserAgent, args));
        } catch (ServiceException e) {
            LOG.error("can't add event", e);
        }
    }

    @Override
    public Set<Type> registerForItemTypes() {
        return ITEMTYPES;
    }
}
