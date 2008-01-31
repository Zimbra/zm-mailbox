/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.fb;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultDocument;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DomUtil;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.regex.Pattern;

public class ExchangeMessage {
	/*
	 * public folder URL:
	 * urlencode( first part + encrypt(/o=<organization>/ou=<organization unit>) + second part + encrypt(/cn=RECIPIENTS/cn=<mailboxid>.EML) )
	 * 
	 * first part:
	 * /public/NON_IPM_SUBTREE/SCHEDULE+ FREE BUSY/EX:
	 * 
	 * second part:
	 * /USER-
	 * encrypt:
	 * s/\//_xF8FF_/g
	 * 
	 * urlencode:
	 * s/\+/%2B/g
	 * s/ /%20/g
	 * 
	 * result:
     * http://exchange/public/NON_IPM_SUBTREE/SCHEDULE%2B%20FREE%20BUSY/EX:_xF8FF_o=First%20Organization_xF8FF_ou=First%20Administrative%20Group/USER-_xF8FF_cn=RECIPIENTS_xF8FF_cn=USER1.EML
	 */
	public static final String PUBURL_FIRST_PART = "/public/NON_IPM_SUBTREE/SCHEDULE+ FREE BUSY/EX:";
	public static final String PUBURL_SECOND_PART = "USER-";
	public static final String PUBURL_RCPT = "/cn=RECIPIENTS/cn=";
	public static final String PUBURL_EML = ".EML";
	public static final String ENCODED_SLASH = "_xF8FF_";
	public static final String ENCODED_SPACE = "%20";
	public static final String ENCODED_PLUS = "%2B";
	
	public static final String MV_INT = "mv.int";
	public static final String MV_BIN = "mv.bin.base64";
	
	public static final int MINS_IN_DAY = 60 * 24;
	
    public static final Namespace NS_DAV = Namespace.get("D", "DAV:");
    public static final Namespace NS_XML = Namespace.get("c", "xml:");
    public static final Namespace NS_MSFT = Namespace.get("b", "http://schemas.microsoft.com/mapi/proptag/");
    public static final Namespace NS_WEB_FOLDERS = Namespace.get("e", "urn:uuid:c2f41010-65b3-11d1-a29f-00aa00c14882/");
	
    public static final QName EL_SET = QName.get("set", NS_DAV);
    public static final QName EL_PROP = QName.get("prop", NS_DAV);
    public static final QName EL_PROPERTYUPDATE = QName.get("propertyupdate", NS_DAV);
    public static final QName EL_V = QName.get("v", NS_XML);
    
    public static final QName ATTR_DT = QName.get("dt", NS_WEB_FOLDERS);
    
    public static final QName PR_FREEBUSY_ALL_EVENTS       = QName.get("0x68501102", NS_MSFT);
    public static final QName PR_FREEBUSY_ALL_MONTHS       = QName.get("0x684F1003", NS_MSFT);
    public static final QName PR_FREEBUSY_BUSY_EVENTS      = QName.get("0x68541102", NS_MSFT);
    public static final QName PR_FREEBUSY_BUSY_MONTHS      = QName.get("0x68531003", NS_MSFT);
    public static final QName PR_FREEBUSY_EMAIL_ADDRESS    = QName.get("0x6849001F", NS_MSFT);
    public static final QName PR_FREEBUSY_END_RANGE        = QName.get("0x68480003", NS_MSFT);
    public static final QName PR_FREEBUSY_ENTRYIDS         = QName.get("0x36E41102", NS_MSFT);
    public static final QName PR_FREEBUSY_LAST_MODIFIED    = QName.get("0x68680040", NS_MSFT);
    public static final QName PR_FREEBUSY_NUM_MONTHS       = QName.get("0x68690003", NS_MSFT);
    public static final QName PR_FREEBUSY_OOF_EVENTS       = QName.get("0x68561102", NS_MSFT);
    public static final QName PR_FREEBUSY_OOF_MONTHS       = QName.get("0x68551003", NS_MSFT);
    public static final QName PR_FREEBUSY_START_RANGE      = QName.get("0x68470003", NS_MSFT);
    public static final QName PR_FREEBUSY_TENTATIVE_EVENTS = QName.get("0x68521102", NS_MSFT);
    public static final QName PR_FREEBUSY_TENTATIVE_MONTHS = QName.get("0x68511003", NS_MSFT);
    public static final QName PR_PROCESS_MEETING_REQUESTS  = QName.get("0x686D000B", NS_MSFT);
    public static final QName PR_DECLINE_RECURRING_MEETING_REQUESTS = QName.get("0x686E000B", NS_MSFT);
    public static final QName PR_DECLINE_CONFLICTING_MEETING_REQUESTS = QName.get("0x686F000B", NS_MSFT);
    public static final QName PR_CAL_END_TIME              = QName.get("0x10C40040", NS_MSFT);
    public static final QName PR_CAL_RECURRING_ID          = QName.get("0x10C50040", NS_MSFT);
    public static final QName PR_CAL_REMINDER_NEXT_TIME    = QName.get("0x10CA0040", NS_MSFT);
    public static final QName PR_CAL_START_TIME            = QName.get("0x10C30040", NS_MSFT);
    public static final QName PR_SUBJECT_A                 = QName.get("0x0037001E", NS_MSFT);
    
