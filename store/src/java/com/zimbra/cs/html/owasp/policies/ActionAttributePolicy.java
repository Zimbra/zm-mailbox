package com.zimbra.cs.html.owasp.policies;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
import java.net.MalformedURLException;
import java.net.URL;

import org.owasp.html.AttributePolicy;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.ZThreadLocal;

public class ActionAttributePolicy implements AttributePolicy {

    /** enable same host post request for a form in email */
    private static boolean sameHostFormPostCheck = DebugConfig.defang_block_form_same_host_post_req;
    /** The Host header received in the request. */
    private String reqVirtualHost = null;

    public ActionAttributePolicy() {
        if (ZThreadLocal.getRequestContext() != null) {
            this.reqVirtualHost = ZThreadLocal.getRequestContext().getVirtualHost();
        }
    }

    @Override
    public String apply(String elementName, String attributeName, String value) {
        if (sameHostFormPostCheck && reqVirtualHost != null) {
            try {
                URL url = new URL(value);
                String formActionHost = url.getHost().toLowerCase();

                if (formActionHost.equalsIgnoreCase(reqVirtualHost)) {
                    value = value.replace(formActionHost, "SAMEHOSTFORMPOST-BLOCKED");
                }
            } catch (MalformedURLException e) {
                ZimbraLog.soap.warn("Failure while trying to block malicious code. Check for URL"
                    + " match between the host and the action URL of a FORM."
                    + " Error parsing URL, possible relative URL." + e.getMessage());
            }
        }
        return value;
    }

}
