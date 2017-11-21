/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.http.HttpException;
import org.dom4j.QName;

import com.google.common.primitives.Ints;
import com.zimbra.client.ZContact;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.common.account.Key;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.ContactGroup;
import com.zimbra.cs.mailbox.ContactGroup.Member;
import com.zimbra.cs.mailbox.Conversation;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Message.EventFlag;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.service.util.SpamHandler.SpamReport;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.Zimbra;

public class ItemActionHelper {

    protected ItemActionResult mResult;

    protected SoapProtocol mResponseProtocol;
    protected Op mOperation;
    protected int[] itemIds;
    protected MailItem.Type type;
    protected boolean mFlagValue;
    protected TargetConstraint mTargetConstraint;
    protected int mHopCount;

    // only when Op=TAG
    protected String mTagName;

    // only when OP=COLOR or OP=UPDATE
    protected Color mColor;

    // only when OP=RENAME or OP=UPDATE
    protected String mName;

    // only when OP=MOVE or OP=COPY or OP=RENAME or OP=UPDATE or OP=SPAM
    protected ItemId mIidFolder, mIidRequestedFolder;

    // only when OP=UPDATE
    protected String mFlags;
    protected String[] mTags;

    protected ItemIdFormatter mIdFormatter;
    protected Account mAuthenticatedAccount;

    private final OperationContext mOpCtxt;
    private final Mailbox mMailbox;

