/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.util.Arrays;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

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

public class TestPurge{

    @Rule
    public TestName testInfo = new TestName();
    private static String USER_NAME = null;
    private static final String NAME_PREFIX = TestPurge.class.getSimpleName();
    private String originalSystemPolicy;

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";
        cleanUp();
        TestUtil.createAccount(USER_NAME);
        originalSystemPolicy = Provisioning.getInstance().getConfig().getMailPurgeSystemPolicy();
    }

    /**
     * Tests the SOAP API for setting retention policy on a folder.
     */
    @Test
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
        Assert.assertEquals("retentionpolicy", res.getAction().getOperation());
        Assert.assertEquals(folder.getId(), res.getAction().getId());

        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        Assert.assertEquals(1, rp.getKeepPolicy().size());
        Assert.assertEquals(0, rp.getPurgePolicy().size());
        Policy p = rp.getKeepPolicy().get(0);
        Assert.assertEquals(Policy.Type.USER, p.getType());
        Assert.assertEquals("30d", p.getLifetime());

        // Turn off keep policy and set purge policy.
        action = new FolderActionSelector(folder.getId(), "retentionpolicy");
        rp = new RetentionPolicy(null, Arrays.asList(Policy.newUserPolicy("45d")));
        action.setRetentionPolicy(rp);
        req = new FolderActionRequest(action);

        res = mbox.invokeJaxb(req);
        Assert.assertEquals("retentionpolicy", res.getAction().getOperation());
        Assert.assertEquals(folder.getId(), res.getAction().getId());

        // Make sure that the retention policy is now set.
        folder = mbox.getFolderById(folder.getId());
        rp = folder.getRetentionPolicy();
        Assert.assertEquals(0, rp.getKeepPolicy().size());
        Assert.assertEquals(1, rp.getPurgePolicy().size());
        p = rp.getPurgePolicy().get(0);
        Assert.assertEquals(Policy.Type.USER, p.getType());
        Assert.assertEquals("45d", p.getLifetime());

        // Start a new session and make sure that the retention policy is still returned.
        mbox = TestUtil.getZMailbox(USER_NAME);
        folder = mbox.getFolderById(folder.getId());
        Assert.assertTrue(folder.getRetentionPolicy().isSet());
    }
    @Test
    public void testTagRetentionPolicy() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZTag tag = mbox.createTag(NAME_PREFIX + "-testTagRetentionPolicy", null);

        // Set user keep policy for folder.
        TagActionSelector action = new TagActionSelector(tag.getId(), "retentionpolicy");
        RetentionPolicy rp = new RetentionPolicy(Arrays.asList(Policy.newUserPolicy("30d")), null);
        action.setRetentionPolicy(rp);
        TagActionRequest req = new TagActionRequest(action);

        TagActionResponse res = mbox.invokeJaxb(req);
        Assert.assertEquals("retentionpolicy", res.getAction().getOperation());
        Assert.assertEquals(tag.getId(), res.getAction().getSuccesses());

        // Make sure that the retention policy is now set.
        tag = mbox.getTagById(tag.getId());
        rp = tag.getRetentionPolicy();
        Assert.assertEquals(1, rp.getKeepPolicy().size());
        Assert.assertEquals(0, rp.getPurgePolicy().size());
        Policy p = rp.getKeepPolicy().get(0);
        Assert.assertEquals(Policy.Type.USER, p.getType());
        Assert.assertEquals("30d", p.getLifetime());

        // Turn off keep policy and set purge policy.
        action = new TagActionSelector(tag.getId(), "retentionpolicy");
        rp = new RetentionPolicy(null, Arrays.asList(Policy.newUserPolicy("45d")));
        action.setRetentionPolicy(rp);
        req = new TagActionRequest(action);

        res = mbox.invokeJaxb(req);
        Assert.assertEquals("retentionpolicy", res.getAction().getOperation());
        Assert.assertEquals(tag.getId(), res.getAction().getSuccesses());

        // Make sure that the retention policy is now set.
        tag = mbox.getTagById(tag.getId());
        rp = tag.getRetentionPolicy();
        Assert.assertEquals(0, rp.getKeepPolicy().size());
        Assert.assertEquals(1, rp.getPurgePolicy().size());
        p = rp.getPurgePolicy().get(0);
        Assert.assertEquals(Policy.Type.USER, p.getType());
        Assert.assertEquals("45d", p.getLifetime());

        // Start a new session and make sure that the retention policy is still returned.
        mbox = TestUtil.getZMailbox(USER_NAME);
        tag = mbox.getTagById(tag.getId());
        Assert.assertTrue(tag.getRetentionPolicy().isSet());
    }
    @Test
    public void testSystemRetentionPolicy() throws Exception {
        SoapProvisioning prov = TestUtil.newSoapProvisioning();

        // Test getting empty system policy.
        GetSystemRetentionPolicyRequest getReq = new GetSystemRetentionPolicyRequest();
        GetSystemRetentionPolicyResponse getRes = prov.invokeJaxb(getReq);
        RetentionPolicy rp = getRes.getRetentionPolicy();
        Assert.assertEquals(0, rp.getKeepPolicy().size());
        Assert.assertEquals(0, rp.getPurgePolicy().size());

        // Create keep policy.
        Policy keep = Policy.newSystemPolicy("keep", "60d");
        CreateSystemRetentionPolicyRequest createReq = CreateSystemRetentionPolicyRequest.newKeepRequest(keep);
        CreateSystemRetentionPolicyResponse createRes = prov.invokeJaxb(createReq);
        Policy p = createRes.getPolicy();
        Assert.assertNotNull(p.getId());
        Assert.assertEquals(keep.getName(), p.getName());
        Assert.assertEquals(keep.getLifetime(), p.getLifetime());
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
        Assert.assertEquals(1, rp.getKeepPolicy().size());
        Assert.assertEquals(keep, rp.getKeepPolicy().get(0));
        Assert.assertEquals(2, rp.getPurgePolicy().size());
        Assert.assertEquals(purge1, rp.getPolicyById(purge1.getId()));
        Assert.assertEquals(purge2, rp.getPolicyById(purge2.getId()));

        // Get system policy with the mail API.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        com.zimbra.soap.mail.message.GetSystemRetentionPolicyResponse mailRes =
            mbox.invokeJaxb(new com.zimbra.soap.mail.message.GetSystemRetentionPolicyRequest());
        Assert.assertEquals(rp, mailRes.getRetentionPolicy());

        // Modify lifetime.
        Policy modLifetime = Policy.newSystemPolicy(purge1.getId(), null, "121d");
        ModifySystemRetentionPolicyRequest modifyReq = new ModifySystemRetentionPolicyRequest(modLifetime);
        ModifySystemRetentionPolicyResponse modifyRes = prov.invokeJaxb(modifyReq);
        Policy newPurge1 = modifyRes.getPolicy();
        Assert.assertEquals(Policy.newSystemPolicy(purge1.getId(), "purge1", "121d"), newPurge1);

        getRes = prov.invokeJaxb(getReq);
        Assert.assertEquals(newPurge1, getRes.getRetentionPolicy().getPolicyById(newPurge1.getId()));

        // Modify name.
        Policy modName = Policy.newSystemPolicy(purge1.getId(), "purge1-new", null);
        modifyReq = new ModifySystemRetentionPolicyRequest(modName);
        modifyRes = prov.invokeJaxb(modifyReq);
        newPurge1 = modifyRes.getPolicy();
        Assert.assertEquals(Policy.newSystemPolicy(purge1.getId(), "purge1-new", "121d"), newPurge1);

        // Delete.
        DeleteSystemRetentionPolicyRequest deleteReq = new DeleteSystemRetentionPolicyRequest(purge1);
        @SuppressWarnings("unused")
        DeleteSystemRetentionPolicyResponse deleteRes = prov.invokeJaxb(deleteReq);
        getRes = prov.invokeJaxb(getReq);
        rp = getRes.getRetentionPolicy();
        Assert.assertEquals(1, rp.getKeepPolicy().size());
        Assert.assertEquals(keep, rp.getKeepPolicy().get(0));
        Assert.assertEquals(1, rp.getPurgePolicy().size());
        Assert.assertEquals(purge2, rp.getPurgePolicy().get(0));
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
        Provisioning.getInstance().getConfig().setMailPurgeSystemPolicy(originalSystemPolicy);
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestPurge.class);
    }
}
