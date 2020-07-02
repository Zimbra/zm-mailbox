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
package com.zimbra.cs.store.external;

import java.io.File;
import org.junit.Ignore;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.AbstractBlobConsistencyCheckTest;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.BlobConsistencyChecker;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class ExternalBlobConsistencyCheckTest extends AbstractBlobConsistencyCheckTest {

    @Override
    protected void deleteAllBlobs() throws ServiceException, IOException {
        SimpleStoreManager sm = (SimpleStoreManager) StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        List<String> allPaths = sm.getAllBlobPaths(mbox);
        if (allPaths != null) {
            for (String path : allPaths) {
                new File(path).delete();
            }
        }
    }

    @Override
    protected void appendText(MailboxBlob blob, String text) throws IOException {
        FileWriter writer = new FileWriter(blob.getLocalBlob().getFile(), true);
        writer.write(text);
        writer.close();
    }

    @Override
    protected StoreManager getStoreManager() {
        return new SimpleStoreManager();
    }

    @Override
    protected BlobConsistencyChecker getChecker() {
        return new ExternalBlobConsistencyChecker();
    }

    @Override
    protected Collection<Short> getVolumeIds() {
        return null;
    }

    @Override
    protected String createUnexpectedBlob(int index) throws ServiceException, IOException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        SimpleStoreManager sm = (SimpleStoreManager) StoreManager.getInstance();
        String dir = sm.dirName(mbox);
        File file = new File(dir + "/foo" + index + ".txt");
        file.createNewFile();
        return file.getCanonicalPath();
    }
}
