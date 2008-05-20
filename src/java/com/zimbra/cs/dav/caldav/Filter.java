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
package com.zimbra.cs.dav.caldav;

import java.util.Collection;
import java.util.HashSet;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.mailbox.calendar.ZCalendar;

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
	protected TimeRange mTimeRange;
	protected HashSet<CompFilter> mComps;
	protected HashSet<PropFilter> mProps;
	protected HashSet<ParamFilter> mParams;
	protected HashSet<TextMatch> mTextMatches;

	public Filter(Element elem) {
		mProps = new HashSet<PropFilter>();
		mComps = new HashSet<CompFilter>();
		mParams = new HashSet<ParamFilter>();
		mTextMatches = new HashSet<TextMatch>();
		mName = elem.attributeValue(DavElements.P_NAME);
		if (mName.equals("VCALENDAR")) {
			elem = (Element)elem.elementIterator().next();
			mName = elem.attributeValue(DavElements.P_NAME);
		}
		parse(elem);
	}

	public String getName() {
		return mName;
	}
	
	protected void parse(Element elem) {
		for (Object o : elem.elements()) {
			if (o instanceof Element) {
				Element e = (Element) o;
				QName name = e.getQName();
				if (canHaveCompFilter() && name.equals(DavElements.E_COMP_FILTER))
					mComps.add(new CompFilter(e));
				else if (canHavePropFilter() && name.equals(DavElements.E_PROP_FILTER))
					mProps.add(new PropFilter(e));
				else if (canHaveParamFilter() && name.equals(DavElements.E_PARAM_FILTER))
					mParams.add(new ParamFilter(e));
				else if (name.equals(DavElements.E_TEXT_MATCH))
					mTextMatches.add(new TextMatch(e));
				else if (name.equals(DavElements.E_TIME_RANGE))
					mTimeRange = new TimeRange(e);
				else
					ZimbraLog.dav.info("unrecognized filter "+name.getNamespaceURI()+":"+name.getName());
			}
		}
		if (mTimeRange == null)
			mTimeRange = new TimeRange(null);
	}

	public boolean match(ZCalendar.ZComponent comp) {
		return comp.getName().equals(mName);
	}
	
	public boolean mIsNotDefinedSet() {
		return mIsNotDefinedSet;
	}

	protected boolean runTextMatch(String text) {
		boolean matched = true;
		for (TextMatch tm : mTextMatches)
			matched &= tm.match(text);
		return matched;
	}
	protected boolean canHaveCompFilter()  { return true; }
	protected boolean canHavePropFilter()  { return true; }
	protected boolean canHaveParamFilter() { return true; }
	
	public static class TextMatch {
		private String mCollation;
		private String mText;
		
		public TextMatch(Element elem) {
			mCollation = elem.attributeValue(DavElements.P_COLLATION);
			mText = elem.getText();
		}
		
		public boolean match(String val) {
			boolean ignoreCase = mCollation.equals(DavElements.ASCII);
			
			if (ignoreCase)
				return val.equalsIgnoreCase(mText);
			else
				return val.equals(mText);
		}
	}
	public static class ParamFilter extends Filter {
		public ParamFilter(Element elem) {
			super(elem);
		}
		public boolean match(ZCalendar.ZProperty prop) {
			ZCalendar.ICalTok tok = ZCalendar.ICalTok.lookup(mName);
			ZCalendar.ZParameter param = prop.getParameter(tok);
			if (param == null)
				return false;
			return runTextMatch(param.getValue());
		}
		protected boolean canHaveCompFilter()  { return false; }
		protected boolean canHavePropFilter()  { return false; }
		protected boolean canHaveParamFilter() { return false; }
	}
	public static class PropFilter extends Filter {
		
		public PropFilter(Element elem) {
			super(elem);
		}
		
		public boolean match(ZCalendar.ZComponent comp) {
			ZCalendar.ICalTok tok = ZCalendar.ICalTok.lookup(mName);
			if (tok == null)
				return false;
			boolean matched = true;
			ZCalendar.ZProperty prop = comp.getProperty(tok);
			for (ParamFilter pf : mParams)
				matched &= pf.match(prop);
			matched &= runTextMatch(prop.getValue());
			return matched;
		}

		protected boolean canHaveCompFilter()  { return false; }
		protected boolean canHavePropFilter()  { return false; }
	}
	public static class CompFilter extends Filter {
		
		public CompFilter(Element elem) {
			super(elem);
		}
		
		public boolean match(ZCalendar.ZComponent comp) {
			boolean matched = super.match(comp);
			
			if (matched && mComps.size() > 0) {
				matched = false;
				// needs to match at least one subcomponent
				for (CompFilter cf : mComps) {
					ZCalendar.ICalTok tok = ZCalendar.ICalTok.lookup(cf.mName);
					if (tok != null) {
						ZCalendar.ZComponent sub = comp.getComponent(tok);
						if (sub != null)
							matched |= cf.match(sub);
					}
				}
			}
			
			if (matched && mProps.size() > 0) {
				// needs to match all the properties
				for (PropFilter pf : mProps) {
					matched &= pf.match(comp);
				}
			}
			return matched;
		}
		
		protected boolean canHaveParamFilter() { return false; }
		
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
