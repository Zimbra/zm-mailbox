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
package com.zimbra.cs.service.mail;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Mountpoint;

public class GetFolderTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void depth() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.createFolder(null, "Inbox/foo/bar/baz", new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT));

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        Element response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        Element folder = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", folder);

        folder = getSubfolder(folder, "Inbox");
        Assert.assertNotNull("Inbox is listed", folder);

        folder = getSubfolder(folder, "foo");
        Assert.assertNotNull("foo is listed", folder);

        folder = getSubfolder(folder, "bar");
        Assert.assertNotNull("bar is listed", folder);

        folder = getSubfolder(folder, "baz");
        Assert.assertNotNull("baz is listed", folder);

        Assert.assertTrue("no more subfolders", folder.listElements(MailConstants.E_FOLDER).isEmpty());


        // next, test constraining to a single level of subfolders
        request.addAttribute(MailConstants.A_FOLDER_DEPTH, 1);
        response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        folder = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", folder);

        folder = getSubfolder(folder, "Inbox");
        Assert.assertNotNull("Inbox is listed", folder);

        folder = getSubfolder(folder, "foo");
        Assert.assertNull("foo is listed", folder);
    }

    @Test
    public void view() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        Folder.FolderOptions fopt = new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "foo", fopt);
        mbox.createFolder(null, "bar/baz", fopt);
        mbox.createFolder(null, "Inbox/woot", fopt);

        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_DEFAULT_VIEW, MailItem.Type.DOCUMENT.toString());
        Element response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        Element root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);
        Assert.assertTrue("top level folder is stubbed", isStubbed(root));

        Set<String> subfolderNames = ImmutableSet.of("Trash", "Briefcase", "foo", "bar", "Inbox", "Files shared with me");
        Set<String> stubbedSubfolderNames = ImmutableSet.of("bar", "Inbox");
        List<Element> subfolders = root.listElements(MailConstants.E_FOLDER);
        Assert.assertEquals("number of listed subfolders", subfolderNames.size(), subfolders.size());
        for (Element subfolder : subfolders) {
            String subfolderName = subfolder.getAttribute(MailConstants.A_NAME);
            Assert.assertTrue("", subfolderNames.contains(subfolderName));
            boolean expectStubbed = stubbedSubfolderNames.contains(subfolderName);
            Assert.assertNotNull(subfolderName + " folder is " + (expectStubbed ? "" : " not") + " stubbed", isStubbed(subfolder));
        }

        Element leaf = getSubfolder(getSubfolder(root, "bar"), "baz");
        Assert.assertNotNull("leaf node present", leaf);
        Assert.assertFalse("leaf not stubbed", isStubbed(leaf));
    }

    @Test
    public void mount() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

        Folder.FolderOptions fopt = new Folder.FolderOptions().setDefaultView(MailItem.Type.DOCUMENT);
        int folderId = mbox2.createFolder(null, "foo", fopt).getId();
        Folder folder = mbox2.getFolderById(null, folderId);
        mbox2.createFolder(null, "bar", folderId, fopt);
        mbox2.grantAccess(null, folderId, acct.getId(), ACL.GRANTEE_USER, (short) (ACL.RIGHT_READ | ACL.RIGHT_WRITE), null);

        Mountpoint mpt = mbox.createMountpoint(null, Mailbox.ID_FOLDER_USER_ROOT, "remote", acct2.getId(), folderId, folder.getUuid(), MailItem.Type.DOCUMENT, 0, (byte) 2, false);

        // fetch the mountpoint directly
        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, mpt.getId());
        Element response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        Element root = response.getOptionalElement(MailConstants.E_MOUNT);
        Assert.assertNotNull("top-level mountpoint is listed", root);
        Assert.assertFalse("top level mountpont is not stubbed", isStubbed(root));

        Element leaf = getSubfolder(root, "bar");
        Assert.assertNotNull("leaf node present", leaf);
        Assert.assertFalse("leaf not stubbed", isStubbed(leaf));


        // fetch the entire tree (does not recurse through mountpoint)
        request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        Element mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertTrue("mountpoint stubbed", isStubbed(mount));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNull("leaf node not present", leaf);


        // fetch the entire tree, traversing mountpoints
        request.addAttribute(MailConstants.A_TRAVERSE, true);
        response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertFalse("mountpoint not stubbed", isStubbed(mount));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNotNull("leaf node present", leaf);
        Assert.assertFalse("leaf node not stubbed", isStubbed(leaf));


        // fetch the tree to a depth of 1, traversing mountpoints
        request.addAttribute(MailConstants.A_TRAVERSE, true).addAttribute(MailConstants.A_FOLDER_DEPTH, 1);
        response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertFalse("mountpoint not stubbed", isStubbed(mount));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNull("leaf node not present", leaf);


        // broken link
        mbox2.delete(null, folderId, MailItem.Type.FOLDER);

        response = new GetFolder().handle(request, ServiceTestUtil.getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertTrue("mountpoint is stubbed", isStubbed(mount));
        Assert.assertTrue("mountpoint is broken", mount.getAttributeBool(MailConstants.A_BROKEN, false));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNull("leaf node absent", leaf);
    }

    private static final Set<String> FOLDER_TYPES = ImmutableSet.of(MailConstants.E_FOLDER, MailConstants.E_MOUNT, MailConstants.E_SEARCH);

    private static Element getSubfolder(Element parent, String foldername) {
        if (parent != null) {
            for (Element elt : parent.listElements()) {
                if (FOLDER_TYPES.contains(elt.getName()) && foldername.equals(elt.getAttribute(MailConstants.A_NAME, null))) {
                    return elt;
                }
            }
        }
        return null;
    }

    private static boolean isStubbed(Element folder) {
        return folder.getAttribute(MailConstants.A_SIZE, null) == null;
    }
}
