package com.zimbra.cs.store.external;

import java.io.File;
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

public class ExternalBlobConsistencyCheckTest extends AbstractBlobConsistencyCheckTest {

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
