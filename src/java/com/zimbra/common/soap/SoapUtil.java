/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import java.util.Arrays;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;

final class SoapUtil {

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
     * @return A new {@code contex} Element in the appropriate markup
     */
    static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken) {
        Element ctxt = protocol.getFactory().createElement(HeaderConstants.CONTEXT);
        if (authToken != null)
            authToken.encodeSoapCtxt(ctxt);
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
    static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, String sessionId, int sequence) {
        Element ctxt = toCtxt(protocol, authToken);
        return addSessionToCtxt(ctxt, authToken == null ? null : sessionId, sequence);
    }

    static Element disableNotificationOnCtxt(Element ctxt) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

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
    static Element addSessionToCtxt(Element ctxt, String sessionId, long sequence) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        Element eSession = ctxt.addUniqueElement(HeaderConstants.E_SESSION);
        if (sessionId != null && !sessionId.trim().equals("")) {
            // be backwards-compatible for sanity-preservation purposes
            for (Element elt : Arrays.asList(eSession, ctxt.addUniqueElement("sessionId"))) {
                elt.addAttribute(HeaderConstants.A_ID, sessionId);
                if (sequence > 0)
                    elt.addAttribute(HeaderConstants.A_SEQNO, sequence);
            }
        }
        return ctxt;
    }

    static Element addTargetAccountToCtxt(Element ctxt, String targetAccountId, String targetAccountName) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        String by = targetAccountId != null ? HeaderConstants.BY_ID : HeaderConstants.BY_NAME;
        String target = targetAccountId != null ? targetAccountId : targetAccountName;

        if (target != null && !target.trim().equals(""))
            ctxt.addUniqueElement(HeaderConstants.E_ACCOUNT).addAttribute(HeaderConstants.A_BY, by).setText(target);
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
    static Element addUserAgentToCtxt(Element ctxt, String name, String version) {
        if (ctxt == null || Strings.isNullOrEmpty(name))
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        Element eUserAgent = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        eUserAgent.addAttribute(HeaderConstants.A_NAME, name);
        if (!Strings.isNullOrEmpty(version))
            eUserAgent.addAttribute(HeaderConstants.A_VERSION, version);
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
    static Element addChangeTokenToCtxt(Element ctxt, String token, String type) {
        if (ctxt == null || Strings.isNullOrEmpty(token))
            return ctxt;
        if (!ctxt.getName().equalsIgnoreCase(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        Element eChange = ctxt.addUniqueElement(HeaderConstants.E_CHANGE);
        eChange.addAttribute(HeaderConstants.A_CHANGE_ID, token);
        if (!Strings.isNullOrEmpty(type))
            eChange.addAttribute(HeaderConstants.A_TYPE, type);
        return ctxt;
    }

    static Element addResponseProtocolToCtxt(Element ctxt, SoapProtocol proto) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        if (proto != null) {
            ctxt.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE,
                    proto == SoapProtocol.SoapJS ? HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        }
        return ctxt;
    }
}
