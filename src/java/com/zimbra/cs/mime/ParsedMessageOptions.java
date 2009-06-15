/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.File;

import javax.mail.internet.MimeMessage;

public class ParsedMessageOptions {

    private MimeMessage mMimeMessage;
    private byte[] mRawData;
    private File mFile;
    private String mRawDigest;
    private Integer mRawSize;
    private Long mReceivedDate;
    private Boolean mIndexAttachments;
    
    public ParsedMessageOptions() {
    }
    
    public ParsedMessageOptions withContent(MimeMessage mimeMessage) {
        if (mRawData != null || mFile != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mMimeMessage = mimeMessage;
        return this;
    }
    
    public ParsedMessageOptions withContent(byte[] rawData) {
        if (mMimeMessage != null || mFile != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mRawData = rawData;
        return this;
    }
    
    public ParsedMessageOptions withContent(File file) {
        if (mRawData != null || mMimeMessage != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mFile = file;
        return this;
    }
    
    public ParsedMessageOptions withDigest(String digest) {
        mRawDigest = digest;
        return this;
    }
    
    public ParsedMessageOptions withSize(long size) {
        mRawSize = (int) size;
        return this;
    }
    
    public ParsedMessageOptions withReceivedDate(long receivedDate) {
        mReceivedDate = receivedDate;
        return this;
    }
    
    public ParsedMessageOptions withAttachmentIndexing(boolean enabled) {
        mIndexAttachments = enabled;
        return this;
    }
    
    public MimeMessage getMimeMessage() { return mMimeMessage; }
    public byte[] getRawData() { return mRawData; }
    public File getFile() { return mFile; }
    public String getDigest() { return mRawDigest; }
    public Integer getSize() { return mRawSize; }
    public Long getReceivedDate() { return mReceivedDate; }
    public Boolean getAttachmentIndexing() { return mIndexAttachments; }
}
