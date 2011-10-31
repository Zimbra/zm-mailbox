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
                StoreManager.getInstance(), 0, 0);

        patchStore = new PatchStore(blobStore);
    }

    @Test
    public void acceptTest() throws Exception
    {
        PatchStore.IncomingPatch ip = patchStore.createIncomingPatch("foo", "123");
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
        PatchStore.IncomingPatch ip = patchStore.createIncomingPatch("foo", "123");
        ip.getOutputStream().write("hello world".getBytes());
        patchStore.rejectPatch(ip);

        PatchStore.StoredPatch sp = patchStore.lookupPatch("foo", 1, 1);

        Assert.assertNull(sp);
    }

}
