/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.AbstractBlobConsistencyCheckTest;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;

public class FileBlobStoreConsistencyCheckTest extends AbstractBlobConsistencyCheckTest {

    @Override
    public BlobConsistencyChecker getChecker() {
        return new BlobConsistencyChecker();
    }

    @Override
    public Collection<Short> getVolumeIds() {
        Volume vol = VolumeManager.getInstance().getCurrentMessageVolume();
        Set<Short> volumes = new HashSet<Short>();
        volumes.add(vol.getId());
        return volumes;
    }

    @Override
    protected void deleteAllBlobs() throws ServiceException, IOException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Volume vol = VolumeManager.getInstance().getCurrentMessageVolume();
        String dir = vol.getMailboxDir(mbox.getId(), Volume.TYPE_MESSAGE);
        log.info("mailbox dir is %s", dir);
        File blobDir = new File(dir);
        FileUtil.deleteDirContents(blobDir);
    }

    @Override
    protected void appendText(MailboxBlob blob, String text) throws IOException {
        FileWriter writer = new FileWriter(blob.getLocalBlob().getFile(), true);
        writer.write(text);
        writer.close();
    }

    @Override
    protected String createUnexpectedBlob(int index) throws ServiceException, IOException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Volume vol = VolumeManager.getInstance().getCurrentMessageVolume();
        String dir = vol.getBlobDir(mbox.getId(), 0);

        File file = new File(dir + "/foo" + index + ".txt");
        file.mkdirs();
        file.createNewFile();
        return file.getCanonicalPath();
    }

    @Override
    protected StoreManager getStoreManager() {
        return new FileBlobStore();
    }

}
