/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav.service.method;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.caldav.TimeRange;
import com.zimbra.cs.dav.caldav.Filter.CompFilter;
import com.zimbra.cs.dav.resource.CalendarCollection;
import com.zimbra.cs.dav.resource.CalendarObject;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.service.DavResponse;

/*
 * draft-dusseault-caldav section 9.5
 * 
 *     <!ELEMENT calendar-query ((DAV:allprop |
 *                                DAV:propname |
 *                                DAV:prop)?, filter, timezone?)>
 *                                
 */
public class CalendarQuery extends Report {

	public void handle(DavContext ctxt) throws DavException, ServiceException {
		Element query = ctxt.getRequestMessage().getRootElement();
		if (!query.getQName().equals(DavElements.E_CALENDAR_QUERY))
			throw new DavException("msg "+query.getName()+" is not calendar-query", HttpServletResponse.SC_BAD_REQUEST, null);
		
		RequestProp reqProp = getRequestProp(ctxt);
		QueryContext qctxt = new QueryContext(ctxt, query, reqProp);
		
		if (qctxt.componentFilter == null)
			throw new DavException("missing filter element in the request", HttpServletResponse.SC_BAD_REQUEST, null);
		
		DavResource rsc = ctxt.getRequestedResource();
		if (!(rsc instanceof CalendarCollection))
			throw new DavException("not a calendar resource", HttpServletResponse.SC_BAD_REQUEST, null);
		
		CalendarCollection cal = (CalendarCollection) rsc;
		TimeRange tr = qctxt.componentFilter.getTimeRange();

		for (DavResource calItem : cal.get(ctxt, tr))
		    handleCalendarItem(qctxt, calItem);
	}
	
	private void handleCalendarItem(QueryContext ctxt, DavResource calItem) {
	    if (!(calItem instanceof CalendarObject))
	        return;
		try {
		    CalendarObject calobj = (CalendarObject)calItem;
			if (!calobj.match(ctxt.componentFilter))
				return;
			DavResponse resp = ctxt.davCtxt.getDavResponse();
			resp.addResource(ctxt.davCtxt, calItem, ctxt.props, false);
		} catch (DavException de) {
			ZimbraLog.dav.error("can't get calendar item data", de);
		}
	}
	
	private static class QueryContext {
		RequestedComponent requestedComponent;
		CompFilter         componentFilter;
		DavContext         davCtxt;
		RequestProp        props;
		
		QueryContext(DavContext ctxt, Element query, RequestProp rp) throws DavException {
			davCtxt = ctxt;
			props = rp;
			
			for (Object o : query.elements()) {
				if (o instanceof Element) {
					Element elem = (Element) o;
					QName name = elem.getQName();
					if (name.equals(DavElements.E_FILTER)) {
						parseFilter(elem);
					} else if (name.equals(DavElements.E_TIMEZONE)) {
					} else if (name.equals(DavElements.E_PROP)) {
						for (Object obj : elem.elements())
							if (obj instanceof Element) {
								elem = (Element) obj;
								if (elem.getQName().equals(DavElements.E_CALENDAR_DATA))
									parseCalendarData(elem);
							}
					}
				}
			}
		}
		
		/*
		 *   <!ELEMENT calendar-data ((comp?, (expand |
		 *                                     limit-recurrence-set)?,
		 *                                     limit-freebusy-set?) |
		 *                            #PCDATA)?>
		 */
		private void parseCalendarData(Element cd) {
			// TODO
			// expand
			// limit-recurrence-set
			// limit-freebusy-set
			
			// comp
			Element comp = cd.element(DavElements.E_COMP);
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