    public static final QName PR_68410003 = QName.get("0x68410003", NS_MSFT);
    public static final QName PR_6842000B = QName.get("0x6842000B", NS_MSFT);
    public static final QName PR_6843000B = QName.get("0x6843000B", NS_MSFT);
    public static final QName PR_6846000B = QName.get("0x6846000B", NS_MSFT);
    public static final QName PR_684B000B = QName.get("0x684B000B", NS_MSFT);
    
    private static Pattern SLASH = Pattern.compile("\\/");
    private static Pattern SPACE = Pattern.compile(" ");
    private static Pattern PLUS  = Pattern.compile("\\+");
    
    private String mOu;
    private String mMail;
    
    public ExchangeMessage(String ou, String mail) {
    	mOu = ou;
    	mMail = mail;
    }
    
    public String getUrl() {
    	StringBuilder buf = new StringBuilder(PUBURL_FIRST_PART);
    	buf.append(SLASH.matcher(mOu).replaceAll(ENCODED_SLASH));
    	buf.append("/").append(PUBURL_SECOND_PART);
    	buf.append(SLASH.matcher(PUBURL_RCPT).replaceAll(ENCODED_SLASH));
    	buf.append(mMail);
    	buf.append(PUBURL_EML);
    	
    	String ret = buf.toString();
    	ret = SPACE.matcher(ret).replaceAll(ENCODED_SPACE);
    	ret = PLUS.matcher(ret).replaceAll(ENCODED_PLUS);
    	return ret;
    }
    
