/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.cs.gal.GalSearchConfig.GalType;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link GalContact} — construction, attribute access, single-value
 * extraction (including multi-value array handling), group detection, sort-field derivation via
 * {@link Comparable#compareTo}, and the GAL-type predicate.
 */
public class GalContactTest {

    private Map<String, Object> attrs(Object... kv) {
        Map<String, Object> m = new HashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ---------- constructors / getId / getAttrs ----------

    @Test
    public void constructorDnAndAttrsExposesIdAndAttrs() {
        // Arrange
        Map<String, Object> a = attrs(ContactConstants.A_email, "joe@x.com");

        // Act
        GalContact gc = new GalContact("uid=joe", a);

        // Assert
        assertEquals("id is the dn passed in", "uid=joe", gc.getId());
        assertSame("attrs map is the one passed in", a, gc.getAttrs());
        assertEquals("attr value retrievable", "joe@x.com", gc.getSingleAttr(ContactConstants.A_email));
    }

    @Test
    public void isZimbraGalZimbraTypeReturnsTrue() {
        // Arrange
        GalContact gc = new GalContact(GalType.zimbra, "uid=z", attrs());

        // Act / Assert
        assertTrue("zimbra GalType => isZimbraGal true", gc.isZimbraGal());
    }

    @Test
    public void isZimbraGalLdapTypeReturnsFalse() {
        // Arrange
        GalContact gc = new GalContact(GalType.ldap, "uid=l", attrs());

        // Act / Assert
        assertFalse("ldap GalType => isZimbraGal false", gc.isZimbraGal());
    }

    @Test
    public void isZimbraGalDnOnlyConstructorReturnsFalse() {
        // Arrange — two-arg constructor leaves mGalType null
        GalContact gc = new GalContact("uid=none", attrs());

        // Act / Assert
        assertFalse("null GalType => not zimbra GAL", gc.isZimbraGal());
    }

    // ---------- getSingleAttr ----------

    @Test
    public void getSingleAttrStringArrayValueReturnsFirstElement() {
        // Arrange
        GalContact gc = new GalContact("uid=arr",
                attrs(ContactConstants.A_email, new String[] {"a@x.com", "b@x.com"}));

        // Act
        String v = gc.getSingleAttr(ContactConstants.A_email);

        // Assert
        assertEquals("first array element returned", "a@x.com", v);
    }

    @Test
    public void getSingleAttrPlainStringReturnsValue() {
        // Arrange
        GalContact gc = new GalContact("uid=s", attrs(ContactConstants.A_firstName, "Jane"));

        // Act / Assert
        assertEquals("plain string returned unchanged", "Jane",
                gc.getSingleAttr(ContactConstants.A_firstName));
    }

    @Test
    public void getSingleAttrMissingAttrReturnsNull() {
        // Arrange
        GalContact gc = new GalContact("uid=empty", attrs());

        // Act / Assert
        assertNull("missing attr => null", gc.getSingleAttr(ContactConstants.A_email));
    }

    // ---------- isGroup ----------

    @Test
    public void isGroupTypeGroupReturnsTrue() {
        // Arrange
        GalContact gc = new GalContact("uid=g",
                attrs(ContactConstants.A_type, ContactConstants.TYPE_GROUP));

        // Act / Assert
        assertTrue("type=group => isGroup true", gc.isGroup());
    }

    @Test
    public void isGroupNoTypeReturnsFalse() {
        // Arrange
        GalContact gc = new GalContact("uid=ng", attrs());

        // Act / Assert
        assertFalse("absent type => isGroup false", gc.isGroup());
    }

    // ---------- compareTo (drives getSortField) ----------

    @Test
    public void compareToByFullNameOrdersAlphabetically() {
        // Arrange
        GalContact alice = new GalContact("uid=a", attrs(ContactConstants.A_fullName, "Alice"));
        GalContact bob = new GalContact("uid=b", attrs(ContactConstants.A_fullName, "Bob"));

        // Act / Assert
        assertTrue("Alice sorts before Bob by fullName", alice.compareTo(bob) < 0);
        assertTrue("Bob sorts after Alice", bob.compareTo(alice) > 0);
        assertEquals("equal fullName compares equal", 0, alice.compareTo(
                new GalContact("uid=a2", attrs(ContactConstants.A_fullName, "Alice"))));
    }

    @Test
    public void compareToNoFullNameFallsBackToFirstAndLastName() {
        // Arrange — no fullName, so sort field is "first last"
        GalContact jd = new GalContact("uid=jd",
                attrs(ContactConstants.A_firstName, "John", ContactConstants.A_lastName, "Doe"));
        GalContact zz = new GalContact("uid=zz",
                attrs(ContactConstants.A_firstName, "Zoe", ContactConstants.A_lastName, "Zane"));

        // Act / Assert — "John Doe" < "Zoe Zane"
        assertTrue("first+last sort field orders John before Zoe", jd.compareTo(zz) < 0);
    }

    @Test
    public void compareToOnlyEmailUsesEmailAsSortField() {
        // Arrange — no name attrs at all => sort by email
        GalContact a = new GalContact("uid=ea", attrs(ContactConstants.A_email, "aaa@x.com"));
        GalContact b = new GalContact("uid=eb", attrs(ContactConstants.A_email, "bbb@x.com"));

        // Act / Assert
        assertTrue("email used as sort field when no name present", a.compareTo(b) < 0);
    }

    @Test
    public void compareToNoSortableAttrsTreatedAsEmptyString() {
        // Arrange — both empty sort fields compare equal
        GalContact a = new GalContact("uid=x", attrs());
        GalContact b = new GalContact("uid=y", attrs());

        // Act / Assert
        assertEquals("two empty sort fields are equal", 0, a.compareTo(b));
    }

    @Test
    public void compareToNonGalContactReturnsZero() {
        // Arrange
        GalContact gc = new GalContact("uid=z", attrs(ContactConstants.A_fullName, "Z"));

        // Act / Assert
        assertEquals("comparing against a non-GalContact yields 0", 0, gc.compareTo("not a contact"));
    }

    // ---------- toString ----------

    @Test
    public void toStringIncludesId() {
        // Arrange
        GalContact gc = new GalContact("uid=tostr", attrs());

        // Act
        String s = gc.toString();

        // Assert
        assertTrue("toString embeds the id", s.contains("uid=tostr"));
        assertTrue("toString labels the contact", s.contains("LdapGalContact"));
    }

    // Keep AccountBy referenced to mirror neighbor imports without unused-import warnings.
    @SuppressWarnings("unused")
    private static final AccountBy UNUSED = AccountBy.name;
}
