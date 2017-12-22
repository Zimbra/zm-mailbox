package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.Recurrence.RecurrenceRule;
import com.zimbra.cs.mailbox.calendar.Recurrence.SimpleRepeatingRule;

public class AllDayAcceptInstanceTest {

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            System.out.println(method.getName() + " " + e.getClass().getSimpleName());
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, "Asia/Kolkata");
        prov.createAccount("Organizer1_ZCS2655@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraPrefTimeZoneId, "America/Panama");
        prov.createAccount("Organizer2_ZCS2655@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("Attendee_ZCS2655@zimbra.com", "secret", attrs);

        String tzFilePath = LC.timezone_file.value();
        File tzFile = new File(tzFilePath);
        WellKnownTimeZones.loadFromFile(tzFile);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {
                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {
                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm,
                                    Collection<RollbackData> rollbacks) {
                                try {
                                    return Arrays.asList(getRecipients(mm));
                                } catch (Exception e) {
                                    return Collections.emptyList();
                                }
                            }
                        };
                    }
                };
            }
        });
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.clearData();
    }

    /*
     * Test with organizer timezone GMT+
     * In this case GMT+5:30 Asia/Kolkata
     */
    @Test
    public void testZCS2655PositiveTimeZone() throws Exception {
        ScheduledTaskManager.startup();
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "Organizer1_ZCS2655@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "Attendee_ZCS2655@zimbra.com");
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

        String desc = "The following is a new meeting request";
        Folder calendarFolder = mbox1.getCalendarFolders(null, SortBy.NONE).get(0);
        String fragment = "Some message";
        ZVCalendar calendar = new ZVCalendar();
        calendar.addDescription(desc, null);
        ZComponent comp = new ZComponent("VEVENT");
        calendar.addComponent(comp);

        Invite invite = MailboxTestUtil.generateInvite(acct1, fragment, calendar);
        ICalTimeZone pacific = new ICalTimeZone("America/Los_Angeles", -28800000, "16010101T020000",
            "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=11;BYDAY=1SU", "PST", -25200000,
            "16010101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=3;BYDAY=2SU", "PDT");
        TimeZoneMap tzmap = new TimeZoneMap(pacific);
        ParsedDateTime s = ParsedDateTime.parse("VALUE=DATE:20170701", tzmap);
        s.forceDateOnly();
        ParsedDateTime e = ParsedDateTime.parse("VALUE=DATE:20170702", tzmap);
        e.forceDateOnly();
        invite.setDtStart(s);
        invite.setDtEnd(e);
        invite.setPriority("5");
        invite.setClassProp("PRI");
        invite.setOrganizer(new ZOrganizer("test@zimbra.com", null));
        invite.setUid(UUID.randomUUID().toString());
        invite.setMethod("REQUEST");
        invite.setName("Testing");
        invite.setFreeBusy("B");
        invite.setIsOrganizer(true);
        invite.setItemType(MailItem.Type.APPOINTMENT);
        String uid = UUID.randomUUID().toString();
        invite.setUid(uid);
        ZAttendee attendee = new ZAttendee(acct2.getName());
        invite.addAttendee(attendee);
        invite.setIsAllDayEvent(true);
        ParsedDateTime dtStart = ParsedDateTime.parse("VALUE=DATE:20170701", tzmap);
        dtStart.forceDateOnly();
        ParsedDuration duration = ParsedDuration.parse("P1D");
        List<IRecurrence> addRules = new ArrayList<IRecurrence>();
        List<IRecurrence> subRules = new ArrayList<IRecurrence>();
        ZRecur rule = new ZRecur("FREQ=WEEKLY;COUNT=4;INTERVAL=1;BYDAY=SA", tzmap);
        addRules.add(new SimpleRepeatingRule(dtStart, duration, rule, null));
        RecurrenceRule recurrence = new RecurrenceRule(dtStart, duration, null, addRules, subRules);
        invite.setRecurrence(recurrence);
        // Organizer sends All day recurring appointment to test2 account
        mbox1.addInvite(null, invite, calendarFolder.getId());

        Invite inviteReply = MailboxTestUtil.generateInvite(acct2, fragment, calendar);
        ParsedDateTime sR = ParsedDateTime.parse("20170701T000000", tzmap);
        ParsedDateTime eR = ParsedDateTime.parse("20170702T000000", tzmap);
        inviteReply.setDtStart(sR);
        inviteReply.setDtEnd(eR);
        inviteReply.setPriority("5");
        inviteReply.setClassProp("PRI");
        inviteReply.setOrganizer(new ZOrganizer("test@zimbra.com", null));
        inviteReply.setUid(UUID.randomUUID().toString());
        inviteReply.setMethod("REPLY");
        inviteReply.setName("Testing");
        inviteReply.setFreeBusy("B");
        inviteReply.setIsOrganizer(true);
        inviteReply.setItemType(MailItem.Type.APPOINTMENT);
        inviteReply.setUid(uid);
        RecurId rid = new RecurId(sR, null);
        inviteReply.setRecurId(rid);
        ZAttendee attendeeR = new ZAttendee(acct2.getName());
        attendeeR.setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
        inviteReply.addAttendee(attendeeR);
        inviteReply.setIsAllDayEvent(true);
        // test2 accepts first invite instance and sends acceptance notification to organizer
        mbox1.addInvite(null, inviteReply, calendarFolder.getId());
        List<String> uids = new ArrayList<String>();
        uids.add(uid);
        Map<String, CalendarItem> calItems = mbox1.getCalendarItemsByUid(null, uids);
        CalendarItem calItem = calItems.get(uid);
        Invite[] invites = calItem.getInvites();
        Invite exception = null;
        for(Invite inv : invites) {
            if(inv.getRecurId() != null) {
                exception = inv;
                break;
            }
        }
        if(exception == null) {
            Assert.fail("Exceptional invite not found");
        } else {
            // The exception created in organizers series should not have the timezone in start/end time as sent in acceptance notification
            Assert.assertEquals(
                "Start time is not correct[" + exception.getStartTime().toString() + "]",
                "VALUE=DATE:20170701", exception.getStartTime().toString());
            Assert.assertEquals("End time is not correct[" + exception.getEndTime().toString() + "]",
                "VALUE=DATE:20170702", exception.getEndTime().toString());
        }
    }

    /*
     * Test with organizer timezone GMT-
     * In this case GMT-5:00 America/Panama
     */
    @Test
    public void testZCS2655NegativeTimezone() throws Exception {
        ScheduledTaskManager.startup();
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "Organizer2_ZCS2655@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "Attendee_ZCS2655@zimbra.com");
        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

        String desc = "The following is a new meeting request";
        Folder calendarFolder = mbox1.getCalendarFolders(null, SortBy.NONE).get(0);
        String fragment = "Some message";
        ZVCalendar calendar = new ZVCalendar();
        calendar.addDescription(desc, null);
        ZComponent comp = new ZComponent("VEVENT");
        calendar.addComponent(comp);

        Invite invite = MailboxTestUtil.generateInvite(acct1, fragment, calendar);
        ICalTimeZone pacific = new ICalTimeZone("America/Los_Angeles", -28800000, "16010101T020000",
            "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=11;BYDAY=1SU", "PST", -25200000,
            "16010101T020000", "FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=3;BYDAY=2SU", "PDT");
        TimeZoneMap tzmap = new TimeZoneMap(pacific);
        ParsedDateTime s = ParsedDateTime.parse("VALUE=DATE:20170701", tzmap);
        s.forceDateOnly();
        ParsedDateTime e = ParsedDateTime.parse("VALUE=DATE:20170702", tzmap);
        e.forceDateOnly();
        invite.setDtStart(s);
        invite.setDtEnd(e);
        invite.setPriority("5");
        invite.setClassProp("PRI");
        invite.setOrganizer(new ZOrganizer("test@zimbra.com", null));
        invite.setUid(UUID.randomUUID().toString());
        invite.setMethod("REQUEST");
        invite.setName("Testing");
        invite.setFreeBusy("B");
        invite.setIsOrganizer(true);
        invite.setItemType(MailItem.Type.APPOINTMENT);
        String uid = UUID.randomUUID().toString();
        invite.setUid(uid);
        ZAttendee attendee = new ZAttendee(acct2.getName());
        invite.addAttendee(attendee);
        invite.setIsAllDayEvent(true);
        ParsedDateTime dtStart = ParsedDateTime.parse("VALUE=DATE:20170701", tzmap);
        dtStart.forceDateOnly();
        ParsedDuration duration = ParsedDuration.parse("P1D");
        List<IRecurrence> addRules = new ArrayList<IRecurrence>();
        List<IRecurrence> subRules = new ArrayList<IRecurrence>();
        ZRecur rule = new ZRecur("FREQ=WEEKLY;COUNT=4;INTERVAL=1;BYDAY=SA", tzmap);
        addRules.add(new SimpleRepeatingRule(dtStart, duration, rule, null));
        RecurrenceRule recurrence = new RecurrenceRule(dtStart, duration, null, addRules, subRules);
        invite.setRecurrence(recurrence);
        // Organizer sends All day recurring appointment to test2 account
        mbox1.addInvite(null, invite, calendarFolder.getId());

        Invite inviteReply = MailboxTestUtil.generateInvite(acct2, fragment, calendar);
        ParsedDateTime sR = ParsedDateTime.parse("20170701T000000", tzmap);
        ParsedDateTime eR = ParsedDateTime.parse("20170702T000000", tzmap);
        inviteReply.setDtStart(sR);
        inviteReply.setDtEnd(eR);
        inviteReply.setPriority("5");
        inviteReply.setClassProp("PRI");
        inviteReply.setOrganizer(new ZOrganizer("test@zimbra.com", null));
        inviteReply.setUid(UUID.randomUUID().toString());
        inviteReply.setMethod("REPLY");
        inviteReply.setName("Testing");
        inviteReply.setFreeBusy("B");
        inviteReply.setIsOrganizer(true);
        inviteReply.setItemType(MailItem.Type.APPOINTMENT);
        inviteReply.setUid(uid);
        RecurId rid = new RecurId(sR, null);
        inviteReply.setRecurId(rid);
        ZAttendee attendeeR = new ZAttendee(acct2.getName());
        attendeeR.setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
        inviteReply.addAttendee(attendeeR);
        inviteReply.setIsAllDayEvent(true);
        // test2 accepts first invite instance and sends acceptance notification to organizer
        mbox1.addInvite(null, inviteReply, calendarFolder.getId());
        List<String> uids = new ArrayList<String>();
        uids.add(uid);
        Map<String, CalendarItem> calItems = mbox1.getCalendarItemsByUid(null, uids);
        CalendarItem calItem = calItems.get(uid);
        Invite[] invites = calItem.getInvites();
        Invite exception = null;
        for(Invite inv : invites) {
            if(inv.getRecurId() != null) {
                exception = inv;
                break;
            }
        }
        if(exception == null) {
            Assert.fail("Exceptional invite not found");
        } else {
            // The exception created in organizers series should not have the timezone in start/end time as sent in acceptance notification
            Assert.assertEquals(
                "Start time is not correct[" + exception.getStartTime().toString() + "]",
                "VALUE=DATE:20170701", exception.getStartTime().toString());
            Assert.assertEquals("End time is not correct[" + exception.getEndTime().toString() + "]",
                "VALUE=DATE:20170702", exception.getEndTime().toString());
        }
    }

    @After
    public void tearDown() {
        try {
            MailboxTestUtil.clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
