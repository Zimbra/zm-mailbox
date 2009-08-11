package com.zimbra.cs.datasource;

import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.BufferStream;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StorageCallback;
import com.zimbra.cs.store.StoreManager;

public class MessageContent {
    private Blob blob;
    private byte[] data;

    public static MessageContent read(InputStream is, int size) throws IOException, ServiceException {
        MessageContent mb = new MessageContent();
        mb.readContent(is, size);
        return mb;
    }

    private MessageContent() {}
    
    private void readContent(InputStream is, int sizeHint) throws IOException, ServiceException {
        if (sizeHint < StorageCallback.getDiskStreamingThreshold()) {
            BufferStream bs = new BufferStream(sizeHint);
            
            if (bs.readFrom(is) != sizeHint) {
                // ZimbraLog.datasource.debug("Content size mismatch: expected %d but got %d bytes", size, baos.size());
            }
            data = bs.toByteArray();
            bs.close();
        } else {
            blob = StoreManager.getInstance().storeIncoming(is, sizeHint, null);
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
