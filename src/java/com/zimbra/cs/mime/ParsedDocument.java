/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 15, 2006
 */
package com.zimbra.cs.mime;

import javax.activation.FileDataSource;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.convert.ConversionException;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.Volume;

public class ParsedDocument {
	private Blob mBlob;
    private int mSize;
    private String mDigest;
    private String mContentType;
    private String mFilename;
    private String mCreator;
    private Document mDocument = null;
    private String mFragment;
    private long mCreatedDate;
    private boolean mIndexFailed;

    private static Blob saveInputAsBlob(InputStream in) throws ServiceException, IOException {
    	return StoreManager.getInstance().storeIncoming(in, 0, null, Volume.getCurrentMessageVolume().getId());
    }
    public ParsedDocument(InputStream in, String filename, String ctype, long createdDate, String creator)
    	throws ServiceException, IOException {
    	this(saveInputAsBlob(in), filename, ctype, createdDate, creator);
    }
    public ParsedDocument(Blob blob, String filename, String ctype, long createdDate, String creator)
    throws ServiceException, IOException {
        mBlob = blob;
        mSize = blob.getRawSize();
        mDigest = blob.getDigest();
        mContentType = ctype;
        mFilename = filename;
        mCreatedDate = createdDate;
        mCreator = creator;
        mIndexFailed = false;

        try {
            MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, filename);
            assert(handler != null);

            if (handler.isIndexingEnabled())
                handler.init(new FileDataSource(blob.getFile()));
            handler.setFilename(filename);
            handler.setPartName(LuceneFields.L_PARTNAME_TOP);
            handler.setMessageDigest(mDigest);
            
            String textContent = "";
            try {
            	textContent = handler.getContent();
            } catch (Exception e) {
            	ZimbraLog.wiki.warn("Can't extract the text from the document.  (is convertd down?)", e);
            	mIndexFailed = true;
            }
            mFragment = Fragment.getFragment(textContent, Fragment.Source.NOTEBOOK);
            try {
            	mDocument = handler.getDocument();
            	mDocument.add(new Field(LuceneFields.L_SIZE, Integer.toString(mSize), Field.Store.YES, Field.Index.NO));
            	mDocument.add(new Field(LuceneFields.L_H_SUBJECT, filename, Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_CONTENT, filename,  Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_H_FROM, creator, Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_FILENAME, filename, Field.Store.YES, Field.Index.TOKENIZED));
            } catch (MimeHandlerException e) {
                if (ConversionException.isTemporaryCauseOf(e)) {
                    ZimbraLog.index.info("Temporary failure indexing wiki document "+filename, e);
                    mIndexFailed = true;
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

    public void setVersion(int v) {
        // should be indexed so we can add search constraints on the index version
    	if (mDocument == null)
        	ZimbraLog.wiki.warn("Can't index document version.  (is convertd down?)");
    	else
    		mDocument.add(new Field(LuceneFields.L_VERSION, Integer.toString(v), Field.Store.YES, Field.Index.UN_TOKENIZED));
    }

    public int getSize()            { return mSize; }
    public String getDigest()       { return mDigest; }
    public Blob getBlob()           { return mBlob; }

    public String getFilename()     { return mFilename; }
    public String getContentType()  { return mContentType; }

    public Document getDocument()   { return mDocument; }  // it could return null if the conversion has failed
    public List<Document> getDocumentList() { 
    	if (mDocument == null)
    		return java.util.Collections.emptyList();
        List<Document> toRet = new ArrayList<Document>(1); 
        toRet.add(mDocument); 
        return toRet; 
    }
    public String getFragment()     { return mFragment; }

    public String getCreator()      { return mCreator; }
    public long getCreatedDate()    { return mCreatedDate; }
    public boolean hasTemporaryAnalysisFailure() { return mIndexFailed; }
}
