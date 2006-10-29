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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.report;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.caldav.Filter.CompFilter;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.resource.CalendarCollection;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.service.ServiceException;

/*
 * draft-dusseault-caldav section 9.5
 * 
 *     <!ELEMENT calendar-query ((DAV:allprop |
 *                                DAV:propname |
 *                                DAV:prop)?, filter, timezone?)>
 *                                
 */
public class CalendarQuery extends ReportRequest {

	public Element handle(DavContext ctxt, Element query, Document document) throws ServiceException, DavException {
		if (!query.getQName().equals(DavElements.E_CALENDAR_QUERY))
			throw new DavException("msg "+query.getName()+" is not calendar-query", HttpServletResponse.SC_BAD_REQUEST, null);
		
		QueryContext qctxt = new QueryContext(ctxt, query);
		
		if (qctxt.componentFilter == null)
			throw new DavException("missing filter element in the request", HttpServletResponse.SC_BAD_REQUEST, null);
		
		DavResource rsc = UrlNamespace.getResource(ctxt);
		if (!(rsc instanceof CalendarCollection))
			throw new DavException("not a calendar resource", HttpServletResponse.SC_BAD_REQUEST, null);
		
		CalendarCollection cal = (CalendarCollection) rsc;
		Element resp = document.addElement(DavElements.E_MULTISTATUS);
		TimeRange tr = qctxt.componentFilter.getTimeRange();
		
		for (Appointment appt : cal.get(ctxt, tr))
			handleAppt(qctxt, appt, resp);

		return resp;
	}
	
	private void handleAppt(QueryContext ctxt, Appointment appt, Element response) throws DavException {
		Element r = response.addElement(DavElements.E_RESPONSE);
		try {
			CalendarObject calobj = new CalendarObject(appt);
			calobj.addHref(r, false);
			Element prop = r.addElement(DavElements.E_PROPSTAT).addElement(DavElements.E_PROP);
			if (ctxt.allProp) {
				
			} else
				for (QName requestedProp : ctxt.props) {
					ResourceProperty rp = calobj.getProperty(requestedProp);
					// TODO error checking
					if (rp != null)
						rp.toElement(ctxt.davCtxt, prop, false);
				}
			String apptData = new String(ByteUtil.getContent(calobj.getContent(), 
															   calobj.getContentLength()), 
										  "UTF-8");
			prop.addElement(DavElements.E_CALENDAR_DATA).setText(apptData);
		} catch (IOException ioe) {
			ZimbraLog.dav.error("can't get appointment data", ioe);
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get appointment data", se);
		}
	}
	
	private static class QueryContext {
		RequestedComponent requestedComponent;
		CompFilter         componentFilter;
		DavContext         davCtxt;
		
		boolean allProp;
		boolean propName;
		List<QName> props;
		
		QueryContext(DavContext ctxt, Element query) throws DavException {
			davCtxt = ctxt;
			allProp = false;
			propName = false;
			props = new ArrayList<QName>();
			
			boolean prop = false;
			for (Object o : query.elements()) {
				if (o instanceof Element) {
					Element elem = (Element) o;
					QName name = elem.getQName();
					if (name.equals(DavElements.E_FILTER)) {
						parseFilter(elem);
					} else if (name.equals(DavElements.E_TIMEZONE)) {
					} else {
						
						if (prop)
							throw new DavException("malformed calendar-query", HttpServletResponse.SC_BAD_REQUEST, null);

						if (name.equals(DavElements.E_ALLPROP)) {
							allProp = true;
						} else if (name.equals(DavElements.E_PROPNAME)) {
							propName = true;
						} else if (name.equals(DavElements.E_PROP)) {
							parseProp(elem);
						} else {
							throw new DavException("element "+name.getName()+" not recognized", HttpServletResponse.SC_BAD_REQUEST, null);
						}
						prop = true;
					}
				}
			}
		}
		
		private void parseProp(Element prop) {
			for (Object o : prop.elements()) {
				// get requested properties
				Element elem = (Element) o;
				QName name = elem.getQName();
				if (name.equals(DavElements.E_CALENDAR_DATA))
					parseCalendarData(elem);
				else 
					props.add(name);
			}
		}
		
		/*
		 *   <!ELEMENT calendar-data ((comp?, (expand |
		 *                                     limit-recurrence-set)?,
		 *                                     limit-freebusy-set?) |
		 *                            #PCDATA)?>
		 */
		private void parseCalendarData(Element gcd) {
			// TODO
			// expand
			// limit-recurrence-set
			// limit-freebusy-set
			
			// comp
			Element comp = gcd.element(DavElements.E_COMP);
			if (comp != null)
				requestedComponent = new RequestedComponent(comp);
		}
		
		private void parseFilter(Element filter) {
			for (Object o : filter.elements()) {
				if (o instanceof Element) {
					Element e = (Element) o;
					if (e.getQName().equals(DavElements.E_COMP_FILTER))
						componentFilter = new CompFilter(e);
				}
			}
		}
	}
	
	/*
	 *  <!ELEMENT comp ((allprop | prop*), (allcomp | comp*))>
	 *  <!ATTLIST comp name CDATA #REQUIRED>
	 */
	private static class RequestedComponent {
		String name;
		Set<String> props;
		Set<RequestedComponent> comps;
		
		RequestedComponent(Element elem) {
			props = new HashSet<String>();
			comps = new HashSet<RequestedComponent>();
		}
		
		void parse(Element elem) {
			// TODO handle allprop and allcomp
			name = elem.attributeValue(DavElements.P_NAME);
			for (Object o : elem.elements()) {
				if (o instanceof Element) {
					Element e = (Element) o;
					QName qname = e.getQName();
					if (qname.equals(DavElements.E_PROP))
						props.add(e.attributeValue(DavElements.P_NAME));
					else if (qname.equals(DavElements.E_COMP))
						comps.add(new RequestedComponent(e));
				}
			}
		}
	}
}
