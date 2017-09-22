/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.activation.DataSource;
import javax.mail.internet.MimeUtility;

import org.apache.lucene.document.Document;
import org.apache.solr.common.SolrInputDocument;

import com.google.common.base.Strings;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.convert.AttachmentInfo;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.IndexDocument;
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

    private MimeTypeInfo mimeTypeInfo;
    private String filename;
    private DataSource dataSource;
    private String contentType;
    private long size = -1;
    private String defaultCharset;
    private String locale;
    private String partName; // dotted-number part name

    /** Returns <tt>true</tt> if a request for the handler to perform text
     *  extraction or HTML conversion will result in an RPC to an external
     *  process.  <tt>false</tt> indicates what all processing is done
     *  in-process. */
    protected abstract boolean runsExternally();

    MimeTypeInfo getMimeTypeInfo() {
        return mimeTypeInfo;
    }

    void setMimeTypeInfo(MimeTypeInfo value) {
        mimeTypeInfo = value;
    }

    public String getContentType() {
        return Strings.nullToEmpty(contentType);
    }

    public void setContentType(String value) {
        contentType = value;
    }

    protected String getDefaultCharset() {
        return defaultCharset;
    }

    public void setDefaultCharset(String value) {
        defaultCharset = value;
    }

    protected Locale getLocale() {
        if (locale != null) {
            return Locale.forLanguageTag(locale);
        }
        try {
            Locale loc = Provisioning.getInstance().getConfig().getLocale();
            if (null != loc) {
                ZimbraLog.misc.debug("Chose locale '%s' by zimbraLocale", loc);
                return loc;
            }
        } catch (ServiceException e) {
        }
        return Locale.getDefault();
    }

    public void setLocale(String value) {
        locale = value;
    }

    public String getDescription() {
        if (mimeTypeInfo == null) {
            return "";
        }
        return mimeTypeInfo.getDescription();
    }

    public boolean isIndexingEnabled() {
        return mimeTypeInfo == null? false : mimeTypeInfo.isIndexingEnabled();
    }

    /**
     * Initializes the data source for text extraction.
     *
     * @see #getContentImpl()
     * @see #addFields(Document)
     */
    public void init(DataSource source) {
        dataSource = source;
    }

    void setPartName(String value) {
        partName = value;
    }

    public String getPartName() {
        return partName;
    }

    public void setFilename(String value) {
        filename = value;
    }

    public String getFilename() {
        return filename;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long value) {
        size = value;
    }

    /**
     * Adds the indexed fields to the Lucene document for search. Each handler determines
     * a set of fields that it deems important for the type of documents it handles.
     */
    protected abstract void addFields(SolrInputDocument doc) throws MimeHandlerException;

    /**
     * Gets the text content of the document.
     */
    public final String getContent() throws MimeHandlerException {
        if (!DebugConfig.disableMimePartExtraction) {
            String toRet = getContentImpl();
            if (toRet == null)
                return "";
            else
                return toRet;
        } else {
            if (dataSource != null && !mDrainedContent) {
                InputStream is = null;
                try {
                    is = dataSource.getInputStream();
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

    /**
     * Subclass should override.
     *
     * @return null
     * @throws MimeHandlerException if a MIME parser error occurred.
     */
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

    /**
     * Returns a Lucene document to index this content.
     *
     * @return Solr document
     * @throws MimeHandlerException if a MIME parser error occurred
     * @throws ObjectHandlerException if a Zimlet error occurred
     * @throws ServiceException if other error occurred
     */
    public final SolrInputDocument getDocument()
        throws MimeHandlerException, ObjectHandlerException, ServiceException {

        IndexDocument doc = new IndexDocument();
        doc.addMimeType(getContentType());

        addFields(doc.toInputDocument());
        String content = getContent();
        doc.addContent(content);
        getObjects(content, doc);

        doc.addPartName(partName);

        if (dataSource != null) {
            String name = dataSource.getName();
            if (name != null) {
                try {
                    name = MimeUtility.decodeText(name);
                } catch (UnsupportedEncodingException ignore) {
                }
                doc.addFilename(name);
            }
        }
        return doc.toInputDocument();
    }

    public static void getObjects(String text, IndexDocument doc)
        throws ObjectHandlerException, ServiceException {

        if (DebugConfig.disableObjects) {
            return;
        }

        List<ObjectHandler> objects = ObjectHandler.getObjectHandlers();
        StringBuffer l_objects = new StringBuffer();
        for (ObjectHandler h : objects) {
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
            doc.addObjects(l_objects.toString());
        }
    }

    /**
     * Subclass should override.
     *
     * @param archiveDocInfo attachment info
     * @param seq sequence number
     * @return null
     * @throws IOException if an IO error occurred.
     */
    public AttachmentInfo getDocInfoFromArchive(AttachmentInfo archiveDocInfo,
            String seq) throws IOException {
        return null;
    }
}
