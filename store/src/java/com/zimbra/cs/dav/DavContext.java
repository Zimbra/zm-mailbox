/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.resource.AddressObject;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.Collection;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.resource.UrlNamespace.UrlComponents;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.FileUploadServlet;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.AccountUtil;

/**
 *
 * @author jylee
 *
 */
public class DavContext {

    // extension that gets called at the end of every requests.
    public interface Extension {
        public void callback(DavContext ctxt);
    }

    private static HashSet<Extension> sExtensions;
    static {
        synchronized (DavContext.class) {
            if (sExtensions == null)
                sExtensions = new HashSet<Extension>();
        }
    }
    public static void addExtension(Extension ex) {
        synchronized (sExtensions) {
            sExtensions.add(ex);
        }
    }
    public static void removeExtension(Extension ex) {
        synchronized (sExtensions) {
            sExtensions.remove(ex);
        }
    }

    private final HttpServletRequest  mReq;
    private final HttpServletResponse mResp;
    private final OperationContext mOpCtxt;
    private final Account mAuthAccount;
    private String mUri;
    private String mUser;
    private String mPath;
    private int mStatus;
    private Document mRequestMsg;
    private FileUploadServlet.Upload mUpload;
    private DavResponse mResponse;
    private boolean mResponseSent;
    private DavResource mRequestedResource;
    private Collection mRequestedParentCollection;
    private RequestType mRequestType;
    private String mCollectionPath;
    private RequestProp mResponseProp;
    private String mDavCompliance;
    /**
     * "actingAsDelegateFor" is used to form part of the scheduling inbox or scheduling outbox URL used
     * for scheduling on behalf of another user (the principal we are acting as a delegate for).
     * e.g.  when actingAsDelegateFor = "user3@example.com",
     *       the associated inbox would be at path /Inbox/user3@example.com/
     */
    private String actingAsDelegateFor = null;
    private boolean mOverwrite;
    private boolean mBrief;

    private enum RequestType { PRINCIPAL, RESOURCE };

    /* List of properties in the PROPFIND or PROPPATCH request. */
    public static class RequestProp {
        boolean nameOnly;
        boolean allProp;
        HashMap<QName, Element> props;
        HashMap<QName, DavException> errProps;

        public RequestProp(boolean no) {
            props = new HashMap<QName, Element>();
            errProps = new HashMap<QName, DavException>();
            nameOnly = no;
            allProp = true;
        }

        public RequestProp(Element top) {
            this(false);

            allProp = false;
            for (Object obj : top.elements()) {
                if (!(obj instanceof Element))
                    continue;
                Element e = (Element) obj;
                String name = e.getName();
                if (name.equals(DavElements.P_ALLPROP))
                    allProp = true;
                else if (name.equals(DavElements.P_PROPNAME))
                    nameOnly = true;
                else if (name.equals(DavElements.P_PROP)) {
                    @SuppressWarnings("unchecked")
                    List<Element> propElems = e.elements();
                    for (Element prop : propElems)
                        props.put(prop.getQName(), prop);
                }
            }
        }

        public RequestProp(java.util.Collection<Element> set, java.util.Collection<QName> remove) {
            this(false);
            allProp = false;
            for (Element e : set)
                props.put(e.getQName(), e);
            for (QName q : remove)
                props.put(q, DocumentHelper.createElement(q));
        }

        public boolean isNameOnly() {
            return nameOnly;
        }
        public boolean isAllProp() {
            return allProp;
        }
        public void addProp(Element p) {
            allProp = false;
            props.put(p.getQName(), p);
        }
        public java.util.Collection<QName> getProps() {
            return props.keySet();
        }
        public Element getProp(QName p) {
            return props.get(p);
        }
        public void addPropError(QName prop, DavException ex) {
            allProp = false;
            if (!props.containsKey(prop))
                props.put(prop, DocumentHelper.createElement(prop));
            errProps.put(prop, ex);
        }
        public Map<QName, DavException> getErrProps() {
            return errProps;
        }
    }
    public RequestProp getRequestProp() throws DavException {
        if (hasRequestMessage()) {
            Document req = getRequestMessage();
            return new RequestProp(req.getRootElement());
        }
        return sEmptyProp;
    }

    public RequestProp getResponseProp() {
        return mResponseProp;
    }

    public void setResponseProp(RequestProp props) {
        mResponseProp = props;
    }

