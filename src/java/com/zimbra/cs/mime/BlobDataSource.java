/*
 * Created on Apr 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.store.StoreManager;


/**
 * @author schemers
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BlobDataSource implements DataSource {

    private MailboxBlob mBlob;
    
    /**
     * @param blob
     */
    public BlobDataSource(MailboxBlob blob) {
        mBlob = blob;
    }

    /* (non-Javadoc)
     * @see javax.activation.DataSource#getContentType()
     */
    public String getContentType() {
        return mBlob.getMimeType();
    }

    /**
     * Returns the InputStream for this blob. Note that this method 
     * needs a database connection and will obtain/release one
     * automatically if needed, or use the one passed to it from
     * the constructor.
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return StoreManager.getInstance().getContent(mBlob);
    }

    /* (non-Javadoc)
     * @see javax.activation.DataSource#getName()
     */
    public String getName() {
        // TODO should we just return null?
        return mBlob.toString();
    }

    /* (non-Javadoc)
     * @see javax.activation.DataSource#getOutputStream()
     */
    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

}
