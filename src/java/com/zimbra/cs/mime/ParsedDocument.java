/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.index.analysis.RFC822AddressTokenStream;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

/**
 * @since Feb 15, 2006
 */
public final class ParsedDocument {
    private final Blob blob;
    private final int size;
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
        return StoreManager.getInstance().storeIncoming(in, null);
    }

    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator,
            String description) throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator, description, true);
    }

    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled) throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator, description, descEnabled);
    }
    
    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled, boolean forceParsing) throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator, description, descEnabled, forceParsing);
    }
    
    public ParsedDocument(Blob blob, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled) throws IOException {
        this(blob, filename, ctype, createdDate, creator, description, descEnabled, false);
    }

    public ParsedDocument(Blob blob, String filename, String ctype, long createdDate, String creator,
            String description, boolean descEnabled, boolean forceParsing) throws IOException {
        this.blob = blob;
        this.size = (int) blob.getRawSize();
        this.digest = blob.getDigest();
        this.contentType = ctype;
        this.filename = StringUtil.sanitizeFilename(filename);
        this.createdDate = createdDate;
        this.creator = creator;
        this.description = description;
        this.descEnabled = descEnabled;
        if (forceParsing)
            performExtraction();
    }

    /**
     * Performs the text extraction lazily if it hasn't been done already
     */
    private synchronized void performExtraction() {
        try {
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
                    ZimbraLog.wiki.warn("Temporary failure extracting from the document.  (is convertd down?)", e);
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
            appendToContent(content, filename);
            appendToContent(content, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_FILENAME, filename));
            appendToContent(content, textContent);
            appendToContent(content, description);

            document.addContent(content.toString());
            document.addFrom(new RFC822AddressTokenStream(creator));
            document.addFilename(filename);
        } catch (MimeHandlerException mhe) {
            if (ConversionException.isTemporaryCauseOf(mhe)) {
                ZimbraLog.wiki.warn("Temporary failure extracting from the document.  (is convertd down?)", mhe);
                temporaryAnalysisFailure = true;
            } else {
                ZimbraLog.wiki.error("cannot create ParsedDocument", mhe);
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
            ZimbraLog.wiki.warn("Can't index document version.  (is convertd down?)");
        } else {
            document.addVersion(version);
        }
    }

    public int getSize() {
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
