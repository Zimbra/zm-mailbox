/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.dav.service.method;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.account.Key;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavContext.Depth;
import com.zimbra.cs.dav.DavContext.RequestProp;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.property.Acl.Ace;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.MailItemResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavResponse;
import com.zimbra.soap.type.GalSearchType;

public class AclReports extends Report {

    @Override
    public void handle(DavContext ctxt) throws DavException, ServiceException {
        ctxt.setStatus(DavProtocol.STATUS_MULTI_STATUS);
        Element query = ctxt.getRequestMessage().getRootElement();
        if (query.getQName().equals(DavElements.E_PRINCIPAL_PROPERTY_SEARCH))
            handlePrincipalPropertySearch(ctxt, query);
        else if (query.getQName().equals(DavElements.E_ACL_PRINCIPAL_PROP_SET))
            handleAclPrincipalPropSet(ctxt, query);
        else if (query.getQName().equals(DavElements.E_PRINCIPAL_MATCH))
            handlePrincipalMatch(ctxt, query);
        else if (query.getQName().equals(DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET))
            handlePrincipalSearchPropertySet(ctxt, query);
        else
            throw new DavException("msg "+query.getName()+" is not an ACL report", HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * http://tools.ietf.org/html/rfc3744#section-9.4. DAV:principal-property-search REPORT
     * Preconditions:  None
     * Postconditions: DAV:number-of-matches-within-limits
     */
    private void handlePrincipalPropertySearch(DavContext ctxt, Element query) throws DavException, ServiceException {
        RequestProp reqProp = ctxt.getRequestProp();
        DavResponse resp = ctxt.getDavResponse();
        // http://tools.ietf.org/html/rfc3744#section-9.4
        // The response body for a successful request MUST be a DAV:multistatus XML element.  In the case where there
        // are no response elements, the returned multistatus XML element is empty.
        resp.getTop(DavElements.E_MULTISTATUS);
        for (DavResource rs : getMatchingResources(ctxt, query)) {
            resp.addResource(ctxt, rs, reqProp, false);
        }
    }

    private ArrayList<DavResource> getMatchingResources(DavContext ctxt, Element query) throws DavException, ServiceException {
        // needs to be /principals/users, or apply-to-principal-collection-set is set.
        ArrayList<DavResource> ret = new ArrayList<DavResource>();
        boolean applyToPrincipalCollection = query.element(DavElements.E_APPLY_TO_PRINCIPAL_COLLECTION_SET) != null;
        String path = ctxt.getUri();
        if (!applyToPrincipalCollection && !path.startsWith(UrlNamespace.PRINCIPALS_PATH))
            return ret;

        // apple hack to do user / resource search
        GalSearchType type = GalSearchType.all;
        String queryType = query.attributeValue("type");
        if (queryType != null) {
            if (queryType.compareToIgnoreCase("INDIVIDUAL") == 0)
                type = GalSearchType.account;
            else if (queryType.compareToIgnoreCase("RESOURCE") == 0)
                type = GalSearchType.resource;
        }
        @SuppressWarnings("unchecked")
        List propSearch = query.elements(DavElements.E_PROPERTY_SEARCH);
        for (Object obj : propSearch) {
            if (!(obj instanceof Element))
                continue;
            Element ps = (Element) obj;
            Element prop = ps.element(DavElements.E_PROP);
            Element match = ps.element(DavElements.E_MATCH);
            if (prop != null && match != null) {
                Element e = (Element)prop.elements().get(0);
                ret.addAll(getMatchingPrincipals(ctxt, e.getQName(), match.getText(), type));
            }
        }

        return ret;
    }

    private ArrayList<DavResource> getMatchingPrincipals(DavContext ctxt, QName prop, String match, GalSearchType type) throws DavException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        ArrayList<DavResource> ret = new ArrayList<DavResource>();
        Account authAccount = ctxt.getAuthAccount();
        if (prop.equals(DavElements.E_DISPLAYNAME)) {
            if (!authAccount.isFeatureGalEnabled() || !authAccount.isFeatureGalAutoCompleteEnabled())
                return ret;
            SearchGalResult result = prov.searchGal(prov.getDomain(authAccount), match, type, Provisioning.GalMode.zimbra, null);
            for (GalContact ct : result.getMatches()) {
                String email = (String)ct.getAttrs().get(ContactConstants.A_email);
                if (email != null) {
                    Account acct = prov.get(Key.AccountBy.name, email);
                    if (acct != null)
                        ret.add(UrlNamespace.getPrincipal(ctxt, acct));
                }
            }
        } else if (prop.equals(DavElements.E_CALENDAR_HOME_SET)) {
            int index = match.lastIndexOf('/');
            if (index > 0)
                match = match.substring(index+1);
            Account acct = prov.get(Key.AccountBy.name, match);
            if (acct != null)
                ret.add(UrlNamespace.getPrincipal(ctxt, acct));
        }
        return ret;
    }

    /**
     * http://tools.ietf.org/html/rfc3744#section-9.2 DAV:acl-principal-prop-set REPORT
     * Postconditions:
     *     (DAV:number-of-matches-within-limits): The number of matching principals must fall within
     *     server-specific, predefined limits. For example, this condition might be triggered if a search
     *     specification would cause the return of an extremely large number of responses.
     */
    private void handleAclPrincipalPropSet(DavContext ctxt, Element query) throws DavException, ServiceException {
        /* From rfc3744#section-9.2 DAV:acl-principal-prop-set REPORT
         *    This report is only defined when the Depth header has value "0"; other values result in a
         *    400 (Bad Request) error response.  Note that [RFC3253], Section 3.6, states that if the Depth header is
         *    not present, it defaults to a value of "0".
         */
        if (ctxt.getDepth() != Depth.zero) {
            throw new DavException.REPORTwithDisallowedDepthException(query.getQName().getName(), ctxt.getDepth());
        }
        RequestProp reqProp = ctxt.getRequestProp();
        DavResponse resp = ctxt.getDavResponse();
        // The response body for a successful request MUST be a DAV:multistatus XML element.  In the case where there
        // are no response elements, the returned multistatus XML element is empty.
        resp.getTop(DavElements.E_MULTISTATUS);
        for (DavResource rs : getAclPrincipals(ctxt))
            resp.addResource(ctxt, rs, reqProp, false);
    }

    private ArrayList<DavResource> getAclPrincipals(DavContext ctxt) throws DavException, ServiceException {
        ArrayList<DavResource> ret = new ArrayList<DavResource>();
        DavResource res = ctxt.getRequestedResource();
        if (!(res instanceof MailItemResource))
            return ret;
        List<Ace> aces = ((MailItemResource)res).getAce(ctxt);
        Provisioning prov = Provisioning.getInstance();
        for (Ace ace : aces) {
            if (ace.hasHref()) {
                Account acct = prov.get(Key.AccountBy.id, ace.getZimbraId());
                if (acct != null)
                    ret.add(UrlNamespace.getPrincipal(ctxt, acct));
            }
        }
        return ret;
    }

    /**
     * http://tools.ietf.org/html/rfc3744#section-9.3. DAV:principal-match REPORT
     *     This report is only defined when the Depth header has value "0";
     *     other values result in a 400 (Bad Request) error response.
     */
    private void handlePrincipalMatch(DavContext ctxt, Element query) throws DavException, ServiceException {
        if (ctxt.getDepth() != Depth.zero) {
            throw new DavException.REPORTwithDisallowedDepthException(query.getQName().getName(), ctxt.getDepth());
        }
        ArrayList<DavResource> ret = new ArrayList<DavResource>();
        RequestProp reqProp = ctxt.getRequestProp();
        DavResponse resp = ctxt.getDavResponse();
        // The response body for a successful request MUST be a DAV:multistatus XML element.  In the case where there
        // are no response elements, the returned multistatus XML element is empty.
        resp.getTop(DavElements.E_MULTISTATUS);
        Element principalProp = query.element(DavElements.E_PRINCIPAL_PROPERTY);
        if (principalProp == null) {
            // request must be to the principals path
            String path = ctxt.getUri();
            if (path.startsWith(UrlNamespace.PRINCIPALS_PATH))
                ret.add(UrlNamespace.getPrincipal(ctxt, ctxt.getAuthAccount()));
        } else {
            // we know of only <owner/> element
            Element owner = principalProp.element(DavElements.E_OWNER);
            if (owner != null) {
                // return the all the members of the collection.
                DavResource rs = ctxt.getRequestedResource();
                if (rs.isCollection())
                    ret.addAll(rs.getChildren(ctxt));
            }
        }
        for (DavResource rs : ret)
            resp.addResource(ctxt, rs, reqProp, false);
    }

    private static final ArrayList<Pair<QName,Element>> PRINCIPAL_SEARCH_PROPERTIES;

    static {
        PRINCIPAL_SEARCH_PROPERTIES = new ArrayList<Pair<QName,Element>>();
        addSearchProperty(DavElements.E_DISPLAYNAME, "Full name");
        addSearchProperty(DavElements.E_EMAIL_ADDRESS_SET, "Email Address");
        addSearchProperty(DavElements.E_CALENDAR_USER_TYPE, "User type");
        addSearchProperty(DavElements.E_CALENDAR_USER_ADDRESS_SET, "Calendar user address");
        addSearchProperty(DavElements.E_CALENDAR_HOME_SET, "Calendar home");
    }

    private static void addSearchProperty(QName prop, String desc) {
        Element elem = DocumentHelper.createElement(DavElements.E_DESCRIPTION);
        elem.addAttribute(DavElements.E_LANG, DavElements.LANG_EN_US);
        elem.setText(desc);
        PRINCIPAL_SEARCH_PROPERTIES.add(new Pair<QName,Element>(prop, elem));
    }

    /**
     * http://tools.ietf.org/html/rfc3744#section-9.5 DAV:principal-search-property-set REPORT
     *     This report is only defined when the Depth header has value "0";
     *     other values result in a 400 (Bad Request) error response.
     */
    private void handlePrincipalSearchPropertySet(DavContext ctxt, Element query)
    throws DavException, ServiceException {
        if (ctxt.getDepth() != Depth.zero) {
            throw new DavException.REPORTwithDisallowedDepthException(query.getQName().getName(), ctxt.getDepth());
        }
        Element response = ctxt.getDavResponse().getTop(DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET);
        ctxt.setStatus(HttpServletResponse.SC_OK);
        for (Pair<QName,Element> prop : PRINCIPAL_SEARCH_PROPERTIES) {
            Element searchProp = response.addElement(DavElements.E_PRINCIPAL_SEARCH_PROPERTY);
            searchProp.addElement(DavElements.E_PROP).addElement(prop.getFirst());
            searchProp.add(prop.getSecond().createCopy());
        }
    }
}
