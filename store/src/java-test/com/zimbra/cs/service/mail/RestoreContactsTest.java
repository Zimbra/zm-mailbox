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
package com.zimbra.cs.service.mail;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.apache.http.StatusLine;
import org.apache.http.HttpResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.store.file.FileBlobStore;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ RestoreContacts.class, HttpResponse.class, StatusLine.class,
    FileBlobStore.class })
@PowerMockIgnore({ "javax.crypto.*", "javax.xml.bind.annotation.*" })
public class RestoreContactsTest {

    private Account acct = null;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
    }

    @Test
    public void testRestore() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Folder folder = mbox.createFolder(null, "Briefcase/ContactsBackup",
            new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));
        OperationContext octxt = new OperationContext(acct);
        // Upload the contacts backup file to ContactsBackup folder in briefcase
        mbox.createDocument(octxt, folder.getId(), "backup_dummy_test.tgz",
            MimeConstants.CT_APPLICATION_ZIMBRA_DOC, "author", "description",
            new ByteArrayInputStream("dummy data".getBytes()));
        HttpResponse httpResponse = PowerMockito.mock(HttpResponse.class);
        StatusLine httpStatusLine = PowerMockito.mock(StatusLine.class);
        Mockito.when(httpStatusLine.getStatusCode()).thenReturn(200);
        Mockito.when(httpResponse.getStatusLine()).thenReturn(httpStatusLine);
        PowerMockito.stub(PowerMockito.method(RestoreContacts.class, "httpPostBackup"))
            .toReturn(httpResponse);
        PowerMockito.stub(PowerMockito.method(FileBlobStore.class, "getBlobPath", Mailbox.class,
            int.class, int.class, short.class)).toReturn("/");
        // RestoreContactRequest with valid backup file name
        Element request = new Element.XMLElement(MailConstants.RESTORE_CONTACTS_REQUEST);
        request.addAttribute("contactsBackupFileName", "backup_dummy_test.tgz");
        Map<String, Object> context = ServiceTestUtil.getRequestContext(acct);
        Element response = new RestoreContacts().handle(request, context);
        String expectedResponse = "<RestoreContactsResponse status=\"SUCCESS\" xmlns=\"urn:zimbraMail\"/>";
        Assert.assertEquals(expectedResponse, response.prettyPrint());
        try {
            // RestoreContactRequest with non-existing backup file name
            Element request2 = new Element.XMLElement(MailConstants.RESTORE_CONTACTS_REQUEST);
            request2.addAttribute("contactsBackupFileName", "backup_dummy_test_non_existing.tgz");
            new RestoreContacts().handle(request2, context);
            Assert.fail("ServiceException was expected");
        } catch (ServiceException e) {
            Assert.assertEquals("invalid request: No such file: backup_dummy_test_non_existing.tgz",
                e.getMessage());
            Assert.assertEquals("service.INVALID_REQUEST", e.getCode());

        }
    }
}
