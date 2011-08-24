/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.index.global;

/**
 * Document item in the global index store.
 *
 * @author ysasaki
 */
public final class GlobalDocument {
    private final GlobalItemID gid;
    private String filename;
    private String creator;
    private String mimeType;
    private String fragment;
    private long date;
    private long size;

    GlobalDocument(GlobalItemID gid) {
        this.gid = gid;
    }

    public GlobalItemID getGID() {
        return gid;
    }

    void setCreator(String value) {
        creator = value;
    }

    String getCreator() {
        return creator;
    }

    void setMimeType(String value) {
        mimeType = value;
    }

    String getMimeType() {
        return mimeType;
    }

    void setFragment(String value) {
        fragment = value;
    }

    public String getFragment() {
        return fragment;
    }

    void setDate(long value) {
        date = value;
    }

    public long getDate() {
        return date;
    }

    void setFilename(String value) {
        filename = value;
    }

    public String getFilename() {
        return filename;
    }

    void setSize(long value) {
        size = value;
    }

    public long getSize() {
        return size;
    }
}
