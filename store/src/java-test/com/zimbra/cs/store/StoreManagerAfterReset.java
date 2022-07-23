package com.zimbra.cs.store;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

import java.io.IOException;
import java.io.InputStream;

public class StoreManagerAfterReset extends StoreManager{

    @Override
    public void startup() throws IOException, ServiceException {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean supports(StoreFeature feature) {
        return false;
    }

    @Override
    public boolean supports(StoreFeature feature, String locator) {
        return false;
    }

    @Override
    public BlobBuilder getBlobBuilder() throws IOException, ServiceException {
        return null;
    }

    @Override
    public Blob storeIncoming(InputStream data, boolean storeAsIs) throws IOException, ServiceException {
        return null;
    }

    @Override
    public StagedBlob stage(InputStream data, long actualSize, Mailbox mbox) throws IOException, ServiceException {
        return null;
    }

    @Override
    public StagedBlob stage(Blob blob, Mailbox mbox) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, int destRevision) throws IOException, ServiceException {
        return null;
    }

    @Override
    public boolean delete(Blob blob) throws IOException {
        return false;
    }

    @Override
    public boolean delete(StagedBlob staged) throws IOException {
        return false;
    }

    @Override
    public boolean delete(MailboxBlob mblob) throws IOException {
        return false;
    }

    @Override
    public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, int revision, String locator, boolean validate) throws ServiceException {
        return null;
    }

    @Override
    public InputStream getContent(MailboxBlob mboxBlob) throws IOException {
        return null;
    }

    @Override
    public InputStream getContent(Blob blob) throws IOException {
        return null;
    }

    @Override
    public boolean deleteStore(Mailbox mbox, Iterable<MailboxBlob.MailboxBlobInfo> blobs) throws IOException, ServiceException {
        return false;
    }
}