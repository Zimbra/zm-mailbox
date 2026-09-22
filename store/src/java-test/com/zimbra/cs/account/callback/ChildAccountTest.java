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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ChildAccount} attribute callback. Verifies visible-child
 * consistency and the circular parent/child detection against real accounts created via
 * the in-memory harness.
 */
public class ChildAccountTest {

    private static final String CHILD = Provisioning.A_zimbraChildAccount;

    private static final String VISIBLE = Provisioning.A_zimbraPrefChildVisibleAccount;

    private Provisioning prov;

    private Account parent;

    private Account child;

    private ChildAccount callback;

    /** Entities created per test, deleted in {@link #tearDown()} even when a test fails. */
    private final List<Account> createdAccounts = new ArrayList<Account>();

    private final List<Cos> createdCos = new ArrayList<Cos>();

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();

        Map<String, Object> childAttrs = new HashMap<String, Object>();
        childAttrs.put(Provisioning.A_zimbraId, "child-id-1111");
        child = prov.createAccount("child@example.com", "test123", childAttrs);
        createdAccounts.add(child);

        Map<String, Object> parentAttrs = new HashMap<String, Object>();
        parentAttrs.put(Provisioning.A_zimbraId, "parent-id-2222");
        parent = prov.createAccount("parent@example.com", "test123", parentAttrs);
        createdAccounts.add(parent);