    protected static RequestProp sEmptyProp;

    static {
        sEmptyProp = new RequestProp(false);
    }

    public DavContext(DavContext copy, String path) {
        mReq = copy.mReq;
        mResp = copy.mResp;
        mAuthAccount = copy.mAuthAccount;
        mOpCtxt = copy.mOpCtxt;
        mUser = copy.mUser;
        mPath = path;
    }

    public DavContext(HttpServletRequest req, HttpServletResponse resp, Account authUser) {
        mReq = req;  mResp = resp;
        mUri = req.getPathInfo();
        if (mUri != null && mUri.length() > 1) {
            // Special handling for .ics and .vcf urls
            if (mUri.toLowerCase().endsWith(CalendarObject.CAL_EXTENSION) || mUri.toLowerCase().endsWith(AddressObject.VCARD_EXTENSION)) {
                String rawUri = req.getRequestURI();
                String[] fragments = HttpUtil.getPathFragments(URI.create(rawUri));

                if (!req.getContextPath().isEmpty() && fragments.length > 0 && HttpUtil.urlUnescape(req.getContextPath()).equals("/" + fragments[0])) {
                    // Ignore the first fragment
                    fragments = Arrays.copyOfRange(fragments, 1, fragments.length);
                }
                String servletPath = req.getServletPath();
                if (!servletPath.isEmpty()) {
                    String[] servletPathFragments = HttpUtil.getPathFragments(URI.create(servletPath));
                    for (String servletPathFragment : servletPathFragments) {
                        if (fragments.length > 0 && servletPathFragment.equals(fragments[0])) {
                            fragments = Arrays.copyOfRange(fragments, 1, fragments.length);
                        }
                    }
                }
                // Encode the last fragment again
                fragments[fragments.length - 1] = HttpUtil.urlEscapeIncludingSlash(fragments[fragments.length - 1]);
                URI uri = HttpUtil.getUriFromFragments(fragments, req.getQueryString(), true, false);
                mUri = uri.getPath();
            }

            int index = mUri.indexOf('/', 1);
            if (index > 0) {
                String reqType = mUri.substring(1, index);
                int start = index+1;
                if (reqType.equals("home")) {
                    mRequestType = RequestType.RESOURCE;
                } else if (mUri.startsWith(UrlNamespace.PRINCIPALS_PATH)) {
                    mRequestType = RequestType.PRINCIPAL;
                    start = UrlNamespace.PRINCIPALS_PATH.length();
                }
                index = mUri.indexOf('/', start);
                if (index != -1) {
                    mUser = mUri.substring(start, index);
                    mPath = mUri.substring(index);
                } else {
                    mUser = mUri.substring(start);
                    mPath = "/";
                }
                index = mPath.lastIndexOf('/');
                if (index < mPath.length() - 1 && index > 0)
                    mCollectionPath = mPath.substring(0, index);
            }
        }
        mStatus = HttpServletResponse.SC_OK;
        mAuthAccount = authUser;
        mOpCtxt = new OperationContext(authUser);
        mOpCtxt.setUserAgent(req.getHeader("User-Agent"));
        mDavCompliance = DavProtocol.getDefaultComplianceString();
        String overwrite = mReq.getHeader(DavProtocol.HEADER_OVERWRITE);
        mOverwrite = (overwrite != null && overwrite.equals("F")) ? false : true;
        String brief = mReq.getHeader(DavProtocol.HEADER_BRIEF);
        mBrief = (brief != null && brief.equals("t")) ? true : false;
    }

    /* Returns HttpServletRequest object containing the current DAV request. */
    public HttpServletRequest getRequest() {
        return mReq;
    }

    /* Returns HttpServletResponse object used to return DAV response. */
    public HttpServletResponse getResponse() {
        return mResp;
    }

    public String getFullUrlBeforePath() {
        return new StringBuilder(mReq.getScheme()).append("://")
                .append(mReq.getServerName()).append(':')
                .append(mReq.getServerPort()).toString();
    }

    /* Returns OperationContext used to access Mailbox. */
    public OperationContext getOperationContext() {
        return mOpCtxt;
    }

    /* Returns the authenticated account used to make the current request. */
    public Account getAuthAccount() {
        return mAuthAccount;
    }

