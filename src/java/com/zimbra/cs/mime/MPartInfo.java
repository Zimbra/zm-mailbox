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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 18, 2004
 */
package com.zimbra.cs.mime;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;


public class MPartInfo {
	MimePart mPart;
	MPartInfo mParent;
	List<MPartInfo> mChildren;
	String mPartName;
	String mContentType = "";
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
        sb.append("contentType: ").append(mContentType).append(", ");
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

	public List<MPartInfo> getChildren() {
		return mChildren;
	}
	
	public String getPartName() {
		return mPartName;
	}
	
	public int getPartNum() {
		return mPartNum;
	}

    public String getContentType() {
        return mContentType;
    }

    public String getContentTypeParameter(String name) {
        try {
            return new MimeCompoundHeader(mPart.getContentType()).getParameter(name);
        } catch (MessagingException e) {
            return null;
        }
    }

    public String getDisposition() {
        return mDisposition;
    }
    
    public String getFilename() {
        return mFilename;
    }
}