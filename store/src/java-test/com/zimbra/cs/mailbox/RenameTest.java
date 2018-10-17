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
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import org.junit.Ignore;
import java.io.InputStream;
import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedDocument;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class RenameTest {
    private Mailbox mbox;
    private Folder folder;
    private MailItem doc;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        folder = mbox.createFolder(null, "/Briefcase/f", new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
        InputStream in = new ByteArrayInputStream("This is a document".getBytes());
        ParsedDocument pd = new ParsedDocument(in, "doc.txt", "text/plain", System.currentTimeMillis(), null, null);
        doc = mbox.createDocument(null, folder.getId(), pd, MailItem.Type.DOCUMENT, 0);
    }

    @Test
    public void renameModContentTest() throws Exception {
        int id = doc.getId();
        int mod_content = doc.getSavedSequence();
        mbox.rename(null, id, doc.getType(), "newdoc.txt", folder.getId());
        mbox.purge(MailItem.Type.UNKNOWN);
        MailItem newdoc = mbox.getItemById(null, id, MailItem.Type.UNKNOWN, false);
        Assert.assertEquals(mod_content, newdoc.getSavedSequence());
    }
}
