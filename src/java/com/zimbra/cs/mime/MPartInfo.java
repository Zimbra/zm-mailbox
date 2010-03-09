/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
 * Created on Apr 18, 2004
 */
package com.zimbra.cs.mime;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimePart;

import com.zimbra.common.mime.ContentType;

public class MPartInfo {
    MimePart mPart;
    MPartInfo mParent;
    List<MPartInfo> mChildren;
    String mPartName;
    String mContentType;
    String mDisposition;
    String mFilename;
    int mPartNum;
    int mSize;
    boolean mIsFilterableAttachment;
    boolean mIsToplevelAttachment;

    @Override public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("MPartInfo: {");
        sb.append("partName: ").append(mPartName).append(", ");
        sb.append("contentType: ").append(mContentType).append(", ");
        sb.append("size: ").append(mSize).append(", ");
        sb.append("disposition: ").append(mDisposition).append(", ");
        sb.append("filename: ").append(mFilename).append(", ");
        sb.append("partNum: ").append(mPartNum).append(", ");
        sb.append("isFilterableAttachment: ").append(mIsFilterableAttachment);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns true if we consider this to be an attachment for the sake of "filtering" by attachments.
     * i.e., if someone searches for messages with attachment types of "text/plain", we probably wouldn't want
     * every multipart/mixed message showing up, since 99% of them will have a first body part of text/plain.
     * 
     * @param part
     * @return
     */
    public boolean isFilterableAttachment() {
        return mIsFilterableAttachment;
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

    public int getSize() {
        return mSize;
    }

    public String getContentTypeParameter(String name) {
        try {
            return new ContentType(mPart.getContentType()).getParameter(name);
        } catch (MessagingException e) {
            return null;
        }
    }

    public String getContentID() {
        try {
            return mPart.getContentID();
        } catch (MessagingException me) {
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