/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

import java.util.Map;

public class TwoFactorAuthEmailCode extends AttributeCallback {
    public static final int MAX_CODE_LENGTH = 10;

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry) throws ServiceException {
        String value = (String) attrValue;
        if (StringUtil.isNullOrEmpty(value)) {
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength + " cannot set to empty.", null);
        }
        validateAttributeValue(attrName, Integer.valueOf(value) );
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {

    }

    private void validateAttributeValue(String attrName, int attrValue) throws ServiceException {
        if (attrValue > MAX_CODE_LENGTH) {
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength + " cannot set above " + MAX_CODE_LENGTH, null);
        }
        if ((attrValue == Provisioning.getInstance().getConfig().getTwoFactorCodeLength()) ||
                (attrValue == Provisioning.getInstance().getConfig().getTwoFactorScratchCodeLength())) {
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraTwoFactorAuthEmailCodeLength + " must be different from zimbraTwoFactorCodeLength and zimbraTwoFactorScratchCodeLength", null);
        }
    }
}