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
import com.zimbra.cs.mailbox.Appointment;

public abstract class Filter {
	protected String mName;
	protected boolean mIsNotDefinedSet;

	public Filter(Element elem) {
		mName = elem.attributeValue(DavElements.P_NAME);
	}

	public String getName() {
		return mName;
	}
	
	public abstract boolean match(Appointment appt);
	
	public boolean mIsNotDefinedSet() {
		return mIsNotDefinedSet;
	}

	public static class TextMatch extends Filter {
		public TextMatch(Element elem) {
			super(elem);
		}
		
		public boolean match(Appointment appt) {
			return true;
		}
	}
	public static class ParamFilter extends Filter {
		public ParamFilter(Element elem) {
			super(elem);
			
		}
		public boolean match(Appointment appt) {
			return true;
		}
	}
	public static class PropFilter extends Filter {
		private HashSet<ParamFilter> mParams;
		
		public PropFilter(Element elem) {
			super(elem);
			
		}
		
		public boolean match(Appointment appt) {
			return true;
		}

		public Collection<ParamFilter> getParamFilters() {
			return mParams;
		}
	}
	public static class CompFilter extends Filter {
		private TimeRange mTimeRange;
		private HashSet<PropFilter> mProps;
		private HashSet<CompFilter> mComps;
		
		public CompFilter(Element elem) {
			super(elem);
			mProps = new HashSet<PropFilter>();
			mComps = new HashSet<CompFilter>();
			parse(elem);
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
						ZimbraLog.dav.info("unrecognized filter "+name);
				}
			}
			if (mTimeRange == null)
				mTimeRange = new TimeRange(null);
		}

		public boolean match(Appointment appt) {
			return true;
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
