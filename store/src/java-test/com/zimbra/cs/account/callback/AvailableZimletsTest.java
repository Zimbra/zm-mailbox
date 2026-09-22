/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

package com.zimbra.cs.account.callback;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AvailableZimlets} - the AttributeCallback that re-shuffles
 * zimbraZimletAvailableZimlets prefixes (replace/add/delete) before a modify is applied.
 */
public class AvailableZimletsTest {

    private static final String ATTR = Provisioning.A_zimbraZimletAvailableZimlets;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private static Set<String> setOf(String[] arr) {
        return new HashSet<String>(Arrays.asList(arr));
    }

    private CallbackContext modifyCtx() {
        return new CallbackContext(CallbackContext.Op.MODIFY);
    }

    @Test
    public void preModifyReplaceWithStringArrayNormalizesIntoSingleReplacedValue() throws Exception {
        // Arrange
        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, new String[] {"+foo", "-bar" });

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);

        // Assert - the +/- keys are gone, replaced by a single normalized value array
        assertFalse("delete key must be cleared", attrsToModify.containsKey("-" + ATTR));
        assertFalse("add key must be cleared", attrsToModify.containsKey("+" + ATTR));
        assertTrue("replace key must remain", attrsToModify.containsKey(ATTR));
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals(2, result.length);
        assertEquals(setOf(new String[] {"+foo", "-bar" }), setOf(result));
    }

    @Test
    public void preModifyReplaceConflictingPrefixesLastPrefixWins() throws Exception {
        // Arrange - both !foo and +foo provided; ZimletPresence dedupes by name, last wins
        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, new String[] {"!foo", "+foo" });

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);

        // Assert - deduped to a single entry, with the later (enabled '+') prefix
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals("conflicting prefixes for same zimlet must dedupe to one", 1, result.length);
        assertEquals("+foo", result[0]);
    }

    @Test
    public void preModifyReplaceSingleStringProducesSingleValue() throws Exception {
        // Arrange
        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(ATTR, "!baz");

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get(ATTR), attrsToModify, null);

        // Assert
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals(1, result.length);
        assertEquals("!baz", result[0]);
    }

    @Test
    public void preModifyAddOnEntryWithCurrentValuesMergesAndChangesPrefix() throws Exception {
        // Arrange - account currently has +bar; admin adds -bar (same zimlet, new prefix)
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(ATTR, "+bar");
        Account account = prov.createAccount("zimlet-add@example.com", "test123", acctAttrs);
        assertEquals(setOf(new String[] {"+bar" }), account.getMultiAttrSet(ATTR));

        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("+" + ATTR, "-bar");

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get("+" + ATTR), attrsToModify, account);

        // Assert - existing +bar replaced by -bar; final value array has the new prefix
        assertFalse(attrsToModify.containsKey("+" + ATTR));
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals(1, result.length);
        assertEquals("-bar", result[0]);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyDeleteMatchingPrefixRemovesZimlet() throws Exception {
        // Arrange - account has !foo; admin deletes foo (no prefix matches anything)
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(ATTR, "!foo");
        Account account = prov.createAccount("zimlet-del@example.com", "test123", acctAttrs);

        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("-" + ATTR, "foo");

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get("-" + ATTR), attrsToModify, account);

        // Assert - foo removed; resulting value list is empty
        assertFalse(attrsToModify.containsKey("-" + ATTR));
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals("matching delete must remove the zimlet", 0, result.length);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyDeleteNonMatchingPrefixIsNoop() throws Exception {
        // Arrange - account has !foo; admin sends +foo to delete (non-matching prefix => noop)
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(ATTR, "!foo");
        Account account = prov.createAccount("zimlet-noop@example.com", "test123", acctAttrs);

        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("-" + ATTR, "+foo");

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get("-" + ATTR), attrsToModify, account);

        // Assert - foo survives because the prefix did not match
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals("non-matching delete must be a noop", 1, result.length);
        assertEquals("!foo", result[0]);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyDeleteWithStringArrayRemovesMultiple() throws Exception {
        // Arrange
        Map<String, Object> acctAttrs = new HashMap<String, Object>();
        acctAttrs.put(ATTR, new String[] {"!foo", "+bar" });
        Account account = prov.createAccount("zimlet-multi@example.com", "test123", acctAttrs);

        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("-" + ATTR, new String[] {"foo", "bar" });

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get("-" + ATTR), attrsToModify, account);

        // Assert
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals(0, result.length);

        prov.deleteAccount(account.getId());
    }

    @Test
    public void preModifyAddWithStringArrayOnCosAddsAll() throws Exception {
        // Arrange
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        Cos cos = prov.createCos("zimlet-cos", cosAttrs);

        AvailableZimlets callback = new AvailableZimlets();
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put("+" + ATTR, new String[] {"+a", "!b" });

        // Act
        callback.preModify(modifyCtx(), ATTR, attrsToModify.get("+" + ATTR), attrsToModify, cos);

        // Assert
        String[] result = (String[]) attrsToModify.get(ATTR);
        assertEquals(2, result.length);
        assertEquals(setOf(new String[] {"+a", "!b" }), setOf(result));
    }

    @Test
    public void postModifyIsNoopDoesNotThrow() throws Exception {
        // Arrange
        AvailableZimlets callback = new AvailableZimlets();

        // Act / Assert - postModify is a documented no-op
        callback.postModify(modifyCtx(), ATTR, null);
        assertNotNull("callback instance still usable after postModify", callback);
    }
}
