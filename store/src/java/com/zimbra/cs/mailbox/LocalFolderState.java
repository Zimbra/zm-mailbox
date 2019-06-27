package com.zimbra.cs.mailbox;

import java.util.Map;

import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class LocalFolderState extends LocalMailItemState implements IFolderState {

    private long totalSize;
    private int deletedCount;
    private int deletedUnreadCount;
    private int imapUIDNEXT;
    private int imapMODSEQ;
    private int imapRECENT;
    private int imapRECENTCutoff;
    private Map<String, Integer> subfolders;
    private Integer parentFolder; //nullable for when a folder has no parent

    public LocalFolderState(UnderlyingData data) {
        super(data);
    }

    @Override
    public long getTotalSize() {
        return totalSize;
    }

    @Override
    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;

    }

    @Override
    public void setTotalSize(long totalSize, AccessMode setMode) {
        this.totalSize = totalSize;

    }

    @Override
    public int getDeletedCount() {
        return deletedCount;
    }

    @Override
    public void setDeletedCount(int deletedCount) {
        this.deletedCount = deletedCount;
    }

    @Override
    public void setDeletedCount(int deletedCount, AccessMode setMode) {
        this.deletedCount = deletedCount;

    }

    @Override
    public int getDeletedUnreadCount() {
        return deletedUnreadCount;
    }

    @Override
    public void setDeletedUnreadCount(int deletedUnreadCount) {
        this.deletedUnreadCount = deletedUnreadCount;
    }

    @Override
    public void setDeletedUnreadCount(int deletedUnreadCount, AccessMode setMode) {
        this.deletedUnreadCount = deletedUnreadCount;
    }

    @Override
    public int getImapUIDNEXT() {
        return imapUIDNEXT;
    }

    @Override
    public void setImapUIDNEXT(int imapUIDNEXT) {
        this.imapUIDNEXT = imapUIDNEXT;
    }

    @Override
    public void setImapUIDNEXT(int imapUIDNEXT, AccessMode setMode) {
        this.imapUIDNEXT = imapUIDNEXT;
    }

    @Override
    public int getImapMODSEQ() {
        return imapMODSEQ;
    }

    @Override
    public void setImapMODSEQ(int imapMODSEQ) {
        this.imapMODSEQ = imapMODSEQ;
    }

    @Override
    public void setImapMODSEQ(int imapMODSEQ, AccessMode setMode) {
        this.imapMODSEQ = imapMODSEQ;
    }

    @Override
    public int getImapRECENT() {
        return imapRECENT;
    }

    @Override
    public void setImapRECENT(int imapRECENT) {
        this.imapRECENT =imapRECENT;
    }

    @Override
    public void setImapRECENT(int imapRECENT, AccessMode setMode) {
        this.imapRECENT = imapRECENT;
    }

    @Override
    public int getImapRECENTCutoff() {
        return imapRECENTCutoff;
    }

    @Override
    public void setImapRECENTCutoff(int imapRECENTCutoff) {
        this.imapRECENTCutoff = imapRECENTCutoff;

    }

    @Override
    public void setImapRECENTCutoff(int imapRECENTCutoff, AccessMode setMode) {
        this.imapRECENTCutoff = imapRECENTCutoff;
    }

    @Override
    public Map<String, Integer> getSubfolders() {
        return subfolders;
    }

    @Override
    public void setSubfolders(Map<String, Integer> subfolders) {
        this.subfolders = subfolders;
    }

    @Override
    public void setSubfolders(Map<String, Integer> subfolders, AccessMode setMode) {
        this.subfolders = subfolders;
    }

    @Override
    public Integer getParentFolder() {
        return parentFolder;
    }

    @Override
    public void setParentFolderById(int parentId) {
        this.parentFolder = parentId;
    }

    @Override
    public void setParentFolderById(int parentId, AccessMode setMode) {
        this.parentFolder = parentId;
    }

    @Override
    public void setParentFolder(Folder folder) {
        this.parentFolder = folder.getId();
    }

    @Override
    public void setParentFolder(Folder folder, AccessMode setMode) {
        this.parentFolder = folder.getId();
    }

    @Override
    public void unsetParentFolderId() {
        this.parentFolder = null;
    }
}
