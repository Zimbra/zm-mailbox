/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.store.file;

import java.io.IOException;
import java.io.ObjectInputStream;
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

    private final void readObject(ObjectInputStream in) throws java.io.IOException {
        throw new IOException("Cannot be deserialized");
    }

}
