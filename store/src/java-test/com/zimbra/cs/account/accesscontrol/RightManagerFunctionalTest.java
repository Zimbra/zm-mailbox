/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link RightManager}. The manager loads the real right-definition XML files
 * (zimbra-rights.xml, zimbra-user-rights.xml, rights-unittest.xml) from the configured rights
 * directory at construction, building the user/admin right maps. These tests drive the loaded
 * singleton: looking rights up by name (success and not-found), the inline-attr-right branch of
 * {@link RightManager#getRight}, and the aggregate getters. The rights directory is wired by
 * {@link MailboxTestUtil#initServer()}.
 */
public class RightManagerFunctionalTest {

    private static RightManager rm;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        rm = RightManager.getInstance();
    }

    @Test
    public void getInstanceCalledTwiceReturnsSameSingleton() throws Exception {
        // Act
        RightManager a = RightManager.getInstance();
        RightManager b = RightManager.getInstance();

        // Assert — cached singleton
        assertNotNull("RightManager must load", a);
        assertSame("getInstance must be a singleton", a, b);
    }

    @Test
    public void getAllUserRightsAfterLoadIsNonEmptyAndSorted() throws Exception {
        // Act
        Map<String, UserRight> userRights = rm.getAllUserRights();

        // Assert — real rights were parsed from the user-rights XML
        assertNotNull(userRights);
        assertFalse("user rights must be loaded from XML", userRights.isEmpty());

        // sorted (TreeMap) — keys ascend
        String prev = null;
        for (String name : userRights.keySet()) {
            if (prev != null) {
                assertTrue("user rights must be sorted: " + prev + " <= " + name, prev.compareTo(name) <= 0);
            }
            prev = name;
        }
    }

    @Test
    public void getAllAdminRightsAfterLoadIsNonEmpty() throws Exception {
        // Act
        Map<String, AdminRight> adminRights = rm.getAllAdminRights();

        // Assert
        assertNotNull(adminRights);
        assertFalse("admin rights must be loaded from XML", adminRights.isEmpty());
    }

    @Test
    public void getUserRightKnownNameReturnsMatchingUserRight() throws Exception {
        // Arrange — pick any loaded user right by name
        String name = rm.getAllUserRights().keySet().iterator().next();

        // Act
        UserRight r = rm.getUserRight(name);

        // Assert — same object that is stored in the map, and it is a user right
        assertNotNull(r);
        assertEquals(name, r.getName());
        assertTrue("getUserRight must return a user right", r.isUserRight());
        assertSame(rm.getAllUserRights().get(name), r);
    }

    @Test
    public void getUserRightUnknownNameThrowsFailure() throws Exception {
        // Act / Assert
        try {
            rm.getUserRight("noSuchUserRight_xyz");
            fail("expected FAILURE for unknown user right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue(e.getMessage().contains("invalid right"));
        }
    }

    @Test
    public void getAdminRightKnownNameReturnsMatchingAdminRight() throws Exception {
        // Arrange
        String name = rm.getAllAdminRights().keySet().iterator().next();

        // Act
        AdminRight r = rm.getAdminRight(name);

        // Assert
        assertNotNull(r);
        assertEquals(name, r.getName());
        assertFalse("admin right is not a user right", r.isUserRight());
    }

    @Test
    public void getAdminRightUnknownNameThrowsFailure() throws Exception {
        // Act / Assert
        try {
            rm.getAdminRight("noSuchAdminRight_xyz");
            fail("expected FAILURE for unknown admin right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void getRightKnownUserRightResolvesViaInternalLookup() throws Exception {
        // Arrange — a plain (non-inline) right name
        String name = rm.getAllUserRights().keySet().iterator().next();

        // Act — getRight routes non-inline names through getRightInternal
        Right r = rm.getRight(name);

        // Assert
        assertNotNull(r);
        assertEquals(name, r.getName());
    }

    @Test
    public void getRightUnknownNonInlineNameThrowsNoSuchRight() throws Exception {
        // Act / Assert — getRightInternal(mustFind=true) throws NO_SUCH_RIGHT
        try {
            rm.getRight("totallyUnknownRight");
            fail("expected NO_SUCH_RIGHT for unknown right");
        } catch (AccountServiceException e) {
            assertEquals(AccountServiceException.NO_SUCH_RIGHT, e.getCode());
        }
    }

    @Test
    public void getRightInlineAttrRightSyntaxBuildsInlineRight() throws Exception {
        // Arrange — inline attr right looks like "set.account.<attr>" (contains dots)
        String inline = "set.account.zimbraId";

        // Act — getRight detects the inline syntax and builds an InlineAttrRight (no map lookup)
        Right r = rm.getRight(inline);

        // Assert — an inline right object is returned for the dotted name
        assertNotNull("inline attr right must be constructed", r);
        assertTrue("constructed right must be recognized as inline syntax",
                InlineAttrRight.looksLikeOne(inline));
    }

    @Test
    public void getRightInlineAttrRightUnknownTargetThrows() throws Exception {
        // Act / Assert — a dotted name with a bogus target type must fail to build
        try {
            rm.getRight("set.notATargetType.zimbraId");
            fail("expected ServiceException for inline right with invalid target type");
        } catch (ServiceException e) {
            assertNotNull("must report a failure code", e.getCode());
        }
    }

    @Test
    public void getRightKnownAdminRightResolvesViaInternalLookupToAdminRight() throws Exception {
        // Arrange — an admin right name routes through getRightInternal's sAdminRights branch
        String name = rm.getAllAdminRights().keySet().iterator().next();

        // Act
        Right r = rm.getRight(name);

        // Assert — the admin-right fallback branch of getRightInternal is exercised
        assertNotNull(r);
        assertEquals(name, r.getName());
        assertFalse("an admin right must not report as a user right", r.isUserRight());
        assertSame("getRight must return the same instance held in the admin map",
                rm.getAllAdminRights().get(name), r);
    }

    @Test
    public void genRightConstRightWithDescriptionEmitsConstantAndJavadoc() throws Exception {
        // Arrange — pick a user right that has a description set (most do)
        Right described = null;
        for (UserRight ur : rm.getAllUserRights().values()) {
            if (ur.getDesc() != null && !ur.getDesc().isEmpty()) {
                described = ur;
                break;
            }
        }
        assertNotNull("expected at least one user right with a description", described);

        // Act
        StringBuilder sb = new StringBuilder();
        rm.genRightConst(described, sb);
        String generated = sb.toString();

        // Assert — emits the RT_ constant declaration plus a javadoc block with the description
        assertTrue("must declare the RT_ constant",
                generated.contains("public static final String RT_" + described.getName()));
        assertTrue("constant value must be the right name",
                generated.contains("= \"" + described.getName() + "\";"));
        assertTrue("must open a javadoc block when a description exists", generated.contains("/**"));
        assertTrue("must close the javadoc block", generated.contains("*/"));
    }

    @Test
    public void genRightConstAppendsToExistingBufferDoesNotClobberPriorContent() throws Exception {
        // Arrange
        Right any = rm.getAllUserRights().values().iterator().next();
        StringBuilder sb = new StringBuilder("PREFIX-CONTENT");

        // Act
        rm.genRightConst(any, sb);

        // Assert — pre-existing buffer content is preserved and the new const is appended after it
        String out = sb.toString();
        assertTrue("prior buffer content must be retained", out.startsWith("PREFIX-CONTENT"));
        assertTrue("new constant appended after prior content",
                out.indexOf("RT_" + any.getName()) > "PREFIX-CONTENT".length() - 1);
    }

    @Test
    public void getUserRightAdminRightNameThrowsBecauseNotInUserMap() throws Exception {
        // Arrange — a real admin right name is absent from the user-rights map
        String adminName = rm.getAllAdminRights().keySet().iterator().next();

        // Act / Assert — getUserRight only consults sUserRights, so an admin name is "invalid"
        try {
            rm.getUserRight(adminName);
            fail("expected FAILURE: admin right is not a user right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue(e.getMessage().contains("invalid right"));
        }
    }

    @Test
    public void getAdminRightUserRightNameThrowsBecauseNotInAdminMap() throws Exception {
        // Arrange — a real user right name is absent from the admin-rights map
        String userName = rm.getAllUserRights().keySet().iterator().next();

        // Act / Assert — getAdminRight only consults sAdminRights
        try {
            rm.getAdminRight(userName);
            fail("expected FAILURE: user right is not an admin right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue(e.getMessage().contains("invalid right"));
        }
    }

    @Test
    public void getInstanceUnittestReturnsSameSingletonAsDefault() throws Exception {
        // Act — the unittest overload still returns the already-built singleton
        RightManager viaUnittest = RightManager.getInstance(true);

        // Assert
        assertSame("getInstance(true) must return the cached singleton", rm, viaUnittest);
    }

    @Test
    public void allAdminRightsContainAComboRightWithChildRights() throws Exception {
        // Arrange / Act — scan the loaded admin rights for a combo right and verify it aggregates
        ComboRight combo = null;
        for (AdminRight ar : rm.getAllAdminRights().values()) {
            if (ar.isComboRight()) {
                combo = (ComboRight) ar;
                break;
            }
        }

        // Assert — combo rights were parsed and they reference real child rights
        assertNotNull("expected at least one combo admin right loaded from XML", combo);
        assertFalse("a combo right must contain child rights", combo.getRights().isEmpty());
        for (Right child : combo.getRights()) {
            assertFalse("combo rights may only contain admin rights", child.isUserRight());
        }
    }

    @Test
    public void getRightInlineGetAttrRightBuildsInlineRightForGetAction() throws Exception {
        // Arrange — the "get" inline action variant (vs "set" already covered)
        String inline = "get.account.zimbraId";

        // Act
        Right r = rm.getRight(inline);

        // Assert
        assertNotNull("inline get-attr right must be constructed", r);
        assertTrue("must be recognized as inline syntax", InlineAttrRight.looksLikeOne(inline));
    }

    // ------------------------------------------------------------------
    // parseDefault (L271-279): <default>allow</default> -> Boolean.TRUE
    // ------------------------------------------------------------------

    @Test
    public void parseDefaultInviteRightDefaultIsAllowTrue() throws Exception {
        // The "invite" user right declares <default>allow</default>.
        // NegateConditionals on the "allow" branch would yield deny (FALSE); VoidMethodCall on
        // setDefault would leave the default null. Both are caught by this exact-value assertion.
        UserRight invite = rm.getUserRight("invite");
        assertEquals("invite default must parse 'allow' to Boolean.TRUE",
                Boolean.TRUE, invite.getDefault());
    }

    @Test
    public void parseDefaultNoDefaultElementDefaultRemainsNull() throws Exception {
        // The "loginAs" user right has no <default> element, so setDefault is never called
        // and getDefault() must stay null. This distinguishes it from the allow/deny branches.
        UserRight loginAs = rm.getUserRight("loginAs");
        assertEquals("loginAs has no <default>, so default must be null", null, loginAs.getDefault());
    }

    // ------------------------------------------------------------------
    // loadFallback (L419-433): fallback="InviteFallback" -> instance wired to right
    // ------------------------------------------------------------------

    @Test
    public void loadFallbackInviteRightBuildsInviteFallbackWiredToRight() throws Exception {
        // The "invite" right declares fallback="InviteFallback".
        // NullReturnVals (L432) would make loadFallback return null -> getFallback() null.
        // VoidMethodCall (L428) would skip cb.setRight(right) -> fallback.mRight stays null.
        UserRight invite = rm.getUserRight("invite");
        CheckRightFallback fallback = invite.getFallback();

        assertNotNull("invite must have a fallback handler", fallback);
        assertEquals("fallback must be the InviteFallback implementation",
                "com.zimbra.cs.account.accesscontrol.fallback.InviteFallback",
                fallback.getClass().getName());
        // setRight side effect: the fallback's back-reference must point at the invite right.
        assertSame("loadFallback must wire the right into the fallback via setRight",
                invite, fallback.mRight);
    }

    @Test
    public void noFallbackAttributeFallbackIsNull() throws Exception {
        // "loginAs" has no fallback attribute, so getFallback() must be null.
        UserRight loginAs = rm.getUserRight("loginAs");
        assertEquals("loginAs declares no fallback", null, loginAs.getFallback());
    }

    // ------------------------------------------------------------------
    // getBooleanAttr cache="1" (L219-225) + parseRight setCacheable
    // ------------------------------------------------------------------

    @Test
    public void cacheAttrDistinguishesCacheableFromNonCacheable() throws Exception {
        // "invite" has cache="1" -> getBoolean returns true -> setCacheable() called.
        // "createDistList" also has cache="1". Use adminLoginAs (cache="1") vs a getAttrs right.
        UserRight invite = rm.getUserRight("invite");
        assertTrue("invite cache='1' must be cacheable", invite.isCacheable());

        // "getAccount" admin right declares no cache attribute -> defaultValue false branch
        // (getBooleanAttr L222 BooleanFalseReturnVals / L224 changes return the wrong value).
        AdminRight getAccount = rm.getAdminRight("getAccount");
        assertFalse("getAccount declares no cache attr, default must be false",
                getAccount.isCacheable());
    }

    // ------------------------------------------------------------------
    // parseAttr / parseAttrs (L281-303): <attrs><a n=.../></attrs>
    // ------------------------------------------------------------------

    @Test
    public void parseAttrsSingleAttrRightCollectsDeclaredAttr() throws Exception {
        // "viewAdminSavedSearch" is a getAttrs right with exactly one <a n="zimbraAdminSavedSearches"/>.
        // NegateConditionals on the E_A element check (L296/L298) or removing addAttr (L287)
        // would drop the attribute from the set.
        AdminRight r = rm.getAdminRight("viewAdminSavedSearch");
        assertTrue("viewAdminSavedSearch must be an attr right", r.isAttrRight());
        Set<String> attrs = ((AttrRight) r).getAttrs();
        assertTrue("declared attr must be collected by parseAttr/parseAttrs",
                attrs.contains("zimbraAdminSavedSearches"));
        assertEquals("exactly one attr was declared", 1, attrs.size());
    }

    @Test
    public void parseAttrsMultiAttrRightCollectsEveryDeclaredAttr() throws Exception {
        // "configureQuota" declares four <a> elements; every one must land in the attr set,
        // proving parseAttr is invoked once per <a> child and the name is non-null (L283 guard).
        AdminRight r = rm.getAdminRight("configureQuota");
        Set<String> attrs = ((AttrRight) r).getAttrs();
        assertTrue(attrs.contains("zimbraMailQuota"));
        assertTrue(attrs.contains("zimbraQuotaWarnPercent"));
        assertTrue(attrs.contains("zimbraQuotaWarnInterval"));
        assertTrue(attrs.contains("zimbraQuotaWarnMessage"));
        assertEquals("all four configureQuota attrs must be collected", 4, attrs.size());
    }

    // ------------------------------------------------------------------
    // loadUI / parseUI (L570-587, L252-268): <ui name=.../> on a right
    // ------------------------------------------------------------------

    @Test
    public void loadUIRightWithUiElementResolvesUiDescription() throws Exception {
        // "setAdminConsoleAccountsInfoTab" references ui "Manage-Accounts-Account-GeneralInfo(modify)".
        // loadUI must register the UI (L576/L581 setDesc) and parseUI must attach it to the right.
        AdminRight r = rm.getAdminRight("setAdminConsoleAccountsInfoTab");
        UI ui = r.getUI();
        assertNotNull("right must have its UI resolved", ui);
        assertEquals("Manage-Accounts-Account-GeneralInfo(modify)", ui.getName());
        assertTrue("UI desc must be loaded from adminconsole-ui.xml",
                ui.getDesc().contains("General Information"));
    }

    // ------------------------------------------------------------------
    // loadHelp / parseHelp (L540-568, L233-249): <help> with desc + items
    // ------------------------------------------------------------------

    @Test
    public void loadHelpHelpWithItemsResolvesDescAndAllItems() throws Exception {
        // combo right "migrationAdminDomainRights" references help "migrationAdminHelp",
        // which has a <desc> (L556 setDesc) and two <item> elements (L558 addItem).
        AdminRight r = rm.getAdminRight("migrationAdminDomainRights");
        Help help = r.getHelp();
        assertNotNull("combo right must have its help resolved", help);
        assertEquals("migrationAdminHelp", help.getName());
        assertTrue("help desc must be loaded",
                help.getDesc().contains("running the migration wizard"));
        assertEquals("both <item> elements must be added", 2, help.getItems().size());
    }

    // ------------------------------------------------------------------
    // loadRight (L511-538): preset right typing + UserRight vs AdminRight map placement
    // ------------------------------------------------------------------

    @Test
    public void loadRightUserRightStoredInUserMapNotAdminMap() throws Exception {
        // loadRight (L530) puts UserRights into sUserRights and everything else into sAdminRights.
        // NegateConditionals on the instanceof check would swap the maps.
        assertNotNull("invite must be in the user-rights map", rm.getAllUserRights().get("invite"));
        assertEquals("invite must NOT be in the admin-rights map",
                null, rm.getAllAdminRights().get("invite"));
    }

    @Test
    public void loadRightAdminPresetRightTypedAsPreset() throws Exception {
        // "adminLoginAs" is type="preset". parseRight + loadRight must yield a preset right type.
        AdminRight r = rm.getAdminRight("adminLoginAs");
        assertSame("adminLoginAs must be a preset right", Right.RightType.preset, r.getRightType());
    }

    // ------------------------------------------------------------------
    // parseRight combo (L305-332): combo right aggregates only admin child rights
    // ------------------------------------------------------------------

    @Test
    public void parseRightsComboRightContainsExactDeclaredChildren() throws Exception {
        // "besAdminDomainRights" is a combo with a single child <r n="adminLoginAs"/>.
        // Removing parseRight's addRight or negating the user-right guard would change membership.
        AdminRight r = rm.getAdminRight("besAdminDomainRights");
        assertTrue("besAdminDomainRights must be a combo right", r.isComboRight());
        ComboRight combo = (ComboRight) r;
        boolean hasAdminLoginAs = false;
        for (Right child : combo.getRights()) {
            assertFalse("combo children must all be admin rights", child.isUserRight());
            if ("adminLoginAs".equals(child.getName())) {
                hasAdminLoginAs = true;
            }
        }
        assertTrue("declared child adminLoginAs must be present", hasAdminLoginAs);
        assertEquals("besAdminDomainRights declares exactly one child", 1, combo.getRights().size());
    }

    // ------------------------------------------------------------------
    // genRightConst L681: javadoc emitted only when desc != null, with the desc text
    // ------------------------------------------------------------------

    @Test
    public void genRightConstDescribedRightEmitsEscapedDescriptionInJavadoc() throws Exception {
        // "loginAs" has a known description. NegateConditionals on L681 (desc != null) would
        // suppress the description body; this asserts the desc text actually appears.
        UserRight loginAs = rm.getUserRight("loginAs");
        StringBuilder sb = new StringBuilder();
        rm.genRightConst(loginAs, sb);
        String out = sb.toString();
        assertTrue("javadoc must contain the right's description text",
                out.contains("login as another user"));
        assertTrue("constant must be declared with the right name",
                out.contains("public static final String RT_loginAs = \"loginAs\";"));
    }

    // ------------------------------------------------------------------
    // getInstance (L144-159): singleton non-null, Right.init wiring (named rights resolvable)
    // ------------------------------------------------------------------

    @Test
    public void getInstanceAfterInitNamedRightsResolvableThroughRightClass() throws Exception {
        // L158 NullReturnVals would make getInstance return null; L153 VoidMethodCall would skip
        // Right.init(mInstance), leaving the static Rights.User/Admin fields unwired.
        assertNotNull("getInstance must never return null after load", RightManager.getInstance());
        assertNotNull("Right.init must have wired the user loginAs right",
                Rights.User.R_loginAs);
        assertEquals("wired right must resolve to the same loaded loginAs right",
                "loginAs", Rights.User.R_loginAs.getName());
    }
}
