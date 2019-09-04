package com.zimbra.cs.mailbox.cache;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;

public class LocalFolderTagSnapshotCache extends FolderTagSnapshotCache {

    private Metadata folderTagMeta = null;

    public LocalFolderTagSnapshotCache(Mailbox mbox) {
        super(mbox);
    }

    @Override
    protected Metadata getCachedTagsAndFolders() {
        return folderTagMeta;
    }

    @Override
    protected void cacheFoldersTagsMeta(Metadata folderTagMeta) {
        this.folderTagMeta = folderTagMeta;
    }
}
