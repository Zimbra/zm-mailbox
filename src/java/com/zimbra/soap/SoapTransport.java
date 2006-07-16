/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * SoapTransport.java
 */

package com.zimbra.soap;

import com.zimbra.soap.SoapProtocol;
import com.zimbra.soap.SoapFaultException;

import java.io.IOException;

import org.dom4j.DocumentException;

/**
 * Abstract class for sending a soap message.
 */

public abstract class SoapTransport {

    private SoapProtocol mSoapProto;
    private boolean mPrettyPrint;
    private String mAuthToken;
    private String mTargetAcctId = null;
    private String mTargetAcctName = null;    
    private String mSessionId = null;
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
        mSoapProto = SoapProtocol.Soap12;
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

    public void setAuthToken(String authToken) {
    	mAuthToken = authToken;
    }
    
    public void setTargetAcctId(String acctId) {
        mTargetAcctId = acctId;
    }
    
    public void setTargetAcctName(String acctName) {
        mTargetAcctName = acctName;
    }
    
    public String getAuthToken() {
    	return mAuthToken;
    }
    
    /**
     * @return Zimbra context (&lt;context xmlns="urn:zimbra"&gt;) from last invoke, if there was one present.
     */
    public Element getZimbraContext() {
        return mContext;
    }

    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }
    public String getSessionId() { return mSessionId; }

    public void setMaxNoitfySeq(long seq) {
        mMaxNotifySeq = seq;
    }
    public long getMaxNotifySeq() { return mMaxNotifySeq; }
    
    /**
     * Sets the SOAP client name and version number.
     * @param name the SOAP client name
     * @param version the SOAP client version number, or <code>null</code>
     */
    public void setUserAgent(String name, String version) {
        mUserAgentName = name;
        mUserAgentVersion = version;
    }
    
    /**
     * Which version of Soap to use.
     *
     * <p> Default value is <code>Constants.SOAP_VERSION_12</code>.
     */
    public void setSoapProtocol(SoapProtocol soapProto) {
        this.mSoapProto = soapProto;
    }

    /**
     * Get the soap protocol.
     */
    public SoapProtocol getSoapProtocol() {
        return mSoapProto;
    }

    protected String generateSoapMessage(Element document, boolean raw, boolean noSession, boolean noNotify, String requestedAccountId) {
    	if (raw) {
            if (mDebugListener != null) mDebugListener.sendSoapMessage(document);
    		return SoapProtocol.toString(document, mPrettyPrint);
        }
        
        Element context = ZimbraSoapContext.toCtxt(mSoapProto, mAuthToken, mTargetAcctId, mTargetAcctName, noSession);
        ZimbraSoapContext.addSessionToCtxt(context, mSessionId, noNotify);
        if (mUserAgentName != null) {
            ZimbraSoapContext.addUserAgentToCtxt(context, mUserAgentName, mUserAgentVersion);
        }

        if (requestedAccountId != null) {
            context.addElement(ZimbraSoapContext.E_ACCOUNT).addAttribute(ZimbraSoapContext.A_BY, ZimbraSoapContext.BY_ID).setText(requestedAccountId);
        }
        if (mMaxNotifySeq != -1) {
            context.addElement(ZimbraNamespace.E_NOTIFY).addAttribute(ZimbraSoapContext.A_SEQNO, mMaxNotifySeq); 
        }
        Element envelope = mSoapProto.soapEnvelope(document, context);
        if (mDebugListener != null) mDebugListener.sendSoapMessage(envelope);
        String soapMessage = SoapProtocol.toString(envelope, getPrettyPrint());
    	return soapMessage;
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
    
    Element extractBodyElement(Element env) throws SoapParseException, SoapFaultException {
        SoapProtocol proto = SoapProtocol.determineProtocol(env);
        if (proto == null)
            throw new SoapParseException("cannot determine soap protocol in reply", env.toString());

        Element e = proto.getBodyElement(env);
        if (e == null)
            throw new SoapParseException("malformed soap structure in reply", env.toString());

        mContext = proto.getHeader(env, ZimbraSoapContext.CONTEXT);
        if (mContext != null) {
            String sid = mContext.getAttribute(ZimbraSoapContext.E_SESSION_ID, null);
            if (sid != null)
                mSessionId = sid;
        }

        if (proto.isFault(e))
            throw proto.soapFault(e);
        else
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
    	return invoke(document, false, false, false, null);
    }

    /**
     * Sends the specified SOAP envelope.  Envelope is sent as is.  (raw mode)
     * @param envelope
     * @return
     * @throws SoapFaultException
     * @throws IOException
     */
    public final Element invokeRaw(Element envelope) throws SoapFaultException, IOException {
    	return invoke(envelope, true, false, false, null);
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
    	return invoke(document, false, true, true, null);
    }

    /**
     * Sends the specified document as a SOAP message.  The session created
     * for this request will have notifications turned off.
     * @param document
     * @return
     * @throws SoapFaultException
     * @throws IOException
     */
    public final Element invokeWithoutNotify(Element document) throws SoapFaultException, IOException {
    	return invoke(document, false, false, true, null);
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p /> 
     * 
     * If <code>raw</code> is true, then it expects <code>document</code> to already be 
     * a &lt;soap:Envelope&gt; element, otherwise it wraps it in an envelope/body.
     * 
     * If <code>noNotify</code> is true, response omits change notification block.
     */
    public abstract Element invoke(Element document, boolean raw, boolean noSession, boolean noNotify, String requestedAccountId) 
    	throws SoapFaultException, IOException;

}

/*
 * TODOs:
 * validation
 */
