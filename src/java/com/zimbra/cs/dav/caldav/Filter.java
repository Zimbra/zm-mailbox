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
package com.zimbra.cs.dav.caldav;

import java.util.Collection;
import java.util.HashSet;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.CalDavProperty.CalComponent;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.calendar.Invite;

/*
 * draft-dusseault-caldav section 9.7
 * 
 *          <!ELEMENT filter (comp-filter)>
 *          
 *          <!ELEMENT comp-filter (is-not-defined | (time-range?,
 *                                 prop-filter*, comp-filter*))>
 *          <!ATTLIST comp-filter name CDATA #REQUIRED>
 *          
 *          <!ELEMENT prop-filter ((is-not-defined |
 *                                ((time-range | text-match)?,
 *                                 param-filter*))>
 *          <!ATTLIST prop-filter name CDATA #REQUIRED>
 *          
 *          <!ELEMENT param-filter (is-not-defined | text-match)?>
 *          <!ATTLIST param-filter name CDATA #REQUIRED>
 *
 *          <!ELEMENT text-match (#PCDATA)>
 *          PCDATA value: string
 *          <!ATTLIST text-match collation        CDATA "i;ascii-casemap"
 *                               negate-condition (yes | no) "no">
 */
public abstract class Filter {
	protected String mName;
	protected boolean mIsNotDefinedSet;

	public Filter(Element elem) {
		mName = elem.attributeValue(DavElements.P_NAME);
	}

	public String getName() {
		return mName;
	}
	
	public abstract boolean match(Invite invite);
	
	public boolean mIsNotDefinedSet() {
		return mIsNotDefinedSet;
	}

	/* TODO: implement */
	public static class TextMatch extends Filter {
		public TextMatch(Element elem) {
			super(elem);
		}
		
		public boolean match(Invite invite) {
			return true;
		}
	}
	/* TODO: implement */
	public static class ParamFilter extends Filter {
		public ParamFilter(Element elem) {
			super(elem);
			
		}
		public boolean match(Invite invite) {
			return true;
		}
	}
	/* TODO: implement */
	public static class PropFilter extends Filter {
		private HashSet<ParamFilter> mParams;
		
		public PropFilter(Element elem) {
			super(elem);
			
		}
		
		public boolean match(Invite invite) {
			return true;
		}

		public Collection<ParamFilter> getParamFilters() {
			return mParams;
		}
	}
	public static class CompFilter extends Filter {
		private CalComponent mComponent;
		private TimeRange mTimeRange;
		private HashSet<PropFilter> mProps;
		private HashSet<CompFilter> mComps;
		
		public CompFilter(Element elem) {
			super(elem);
			mProps = new HashSet<PropFilter>();
			mComps = new HashSet<CompFilter>();
			parse(elem);
			String name = elem.attributeValue(DavElements.P_NAME);
			mComponent = CalDavProperty.getCalComponent(name);
		}
		
		void parse(Element elem) {
			for (Object o : elem.elements()) {
				if (o instanceof Element) {
					Element e = (Element) o;
					QName name = e.getQName();
					if (name.equals(DavElements.E_COMP_FILTER))
						mComps.add(new CompFilter(e));
					else if (name.equals(DavElements.E_PROP_FILTER))
						mProps.add(new PropFilter(e));
					else if (name.equals(DavElements.E_TIME_RANGE))
						mTimeRange = new TimeRange(e);
					else
						ZimbraLog.dav.info("unrecognized filter "+name.getNamespaceURI()+":"+name.getName());
				}
			}
			if (mTimeRange == null)
				mTimeRange = new TimeRange(null);
		}

		public boolean match(Invite invite) {
			boolean matched = false;
            byte type = invite.getItemType();
			
			if (mComponent == CalComponent.VCALENDAR)
				matched = true;
            else if (type == MailItem.TYPE_APPOINTMENT)
				matched = (mComponent == CalComponent.VEVENT);
            else if (type == MailItem.TYPE_TASK)
				matched = (mComponent == CalComponent.VTODO);

			if (matched && mComps.size() > 0) {
				matched = false;
				// needs to match at least one subcomponent
				for (CompFilter cf : mComps) {
					matched |= cf.match(invite);
				}
			}
			return matched;
		}
		
		public TimeRange getTimeRange() {
			return mTimeRange;
		}
		
		public Collection<PropFilter> getPropFilters() {
			return mProps;
		}
		
		public Collection<CompFilter> getCompFilters() {
			return mComps;
		}
	}
}
