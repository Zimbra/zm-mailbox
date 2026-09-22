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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AccountServiceException}. These exercise the real
 * factory methods, verifying the resulting exception's message, code, fault
 * direction, and cause chaining (deep state), plus the nested
 * {@link AuthFailedServiceException} reason-hiding behavior.
 */
public class AccountServiceExceptionTest {

    @Test
    public void noSuchAccountBuildsSenderFaultWithNameInMessage() {
        // Act
        AccountServiceException e = AccountServiceException.NO_SUCH_ACCOUNT("joe@x.com");

        // Assert
        assertEquals(AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        assertTrue("message must include the missing name", e.getMessage().contains("joe@x.com"));
        assertTrue("message must say 'no such account'", e.getMessage().contains("no such account"));
        assertFalse("NO_SUCH_ACCOUNT is the sender's fault", e.isReceiversFault());
    }

    @Test
    public void accountExistsSimpleNameBuildsSenderFaultWithName() {
        // Act
        AccountServiceException e = AccountServiceException.ACCOUNT_EXISTS("dup@x.com");

        // Assert
        assertEquals(AccountServiceException.ACCOUNT_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("dup@x.com"));
        assertTrue(e.getMessage().contains("already exists"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void accountExistsWithDnAndCauseChainsCauseAndDn() {
        // Arrange
        Throwable cause = new IllegalStateException("ldap collision");

        // Act
        AccountServiceException e =
                AccountServiceException.ACCOUNT_EXISTS("dup@x.com", "uid=dup,dc=x", cause);

        // Assert
        assertEquals(AccountServiceException.ACCOUNT_EXISTS, e.getCode());
        assertTrue("DN must appear in message", e.getMessage().contains("uid=dup,dc=x"));
        assertSame("cause must be preserved", cause, e.getCause());
    }

    @Test
    public void maintenanceModeIsReceiversFault() {
        // Act
        AccountServiceException e = AccountServiceException.MAINTENANCE_MODE();

        // Assert
        assertEquals(AccountServiceException.MAINTENANCE_MODE, e.getCode());
        assertTrue("maintenance mode is the server's fault", e.isReceiversFault());
        assertTrue(e.getMessage().contains("maintenance mode"));
    }

    @Test
    public void accountInactiveIsReceiversFaultWithName() {
        // Act
        AccountServiceException e = AccountServiceException.ACCOUNT_INACTIVE("bob@x.com");

        // Assert
        assertEquals(AccountServiceException.ACCOUNT_INACTIVE, e.getCode());
        assertTrue(e.isReceiversFault());
        assertTrue(e.getMessage().contains("bob@x.com"));
    }

    @Test
    public void noSuchDomainUsesSharedNoSuchDomainCode() {
        // Act
        AccountServiceException e = AccountServiceException.NO_SUCH_DOMAIN("x.com");

        // Assert — this factory deliberately uses the common Constants code, not a local one
        assertNotNull(e.getCode());
        assertTrue(e.getMessage().contains("no such domain"));
        assertTrue(e.getMessage().contains("x.com"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void invalidPasswordBuildsExpectedMessageAndCode() {
        // Act
        AccountServiceException e = AccountServiceException.INVALID_PASSWORD("too short");

        // Assert
        assertEquals(AccountServiceException.INVALID_PASSWORD, e.getCode());
        assertTrue(e.getMessage().contains("too short"));
        assertTrue(e.getMessage().contains("invalid password"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void domainNotEmptyChainsUnderlyingException() {
        // Arrange
        Exception cause = new IllegalArgumentException("still has accounts");

        // Act
        AccountServiceException e = AccountServiceException.DOMAIN_NOT_EMPTY("x.com", cause);

        // Assert
        assertEquals(AccountServiceException.DOMAIN_NOT_EMPTY, e.getCode());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("x.com"));
    }

    @Test
    public void noSuchMemberListsMembersAndList() {
        // Act
        AccountServiceException e = AccountServiceException.NO_SUCH_MEMBER("dl@x.com", "a@x.com");

        // Assert
        assertEquals(AccountServiceException.NO_SUCH_MEMBER, e.getCode());
        assertTrue(e.getMessage().contains("dl@x.com"));
        assertTrue(e.getMessage().contains("a@x.com"));
    }

    @Test
    public void isInstanceOfServiceExceptionSoGenericHandlersCatchIt() {
        // Act
        AccountServiceException e = AccountServiceException.NO_SUCH_COS("default");

        // Assert — must be catchable as the base type by generic SOAP handling
        assertTrue("AccountServiceException must be a ServiceException",
                e instanceof ServiceException);
    }

    @Test
    public void authFailedFullArgsHidesReasonFromGenericMessage() {
        // Arrange
        Throwable cause = new IllegalStateException("bad creds");

        // Act
        AuthFailedServiceException e = AccountServiceException.AuthFailedServiceException
                .AUTH_FAILED("real@x.com", "typed@x.com", "wrong password", cause);

        // Assert — generic message exposes only the typed-in name, NOT the reason
        assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
        assertTrue("message exposes only the name passed in",
                e.getMessage().contains("typed@x.com"));
        assertFalse("the private reason must NOT leak into the client message",
                e.getMessage().contains("wrong password"));
        assertEquals("but the reason is retrievable for server-side logging",
                "wrong password", e.getReason());
        assertSame(cause, e.getCause());
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void authFailedNullReasonGetReasonReturnsEmptyString() {
        // Act
        AuthFailedServiceException e =
                AccountServiceException.AuthFailedServiceException.AUTH_FAILED("real@x.com", "typed@x.com");

        // Assert — null reason is normalized to "" by getReason()
        assertEquals("", e.getReason());
        assertTrue(e.getMessage().contains("typed@x.com"));
    }

    @Test
    public void authFailedGetReasonWithFormatAppliesFormatWhenReasonPresent() {
        // Act
        AuthFailedServiceException e = AccountServiceException.AuthFailedServiceException
                .AUTH_FAILED("real@x.com", "typed@x.com", "locked out");

        // Assert
        assertEquals("reason: locked out", e.getReason("reason: %s"));
    }

    @Test
    public void authFailedGetReasonWithFormatEmptyWhenReasonNull() {
        // Act
        AuthFailedServiceException e =
                AccountServiceException.AuthFailedServiceException.AUTH_FAILED("real@x.com", "typed@x.com");

        // Assert — format ignored entirely when there is no reason
        assertEquals("", e.getReason("reason: %s"));
    }

    @Test
    public void twoFactorAuthFailedReturnsAuthFailedSubtypeWithReason() {
        // Act
        AuthFailedServiceException e =
                AccountServiceException.TWO_FACTOR_AUTH_FAILED("real@x.com", "typed@x.com", "bad code");

        // Assert
        assertEquals(AccountServiceException.TWO_FACTOR_AUTH_FAILED, e.getCode());
        assertEquals("bad code", e.getReason());
        assertTrue(e.getMessage().contains("typed@x.com"));
    }

    @Test
    public void tooManyAccountsIsReceiversFault() {
        // Act
        AccountServiceException e = AccountServiceException.TOO_MANY_ACCOUNTS("limit=5");

        // Assert
        assertEquals(AccountServiceException.TOO_MANY_ACCOUNTS, e.getCode());
        assertTrue(e.isReceiversFault());
        assertTrue(e.getMessage().contains("limit=5"));
    }

    // ---------- additional CHANGE/RESET/PASSWORD factories ----------

    @Test
    public void changePasswordBuildsSenderFaultMustChangeMessage() {
        AccountServiceException e = AccountServiceException.CHANGE_PASSWORD();

        assertEquals(AccountServiceException.CHANGE_PASSWORD, e.getCode());
        assertTrue(e.getMessage().contains("change password"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void resetPasswordBuildsSenderFaultResetMessage() {
        AccountServiceException e = AccountServiceException.RESET_PASSWORD();

        assertEquals(AccountServiceException.RESET_PASSWORD, e.getCode());
        assertTrue(e.getMessage().contains("reset password"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void passwordLockedBuildsSenderFaultLockedMessage() {
        AccountServiceException e = AccountServiceException.PASSWORD_LOCKED();

        assertEquals(AccountServiceException.PASSWORD_LOCKED, e.getCode());
        assertTrue(e.getMessage().contains("locked"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void passwordChangeTooSoonBuildsExpectedCodeAndMessage() {
        AccountServiceException e = AccountServiceException.PASSWORD_CHANGE_TOO_SOON();

        assertEquals(AccountServiceException.PASSWORD_CHANGE_TOO_SOON, e.getCode());
        assertTrue(e.getMessage().contains("can't be changed yet"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void passwordRecentlyUsedBuildsExpectedCodeAndMessage() {
        AccountServiceException e = AccountServiceException.PASSWORD_RECENTLY_USED();

        assertEquals(AccountServiceException.PASSWORD_RECENTLY_USED, e.getCode());
        assertTrue(e.getMessage().contains("recently used"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void invalidPasswordWithArgumentsCarriesArgumentsAndCode() {
        // Arrange — the varargs overload routes through the protected varargs constructor
        ServiceException.Argument arg =
                new ServiceException.Argument("minLength", "8", ServiceException.Argument.Type.NUM);

        // Act
        AccountServiceException e = AccountServiceException.INVALID_PASSWORD("too short", arg);

        // Assert
        assertEquals(AccountServiceException.INVALID_PASSWORD, e.getCode());
        assertTrue(e.getMessage().contains("too short"));
        assertFalse(e.isReceiversFault());
        assertNotNull("arguments must be retained", e.getArgs());
    }

    // ---------- MULTIPLE_* matched factories ----------

    @Test
    public void multipleAccountsMatchedListsDescription() {
        AccountServiceException e = AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED("uid=joe");

        assertEquals(AccountServiceException.MULTIPLE_ACCOUNTS_MATCHED, e.getCode());
        assertTrue(e.getMessage().contains("uid=joe"));
        assertTrue(e.getMessage().contains("multiple accounts"));
    }

    @Test
    public void multipleDomainsMatchedListsDescription() {
        AccountServiceException e = AccountServiceException.MULTIPLE_DOMAINS_MATCHED("dc=x");

        assertEquals(AccountServiceException.MULTIPLE_DOMAINS_MATCHED, e.getCode());
        assertTrue(e.getMessage().contains("dc=x"));
        assertTrue(e.getMessage().contains("multiple domains"));
    }

    @Test
    public void multipleEntriesMatchedChainsCause() {
        Throwable cause = new IllegalStateException("dup");
        AccountServiceException e = AccountServiceException.MULTIPLE_ENTRIES_MATCHED("filter", cause);

        assertEquals(AccountServiceException.MULTIPLE_ENTRIES_MATCHED, e.getCode());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("filter"));
    }

    // ---------- INVALID_ATTR_*, NO_SMIME_CONFIG ----------

    @Test
    public void invalidAttrNameChainsCauseAndKeepsMessage() {
        Throwable cause = new IllegalArgumentException("bad");
        AccountServiceException e = AccountServiceException.INVALID_ATTR_NAME("attr foo invalid", cause);

        assertEquals(AccountServiceException.INVALID_ATTR_NAME, e.getCode());
        assertSame(cause, e.getCause());
        assertEquals("attr foo invalid", e.getMessage());
    }

    @Test
    public void invalidAttrValueChainsCauseAndKeepsMessage() {
        Throwable cause = new IllegalArgumentException("bad");
        AccountServiceException e = AccountServiceException.INVALID_ATTR_VALUE("value out of range", cause);

        assertEquals(AccountServiceException.INVALID_ATTR_VALUE, e.getCode());
        assertSame(cause, e.getCause());
        assertEquals("value out of range", e.getMessage());
    }

    @Test
    public void noSmimeConfigBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SMIME_CONFIG("user@x.com");

        assertEquals(AccountServiceException.NO_SMIME_CONFIG, e.getCode());
        assertTrue(e.getMessage().contains("user@x.com"));
        assertTrue(e.getMessage().contains("SMIME"));
    }

    // ---------- NO_SUCH_* family ----------

    @Test
    public void noSuchAliasBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_ALIAS("a@x.com");
        assertEquals(AccountServiceException.NO_SUCH_ALIAS, e.getCode());
        assertTrue(e.getMessage().contains("a@x.com"));
    }

    @Test
    public void noSuchOrgUnitBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_ORG_UNIT("people");
        assertEquals(AccountServiceException.NO_SUCH_ORG_UNIT, e.getCode());
        assertTrue(e.getMessage().contains("people"));
    }

    @Test
    public void noSuchShareLocatorBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_SHARE_LOCATOR("loc-1");
        assertEquals(AccountServiceException.NO_SUCH_SHARE_LOCATOR, e.getCode());
        assertTrue(e.getMessage().contains("loc-1"));
    }

    @Test
    public void noSuchGrantBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_GRANT("grant-x");
        assertEquals(AccountServiceException.NO_SUCH_GRANT, e.getCode());
        assertTrue(e.getMessage().contains("grant-x"));
    }

    @Test
    public void noSuchRightBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_RIGHT("viewMail");
        assertEquals(AccountServiceException.NO_SUCH_RIGHT, e.getCode());
        assertTrue(e.getMessage().contains("viewMail"));
    }

    @Test
    public void noSuchServerBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_SERVER("srv1");
        assertEquals(AccountServiceException.NO_SUCH_SERVER, e.getCode());
        assertTrue(e.getMessage().contains("srv1"));
    }

    @Test
    public void noSuchAlwaysonclusterBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_ALWAYSONCLUSTER("cluster1");
        assertEquals(AccountServiceException.NO_SUCH_ALWAYSONCLUSTER, e.getCode());
        assertTrue(e.getMessage().contains("cluster1"));
    }

    @Test
    public void noSuchUcServiceBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_UC_SERVICE("uc1");
        assertEquals(AccountServiceException.NO_SUCH_UC_SERVICE, e.getCode());
        assertTrue(e.getMessage().contains("uc1"));
    }

    @Test
    public void noSuchIdentityBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_IDENTITY("id1");
        assertEquals(AccountServiceException.NO_SUCH_IDENTITY, e.getCode());
        assertTrue(e.getMessage().contains("id1"));
    }

    @Test
    public void noSuchSignatureBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_SIGNATURE("sig1");
        assertEquals(AccountServiceException.NO_SUCH_SIGNATURE, e.getCode());
        assertTrue(e.getMessage().contains("sig1"));
    }

    @Test
    public void noSuchDataSourceBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_DATA_SOURCE("ds1");
        assertEquals(AccountServiceException.NO_SUCH_DATA_SOURCE, e.getCode());
        assertTrue(e.getMessage().contains("ds1"));
    }

    @Test
    public void noSuchZimletBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_ZIMLET("z1");
        assertEquals(AccountServiceException.NO_SUCH_ZIMLET, e.getCode());
        assertTrue(e.getMessage().contains("z1"));
    }

    @Test
    public void noSuchXmppComponentBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_XMPP_COMPONENT("xmpp1");
        assertEquals(AccountServiceException.NO_SUCH_XMPP_COMPONENT, e.getCode());
        assertTrue(e.getMessage().contains("xmpp1"));
    }

    @Test
    public void noSuchDistributionListBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_DISTRIBUTION_LIST("dl@x.com");
        assertEquals(AccountServiceException.NO_SUCH_DISTRIBUTION_LIST, e.getCode());
        assertTrue(e.getMessage().contains("dl@x.com"));
    }

    @Test
    public void noSuchAddressListBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_ADDRESS_LIST("al1");
        assertEquals(AccountServiceException.NO_SUCH_ADDRESS_LIST, e.getCode());
        assertTrue(e.getMessage().contains("al1"));
    }

    @Test
    public void noSuchGroupBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_GROUP("grp@x.com");
        assertEquals(AccountServiceException.NO_SUCH_GROUP, e.getCode());
        assertTrue(e.getMessage().contains("grp@x.com"));
    }

    @Test
    public void noSuchCalendarResourceBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_CALENDAR_RESOURCE("room@x.com");
        assertEquals(AccountServiceException.NO_SUCH_CALENDAR_RESOURCE, e.getCode());
        assertTrue(e.getMessage().contains("room@x.com"));
    }

    @Test
    public void noSuchExternalEntryBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.NO_SUCH_EXTERNAL_ENTRY("ext1");
        assertEquals(AccountServiceException.NO_SUCH_EXTERNAL_ENTRY, e.getCode());
        assertTrue(e.getMessage().contains("ext1"));
    }

    // ---------- *_EXISTS family ----------

    @Test
    public void domainExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.DOMAIN_EXISTS("x.com");
        assertEquals(AccountServiceException.DOMAIN_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("x.com"));
        assertFalse(e.isReceiversFault());
    }

    @Test
    public void cosExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.COS_EXISTS("default");
        assertEquals(AccountServiceException.COS_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("default"));
    }

    @Test
    public void rightExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.RIGHT_EXISTS("viewMail");
        assertEquals(AccountServiceException.RIGHT_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("viewMail"));
    }

    @Test
    public void serverExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.SERVER_EXISTS("srv1");
        assertEquals(AccountServiceException.SERVER_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("srv1"));
    }

    @Test
    public void alwaysonclusterExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.ALWAYSONCLUSTER_EXISTS("cl1");
        assertEquals(AccountServiceException.ALWAYSONCLUSTER_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("cl1"));
    }

