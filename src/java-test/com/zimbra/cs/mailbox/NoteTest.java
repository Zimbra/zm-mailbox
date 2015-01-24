package com.zimbra.cs.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

public class NoteTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testConstructDocumentFromData() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Note note = mbox.createNote(null, "this is a note", null, Note.DEFAULT_COLOR, Mailbox.ID_FOLDER_COMMENTS);

        UnderlyingData ud = DbMailItem.getById(mbox.getId(), mbox.getSchemaGroupId(), note.getId(), note.getType(), note.inDumpster(), DbPool.getConnection(mbox.getId(), mbox.getSchemaGroupId()));
        assertNotNull("Underlying data is null", ud);
        assertEquals("underlying data has wrong type", MailItem.Type.NOTE,MailItem.Type.of(ud.type));
        assertEquals("underlying data has wrong folder ID", Mailbox.ID_FOLDER_COMMENTS,ud.folderId);
        assertEquals("underlying data has wrong UUID", note.getUuid(),ud.uuid);

        MailItem testItem = MailItem.constructItem(Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID),ud,mbox.getId());
        assertNotNull("reconstructed mail item is null", testItem);
        assertEquals("reconstructed doc has wrong item type", MailItem.Type.NOTE,testItem.getType());
        assertEquals("reconstructed doc has wrong UUID", note.getUuid(), testItem.getUuid());
        assertEquals("reconstructed doc has wrong ID", note.getId(), testItem.getId());
        assertEquals("reconstructed doc has wrong folder", note.getFolderId(), testItem.getFolderId());
        assertEquals("reonstructed doc has wrong content", Arrays.toString(note.getContent()),Arrays.toString(testItem.getContent()));
        List<IndexDocument> docs = testItem.generateIndexDataAsync(false);
        Assert.assertEquals(1, docs.size());
    }

    @Test
    public void testGenerateIndexData() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        Note note = mbox.createNote(null, "this is a note", null, Note.DEFAULT_COLOR, Mailbox.ID_FOLDER_COMMENTS);

        List<IndexDocument> docs = note.generateIndexData();
        Assert.assertEquals(1, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String filename = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_FILENAME);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("note content is null", body);
        Assert.assertEquals("this is a note", subject);
        Assert.assertNull("note's filename should be null", filename);
        Assert.assertEquals("this is a note", body);
    }

    @Test
    public void testGenerateIndexDataAsync() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        Note note = mbox.createNote(null, "this is a note", null, Note.DEFAULT_COLOR, Mailbox.ID_FOLDER_COMMENTS);

        List<IndexDocument> docs = note.generateIndexDataAsync(false);
        Assert.assertEquals(1, docs.size());
        String subject = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String filename = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_FILENAME);
        String body = (String) docs.get(0).toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("note content is null", body);
        Assert.assertEquals("this is a note", subject);
        Assert.assertNull("note's filename should be null", filename);
        Assert.assertEquals("this is a note", body);
    }
}
