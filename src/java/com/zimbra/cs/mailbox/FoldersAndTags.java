/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public class FoldersAndTags {
    private static final int DATA_VERSION = 1;
    private static final String FN_DATA_VERSION = "dv";
    private static final String FN_FOLDERS = "folders";
    private static final String FN_TAGS = "tags";

    /** primary index of folder metadata */
    private MetadataList mFolders = new MetadataList();

    /** primary index of tag metadata */
    private MetadataList mTags = new MetadataList();

    /** Default constructor */
    public FoldersAndTags() {}

    /** Constructor */
    public FoldersAndTags(List<Folder> folders, List<Tag> tags) {
        this();
        for (Folder f : folders) {
            add(f);
        }
        for (Tag t : tags) {
            add(t);
        }
    }

    /** Constructor, hydrating state from serializable data */
    public FoldersAndTags(MetadataList folders, MetadataList tags) {
        mFolders = folders;
        mTags = tags;
    }

    public void add(Folder folder) {
        mFolders.add(folder.serializeUnderlyingData());
    }

    public void add(Tag tag) {
        mTags.add(tag.serializeUnderlyingData());
    }

    public Metadata encode() {
        Metadata meta = new Metadata();
        meta.put(FN_DATA_VERSION, DATA_VERSION);
        meta.put(FN_FOLDERS, mFolders);
        meta.put(FN_TAGS, mTags);
        return meta;
    }

    public static FoldersAndTags decode(Metadata meta) throws ServiceException {
        int ver = (int) meta.getLong(FN_DATA_VERSION, 0);
        if (ver != DATA_VERSION) {
            ZimbraLog.mailbox.info("Ignoring cached folders/tags with stale data version");
            return null;
        }
        MetadataList folders = meta.getList(FN_FOLDERS);
        MetadataList tags = meta.getList(FN_TAGS);
        return new FoldersAndTags(folders, tags);
    }

    public List<Metadata> getFolderMetadata() {
        List<Metadata> toRet = new ArrayList<Metadata>();
        List<Object> list = mFolders.asList();
        for (Object obj : list) {
            if (obj instanceof Metadata)
                toRet.add((Metadata) obj);
        }
        return toRet;
    }

    public List<Metadata> getTagMetadata() {
        List<Metadata> toRet = new ArrayList<Metadata>();
        List<Object> list = mTags.asList();
        for (Object obj : list) {
            if (obj instanceof Metadata)
                toRet.add((Metadata) obj);
        }
        return toRet;
    }
}
