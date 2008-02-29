/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * SoapTransport.java
 */
package com.zimbra.common.soap;

import java.util.Map;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import org.dom4j.DocumentException;

import java.io.IOException;

/**
 * Abstract class for sending a soap message.
 */

public abstract class SoapTransport {

    private SoapProtocol mRequestProto;
    private SoapProtocol mResponseProto;
    private boolean mPrettyPrint;
    private ZAuthToken mAuthToken;
    private String mTargetAcctId = null;
    private String mTargetAcctName = null;    
    private String mSessionId = null;
    private String mClientIp = null;
    private long mMaxNotifySeq = -1;
    private Element mContext = null;
    private String mUserAgentName;
    private String mUserAgentVersion;
    private DebugListener mDebugListener;
    
    public interface DebugListener {
        public void sendSoapMessage(Element envelope);
        public void receiveSoapMessage(Element envelope);        
    }

    protected SoapTransport() {
        mRequestProto = SoapProtocol.Soap12;
        mPrettyPrint = false;
    }

    public void setDebugListener(DebugListener listener) {
        mDebugListener = listener;
    }

    public DebugListener getDebugListener() {
        return mDebugListener;
    }
    /**
     * Whether or not to pretty-print XML before sending it.
     *
     * <p> Default value is <code>false</code>.
     */
    public void setPrettyPrint(boolean prettyPrint) {
        this.mPrettyPrint = prettyPrint;
    }

    /**
     * Get the mPrettyPrint value.
     */
    public boolean getPrettyPrint() {
        return mPrettyPrint;
    }

    // AP-TODO-7: retire this?
    public void setAuthToken(String authToken) {
    	mAuthToken = new ZAuthToken(null, authToken, null);
    }
    
    public void setAuthToken(ZAuthToken authToken) {
        mAuthToken = authToken;
    }
    
    public void setTargetAcctId(String acctId) {
        mTargetAcctId = acctId;
    }
    
    
    public String getTargetAcctId() {
        return mTargetAcctId;
    }
    
    public void setTargetAcctName(String acctName) {
        mTargetAcctName = acctName;
    }
    
    /**
     * @return Zimbra context (&lt;context xmlns="urn:zimbra"&gt;) from last invoke, if there was one present.
     */
    public Element getZimbraContext() {
        return mContext;
    }

    public void clearZimbraContext() {
        mContext = null;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }

    public String getSessionId() { return mSessionId; }

    public void setClientIp(String clientIp) {
        mClientIp = clientIp;
    }

    public String getClientIp() { return mClientIp; }
    
    public void setMaxNotifySeq(long seq) {
        mMaxNotifySeq = seq;
    }

    public long getMaxNotifySeq() { return mMaxNotifySeq; }
    
    /**
     * Sets the SOAP client name and version number.
     * @param name the SOAP client name
     * @param version the SOAP client version number, or <code>null</code>
     */
    public void setUserAgent(String name, String
        version) {
        mUserAgentName = name;
        mUserAgentVersion = version;
    }

    /** Sets the version of SOAP to use when generating requests. */
    public void setRequestProtocol(SoapProtocol proto) {
        if (proto != null)
            mRequestProto = proto;
    }

    /** Returns the version of SOAP used to generate requests.  Defaults
     *  to {@link SoapProtocol#Soap12}. */
    public SoapProtocol getRequestProtocol() {
        return mRequestProto;
    }

    /** Sets the version of SOAP we'd like the server to reply with.  <i>(Note
     *  that this only controls XML/JSON serialization; requesting SOAP 1.1
     *  responses to a SOAP 1.2 request will not be honored.)</i> */
    public void setResponseProtocol(SoapProtocol proto) {
        if (proto != null)
            mResponseProto = proto;
    }

    /** Returns the version of SOAP used by the remote server when it generates
     *  its responses.  Defaults to {@link #getRequestProtocol()} .*/
    public SoapProtocol getResponseProtocol() {
        return mResponseProto == null ? mRequestProto : mResponseProto;
    }

