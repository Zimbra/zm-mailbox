package com.zimbra.cs.imap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.mailbox.ZimbraTag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.session.ModificationItem;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.session.PendingRemoteModifications;
import com.zimbra.soap.account.message.ImapMessageInfo;

public class PendingRemoteNotificationsTest {

    @Test
    public void testPendingRemoteNotifications() throws Exception {
        String acctId = "12aa345b-2b47-44e6-8cb8-7fdfa18c1a9f";
        ImapMessageInfo imapMsg1 = new ImapMessageInfo(123, 123, Type.MESSAGE.toString(), 0, null);
        BaseItemInfo msg1 = ModificationItem.itemUpdate(imapMsg1, 2, acctId);
        ImapMessageInfo imapMsg2 = new ImapMessageInfo(456, 456, Type.MESSAGE.toString(), 0, null);
        BaseItemInfo msg2 = ModificationItem.itemUpdate(imapMsg2, 2, acctId);
        PendingRemoteModifications prm = new PendingRemoteModifications();
        prm.recordCreated(msg1);
        assertTrue(!prm.created.isEmpty());
        BaseItemInfo newItem = prm.created.get(new ModificationKey(msg1));
        assertEquals(imapMsg1.getId(), (Integer) newItem.getIdInMailbox());
        assertEquals(imapMsg1.getImapUid(), (Integer) newItem.getImapUid());
        assertEquals(acctId, newItem.getAccountId());

        //rename a tag
        ZimbraTag tag = ModificationItem.tagRename(2, "tagname");
        prm.recordModified(tag, acctId, Change.NAME);
        Change tagChange = prm.modified.get(new ModificationKey(acctId, tag.getTagId()));
        assertNotNull(tagChange);
        assertEquals(Change.NAME, tagChange.why);
        assertEquals(tag, tagChange.what);

        //rename a folder
        ModificationItem folder = ModificationItem.folderRename(3, "/newpath", acctId);
        prm.recordModified(folder, Change.NAME);
        Change folderChange = prm.modified.get(new ModificationKey(folder));
        assertNotNull(folderChange);
        assertEquals(Change.NAME, folderChange.why);
        assertEquals(folder, folderChange.what);

        //modify an item
        BaseItemInfo updateItem = ModificationItem.itemUpdate(imapMsg2, 2, acctId);
        prm.recordModified(updateItem, Change.FLAGS);
        Change itemChange = prm.modified.get(new ModificationKey(updateItem));
        assertNotNull(itemChange);
        assertEquals(Change.FLAGS, itemChange.why);
        assertEquals(updateItem, itemChange.what);

        //adding a delete notification for the previously added message
        //should remove it from the created map, and NOT add it to the deleted map
        prm.recordDeleted(Type.MESSAGE, acctId, imapMsg1.getId());
        assertTrue(prm.created.isEmpty());
        assertTrue(prm.deleted == null);

        //adding a delete notification for a previously modified message
        //should remove it from the modified map AND add it to the deleted map
        prm.recordDeleted(Type.MESSAGE, acctId, imapMsg2.getId());
        assertNull(prm.modified.get(new ModificationKey(msg2)));
        assertEquals(1, prm.deleted.size());
        Change deletionChange = prm.deleted.get(new ModificationKey(msg2));
        assertEquals(Change.NONE, deletionChange.why);
        assertEquals(MailItem.Type.MESSAGE, deletionChange.what);
    }

}
