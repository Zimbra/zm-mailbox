/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.store.file;

import java.io.Serializable;

import com.zimbra.znative.IO.FileInfo;

public class BlobReference implements Serializable {

    private static final long serialVersionUID = -1132877177535802600L;

    private long id;
    private int mailboxId;
    private short volumeId;
    private int itemId;
    private int revision;
    private String digest;
    private boolean processed;
    private FileInfo fileInfo;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public int getMailboxId() {
        return mailboxId;
    }
    public void setMailboxId(int mailboxId) {
        this.mailboxId = mailboxId;
    }
    public short getVolumeId() {
        return volumeId;
    }
    public void setVolumeId(short volumeId) {
        this.volumeId = volumeId;
    }
    public int getItemId() {
        return itemId;
    }
    public void setItemId(int itemId) {
        this.itemId = itemId;
    }
    public int getRevision() {
        return revision;
    }
    public void setRevision(int revision) {
        this.revision = revision;
    }
    public String getDigest() {
        return digest;
    }
    public void setDigest(String digest) {
        this.digest = digest;
    }
    public boolean isProcessed() {
        return processed;
    }
    public void setProcessed(boolean processed) {
        this.processed = processed;
    }
    
    public FileInfo getFileInfo() {
        return fileInfo;
    }
    
    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

}
