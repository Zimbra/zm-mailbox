package com.zimbra.cs.mailbox;

import static org.junit.Assert.assertEquals;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.calendar.IcsImportParseHandler.ImportInviteVisitor;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Recurrence;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class RecurringTaskTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testTaskRecurrenceDuration() throws ServiceException, UnsupportedEncodingException {
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);

        String task = "BEGIN:VCALENDAR\n"
                    + "PRODID:Zimbra-Calendar-Provider\n"
                    + "VERSION:2.0\n"
                    + "METHOD:PUBLISH\n"
                    + "BEGIN:VTIMEZONE\n"
                    + "TZID:Africa/Harare\n"
                    + "BEGIN:STANDARD\n"
                    + "DTSTART:16010101T000000\n"
                    + "TZOFFSETTO:+0200\n"
                    + "TZOFFSETFROM:+0200\n"
                    + "TZNAME:CAT\n"
                    + "END:STANDARD\n"
                    + "END:VTIMEZONE\n"
                    + "BEGIN:VTODO\n"
                    + "UID:93077c29_ab51_41ad_aaa8_f63a68f963a6_migwiz\n"
                    + "RRULE:FREQ=MONTHLY;INTERVAL=1;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=5\n"
                    + "SUMMARY:Test Recurring Task\n"
                    + "ATTENDEE;CN=User Test;CUTYPE=RESOURCE;ROLE=NON-PARTICIPANT;PARTSTAT=NEEDS-ACTION:mailto:testuser@zimbra.com\n"
                    + "ATTENDEE;CN=User Test;ROLE=OPT-PARTICIPANT;PARTSTAT=NEEDS-ACTION:mailto:testuser@zimbra.com\n"
                    + "PRIORITY:5\n"
                    + "PERCENT-COMPLETE:0\n"
                    + "ORGANIZER:mailto:admin@zimbra.com\n"
                    + "DTSTART;VALUE=DATE:20120831\n"
                    + "DUE;VALUE=DATE:20440709\n"
                    + "STATUS:NEEDS-ACTION\n"
                    + "CLASS:PUBLIC\n"
                    + "LAST-MODIFIED:20131125T135454Z\n"
                    + "DTSTAMP:20131125T135454Z\n"
                    + "SEQUENCE:0\n"
                    + "END:VTODO\n"
                    + "END:VCALENDAR";

        OperationContext octxt = new OperationContext(acct);
        Folder taskFolder = mbox.getFolderById(octxt, 15);
        String charset = MimeConstants.P_CHARSET_UTF8;
        InputStream is = new ByteArrayInputStream(task.getBytes(charset));
        List<ZVCalendar> icals = ZCalendarBuilder.buildMulti(is, charset);
        ImportInviteVisitor visitor = new ImportInviteVisitor(octxt, taskFolder, false);
        List<Invite> invites = Invite.createFromCalendar(acct, null, icals, true, false, visitor);
        Recurrence.IRecurrence recur =  invites.get(0).getRecurrence();
        Metadata meta = recur.encodeMetadata();
        assertEquals("P1D", meta.get("duration"));
    }
}
