package com.zimbra.cs.session;

import com.zimbra.common.mailbox.BaseFolderInfo;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.account.message.ImapMessageInfo;

public class ModificationItem implements BaseItemInfo, BaseFolderInfo, ZimbraTag {

    private String acctId;
    private int id;
    private int imapUid;
    private int folderId;
    private int flags;
    private String[] tags;
    private MailItemType type;

    //for folder rename
    private String path;

    //for tag rename
    private int tagId;
    private String tagName;

    public static ZimbraTag tagRename(int tagId, String tagName) {
        return new ModificationItem(tagId, tagName);
    }

    /*
     * This returns ModificationItem instead of BaseFolderInfo because ImapListener.handleModify
     * assumes the instance of BaseFolderInfo also implements BaseItemInfo
     */
    public static ModificationItem folderRename(int folderId, String path, String acctId) {
        return new ModificationItem(folderId, path, acctId);
    }

    public static BaseItemInfo itemUpdate(ImapMessageInfo msg, int folderId, String acctId) {
        return new ModificationItem(msg, folderId, acctId);
    }

    private ModificationItem(int tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

    private ModificationItem(int id, String path, String acctId) {
        this.id = id;
        this.acctId = acctId;
        this.path = path;
        this.type = MailItemType.FOLDER;
    }
    private ModificationItem(ImapMessageInfo msg, int folderId, String acctId) {
        this.acctId = acctId;
        this.flags = msg.getFlags();
        this.tags = msg.getTags() == null ? null : msg.getTags().split(",");
        this.id = msg.getId();
        this.imapUid = msg.getImapUid();
        this.type = MailItem.Type.of(msg.getType()).toCommon();
        this.folderId = folderId;
    }


    @Override
    public String getAccountId() throws ServiceException {
        return acctId;
    }

    @Override
    public int getFlagBitmask() {
        return flags;
    }

    @Override
    public int getFolderIdInMailbox() throws ServiceException {
        return folderId;
    }

    @Override
    public int getIdInMailbox() throws ServiceException {
        return id;
    }

    @Override
    public String[] getTags() {
        return tags;
    }

    @Override
    public int getImapUid() {
        return imapUid;
    }

    @Override
    public MailItemType getMailItemType() {
        return type;
    }

    @Override
    public String getPath() {
        return path;
    }


    @Override
    public int getFolderIdInOwnerMailbox() {
        return id;
    }

    @Override
    public int getTagId() {
        return tagId;
    }
    @Override
    public String getTagName() {
        return tagName;
    }
}
