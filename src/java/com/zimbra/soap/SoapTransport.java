/*
 * SoapTransport.java
 */

package com.zimbra.soap;

import com.zimbra.cs.service.Element;
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
    private String mSessionId = null; 

    protected SoapTransport() 
    {
        mSoapProto = SoapProtocol.Soap12;
        mPrettyPrint = false;
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
    
    public String getAuthToken() {
    	return mAuthToken;
    }
    
    public void setSessionId(String sessionId) {
        mSessionId = sessionId;
    }
    public String getSessionId() { return mSessionId; }
    
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

    protected String generateSoapMessage(Element document, boolean raw, boolean noSession, boolean noNotify) {
    	if (raw)
    		return SoapProtocol.toString(document, mPrettyPrint);

        Element context = ZimbraContext.toCtxt(mSoapProto, mAuthToken, noSession, mSessionId, noNotify);
        Element envelope = mSoapProto.soapEnvelope(document, context);
        String soapMessage = SoapProtocol.toString(envelope, getPrettyPrint());
    	return soapMessage;
    }

    public Element parseSoapResponse(String envelopeStr, boolean raw) throws SoapParseException, SoapFaultException {
        Element env;
        try {
            if (envelopeStr.trim().startsWith("<"))
                env = Element.XMLElement.parseText(envelopeStr);
            else
                env = Element.JavaScriptElement.parseText(envelopeStr);
        } catch (DocumentException de) {
            throw new SoapParseException("unable to parse response", envelopeStr);
        }

        if (raw)
            return env;

        SoapProtocol p = SoapProtocol.determineProtocol(env);
//        if (p == null || p != mSoapProto)
        if (p == null)
            throw new SoapParseException("unexpected soap protocol in reply, " +
                                         "got " + p + " expecting " + mSoapProto, envelopeStr);

        Element e = mSoapProto.getBodyElement(env);
        
        if (mSoapProto.isFault(e))
            throw mSoapProto.soapFault(e);

        Element context = mSoapProto.getHeader(env, ZimbraContext.CONTEXT);
        if (context != null) {
            String sid = context.getAttribute(ZimbraContext.E_SESSION_ID, null);
            if (sid != null)
                mSessionId = sid;
        }
        
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
    	return invoke(document, false, false, false);
    }

    /**
     * Sends the specified SOAP envelope.  Envelope is sent as is.  (raw mode)
     * @param envelope
     * @return
     * @throws SoapFaultException
     * @throws IOException
     */
    public final Element invokeRaw(Element envelope) throws SoapFaultException, IOException {
    	return invoke(envelope, true, false, false);
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
    	return invoke(document, false, true, true);
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
    	return invoke(document, false, false, true);
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
    protected abstract Element invoke(Element document, boolean raw, boolean noSession, boolean noNotify) 
    	throws SoapFaultException, IOException;

}

/*
 * TODOs:
 * validation
 */
