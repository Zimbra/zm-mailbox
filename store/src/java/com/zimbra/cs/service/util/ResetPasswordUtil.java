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
package com.zimbra.cs.service.util;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.zimbra.common.account.ForgetPasswordEnums.CodeConstants;
import com.zimbra.common.account.ZAttrProvisioning.FeatureResetPasswordStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ForgetPasswordException;

public class ResetPasswordUtil {

    public static void validateFeatureResetPasswordStatus(Account account) throws ServiceException {
        if (account == null) {
            throw ServiceException.INVALID_REQUEST("account is null.", null);
        }
        switch (account.getFeatureResetPasswordStatus()) {
        case enabled:
            break;
        case suspended:
            Map<String, String> codeMap = JWEUtil.getDecodedJWE(account.getResetPasswordRecoveryCode());
            if (codeMap != null && StringUtils.isNotEmpty(codeMap.get(CodeConstants.SUSPENSION_TIME.toString()))) {
                long suspensionTime = Long.parseLong(codeMap.get(CodeConstants.SUSPENSION_TIME.toString()));
                Date now = new Date();
                if (suspensionTime < now.getTime()) {
                    account.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
                    account.unsetResetPasswordRecoveryCode();
                } else {
                    throw ForgetPasswordException.FEATURE_RESET_PASSWORD_SUSPENDED("Password reset feature is suspended.");
                }
            } else {
                account.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
            }
            break;
        case disabled:
        default:
            throw ForgetPasswordException.FEATURE_RESET_PASSWORD_DISABLED("Password reset feature is disabled.");
        }
    }
}
