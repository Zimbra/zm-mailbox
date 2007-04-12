/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 15, 2006
 */
package com.zimbra.cs.mime;

import java.io.File;
import java.io.IOException;

import javax.mail.util.ByteArrayDataSource;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.object.ObjectHandlerException;

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

    public ParsedDocument(File file, String filename, String ctype, long createdDate, String creator)
    throws ServiceException, IOException {
        init(ByteUtil.getContent(file), filename, ctype, createdDate, creator);
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
            MimeHandler handler = MimeHandler.getMimeHandler(ctype);
            assert(handler != null);

            if (handler.isIndexingEnabled())
                handler.init(new ByteArrayDataSource(content, ctype));
            handler.setFilename(filename);
            handler.setPartName(LuceneFields.L_PARTNAME_TOP);
            handler.setMessageDigest(mDigest);
            
            mFragment = Fragment.getFragment(handler.getContent(), false);
            mDocument = handler.getDocument();
            mDocument.add(new Field(LuceneFields.L_SIZE, Integer.toString(mSize), Field.Store.YES, Field.Index.NO));
            mDocument.add(new Field(LuceneFields.L_H_SUBJECT, filename, Field.Store.NO, Field.Index.TOKENIZED));
            mDocument.add(new Field(LuceneFields.L_CONTENT, filename,  Field.Store.NO, Field.Index.TOKENIZED));
            mDocument.add(new Field(LuceneFields.L_H_FROM, creator, Field.Store.NO, Field.Index.TOKENIZED));
            mDocument.add(new Field(LuceneFields.L_FILENAME, filename, Field.Store.YES, Field.Index.TOKENIZED));
        } catch (MimeHandlerException mhe) {
            throw ServiceException.FAILURE("cannot create ParsedDocument", mhe);
        } catch (ObjectHandlerException ohe) {
            throw ServiceException.FAILURE("cannot create ParsedDocument", ohe);
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

    public Document getDocument()   { return mDocument; }
    public String getFragment()     { return mFragment; }

    public String getCreator()      { return mCreator; }
    public long getCreatedDate()    { return mCreatedDate; }


    public static void main(String[] args) throws Throwable {
        ParsedDocument pd;
        long timer, time;
        String creator = "test@zimbra.com";
        for (int i = 0; i < 5; i++) {
            timer = System.currentTimeMillis();
            pd = new ParsedDocument(new File("C:\\tmp\\todo.txt"), "todo.txt", "text/plain", timer, creator);
            time = (System.currentTimeMillis() - timer);
            System.out.println(pd.getFilename() + " (" + pd.getSize() + "b) {" + time + "us} [" + pd.getDigest() + "]: " + pd.getFragment());

            timer = System.currentTimeMillis();
            pd = new ParsedDocument(new File("C:\\tmp\\SOLTYREI.html"), "SOLTYREI.html", "text/html", timer, creator);
            time = (System.currentTimeMillis() - timer);
            System.out.println(pd.getFilename() + " (" + pd.getSize() + "b) {" + time + "us} [" + pd.getDigest() + "]: " + pd.getFragment());

            timer = System.currentTimeMillis();
            pd = new ParsedDocument(new File("C:\\tmp\\postgresql-8.2.1-US.pdf"), "postgresql-8.2.1-US.pdf", "application/pdf", timer, creator);
            time = (System.currentTimeMillis() - timer);
            System.out.println(pd.getFilename() + " (" + pd.getSize() + "b) {" + time + "us} [" + pd.getDigest() + "]: " + pd.getFragment());
        }
    }
}
