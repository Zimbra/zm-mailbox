/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Future;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;

/**
 * Abstract class for sending a soap message.
 * <p>
 * TODO: validation
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
    private String csrfToken;
    private String trustedToken;
    private boolean voidOnExpired = false;
    private boolean isAdmin = false;
    private String originalUserAgent;
    public static final String DEFAULT_USER_AGENT_NAME = "ZCS";
    private static String sDefaultUserAgentName = DEFAULT_USER_AGENT_NAME;
    private static String sDefaultUserAgentVersion;
    private static final ViaHolder viaHolder = new ViaHolder();
    public static enum NotificationFormat {DEFAULT, IMAP};
    // This needs to be a LinkedList and not a Deque, to support older Android
    // devices that run JDK 1.5.
    private static final class ViaHolder extends ThreadLocal<LinkedList<String>> {
        @Override
        protected LinkedList<String> initialValue() {
            return new LinkedList<String>();
        }
    }

    public interface DebugListener {
        void sendSoapMessage(Element envelope);
        void receiveSoapMessage(Element envelope);
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

    public ZAuthToken getAuthToken() {
        return mAuthToken;
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

    public String getTargetAcctName() {
    	return mTargetAcctName;
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
    public void setUserAgent(String name, String version) {
        mUserAgentName = name;
        mUserAgentVersion = version;
    }

    /**
     * Sets the default SOAP client name and version.  These global value are
     * used when the instance-level values are not specified.
     */
    public static void setDefaultUserAgent(String defaultName, String defaultVersion) {
        sDefaultUserAgentName = defaultName;
        if ("unknown".equals(defaultVersion)) {
            sDefaultUserAgentVersion = null;
        } else {
            sDefaultUserAgentVersion = defaultVersion;
        }
    }

    public String getUserAgentName() {
        if (mUserAgentName != null) {
            return mUserAgentName;
        } else {
            return sDefaultUserAgentName;
        }
    }

    public String getUserAgentVersion() {
        if (mUserAgentVersion != null) {
            return mUserAgentVersion;
        } else {
            return sDefaultUserAgentVersion;
        }
    }

    /**
     * @return the csrfToken
     */
    public String getCsrfToken() {
        return csrfToken;
    }

    /**
     * @param csrfToken the csrfToken to set
     */
    public void setCsrfToken(String csrfToken) {
        this.csrfToken = csrfToken;
    }

    public String getTrustedToken() {
        return trustedToken;
    }

    public void setTrustedToken(String trustedToken) {
        this.trustedToken = trustedToken;
    }

    /**
     * Sets a {@code via} header value to the current thread context.
     * <p>
     * This is intended to be called by the SOAP engine if the current thread is
     * a SOAP handler. All subsequent SOAP requests, i.e. proxy requests will
     * include this {@code via} header, so that we can track the proxy chain.
     * Since a SOAP handler may dispatch another SOAP handler recursively in
     * the same thread, this is internally maintained by a stack.
     *
     * @param value {@code via} header value
     */
    public static void setVia(String value) {
        viaHolder.get().addFirst(value);
    }

    /**
     * Removes the {@code via} header from the current thread context.
     *
     * @see #setVia(String)
     */
    public static void clearVia() {
        viaHolder.get().removeFirst();
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

    /**
     * Return true if Zimbra-specific context header should be added
     * @see com.zimbra.cs.offline.util.ymail.YMailClient
     */
    public boolean generateContextHeader() {
        return true;
    }

    protected Element generateSoapMessage(Element document, boolean raw, boolean noSession,
            String requestedAccountId, String changeToken, String tokenType) {
            return generateSoapMessage(document, raw, noSession, requestedAccountId, changeToken, tokenType, null, null);
    }

    protected Element generateSoapMessage(Element document, boolean raw, boolean noSession,
            String requestedAccountId, String changeToken, String tokenType, NotificationFormat nFormat, String curWaitSetID) {

        if (raw) {
            if (mDebugListener != null) {
                mDebugListener.sendSoapMessage(document);
            }
            return document;
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

        String targetId = requestedAccountId != null ? requestedAccountId : mTargetAcctId;
        String targetName = targetId == null ? mTargetAcctName : null;

        Element context = null;
        if (generateContextHeader()) {
            context = SoapUtil.toCtxt(proto, mAuthToken, this.csrfToken);
            if (noSession) {
                SoapUtil.disableNotificationOnCtxt(context);
            } else {
                SoapUtil.addSessionToCtxt(context, mAuthToken == null ? null : mSessionId, mMaxNotifySeq, nFormat, curWaitSetID);
            }
            SoapUtil.addTargetAccountToCtxt(context, targetId, targetName);
            SoapUtil.addChangeTokenToCtxt(context, changeToken, tokenType);
            SoapUtil.addUserAgentToCtxt(context, getUserAgentName(), getUserAgentVersion());
            SoapUtil.addAuthTokenControl(context, voidOnExpired);
            if (responseProto != proto) {
                SoapUtil.addResponseProtocolToCtxt(context, responseProto);
            }

            String via = viaHolder.get().peek();
            if (via != null) {
                context.addUniqueElement(HeaderConstants.E_VIA).setText(via);
            }
        }
        Element envelope = proto.soapEnvelope(document, context);
        if (mDebugListener != null) {
            mDebugListener.sendSoapMessage(envelope);
        }
        return envelope;
    }

    protected Element parseSoapResponse(String envelopeStr, boolean raw) throws SoapParseException, SoapFaultException {
        Element env;
        try {
            if (envelopeStr.trim().startsWith("<"))
                env = Element.parseXML(envelopeStr);
            else
                env = Element.parseJSON(envelopeStr);
        } catch (XmlParseException e) {
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
            String sid = mContext.getAttribute(HeaderConstants.E_SESSION, null);
            // be backwards-compatible for sanity-preservation purposes
            if (sid == null) {
                mContext.getAttribute(HeaderConstants.E_SESSION_ID, null);
            }
            if (sid != null) {
                mSessionId = sid;
            }
        }

        if (proto.isFault(e)) {
            if (mTargetAcctId != null)
                proto.updateArgumentsForRemoteFault(e, mTargetAcctId);
            throw proto.soapFault(e);
        } else
            return e;
    }

    public abstract Future<HttpResponse> invokeAsync(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, NotificationFormat nFormat, String curWaitSetID, FutureCallback<HttpResponse> cb) throws IOException;

    public final Future<HttpResponse> invokeAsync(Element document, boolean raw, boolean noSession, String requestedAccountId,
            String changeToken, String tokenType, FutureCallback<HttpResponse> cb) throws IOException {
        return invokeAsync(document, raw, noSession, requestedAccountId,
                changeToken, tokenType, null, null, cb);
    }

    public final Future<HttpResponse> invokeAsync(Element document, FutureCallback<HttpResponse> cb) throws IOException {
        return invokeAsync(document, false, false, null, null, null, cb);
    }

    public final Future<HttpResponse> invokeWithoutSessionAsync(Element document, FutureCallback<HttpResponse> cb) throws IOException {
        return invokeAsync(document, false, true, null, null, null, cb);
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p>
     * Uses the <code>invoke(document, raw, noNotify)</code> method
     * and passes <code>false</code> and <code>false</code>.
     *
     * @throws IOException
     * @throws ServiceException
     *
     */
    public final Element invoke(Element document) throws IOException, ServiceException {
        return invoke(document, false, false, null);
    }

    /**
     * Sends the specified SOAP envelope.  Envelope is sent as is.  (raw mode)
     * @param envelope
     * @return
     * @throws IOException
     * @throws ServiceException 
     */
    public final Element invokeRaw(Element envelope) throws IOException, ServiceException {
        return invoke(envelope, true, false, null);
    }

    /**
     * Sends the specified document as a SOAP message without creating a
     * session on the server.
     * @param document
     * @return
     * @throws IOException
     * @throws ServiceException
     */
    public final Element invokeWithoutSession(Element document) throws IOException, ServiceException {
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
     * @throws ServiceException
     * @throws IOException  
     */
    public final Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId) throws IOException, ServiceException{
        return invoke(document, raw, noSession, requestedAccountId, null, null);
    }

    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p />
     *
     * If <code>raw</code> is true, then it expects <code>document</code> to already be
     * a &lt;soap:Envelope&gt; element, otherwise it wraps it in an envelope/body.
     *
     * <tt>nFormat</tt> indicates the format for notifications in response header (IMAP or DEFAULT).
     *
     * <tt>noSession</tt> is assumed to be false.
     * @throws ServiceException
     */
    public final Element invoke(Element document, boolean raw, String requestedAccountId, NotificationFormat nFormat, String curWaitSetID) throws IOException, ServiceException {
        return invoke(document, raw, false, requestedAccountId, null, null, nFormat, curWaitSetID);
    }

    public abstract Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId, String changeToken,
            String tokenType, NotificationFormat nFormat, String curWaitSetID) throws IOException, ServiceException;
    /**
     * Sends the specified document as a Soap message
     * and parses the response as a Soap message. <p />
     *
     * If <code>changeToken</code> is non-null, it's used in the soap context to
     * detect modify conflict.
     */
    public abstract Element invoke(Element document, boolean raw, boolean noSession, String requestedAccountId, String changeToken, String tokenType)
        throws ServiceException, IOException;

    /**
     * Sets the number of milliseconds to wait when reading data during a invoke
     * call.
     * <p>
     * This implementation has no effect. Subclasses may override this method.
     *
     * @param ms read timeout
     */
    public void setTimeout(int ms) {
        // do nothing by default
    }

    /**
     * Controls authTokenControl SOAP header
     * @return
     */
    public boolean voidOnExpired() {
        return voidOnExpired;
    }

    /**
     * Controls authTokenControl SOAP header
     * @param mVoidOnExpired
     */
    public void setVoidOnExpired(boolean mVoidOnExpired) {
        this.voidOnExpired = mVoidOnExpired;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getOriginalUserAgent() {
        return originalUserAgent;
    }

    public void setOriginalUserAgent(String originalUserAgent) {
        this.originalUserAgent = originalUserAgent;
    }

}
