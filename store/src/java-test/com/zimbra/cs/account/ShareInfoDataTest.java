/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the {@link ShareInfoData} value object: getters/setters,
 * derived accessors, expiry logic, notification name fallbacks, and the
 * String/JAXB round-trip helpers.
 */
public class ShareInfoDataTest {

    private ShareInfoData buildFullShare() {
        ShareInfoData sid = new ShareInfoData();
        sid.setOwnerAcctId("owner-id");
        sid.setOwnerAcctEmail("owner@example.com");
        sid.setOwnerAcctDisplayName("Owner Display");
        sid.setItemId(42);
        sid.setItemUuid("uuid-42");
        sid.setPath("/Inbox/Shared");
        sid.setFolderDefaultView(MailItem.Type.MESSAGE);
        sid.setType(MailItem.Type.FOLDER);
        sid.setRights(ACL.RIGHT_READ);
        sid.setGranteeType(ACL.GRANTEE_USER);
        sid.setGranteeId("grantee-id");
        sid.setGranteeName("grantee@example.com");
        sid.setGranteeDisplayName("Grantee Display");
        return sid;
    }

    @Test
    public void settersAndGettersFullShareRoundTripValues() {
        // Arrange / Act
        ShareInfoData sid = buildFullShare();

        // Assert
        assertEquals("owner-id", sid.getOwnerAcctId());
        assertEquals("owner@example.com", sid.getOwnerAcctEmail());
        assertEquals("Owner Display", sid.getOwnerAcctDisplayName());
        assertEquals(42, sid.getItemId());
        assertEquals("uuid-42", sid.getItemUuid());
        assertEquals("/Inbox/Shared", sid.getPath());
        assertEquals("grantee-id", sid.getGranteeId());
        assertEquals("grantee@example.com", sid.getGranteeName());
        assertEquals("Grantee Display", sid.getGranteeDisplayName());
    }

    @Test
    public void getNamePathWithSlashesReturnsLeaf() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setPath("/Inbox/Sub/Leaf");

        // Act
        String name = sid.getName();

