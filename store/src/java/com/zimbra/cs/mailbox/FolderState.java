package com.zimbra.cs.mailbox;

import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class FolderState extends MailItemState {

    private long totalSize;
    private int deletedCount;
    private int deletedUnreadCount;
    private int imapUIDNEXT;
    private int imapMODSEQ;
    private int imapRECENT;
    private int imapRECENTCutoff;

    public static final String F_TOTAL_SIZE = "totalSize";
    public static final String F_DELETED_COUNT = "deletedCount";
    public static final String F_DELETED_UNREAD_COUNT = "deletedUnreadCount";
    public static final String F_IMAP_UID_NEXT = "imapUIDNEXT";
    public static final String F_IMAP_MODSEQ = "imapMODSEQ";
    public static final String F_IMAP_RECENT = "imapRECENT";
    public static final String F_IMAP_RECENT_CUTOFF = "imapRECENTCutoff";

    public FolderState(UnderlyingData data) {
        super(data);
    }

    public long getTotalSize() {
        return getLongField(F_TOTAL_SIZE).get();
    }

    public void setTotalSize(long totalSize) {
        setTotalSize(totalSize, AccessMode.DEFAULT);
    }

    public void setTotalSize(long totalSize, AccessMode setMode) {
        getField(F_TOTAL_SIZE).set(totalSize, setMode);
    }

    public int getDeletedCount() {
        return getIntField(F_DELETED_COUNT).get();
    }

    public void setDeletedCount(int deletedCount) {
        setDeletedCount(deletedCount, AccessMode.DEFAULT);
    }

    public void setDeletedCount(int deletedCount, AccessMode setMode) {
        getField(F_DELETED_COUNT).set(deletedCount, setMode);
    }

    public int getDeletedUnreadCount() {
        return getIntField(F_DELETED_UNREAD_COUNT).get();
    }

    public void setDeletedUnreadCount(int deletedUnreadCount) {
        setDeletedUnreadCount(deletedUnreadCount, AccessMode.DEFAULT);
    }

    public void setDeletedUnreadCount(int deletedUnreadCount, AccessMode setMode) {
        getField(F_DELETED_UNREAD_COUNT).set(deletedUnreadCount, setMode);
    }

    public int getImapUIDNEXT() {
        return getIntField(F_IMAP_UID_NEXT).get();
    }

    public void setImapUIDNEXT(int imapUIDNEXT) {
        setImapUIDNEXT(imapUIDNEXT, AccessMode.DEFAULT);
    }

    public void setImapUIDNEXT(int imapUIDNEXT, AccessMode setMode) {
        getField(F_IMAP_UID_NEXT).set(imapUIDNEXT, setMode);
    }

    public int getImapMODSEQ() {
        return getIntField(F_IMAP_MODSEQ).get();
    }

    public void setImapMODSEQ(int imapMODSEQ) {
        setImapMODSEQ(imapMODSEQ, AccessMode.DEFAULT);
    }

    public void setImapMODSEQ(int imapMODSEQ, AccessMode setMode) {
        getField(F_IMAP_MODSEQ).set(imapMODSEQ, setMode);
    }

    public int getImapRECENT() {
        return getIntField(F_IMAP_RECENT).get();
    }

    public void setImapRECENT(int imapRECENT) {
        setImapRECENT(imapRECENT, AccessMode.DEFAULT);
    }

    public void setImapRECENT(int imapRECENT, AccessMode setMode) {
        getField(F_IMAP_RECENT).set(imapRECENT, setMode);
    }

    public int getImapRECENTCutoff() {
        return getIntField(F_IMAP_RECENT_CUTOFF).get();
    }

    public void setImapRECENTCutoff(int imapRECENTCutoff) {
        setImapRECENT(imapRECENTCutoff, AccessMode.DEFAULT);
    }

    public void setImapRECENTCutoff(int imapRECENTCutoff, AccessMode setMode) {
        getField(F_IMAP_RECENT_CUTOFF).set(imapRECENTCutoff, setMode);
    }

    @Override
    protected void initFields() {
        super.initFields();
        initFolderFields();
    }

    private void initFolderFields() {
        addField(new ItemField<Long>(F_TOTAL_SIZE) {

            @Override
            protected void setLocal(Long value) { FolderState.this.totalSize = value; }

            @Override
            protected Long getLocal() { return FolderState.this.totalSize; }
        });

        addField(new ItemField<Integer>(F_DELETED_COUNT) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.deletedCount = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.deletedCount; }
        });

        addField(new ItemField<Integer>(F_DELETED_UNREAD_COUNT) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.deletedUnreadCount = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.deletedUnreadCount; }
        });

        addField(new ItemField<Integer>(F_IMAP_UID_NEXT) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.imapUIDNEXT = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.imapUIDNEXT; }
        });

        addField(new ItemField<Integer>(F_IMAP_MODSEQ) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.imapMODSEQ = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.imapMODSEQ; }
        });

        addField(new ItemField<Integer>(F_IMAP_RECENT) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.imapRECENT = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.imapRECENT; }
        });

        addField(new ItemField<Integer>(F_IMAP_RECENT_CUTOFF) {

            @Override
            protected void setLocal(Integer value) { FolderState.this.imapRECENTCutoff = value; }

            @Override
            protected Integer getLocal() { return FolderState.this.imapRECENTCutoff; }
        });
    }
}
