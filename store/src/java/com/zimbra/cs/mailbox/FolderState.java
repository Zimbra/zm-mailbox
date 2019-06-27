package com.zimbra.cs.mailbox;

import java.util.Map;

public interface FolderState extends MailItemState {

    public long getTotalSize();

    public void setTotalSize(long totalSize);

    public void setTotalSize(long totalSize, AccessMode setMode);

    public int getDeletedCount();

    public void setDeletedCount(int deletedCount);

    public void setDeletedCount(int deletedCount, AccessMode setMode);

    public int getDeletedUnreadCount();

    public void setDeletedUnreadCount(int deletedUnreadCount);

    public void setDeletedUnreadCount(int deletedUnreadCount, AccessMode setMode);

    public int getImapUIDNEXT();

    public void setImapUIDNEXT(int imapUIDNEXT);

    public void setImapUIDNEXT(int imapUIDNEXT, AccessMode setMode);

    public int getImapMODSEQ();

    public void setImapMODSEQ(int imapMODSEQ);

    public void setImapMODSEQ(int imapMODSEQ, AccessMode setMode);

    public int getImapRECENT();

    public void setImapRECENT(int imapRECENT);

    public void setImapRECENT(int imapRECENT, AccessMode setMode);

    public int getImapRECENTCutoff();

    public void setImapRECENTCutoff(int imapRECENTCutoff);

    public void setImapRECENTCutoff(int imapRECENTCutoff, AccessMode setMode);

    public Map<String, Integer> getSubfolders();

    public void setSubfolders(Map<String, Integer> subfolders);

    public void setSubfolders(Map<String, Integer> subfolders, AccessMode setMode);

    public Integer getParentFolder();

    public void setParentFolderById(int parentId);

    public void setParentFolderById(int parentId, AccessMode setMode);

    public void setParentFolder(Folder folder);

    public void setParentFolder(Folder folder, AccessMode setMode);

    public void unsetParentFolderId();
}
