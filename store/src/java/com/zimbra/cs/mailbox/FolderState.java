package com.zimbra.cs.mailbox;

import java.util.Map;

import com.google.common.base.MoreObjects;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class FolderState extends MailItemState {

    private long totalSize;
    private int deletedCount;
    private int deletedUnreadCount;
    private int imapUIDNEXT;
    private int imapMODSEQ;
    private int imapRECENT;
    private int imapRECENTCutoff;
    private Map<String, Integer> subfolders;
    private Integer parentFolder; //nullable for when a folder has no parent

    public static final String F_TOTAL_SIZE = "totalSize";
    public static final String F_DELETED_COUNT = "deletedCount";
    public static final String F_DELETED_UNREAD_COUNT = "deletedUnreadCount";
    public static final String F_IMAP_UID_NEXT = "imapUIDNEXT";
    public static final String F_IMAP_MODSEQ = "imapMODSEQ";
    public static final String F_IMAP_RECENT = "imapRECENT";
    public static final String F_IMAP_RECENT_CUTOFF = "imapRECENTCutoff";
    public static final String F_SUBFOLDERS = "subfolders";
    public static final String F_PARENT_FOLDER = "parentFolder";

    public FolderState(UnderlyingData data) {
        super(data);
    }

    public long getTotalSize() { return getLongFieldValue(F_TOTAL_SIZE); }

    public void setTotalSize(long totalSize) {
        setTotalSize(totalSize, AccessMode.DEFAULT);
    }

    public void setTotalSize(long totalSize, AccessMode setMode) {
        getField(F_TOTAL_SIZE).set(totalSize, setMode);
    }

    public int getDeletedCount() {
        return getIntFieldValue(F_DELETED_COUNT);
    }

    public void setDeletedCount(int deletedCount) {
        setDeletedCount(deletedCount, AccessMode.DEFAULT);
    }

    public void setDeletedCount(int deletedCount, AccessMode setMode) {
        getField(F_DELETED_COUNT).set(deletedCount, setMode);
    }

    public int getDeletedUnreadCount() {
        return getIntFieldValue(F_DELETED_UNREAD_COUNT);
    }

    public void setDeletedUnreadCount(int deletedUnreadCount) {
        setDeletedUnreadCount(deletedUnreadCount, AccessMode.DEFAULT);
    }

    public void setDeletedUnreadCount(int deletedUnreadCount, AccessMode setMode) {
        getField(F_DELETED_UNREAD_COUNT).set(deletedUnreadCount, setMode);
    }

    public int getImapUIDNEXT() {
        return getIntFieldValue(F_IMAP_UID_NEXT);
    }

    public void setImapUIDNEXT(int imapUIDNEXT) {
        setImapUIDNEXT(imapUIDNEXT, AccessMode.DEFAULT);
    }

    public void setImapUIDNEXT(int imapUIDNEXT, AccessMode setMode) {
        getField(F_IMAP_UID_NEXT).set(imapUIDNEXT, setMode);
    }

    public int getImapMODSEQ() {
        return getIntFieldValue(F_IMAP_MODSEQ);
    }

    public void setImapMODSEQ(int imapMODSEQ) {
        setImapMODSEQ(imapMODSEQ, AccessMode.DEFAULT);
    }

    public void setImapMODSEQ(int imapMODSEQ, AccessMode setMode) {
        getField(F_IMAP_MODSEQ).set(imapMODSEQ, setMode);
    }

    public int getImapRECENT() {
        return getIntFieldValue(F_IMAP_RECENT);
    }

    public void setImapRECENT(int imapRECENT) {
        setImapRECENT(imapRECENT, AccessMode.DEFAULT);
    }

    public void setImapRECENT(int imapRECENT, AccessMode setMode) {
        getField(F_IMAP_RECENT).set(imapRECENT, setMode);
    }

    public int getImapRECENTCutoff() {
        return getIntFieldValue(F_IMAP_RECENT_CUTOFF);
    }

    public void setImapRECENTCutoff(int imapRECENTCutoff) {
        setImapRECENT(imapRECENTCutoff, AccessMode.DEFAULT);
    }

    public void setImapRECENTCutoff(int imapRECENTCutoff, AccessMode setMode) {
        getField(F_IMAP_RECENT_CUTOFF).set(imapRECENTCutoff, setMode);
    }

    public Map<String, Integer> getSubfolders() {
        ItemField<Map<String, Integer>> field = getField(F_SUBFOLDERS);
        return field.get();
    }

    public void setSubfolders(Map<String, Integer> subfolders) {
        setSubfolders(subfolders, AccessMode.DEFAULT);
    }

    public void setSubfolders(Map<String, Integer> subfolders, AccessMode setMode) {
        getField(F_SUBFOLDERS).set(subfolders, setMode);
    }

    public Integer getParentFolder() {
        return getIntField(F_PARENT_FOLDER).get();  /* note: can be null */
    }

    public void setParentFolder(Folder folder) {
        setParentFolder(folder, AccessMode.DEFAULT);
    }

    public void setParentFolder(Folder folder, AccessMode setMode) {
        if (folder != null) {
            getField(F_PARENT_FOLDER).set(folder.getId(), setMode);
        }
    }

    public void unsetParentFolderId() {
        getField(F_PARENT_FOLDER).unset();
    }

    @Override
    protected void initFields() {
        super.initFields();
        initFolderFields();
    }

    private void initFolderFields() {
        addField(new ItemField<Long>(F_TOTAL_SIZE) {

            @Override
            protected void setLocal(Long value) {
                totalSize = newLongLocalValue(this, value);
            }

            @Override
            protected Long getLocal() { return totalSize; }
        });

        addField(new ItemField<Integer>(F_DELETED_COUNT) {

            @Override
            protected void setLocal(Integer value) {
                deletedCount = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return deletedCount; }
        });

        addField(new ItemField<Integer>(F_DELETED_UNREAD_COUNT) {

            @Override
            protected void setLocal(Integer value) {
                deletedUnreadCount = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return deletedUnreadCount; }
        });

        addField(new ItemField<Integer>(F_IMAP_UID_NEXT) {

            @Override
            protected void setLocal(Integer value) {
                imapUIDNEXT = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return imapUIDNEXT; }
        });

        addField(new ItemField<Integer>(F_IMAP_MODSEQ) {

            @Override
            protected void setLocal(Integer value) {
                imapMODSEQ = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return imapMODSEQ; }
        });

        addField(new ItemField<Integer>(F_IMAP_RECENT) {

            @Override
            protected void setLocal(Integer value) {
                imapRECENT = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return imapRECENT; }
        });

        addField(new ItemField<Integer>(F_IMAP_RECENT_CUTOFF) {

            @Override
            protected void setLocal(Integer value) {
                imapRECENTCutoff = newIntLocalValue(this, value);
            }

            @Override
            protected Integer getLocal() { return imapRECENTCutoff; }
        });

        addField(new ItemField<Map<String, Integer>>(F_SUBFOLDERS) {

            @Override
            protected void setLocal(Map<String, Integer> value) { subfolders = value; }

            @Override
            protected Map<String, Integer> getLocal() { return subfolders; }
        });

        addField(new ItemField<Integer>(F_PARENT_FOLDER) {

            @Override
            protected void setLocal(Integer value) { parentFolder = value; }

            @Override
            protected Integer getLocal() { return parentFolder; }
        });
    }

    @Override
    protected MoreObjects.ToStringHelper toStringHelper() {
        return super.toStringHelper()
                .add("totalSize", totalSize)
                .add("deletedCount", deletedCount)
                .add("deletedUnreadCount", deletedUnreadCount)
                .add("imapUIDNEXT", imapUIDNEXT)
                .add("imapMODSEQ", imapMODSEQ)
                .add("imapRECENT", imapRECENT)
                .add("imapRECENTCutoff", imapRECENTCutoff);
    }
}