    protected String generateSoapMessage(Element document, boolean raw, boolean noSession, String requestedAccountId, String changeToken, String tokenType) {
    	if (raw) {
            if (mDebugListener != null) mDebugListener.sendSoapMessage(document);
    		return SoapProtocol.toString(document, mPrettyPrint);
        }

        // don't use the default protocol version if it's incompatible with the passed-in request
        SoapProtocol proto = mRequestProto;
        if (proto == SoapProtocol.SoapJS) {
            if (document instanceof XMLElement)
                proto = SoapProtocol.Soap12;
        } else {
            if (document instanceof JSONElement)
                proto = SoapProtocol.SoapJS;
        }
        SoapProtocol responseProto = mResponseProto == null ? proto : mResponseProto;

        Element context = SoapUtil.toCtxt(proto, mAuthToken, mTargetAcctId, mTargetAcctName, noSession);
        SoapUtil.addSessionToCtxt(context, mSessionId);
        SoapUtil.addChangeTokenToCtxt(context, changeToken, tokenType);
        if (mUserAgentName != null)
            SoapUtil.addUserAgentToCtxt(context, mUserAgentName, mUserAgentVersion);

        if (requestedAccountId != null)
            context.addElement(HeaderConstants.E_ACCOUNT).addAttribute(HeaderConstants.A_BY, HeaderConstants.BY_ID).setText(requestedAccountId);
        if (mMaxNotifySeq != -1)
            context.addElement(ZimbraNamespace.E_NOTIFY).addAttribute(HeaderConstants.A_SEQNO, mMaxNotifySeq);
        if (responseProto != proto)
            context.addElement(HeaderConstants.E_FORMAT).addAttribute(HeaderConstants.A_TYPE, responseProto == SoapProtocol.SoapJS ? HeaderConstants.TYPE_JAVASCRIPT : HeaderConstants.TYPE_XML);

        Element envelope = proto.soapEnvelope(document, context);
        if (mDebugListener != null) mDebugListener.sendSoapMessage(envelope);

        return SoapProtocol.toString(envelope, getPrettyPrint());
    }

    Element parseSoapResponse(String envelopeStr, boolean raw) throws SoapParseException, SoapFaultException {
        Element env;
        try {
            if (envelopeStr.trim().startsWith("<"))
                env = Element.parseXML(envelopeStr);
            else
                env = Element.parseJSON(envelopeStr);
        } catch (DocumentException de) {
            throw new SoapParseException("unable to parse response", envelopeStr);
        }
        
        if (mDebugListener != null) mDebugListener.receiveSoapMessage(env);

        return raw ? env : extractBodyElement(env);
    }

    
    public Element extractBodyElement(Element env) throws SoapParseException, SoapFaultException {
        SoapProtocol proto = SoapProtocol.determineProtocol(env);
        if (proto == null)
            throw new SoapParseException("cannot determine soap protocol in reply", env.toString());

        Element e = proto.getBodyElement(env);
        if (e == null)
            throw new SoapParseException("malformed soap structure in reply", env.toString());

        mContext = proto.getHeader(env, HeaderConstants.CONTEXT);
        if (mContext != null) {
            String sid = mContext.getAttribute(HeaderConstants.E_SESSION_ID, null);
            if (sid != null)
                mSessionId = sid;
        }

        if (proto.isFault(e)) {
            if (mTargetAcctId != null) {
                proto.updateArgumentsForRemoteFault(e, mTargetAcctId);
            }
            throw proto.soapFault(e);
        } else
            return e;
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p> 
     * Uses the <code>invoke(document, raw, noNotify)</code> method
     * and passes <code>false</code> and <code>false</code>.
     *
     * @throws IOException
     * @throws SoapFaultException
     *
     */
    public final Element invoke(Element document) throws SoapFaultException, IOException {
    	return invoke(document, false, false, null);
    }

    /**
     * Sends the specified SOAP envelope.  Envelope is sent as is.  (raw mode)
     * @param envelope
     * @return
     * @throws SoapFaultException
     * @throws IOException
     */
    public final Element invokeRaw(Element envelope) throws SoapFaultException, IOException {
    	return invoke(envelope, true, false, null);
    }

    /**
     * Sends the specified document as a SOAP message without creating a
     * session on the server.
     * @param document
     * @return
     * @throws SoapFaultException
     * @throws IOException
     */
    public final Element invokeWithoutSession(Element document) throws SoapFaultException, IOException {
    	return invoke(document, false, true, null);
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p /> 
     * 
     * If <code>raw</code> is true, then it expects <code>document</code> to already be 
     * a &lt;soap:Envelope&gt; element, otherwise it wraps it in an envelope/body.
     * 
     * If <tt>noSession</tt> is true, no session object is created/accessed for this request.
     */
    public final Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId) throws SoapFaultException, IOException {
    	return invoke(document, raw, noSession, requestedAccountId, null, null);
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p /> 
     * 
     * If <code>changeToken</code> is non-null, it's used in the soap context to
     * detect modify conflict.
     */
    public abstract Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId, String changeToken, String tokenType) 
    	throws SoapFaultException, IOException;
}

/*
 * TODOs:
 * validation
 */