        // Assert
        assertEquals("Leaf", name);
    }

    @Test
    public void getRightsReadRightReturnsAbbreviatedString() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setRights(ACL.RIGHT_READ);

        // Act
        String rights = sid.getRights();

        // Assert
        assertEquals(ACL.rightsToString(ACL.RIGHT_READ), rights);
        assertEquals(ACL.RIGHT_READ, sid.getRightsCode());
    }

    @Test
    public void getTypeNotSetReturnsUnknown() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();

        // Act / Assert
        assertEquals(MailItem.Type.UNKNOWN, sid.getType());
    }

    @Test
    public void getTypeSetReturnsConfiguredType() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setType(MailItem.Type.APPOINTMENT);

        // Act / Assert
        assertEquals(MailItem.Type.APPOINTMENT, sid.getType());
    }

    @Test
    public void getGranteeTypeUserGranteeMatchesAclEncoding() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeType(ACL.GRANTEE_USER);

        // Act / Assert
        assertEquals(ACL.GRANTEE_USER, sid.getGranteeTypeCode());
        assertEquals(ACL.typeToString(ACL.GRANTEE_USER), sid.getGranteeType());
    }

    @Test
    public void getMountpointIdZmprovOnlyNotSetReturnsEmptyString() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();

        // Act / Assert
        assertEquals("", sid.getMountpointId_zmprov_only());
    }

    @Test
    public void getMountpointIdZmprovOnlySetReturnsValue() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setMountpointId_zmprov_only("mpt-7");

        // Act / Assert
        assertEquals("mpt-7", sid.getMountpointId_zmprov_only());
    }

    @Test
    public void isExpiredNoExpirySetReturnsFalse() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();

        // Act / Assert
        assertFalse(sid.isExpired());
        assertEquals(0L, sid.getExpiry());
    }

    @Test
    public void isExpiredPastExpiryReturnsTrue() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setExpiry(System.currentTimeMillis() - 10000L);

        // Act / Assert
        assertTrue(sid.isExpired());
    }

    @Test
    public void isExpiredFutureExpiryReturnsFalse() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setExpiry(System.currentTimeMillis() + 600000L);

        // Act / Assert
        assertFalse(sid.isExpired());
    }

    @Test
    public void getOwnerNotifNameDisplayNameSetPrefersDisplayName() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setOwnerAcctEmail("owner@example.com");
        sid.setOwnerAcctDisplayName("Owner Display");

        // Act / Assert
        assertEquals("Owner Display", sid.getOwnerNotifName());
    }

    @Test
    public void getOwnerNotifNameNoDisplayNameFallsBackToEmail() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setOwnerAcctEmail("owner@example.com");

        // Act / Assert
        assertEquals("owner@example.com", sid.getOwnerNotifName());
    }

    @Test
    public void getGranteeNotifNameNoDisplayNameFallsBackToGranteeName() {
        // Arrange
        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeName("grantee@example.com");

        // Act / Assert
        assertEquals("grantee@example.com", sid.getGranteeNotifName());
    }

    @Test
    public void toJAXBFullShareCopiesAllFields() {
        // Arrange
        ShareInfoData sid = buildFullShare();

        // Act
        com.zimbra.soap.type.ShareInfo jaxb = sid.toJAXB(null);

        // Assert — every field copied by toJAXB (L307-318) is asserted so that removing any
        // individual setter call is detected.
        assertEquals("owner-id", jaxb.getOwnerId());
        assertEquals("owner@example.com", jaxb.getOwnerEmail());          // L308
        assertEquals("Owner Display", jaxb.getOwnerDisplayName());        // L309
        assertEquals(42, jaxb.getFolderId());
        assertEquals("uuid-42", jaxb.getFolderUuid());                    // L311
        assertEquals("/Inbox/Shared", jaxb.getFolderPath());
        assertEquals(MailItem.Type.MESSAGE.toString(), jaxb.getDefaultView()); // L313
        assertEquals(sid.getRights(), jaxb.getRights());
        assertEquals(ACL.typeToString(ACL.GRANTEE_USER), jaxb.getGranteeType()); // L315
        assertEquals("grantee-id", jaxb.getGranteeId());                  // L316
        assertEquals("grantee@example.com", jaxb.getGranteeName());       // L317
        assertEquals("Grantee Display", jaxb.getGranteeDisplayName());    // L318
    }

    @Test
    public void fromJaxbShareInfoCopiesEveryField() throws Exception {
        // Arrange — distinct, verifiable values for each copied field.
        com.zimbra.soap.type.ShareInfo in = new com.zimbra.soap.type.ShareInfo();
        in.setOwnerId("owner-id");
        in.setOwnerEmail("owner@example.com");
        in.setOwnerDisplayName("Owner Display");
        in.setFolderId(7);
        in.setFolderUuid("fuuid");
        in.setFolderPath("/Inbox/Leaf");
        in.setDefaultView("message");
        in.setRights("rw");
        in.setGranteeType("usr");
        in.setGranteeId("grantee-id");
        in.setGranteeName("grantee@example.com");
        in.setGranteeDisplayName("Grantee Display");
        in.setMountpointId("mpt-99");

        // Act
        ShareInfoData sid = ShareInfoData.fromJaxbShareInfo(in);

        // Assert — one assertion per setter call in fromJaxbShareInfo (L267-279). Removing any
        // single setter call leaves its target field unset, failing the matching assertion.
        assertEquals("owner-id", sid.getOwnerAcctId());                       // L267
        assertEquals("owner@example.com", sid.getOwnerAcctEmail());           // L268
        assertEquals("Owner Display", sid.getOwnerAcctDisplayName());         // L269
        assertEquals(7, sid.getItemId());                                     // L270
        assertEquals("fuuid", sid.getItemUuid());                            // L271
        assertEquals("/Inbox/Leaf", sid.getPath());                          // L272
        assertEquals(MailItem.Type.MESSAGE, sid.getFolderDefaultViewCode()); // L273
        assertEquals(ACL.stringToRights("rw"), sid.getRightsCode());         // L274
        assertEquals(ACL.stringToType("usr"), sid.getGranteeTypeCode());     // L275
        assertEquals("grantee-id", sid.getGranteeId());                      // L276
        assertEquals("grantee@example.com", sid.getGranteeName());           // L277
        assertEquals("Grantee Display", sid.getGranteeDisplayName());        // L278
        assertEquals("mpt-99", sid.getMountpointId_zmprov_only());           // L279
    }

    @Test
    public void fromJaxbShareInfoThenToStringReflectsOwnerAndGrantee() throws Exception {
        // Arrange
        com.zimbra.soap.type.ShareInfo in = new com.zimbra.soap.type.ShareInfo();
        in.setOwnerId("owner-id");
        in.setOwnerEmail("owner@example.com");
        in.setOwnerDisplayName("Owner Display");
        in.setFolderId(7);
        in.setFolderUuid("fuuid");
        in.setFolderPath("/Inbox");
        in.setDefaultView("message");
        in.setRights("r");
        in.setGranteeType("usr");
        in.setGranteeId("grantee-id");
        in.setGranteeName("grantee@example.com");
        in.setGranteeDisplayName("Grantee Display");

        // Act
        ShareInfoData sid = ShareInfoData.fromJaxbShareInfo(in);
        String dump = sid.toString();

        // Assert
        assertEquals("owner-id", sid.getOwnerAcctId());
        assertEquals(7, sid.getItemId());
        assertEquals(MailItem.Type.MESSAGE, sid.getFolderDefaultViewCode());
        assertTrue("toString should include owner email", dump.contains("owner@example.com"));
        assertTrue("toString should include grantee display", dump.contains("Grantee Display"));
    }

    @Test
    public void getExpirySetNonZeroReturnsExactValue() {
        // getExpiry() must return the stored value, not a mutated primitive (0). The
        // PrimitiveReturns mutation at L228 would return 0 for any input.
        ShareInfoData sid = new ShareInfoData();
        sid.setExpiry(1234567890123L);
        assertEquals(1234567890123L, sid.getExpiry());
    }
}
