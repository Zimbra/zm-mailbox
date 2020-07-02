package com.zimbra.cs.mailbox;

import static org.junit.Assert.*;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.qa.unittest.TestUtil;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class SmartFolderTest {

    private static final String USER = "SmartFolderTest";
    private Mailbox mbox;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount(USER, "test123", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testCreateSmartFolder() throws Exception {
        String smartFolderName = "finance";
        SmartFolder smartFolder = mbox.createSmartFolder(null, smartFolderName);
        assertEquals("wrong name", SmartFolder.getInternalTagName(smartFolderName), smartFolder.getName());
        assertEquals("wrong external name", smartFolderName, smartFolder.getSmartFolderName());

        SmartFolder sf = mbox.getSmartFolder(null, smartFolderName);
        assertEquals("wrong SmartFolder ID", smartFolder.getId(), sf.getId());
        assertEquals("wrong SmartFolder name", smartFolder.getSmartFolderName(), sf.getSmartFolderName());

        List<SmartFolder> smartFolders = mbox.getSmartFolders(null);
        assertEquals("should see 1 smart folder", 1, smartFolders.size());
        assertEquals("wrong ID", smartFolder.getId(), smartFolders.get(0).getId());
        assertEquals("wrong external name", smartFolder.getSmartFolderName(), smartFolders.get(0).getSmartFolderName());
        mbox.delete(null, smartFolder.getId(), MailItem.Type.SMARTFOLDER);
        try {
            mbox.getSmartFolder(null, smartFolderName);
            fail("should not be able to get deleted smartfolder");
        } catch (NoSuchItemException e) {}
    }

    @Test
    public void testTagWithSmartFolder() throws Exception {
        Message msg = TestUtil.addMessage(mbox, MailboxTestUtil.generateMessage("smartfolder test"));
        SmartFolder smartFolder = mbox.createSmartFolder(null, "finance");
        mbox.alterTag(null, msg.getId(), Type.MESSAGE, smartFolder.getName(), true, null);
        assertEquals("message should not have any tags", 0, msg.getTags().length);
        String[] smartFolders = msg.getSmartFolders();
        assertEquals("message should have one tag", 1, smartFolders.length);
        assertEquals("wrong internal tag name", SmartFolder.getInternalTagName("finance"), smartFolders[0]);
    }

    @Test
    public void testNameCollision() throws ServiceException {
        SmartFolder smartFolder = mbox.createSmartFolder(null, "finance");
        //should be able to create a tag with the same name without a collision
        Tag tag = mbox.createTag(null, "finance", (byte)0);
        List<Tag> tags = mbox.getTagList(null);
        assertEquals("should see 1 tag", 1, tags.size());
        assertEquals("wrong tag ID", tag.getId(), tags.get(0).getId());
        List<SmartFolder> smartFolders = mbox.getSmartFolders(null);
        assertEquals("should see 1 smart folder", 1, smartFolders.size());
        assertEquals("wrong ID", smartFolder.getId(), smartFolders.get(0).getId());
    }

    private Map<String, SmartFolder> getSmartFolders() throws Exception {
        Map<String, SmartFolder> map = new HashMap<>();
        for (SmartFolder sf: mbox.getSmartFolders(null)) {
            map.put(sf.getSmartFolderName(), sf);
        }
        return map;
    }

    private SmartFolderProvider getProvider(String... names) {
        return new SmartFolderProvider() {

            @Override
            public Set<String> getSmartFolderNames() throws ServiceException {
                return Sets.newHashSet(names);
            }
        };
    }

    private void syncSmartFolders(String... smartFolderNames) throws Exception {
        mbox.syncSmartFolders(null, getProvider(smartFolderNames));
        Map<String, SmartFolder> smartFolders = getSmartFolders();
        assertEquals(String.format("should see %d smart folders", smartFolderNames.length), smartFolderNames.length, smartFolders.size());
        for (String name: smartFolderNames) {
            assertTrue(String.format("%s should exist", name), smartFolders.containsKey(name));
        }
    }

    @Test
    public void testSyncSmartFolders() throws Exception {
        syncSmartFolders("folder1");
        syncSmartFolders("folder1", "folder2");
        syncSmartFolders("folder2");
        syncSmartFolders("folder3");
    }
}
