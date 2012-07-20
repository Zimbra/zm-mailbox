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
package com.zimbra.cs.store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.store.MailboxBlob.MailboxBlobInfo;

public class MailboxBlobTest {
    @Test
    public void serialization() throws Exception {
        MailboxBlobInfo mbinfo = new MailboxBlobInfo(MockProvisioning.DEFAULT_ACCOUNT_ID, Mailbox.FIRST_USER_ID, 1, "locator");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(mbinfo);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MailboxBlobInfo mbi2 = (MailboxBlobInfo) ois.readObject();
        Assert.assertEquals(mbinfo.accountId, mbi2.accountId);
        Assert.assertEquals(mbinfo.itemId, mbi2.itemId);
        Assert.assertEquals(mbinfo.revision, mbi2.revision);
        Assert.assertEquals(mbinfo.locator, mbi2.locator);
    }
}
