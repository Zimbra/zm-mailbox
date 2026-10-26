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
public class NativeOAuthAccessTokenLifetime extends AttributeCallback  {
    private static final Pattern WITH_UNIT = Pattern.compile("^(\\d+)(ms|[hms])$", Pattern.CASE_INSENSITIVE);
    static final long MIN_MILLIS = 5L * Constants.MILLIS_PER_MINUTE;
    static final long MAX_MILLIS = 24L * Constants.MILLIS_PER_HOUR;
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
        Matcher m = WITH_UNIT.matcher(value);
        if (!m.matches()) {
            throw AccountServiceException.INVALID_ATTR_VALUE(
                    attrName + " must be a positive duration with unit suffix h, m, s, or ms (e.g. 1h, 30m, 3600s). " + "Days ('d') and unitless values are not allowed; got '" + value + "'",
                    null);
        }
        BigInteger durationValue = new BigInteger(m.group(1));
        if (durationValue.compareTo(BigInteger.ZERO) <= 0) {
            throw AccountServiceException.INVALID_ATTR_VALUE(attrName + " must be greater than 0; got '" + value + "'",
                    null);
        }
        BigInteger millis = durationValue.multiply(BigInteger.valueOf(unitMillis(m.group(2).toLowerCase())));
        if (millis.compareTo(BigInteger.valueOf(MIN_MILLIS)) < 0
                || millis.compareTo(BigInteger.valueOf(MAX_MILLIS)) > 0) {
            throw AccountServiceException.INVALID_ATTR_VALUE(attrName
                    + " must be between 5m and 24h; got '" + value + "'", null);
        }
    }
    private static long unitMillis(String unit) {
        switch (unit) {
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


