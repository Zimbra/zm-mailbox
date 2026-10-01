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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.auth.AuthMechanism;
import com.zimbra.cs.account.auth.twofactor.AuthMechConstants;
import java.util.Map;

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

        if (!context.isCreate() && entry instanceof Domain) {
            String previousAuthMech = entry.getAttr(attrName);
            String newAuthMech = mod.setting() ? mod.value() : null;
            if (shouldTriggerRopcDomainCleanup(previousAuthMech, newAuthMech)) {
                // Capture current timestamp at time of auth mech change
                // This timestamp will be used to only clear MFA sessions created BEFORE this time
                long changeTimestamp = System.currentTimeMillis();
                context.setData(CallbackContext.DataKey.PREV_AUTH_MECH, previousAuthMech);
                context.setData(CallbackContext.DataKey.NEW_AUTH_MECH, newAuthMech);
                context.setData(CallbackContext.DataKey.AUTH_MECH_CHANGE_TIMESTAMP, String.valueOf(changeTimestamp));
                context.setData(CallbackContext.DataKey.ROPC_AUTH_MECH_TRANSITION, Boolean.TRUE.toString());
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        if (!(entry instanceof Domain)) {
            return;
        }
        if (!Boolean.parseBoolean(context.getData(CallbackContext.DataKey.ROPC_AUTH_MECH_TRANSITION))) {
            return;
        }
        if (context.isDoneAndSetIfNot(AuthMech.class)) {
            return;
        }

        Domain domain = (Domain) entry;
        try {
            triggerSessionClear(domain.getName(),
                    context.getData(CallbackContext.DataKey.AUTH_MECH_CHANGE_TIMESTAMP));
        } catch (Exception e) {
            ZimbraLog.account.warn("Failed to purge IdP ROPC data for domain %s after %s change", domain.getName(),
                    attrName, e);
        }
    }

    private void triggerSessionClear(String domain, String changeTimestamp) {
        try {
            ZimbraLog.account.info("Authentication mechanism changed for domain %s. Clearing MFA sessions.",
                    domain);
            ThirdPartyMFASessionClearanceService.clearSessionsForDomain(domain, changeTimestamp);
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to clear MFA sessions after auth mech change for domain %s",
                    domain, e);
        }
    }

    static boolean shouldTriggerRopcDomainCleanup(String previousAuthMech, String newAuthMech) {
        return usesIdpRopc(previousAuthMech) != usesIdpRopc(newAuthMech);
    }

    static boolean usesIdpRopc(String authMech) {
        return authMech != null && authMech.toLowerCase().contains(AuthMechConstants.IDP_ROPC);
    }

}
