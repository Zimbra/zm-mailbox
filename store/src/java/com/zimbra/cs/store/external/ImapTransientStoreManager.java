/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.FileUtil;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;

/**
 * Simple ExternalStoreManager implementation that is intended for use in storing
 * transient blobs.
 *
 * <p>This StoreManager is used by the {@link com.zimbra.cs.imap.ImapDaemon} while
 * constructing blobs for an APPEND operation.  The blobs are deleted when the
 * APPEND is finalized.
 *
 */
public class ImapTransientStoreManager extends ExternalStoreManager {

    protected File baseDirectory;

    @Override
    public void startup() throws IOException, ServiceException {
        startup(LC.imapd_tmp_directory.value());
    }

    public void startup(String baseDirectoryPath) throws IOException, ServiceException {
        super.startup();
        baseDirectory = new File(baseDirectoryPath);
        FileUtil.mkdirs(baseDirectory);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }

    @Override
    public String writeStreamToStore(InputStream in, long actualSize, Mailbox mbox)
            throws IOException, ServiceException {
        File destFile = createBlobFile(mbox);
        FileUtil.copy(in, false, destFile);
        return destFile.getCanonicalPath();
    }

    @Override
    public InputStream readStreamFromStore(String locator, Mailbox mbox) throws IOException {
        return new FileInputStream(locator);
    }

    @Override
    public boolean deleteFromStore(String locator, Mailbox mbox) throws IOException {
        File deleteFile = new File(locator);
        return deleteFile.delete();
    }

    @Override
    public boolean supports(StoreFeature feature) {
        return feature == StoreFeature.CENTRALIZED ? false : super.supports(feature);
    }

    private File createBlobFile(Mailbox mbox) throws IOException {
        synchronized (this) {
            return File.createTempFile(mbox.getAccountId(), ".msg", baseDirectory);
        }
    }

	@Override
	public MailboxBlob copy(MailboxBlob src, Mailbox destMbox, int destMsgId, long destRevision)
			throws IOException, ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MailboxBlob link(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision)
			throws IOException, ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MailboxBlob renameTo(StagedBlob src, Mailbox destMbox, int destMsgId, long destRevision)
			throws IOException, ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MailboxBlob getMailboxBlob(Mailbox mbox, int itemId, long revision, String locator, boolean validate)
			throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

}
