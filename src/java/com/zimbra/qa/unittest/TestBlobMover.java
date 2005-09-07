/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.hsm.BlobMover;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.Volume;
import com.zimbra.cs.util.FileUtil;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;

public class TestBlobMover extends TestCase {
    
    private static final String TEMP_VOLUME_DIR_NAME =
        System.getProperty("java.io.tmpdir", "/tmp") + "/TestBlobMover";
    private static final File TEMP_VOLUME_DIR = new File(TEMP_VOLUME_DIR_NAME);
    private static final String VOLUME_NAME = StringUtil.getSimpleClassName(TestBlobMover.class.getName());
    private static final String USER_NAME = "user1";
    private File mSourceDir;
    private File mDestDir;
    private int mFileCount = -1;
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        Volume volume = Volume.create(Volume.ID_AUTO_INCREMENT, Volume.TYPE_MESSAGE_SECONDARY, VOLUME_NAME,
            TEMP_VOLUME_DIR_NAME, (short) 8, (short) 12, (short) 8, (short) 12);
        assertNotNull(volume);
        Volume.setCurrentVolume(Volume.TYPE_MESSAGE_SECONDARY, volume.getId());
        assertEquals(volume, Volume.getCurrentSecondaryMessageVolume());
        ZimbraLog.test.debug("Created " + volume);
    }

    public void testBlobMover()
    throws Exception {
        BlobMover mover = new BlobMover();
        Volume source = Volume.getCurrentMessageVolume();
        Volume dest = Volume.getCurrentSecondaryMessageVolume();
        Account account = TestUtil.getAccount(USER_NAME);
        Mailbox mbox = Mailbox.getMailboxByAccount(account);
        
        mSourceDir = new File(source.getRootPath());
        mDestDir = new File(dest.getRootPath());
        mFileCount = getFileCount(mSourceDir);

        // Verify filesystem and volume id's before move
        assertTrue("No files found in " + mSourceDir.getPath(), getFileCount(mSourceDir) > 0);
        assertEquals("Files found in " + mDestDir.getPath(), 0, getFileCount(mDestDir));
        verifyVolume(mbox, source);
        
        // Move messages to destination and verify
        mover.moveItems(mbox, Long.MAX_VALUE, dest.getId());
        // Get a fresh copy of the mailbox, since moveItems() cleared the
        // mailbox cache entry
        mbox = Mailbox.getMailboxByAccount(account);
        
        assertEquals("Files found in " + mSourceDir.getPath(), 0, getFileCount(mSourceDir));
        assertEquals("Incorrect number of files in " + mDestDir.getPath(),
            mFileCount, getFileCount(mDestDir));
        verifyVolume(mbox, dest);
    }

    private int getFileCount(File path)
    throws IOException {
        if (!path.exists())
            return 0;
        if (!path.isDirectory()) {
            return 1;
        }
        File[] files = path.listFiles();
        int fileCount = 0;
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    fileCount += getFileCount(files[i]);
                } else {
                    fileCount++;
                }
            }
        }
        return fileCount;
    }

    /**
     * Confirms that all the messages in <code>mbox</code> are stored in volume <code>v</code>.
     */
    private void verifyVolume(Mailbox mbox, Volume v)
    throws Exception {
        List items = mbox.getItemList(MailItem.TYPE_MESSAGE);
        List appointments = mbox.getItemList(MailItem.TYPE_APPOINTMENT);
        items.addAll(appointments);
        
        Iterator i = items.iterator();
        while (i.hasNext()) {
            MailItem item = (MailItem) i.next();
            assertEquals("Volume id mismatch", v.getId(), item.getVolumeId());
            assertNotNull("Could not find blob for " + item, item.getBlob());
        }
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        // Remove temporary volume
        Iterator i = Volume.getAll().iterator();
        while (i.hasNext()) {
            Volume v = (Volume) i.next();
            if (v.getName().equals(VOLUME_NAME)) {
                // Found temp volume.  Move any messages stored there.
                Account account = TestUtil.getAccount(USER_NAME);
                Mailbox mbox = Mailbox.getMailboxByAccount(account);
                Volume primary = Volume.getCurrentMessageVolume();
                ZimbraLog.test.debug("Moving messages back to " + primary);
                BlobMover mover = new BlobMover();
                
                mover.moveItems(mbox, Long.MAX_VALUE, primary.getId());
                // Get a fresh copy of the mailbox, since moveItems() cleared the
                // mailbox cache entry
                mbox = Mailbox.getMailboxByAccount(account);
                
                // Make sure files were moved back
                if (mFileCount >= 0) {
                    assertEquals("Incorect number of files in " + mSourceDir.getPath(),
                        mFileCount, getFileCount(mSourceDir));
                    assertEquals("Files found in " + mDestDir.getPath(), 0, getFileCount(mDestDir));
                }

                // Delete the temp volume
                ZimbraLog.test.debug("Deleting " + v);
                Volume.setCurrentVolume(Volume.TYPE_MESSAGE_SECONDARY, Volume.ID_NONE);
                Volume.delete(v.getId(), true);
            }
        }

        // Remove volume directory
        if (TEMP_VOLUME_DIR.exists()) {
            ZimbraLog.test.debug("Deleting " + TEMP_VOLUME_DIR_NAME);
            FileUtil.deleteDir(TEMP_VOLUME_DIR);
        }
        assertFalse("Detected " + TEMP_VOLUME_DIR_NAME, TEMP_VOLUME_DIR.exists());
    }
}
