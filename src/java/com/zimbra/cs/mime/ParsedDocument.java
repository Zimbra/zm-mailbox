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

import java.io.IOException;

import javax.mail.util.ByteArrayDataSource;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

public class ParsedDocument {
    private byte[] mContent;
    private int mSize;
    private String mDigest;
    private String mContentType;
    private String mFilename;
    private String mCreator;
    private Document mDocument = null;
    private String mFragment;
    private long mCreatedDate;

    public ParsedDocument(MailboxBlob blob, String filename, String ctype, long createdDate, String creator)
    throws ServiceException, IOException {
        init(ByteUtil.getContent(StoreManager.getInstance().getContent(blob), 0), filename, ctype, createdDate, creator);
    }

    public ParsedDocument(byte[] rawData, String filename, String ctype, long createdDate, String creator)
    throws ServiceException {
        init(rawData, filename, ctype, createdDate, creator);
    }

    private void init(byte[] content, String filename, String ctype, long createdDate, String creator)
    throws ServiceException {
        mContent = content;
        mSize = content.length;
        mDigest = ByteUtil.getDigest(content);
        mContentType = ctype;
        mFilename = filename;
        mCreatedDate = createdDate;
        mCreator = creator;

        try {
            MimeHandler handler = MimeHandlerManager.getMimeHandler(ctype, filename);
            assert(handler != null);

            if (handler.isIndexingEnabled())
                handler.init(new ByteArrayDataSource(content, ctype));
            handler.setFilename(filename);
            handler.setPartName(LuceneFields.L_PARTNAME_TOP);
            handler.setMessageDigest(mDigest);
            
            String textContent = "";
            try {
            	textContent = handler.getContent();
            } catch (Exception e) {
            	// ignore conversion errors
            }
            mFragment = Fragment.getFragment(textContent, false);
            try {
            	mDocument = handler.getDocument();
            	mDocument.add(new Field(LuceneFields.L_SIZE, Integer.toString(mSize), Field.Store.YES, Field.Index.NO));
            	mDocument.add(new Field(LuceneFields.L_H_SUBJECT, filename, Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_CONTENT, filename,  Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_H_FROM, creator, Field.Store.NO, Field.Index.TOKENIZED));
            	mDocument.add(new Field(LuceneFields.L_FILENAME, filename, Field.Store.YES, Field.Index.TOKENIZED));
            } catch (Exception e) {
            	// ignore conversion errors
            }
        } catch (MimeHandlerException mhe) {
            throw ServiceException.FAILURE("cannot create ParsedDocument", mhe);
        }
    }

    public void setVersion(int v) {
        // should be indexed so we can add search constraints on the index version
        mDocument.add(new Field(LuceneFields.L_VERSION, Integer.toString(v), Field.Store.YES, Field.Index.UN_TOKENIZED));
    }

    public int getSize()            { return mSize; }
    public String getDigest()       { return mDigest; }
    public byte[] getContent()      { return mContent; }

    public String getFilename()     { return mFilename; }
    public String getContentType()  { return mContentType; }

    public Document getDocument()   { return mDocument; }  // it could return null if the conversion has failed
    public String getFragment()     { return mFragment; }

    public String getCreator()      { return mCreator; }
    public long getCreatedDate()    { return mCreatedDate; }
}