    public static ItemActionHelper TAG(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, String tagName, boolean flagValue, TargetConstraint tcon)
            throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto,
                    ids, Op.TAG, type, flagValue, tcon);
        ia.setTagName(tagName);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper FLAG(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, boolean flagValue, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.FLAG, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper PRIORITY(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, boolean flagValue, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.PRIORITY, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper READ(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, boolean flagValue, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.READ, type, flagValue, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper COLOR(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon, Color color)
            throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.COLOR, type, true, tcon);
        ia.setColor(color);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper HARD_DELETE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.HARD_DELETE, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper RECOVER(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon, ItemId iidFolder) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.RECOVER, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper DUMPSTER_DELETE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.DUMPSTER_DELETE, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper MOVE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon, ItemId iidFolder) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.MOVE, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper SEEN(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.SEEN, MailItem.Type.MESSAGE, true, null);
        ia.schedule();
        return ia;
    }

    /**
     * Account relative path conversation move.
     */
    public static List<ItemActionHelper> MOVE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, TargetConstraint tcon, String acctRelativePath) throws ServiceException {
        List<ItemActionHelper> returnList = new ArrayList<>();

        // First build all external account / data source root folder ids
        Set<Integer> dsRootFolderIds = new HashSet<>();
        if (!ids.isEmpty()) {
            List<DataSource> dataSources = mbox.getAccount().getAllDataSources();
            if (dataSources != null) {
                for (DataSource ds : dataSources) {
                    int dsFolderId = ds.getFolderId();
                    if (dsFolderId != -1) {
                        dsRootFolderIds.add(dsFolderId);
                    }
                }
            }
        }

        for (int convId : ids) {
            Integer rootFolderIdForConv = null;
            for (Message msg : mbox.getMessagesByConversation(octxt, convId, SortBy.NONE, -1)) {
                int rootFolderIdForThisMsg = AccountUtil.getRootFolderIdForItem(msg, mbox, dsRootFolderIds);
                if (rootFolderIdForConv == null) {
                    rootFolderIdForConv = rootFolderIdForThisMsg;
                } else if (rootFolderIdForConv != rootFolderIdForThisMsg) {
                    // this is conv spanning multiple accounts / data sources
                    rootFolderIdForConv = null;
                    break;
                }
            }
            if (rootFolderIdForConv == null) {
                continue;
            }
            Folder rootFolder = mbox.getFolderById(octxt, rootFolderIdForConv);
            String rootFolderPath = rootFolder.getPath();
            rootFolderPath = "/".equals(rootFolderPath) ? "" : rootFolderPath;
            String targetFolderPath = rootFolderPath.concat(acctRelativePath.startsWith("/") ? acctRelativePath :
                    "/" + acctRelativePath);
            Folder targetFolder;
            try {
                targetFolder = mbox.getFolderByPath(octxt, targetFolderPath);
            } catch (MailServiceException.NoSuchItemException e) {
                targetFolder = mbox.createFolder(octxt, targetFolderPath,
                        new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
            }

            returnList.add(MOVE(octxt, mbox, responseProto, Arrays.asList(convId), MailItem.Type.CONVERSATION, tcon,
                    new ItemId(targetFolder)));
        }

        return returnList;
    }

    public static ItemActionHelper COPY(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
                List<Integer> ids, MailItem.Type type, TargetConstraint tcon, ItemId iidFolder)
    throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.COPY, type, true, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper SPAM(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, boolean flagValue, TargetConstraint tcon, ItemId iidFolder)
            throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.SPAM, type, flagValue, tcon);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper RENAME(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon, String name, ItemId iidFolder)
            throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.RENAME, type, true, tcon);
        ia.setName(name);
        ia.setIidFolder(iidFolder);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper UPDATE(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon, String name, ItemId iidFolder, String flags,
            String[] tags, Color color) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.UPDATE, type, true, tcon);
        ia.setName(name);
        ia.setIidFolder(iidFolder);
        ia.setFlags(flags);
        ia.setTags(tags);
        ia.setColor(color);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper LOCK(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.LOCK, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static ItemActionHelper UNLOCK(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,
            List<Integer> ids, MailItem.Type type, TargetConstraint tcon) throws ServiceException {
        ItemActionHelper ia = new ItemActionHelper(octxt, mbox, responseProto, ids, Op.UNLOCK, type, true, tcon);
        ia.schedule();
        return ia;
    }

    public static enum Op {
        TAG("tag"),
        FLAG("flag"),
        PRIORITY("priority"),
        READ("read"),
        COLOR("color"),
        HARD_DELETE("delete"),
        RECOVER("recover"),
        DUMPSTER_DELETE("dumpsterdelete"),
        MOVE("move"),
        COPY("copy"),
        SPAM("spam"),
        RENAME("rename"),
        UPDATE("update"),
        LOCK("lock"),
        UNLOCK("unlock"),
        SEEN("seen");

        private final String name;

        private Op(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public String toString() {
        StringBuilder toRet = new StringBuilder(super.toString());

        toRet.append(" Op=").append(mOperation.toString());
        toRet.append(" Type=").append(type);
        toRet.append(" FlagValue=").append(mFlagValue);
        if (mTargetConstraint != null) {
            toRet.append(" TargetConst=").append(mTargetConstraint.toString());
        }

        if (mOperation == Op.TAG) {
            toRet.append(" TagName=").append(mTagName);
        }

        if (mOperation == Op.COLOR || mOperation == Op.UPDATE) {
            toRet.append(" Color=").append(mColor);
        }

        if (mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE) {
            toRet.append(" iidFolder=").append(mIidFolder);
        }

        if (mOperation == Op.UPDATE) {
            if (mFlags != null) {
                toRet.append(" flags=").append(mFlags);
            }
            if (mTags != null) {
                toRet.append(" tags=").append(TagUtil.encodeTags(mTags));
            }
        }
        return toRet.toString();
    }

    public void setTagName(String tagName) {
        assert(mOperation == Op.TAG);
        mTagName = tagName;
    }

    public void setColor(Color color) {
        assert(mOperation == Op.COLOR || mOperation == Op.UPDATE);
        mColor = color;
    }

    public void setName(String name) {
        assert(mOperation == Op.RENAME || mOperation == Op.UPDATE);
        mName = name;
    }

    public void setIidFolder(ItemId iidFolder)  {
        assert(mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || mOperation == Op.UPDATE || mOperation == Op.RECOVER);
        mIidRequestedFolder = mIidFolder = iidFolder;
    }

    public void setFlags(String flags) {
        assert(mOperation == Op.UPDATE);
        mFlags = flags;
    }

    public void setTags(String[] tags) {
        assert(mOperation == Op.UPDATE);
        mTags = tags;
    }

    ItemActionHelper(OperationContext octxt, Mailbox mbox, SoapProtocol responseProto,  List<Integer> ids, Op op,
            MailItem.Type type, boolean flagValue, TargetConstraint tcon) throws ServiceException {

        mOpCtxt = octxt;
        mMailbox = mbox;

        mAuthenticatedAccount = octxt == null ? null : octxt.getAuthenticatedUser();
        if (mAuthenticatedAccount == null)
            mAuthenticatedAccount = mbox.getAccount();
        mIdFormatter = new ItemIdFormatter(mAuthenticatedAccount, mbox, false);
        mResponseProtocol = responseProto;

        int i = 0;
        itemIds = new int[ids.size()];
        for (int id : ids)
            itemIds[i++] = id;

        mOperation = op;
        if (mOperation == null)
            throw ServiceException.INVALID_REQUEST("unknown operation: null", null);
        this.type = type;
        mFlagValue = flagValue;
        mTargetConstraint = tcon;
    }

    protected Mailbox getMailbox() { return mMailbox; }
    protected OperationContext getOpCtxt() { return mOpCtxt; }

    protected void schedule() throws ServiceException {
        boolean targeted = mOperation == Op.MOVE || mOperation == Op.SPAM || mOperation == Op.COPY || mOperation == Op.RENAME || (mOperation == Op.UPDATE && mIidFolder != null);

        // deal with local mountpoints pointing at local folders here
        if (targeted && mIidFolder.belongsTo(mMailbox) && mIidFolder.getId() > 0 && mIidFolder.getId() != Mailbox.ID_FOLDER_TRASH && mIidFolder.getId() != Mailbox.ID_FOLDER_SPAM) {
            try {
                Folder folder = mMailbox.getFolderById(mOpCtxt, mIidFolder.getId());
                if (folder instanceof Mountpoint && !((Mountpoint) folder).getOwnerId().equals(mIidFolder.getAccountId())) {
                    mIidFolder = ((Mountpoint) folder).getTarget();
                    mHopCount++;
                }
            } catch (ServiceException e) {
                // could be a PERM_DENIED, could be something else -- this is not the right place to fail, however
            }
        }

        try {
            if (!targeted || mIidFolder.belongsTo(mMailbox))
                mResult = executeLocal();
            else
                mResult = executeRemote();
        } catch (IOException | HttpException ioe) {
            throw ServiceException.FAILURE("exception reading item blob", ioe);
        }
    }

    public ItemActionResult getResult() {
        return mResult;
    }

    private ItemActionResult executeLocalBatch(int[] ids) throws ServiceException {
        // iterate over the local items and perform the requested operation

        List<String> originalIds = new ArrayList<String>(ids.length);
        for (int id : ids) {
            originalIds.add(mIdFormatter.formatItemId(id));
        }
        ItemActionResult result = ItemActionResult.create(mOperation);
        result.setSuccessIds(originalIds);

        switch (mOperation) {
            case FLAG:
                getMailbox().alterTag(getOpCtxt(), ids, type, Flag.FlagInfo.FLAGGED, mFlagValue, mTargetConstraint);
                break;
            case PRIORITY:
                getMailbox().alterTag(getOpCtxt(), ids, type, Flag.FlagInfo.PRIORITY, mFlagValue, mTargetConstraint);
                break;
            case READ:
                getMailbox().alterTag(getOpCtxt(), ids, type, Flag.FlagInfo.UNREAD, !mFlagValue, mTargetConstraint);
                break;
            case TAG:
                getMailbox().alterTag(getOpCtxt(), ids, type, mTagName, mFlagValue, mTargetConstraint);
                break;
            case COLOR:
                getMailbox().setColor(getOpCtxt(), ids, type, mColor);
                break;
            case HARD_DELETE:
                List<Integer> nonExistentItems = new ArrayList<Integer>();
                getMailbox().delete(getOpCtxt(), ids, type, mTargetConstraint, nonExistentItems);
                List<String> nonExistentIds = new ArrayList<String>();
                for (Integer id: nonExistentItems) {
                    nonExistentIds.add(id.toString());
                }
                ((DeleteActionResult)result).setNonExistentIds(nonExistentIds);
                break;
            case RECOVER:
                getMailbox().recover(getOpCtxt(), ids, type, mIidFolder.getId());
                break;
            case DUMPSTER_DELETE:
                getMailbox().deleteFromDumpster(getOpCtxt(), ids);
                break;
            case SPAM:
            case MOVE:
                getMailbox().move(getOpCtxt(), ids, type, mIidFolder.getId(), mTargetConstraint);
                break;
            case COPY:
                List<MailItem> copies = getMailbox().copy(getOpCtxt(), ids, type, mIidFolder.getId());
                List<String> createdIds = new ArrayList<String>(ids.length);
                for (MailItem item : copies) {
                     createdIds.add(mIdFormatter.formatItemId(item));
                }
                ((CopyActionResult)result).setCreatedIds(createdIds);
                break;
            case RENAME:
                for (int id : ids) {
                    getMailbox().rename(getOpCtxt(), id, type, mName, mIidFolder.getId());
                }
                break;
            case UPDATE:
                if (mName != null) {
                    for (int id : ids) {
                        getMailbox().rename(getOpCtxt(), id, type, mName, mIidFolder.getId());
                    }
                } else if (mIidFolder != null && mIidFolder.getId() > 0) {
                    getMailbox().move(getOpCtxt(), ids, type, mIidFolder.getId(), mTargetConstraint);
                }
                if (mTags != null || mFlags != null) {
                    Integer flagMask = null;
                    if (mFlags == null) {
                        flagMask = MailItem.FLAG_UNCHANGED;
                    } else {
                        //check if the flags were passed in as the bitmask
                        flagMask = Ints.tryParse(mFlags);
                        if (flagMask == null) {
                            flagMask = Flag.toBitmask(mFlags);
                        }
                    }

                    if (mTags == null) {
                        mTags = MailItem.TAG_UNCHANGED;
                    }
                    getMailbox().setTags(getOpCtxt(), ids, type, flagMask, mTags, mTargetConstraint);
                }
                if (mColor != null) {
                    getMailbox().setColor(getOpCtxt(), ids, type, mColor);
                }
                break;
            case LOCK:
                for (int id : ids) {
                    getMailbox().lock(getOpCtxt(), id, type, mAuthenticatedAccount.getId());
                }
                break;
            case UNLOCK:
                for (int id : ids) {
                    getMailbox().unlock(getOpCtxt(), id, type, mAuthenticatedAccount.getId());
                }
                break;
            case SEEN:
                for (int id: ids) {
                    getMailbox().markMsgSeen(getOpCtxt(), id);
                }
                break;
            default:
                throw ServiceException.INVALID_REQUEST("unknown operation: " + mOperation, null);
        }

     return result;
    }

    private ItemActionResult executeLocal() throws ServiceException {
        int batchSize = Provisioning.getInstance().getLocalServer().getItemActionBatchSize();
        ZimbraLog.mailbox.debug("ItemAction batchSize=%d", batchSize);
        if (itemIds.length <= batchSize) {
            return executeLocalBatch(itemIds);
        }
        int offset = 0;

        ItemActionResult localResult = ItemActionResult.create(mOperation);

        while (offset < itemIds.length) {
            ItemActionResult batchResult = null;
            int[] batchOfIds = Arrays.copyOfRange(itemIds, offset,
                    (offset + batchSize < itemIds.length) ? offset + batchSize : itemIds.length);

            batchResult = executeLocalBatch(batchOfIds);
            localResult.appendSuccessIds(batchResult.getSuccessIds());
            if (Op.COPY == mOperation) {
                ((CopyActionResult)localResult).appendCreatedIds(batchResult);
            } else if (Op.HARD_DELETE == mOperation) {
                ((DeleteActionResult)localResult).appendNonExistentIds(batchResult);
            }

            offset += batchSize;
            if (offset < itemIds.length) {
                Thread.yield();
            }
        }
        return localResult;
    }

    private AuthToken getAuthToken() throws ServiceException {
        AuthToken authToken = null;

        if (mOpCtxt != null)
            authToken = AuthToken.getCsrfUnsecuredAuthToken(mOpCtxt.getAuthToken());

        if (authToken == null)
            authToken = AuthProvider.getAuthToken(mAuthenticatedAccount);

        return authToken;
    }

    private ItemActionResult executeRemote() throws ServiceException, IOException, HttpException{
        Account target = Provisioning.getInstance().get(Key.AccountBy.id, mIidFolder.getAccountId());

        AuthToken at = getAuthToken();
        String pxyAuthToken = Provisioning.onLocalServer(target) ? null : at.getProxyAuthToken();
        ZAuthToken zat = null;
        if (pxyAuthToken == null) {
            zat = at.toZAuthToken();
            zat.resetProxyAuthToken();
        } else {
            zat = new ZAuthToken(pxyAuthToken);
        }

        ZMailbox.Options zoptions = new ZMailbox.Options(zat, AccountUtil.getSoapUri(target));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(target.getId());
        zoptions.setTargetAccountBy(Key.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        zmbx.setName(target.getName()); /* need this when logging in using another user's auth */

        // check for mountpoints before going any further...
        ZFolder zfolder = zmbx.getFolderById(mIidFolder.toString(mAuthenticatedAccount));
        if (zfolder instanceof ZMountpoint) {
            ItemId iidTarget = new ItemId(((ZMountpoint) zfolder).getCanonicalRemoteId(), mAuthenticatedAccount.getId());
            if (!mIidFolder.equals(iidTarget)) {
                mIidFolder = iidTarget;
                if (++mHopCount > com.zimbra.soap.ZimbraSoapContext.MAX_HOP_COUNT)
                    throw MailServiceException.TOO_MANY_HOPS(mIidRequestedFolder);
                schedule();
                return ItemActionResult.create(mOperation);
            }
        }

        boolean deleteOriginal = mOperation != Op.COPY;
        String folderStr = mIidFolder.toString();
        List<String> createdIds = new ArrayList<String>(itemIds.length);
        List<String> nonExistentIds = new ArrayList<String>();

        boolean toSpam = mIidFolder.getId() == Mailbox.ID_FOLDER_SPAM;
        boolean toMailbox = !toSpam && mIidFolder.getId() != Mailbox.ID_FOLDER_TRASH;

        for (MailItem item : mMailbox.getItemById(mOpCtxt, itemIds, type)) {
            if (item == null) {
                continue;
            }
            List<Message> msgs = null;
            if (item instanceof Conversation) {
                msgs = mMailbox.getMessagesByConversation(mOpCtxt, item.getId(), SortBy.DATE_ASC, -1);
            }
            if (deleteOriginal) {
                if (msgs != null) {
                    // determine which of the conversation's component messages are actually able to be moved
                    boolean permDenied = false;
                    for (Iterator<Message> it = msgs.iterator(); it.hasNext(); ) {
                        Message msg = it.next();
                        if (!TargetConstraint.checkItem(mTargetConstraint, msg)) {
                            it.remove();
                        } else if (!canDelete(msg)) {
                            it.remove();  permDenied = true;
                        }
                    }
                    // stop here if no messages would be moved...
                    if (msgs.isEmpty()) {
                        if (permDenied) {
                            throw ServiceException.PERM_DENIED("cannot delete any messages in " +
                                    item.getType() + " " + item.getId());
                        }
                        // all messages were excluded by the TargetConstraint, so there's no failure...
                        continue;
                    }
                } else {
                    if (!canDelete(item)) {
                        throw ServiceException.PERM_DENIED("cannot delete existing copy of " +
                                item.getType() + " " + item.getId());
                    }
                }
            }

            boolean fromSpam = item.inSpam();
            if ((fromSpam && toMailbox) || (!fromSpam && toSpam)) {
                try {
                    Folder dest = mMailbox.getFolderById(mOpCtxt, mIidFolder.getId());
                    SpamReport report = new SpamReport(toSpam, "remote " + mOperation, dest.getPath());
                    Folder source = mMailbox.getFolderById(mOpCtxt, item.getFolderId());
                    report.setSourceFolderPath(source.getPath());
                    report.setDestAccountName(target.getName());
                    SpamHandler.getInstance().handle(mOpCtxt, mMailbox, item.getId(), item.getType(), report);
                } catch (OutOfMemoryError e) {
                    Zimbra.halt("out of memory", e);
                } catch (Throwable t) {
                    ZimbraLog.mailop.info("could not train spam filter: " + new ItemId(item).toString(), t);
                }
            }

            // since we can't apply tags to a remote object, hardwiring "tags" to null below...
            String flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : item.getFlagString());
            String name = ((mOperation == Op.RENAME || mOperation == Op.UPDATE) && mName != null ? mName : item.getName());
            String createdId = null;
            InputStream in = null;

            switch (item.getType()) {
            case CONTACT:
                Contact ct = (Contact) item;
                Map<String, ZMailbox.ZAttachmentInfo> attachments = new HashMap<String, ZMailbox.ZAttachmentInfo>();
                for (Contact.Attachment att : ct.getAttachments()) {
                    String attachmentId = zmbx.uploadAttachment(att.getFilename(), att.getContent(), att.getContentType(), 0);
                    ZMailbox.ZAttachmentInfo info = new ZMailbox.ZAttachmentInfo().setAttachmentId(attachmentId);
                    attachments.put(att.getName(), info);
                }
                Map<String, String> fields = ct.getFields();
                Map<String, String> members = new HashMap<String, String>();
                for (String key : fields.keySet()) {
                    if (ContactConstants.A_groupMember.equals(key)) {
                        String memberEncoded = fields.get(key);
                        ContactGroup group = ContactGroup.init(memberEncoded);
                        for (Member m : group.getMembers()) {
                            members.put(m.getValue(), m.getType().getSoapEncoded());
                        }
                        break;
                    }
                }
                fields.remove(ContactConstants.A_groupMember);
                ZContact contact = zmbx.createContact(folderStr, null, fields, attachments, members);
                createdId = contact.getId();
                createdIds.add(createdId);
                break;
            case MESSAGE:
                try {
                    in = StoreManager.getInstance().getContent(item.getBlob());
                    createdId = zmbx.addMessage(folderStr, flags, (String) null, item.getDate(), in, item.getSize(), true);
                } finally {
                    ByteUtil.closeStream(in);
                }
                createdIds.add(createdId);
                break;
            case VIRTUAL_CONVERSATION:
            case CONVERSATION:
                for (Message msg : msgs) {
                    flags = (mOperation == Op.UPDATE && mFlags != null ? mFlags : msg.getFlagString());
                    try {
                        in = StoreManager.getInstance().getContent(msg.getBlob());
                        createdId = zmbx.addMessage(folderStr, flags, (String) null, msg.getDate(), in, msg.getSize(), true);
                    } finally {
                        ByteUtil.closeStream(in);
                    }
                    createdIds.add(createdId);
                }
                break;
            case DOCUMENT:
                Document doc = (Document) item;
                SoapHttpTransport transport = new SoapHttpTransport(zoptions.getUri());
                try {
                    in = StoreManager.getInstance().getContent(doc.getBlob());
                    String uploadId = zmbx.uploadContentAsStream(name, in, doc.getContentType(), doc.getSize(), 4000, true);
                    // instead of using convenience method from ZMailbox
                    // we need to hand marshall the request and set the
                    // response protocol explicitly to what was requested
                    // from the client.
                    Element req = new XMLElement(MailConstants.SAVE_DOCUMENT_REQUEST);
                    Element edoc = req.addUniqueElement(MailConstants.E_DOC);
                    edoc.addAttribute(MailConstants.A_NAME, name);
                    edoc.addAttribute(MailConstants.A_FOLDER, folderStr);
                    edoc.addAttribute(MailConstants.A_FLAGS, flags);
                    Element upload = edoc.addNonUniqueElement(MailConstants.E_UPLOAD);
                    upload.addAttribute(MailConstants.A_ID, uploadId);
                    transport.setResponseProtocol(mResponseProtocol);
                    transport.setAuthToken(zat);
                    Element response = transport.invoke(req);
                    createdId = response.getElement(MailConstants.E_DOC).getAttribute(MailConstants.A_ID);
                } finally {
                    ByteUtil.closeStream(in);
                    transport.shutdown();
                }
                createdIds.add(createdId);
                break;
            case APPOINTMENT:
            case TASK:
                CalendarItem cal = (CalendarItem) item;
                // private calendar item may not be moved by non-owner unless permission was granted
                if (!cal.isPublic()) {
                    boolean asAdmin = mOpCtxt != null ? mOpCtxt.isUsingAdminPrivileges() : false;
                    if (!cal.allowPrivateAccess(mAuthenticatedAccount, asAdmin))
                        throw ServiceException.PERM_DENIED(
                                "you do not have permission to move/copy a private calendar item from the current folder/mailbox");
                }

                // Move the item to remote mailbox using SetAppointmentRequest/SetTaskRequest.
                QName qname = (item.getType() == MailItem.Type.TASK ? MailConstants.SET_TASK_REQUEST : MailConstants.SET_APPOINTMENT_REQUEST);
                Element request = new Element.XMLElement(qname).addAttribute(MailConstants.A_FOLDER, folderStr).addAttribute(MailConstants.A_FLAGS, flags);
                ToXML.encodeAlarmTimes(request, cal);

                Invite invDefault = cal.getDefaultInviteOrNull();

                // Takeover as organizer if we're doing a MOVE and source mailbox is the organizer.
                // Don't takeover in a COPY operation.
                boolean takeoverAsOrganizer = false;
                boolean blockMove = false;
                if (Op.MOVE.equals(mOperation)) {
                    Invite inv = invDefault;
                    if (inv == null) {
                        // no default invite; let's use the first invite
                        Invite[] invs = cal.getInvites();
                        if (invs != null && invs.length > 0)
                            inv = invs[0];
                    }
                    takeoverAsOrganizer = inv != null && inv.isOrganizer();
                    blockMove =  takeoverAsOrganizer && inv.hasOtherAttendees();
                }

                if (blockMove) {
                    throw MailServiceException.INVALID_REQUEST(
                        "This operation requires change of organizer and it is not permitted", null);
                }

                if (invDefault != null) {
                    addCalendarPart(request.addUniqueElement(MailConstants.A_DEFAULT), cal, invDefault, zmbx, target, takeoverAsOrganizer);
                }

                for (Invite inv : cal.getInvites()) {
                    if (inv == null || inv == invDefault)
                        continue;
                    String elem = inv.isCancel() ? MailConstants.E_CAL_CANCEL : MailConstants.E_CAL_EXCEPT;
                    addCalendarPart(request.addNonUniqueElement(elem), cal, inv, zmbx, target, takeoverAsOrganizer);
                }

                ToXML.encodeCalendarReplies(request, cal);

                createdId = zmbx.invoke(request).getAttribute(MailConstants.A_CAL_ID);
                createdIds.add(createdId);
                break;
            default:
                throw MailServiceException.CANNOT_COPY(item.getId());
            }

            try {
                if (deleteOriginal && !mIdFormatter.formatItemId(item).equals(createdId)) {
                    List<Integer> nonExistentItems = new ArrayList<Integer>();

                    if (msgs == null) {
                        mMailbox.delete(mOpCtxt, new int[] { item.getId() }, item.getType(), null, nonExistentItems);
                    } else {
                        for (Message msg : msgs) {
                            mMailbox.delete(mOpCtxt, new int[] { msg.getId() }, msg.getType(), null, nonExistentItems);
                        }
                    }

                    for (Integer id: nonExistentItems) {
                        nonExistentIds.add(id.toString());
                    }
                }
            } catch (ServiceException e) {
                if (e.getCode() != ServiceException.PERM_DENIED)
                    throw e;
                // something funky happened permissions-wise between the getEffectivePermissions check and here...
                ZimbraLog.misc.info("could not delete original item " + item.getId() + "; treating operation as a copy instead");
            }
        }

        ItemActionResult result = ItemActionResult.create(mOperation);

        if (Op.HARD_DELETE.equals(mOperation)) {
            ((DeleteActionResult)result).setNonExistentIds(nonExistentIds);
        }
        else if (Op.COPY.equals(mOperation)) {
            ((CopyActionResult)result).setCreatedIds(createdIds);
        }

        for (int itemId : itemIds) {
            result.appendSuccessId(Integer.toString(itemId));
        }
        return result;
    }

    private void addCalendarPart(Element parent, CalendarItem cal, Invite inv, ZMailbox zmbx, Account target, boolean takeoverAsOrganizer)
    throws ServiceException {
        parent.addAttribute(MailConstants.A_CAL_PARTSTAT, inv.getPartStat());
        Element m = parent.addUniqueElement(MailConstants.E_MSG);

        Pair<MimeMessage, Integer> spinfo = cal.getSubpartMessageData(inv.getMailItemId());
        if (spinfo != null) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(spinfo.getSecond());
                spinfo.getFirst().writeTo(baos);
                String uploadId = zmbx.uploadAttachment("message", baos.toByteArray(), MimeConstants.CT_MESSAGE_RFC822, 6000);
                m.addAttribute(MailConstants.A_ATTACHMENT_ID, uploadId);
            } catch (IOException ioe) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), ioe);
            } catch (MessagingException me) {
                ZimbraLog.misc.info("could not read subpart message for part " + inv.getComponentNum() + " of item " + cal.getId(), me);
            }
        }

        if (takeoverAsOrganizer && inv.isOrganizer() && inv.hasOrganizer()) {
            Invite invCopy = inv.newCopy();
            invCopy.setInviteId(inv.getMailItemId());
            // Increment SEQUENCE and bring DTSTAMP current because we're changing organizer.
            invCopy.setSeqNo(inv.getSeqNo() + 1);
            invCopy.setDtStamp(System.currentTimeMillis());
            ZOrganizer org = invCopy.getOrganizer();
            org.setAddress(target.getName());
            org.setCn(target.getDisplayName());
            Account authAcct = mOpCtxt != null ? mOpCtxt.getAuthenticatedUser() : target;
            if (authAcct == null || authAcct.equals(target))
                org.setSentBy(null);
            else
                org.setSentBy(authAcct.getName());
            inv = invCopy;
        }

        // explicitly add the invite metadata here
        ToXML.encodeInvite(m, mIdFormatter, getOpCtxt(), cal, inv, true);
    }

    private boolean canDelete(MailItem item) throws ServiceException {
        return (mMailbox.getEffectivePermissions(mOpCtxt, item.getId(), item.getType()) & ACL.RIGHT_DELETE) != 0;
    }
}
