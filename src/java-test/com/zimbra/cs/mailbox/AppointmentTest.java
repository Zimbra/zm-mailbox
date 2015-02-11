package com.zimbra.cs.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mailbox.calendar.Invite;

public class AppointmentTest {

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

    public AddInviteData createCalendarItem(String subject, String fragment, String location) throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        ZVCalendar calendar = new ZVCalendar();
        calendar.addDescription("my new calendar",null);
        ZComponent comp = new ZComponent("VEVENT");
        calendar.addComponent(comp);

        Invite invite = MailboxTestUtil.generateInvite(mbox.getAccount(), fragment, calendar);
        invite.setUid(new UUID(10L, 1L).toString());
        invite.setSentByMe(true);
        invite.setName(subject);
        invite.setDescription(fragment, fragment);
        invite.setLocation(location);
        AddInviteData inviteData = mbox.addInvite(null, invite, Mailbox.ID_FOLDER_CALENDAR);

        return inviteData;
    }

    @Test
    public void testConstructFromData() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        AddInviteData inviteData = createCalendarItem("this is an invitation", "to meet", "in San Mateo");

        UnderlyingData ud = DbMailItem.getById(mbox.getId(), mbox.getSchemaGroupId(), inviteData.calItemId, MailItem.Type.APPOINTMENT, false, DbPool.getConnection(mbox.getId(), mbox.getSchemaGroupId()));
        assertNotNull("Underlying data is null", ud);
        assertEquals("underlying data has wrong type", MailItem.Type.APPOINTMENT,MailItem.Type.of(ud.type));
        assertEquals("underlying data has wrong folder ID", Mailbox.ID_FOLDER_CALENDAR,ud.folderId);

        MailItem testItem = MailItem.constructItem(Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID),ud,mbox.getId());
        assertNotNull("reconstructed item is null", testItem);
        assertEquals("reconstructed item has wrong item type", MailItem.Type.APPOINTMENT,testItem.getType());
        assertNotNull("reconstructed appointment has null subject", testItem.getSubject());
        assertEquals("reconstructed appointmetn has wrong subject", "this is an invitation", testItem.getSubject());
        assertTrue("reconstructed mail item is not an instance of Appointment class", testItem instanceof Appointment);
        assertNotNull("reconstructed appointment does not have an invite", ((Appointment)testItem).getInvite(0));
        assertEquals("reconstructed appointment has wrong description", "to meet", ((Appointment)testItem).getInvite(0).getDescription());
        assertEquals("reconstructed appointment has wrong location",  "in San Mateo", ((Appointment)testItem).getInvite(0).getLocation());
        List<IndexDocument> docs = testItem.generateIndexDataAsync(true);
        Assert.assertEquals(1, docs.size());

        IndexDocument doc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = doc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) doc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) doc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("appointment content is null", body);
        Assert.assertEquals("this is an invitation", subject);
        Assert.assertEquals("this is an invitation  in San Mateo to meet ", body);
        Assert.assertEquals("_calendaritemclass:public", doc.toDocument().getFieldValue(LuceneFields.L_FIELD));
        Assert.assertEquals("index document has wrong l.partname", "top", doc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
    }

    @Test
    public void testGenerateIndexData() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        AddInviteData inviteData = createCalendarItem("come over tomorrow", "to hangout", "in Palo Alto");
        CalendarItem calItem = mbox.getCalendarItemById(null, inviteData.calItemId);

        List<IndexDocument> docs = calItem.generateIndexData();
        Assert.assertEquals(1, docs.size());
        IndexDocument doc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = doc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) doc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) doc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("appointment content is null", body);
        Assert.assertEquals("come over tomorrow", subject);
        Assert.assertEquals("come over tomorrow  in Palo Alto to hangout ", body);
        Assert.assertEquals("_calendaritemclass:public", doc.toDocument().getFieldValue(LuceneFields.L_FIELD));
        Assert.assertEquals("index document has wrong l.partname", "top", doc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
    }

    @Test
    public void testGenerateIndexDataAsync() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        account.setPrefMailDefaultCharset("ISO-2022-JP");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

        AddInviteData inviteData = createCalendarItem("come over tomorrow", "to hangout", "in Palo Alto");
        CalendarItem calItem = mbox.getCalendarItemById(null, inviteData.calItemId);

        List<IndexDocument> docs = calItem.generateIndexDataAsync(false);
        Assert.assertEquals(1, docs.size());
        IndexDocument doc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        Collection<String> docFields = doc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        String subject = (String) doc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        String body = (String) doc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("appointment content is null", body);
        Assert.assertEquals("come over tomorrow", subject);
        Assert.assertEquals("come over tomorrow  in Palo Alto to hangout ", body);
        Assert.assertEquals("_calendaritemclass:public", doc.toDocument().getFieldValue(LuceneFields.L_FIELD));
        Assert.assertEquals("index document has wrong l.partname", "top", doc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));

        docs = calItem.generateIndexDataAsync(true);
        Assert.assertEquals(1, docs.size());
        doc = docs.get(0);
        assertNotNull("generated IndexDocument is null", doc);
        docFields = doc.toDocument().getFieldNames();
        assertNotNull("generated IndexDocument has NULL fields", docFields);
        assertFalse("generated IndexDocument has no fields", docFields.isEmpty());
        subject = (String) doc.toDocument().getFieldValue(LuceneFields.L_H_SUBJECT);
        body = (String) doc.toDocument().getFieldValue(LuceneFields.L_CONTENT);
        assertNotNull("appointment content is null", body);
        Assert.assertEquals("come over tomorrow", subject);
        Assert.assertEquals("come over tomorrow  in Palo Alto to hangout ", body);
        Assert.assertEquals("_calendaritemclass:public", doc.toDocument().getFieldValue(LuceneFields.L_FIELD));
        Assert.assertEquals("index document has wrong l.partname", "top", doc.toDocument().getFieldValue(LuceneFields.L_PARTNAME));
    }

}
