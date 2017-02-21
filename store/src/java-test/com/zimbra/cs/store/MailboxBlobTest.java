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
        MailboxBlobInfo mbinfo = new MailboxBlobInfo(MockProvisioning.DEFAULT_ACCOUNT_ID, 1, Mailbox.FIRST_USER_ID, 1, "locator", "digest123");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(mbinfo);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MailboxBlobInfo mbi2 = (MailboxBlobInfo) ois.readObject();
        Assert.assertEquals(mbinfo.accountId, mbi2.accountId);
        Assert.assertEquals(mbinfo.mailboxId, mbi2.mailboxId);
        Assert.assertEquals(mbinfo.itemId, mbi2.itemId);
        Assert.assertEquals(mbinfo.revision, mbi2.revision);
        Assert.assertEquals(mbinfo.locator, mbi2.locator);
        Assert.assertEquals(mbinfo.digest, mbi2.digest);
    }
}