        callback = new ChildAccount();
    }

    @After
    public void tearDown() throws Exception {
        for (Account a : createdAccounts) {
            try {
                prov.deleteAccount(a.getId());
            } catch (Exception ignore) {
                // best-effort cleanup: one failure must not block the rest
            }
        }
        for (Cos c : createdCos) {
            try {
                prov.deleteCos(c.getId());
            } catch (Exception ignore) {
                // best-effort cleanup
            }
        }
        createdAccounts.clear();
        createdCos.clear();
    }

    @Test
    public void preModifyAlreadyDoneReturnsImmediately() throws Exception {
        // Arrange — pre-mark done; pass a bogus child id that would otherwise throw
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        ctx.isDoneAndSetIfNot(ChildAccount.class);
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, "does-not-exist");

        // Act — short-circuits, so no NO_SUCH_ACCOUNT is thrown
        callback.preModify(ctx, CHILD, "does-not-exist", mod, parent);

        // Assert — no visible-child entry added because we returned early
        assertFalse(mod.containsKey("-" + VISIBLE));
    }

    @Test
    public void preModifyAddValidChildNoVisibleChildrenSucceeds() throws Exception {
        // Arrange — set child id (exists) as a child of parent; no visible children
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, CHILD, child.getId(), mod, parent);

        // Assert — child id is a valid existing account, no removal entries added
        assertFalse(mod.containsKey("-" + VISIBLE));
        assertEquals(child.getId(), mod.get(CHILD));
    }

    @Test
    public void preModifyValueNotAmongChildrenReturnsEarly() throws Exception {
        // Arrange — allChildren set to child.getId(), but value is some other id not in the set
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — value "other-id" is not contained in allChildren, so callback returns early
        callback.preModify(ctx, CHILD, "other-id", mod, parent);

        // Assert — no exception, no removal entry
        assertFalse(mod.containsKey("-" + VISIBLE));
    }

    @Test
    public void preModifyVisibleChildNotAChildThrowsInvalidRequest() throws Exception {
        // Arrange — children set is {child.getId()}; visible children are
        // {child.getId(), "stranger-id"} where "stranger-id" is NOT a child.
        // The callback is invoked for the child.getId() value (which IS a child) so it
        // does not return early, and the visible-child consistency loop runs.
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        mod.put(VISIBLE, new String[] {child.getId(), "stranger-id" });
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert — visible child must be one of the children
        try {
            callback.preModify(ctx, CHILD, child.getId(), mod, parent);
            fail("expected INVALID_REQUEST when a visible child is not one of the children");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("stranger-id"));
        }
    }

    @Test
    public void preModifyDeletingAllChildrenReturnsEarlyWithoutClearingVisible() throws Exception {
        // Arrange — parent currently has a child, request deletes ALL children (empty string)
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(CHILD, child.getId());
        prov.modifyAttrs(parent, seed);

        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, ""); // deleting all children -> allChildren becomes empty
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — DELETING makes the resulting allChildren empty, so the "" value being
        // modified is not contained in allChildren and the callback returns early
        // (before the deleting() branch that would clear visible children).
        callback.preModify(ctx, CHILD, "", mod, parent);

        // Assert — early return: no visible-child entry was added by the callback
        assertFalse(mod.containsKey(VISIBLE));
        assertFalse(mod.containsKey("-" + VISIBLE));
    }

    @Test
    public void preModifyRemovingChildWithoutUpdatingVisibleRemovesStaleVisibleChild() throws Exception {
        // Arrange — a second child account, so the value being modified can stay a child
        // after the removal (otherwise the callback returns early when the modified value
        // is no longer among the resulting children).
        Map<String, Object> child2Attrs = new HashMap<String, Object>();
        child2Attrs.put(Provisioning.A_zimbraId, "child-id-3333");
        Account child2 = prov.createAccount("child2@example.com", "test123", child2Attrs);
        createdAccounts.add(child2);

        // parent has {child, child2} as children and child2 as a visible child
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(CHILD, new String[] {child.getId(), child2.getId() });
        seed.put(VISIBLE, child2.getId());
        prov.modifyAttrs(parent, seed);

        // Request REMOVES child2 (so it is no longer a child) without touching visible.
        // The callback is invoked for child.getId(), which remains a child.
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put("-" + CHILD, child2.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, CHILD, child.getId(), mod, parent);

        // Assert — stale visible child (child2) scheduled for removal
        Object toRemove = mod.get("-" + VISIBLE);
        assertTrue("expected a String[] removal entry", toRemove instanceof String[]);
        String[] arr = (String[]) toRemove;
        assertEquals(1, arr.length);
        assertEquals(child2.getId(), arr[0]);
    }

    @Test
    public void preModifyChildAccountDoesNotExistThrowsNoSuchAccount() throws Exception {
        // Arrange — reference a child id that has no corresponding account
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, "ghost-child-id");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, CHILD, "ghost-child-id", mod, parent);
            fail("expected NO_SUCH_ACCOUNT for nonexistent child");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        }
    }

    @Test
    public void preModifyCircularRelationshipThrowsInvalidRequest() throws Exception {
        // Arrange — make the child already have the parent as ITS child (circular)
        Map<String, Object> childMod = new HashMap<String, Object>();
        childMod.put(CHILD, parent.getId());
        prov.modifyAttrs(child, childMod);

        // Now parent tries to add child as a child -> circular
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, CHILD, child.getId(), mod, parent);
            fail("expected INVALID_REQUEST for circular parent/child relationship");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().toLowerCase().contains("parent"));
        }
    }

    @Test
    public void preModifyNonAccountEntrySkipsCircularCheck() throws Exception {
        // Arrange — a Cos entry; circular check (entry instanceof Account) is skipped.
        // Use empty children so visible-child loop does nothing.
        Cos cos = prov.createCos("childcos", new HashMap<String, Object>());
        createdCos.add(cos);
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — completes without circular check; child existence not validated for non-Account
        callback.preModify(ctx, CHILD, child.getId(), mod, cos);

        // Assert — no removal entry, no exception
        assertFalse(mod.containsKey("-" + VISIBLE));
    }

    @Test
    public void preModifyValidVisibleChildThatIsAChildSucceeds() throws Exception {
        // Arrange — child is both a child and a visible child (consistent)
        Map<String, Object> mod = new HashMap<String, Object>();
        mod.put(CHILD, child.getId());
        mod.put(VISIBLE, child.getId());
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, VISIBLE, child.getId(), mod, parent);

        // Assert — consistent state, no removal entry added
        assertFalse(mod.containsKey("-" + VISIBLE));
        assertEquals(child.getId(), mod.get(VISIBLE));
    }

    @Test
    public void preModifyLookupChildByIdResolvesViaProvisioning() throws Exception {
        // Arrange — sanity: the child account is retrievable by the id we use in the callback
        Account fetched = prov.get(AccountBy.id, child.getId());

        // Assert — confirms the harness backs the circular-check lookup path
        assertEquals("child@example.com", fetched.getName());
        assertEquals(child.getId(), fetched.getId());
    }
}
