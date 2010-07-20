/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.internet.MimeUtility;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.object.MatchedObject;
import com.zimbra.cs.object.ObjectHandler;
import com.zimbra.cs.object.ObjectHandlerException;

/**
 * @since Apr 30, 2004
 * @author schemers
 */
public abstract class MimeHandler {
    /**
     * The name of the HTTP request parameter used to identify
     * an individual file within an archive. Its value is unspecified;
     * it may be the full path within the archive, or a sequence number.
     * It should be understood between the archive handler and the servlet.
     *
     */
    public static final String ARCHIVE_SEQUENCE = "archseq";
    public static final String CATCH_ALL_TYPE = "all";

    protected MimeTypeInfo mMimeTypeInfo;
    private String mFilename;
    private DataSource mDataSource;
    private String mContentType;
    private int mSize = -1;

    /** dotted-number part name */
    private String mPartName;

    /** Returns <tt>true</tt> if a request for the handler to perform text
     *  extraction or HTML conversion will result in an RPC to an external
     *  process.  <tt>false</tt> indicates what all processing is done
     *  in-process. */
    protected abstract boolean runsExternally();

    protected String getContentType() {
        return mContentType != null ? mContentType : "";
    }

    protected void setContentType(String contentType) {
        mContentType = contentType;
    }

    public String getDescription() {
        return mMimeTypeInfo.getDescription();
    }

    public boolean isIndexingEnabled() {
        return mMimeTypeInfo.isIndexingEnabled();
    }

    /**
     * Initializes the data source for text extraction.
     *
     * @see #getContentImpl()
     * @see #addFields(Document)
     */
    public void init(DataSource source) {
        mDataSource = source;
    }

    void setPartName(String partName) {
        mPartName = partName;
    }

    public String getPartName() {
        return mPartName;
    }

    void setFilename(String filename) {
        mFilename = filename;
    }

    public String getFilename() {
        return mFilename;
    }

    public DataSource getDataSource() {
        return mDataSource;
    }

    public int getSize() {
        return mSize;
    }

    public void setSize(int size) {
        mSize = size;
    }

    /**
     * Adds the indexed fields to the Lucene document for search. Each handler determines
     * a set of fields that it deems important for the type of documents it handles.
     *
     * @param doc
     * @throws MimeHandlerException
     */
    protected abstract void addFields(Document doc) throws MimeHandlerException;

    /**
     * Gets the text content of the document.
     *
     * @return
     * @throws MimeHandlerException
     */
    public final String getContent() throws MimeHandlerException {
        if (!DebugConfig.disableMimePartExtraction) {
            String toRet = getContentImpl();
            if (toRet == null)
                return "";
            else
                return toRet;
        } else {
            if (!mDrainedContent) {
                InputStream is = null;
                try {
                    is = getDataSource().getInputStream();
                    // Read all bytes from the input stream and discard them.
                    // This is useful for testing MIME parser performance, as
                    // the parser may not fully parse the message unless the
                    // message part is read in its entirety.
                    // Multiple buffers will share sDrainBuffer without any
                    // synchronizing, but this is okay since we don't use the
                    // data read.
                    while (is.read(sDrainBuffer) != -1) {}
                } catch (IOException e) {
                    throw new MimeHandlerException("cannot extract text", e);
                } finally {
                    ByteUtil.closeStream(is);
                }
                mDrainedContent = true;
            }
            return "";
        }
    }
    private boolean mDrainedContent = false;
    private static byte[] sDrainBuffer = new byte[4096];

    /**
     * Returns the content for this MIME part.  Length of content returned
     * cannot exceed the value returned by {@link MimeHandlerManager#getMaxContentLength}.
     */
    protected abstract String getContentImpl() throws MimeHandlerException;

    public ZVCalendar getICalendar() throws MimeHandlerException {
        return null;
    }

    /**
     * Converts the document into HTML/images for viewing.
     *
     * @param doc
     * @param urlPart URL base or path
     * @return path to the main converted HTML file.
     * @throws IOException
     * @throws ConversionException
     */
    public abstract String convert(AttachmentInfo doc, String urlPart) throws IOException, ConversionException;

    /**
     * Determines if this handler can process archive files (zip, tar, etc.).
     *
     * @return true if the handler can handle archive, false otherwise.
     */
    public boolean handlesArchive() {
        return false;
    }

    /**
     * Determines if this handler can convert the document into HTML/images.
     * If this method returns false, then the original document will be returned to the browser.
     * @return
     */
    public abstract boolean doConversion();

    public Document getDocument() throws MimeHandlerException, ObjectHandlerException, ServiceException {

        /*
         * Initialize the F_L_TYPE field with the content type from the
         * specified DataSouce. Additionally, if DataSource is an instance
         * of BlobDataSource (which it always should when creating a document),
         * then also initialize F_L_BLOB_ID and F_L_SIZE fields.
         */

        Document doc = new Document();
        doc.add(new Field(LuceneFields.L_MIMETYPE, getContentType(),
                Field.Store.YES, Field.Index.ANALYZED));

        addFields(doc);
        String content = getContent();
        doc.add(new Field(LuceneFields.L_CONTENT, content,
                Field.Store.NO, Field.Index.ANALYZED));
        getObjects(content, doc);

        doc.add(new Field(LuceneFields.L_PARTNAME, mPartName,
                Field.Store.YES, Field.Index.NOT_ANALYZED));

        String name = mDataSource.getName();
        if (name != null) {
            try {
                name = MimeUtility.decodeText(name);
            } catch (UnsupportedEncodingException e) { }
            doc.add(new Field(LuceneFields.L_FILENAME, name,
                    Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        return doc;
    }

    public static void getObjects(String text, Document doc)
        throws ObjectHandlerException, ServiceException {

        if (DebugConfig.disableObjects) {
            return;
        }

        List<?> objects = ObjectHandler.getObjectHandlers();
        StringBuffer l_objects = new StringBuffer();
        for (Object obj : objects) {
            ObjectHandler h = (ObjectHandler) obj;
            if (!h.isIndexingEnabled()) {
                continue;
            }
            List<MatchedObject> matchedObjects = new ArrayList<MatchedObject>();
            h.parse(text, matchedObjects, true);
            if (!matchedObjects.isEmpty()) {
                if (l_objects.length() > 0) {
                    l_objects.append(',');
                }
                l_objects.append(h.getType());
            }
        }
        if (l_objects.length() > 0) {
            doc.add(new Field(LuceneFields.L_OBJECTS, l_objects.toString(),
                    Field.Store.NO, Field.Index.ANALYZED));
        }
    }

    public AttachmentInfo getDocInfoFromArchive(AttachmentInfo archiveDocInfo,
            String seq) throws IOException {
        return null;
    }
}
