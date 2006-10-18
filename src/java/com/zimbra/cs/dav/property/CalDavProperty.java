package com.zimbra.cs.dav.property;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.mailbox.calendar.ZCalendar;
import com.zimbra.cs.mime.Mime;

public class CalDavProperty extends ResourceProperty {

	public static ResourceProperty getSupportedCalendarComponentSet() {
		return new SupportedCalendarComponentSet();
	}
	
	public static ResourceProperty getSupportedCalendarData() {
		return new SupportedCalendarData();
	}
	
	protected CalDavProperty(QName name) {
		super(name);
		setProtected(true);
	}

	private static final String[] sSUPPORTED_COMPONENTS = {
		"VEVENT", "VTODO", "VJOURNAL", "VTIMEZONE", "VFREEBUSY"
	};
	
	private static class SupportedCalendarComponentSet extends CalDavProperty {
		public SupportedCalendarComponentSet() {
			super(DavElements.E_SUPPORTED_CALENDAR_COMPONENT_SET);
		}
		
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element compSet = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return compSet;
			for (String comp : sSUPPORTED_COMPONENTS)
				compSet.addElement(DavElements.E_COMP).addAttribute(DavElements.P_NAME, comp);
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
}
