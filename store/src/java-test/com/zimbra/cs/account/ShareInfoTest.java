/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;
import com.zimbra.soap.mail.message.SendShareNotificationRequest.Action;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author zimbra
 *
 */
public class ShareInfoTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("rcpt@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except use SMTP to deliver mail
        MailboxManager.setInstance(new DirectInsertionMailboxManager());
    }

    @Test
    public void testGenNotifyBody() {

        Locale locale = new Locale("en", "US");
        String notes = "none";

        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId("46031e4c-deb4-4724-b5bb-8f854d0c518a");
        sid.setGranteeName("Demo User Three");
        sid.setGranteeType(ACL.GRANTEE_USER);

        sid.setPath("/Calendar/Cal1");
        sid.setFolderDefaultView(MailItem.Type.APPOINTMENT);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);

        sid.setOwnerAcctId("bbf152ca-e7cd-477e-9f72-70fef715c5f9");
        sid.setOwnerAcctEmail("test@zimbra.com");
        sid.setOwnerAcctDisplayName("Demo User Two");

        try {

            sid.setRights(ACL.stringToRights("rwidxap"));
            MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(sid,
                    notes, locale, null, null);
            Assert.assertNotNull(mmp);
            String body = (String) mmp.getBodyPart(0).getDataHandler()
                    .getContent();
            assertTrue(body.indexOf("Role: Admin") != -1);

        } catch (ServiceException | MessagingException | IOException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    @Test
    public void testGenNotifyBodyForCustom() {

        Locale locale = new Locale("en", "US");
        String notes = "none";

        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId("46031e4c-deb4-4724-b5bb-8f854d0c518a");
        sid.setGranteeName("Demo User Three");
        sid.setGranteeType(ACL.GRANTEE_USER);

        sid.setPath("/Calendar/Cal1");
        sid.setFolderDefaultView(MailItem.Type.APPOINTMENT);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);

        sid.setOwnerAcctId("bbf152ca-e7cd-477e-9f72-70fef715c5f9");
        sid.setOwnerAcctEmail("test@zimbra.com");
        sid.setOwnerAcctDisplayName("Demo User Two");

        try {

            sid.setRights(ACL.stringToRights("rwdxap"));
            MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(sid,
                    notes, locale, null, null);
            Assert.assertNotNull(mmp);
            String body = (String) mmp.getBodyPart(0).getDataHandler()
                    .getContent();
            assertTrue(body.indexOf("Role: Custom") != -1);

        } catch (ServiceException | MessagingException | IOException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

    /* Builds a fully-populated internal-grantee ShareInfoData for the notification builders. */
    private static ShareInfoData internalSid(String rightsStr, MailItem.Type view) throws ServiceException {
        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId("46031e4c-deb4-4724-b5bb-8f854d0c518a");
        sid.setGranteeName("three@zimbra.com");
        sid.setGranteeType(ACL.GRANTEE_USER);
        sid.setPath("/Calendar/Cal1");
        sid.setFolderDefaultView(view);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);
        sid.setOwnerAcctId("bbf152ca-e7cd-477e-9f72-70fef715c5f9");
        sid.setOwnerAcctEmail("test@zimbra.com");
        sid.setOwnerAcctDisplayName("Demo User Two");
        sid.setRights(ACL.stringToRights(rightsStr));
        return sid;
    }

    @Test
    public void genNotifBodyViewerRoleBodyMentionsViewerRole() throws Exception {
        // Arrange — read-only rights map to the "Viewer" role
        ShareInfoData sid = internalSid("r", MailItem.Type.APPOINTMENT);

        // Act
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, "hello", new Locale("en", "US"), null, null);

        // Assert — text part names the Viewer role and an internal share carries an XML part too
        assertNotNull(mmp);
        String body = (String) mmp.getBodyPart(0).getDataHandler().getContent();
        assertTrue("read-only share is the Viewer role", body.contains("Role: Viewer"));
        assertEquals("internal share has text + html + xml parts", 3, mmp.getCount());
    }

    @Test
    public void genNotifBodyManagerRoleBodyMentionsManagerRole() throws Exception {
        // Arrange — rwidx maps to the Manager role
        ShareInfoData sid = internalSid("rwidx", MailItem.Type.MESSAGE);

        // Act
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), null, null);

        // Assert
        assertNotNull(mmp);
        String body = (String) mmp.getBodyPart(0).getDataHandler().getContent();
        assertTrue("rwidx share is the Manager role", body.contains("Role: Manager"));
    }

    @Test
    public void genNotifBodyRevokeActionBodyIsRevocationNotice() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("r", MailItem.Type.APPOINTMENT);

        // Act — the revoke branch uses genRevokePart for both text and html
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), Action.revoke, null);

        // Assert — body is generated and (internal grantee) still has the XML part
        assertNotNull(mmp);
        String body = (String) mmp.getBodyPart(0).getDataHandler().getContent();
        assertNotNull("revoke text part must be generated", body);
        assertEquals(3, mmp.getCount());
    }

    @Test
    public void genNotifBodyExpireActionBodyIsExpirationNotice() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rw", MailItem.Type.TASK);

        // Act — the expire branch uses genExpirePart
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), Action.expire, null);

        // Assert
        assertNotNull(mmp);
        assertNotNull(mmp.getBodyPart(0).getDataHandler().getContent());
    }

    @Test
    public void genNotifBodyEditActionBodyUsesModifiedShareWording() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rwidxa", MailItem.Type.CONTACT);

        // Act — edit action routes through genPart with shareModified=true
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, "edited", new Locale("en", "US"), Action.edit, null);

        // Assert
        assertNotNull(mmp);
        String body = (String) mmp.getBodyPart(0).getDataHandler().getContent();
        assertTrue("admin rights map to the Admin role", body.contains("Role: Admin"));
    }

    @Test
    public void getMimePartHtmlNewShareReturnsHtmlContainingRoleAndFolder() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("r", MailItem.Type.DOCUMENT);

        // Act
        String html = ShareInfo.NotificationSender.getMimePartHtml(
                sid, "notes", new Locale("en", "US"), null, null, null);

        // Assert — html mentions the viewer role text
        assertNotNull(html);
        assertTrue("html part mentions the Viewer role", html.contains("Viewer"));
    }

    @Test
    public void getMimePartTextRevokeActionReturnsRevokeText() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rw", MailItem.Type.APPOINTMENT);

        // Act — text variant on the revoke branch
        String text = ShareInfo.NotificationSender.getMimePartText(
                sid, null, new Locale("en", "US"), Action.revoke, null, null);

        // Assert
        assertNotNull("revoke text must be produced", text);
        assertFalse("revoke text must not be empty", text.isEmpty());
    }

    @Test
    public void getMimePartTextExpireActionReturnsExpireText() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rw", MailItem.Type.MESSAGE);

        // Act
        String text = ShareInfo.NotificationSender.getMimePartText(
                sid, null, new Locale("en", "US"), Action.expire, null, null);

        // Assert
        assertNotNull(text);
        assertFalse(text.isEmpty());
    }

    @Test
    public void genXmlPartNewShareEmitsShareElementWithGranteeAndGrantor() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rwi", MailItem.Type.APPOINTMENT);

        // Act — null action => a "new" share element with action attribute and permission
        String xml = ShareInfo.NotificationSender.genXmlPart(sid, "notes", null, null);

        // Assert — the XML carries grantee/grantor ids and the link permission
        assertNotNull(xml);
        assertTrue("xml names the grantee id", xml.contains("46031e4c-deb4-4724-b5bb-8f854d0c518a"));
        assertTrue("xml names the grantor id", xml.contains("bbf152ca-e7cd-477e-9f72-70fef715c5f9"));
    }

    @Test
    public void genXmlPartRevokeActionOmitsPermissionAndUsesRevokeElement() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rw", MailItem.Type.APPOINTMENT);

        // Act — revoke action takes the non-edit branch (revoke element, no perm attribute)
        String xml = ShareInfo.NotificationSender.genXmlPart(sid, null, null, Action.revoke);

        // Assert — still references the grantee, and produced valid XML
        assertNotNull(xml);
        assertTrue(xml.contains("46031e4c-deb4-4724-b5bb-8f854d0c518a"));
    }

    @Test
    public void genXmlPartExpireActionMarksExpireAttribute() throws Exception {
        // Arrange
        ShareInfoData sid = internalSid("rw", MailItem.Type.APPOINTMENT);

        // Act — expire action adds the expire flag to the revoke element
        String xml = ShareInfo.NotificationSender.genXmlPart(sid, null, null, Action.expire);

        // Assert
        assertNotNull(xml);
        assertTrue("expire xml still references the grantor",
                xml.contains("bbf152ca-e7cd-477e-9f72-70fef715c5f9"));
    }

    @Test
    public void genNotifBodyCustomRightsBodyMentionsCustomRole() throws Exception {
        // Arrange — a non-standard rights combination yields the Custom role
        ShareInfoData sid = internalSid("ra", MailItem.Type.APPOINTMENT);

        // Act
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), null, null);

        // Assert
        String body = (String) mmp.getBodyPart(0).getDataHandler().getContent();
        assertTrue("ra is a non-standard combination => Custom role", body.contains("Role: Custom"));
    }

    /* Returns the plain-text part (index 0) body of a freshly built new-share notification. */
    private static String textBody(ShareInfoData sid) throws Exception {
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), null, null);
        return (String) mmp.getBodyPart(0).getDataHandler().getContent();
    }

    // ------------------------------------------------------------------
    // getRightsText (L795-808): each (rights & MASK) != 0 line and the
    // append order. ACL ACTION ('x') is intentionally NOT listed.
    // ------------------------------------------------------------------

    @Test
    public void getRightsTextReadOnlyListsExactlyView() throws Exception {
        // "r" must produce exactly "View" — no other action and no comma.
        // MathMutator turning any &-mask into |-mask would make an extra action appear;
        // NegateConditionals on the READ line would drop "View".
        String body = textBody(internalSid("r", MailItem.Type.APPOINTMENT));
        assertTrue("read-only allowed actions must be exactly 'View'",
                body.contains("Allowed actions: View\n"));
        assertFalse("read-only must not mention Edit", body.contains("Allowed actions: View, Edit"));
    }

    @Test
    public void getRightsTextReadWriteListsViewThenEditCommaSeparated() throws Exception {
        // "rw" => "View, Edit". This pins appendCommaSeparated (L774):
        //  - boundary mutant (length > 0 -> >= 0) would prepend ", " to the first item;
        //  - negate mutant would drop the separator entirely ("ViewEdit").
        String body = textBody(internalSid("rw", MailItem.Type.MESSAGE));
        assertTrue("rw must render 'View, Edit' with a single comma separator",
                body.contains("Allowed actions: View, Edit\n"));
        assertFalse("first action must not be preceded by a comma",
                body.contains("Allowed actions: , View"));
    }

    @Test
    public void getRightsTextAllFiveActionsListsEveryActionInOrder() throws Exception {
        // "rwida" exercises READ, WRITE, INSERT, DELETE and ADMIN lines (L798-L802).
        // Removing any appendCommaSeparated call (VoidMethodCall) or negating any line
        // would change this exact, ordered list.
        String body = textBody(internalSid("rwida", MailItem.Type.CONTACT));
        assertTrue("all five mapped rights must render in fixed order",
                body.contains("Allowed actions: View, Edit, Add, Remove, Administer\n"));
    }

    @Test
    public void getRightsTextInsertAndDeleteOnlyListsAddRemove() throws Exception {
        // "id" => INSERT + DELETE only => "Add, Remove". This isolates the INSERT (L800) and
        // DELETE (L801) lines: a math mutant on either mask, or negating either conditional,
        // changes this output. READ/WRITE/ADMIN lines must stay silent.
        String body = textBody(internalSid("id", MailItem.Type.MESSAGE));
        assertTrue("insert+delete must render exactly 'Add, Remove'",
                body.contains("Allowed actions: Add, Remove\n"));
        assertFalse("must not include View", body.contains("View"));
    }

    @Test
    public void getRightsTextAdminOnlyListsAdminister() throws Exception {
        // "a" => ADMIN only => "Administer". Isolates the ADMIN line (L802).
        String body = textBody(internalSid("a", MailItem.Type.MESSAGE));
        assertTrue("admin-only must render exactly 'Administer'",
                body.contains("Allowed actions: Administer\n"));
    }

    @Test
    public void getRightsTextActionRightOnlyRendersNoneNotAnAction() throws Exception {
        // "x" is ACL ACTION, which getRightsText does NOT map to any action. The result must be
        // the localized "None" — proving none of the five mask checks (READ/WRITE/INSERT/DELETE/
        // ADMIN) match. A MathMutator turning '&' into '|' on any line would make that action
        // appear here, since (ACTION | mask) is always non-zero.
        String body = textBody(internalSid("x", MailItem.Type.MESSAGE));
        assertTrue("action-only share lists no concrete action => 'None'",
                body.contains("Allowed actions: None\n"));
        assertFalse("ACTION must not be rendered as View", body.contains("Allowed actions: View"));
        assertFalse("ACTION must not be rendered as Administer",
                body.contains("Allowed actions: Administer"));
    }

    // ------------------------------------------------------------------
    // formatFolderDesc (L810-841): folder vs file wording and the view label
    // ------------------------------------------------------------------

    @Test
    public void formatFolderDescAppointmentFolderRendersCalendarFolder() throws Exception {
        // APPOINTMENT view => "Calendar"; notifyForDocument=false (genNotifBody) => "Folder".
        // L832's ternary picks shareNotifBodyFolderDesc; L840 emits "(Calendar Folder)" as {1}.
        String body = textBody(internalSid("r", MailItem.Type.APPOINTMENT));
        assertTrue("appointment folder desc must read '(Calendar Folder)'",
                body.contains("(Calendar Folder)"));
        assertFalse("notifyForDocument=false must not produce the File wording",
                body.contains("(Calendar File)"));
    }

    @Test
    public void formatFolderDescMessageFolderRendersMailFolder() throws Exception {
        // MESSAGE view => "Mail" => "(Mail Folder)".
        String body = textBody(internalSid("r", MailItem.Type.MESSAGE));
        assertTrue("message folder desc must read '(Mail Folder)'",
                body.contains("(Mail Folder)"));
    }

    @Test
    public void formatFolderDescNotifyForDocumentRendersFileWording() throws Exception {
        // notifyForDocument=true flips L832's ternary to shareNotifyFileBodyDesc => "(... File)".
        // getMimePartText with notifyForDocument=true reaches formatFolderDesc with the true branch.
        ShareInfoData sid = internalSid("r", MailItem.Type.DOCUMENT);
        String text = ShareInfo.NotificationSender.getMimePartText(
                sid, null, new Locale("en", "US"), null, null, null, true);
        assertTrue("document file desc must use the File wording",
                text.contains("(Briefcase File)"));
        assertFalse("notifyForDocument=true must not use the Folder wording",
                text.contains("(Briefcase Folder)"));
    }

    // ------------------------------------------------------------------
    // genNotifBody internal vs external grantee (L590-636): part count & XML presence
    // ------------------------------------------------------------------

    @Test
    public void genNotifBodyExternalGuestGranteeOmitsXmlPart() throws Exception {
        // A GUEST grantee makes goesToExternalAddr true, so the XML part (L631 guard) is skipped:
        // the multipart has only text + html (2 parts), unlike the 3 parts for an internal share.
        // Use a non-null action (revoke) so genNotifBody takes the genRevokePart branch and skips
        // the action==null external-URL lookup (getShareAcceptURL), which dereferences the owner
        // account that does not resolve under MockProvisioning. The XML-omission guard at L631 is
        // independent of the action, so this still pins external-grantee behavior.
        ShareInfoData sid = internalSid("r", MailItem.Type.APPOINTMENT);
        sid.setGranteeType(ACL.GRANTEE_GUEST);
        sid.setGranteeName("guest@external.invalid");

        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                sid, null, new Locale("en", "US"), Action.revoke, null);
        assertEquals("external (guest) share must omit the XML part => only text+html",
                2, mmp.getCount());
    }

    @Test
    public void genNotifBodyInternalUserGranteeIncludesXmlPart() throws Exception {
        // Internal user grantee => goesToExternalAddr false => XML part included (3 parts).
        MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(
                internalSid("r", MailItem.Type.APPOINTMENT), null, new Locale("en", "US"), null, null);
        assertEquals("internal share keeps the XML part => text+html+xml", 3, mmp.getCount());
    }
}
