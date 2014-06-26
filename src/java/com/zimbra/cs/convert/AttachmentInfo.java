/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010, 2013, 2014 Zimbra, Inc.
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
    private List<String> mSeqInArchive;
    private String mDigest;
    private String mFilename;
    private long mLength;
    
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename, long length, List<String> seq) {
        mInstream = in;
        mDigest = digest;
        mContentType = ct;
        mPart = p;
        mFilename = filename;
        mLength = length;
        mSeqInArchive = new ArrayList<String>(seq.size());
        mSeqInArchive.addAll(seq);
    }
    
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename, long length, String[] seq) {
        this(in, digest, ct, p, filename, length, Arrays.asList(seq));
    }
   
    public AttachmentInfo(InputStream in, String digest, String ct, String p, String filename, long length) {
        this(in, digest, ct, p, filename, length, new String[0]);
    }

    /**
     * @return Returns the Seq ID of a piece of content in the Archive.
     */
    public List<String> getSeqInArchive() {
        return new ArrayList<String>(mSeqInArchive);
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
            buf.append(mSeqInArchive.get(i));
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
    
    public long getLength() {
        return mLength;
    }
}
