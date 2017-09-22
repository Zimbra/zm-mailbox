/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2017 Synacor, Inc.
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
import java.util.Collections;
import java.util.List;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

/**
 * @since Feb 15, 2006
 */
public final class ParsedDocument {
    private final Blob blob;
    private final long size;
    private final String digest;
    private final String contentType;
    private final String filename;
    private final String creator;
    private IndexDocument document;
    private String fragment;
    private final long createdDate;
    private final String description;
    private final boolean descEnabled;
    private int version = 0;

    /** if TRUE then there was a _temporary_ failure analyzing the message.  We should attempt
     * to re-index this message at a later time */
    private boolean temporaryAnalysisFailure = false;
    private boolean parsed = false; // whether or not analysis has taken place

    private static Blob saveInputAsBlob(InputStream in) throws ServiceException, IOException {
        return StoreManager.getInstance().storeIncoming(in);
    }

    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator,
            String description) throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator, description, true);
    }

    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled) throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator, description, descEnabled);
    }

    public ParsedDocument(Blob blob, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled) throws IOException {
        this.blob = blob;
        this.size = blob.getRawSize();
        this.digest = blob.getDigest();
        this.contentType = ctype;
        this.filename = StringUtil.sanitizeFilename(filename);
        this.createdDate = createdDate;
        this.creator = creator;
        this.description = description;
        this.descEnabled = descEnabled;
        if (LC.documents_disable_instant_parsing.booleanValue() == false) {
            performExtraction();
        }
    }

    /**
     * Performs the text extraction lazily if it hasn't been done already
     */
    private synchronized void performExtraction() {
        try {
            long start = System.currentTimeMillis();
            MimeHandler handler = MimeHandlerManager.getMimeHandler(contentType, filename);
            assert(handler != null);

            if (handler.isIndexingEnabled()) {
                handler.init(new BlobDataSource(blob, contentType));
            }
            handler.setFilename(filename);
            handler.setPartName(LuceneFields.L_PARTNAME_TOP);
            handler.setSize(size);

            String textContent = "";
            try {
                textContent = handler.getContent();
            } catch (MimeHandlerException e) {
                if (ConversionException.isTemporaryCauseOf(e)) {
                    ZimbraLog.doc.warn("Temporary failure extracting from the document.  (is convertd down?)", e);
                    temporaryAnalysisFailure = true;
                } else {
                    ZimbraLog.index.warn("Failure indexing wiki document "+ filename + ".  Item will be partially indexed", e);
                }
            }
            fragment = Fragment.getFragment(textContent, Fragment.Source.NOTEBOOK);

            document = new IndexDocument(handler.getDocument());
            document.addSubject(filename);
            // If the version was changed before extraction, add it in now
            if (version > 0) {
                document.addVersion(version);
            }

            StringBuilder content = new StringBuilder();
            appendToContent(content, textContent);
            appendToContent(content, description);

            document.addContent(content.toString());
            document.addFrom(creator);
            document.addFilename(filename);
            long elapsed = System.currentTimeMillis() - start;
            ZimbraLog.doc.debug("ParsedDocument performExtraction elapsed=" + elapsed);
        } catch (MimeHandlerException mhe) {
            if (ConversionException.isTemporaryCauseOf(mhe)) {
                ZimbraLog.doc.warn("Temporary failure extracting from the document.  (is convertd down?)", mhe);
                temporaryAnalysisFailure = true;
            } else {
                ZimbraLog.doc.error("cannot create ParsedDocument", mhe);
            }
        } catch (Exception e) {
            ZimbraLog.index.warn("Failure indexing wiki document " + filename + ".  Item will be partially indexed", e);
        } finally {
            parsed = true;
        }
    }

    private static final void appendToContent(StringBuilder sb, String s) {
        if (s == null) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(s);
    }

    public void setVersion(int version) {
        this.version = version;
        // should be indexed so we can add search constraints on the index version
        if (document == null) {
            ZimbraLog.doc.warn("Can't index document version.  (is convertd down?)");
        } else {
            document.addVersion(version);
        }
    }

    public long getSize() {
        return size;
    }

    public String getDigest() {
        return digest;
    }

    public Blob getBlob() {
        return blob;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Could return null if the conversion has failed.
     */
    public IndexDocument getDocument() {
        if (!parsed) {
            performExtraction();
        }

        return document;
    }

    public List<IndexDocument> getDocumentList() {
        if (!parsed){
            performExtraction();
        }
        return document == null ? Collections.<IndexDocument>emptyList() : Collections.singletonList(document);
    }

    public String getFragment() {
        if (!parsed) {
            performExtraction();
        }
        return fragment;
    }

    public String getCreator() {
        return creator;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDescriptionEnabled() {
        return descEnabled;
    }

    public boolean hasTemporaryAnalysisFailure() {
        return temporaryAnalysisFailure;
    }

}