    /* Convenience methods used to parse URL to map to DAV resources.
     *
     * Request:
     *
     * http://server:port/service/dav/user1/Notebook/pic1.jpg
     *
     * getUri()  -> /user1/Notebook/pic1.jpg
     * getUser() -> user1
     * getPath() -> /Notebook/pic1.jpg
     * getItem() -> pic1.jpg
     *
     */
    public String getUri() {
        return mUri;
    }

    public String getUser() {
        return mUser;
    }

    public String getPath() {
        return mPath;
    }

    /**
     * @return Last component of the path or "/" if the root.  Can return null
     */
    public String getItem() {
        if (mPath != null) {
            if (mPath.equals("/")) {
                return mPath;
            }
            int index;
            if (mPath.endsWith("/")) {
                int length = mPath.length();
                index = mPath.lastIndexOf('/', length-2);
                if (index != -1) {
                    return mPath.substring(index+1, length-1);
                }
            } else {
                index = mPath.lastIndexOf('/');
                if (index != -1) {
                    return mPath.substring(index+1);
                }
            }
        }
        return null;
    }

    public String getActingAsDelegateFor() {
        return actingAsDelegateFor;
    }
    public void setActingAsDelegateFor(String name) {
        this.actingAsDelegateFor = name;
    }

    public String getCollectionPath() {
        return mCollectionPath;
    }
    public void setCollectionPath(String collPath) {
        mCollectionPath = collPath;
    }

