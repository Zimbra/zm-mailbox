package com.zimbra.cs.datasource;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.store.FileBlobStore;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.common.service.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

public class MessageContent {
    private Blob blob;
    private byte[] data;

    public static MessageContent read(InputStream is, int size) throws IOException, ServiceException {
        MessageContent mb = new MessageContent();
        mb.readContent(is, size);
        return mb;
    }

    private MessageContent() {}
    
    private void readContent(InputStream is, int size) throws IOException, ServiceException {
        if (size < FileBlobStore.getDiskStreamingThreshold()) {
            data = new byte[size];
            new DataInputStream(is).readFully(data);
        } else {
            short vid = Volume.getCurrentMessageVolume().getId();
            blob = StoreManager.getInstance().storeIncoming(is, size, null, vid, null);
        }
    }

    public ParsedMessage getParsedMessage(Long receivedDate, boolean indexAttachments)
        throws IOException, ServiceException {
        if (data != null) {
            return new ParsedMessage(data, receivedDate, indexAttachments);
        } else {
            return new ParsedMessage(blob.getFile(), receivedDate, indexAttachments);
        }
    }

    public DeliveryContext getDeliveryContext() {
        DeliveryContext dc = new DeliveryContext();
        if (blob != null) {
            dc.setIncomingBlob(blob);
        }
        return dc;
    }
    
    public void cleanup() throws IOException {
        if (blob != null) {
            StoreManager.getInstance().delete(blob);
            blob = null;
        }
    }

    @Override
    public void finalize() throws Throwable {
        try {
            cleanup();
        } finally {
            super.finalize();
        }
    }
}
