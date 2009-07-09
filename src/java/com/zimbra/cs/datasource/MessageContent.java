package com.zimbra.cs.datasource;

import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StorageCallback;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mailbox.DeliveryContext;
import com.zimbra.common.service.ServiceException;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

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
            data = readBytes(is, sizeHint);
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

    private byte[] readBytes(InputStream is, int size) throws IOException {
        // Return original byte array and avoid copy if possible
        ByteArrayOutputStream baos = new ByteArrayOutputStream(size) {
            @Override public byte[] toByteArray() {
                return buf.length == count ? buf : super.toByteArray();
            }
        };
        int b;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        if (size != baos.size()) {
            // ZimbraLog.datasource.debug("Content size mismatch: expected %d but got %d bytes", size, baos.size());
        }
        return baos.toByteArray();
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
