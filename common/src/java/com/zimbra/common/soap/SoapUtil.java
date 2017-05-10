/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

import java.util.Arrays;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.SoapTransport.NotificationFormat;

public final class SoapUtil {

    private SoapUtil() {
    }

    /**
     * Creates a SOAP request {@code <context>} {@link com.zimbra.common.soap.Element}.
     * <p>
     * All requests except Auth and a few others must specify an auth token. You
     * must also call {@link #addSessionToCtxt(Element, String)} if you want
     * change notification; the default is not to create a session.
     *
     * @param protocol The markup to use when creating the {@code context}
     * @param authToken The authorization token for the user
     * @param csrfToken
     * @return A new {@code contex} Element in the appropriate markup
     */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken) {
        Element ctxt = protocol.getFactory().createElement(HeaderConstants.CONTEXT);
        if (authToken != null) {
            authToken.encodeSoapCtxt(ctxt);
        }
        return ctxt;
    }

    /**
     * Creates a SOAP request {@code <context>} {@link com.zimbra.common.soap.Element}.
     * <p>
     * All requests except Auth and a few others must specify an auth token. You
     * must also call {@link #addSessionToCtxt(Element, String)} if you want
     * change notification; the default is not to create a session.
     *
     * @param protocol The markup to use when creating the {@code context}
     * @param authToken The authorization token for the user
     * @param csrfToken
     * @return A new {@code contex} Element in the appropriate markup
     */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, String csrfToken) {
        Element ctxt = protocol.getFactory().createElement(HeaderConstants.CONTEXT);
        if (authToken != null) {
            authToken.encodeSoapCtxt(ctxt);
        }
        if (csrfToken != null) {
           Element csrfElmnt = ctxt.addElement(HeaderConstants.E_CSRFTOKEN);
           csrfElmnt.addText(csrfToken);
        }
        return ctxt;
    }

    /**
     * Creates a SOAP request {@code <context>} {@link com.zimbra.common.soap.Element}
     * with notification requested.
     * <p>
     * All requests except Auth and a few others must specify an auth token.
     *
     * @param protocol The markup to use when creating the {@code context}
     * @param authToken The serialized authorization token for the user
     * @param sessionId The ID of the session to add to the {@code context}
     * @return A new {@code context} Element in the appropriate markup
     * @see #toCtxt(com.zimbra.common.soap.SoapProtocol, String, boolean)
     */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, String sessionId, int sequence) {
        Element ctxt = toCtxt(protocol, authToken, null);
        return addSessionToCtxt(ctxt, authToken == null ? null : sessionId, sequence);
    }

    public static Element disableNotificationOnCtxt(Element ctxt) {
        if (ctxt == null) {
            return ctxt;
        }
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        return ctxt;
    }

    /**
     * Adds session information to a {@code <context>} {@link com.zimbra.common.soap.Element}
     * created by a call to {@link #toCtxt(SoapProtocol, ZAuthToken)}.
     * <p>
     * By default, the server creates a session for the client unless a valid
     * sessionId is specified referring to an existing session.
     * <p>
     * No changes to the context occur if no auth token is present.
     *
     * @param ctxt A {@code <context>} Element as created by toCtxt
     * @param sessionId The ID of the session to add to the {@code <context>}
     * @return The passed-in {@code <context>} Element.
     * @see #toCtxt
     */
    public static Element addSessionToCtxt(Element ctxt, String sessionId, long sequence) {
        return addSessionToCtxt(ctxt, sessionId, sequence, null);
    }
    public static Element addSessionToCtxt(Element ctxt, String sessionId, long sequence, NotificationFormat nFormat) {
        if (ctxt == null) {
            return ctxt;
        }
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        Element eSession = ctxt.addUniqueElement(HeaderConstants.E_SESSION);
        if (sessionId != null && !sessionId.trim().equals("")) {
            // be backwards-compatible for sanity-preservation purposes
            for (Element elt : Arrays.asList(eSession, ctxt.addUniqueElement(HeaderConstants.E_SESSION_ID))) {
                elt.addAttribute(HeaderConstants.A_ID, sessionId);
                if (sequence > 0) {
                    elt.addAttribute(HeaderConstants.A_SEQNO, sequence);
                }
            }
        }
        if(nFormat != null) {
            eSession.addAttribute(HeaderConstants.E_FORMAT, nFormat.toString());
        }
        return ctxt;
    }

    public static Element addTargetAccountToCtxt(Element ctxt, String targetAccountId, String targetAccountName) {
        if (ctxt == null) {
            return ctxt;
        }
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        String by = targetAccountId != null ? HeaderConstants.BY_ID : HeaderConstants.BY_NAME;
        String target = targetAccountId != null ? targetAccountId : targetAccountName;

        if (target != null && !target.trim().equals("")) {
            ctxt.addUniqueElement(HeaderConstants.E_ACCOUNT).addAttribute(HeaderConstants.A_BY, by).setText(target);
        }
        return ctxt;
    }

    /**
     * Adds user agent information to a {@code <context>} {@link com.zimbra.common.soap.Element}
     * created by a call to {@link #toCtxt}.
     *
     * @param ctxt A {@code <context>} Element as created by toCtxt
     * @param name  The name of the client application
     * @param version The version number of the client application
     *
     * @return The passed-in {@code <context>} Element
     * @throws IllegalArgumentException if the given Element's name is not {@code context}
     * @see #toCtxt(SoapProtocol, ZAuthToken)
     */
    public static Element addUserAgentToCtxt(Element ctxt, String name, String version) {
        if (ctxt == null || Strings.isNullOrEmpty(name)) {
            return ctxt;
        }
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        Element eUserAgent = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        eUserAgent.addAttribute(HeaderConstants.A_NAME, name);
        if (!Strings.isNullOrEmpty(version)) {
            eUserAgent.addAttribute(HeaderConstants.A_VERSION, version);
        }
        return ctxt;
    }

    /**
     * Adds change token to {@code <context>} {@link com.zimbra.common.soap.Element}.
     *
     * @param ctxt A {@code <context>} Element as created by toCtxt
     * @param token Change number to check for modify conflict
     * @param type "mod" or "new".  Refer to soap.txt for the use.
     * @return The passed-in {@code <context>} Element
     * @throws IllegalArgumentException if the given Element's name is not {@code context}
     * @see #toCtxt(SoapProtocol, ZAuthToken)
     */
    public static Element addChangeTokenToCtxt(Element ctxt, String token, String type) {
        if (ctxt == null || Strings.isNullOrEmpty(token)) {
            return ctxt;
        }
        if (!ctxt.getName().equalsIgnoreCase(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        Element eChange = ctxt.addUniqueElement(HeaderConstants.E_CHANGE);
        eChange.addAttribute(HeaderConstants.A_CHANGE_ID, token);
        if (!Strings.isNullOrEmpty(type)) {
            eChange.addAttribute(HeaderConstants.A_TYPE, type);
        }
        return ctxt;
    }

    /**
     * Add <authTokenControl voidOnExpired="1|0"/> element to <context> element
     * @param ctxt A {@code <context>} Element as created by toCtxt
     * @param voidOnExpired boolean
     * @return
     */
    public static Element addAuthTokenControl(Element ctxt, boolean voidOnExpired) {
        if (ctxt == null) {
            return ctxt;
        }
        if (!ctxt.getName().equalsIgnoreCase(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        Element eChange = ctxt.addUniqueElement(HeaderConstants.E_AUTH_TOKEN_CONTROL);
        eChange.addAttribute(HeaderConstants.A_VOID_ON_EXPIRED, voidOnExpired);
        return ctxt;
    }

    public static Element addResponseProtocolToCtxt(Element ctxt, SoapProtocol proto) {
        if (ctxt == null) {
            return ctxt;
        }
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());
        }
        if (proto != null) {
            ctxt.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE,
                    proto == SoapProtocol.SoapJS ? HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        }
        return ctxt;
    }
}
