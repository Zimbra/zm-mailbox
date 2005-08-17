/*
 * Created on Jan 20, 2005
 *
 */
package com.liquidsys.coco.convert;

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