    @Test
    public void shareLocatorExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.SHARE_LOCATOR_EXISTS("loc1");
        assertEquals(AccountServiceException.SHARE_LOCATOR_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("loc1"));
    }

    @Test
    public void noShareExistsBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.NO_SHARE_EXISTS();
        assertEquals(AccountServiceException.NO_SHARE_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("no share exists"));
    }

    @Test
    public void zimletExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.ZIMLET_EXISTS("z1");
        assertEquals(AccountServiceException.ZIMLET_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("z1"));
    }

    @Test
    public void distributionListExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.DISTRIBUTION_LIST_EXISTS("dl@x.com");
        assertEquals(AccountServiceException.DISTRIBUTION_LIST_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("dl@x.com"));
    }

    @Test
    public void identityExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.IDENTITY_EXISTS("id1");
        assertEquals(AccountServiceException.IDENTITY_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("id1"));
    }

    @Test
    public void ucServiceExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.UC_SERVICE_EXISTS("uc1");
        assertEquals(AccountServiceException.UC_SERVICE_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("uc1"));
    }

    @Test
    public void signatureExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.SIGNATURE_EXISTS("sig1");
        assertEquals(AccountServiceException.SIGNATURE_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("sig1"));
    }

    @Test
    public void signatureCreationFailureBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.SIGNATURE_CREATION_FAILURE("sig1");
        assertEquals(AccountServiceException.SIGNATURE_CREATION_FAILURE, e.getCode());
        assertTrue(e.getMessage().contains("sig1"));
    }

    @Test
    public void dataSourceExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.DATA_SOURCE_EXISTS("ds1");
        assertEquals(AccountServiceException.DATA_SOURCE_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("ds1"));
    }

    @Test
    public void imComponentExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.IM_COMPONENT_EXISTS("im1");
        assertEquals(AccountServiceException.IM_COMPONENT_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("im1"));
    }

    @Test
    public void aliasExistsBuildsExpectedMessageAndCode() {
        AccountServiceException e = AccountServiceException.ALIAS_EXISTS("alias@x.com");
        assertEquals(AccountServiceException.ALIAS_EXISTS, e.getCode());
        assertTrue(e.getMessage().contains("alias@x.com"));
    }

    // ---------- TOO_MANY_* and two-factor ----------

    @Test
    public void tooManyIdentitiesBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.TOO_MANY_IDENTITIES();
        assertEquals(AccountServiceException.TOO_MANY_IDENTITIES, e.getCode());
        assertTrue(e.getMessage().contains("too many identities"));
    }

    @Test
    public void tooManySignaturesBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.TOO_MANY_SIGNATURES();
        assertEquals(AccountServiceException.TOO_MANY_SIGNATURES, e.getCode());
        assertTrue(e.getMessage().contains("too many signatures"));
    }

    @Test
    public void tooManyZimletuserpropertiesBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.TOO_MANY_ZIMLETUSERPROPERTIES();
        assertEquals(AccountServiceException.TOO_MANY_ZIMLETUSERPROPERTIES, e.getCode());
        assertTrue(e.getMessage().contains("zimlets"));
    }

    @Test
    public void tooManyDataSourcesBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.TOO_MANY_DATA_SOURCES();
        assertEquals(AccountServiceException.TOO_MANY_DATA_SOURCES, e.getCode());
        assertTrue(e.getMessage().contains("too many data sources"));
    }

    @Test
    public void tooManySearchResultsIsReceiversFaultAndChainsCause() {
        Exception cause = new IllegalStateException("overflow");
        AccountServiceException e = AccountServiceException.TOO_MANY_SEARCH_RESULTS("limit=100", cause);
        assertEquals(AccountServiceException.TOO_MANY_SEARCH_RESULTS, e.getCode());
        assertTrue(e.isReceiversFault());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("limit=100"));
    }

    @Test
    public void tooManyTrustedSendersUsesProvidedMessage() {
        AccountServiceException e = AccountServiceException.TOO_MANY_TRUSTED_SENDERS("too many senders");
        assertEquals(AccountServiceException.TOO_MANY_TRUSTED_SENDERS, e.getCode());
        assertEquals("too many senders", e.getMessage());
    }

    @Test
    public void twoFactorSetupRequiredNoArgBuildsDefaultMessage() {
        AccountServiceException e = AccountServiceException.TWO_FACTOR_SETUP_REQUIRED();
        assertEquals(AccountServiceException.TWO_FACTOR_SETUP_REQUIRED, e.getCode());
        assertTrue(e.getMessage().contains("two-factor"));
    }

    @Test
    public void twoFactorSetupRequiredWithMessageUsesProvidedMessage() {
        AccountServiceException e = AccountServiceException.TWO_FACTOR_SETUP_REQUIRED("custom setup msg");
        assertEquals(AccountServiceException.TWO_FACTOR_SETUP_REQUIRED, e.getCode());
        assertEquals("custom setup msg", e.getMessage());
    }

    @Test
    public void invalidTrustedDeviceTokenBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.INVALID_TRUSTED_DEVICE_TOKEN();
        assertEquals(AccountServiceException.INVALID_TRUSTED_DEVICE_TOKEN, e.getCode());
        assertTrue(e.getMessage().contains("trusted device token"));
    }

    @Test
    public void twoFactorAuthRequiredBuildsExpectedCode() {
        AccountServiceException e = AccountServiceException.TWO_FACTOR_AUTH_REQUIRED();
        assertEquals(AccountServiceException.TWO_FACTOR_AUTH_REQUIRED, e.getCode());
        assertTrue(e.getMessage().contains("two-factor"));
    }

    @Test
    public void twoFactorAuthMethodNotAllowedNamesMethod() {
        AccountServiceException e = AccountServiceException.TWO_FACTOR_AUTH_METHOD_NOT_ALLOWED("sms");
        assertEquals(AccountServiceException.TWO_FACTOR_AUTH_METHOD_NOT_ALLOWED, e.getCode());
        assertTrue(e.getMessage().contains("sms"));
        assertTrue(e.getMessage().contains("not allowed"));
    }

    @Test
    public void twoFactorAuthInvalidConfigIsReceiversFault() {
        AccountServiceException e = AccountServiceException.TWO_FACTOR_AUTH_INVALID_CONFIG("bad config");
        assertEquals(AccountServiceException.TWO_FACTOR_AUTH_INVALID_CONFIG, e.getCode());
        assertTrue(e.isReceiversFault());
        assertEquals("bad config", e.getMessage());
    }

    @Test
    public void webClientAccessNotAllowedNamesAccount() {
        AccountServiceException e = AccountServiceException.WEB_CLIENT_ACCESS_NOT_ALLOWED("bob@x.com");
        assertEquals(AccountServiceException.WEB_CLIENT_ACCESS_NOT_ALLOWED, e.getCode());
        assertTrue(e.getMessage().contains("bob@x.com"));
        assertTrue(e.getMessage().contains("not allowed"));
    }

    // ---------- AuthFailedServiceException remaining overloads ----------

    @Test
    public void authFailedNamePassedInReasonCauseUsesNAForRealName() {
        Throwable cause = new IllegalStateException("x");
        AuthFailedServiceException e = AccountServiceException.AuthFailedServiceException
                .AUTH_FAILED("typed@x.com", "the reason", cause);

        assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
        assertEquals("the reason", e.getReason());
        assertTrue(e.getMessage().contains("typed@x.com"));
        assertSame(cause, e.getCause());
    }

    @Test
    public void authFailedReasonAndCauseOnlyEmptyNameInMessage() {
        Throwable cause = new IllegalStateException("x");
        AuthFailedServiceException e =
                AccountServiceException.AuthFailedServiceException.AUTH_FAILED("just a reason", cause);

        assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
        assertEquals("just a reason", e.getReason());
        assertSame(cause, e.getCause());
        assertTrue(e.getMessage().contains("[]"));
    }

    @Test
    public void authFailedReasonOnlyNoCauseEmptyName() {
        AuthFailedServiceException e =
                AccountServiceException.AuthFailedServiceException.AUTH_FAILED("only reason");

        assertEquals(AccountServiceException.AUTH_FAILED, e.getCode());
        assertEquals("only reason", e.getReason());
        assertNull(e.getCause());
        assertTrue(e.getMessage().contains("[]"));
    }
}
