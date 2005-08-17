/*
 * Created on Apr 18, 2004
 */
package com.zimbra.cs.mime;

import java.util.List;

import javax.mail.internet.ContentType;
import javax.mail.internet.MimePart;


public class MPartInfo {
	MimePart mPart;
	MPartInfo mParent;
	List mChildren;
	String mPartName;
	ContentType mContentType;
	String mContentTypeString;
    String mDisposition = "";
    String mFilename = "";
	int mPartNum;
	Object mContent; // content set to MimeMultipart or MimeMessage if it was a multipart and/or message
    boolean mIsFAInit = false;
    boolean mIsFA;
    boolean mPullOutInit = false;
    boolean mPullOut;
	
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("MPartInfo: {");
        sb.append("partName: ").append(mPartName).append(", ");
        sb.append("contentType: ").append(mContentTypeString).append(", ");
        sb.append("disposition: ").append(mDisposition).append(", ");
        sb.append("filename: ").append(mFilename).append(", ");
        sb.append("partNum: ").append(mPartNum).append(", ");
        sb.append("isFilterableAttachment: ").append(isFilterableAttachment());
        sb.append("}");
        return sb.toString();
    }
    
	/**
	 * Returns true if we consider this to be an attachment for the sake of "filtering" by attachments.
	 * i.e., if someone searchs for messages with attachment types of "text/plain", we probably wouldn't want
	 * every multipart/mixed message showing up, since 99% of them will have a first body part of text/plain.
	 * 
	 * @param part
	 * @return
	 */
    public boolean isFilterableAttachment() {
        if (!mIsFAInit) {
            mIsFA = Mime.isFilterableAttachment(this);
            mIsFAInit = true;
        }
        return mIsFA;
    }

	public MimePart getMimePart() {
		return mPart;
	}
	
	public MPartInfo getParent() {
		return mParent;
	}
	
	public boolean hasChildren() {
		return mChildren != null && !mChildren.isEmpty();
	}

	public List getChildren() {
		return mChildren;
	}
	
	public String getPartName() {
		return mPartName;
	}
	
	public int getPartNum() {
		return mPartNum;
	}
	
	public ContentType getContentType() {
		return mContentType;
	}

    public String getContentTypeString() {
        return mContentTypeString;
    }

    public String getDisposition() {
        return mDisposition;
    }
    
    public String getFilename() {
        return mFilename;
    }
}