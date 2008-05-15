/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import java.util.Arrays;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.util.StringUtil;

public class SoapUtil {
    /** Creates a SOAP request <tt>&lt;context></tt> {@link com.zimbra.common.soap.Element}.<p>
     *
     *  All requests except Auth and a few others must specify an auth token.
     *  You must also call {@link #addSessionToCtxt(Element, String)} if you
     *  want change notification; the default is not to create a session.
     *
     * @param protocol   The markup to use when creating the <tt>context</tt>.
     * @param authToken  The authorization token for the user.
     * @return A new <tt>context</tt> Element in the appropriate markup. */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken) {
        Element ctxt = protocol.getFactory().createElement(HeaderConstants.CONTEXT);
        if (authToken != null)
            authToken.encodeSoapCtxt(ctxt);
        return ctxt;
    }
    
    /** Creates a SOAP request <tt>&lt;context></tt> {@link com.zimbra.common.soap.Element} with
     *  notification requested.  (All requests except Auth and a few others must
     *  specify an auth token.)
     *
     * @param protocol   The markup to use when creating the <tt>context</tt>.
     * @param authToken  The serialized authorization token for the user.
     * @param sessionId  The ID of the session to add to the <tt>context</tt>.
     * @return A new <tt>context</tt> Element in the appropriate markup.
     * @see #toCtxt(com.zimbra.common.soap.SoapProtocol, String, boolean) */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, String sessionId, int sequence) {
        Element ctxt = toCtxt(protocol, authToken);
        return addSessionToCtxt(ctxt, authToken == null ? null : sessionId, sequence);
    }

    public static Element disableNotificationOnCtxt(Element ctxt) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        return ctxt;
    }

    /** Adds session information to a <tt>&lt;context></tt> {@link com.zimbra.common.soap.Element}
     *  created by a call to {@link #toCtxt(SoapProtocol, ZAuthToken)}.  By default, the server creates
     *  a session for the client unless a valid sessionId is specified referring
     *  to an existing session.<p>
     *  
     *  No changes to the context occur if no auth token is present.
     * @param ctxt       A <tt>&lt;context></tt> Element as created by toCtxt.
     * @param sessionId  The ID of the session to add to the <tt>&lt;context></tt>
     * @return The passed-in <tt>&lt;context></tt> Element.
     * @see #toCtxt */
    public static Element addSessionToCtxt(Element ctxt, String sessionId, long sequence) {
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

    public static Element addTargetAccountToCtxt(Element ctxt, String targetAccountId, String targetAccountName) {
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

    /** Adds user agent information to a <tt>&lt;context></tt> {@link com.zimbra.common.soap.Element}
     *  created by a call to {@link #toCtxt}.
     *
     * @param ctxt       A <tt>&lt;context></tt> Element as created by toCtxt.
     * @param name       The name of the client application
     * @param version    The version number of the client application
     *
     * @return The passed-in <tt>&lt;context></tt> Element.
     * @throws IllegalArgumentException if the given Element's name is not <tt>context</tt>
     * @see #toCtxt(SoapProtocol, ZAuthToken) */
    public static Element addUserAgentToCtxt(Element ctxt, String name, String version) {
        if (ctxt == null || StringUtil.isNullOrEmpty(name))
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        Element eUserAgent = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        eUserAgent.addAttribute(HeaderConstants.A_NAME, name);
        if (!StringUtil.isNullOrEmpty(version))
            eUserAgent.addAttribute(HeaderConstants.A_VERSION, version);
        return ctxt;
    }

    /** Adds change token to <tt>&lt;context></tt> {@link com.zimbra.common.soap.Element}
     * 
     * @param ctxt       A <tt>&lt;context></tt> Element as created by toCtxt.
     * @param token      Change number to check for modify conflict.
     * @param type       "mod" or "new".  Refer to soap.txt for the use.
     * @return The passed-in <tt>&lt;context></tt> Element.
     * @throws IllegalArgumentException if the given Element's name is not <tt>context</tt>
     * @see #toCtxt(SoapProtocol, ZAuthToken) */
    public static Element addChangeTokenToCtxt(Element ctxt, String token, String type) {
    	if (ctxt == null || StringUtil.isNullOrEmpty(token))
    		return ctxt;
    	if (!ctxt.getName().equalsIgnoreCase(HeaderConstants.E_CONTEXT))
    		throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

    	Element eChange = ctxt.addUniqueElement(HeaderConstants.E_CHANGE);
    	eChange.addAttribute(HeaderConstants.A_CHANGE_ID, token);
    	if (!StringUtil.isNullOrEmpty(type))
    		eChange.addAttribute(HeaderConstants.A_TYPE, type);
    	return ctxt;
    }

    public static Element addResponseProtocolToCtxt(Element ctxt, SoapProtocol proto) {
        if (ctxt == null)
            return ctxt;
        if (!ctxt.getName().equals(HeaderConstants.E_CONTEXT))
            throw new IllegalArgumentException("Invalid element: " + ctxt.getName());

        if (proto != null)
            ctxt.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE, proto == SoapProtocol.SoapJS ? HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);
        return ctxt;
    }
}
