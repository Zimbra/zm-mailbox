/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.net.URL;
import java.util.HashMap;
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
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.MockHttpServletRequest;
import com.zimbra.soap.MockSoapEngine;
import com.zimbra.soap.SoapEngine;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

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

    public static Map<String, Object> getRequestContext(Account acct) throws Exception {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put(SoapEngine.ZIMBRA_CONTEXT, new ZimbraSoapContext(AuthProvider.getAuthToken(acct), acct.getId(), SoapProtocol.Soap12, SoapProtocol.Soap12));
        context.put(SoapServlet.SERVLET_REQUEST, new MockHttpServletRequest("test".getBytes("UTF-8"), new URL("http://localhost:7070/service/FooRequest"), ""));
        context.put(SoapEngine.ZIMBRA_ENGINE, new MockSoapEngine(new MailService()));
        return context;
    }

    @Test
    public void depth() throws Exception {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        mbox.createFolder(null, "Inbox/foo/bar/baz", (byte) 0, MailItem.Type.DOCUMENT);

        // first, test the default setup (full tree)
        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        Element response = new GetFolder().handle(request, getRequestContext(acct));

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
        response = new GetFolder().handle(request, getRequestContext(acct));

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

        mbox.createFolder(null, "foo", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "bar/baz", (byte) 0, MailItem.Type.DOCUMENT);
        mbox.createFolder(null, "Inbox/woot", (byte) 0, MailItem.Type.DOCUMENT);

        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST).addAttribute(MailConstants.A_DEFAULT_VIEW, MailItem.Type.DOCUMENT.toString());
        Element response = new GetFolder().handle(request, getRequestContext(acct));

        Element root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);
        Assert.assertTrue("top level folder is stubbed", isStubbed(root));

        Set<String> subfolderNames = ImmutableSet.of("Trash", "Briefcase", "foo", "bar", "Inbox");
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

        int folderId = mbox2.createFolder(null, "foo", (byte) 0, MailItem.Type.DOCUMENT).getId();
        mbox2.createFolder(null, "bar", folderId, MailItem.Type.DOCUMENT, 0, (byte) 0, null);
        mbox2.grantAccess(null, folderId, acct.getId(), ACL.GRANTEE_USER, (short) (ACL.RIGHT_READ | ACL.RIGHT_WRITE), null);

        Mountpoint mpt = mbox.createMountpoint(null, Mailbox.ID_FOLDER_USER_ROOT, "remote", acct2.getId(), folderId, MailItem.Type.DOCUMENT, 0, (byte) 2, false);

        // fetch the mountpoint directly
        Element request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        request.addElement(MailConstants.E_FOLDER).addAttribute(MailConstants.A_FOLDER, mpt.getId());
        Element response = new GetFolder().handle(request, getRequestContext(acct));

        Element root = response.getOptionalElement(MailConstants.E_MOUNT);
        Assert.assertNotNull("top-level mountpoint is listed", root);
        Assert.assertFalse("top level mountpont is not stubbed", isStubbed(root));

        Element leaf = getSubfolder(root, "bar");
        Assert.assertNotNull("leaf node present", leaf);
        Assert.assertFalse("leaf not stubbed", isStubbed(leaf));


        // fetch the entire tree (does not recurse through mountpoint)
        request = new Element.XMLElement(MailConstants.GET_FOLDER_REQUEST);
        response = new GetFolder().handle(request, getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        Element mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertTrue("mountpoint stubbed", isStubbed(mount));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNull("leaf node not present", leaf);


        // fetch the entire tree, traversing mountpoints
        request.addAttribute(MailConstants.A_TRAVERSE, true);
        response = new GetFolder().handle(request, getRequestContext(acct));

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
        response = new GetFolder().handle(request, getRequestContext(acct));

        root = response.getOptionalElement(MailConstants.E_FOLDER);
        Assert.assertNotNull("top-level folder is listed", root);

        mount = getSubfolder(root, "remote");
        Assert.assertNotNull("mountpoint present", mount);
        Assert.assertFalse("mountpoint not stubbed", isStubbed(mount));

        leaf = getSubfolder(mount, "bar");
        Assert.assertNull("leaf node not present", leaf);


        // broken link
        mbox2.delete(null, folderId, MailItem.Type.FOLDER);

        response = new GetFolder().handle(request, getRequestContext(acct));

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
