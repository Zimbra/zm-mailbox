/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    private Long mRawSize;
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
        mRawSize = size;
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
    public Long getSize() { return mRawSize; }
    public Long getReceivedDate() { return mReceivedDate; }
    public Boolean getAttachmentIndexing() { return mIndexAttachments; }
}
