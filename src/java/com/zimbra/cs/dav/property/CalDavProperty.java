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
package com.zimbra.cs.dav.property;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;

public class CalDavProperty extends ResourceProperty {

	public static ResourceProperty getSupportedCalendarComponentSet() {
		return new SupportedCalendarComponentSet();
	}
	
	public static ResourceProperty getSupportedCalendarData() {
		return new SupportedCalendarData();
	}
	
	public static ResourceProperty getSupportedCollationSet() {
		return new SupportedCollationSet();
	}
	
	protected CalDavProperty(QName name) {
		super(name);
		setProtected(true);
	}

	public enum CalComponent {
		VEVENT, VTODO, VJOURNAL, VTIMEZONE, VFREEBUSY
	}
	
	private static final CalComponent[] sSUPPORTED_COMPONENTS = {
		CalComponent.VEVENT, CalComponent.VTIMEZONE, CalComponent.VFREEBUSY
	};
	
	private static class SupportedCalendarComponentSet extends CalDavProperty {
		public SupportedCalendarComponentSet() {
			super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
		}
		
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element compSet = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return compSet;
			for (CalComponent comp : sSUPPORTED_COMPONENTS)
				compSet.addElement(DavElements.E_COMP).addAttribute(DavElements.P_NAME, comp.name());
			return compSet;
		}
	}
	
	private static class SupportedCalendarData extends CalDavProperty {
		public SupportedCalendarData() {
			super(DavElements.E_SUPPORTED_CALENDAR_DATA);
		}
		
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element calData = super.toElement(ctxt, parent, true);
			Element e = calData.addElement(DavElements.E_CALENDAR_DATA);
			e.addAttribute(DavElements.P_CONTENT_TYPE, Mime.CT_TEXT_CALENDAR);
			e.addAttribute(DavElements.P_VERSION, ZCalendar.sIcalVersion);
			
			return calData;
		}
	}
	
	private static class SupportedCollationSet extends CalDavProperty {
		public static final String ASCII = "i;ascii-casemap";  // case insensitive
		public static final String OCTET = "i;octet";
		
		public SupportedCollationSet() {
			super(DavElements.E_SUPPORTED_COLLATION_SET);
		}
		
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element collation = super.toElement(ctxt, parent, true);
			collation.addElement(DavElements.E_SUPPORTED_COLLATION).setText(ASCII);
			collation.addElement(DavElements.E_SUPPORTED_COLLATION).setText(OCTET);
			return collation;
		}
	}
}