    /* Status is HTTP response code that is set by DAV methods in case of
     * exceptional conditions.
     */
    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int s) {
        mStatus = s;
    }

    /* HttpServletResponse body can be written directly by DAV method handlers,
     * in which case DAV method would tell the framework that the response
     * has been already sent.
     */
    public void responseSent() {
        mResponseSent = true;
    }

    public boolean isResponseSent() {
        return mResponseSent;
    }

    /* Depth header - RFC 2518bis section 10.2 */
    public enum Depth {
        zero, one, infinity
    }

    public Depth getDepth() {
        String hdr = mReq.getHeader(DavProtocol.HEADER_DEPTH);
        if (hdr == null)
            return Depth.zero;
        if (hdr.equals("0"))
            return Depth.zero;
        if (hdr.equals("1"))
            return Depth.one;
        if (hdr.equalsIgnoreCase("infinity"))
            return Depth.infinity;

        ZimbraLog.dav.info("invalid depth: "+hdr);
        return Depth.zero;
    }

    /* Returns true if the DAV request contains a message. */
    public boolean hasRequestMessage() {
        try {
            String ct = getUpload().getContentType();
            return getUpload().getSize() > 0 && ct != null && (ct.startsWith(DavProtocol.XML_CONTENT_TYPE) || ct.startsWith(DavProtocol.XML_CONTENT_TYPE2));
        } catch (Exception e) {
        }
        return false;
    }

    public FileUploadServlet.Upload getUpload() throws DavException, IOException {
        if (mUpload == null) {
            String name = null;
            String ctype = getRequest().getContentType();
            if (ctype == null)
                name = getItem();
            try {
                mUpload = FileUploadServlet.saveUpload(mReq.getInputStream(), name, ctype, mAuthAccount.getId(), true);
                ZimbraLog.dav.debug("Request: requested content-type: %s, actual content-type: %s", ctype, mUpload.getContentType());
            } catch (ServiceException se) {
                throw new DavException("can't save upload", HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, se);
            }
        }
        return mUpload;
    }

    public void cleanup() {
        if (sExtensions.size() > 0)
            for (Extension ex : sExtensions)
                ex.callback(this);
        if (mUpload != null)
            FileUploadServlet.deleteUpload(mUpload);
        mUpload = null;
    }

    /* Returns XML Document containing the request. */
    public Document getRequestMessage() throws DavException {
        if (mRequestMsg != null)
            return mRequestMsg;
        try {
            if (hasRequestMessage()) {
                mRequestMsg = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(getUpload().getInputStream());
                return mRequestMsg;
            }
        } catch (XmlParseException e) {
            throw new DavException("unable to parse request message", HttpServletResponse.SC_BAD_REQUEST, e);
        } catch (IOException e) {
            throw new DavException("can't read uploaded file", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
        throw new DavException("no request msg", HttpServletResponse.SC_BAD_REQUEST, null);
    }

    /* Returns true if there is a response message generated by DAV method handler. */
    public boolean hasResponseMessage() {
        return mResponse != null;
    }

    /* Returns DavResponse */
    public DavResponse getDavResponse() {
        if (mResponse == null)
            mResponse = new DavResponse();
        return mResponse;
    }

    public DavResource getRequestedResource() throws DavException, ServiceException {
        if (mRequestedResource == null) {
            if (mRequestType == RequestType.RESOURCE)
                mRequestedResource = UrlNamespace.getResourceAt(this, mUser, mPath);
            else
                mRequestedResource = UrlNamespace.getPrincipalAtUrl(this, mUri);
            if (mRequestedResource != null)
                ZimbraLog.addToContext(ZimbraLog.C_NAME, mRequestedResource.getOwner());
        }
        if (mRequestedResource == null)
            throw new DavException("no DAV resource at "+mUri, HttpServletResponse.SC_NOT_FOUND, null);
        return mRequestedResource;
    }

    public Collection getRequestedParentCollection() throws DavException, ServiceException {
        if (mPath != null && mRequestedParentCollection == null)
            mRequestedParentCollection = UrlNamespace.getCollectionAtUrl(this, mPath);
        return mRequestedParentCollection;
    }

    public java.util.Collection<DavResource> getAllRequestedResources() throws DavException, ServiceException {
        ArrayList<DavResource> rss = new ArrayList<DavResource>();
        if (mRequestType == RequestType.RESOURCE)
            rss = (ArrayList<DavResource>) UrlNamespace.getResources(this, mUser, mPath, getDepth() == Depth.one);
        else {
            DavResource rs = UrlNamespace.getPrincipalAtUrl(this, mUri);
            if (rs != null) {
                ZimbraLog.addToContext(ZimbraLog.C_NAME, rs.getOwner());
                rss.add(rs);
                if (getDepth() == Depth.one)
                    rss.addAll(rs.getChildren(this));
            }
        }
        if (rss.isEmpty())
            throw new DavException("no DAV resource at "+mUri, HttpServletResponse.SC_NOT_FOUND, null);
        return rss;
    }

    public Mailbox getTargetMailbox() throws ServiceException {
        Account acct = Provisioning.getInstance().getAccountByName(mUser);
        if (acct == null)
            return null;
        return MailboxManager.getInstance().getMailboxByAccount(acct);
    }

    private static final String EVOLUTION = "Evolution";
    private static final String ICAL = "iCal/";
    private static final String IPHONE = "iPhone/";
    private static final String ADDRESSBOOK = "Address";
    private static final String MICROSOFT = "Microsoft";
    private static final String MSIE = "MSIE";
    private static final String MOZILLA = "Mozilla";

    private boolean userAgentHeaderContains(String str) {
        String userAgent = mReq.getHeader(DavProtocol.HEADER_USER_AGENT);
        if (userAgent == null)
            return false;
        return userAgent.indexOf(str) >= 0;
    }

    public boolean isAddressbookClient() {
        return userAgentHeaderContains(ADDRESSBOOK);
    }

    public boolean isIcalClient() {
        return userAgentHeaderContains(ICAL);
    }

    public boolean isMsft() {
        return userAgentHeaderContains(MICROSOFT);
    }

    public boolean isWebRequest() {
        return userAgentHeaderContains(MSIE) || userAgentHeaderContains(MOZILLA);
    }

    public static enum KnownUserAgent {
        iCal, iPhone, Evolution;

        static KnownUserAgent lookup(String userAgent) {
            if (userAgent != null) {
                if (userAgent.indexOf(IPHONE) >= 0)
                    return iPhone;
                else if (userAgent.indexOf(ICAL) >= 0)
                    return iCal;
                else if (userAgent.indexOf(EVOLUTION) >= 0)
                    return Evolution;
            }
            return null;
        }
    }

    public KnownUserAgent getKnownUserAgent() {
        String userAgent = mReq.getHeader(DavProtocol.HEADER_USER_AGENT);
        return KnownUserAgent.lookup(userAgent);
    }

    public boolean isFreebusyEnabled() {
        try {
            return !Provisioning.getInstance().getConfig().getBooleanAttr(Provisioning.A_zimbraCalendarCalDavDisableFreebusy, false);
        } catch (ServiceException se) {
            return false;
        }
    }

    public boolean isGzipAccepted() {
        @SuppressWarnings("rawtypes")
        Enumeration acceptEncHdrs = mReq.getHeaders(DavProtocol.HEADER_ACCEPT_ENCODING);
        while (acceptEncHdrs.hasMoreElements()) {
            String acceptEnc = (String) acceptEncHdrs.nextElement();
            // Not the most rigorous check, but it works.
            if (acceptEnc != null && acceptEnc.toLowerCase().contains(DavProtocol.ENCODING_GZIP))
                return true;
        }
        return false;
    }

    public void setDavCompliance(String comp) {
        mDavCompliance = comp;
    }

    public String getDavCompliance() {
        return mDavCompliance;
    }

    public boolean useIcalDelegation() {
        if (mAuthAccount != null)
            return mAuthAccount.isPrefAppleIcalDelegationEnabled();
        return false;
    }

    public boolean isOverwriteSet() {
        return mOverwrite;
    }

    public boolean isBrief() {
        return mBrief;
    }

    public Collection getDestinationCollection() throws DavException {
        String destinationUrl = getDestinationUrl();
        if (!destinationUrl.endsWith("/")) {
            int slash = destinationUrl.lastIndexOf('/');
            destinationUrl = destinationUrl.substring(0, slash+1);
        }
        try {
            destinationUrl = HttpUtil.urlUnescape(destinationUrl);
            destinationUrl = getInternalDestinationUrl(destinationUrl);
            DavResource r = UrlNamespace.getResourceAtUrl(this, destinationUrl);
            if (r instanceof Collection)
                return ((Collection)r);
            return UrlNamespace.getCollectionAtUrl(this, destinationUrl);
        } catch (Exception e) {
            throw new DavException("can't get destination collection", DavProtocol.STATUS_FAILED_DEPENDENCY);
        }
    }

    private String getDestinationUrl() throws DavException {
        String destination = getRequest().getHeader(DavProtocol.HEADER_DESTINATION);
        if (destination == null)
            throw new DavException("no destination specified", HttpServletResponse.SC_BAD_REQUEST, null);
        return destination;
    }

    private String getInternalDestinationUrl(String destinationUrl) throws ServiceException, DavException {
        UrlComponents uc = UrlNamespace.parseUrl(destinationUrl);
        Account targetAcct = Provisioning.getInstance().getAccountByName(uc.user);
        if (targetAcct == null)
            return destinationUrl;
        ZMailbox zmbx = getZMailbox(targetAcct);
        ItemId targetRoot = new ItemId("", Mailbox.ID_FOLDER_USER_ROOT);
        Pair<ZFolder, String> match = zmbx.getFolderByPathLongestMatch(targetRoot.toString(), uc.path);
        ZFolder targetFolder = match.getFirst();
        if (targetFolder instanceof ZMountpoint) {
            ZMountpoint zmp = (ZMountpoint) targetFolder;
            ItemId target = new ItemId(zmp.getOwnerId(), Integer.parseInt(zmp.getRemoteId()));
            Account acct = Provisioning.getInstance().getAccountById(zmp.getOwnerId());
            ZMailbox targetZmbx = getZMailbox(acct);
            ZFolder f = targetZmbx.getFolderById(target.toString());
            String extraPath = match.getSecond();
            destinationUrl = DavServlet.DAV_PATH + "/" + acct.getName() + f.getPath() + ((extraPath != null) ? "/" + extraPath : "");
        }
        return destinationUrl;
    }


    public ZMailbox getZMailbox(Account acct) throws ServiceException {
        AuthToken authToken = AuthProvider.getAuthToken(getAuthAccount());
        ZMailbox.Options zoptions = new ZMailbox.Options(authToken.toZAuthToken(), AccountUtil.getSoapUri(acct));
        zoptions.setNoSession(true);
        zoptions.setTargetAccount(acct.getId());
        zoptions.setTargetAccountBy(Key.AccountBy.id);
        ZMailbox zmbx = ZMailbox.getMailbox(zoptions);
        if (zmbx != null) {
            zmbx.setName(acct.getName()); /* need this when logging in using another user's auth */
        }
        return zmbx;
    }

    public String getNewName() throws DavException {
        String oldName = getItem();
        String dest = getDestinationUrl();
        int begin, end;
        end = dest.length();
        if (dest.endsWith("/"))
            end--;
        begin = dest.lastIndexOf("/", end-1);
        String newName = dest.substring(begin+1, end);
        try {
            newName = URLDecoder.decode(newName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.dav.warn("can't decode URL ", dest, e);
        }
        if (oldName.equals(newName) == false)
            return newName;
        else
            return null;
    }
}
