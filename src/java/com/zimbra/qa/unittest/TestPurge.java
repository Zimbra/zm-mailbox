/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.util.Arrays;

import junit.framework.TestCase;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZTag;
import com.zimbra.soap.admin.message.CreateSystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.CreateSystemRetentionPolicyResponse;
import com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.DeleteSystemRetentionPolicyResponse;
import com.zimbra.soap.admin.message.GetSystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.GetSystemRetentionPolicyResponse;
import com.zimbra.soap.admin.message.ModifySystemRetentionPolicyRequest;
import com.zimbra.soap.admin.message.ModifySystemRetentionPolicyResponse;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.FolderActionResponse;
import com.zimbra.soap.mail.message.TagActionRequest;
import com.zimbra.soap.mail.message.TagActionResponse;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;
import com.zimbra.soap.mail.type.TagActionSelector;

public class TestPurge extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String NAME_PREFIX = TestPurge.class.getSimpleName();

    private String originalSystemPolicy;

    @Override
    public void setUp() throws Exception {
        cleanUp();
        originalSystemPolicy = Provisioning.getInstance().getConfig().getMailPurgeSystemPolicy();
    }

    /**
     * Tests the SOAP API for setting retention policy on a folder.
     */
    public void testFolderRetentionPolicy()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(mbox, "/" + NAME_PREFIX + "-testFolderRetentionPolicy");

        // Set user keep policy for folder.
        FolderActionSelector action = new FolderActionSelector(folder.getId(), "retentionpolicy");
        RetentionPolicy rp = new RetentionPolicy(Arrays.asList(Policy.newUserPolicy("30d")), null);
        action.setRetentionPolicy(rp);
        FolderActionRequest req = new FolderActionRequest(action);

        FolderActionResponse res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(folder.getId(), res.getAction().getId());

        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        assertEquals(1, rp.getKeepPolicy().size());
        assertEquals(0, rp.getPurgePolicy().size());
        Policy p = rp.getKeepPolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("30d", p.getLifetime());

        // Turn off keep policy and set purge policy.
        action = new FolderActionSelector(folder.getId(), "retentionpolicy");
        rp = new RetentionPolicy(null, Arrays.asList(Policy.newUserPolicy("45d")));
        action.setRetentionPolicy(rp);
        req = new FolderActionRequest(action);

        res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(folder.getId(), res.getAction().getId());

        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        assertEquals(0, rp.getKeepPolicy().size());
        assertEquals(1, rp.getPurgePolicy().size());
        p = rp.getPurgePolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("45d", p.getLifetime());

        // Start a new session and make sure that the retention policy is still returned.
        mbox = TestUtil.getZMailbox(USER_NAME);
        folder = mbox.getFolderById(folder.getId());
        assertTrue(folder.getRetentionPolicy().isSet());
    }

    public void testTagRetentionPolicy() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZTag tag = mbox.createTag(NAME_PREFIX + "-testTagRetentionPolicy", null);

        // Set user keep policy for folder.
        TagActionSelector action = new TagActionSelector(tag.getId(), "retentionpolicy");
        RetentionPolicy rp = new RetentionPolicy(Arrays.asList(Policy.newUserPolicy("30d")), null);
        action.setRetentionPolicy(rp);
        TagActionRequest req = new TagActionRequest(action);

        TagActionResponse res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(tag.getId(), res.getAction().getSuccesses());

        // Make sure that the retention policy is now set.
        tag = mbox.getTagById(tag.getId());
        rp = tag.getRetentionPolicy();
        assertEquals(1, rp.getKeepPolicy().size());
        assertEquals(0, rp.getPurgePolicy().size());
        Policy p = rp.getKeepPolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("30d", p.getLifetime());

        // Turn off keep policy and set purge policy.
        action = new TagActionSelector(tag.getId(), "retentionpolicy");
        rp = new RetentionPolicy(null, Arrays.asList(Policy.newUserPolicy("45d")));
        action.setRetentionPolicy(rp);
        req = new TagActionRequest(action);

        res = mbox.invokeJaxb(req);
        assertEquals("retentionpolicy", res.getAction().getOperation());
        assertEquals(tag.getId(), res.getAction().getSuccesses());

        // Make sure that the retention policy is now set.
        tag = mbox.getTagById(tag.getId());
        rp = tag.getRetentionPolicy();
        assertEquals(0, rp.getKeepPolicy().size());
        assertEquals(1, rp.getPurgePolicy().size());
        p = rp.getPurgePolicy().get(0);
        assertEquals(Policy.Type.USER, p.getType());
        assertEquals("45d", p.getLifetime());

        // Start a new session and make sure that the retention policy is still returned.
        mbox = TestUtil.getZMailbox(USER_NAME);
        tag = mbox.getTagById(tag.getId());
        assertTrue(tag.getRetentionPolicy().isSet());
    }

    public void testSystemRetentionPolicy() throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();

        // Test getting empty system policy.
        GetSystemRetentionPolicyRequest getReq = new GetSystemRetentionPolicyRequest();
        GetSystemRetentionPolicyResponse getRes = prov.invokeJaxb(getReq);
        RetentionPolicy rp = getRes.getRetentionPolicy();
        assertEquals(0, rp.getKeepPolicy().size());
        assertEquals(0, rp.getPurgePolicy().size());

        // Create keep policy.
        Policy keep = Policy.newSystemPolicy("keep", "60d");
        CreateSystemRetentionPolicyRequest createReq = CreateSystemRetentionPolicyRequest.newKeepRequest(keep);
        CreateSystemRetentionPolicyResponse createRes = prov.invokeJaxb(createReq);
        Policy p = createRes.getPolicy();
        assertNotNull(p.getId());
        assertEquals(keep.getName(), p.getName());
        assertEquals(keep.getLifetime(), p.getLifetime());
        keep = p;

        // Create purge policy.
        Policy purge1 = Policy.newSystemPolicy("purge1", "120d");
        createReq = CreateSystemRetentionPolicyRequest.newPurgeRequest(purge1);
        createRes = prov.invokeJaxb(createReq);
        purge1 = createRes.getPolicy();

        Policy purge2 = Policy.newSystemPolicy("purge2", "240d");
        createReq = CreateSystemRetentionPolicyRequest.newPurgeRequest(purge2);
        createRes = prov.invokeJaxb(createReq);
        purge2 = createRes.getPolicy();

        // Test getting updated system policy.
        getRes = prov.invokeJaxb(getReq);
        rp = getRes.getRetentionPolicy();
        assertEquals(1, rp.getKeepPolicy().size());
        assertEquals(keep, rp.getKeepPolicy().get(0));
        assertEquals(2, rp.getPurgePolicy().size());
        assertEquals(purge1, rp.getPolicyById(purge1.getId()));
        assertEquals(purge2, rp.getPolicyById(purge2.getId()));

        // Get system policy with the mail API.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        com.zimbra.soap.mail.message.GetSystemRetentionPolicyResponse mailRes =
            mbox.invokeJaxb(new com.zimbra.soap.mail.message.GetSystemRetentionPolicyRequest());
        assertEquals(rp, mailRes.getRetentionPolicy());

        // Modify lifetime.
        Policy modLifetime = Policy.newSystemPolicy(purge1.getId(), null, "121d");
        ModifySystemRetentionPolicyRequest modifyReq = new ModifySystemRetentionPolicyRequest(modLifetime);
        ModifySystemRetentionPolicyResponse modifyRes = prov.invokeJaxb(modifyReq);
        Policy newPurge1 = modifyRes.getPolicy();
        assertEquals(Policy.newSystemPolicy(purge1.getId(), "purge1", "121d"), newPurge1);

        getRes = prov.invokeJaxb(getReq);
        assertEquals(newPurge1, getRes.getRetentionPolicy().getPolicyById(newPurge1.getId()));

        // Modify name.
        Policy modName = Policy.newSystemPolicy(purge1.getId(), "purge1-new", null);
        modifyReq = new ModifySystemRetentionPolicyRequest(modName);
        modifyRes = prov.invokeJaxb(modifyReq);
        newPurge1 = modifyRes.getPolicy();
        assertEquals(Policy.newSystemPolicy(purge1.getId(), "purge1-new", "121d"), newPurge1);

        // Delete.
        DeleteSystemRetentionPolicyRequest deleteReq = new DeleteSystemRetentionPolicyRequest(purge1);
        @SuppressWarnings("unused")
        DeleteSystemRetentionPolicyResponse deleteRes = prov.invokeJaxb(deleteReq);
        getRes = prov.invokeJaxb(getReq);
        rp = getRes.getRetentionPolicy();
        assertEquals(1, rp.getKeepPolicy().size());
        assertEquals(keep, rp.getKeepPolicy().get(0));
        assertEquals(1, rp.getPurgePolicy().size());
        assertEquals(purge2, rp.getPurgePolicy().get(0));
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
        Provisioning.getInstance().getConfig().setMailPurgeSystemPolicy(originalSystemPolicy);
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestPurge.class);
    }
}
