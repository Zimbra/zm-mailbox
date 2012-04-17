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

package com.zimbra.cs.store.external;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * Example implementation of ExternalStoreManager which writes to a flat directory structure
 * This is intended for illustration purposes only; it should *never* be used in a production environment
 *
 */
public class SimpleStoreManager extends ExternalStoreManager {

    String directory = "/tmp/simplestore";

    @Override
    public void startup() throws IOException, ServiceException {
        super.startup();
        ZimbraLog.store.info("Using SimpleStoreManager. If you are seeing this in production you have done something WRONG!");
        FileUtil.mkdirs(new File(directory));
    }

    private String dirName(Mailbox mbox) {
        return directory + "/" + mbox.getAccountId();
    }

    private File getNewFile(Mailbox mbox) throws IOException {
        String baseName = dirName(mbox);
        FileUtil.mkdirs(new File(baseName));
        baseName += "/zimbrablob";
        String name = baseName;
        synchronized (this) {
            int count = 1;
            File file = new File(name+".msg");
            while (file.exists()) {
                name = baseName+"_"+count++;
                file = new File(name+".msg");
            }
            if (file.createNewFile()) {
                ZimbraLog.store.debug("writing to new file %s",file.getName());
                return file;
            } else {
                throw new IOException("unable to create new file");
            }
        }
    }

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox) throws IOException {
        File destFile = getNewFile(mbox);
        FileUtil.copy(in, false, destFile);
        return destFile.getCanonicalPath();
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException {
        return new FileInputStream(locator);
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox mbox) throws IOException {
        File deleteFile = new File(dirName(mbox)+"/"+locator);
        return deleteFile.delete();
    }

    @Override
    protected boolean isCentralized() {
        return false;
    }
}
