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

import java.util.Map;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.util.StringUtil;

public class SoapUtil {
    /** Creates a SOAP request <code>&lt;context></code> {@link com.zimbra.common.soap.Element}.<p>
     *
     *  All requests except Auth and a few others must specify an auth token.
     *  If noSession is true, the server will not create a session and any
     *  sessionId specified will be ignored.
     *
     * @param protocol   The markup to use when creating the <code>context</code>.
     * @param authToken  The serialized authorization token for the user.
     * @param noSession  Whether to suppress the default new session creation.
     * @return A new <code>context</code> Element in the appropriate markup. */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, 
            String targetAccountId, String targetAccountName, boolean noSession) {
        Element ctxt = toCtxt(protocol, authToken, noSession);

        if (targetAccountId != null || targetAccountName != null) {
            Element acctElt = ctxt.addUniqueElement(HeaderConstants.E_ACCOUNT);
            acctElt.addAttribute(HeaderConstants.A_BY, targetAccountId != null ? HeaderConstants.BY_ID : HeaderConstants.BY_NAME);
            acctElt.setText(targetAccountId != null ? targetAccountId : targetAccountName);
        }

        return ctxt;
    }

    /** Creates a SOAP request <code>&lt;context></code> {@link com.zimbra.common.soap.Element}.<p>
     *
     *  All requests except Auth and a few others must specify an auth token.
     *  If noSession is true, the server will not create a session and any
     *  sessionId specified will be ignored.
     *
     * @param protocol   The markup to use when creating the <code>context</code>.
     * @param authToken  The serialized authorization token for the user.
     * @param noSession  Whether to suppress the default new session creation.
     * @return A new <code>context</code> Element in the appropriate markup. */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, boolean noSession) {
        Element ctxt = protocol.getFactory().createElement(HeaderConstants.CONTEXT);
        
        if (authToken != null)
            authToken.encodeSoapCtxt(ctxt);

        if (noSession)
            ctxt.addUniqueElement(HeaderConstants.E_NO_SESSION);
        return ctxt;
    }

    /** Adds session information to a <code>&lt;context></code> {@link com.zimbra.common.soap.Element}
     *  created by a call to {@link #toCtxt}.  By default, the server creates
     *  a session for the client unless a valid sessionId is specified referring
     *  to an existing session.<p>
     *  
     *  No changes to the context occur if no auth token is present.
     * @param ctxt       A <code>&lt;context></code> Element as created by toCtxt.
     * @param sessionId  The ID of the session to add to the <code>&lt;context></code>
     * @return The passed-in <code>&lt;context></code> Element.
     * @see #toCtxt */
    public static Element addSessionToCtxt(Element ctxt, String sessionId) {
        if (ctxt == null || sessionId == null || sessionId.trim().equals(""))
            return ctxt;
        ctxt.addUniqueElement(HeaderConstants.E_SESSION_ID).addAttribute(HeaderConstants.A_ID, sessionId).setText(sessionId);
        return ctxt;
    }

    /** Adds user agent information to a <code>&lt;context></code> {@link com.zimbra.common.soap.Element}
     *  created by a call to {@link #toCtxt}.
     *
     * @param ctxt       A <code>&lt;context></code> Element as created by toCtxt.
     * @param name       The name of the client application
     * @param version    The version number of the client application
     *
     * @return The passed-in <code>&lt;context></code> Element.
     * @throws IllegalArgumentException if the given Element's name is not <code>context</code>
     * @see #toCtxt */
    public static Element addUserAgentToCtxt(Element ctxt, String name, String version) {
        if (StringUtil.isNullOrEmpty(name))
            return ctxt;
        String elementName = ctxt.getName();
        if (!elementName.equalsIgnoreCase(HeaderConstants.E_CONTEXT)) {
            throw new IllegalArgumentException("Invalid element: " + elementName);
        }

        Element eUserAgent = ctxt.addUniqueElement(HeaderConstants.E_USER_AGENT);
        eUserAgent.addAttribute(HeaderConstants.A_NAME, name).setText(name);
        if (!StringUtil.isNullOrEmpty(version)) {
            eUserAgent.addAttribute(HeaderConstants.A_VERSION, version).setText(version);
        }
        return ctxt;
    }

    /**
     * Adds change token to <code>&lt;context></code> {@link com.zimbra.common.soap.Element}
     * @param ctxt       A <code>&lt;context></code> Element as created by toCtxt.
     * @param token      Change number to check for modify conflict.
     * @param type       "mod" or "new".  Refer to soap.txt for the use.
     * @return The passed-in <code>&lt;context></code> Element.
     * @throws IllegalArgumentException if the given Element's name is not <code>context</code>
     * @see #toCtxt
     */
    public static Element addChangeTokenToCtxt(Element ctxt, String token, String type) {
    	if (StringUtil.isNullOrEmpty(token))
    		return ctxt;
    	String elementName = ctxt.getName();
    	if (!elementName.equalsIgnoreCase(HeaderConstants.E_CONTEXT))
    		throw new IllegalArgumentException("Invalid element: " + elementName);

    	Element eChange = ctxt.addUniqueElement(HeaderConstants.E_CHANGE);
    	eChange.addAttribute(HeaderConstants.A_CHANGE_ID, token);
    	if (!StringUtil.isNullOrEmpty(type))
    		eChange.addAttribute(HeaderConstants.A_TYPE, type);
    	return ctxt;
    }
    
    /** Creates a SOAP request <code>&lt;context></code> {@link com.zimbra.common.soap.Element} with
     *  an associated session.  (All requests except Auth and a few others must
     *  specify an auth token.)
     *
     * @param protocol   The markup to use when creating the <code>context</code>.
     * @param authToken  The serialized authorization token for the user.
     * @param sessionId  The ID of the session to add to the <code>context</code>.
     * @return A new <code>context</code> Element in the appropriate markup.
     * @see #toCtxt(com.zimbra.common.soap.SoapProtocol, String, boolean) */
    public static Element toCtxt(SoapProtocol protocol, ZAuthToken authToken, String sessionId) {
        Element ctxt = toCtxt(protocol, authToken, false);
        return authToken == null ? ctxt : addSessionToCtxt(ctxt, sessionId);
    }
}
