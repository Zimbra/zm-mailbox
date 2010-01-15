/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mime;

import java.io.File;
import java.io.IOException;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.store.Blob;

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
    
    public ParsedMessageOptions(Blob blob, byte buffer[]) throws IOException {
        this(blob, buffer, null, null);
    }

    public ParsedMessageOptions(Blob blob, byte buffer[], Long receivedDate,
        Boolean indexAttachments) throws IOException {
        if (buffer == null)
            setContent(blob.getFile());
        else
            setContent(buffer);
        setDigest(blob.getDigest()).setSize(blob.getRawSize());
        if (receivedDate != null)
            setReceivedDate(receivedDate);
        if (indexAttachments != null)
            setAttachmentIndexing(indexAttachments);
    }
    
    public ParsedMessageOptions setContent(MimeMessage mimeMessage) {
        if (mRawData != null || mFile != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mMimeMessage = mimeMessage;
        return this;
    }
    
    public ParsedMessageOptions setContent(byte[] rawData) {
        if (mMimeMessage != null || mFile != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mRawData = rawData;
        return this;
    }
    
    public ParsedMessageOptions setContent(File file) {
        if (mRawData != null || mMimeMessage != null) {
            throw new IllegalArgumentException("Content can only come from one source.");
        }
        mFile = file;
        return this;
    }
    
    public ParsedMessageOptions setDigest(String digest) {
        mRawDigest = digest;
        return this;
    }
    
    public ParsedMessageOptions setSize(long size) {
        mRawSize = (int) size;
        return this;
    }
    
    public ParsedMessageOptions setReceivedDate(long receivedDate) {
        mReceivedDate = receivedDate;
        return this;
    }
    
    public ParsedMessageOptions setAttachmentIndexing(boolean enabled) {
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
