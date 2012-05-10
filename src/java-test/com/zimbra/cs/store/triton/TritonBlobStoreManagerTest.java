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
package com.zimbra.cs.store.triton;

import java.io.ByteArrayInputStream;
import java.util.Random;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.store.AbstractStoreManagerTest;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StagedBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.StoreManager.StoreFeature;
import com.zimbra.cs.store.external.ContentAddressableStoreManager;
import com.zimbra.cs.store.triton.TritonBlobStoreManager.HashType;
import com.zimbra.qa.unittest.TestUtil;

@Ignore("requires Triton server")
public class TritonBlobStoreManagerTest extends AbstractStoreManagerTest {
    @Override
    protected StoreManager getStoreManager() {
        return new TritonBlobStoreManager("http://192.168.2.107", HashType.SHA0) {
//        return new TritonBlobStoreManager("http://10.33.30.77", HashType.SHA0) {

            @Override
            public boolean supports(StoreFeature feature) {
                switch (feature) {
                    //normally TBSM only supports SIS if using SHA256, force it here
                    case SINGLE_INSTANCE_SERVER_CREATE : return true;
                    default: return super.supports(feature);
                }
            }
        };
    }

    @Test
    public void sis() throws Exception {
        TritonBlobStoreManager sm = (TritonBlobStoreManager) StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Assert.assertTrue("StoreManager is content addressable", sm instanceof ContentAddressableStoreManager);
        Assert.assertTrue("StoreManager supports SIS check", sm.supports(StoreFeature.SINGLE_INSTANCE_SERVER_CREATE));

        Random rand = new Random();
        byte[] bytes = new byte[10000];

        rand.nextBytes(bytes);

        Blob blob = sm.storeIncoming(new ByteArrayInputStream(bytes));

        //blob has not yet been finalized, so it shouldn't exist in remote system yet

        byte[] hash = sm.getHash(blob);

        Assert.assertNull("object not yet created", sm.getSisBlob(hash));

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));

        //blob uploaded, now sis should return true and increment ref count
        Blob sisBlob = sm.getSisBlob(hash);
        Assert.assertNotNull("object created", sisBlob);

        Assert.assertEquals("blob size = incoming written", bytes.length, sisBlob.getRawSize());
        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, sisBlob.getInputStream()));

        //delete once, should still exist;
        sm.delete(mblob);
        Assert.assertNotNull("object still ref'd", sm.getSisBlob(hash));

        //delete twice (once for original, once since we just did a sisCheck above)
        sm.delete(mblob);
        sm.delete(mblob);

        Assert.assertNull("object deleted", sm.getSisBlob(hash));
    }

    @Test
    public void sisEmpty() throws Exception {
        TritonBlobStoreManager sm = (TritonBlobStoreManager) StoreManager.getInstance();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        Assert.assertTrue("StoreManager is content addressable", sm instanceof ContentAddressableStoreManager);
        Assert.assertTrue("StoreManager supports SIS check", sm.supports(StoreFeature.SINGLE_INSTANCE_SERVER_CREATE));

        byte[] bytes = new byte[0];

        Blob blob = sm.storeIncoming(new ByteArrayInputStream(bytes));

        //blob has not yet been finalized, so it shouldn't exist in remote system yet

        byte[] hash = sm.getHash(blob);

        Assert.assertNotNull("empty blob always exists", sm.getSisBlob(hash));

        Assert.assertEquals("blob size = incoming written", bytes.length, blob.getRawSize());

        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, blob.getInputStream()));

        StagedBlob staged = sm.stage(blob, mbox);
        Assert.assertEquals("staged size = blob size", blob.getRawSize(), staged.getSize());

        MailboxBlob mblob = sm.link(staged, mbox, 0, 0);
        Assert.assertEquals("link size = staged size", staged.getSize(), mblob.getSize());
        Assert.assertTrue("link content = mime content", TestUtil.bytesEqual(bytes, mblob.getLocalBlob().getInputStream()));


        //blob uploaded, now sis should return true and increment ref count
        Blob sisBlob = sm.getSisBlob(hash);
        Assert.assertNotNull("object created", sisBlob);


        Assert.assertEquals("blob size = incoming written", bytes.length, sisBlob.getRawSize());
        Assert.assertTrue("blob content = mime content", TestUtil.bytesEqual(bytes, sisBlob.getInputStream()));
    }
}