    public Document createRequest(FreeBusy fb) {
    	Element root = DocumentHelper.createElement(EL_PROPERTYUPDATE);
    	root.add(NS_XML);
    	root.add(NS_MSFT);
    	root.add(NS_WEB_FOLDERS);
    	Element prop = root.addElement(EL_SET).addElement(EL_PROP);
    	addElement(prop, PR_SUBJECT_A, PUBURL_SECOND_PART + PUBURL_RCPT + mMail);
    	addElement(prop, PR_FREEBUSY_START_RANGE, minutesSinceMsEpoch(fb.getStartTime()));
    	addElement(prop, PR_FREEBUSY_END_RANGE, minutesSinceMsEpoch(fb.getEndTime()));
    	addElement(prop, PR_FREEBUSY_EMAIL_ADDRESS, mOu + PUBURL_RCPT + mMail);
    	
    	
    	Element allMonths = addElement(prop, PR_FREEBUSY_ALL_MONTHS, null, ATTR_DT, MV_INT);
    	Element allEvents = addElement(prop, PR_FREEBUSY_ALL_EVENTS, null, ATTR_DT, MV_BIN);
    	Element busyMonths = addElement(prop, PR_FREEBUSY_BUSY_MONTHS, null, ATTR_DT, MV_INT);
    	Element busyEvents = addElement(prop, PR_FREEBUSY_BUSY_EVENTS, null, ATTR_DT, MV_BIN);
    	Element tentativeMonths = addElement(prop, PR_FREEBUSY_TENTATIVE_MONTHS, null, ATTR_DT, MV_INT);
    	Element tentativeEvents = addElement(prop, PR_FREEBUSY_TENTATIVE_EVENTS, null, ATTR_DT, MV_BIN);
    	Element oofMonths = addElement(prop, PR_FREEBUSY_OOF_MONTHS, null, ATTR_DT, MV_INT);
    	Element oofEvents = addElement(prop, PR_FREEBUSY_OOF_EVENTS, null, ATTR_DT, MV_BIN);
    	
    	addElement(prop, PR_68410003, "0");
    	addElement(prop, PR_6842000B, "1");
    	addElement(prop, PR_6843000B, "1");
    	addElement(prop, PR_6846000B, "1");
    	addElement(prop, PR_684B000B, "1");
    	addElement(prop, PR_PROCESS_MEETING_REQUESTS, "0");
    	addElement(prop, PR_DECLINE_RECURRING_MEETING_REQUESTS, "0");
    	addElement(prop, PR_DECLINE_CONFLICTING_MEETING_REQUESTS, "0");
    	
    	Iterator<FreeBusy.Interval> iter = fb.iterator();
    	while (iter.hasNext()) {
    		FreeBusy.Interval interval = iter.next();
    		String status = interval.getStatus();
    		Element elMonth = null;
    		Element elEvent = null;
    		if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
    			elMonth = busyMonths;
    			elEvent = busyEvents;
    		} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
    			elMonth = tentativeMonths;
    			elEvent = tentativeEvents;
    		} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
    			elMonth = oofMonths;
    			elEvent = oofEvents;
    		}
    		if (elMonth != null) {
    			long start = interval.getStart();
    			long end = interval.getEnd();
    			try {
    				String busyMonth = Long.toString(millisToMonths(start));
    				String busyTime = encodeFb(start, end);
    				addElement(elMonth, EL_V, busyMonth);
    				addElement(elEvent, EL_V, busyTime);
    				addElement(allMonths, EL_V, busyMonth);
    				addElement(allEvents, EL_V, busyTime);
    			} catch (IOException e) {
    				ZimbraLog.misc.warn("error converting millis to minutes "+start+", "+end, e);
    				continue;
    			}
    		}
    	}
    	if (allMonths.elements().size() == 0) allMonths.detach();
    	if (allEvents.elements().size() == 0) allEvents.detach();
    	if (busyMonths.elements().size() == 0) busyMonths.detach();
    	if (busyEvents.elements().size() == 0) busyEvents.detach();
    	if (tentativeMonths.elements().size() == 0) tentativeMonths.detach();
    	if (tentativeEvents.elements().size() == 0) tentativeEvents.detach();
    	if (oofMonths.elements().size() == 0) oofMonths.detach();
    	if (oofEvents.elements().size() == 0) oofEvents.detach();
    	return new DefaultDocument(root);
    }
    
    public HttpMethod createMethod(String uri, FreeBusy fb) throws IOException {
    	// PROPPATCH
    	PostMethod method = new PostMethod(uri) {
    		private String PROPPATCH = "PROPPATCH";
    		public String getName() {
    			return PROPPATCH;
    		}
    	};
		Document doc = createRequest(fb);
		byte[] buf = DomUtil.getBytes(doc);
		ByteArrayRequestEntity re = new ByteArrayRequestEntity(buf, "text/xml");
		method.setRequestEntity(re);
		return method;
    }
    private Element addElement(Element parent, QName name, String text, QName attr, String attrVal) {
    	Element el = parent.addElement(name);
    	if (text != null)
    		el.setText(text);
    	if (attr != null && attrVal != null)
    		el.addAttribute(attr, attrVal);
    	return el;
    }
    private Element addElement(Element parent, QName name, String text) {
    	return addElement(parent, name, text, null, null);
    }
    private String encodeFb(long s, long e) throws IOException {
    	int start = millisToMinutes(s);
    	int end = millisToMinutes(e);
    	// swap bytes and convert to little endian.  then lay out bytes
    	// two bytes each for start time, then end time
    	byte[] buf = new byte[4];
    	int i = 0;
    	buf[i++] = (byte)(start & 0xFF);
    	buf[i++] = (byte)(start >> 8 & 0xFF);
    	buf[i++] = (byte)(end & 0xFF);
    	buf[i++] = (byte)(end >> 8 & 0xFF);
    	
    	// base64 encode
    	byte[] encoded = Base64.encodeBase64(buf);
    	return new String(encoded, "UTF-8");
    }
    private int millisToMinutes(long millis) throws IOException {
    	// millis since epoch to minutes since 1st of the month
    	Calendar c = new GregorianCalendar();
    	c.setTime(new Date(millis));
    	int days = c.get(Calendar.DAY_OF_MONTH) - 1;
    	int hours =  24 * days + c.get(Calendar.HOUR_OF_DAY);
    	return 60 * hours + c.get(Calendar.MINUTE);
    }
    private long millisToMonths(long millis) {
    	// number of freebusy months = year * 16 + month
    	// why * 16 not * 12, ask msft.
    	Calendar c = new GregorianCalendar();
    	c.setTime(new Date(millis));
    	return c.get(Calendar.YEAR) * 16 + c.get(Calendar.MONTH) + 1;  // january is 0
    }
    private static char[] HEX = { 
    	'0', '1', '2', '3', '4', '5', '6', '7',
    	'8', '9', 'a', 'b', 'c', 'd', 'e', 'f'			
    };
    private String minutesSinceMsEpoch(long millis) {
    	// filetime or ms epoch is calculated as minutes since Jan 1 1601
    	// standard epoch is Jan 1 1970.  the offset in seconds is
    	// 11644473600
    	long mins = (millis / 1000 + 11644473600L) / 60;
    	
    	// convert to hex in little endian.
    	StringBuilder buf = new StringBuilder();
    	for (int i = 0; i < 8; i++) {
        	buf.insert(0, HEX[(int)(mins & 0xF)]);
        	mins >>= 4;
    	}
    	buf.insert(0, "0x");
    	return buf.toString();
    }
}
