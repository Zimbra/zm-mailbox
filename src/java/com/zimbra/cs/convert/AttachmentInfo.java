/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jan 20, 2005
 *
 */
package com.zimbra.cs.convert;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents an attachment to a message or a piece of content within an archive-type
 * attachment. 
 * 
 * @author kchen
 */
public class AttachmentInfo {
    private InputStream mInstream;
    private String mContentType;
    private String mPart;
    private List mSeqInArchive;
    private String mDigest;
    private String mFilename;
    
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename, List seq) {
        mInstream = in;
        mDigest = digest;
        mContentType = ct;
        mPart = p;
        mFilename = filename;
        mSeqInArchive = new ArrayList(seq.size());
        mSeqInArchive.addAll(seq);
    }
    
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename, String[] seq) {
        this(in, digest, ct, p, filename, Arrays.asList(seq));
    }
   
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename) {
        this(in, digest, ct, p, filename, new String[0]);
    }

    /**
     * @return Returns the Seq ID of a piece of content in the Archive.
     */
    public List getSeqInArchive() {
        return new ArrayList(mSeqInArchive);
    }

    /**
     * @return Returns the Part.
     */
    public String getPart() {
        return mPart;
    }
    
    /**
     * @return Returns the ContentType.
     */
    public String getContentType() {
        return mContentType;
    }
    
    /**
     * @return Returns the Instream.
     */
    public InputStream getInputStream() {
        return mInstream;
    }
    
    /**
     * 
     * @return the digest of the containing message. This is the digest
     * computed over the body of the message plus all its attachments.
     */
    public String getDigest() {
        return mDigest;
    }
    
    /**
     * @return the relative file path to the converted main HTML file.
     * Its general construct is: <msg digest>/<attachment part id>[/<seq id1>/<seq id2>/.../<seq idn>].html
     * where <seq idn> refers to the sequence ID for content piece within an archive type attachment.
     * They are not applicable if the attachment is not archive type.
     */
    public String getFilePath() {
        StringBuffer buf = new StringBuffer();
        buf.append(mDigest).append(File.separator);
        buf.append(mPart);
        for (int i = 0; i < mSeqInArchive.size(); i++) {
            buf.append(File.separator);
            buf.append((String) mSeqInArchive.get(i));
        }
        buf.append(".html");
        return buf.toString();
    }
    
    public String getPartId() {
        return mDigest + "_" + mPart;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("AttachmentInfo: input stream=").append(mInstream.getClass());
        buf.append(", content type=").append(mContentType);
        return buf.toString();
    }

    /**
     * 
     */
    public void resetSequences() {
        mSeqInArchive.clear();
    }

    /**
     * @return
     */
    public String getFilename() {
        return mFilename;
    }
}
