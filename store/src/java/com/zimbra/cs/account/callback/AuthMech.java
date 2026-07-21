/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.auth.AuthMechanism;

public class AuthMech extends AttributeCallback {

    private static final String FALLBACK_PREFIX = "fallback:";

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
            throws ServiceException {

        String authMech;

        SingleValueMod mod = singleValueMod(attrName, attrValue);
        if (mod.setting()) {
            authMech = mod.value();

            boolean valid = false;

            if (authMech == null) {
                valid = true;
            } else if (authMech.startsWith(AuthMechanism.AuthMech.custom.name())) {
                valid = true;
            } else {
                // allow an optional "fallback:<zimbra|ad|kerberos5> " prefix in front of the
                // real auth mech value, e.g. "fallback:zimbra custom:idp-ropc arg1 arg2".
                String mechToValidate = authMech;

                if (mechToValidate.startsWith(FALLBACK_PREFIX)) {
                    int spaceIdx = mechToValidate.indexOf(' ');
                    if (spaceIdx > 0) {
                        String fallbackMech = mechToValidate.substring(FALLBACK_PREFIX.length(), spaceIdx);
                        // fallback to another custom auth mech is not supported
                        if (fallbackMech.startsWith(AuthMechanism.AuthMech.custom.name())) {
                            ZimbraLog.account.error("fallback to custom auth not supported: " + fallbackMech);
                            throw ServiceException.INVALID_REQUEST(
                                    "invalid value: " + authMech + " — fallback to custom auth not supported", null);
                        }
                        // fallback mech must be one of the standard AuthMech values
                        try {
                            AuthMechanism.AuthMech.fromString(fallbackMech);
                        } catch (ServiceException e) {
                            ZimbraLog.account.error("invalid fallback auth mech: " + fallbackMech, e);
                            throw ServiceException.INVALID_REQUEST("invalid value: " + authMech, null);
                        }

                        // strip "fallback:<mech> " off, leaving the real mech (e.g. "custom:idp-ropc ...")
                        mechToValidate = mechToValidate.substring(spaceIdx + 1).trim();
                    } else {
                        ZimbraLog.account.error(
                                "invalid auth mech config: no mechanism after fallback prefix: " + authMech);
                        throw ServiceException.INVALID_REQUEST("invalid value: " + authMech, null);
                    }
                }
                if (mechToValidate.startsWith(AuthMechanism.AuthMech.custom.name())) {
                    valid = true;
                } else {
                    try {
                        AuthMechanism.AuthMech mech = AuthMechanism.AuthMech.fromString(mechToValidate);
                        valid = true;
                    } catch (ServiceException e) {
                        ZimbraLog.account.error("invalid auth mech", e);
                    }
                }
            }

            if (!valid) {
                throw ServiceException.INVALID_REQUEST("invalid value: " + authMech, null);
            }
        }

    }


    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
