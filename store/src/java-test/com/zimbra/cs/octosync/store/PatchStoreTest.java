/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.octosync.store;

import java.io.DataInputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.StoreManager;

public class PatchStoreTest
{
    @BeforeClass
    public static void init() throws Exception
    {
        MailboxTestUtil.initServer();
    }

    private StoreManagerBasedTempBlobStore blobStore;
    private PatchStore patchStore;

    @Before
    public void setUp() throws Exception
    {
        MailboxTestUtil.clearData();

        blobStore = new StoreManagerBasedTempBlobStore(
                StoreManager.getInstance(), 0, 0, true);

        patchStore = new PatchStore(blobStore);
    }

    @Test
    public void acceptTest() throws Exception
    {
        PatchStore.IncomingPatch ip = patchStore.createIncomingPatch("foo");
        ip.getOutputStream().write("hello world".getBytes());
        patchStore.acceptPatch(ip, 1, 1);

        PatchStore.StoredPatch sp = patchStore.lookupPatch("foo", 1, 1);
        byte[] buf = new byte[11];
        new DataInputStream(sp.getInputStream()).readFully(buf);

        Assert.assertArrayEquals(buf, "hello world".getBytes());
    }

    @Test
    public void deleteTest() throws Exception
    {
        acceptTest();

        {
            PatchStore.StoredPatch sp = patchStore.lookupPatch("foo", 1, 1);
            Assert.assertNotNull(sp);
            patchStore.deletePatch("foo", 1);
        }

        PatchStore.StoredPatch sp = patchStore.lookupPatch("foo", 1, 1);

        Assert.assertNull(sp);
    }

    @Test
    public void rejectTest() throws Exception
    {
        PatchStore.IncomingPatch ip = patchStore.createIncomingPatch("foo");
        ip.getOutputStream().write("hello world".getBytes());
        patchStore.rejectPatch(ip);

        PatchStore.StoredPatch sp = patchStore.lookupPatch("foo", 1, 1);

        Assert.assertNull(sp);
    }

}
