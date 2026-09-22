/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link ForgetPasswordException} static factory methods.
 * Each factory must produce a {@link ForgetPasswordException}, attach the right
 * service code, embed the supplied message, and be flagged as the sender's fault.
 */
public class ForgetPasswordExceptionTest {

    private void assertSenderFaultWithCode(ServiceException ex, String expectedCode, String message) {
        // Assert — concrete type, code, message, and fault attribution.
        assertTrue("must be a ForgetPasswordException", ex instanceof ForgetPasswordException);
        assertEquals(expectedCode, ex.getCode());
        assertTrue("message must embed the supplied text", ex.getMessage().contains(message));
        assertTrue("message must carry the service-exception prefix",
                ex.getMessage().contains("service exception:"));
        assertFalse("ForgetPasswordException is always sender's fault", ex.isReceiversFault());
    }

    @Test
    public void recoveryEmailSameAsPrimaryOrAliasWithMessageBuildsSenderFault() {
        // Arrange
        String message = "recovery equals primary";

        // Act
        ServiceException ex = ForgetPasswordException.RECOVERY_EMAIL_SAME_AS_PRIMARY_OR_ALIAS(message);

        // Assert
        assertSenderFaultWithCode(ex, "service.RECOVERY_EMAIL_SAME_AS_PRIMARY_OR_ALIAS", message);
    }

    @Test
    public void codeAlreadySentWithMessageBuildsSenderFault() {
        String message = "already sent";
        ServiceException ex = ForgetPasswordException.CODE_ALREADY_SENT(message);
        assertSenderFaultWithCode(ex, "service.CODE_ALREADY_SENT", message);
    }

    @Test
    public void maxAttemptsReachedWithMessageBuildsSenderFault() {
        String message = "too many attempts";
        ServiceException ex = ForgetPasswordException.MAX_ATTEMPTS_REACHED(message);
        assertSenderFaultWithCode(ex, "service.MAX_ATTEMPTS_REACHED", message);
    }

    @Test
    public void maxAttemptsReachedSuspendFeatureWithMessageBuildsSenderFault() {
        String message = "suspend feature";
        ServiceException ex = ForgetPasswordException.MAX_ATTEMPTS_REACHED_SUSPEND_FEATURE(message);
        assertSenderFaultWithCode(ex, "service.MAX_ATTEMPTS_REACHED_SUSPEND_FEATURE", message);
    }

    @Test
    public void codeNotFoundWithMessageBuildsSenderFault() {
        String message = "no code";
        ServiceException ex = ForgetPasswordException.CODE_NOT_FOUND(message);
        assertSenderFaultWithCode(ex, "service.CODE_NOT_FOUND", message);
    }

    @Test
    public void codeMismatchWithMessageBuildsSenderFault() {
        String message = "mismatch";
        ServiceException ex = ForgetPasswordException.CODE_MISMATCH(message);
        assertSenderFaultWithCode(ex, "service.CODE_MISMATCH", message);
    }

    @Test
    public void codeExpiredWithMessageBuildsSenderFault() {
        String message = "expired";
        ServiceException ex = ForgetPasswordException.CODE_EXPIRED(message);
        assertSenderFaultWithCode(ex, "service.CODE_EXPIRED", message);
    }

    @Test
    public void contactAdminWithMessageBuildsSenderFault() {
        String message = "contact admin";
        ServiceException ex = ForgetPasswordException.CONTACT_ADMIN(message);
        assertSenderFaultWithCode(ex, "service.CONTACT_ADMIN", message);
    }

    @Test
    public void featureResetPasswordSuspendedWithMessageBuildsSenderFault() {
        String message = "suspended";
        ServiceException ex = ForgetPasswordException.FEATURE_RESET_PASSWORD_SUSPENDED(message);
        assertSenderFaultWithCode(ex, "service.FEATURE_RESET_PASSWORD_SUSPENDED", message);
    }

    @Test
    public void featureResetPasswordDisabledWithMessageBuildsSenderFault() {
        String message = "disabled";
        ServiceException ex = ForgetPasswordException.FEATURE_RESET_PASSWORD_DISABLED(message);
        assertSenderFaultWithCode(ex, "service.FEATURE_RESET_PASSWORD_DISABLED", message);
    }

    @Test
    public void factoriesEmptyMessageStillProduceWellFormedException() {
        // Arrange — boundary: empty message must not break code/fault attribution.
        ServiceException ex = ForgetPasswordException.CODE_EXPIRED("");

        // Assert
        assertEquals("service.CODE_EXPIRED", ex.getCode());
        assertTrue(ex.getMessage().contains("service exception:"));
        assertFalse(ex.isReceiversFault());
    }

    @Test
    public void distinctFactoriesProduceDistinctCodes() {
        // Arrange / Act
        ServiceException a = ForgetPasswordException.CODE_MISMATCH("x");
        ServiceException b = ForgetPasswordException.CODE_EXPIRED("x");

        // Assert — different factories map to different codes on identical input.
        assertFalse("codes must differ across factories", a.getCode().equals(b.getCode()));
    }
}
