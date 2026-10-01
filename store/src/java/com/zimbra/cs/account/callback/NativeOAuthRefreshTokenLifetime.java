/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Synacor, Inc.
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

import com.zimbra.common.util.Constants;
import java.math.BigInteger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

public class NativeOAuthRefreshTokenLifetime extends AttributeCallback{

    private static final Pattern WITH_UNIT = Pattern.compile("^(\\d+)(ms|[dhms])$",Pattern.CASE_INSENSITIVE);
    static final long MIN_MILLIS = Constants.MILLIS_PER_DAY;
    static final long MAX_MILLIS = 365L * Constants.MILLIS_PER_DAY;

    @SuppressWarnings("rawtypes")
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry) throws ServiceException {
        SingleValueMod mod = singleValueMod(attrName, attrValue);
        if (mod.setting()) {
            validate(attrName, mod.value());
        }
    }
    static void validate(String attrName, String value) throws ServiceException {
        if (value != null) {
            value = value.trim();
        }
        if ("0".equals(value) || "0d".equals(value)) {
            return;
        }
        Matcher m = WITH_UNIT.matcher(value);
        if (!m.matches()) {
            throw AccountServiceException.INVALID_ATTR_VALUE(attrName + " must be 0/0d or a non-negative "
                    + "duration with a unit suffix (d, h, m, s, ms), e.g. 30d; got '" + value + "'", null);
        }
        BigInteger millis = new BigInteger(m.group(1)).multiply(BigInteger.valueOf(unitMillis(m.group(2).toLowerCase())));
        if (millis.compareTo(BigInteger.valueOf(MIN_MILLIS)) < 0
                || millis.compareTo(BigInteger.valueOf(MAX_MILLIS)) > 0) {
            throw AccountServiceException.INVALID_ATTR_VALUE(attrName
                    + " must be between 1d and 365d (or 0/0d to disable); got '" + value + "'", null);
        }
    }
    private static long unitMillis(String unit) {
        switch (unit) {
            case "d": return Constants.MILLIS_PER_DAY;
            case "h": return Constants.MILLIS_PER_HOUR;
            case "m": return Constants.MILLIS_PER_MINUTE;
            case "s": return Constants.MILLIS_PER_SECOND;
            default: return 1L; // ms
        }
    }
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
