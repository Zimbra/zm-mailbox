/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Feb 15, 2006
 */
package com.zimbra.cs.mime;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.ZimbraAnalyzer;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;

public class ParsedDocument {
    private Blob mBlob;
    private int mSize;
    private String mDigest;
    private String mContentType;
    private String mFilename;
    private String mCreator;
    private IndexDocument mZDocument = null;
    private String mFragment;
    private long mCreatedDate;

    /** if TRUE then there was a _temporary_ failure analyzing the message.  We should attempt
     * to re-index this message at a later time */
    private boolean mTemporaryAnalysisFailure = false;

    private static Blob saveInputAsBlob(InputStream in) throws ServiceException, IOException {
        return StoreManager.getInstance().storeIncoming(in, 0, null);
    }
    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator)
        throws ServiceException, IOException {
        this(saveInputAsBlob(in), filename, ctype, createdDate, creator);
    }

    public ParsedDocument(Blob blob, String filename, String ctype, long createdDate, String creator)
        throws ServiceException, IOException {

        mBlob = blob;
        mSize = (int) blob.getRawSize();
        mDigest = blob.getDigest();
        mContentType = ctype;
        mFilename = filename;
        mCreatedDate = createdDate;
        mCreator = creator;

        try {
            MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, filename);
            assert(handler != null);

            if (handler.isIndexingEnabled())
                handler.init(new BlobDataSource(mBlob, ctype));
            handler.setFilename(filename);
            handler.setPartName(LuceneFields.L_PARTNAME_TOP);
            handler.setSize(mSize);

            String textContent = "";
            try {
                textContent = handler.getContent();
            } catch (MimeHandlerException e) {
                if (ConversionException.isTemporaryCauseOf(e)) {
                    ZimbraLog.wiki.warn("Temporary failure extracting from the document.  (is convertd down?)", e);
                    mTemporaryAnalysisFailure = true;
                } else {
                    ZimbraLog.index.warn("Failure indexing wiki document "+filename+".  Item will be partially indexed", e);
                }
            }
            mFragment = Fragment.getFragment(textContent, Fragment.Source.NOTEBOOK);
            try {
                mZDocument = new IndexDocument(handler.getDocument());
                org.apache.lucene.document.Document doc = (org.apache.lucene.document.Document)(mZDocument.getWrappedDocument());
                doc.add(new Field(LuceneFields.L_H_SUBJECT, filename,
                        Field.Store.NO, Field.Index.ANALYZED));

                StringBuilder content = new StringBuilder();
                appendToContent(content, filename);
                appendToContent(content, ZimbraAnalyzer.getAllTokensConcatenated(LuceneFields.L_FILENAME, filename));
                appendToContent(content, textContent);

                doc.add(new Field(LuceneFields.L_CONTENT, content.toString(),
                        Field.Store.NO, Field.Index.ANALYZED));
                doc.add(new Field(LuceneFields.L_H_FROM, creator,
                        Field.Store.NO, Field.Index.ANALYZED));
                doc.add(new Field(LuceneFields.L_FILENAME, filename,
                        Field.Store.YES, Field.Index.ANALYZED));

            } catch (MimeHandlerException e) {
                if (ConversionException.isTemporaryCauseOf(e)) {
                    ZimbraLog.wiki.warn("Temporary failure extracting from the document.  (is convertd down?)", e);
                    mTemporaryAnalysisFailure = true;
                } else {
                    ZimbraLog.index.warn("Failure indexing wiki document "+filename+".  Item will be partially indexed", e);
                }
            } catch (Exception e) {
                ZimbraLog.index.warn("Failure indexing wiki document "+filename+".  Item will be partially indexed", e);
            }
        } catch (MimeHandlerException mhe) {
            throw ServiceException.FAILURE("cannot create ParsedDocument", mhe);
        }
    }

    private static final void appendToContent(StringBuilder sb, String s) {
        if (sb.length() > 0)
            sb.append(' ');
        sb.append(s);
    }


    public void setVersion(int v) {
        // should be indexed so we can add search constraints on the index version
        if (mZDocument == null) {
            ZimbraLog.wiki.warn("Can't index document version.  (is convertd down?)");
        } else {
            org.apache.lucene.document.Document doc = (org.apache.lucene.document.Document)(mZDocument.getWrappedDocument());
            doc.add(new Field(LuceneFields.L_VERSION, Integer.toString(v),
                    Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
    }

    public int getSize()            { return mSize; }
    public String getDigest()       { return mDigest; }
    public Blob getBlob()           { return mBlob; }

    public String getFilename()     { return mFilename; }
    public String getContentType()  { return mContentType; }

    public IndexDocument getDocument()   { return mZDocument; }  // it could return null if the conversion has failed
    public List<IndexDocument> getDocumentList() {
        if (mZDocument == null)
            return java.util.Collections.emptyList();
        List<IndexDocument> toRet = new ArrayList<IndexDocument>(1);
        toRet.add(mZDocument);
        return toRet;
    }
    public String getFragment()     { return mFragment; }

    public String getCreator()      { return mCreator; }
    public long getCreatedDate()    { return mCreatedDate; }
    public boolean hasTemporaryAnalysisFailure() { return mTemporaryAnalysisFailure; }
}
