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

package com.zimbra.cs.account.auth.ropc.webhook;

import com.zimbra.common.util.ZimbraLog;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class OktaWebhookHandler implements IRopcWebhookHandler {

    @Override
    public String getProviderName() {
        return "okta";
    }

    @Override
    public String extractUsername(String rawPayload) {
        try {
            JSONObject json = new JSONObject(rawPayload);
            JSONArray events = json.getJSONObject("data").getJSONArray("events");
            // parse the events array
            for (int i = 0; i < events.length(); i++) {
                JSONObject event = events.getJSONObject(i);
                String eventType = event.getString("eventType");

                // Target the specific password change event
                if ("user.account.update_password".equals(eventType)) {
                    JSONArray targets = event.getJSONArray("target");
                    for (int j = 0; j < targets.length(); j++) {
                        JSONObject target = targets.getJSONObject(j);

                        if ("User".equals(target.getString("type"))) {
                            return target.getString("alternateId");
                        }
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.account.error("Failed to extract username from okta payload", e);
        }
        return null;
    }

    @Override
    public void handleVerificationChallenge(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String challenge = request.getHeader("X-Okta-Verification-Challenge");

        if (challenge != null) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);

            PrintWriter out = response.getWriter();
            out.print("{\"verification\":\"" + challenge + "\"}");
            out.flush();
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
