/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox.calendar;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.mime.MimeConstants;

import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;

public class ZCalendar {
    
    public static final String sZimbraProdID = "Zimbra-Calendar-Provider";
    public static final String sIcalVersion = "2.0";
    public static final String sObsoleteVcalVersion = "1.0";
    
    public static enum ICalTok {
        ACTION, ALTREP, ATTACH, ATTENDEE, BINARY, BOOLEAN,
        CAL_ADDRESS, CALSCALE, CATEGORIES, CLASS, CN, COMMENT,
        COMPLETED, CONTACT, CREATED, CUTYPE, DATE, DATE_TIME,
        DELEGATED_FROM, DELEGATED_TO, DESCRIPTION, DIR, DTEND, DTSTAMP,
        DTSTART, DUE, DURATION, ENCODING, EXDATE,
        EXRULE, FBTYPE, FLOAT, FMTTYPE, FREEBUSY, GEO,
        INTEGER, LANGUAGE, LAST_MODIFIED, LOCATION, MEMBER, METHOD,
        ORGANIZER, PARTSTAT, PERCENT_COMPLETE, PERIOD, PRIORITY, PRODID,
        RDATE, RECUR, RECURRENCE_ID, RELATED, RELATED_TO,
        RELTYPE, REPEAT, RESOURCES, ROLE, RRULE, RSVP,
        SENT_BY, SEQUENCE, STATUS, SUMMARY, TEXT, TIME,
        TRANSP, TRIGGER, TZID, TZNAME, TZOFFSETFROM,
        TZOFFSETTO, TZURL, UID, URI, URL, UTC_OFFSET,
        VALARM, VALUE, VERSION, VEVENT, VFREEBUSY, VJOURNAL,
        VTIMEZONE, VTODO, 
        
        // METHOD
        PUBLISH, REQUEST, REPLY, ADD, CANCEL, REFRESH, COUNTER, DECLINECOUNTER,

        // CLASS
        PUBLIC, PRIVATE, CONFIDENTIAL,
        
        // ROLE
        CHAIR, REQ_PARTICIPANT, OPT_PARTICIPANT, NON_PARTICIPANT,

        // CUTYPE
        INDIVIDUAL, GROUP, RESOURCE, ROOM, UNKNOWN,

        // STATUS
        TENTATIVE, CONFIRMED, /*CANCELLED,*/ NEEDS_ACTION, /*COMPLETED,*/ IN_PROCESS, CANCELLED,
        DRAFT, FINAL,
        
        // PARTSTAT
        ACCEPTED, /*COMPLETED,*/ DECLINED, DELEGATED, /*IN_PROCESS,*/ /*NEEDS_ACTION,*/ /*TENTATIVE,*/
        
        // TRANSPARENCY
        TRANSPARENT, OPAQUE,
        
        // VTIMEZONE
        STANDARD, DAYLIGHT,
        
        // RECURRENCE-ID
        RANGE, THISANDFUTURE, THISANDPRIOR,

        // alternate DESCRIPTION (HTML; used by Outlook 2007)
        X_ALT_DESC,

        X_MICROSOFT_CDO_ALLDAYEVENT, X_MICROSOFT_CDO_INTENDEDSTATUS, X_MICROSOFT_DISALLOW_COUNTER,

        // ZCO Custom values
        X_ZIMBRA_STATUS, X_ZIMBRA_STATUS_WAITING, X_ZIMBRA_STATUS_DEFERRED,
        X_ZIMBRA_PARTSTAT_WAITING, X_ZIMBRA_PARTSTAT_DEFERRED,

        // whether VEVENT/VTODO for an exception is a local-only change not shared with other attendees
        X_ZIMBRA_LOCAL_ONLY,

        // set to TRUE in series update to tell attendee to discard all exceptions while applying new series
        // This is a ZCO special.
        X_ZIMBRA_DISCARD_EXCEPTIONS,

        // tracks important data that changed in a modify operation
        // comma-separated list of "time", "location", etc.  (See InviteChanges class for more info.)
        X_ZIMBRA_CHANGES;

        public static ICalTok lookup(String str) 
        {
            try {
                str = str.replace('-', '_');
                return ICalTok.valueOf(str);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        public String toString() {
            return super.toString().replace('_', '-');
        }
    }

    private static final String LINE_BREAK = "\r\n";

    /**
     * @author tim
     * 
     * Calendar has
     *    Components
     *    Properties
     */
    public static class ZVCalendar
    {
        List<ZComponent> mComponents = new ArrayList<ZComponent>();
        List<ZProperty> mProperties = new ArrayList<ZProperty>();
        
        public ZVCalendar() { 
            addProperty(new ZProperty(ICalTok.PRODID, sZimbraProdID));
            addProperty(new ZProperty(ICalTok.VERSION, sIcalVersion));
        }
        
        public void addProperty(ZProperty prop) { mProperties.add(prop); }
        public void addComponent(ZComponent comp) { mComponents.add(comp); }

        public ZComponent getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        public Iterator<ZComponent> getComponentIterator() { return mComponents.iterator(); }
        public ZProperty getProperty(ICalTok tok) { return findProp(mProperties, tok); }
        public String getPropVal(ICalTok tok, String defaultValue) {
            ZProperty prop = getProperty(tok);
            if (prop != null) 
                return prop.mValue;
            
            return defaultValue;
        }
        public long getPropLongVal(ICalTok tok, long defaultValue) {
            ZProperty prop = getProperty(tok);
            if (prop != null) 
                return Long.parseLong(prop.mValue);
            
            return defaultValue;
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer("BEGIN:VCALENDAR");
            toRet.append(LINE_BREAK);
            String INDENT = "\t";
            for (ZProperty prop : mProperties) {
                toRet.append(prop.toString(INDENT));
            }
            
            for (ZComponent comp : mComponents) {
                toRet.append(comp.toString(INDENT));
            }
            toRet.append("END:VCALENDAR");
            return toRet.toString();
        }

        public void toICalendar(Writer w) throws IOException {
            toICalendar(w, false);
        }

        public void toICalendar(Writer w, boolean needAppleICalHacks) throws IOException {
            w.write("BEGIN:VCALENDAR");
            w.write(LINE_BREAK);
            for (ZProperty prop : mProperties)
                prop.toICalendar(w, needAppleICalHacks);
            
            for (ZComponent comp : mComponents)
                comp.toICalendar(w, needAppleICalHacks);

            w.write("END:VCALENDAR");
        }

        public void addDescription(String desc, String descHtml) {
            ZProperty descProp = new ZProperty(ICalTok.DESCRIPTION, desc);
            ZProperty altDescProp = new ZProperty(ICalTok.X_ALT_DESC, descHtml);
            altDescProp.addParameter(new ZParameter(ICalTok.FMTTYPE, MimeConstants.CT_TEXT_HTML));
            for (ZComponent comp : mComponents) {
                ICalTok name = comp.getTok();
                if (ICalTok.VEVENT.equals(name) || ICalTok.VTODO.equals(name) || ICalTok.VJOURNAL.equals(name)) {
                    if (desc != null && desc.length() > 0) {
                        // Add DESCRIPTION property to components that take that property,
                        // if the property is not set.
                        ZProperty prop = comp.getProperty(ICalTok.DESCRIPTION);
                        if (prop == null) {
                            comp.addProperty(descProp);
                        } else {
                            String val = prop.getValue();
                            if (val == null || val.length() < 1)
                                prop.setValue(desc);
                        }
                    }
                    if (descHtml != null && descHtml.length() > 0) {
                        // Just add the X-ALT-DESC value.  Don't worry about replacing existing value.
                        comp.addProperty(altDescProp);
                    }
                }
            }
        }

        public ICalTok getMethod() {
            ICalTok ret = null;
            ZProperty method = getProperty(ICalTok.METHOD);
            if (method != null) {
                String methodStr = method.getValue();
                if (methodStr != null) {
                    try {
                        ret = ICalTok.valueOf(methodStr);
                    } catch (IllegalArgumentException e) {}
                }
            }
            return ret;
        }
    }
    
    /**
     * @author tim
     * 
     * Component has
     *     Name
     *     Properties
     *     Components
     */
    public static class ZComponent
    {
        public ZComponent(String name) {
            mName = name.toUpperCase();
            mTok = ICalTok.lookup(mName);
        }
        
        public ZComponent(ICalTok tok) {
            mTok = tok;
            mName = tok.toString();
        }
        
        private String mName;
        ICalTok mTok;
        
        public String getName() { return mName; }
        public ICalTok getTok() { return mTok; }

        List<ZProperty> mProperties = new ArrayList<ZProperty>();
        List<ZComponent> mComponents = new ArrayList<ZComponent>();
        
        public void addProperty(ZProperty prop) { mProperties.add(prop); }
        public void addComponent(ZComponent comp) { mComponents.add(comp); }
        
        public ZComponent getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        public Iterator<ZComponent> getComponentIterator() { return mComponents.iterator(); }
        public Iterator<ZProperty> getPropertyIterator() { return mProperties.iterator(); }
        public ZProperty getProperty(ICalTok tok) { return findProp(mProperties, tok); }
        public String getPropVal(ICalTok tok, String defaultValue) {
            ZProperty prop = getProperty(tok);
            if (prop != null) 
                return prop.mValue;
            
            return defaultValue;
        }
        long getPropLongVal(ICalTok tok, long defaultValue) {
            ZProperty prop = getProperty(tok);
            if (prop != null) 
                return Long.parseLong(prop.mValue);
            
            return defaultValue;
        }
        
        
        public String toString() {
            return toString("");
        }
        
        public String toString(String INDENT) {
            StringBuffer toRet = new StringBuffer(INDENT).append("COMPONENT:").append(mName).append('(').append(mTok).append(')').append('\n');
            String NEW_INDENT = INDENT+'\t';
            for (ZProperty prop : mProperties) {
                toRet.append(prop.toString(NEW_INDENT));
            }
            for (ZComponent comp : mComponents) {
                toRet.append(comp.toString(NEW_INDENT));
            }
            toRet.append(INDENT).append("END:").append(mName).append('\n');
            return toRet.toString();
        }

        public void toICalendar(Writer w) throws IOException {
            toICalendar(w, false);
        }

        public void toICalendar(Writer w, boolean needAppleICalHacks) throws IOException {
            toICalendar(w, needAppleICalHacks, false);
        }

        public void toICalendar(Writer w, boolean needAppleICalHacks, boolean escapeHtmlTags) throws IOException {
            w.write("BEGIN:");
            w.write(mName);
            w.write(LINE_BREAK);
            
            for (ZProperty prop : mProperties) {
                // If we're dealing with Apple iCal, don't generate the X-ALT-DESC property.
                // iCal can't handle it when the value is too long.  (Exact threshold is unknown,
                // but we've seen the failure with a 19KB sample.)  iCal doesn't support x-props anyway,
                // so there is no loss of functionality.
                if (needAppleICalHacks && ICalTok.X_ALT_DESC.equals(prop.getToken()))
                    continue;
                prop.toICalendar(w, needAppleICalHacks, escapeHtmlTags);
            }

            for (ZComponent comp : mComponents)
                comp.toICalendar(w, needAppleICalHacks);
            
            w.write("END:");
            w.write(mName);
            w.write(LINE_BREAK);
        }

        public String getDescriptionHtml() {
            for (ZProperty prop : mProperties) {
                if (ICalTok.X_ALT_DESC.equals(prop.getToken())) {
                    ZParameter fmttype = prop.getParameter(ICalTok.FMTTYPE);
                    if (fmttype != null && MimeConstants.CT_TEXT_HTML.equalsIgnoreCase(fmttype.getValue()))
                        return prop.getValue();
                }
            }
            return null;
        }
    }

    // these are the characters that MUST be escaped: , ; " \n and \ -- note that \
    // becomes \\\\ here because it is double-unescaped during the compile process!
    private static final Pattern MUST_ESCAPE = Pattern.compile("[,;\n\\\\]");
    private static final Pattern SIMPLE_ESCAPE = Pattern.compile("([,;\\\\])");
    private static final Pattern NEWLINE_CRLF_ESCAPE = Pattern.compile("\r\n");
    private static final Pattern NEWLINE_BARE_CR_OR_LF_ESCAPE = Pattern.compile("[\r\n]");
    
    /**
     * ,;"\ and \n must all be escaped.  
     */
    public static String escape(String str) {
        if (str!= null && MUST_ESCAPE.matcher(str).find()) {
            // escape ([,;"])'s
            String toRet = SIMPLE_ESCAPE.matcher(str).replaceAll("\\\\$1");
            
            // escape
            toRet = NEWLINE_CRLF_ESCAPE.matcher(toRet).replaceAll("\\\\n");
            toRet = NEWLINE_BARE_CR_OR_LF_ESCAPE.matcher(toRet).replaceAll("\\\\n");
            return toRet;
        }

        return str;
    }
    
    private static final Pattern SIMPLE_ESCAPED = Pattern.compile("\\\\([,;\\\\])");
    private static final Pattern NEWLINE_ESCAPED = Pattern.compile("\\\\[nN]");


    public static String unescape(String str) {
        if (str != null && str.indexOf('\\') >= 0) {
            String toRet = SIMPLE_ESCAPED.matcher(str).replaceAll("$1"); 
            return NEWLINE_ESCAPED.matcher(toRet).replaceAll("\r\n"); 
        }
        return str;
    }

    public static String toCommaSepText(List<String> vals) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String val : vals) {
            if (first)
                first = false;
            else
                sb.append(",");
            sb.append(escape(val));
        }
        return sb.toString();
    }

    public static List<String> parseCommaSepText(String encoded) {
        List<String> list = new ArrayList<String>();
        int len;
        if (encoded != null && (len = encoded.length()) > 0) {
            int start = 0;
            char prev = encoded.charAt(0);
            for (int i = 0; i < len; i++) {
                char curr = encoded.charAt(i);
                if (curr == ',' && prev != '\\') {
                    String val = encoded.substring(start, i);
                    list.add(unescape(val));
                    start = i + 1;
                }
                prev = curr;
            }
            String val = encoded.substring(start);
            list.add(unescape(val));
        }
        return list;
    }


    /**
     * @author tim
     * 
     * Property has
     *    Name
     *    Parameters
     *    Value
     */
    public static class ZProperty 
    {
        public ZProperty(String name) {
            setName(name);
            mTok = ICalTok.lookup(mName);
        }
        public ZProperty(ICalTok tok) {
            mTok = tok;
            mName = tok.toString();
        }
        public ZProperty(ICalTok tok, String value) {
            mTok = tok;
            mName = tok.toString();
            setValue(value);
        }
        public ZProperty(ICalTok tok, boolean value) {
            mTok = tok;
            mName = tok.toString();
            mValue = value ? "TRUE" : "FALSE";
        }
        public ZProperty(ICalTok tok, long value) {
            mTok = tok;
            mName = tok.toString();
            mValue = Long.toString(value);
        }
        public ZProperty(ICalTok tok, int value) {
            mTok = tok;
            mName = tok.toString();
            mValue = Integer.toString(value);
        }
        
        public void setName(String name) {
            mName = name.toUpperCase();
        }
        public void setValue(String value) {
            mValue = value;
        }
        public void setValueList(List<String> valueList) {
            mValueList = valueList;
        }
        
        
        List<ZParameter> mParameters = new ArrayList<ZParameter>();
        
        public void addParameter(ZParameter param) { mParameters.add(param); }
        
        public ZParameter getParameter(ICalTok tok) { return findParameter(mParameters, tok); }
        public Iterator<ZParameter> parameterIterator() { return mParameters.iterator(); }
        public int getNumParameters() { return mParameters.size(); }
        
        String getParameterVal(ICalTok tok, String defaultValue) { 
            ZParameter param = findParameter(mParameters, tok); 
            if (param != null)
                return param.getValue();
            else
                return defaultValue;
        }
        
        public String paramVal(ICalTok tok, String defaultValue) { 
            ZParameter param = getParameter(tok);
            if (param != null) {
                return param.getValue();
            }
            return defaultValue;
        }
        

        public String toString() {
            return toString("");
        }
        
        public String toString(String INDENT) {
            StringBuffer toRet = new StringBuffer(INDENT).append("PROPERTY:").append(mName).append('(').append(mTok).append(')').append('\n');
            String NEW_INDENT = INDENT+'\t';
            for (ZParameter param: mParameters) {
                toRet.append(param.toString(NEW_INDENT));
            }
            toRet.append(NEW_INDENT).append("VALUE=\"").append(mValue).append("\"\n");
            toRet.append(INDENT).append("END:").append(mName).append('\n');
            return toRet.toString();
        }

        private static final int CHARS_PER_FOLDED_LINE = 76;

        public void toICalendar(Writer w) throws IOException {
            toICalendar(w, false);
        }
        public void toICalendar(Writer w, boolean needAppleICalHacks) throws IOException {
            toICalendar(w, needAppleICalHacks, false);
        }

        public void toICalendar(Writer w, boolean needAppleICalHacks, boolean escapeHtmlTags) throws IOException {
            StringWriter sw = new StringWriter();
            Pattern htmlPattern = Pattern.compile("<([^>]+)>");
            
            sw.write(mName);
            for (ZParameter param: mParameters)
                param.toICalendar(sw, needAppleICalHacks);

            sw.write(':');
            if (ICalTok.CATEGORIES.equals(mTok) || ICalTok.RESOURCES.equals(mTok)) {
                if (mValueList != null)
                    sw.write(toCommaSepText(mValueList));
            } else if (mValue != null) {
                String value = mValue;
                boolean noEscape = false;
                if (mTok != null) {
                    switch (mTok) {
                    case RRULE:
                    case EXRULE:
                    case RDATE:
                    case EXDATE:
                    case GEO:
                        noEscape = true;
                        break;
                    }
                    if (needAppleICalHacks && mTok.equals(ICalTok.TZID)) {
                        // bug 15549: Apple iCal refuses to work with anything other than Olson TZIDs.
                        value = TZIDMapper.canonicalize(value);
                    }
                }
                if (noEscape)
                	sw.write(value);
                else
    	            sw.write(escape(value));
            }

            // Write with folding.
            String rawval = sw.toString();

            if (escapeHtmlTags) { // Use only escapeHtmlTags when the file isn't supposed to be downloaded (ie. it's supposed to be shown in the browser)
                w.write(htmlPattern.matcher(rawval).replaceAll("&lt;$1&gt;"));
            } else {
                int len = rawval.length();
                for (int i = 0; i < len; i += CHARS_PER_FOLDED_LINE) {
                    int upto = Math.min(i + CHARS_PER_FOLDED_LINE, len);
                    String segment = rawval.substring(i, upto);
                    if (i > 0) {
                        w.write(LINE_BREAK);
                        w.write(' ');
                    }
                    w.write(segment);
                }
            }
            w.write(LINE_BREAK);
        }

        public ICalTok getToken() { return mTok; }  // may be null
        public String getName() { return mName; }
        public String getValue() { return mValue; }
        public List<String> getValueList() { return mValueList; }
        public long getLongValue() { return Long.parseLong(mValue); };
        public int getIntValue() { return Integer.parseInt(mValue); };
        public boolean getBoolValue() { return mValue.equalsIgnoreCase("TRUE"); }
        
        private ICalTok mTok;
        private String mName;
        private String mValue;
        private List<String> mValueList;  // used only for CATEGORIES and RESOURCES properties
    }
    
    /**
     * @author tim
     *
     * Name:Value pair
     */
    public static class ZParameter
    {
        public ZParameter(String name, String value) {
            setName(name);
            setValue(value);
            mTok = ICalTok.lookup(mName);
        }
        public ZParameter(ICalTok tok, String value) {
            mTok = tok;
            mName = tok.toString();
            setValue(value);
        }
        public ZParameter(ICalTok tok, boolean value) {
            mTok = tok;
            mName = tok.toString();
            maValue = value ? "TRUE" : "FALSE";
        }
        
        public void setName(String name) {
            mName = name.toUpperCase();
        }
        public void setValue(String value) {
            maValue = unquote(value);
        }

        public String toString() {
            return toString("");
        }
        
        public String toString(String INDENT) {
            StringBuffer toRet = new StringBuffer(INDENT).append("PARAM:").append(mName).append('(').append(mTok).append(')').append(':').append(maValue).append('\n');
            return toRet.toString();
        }

        public void toICalendar(Writer w) throws IOException {
            toICalendar(w, false);
        }

        public void toICalendar(Writer w, boolean needAppleICalHacks) throws IOException {
            w.write(';');
            w.write(mName);
            w.write('=');
            if (maValue == null || maValue.length()==0) {
                w.write("\"\""); // bug 4941: cannot put a completely blank parameter value, will confuse parsers
            } else if (ICalTok.TZID.equals(mTok)) {
                String value = maValue;
                if (needAppleICalHacks) {
                    // bug 15549: Apple iCal refuses to work with anything other than Olson TZIDs.
                    value = TZIDMapper.canonicalize(value);
                }

                // Microsoft Entourage 2004 (Outlook-like program for Mac)
                // insists on quoting TZID parameter value (but not TZID
                // property value).  It's an Entourage bug, but we have to
                // keep it happy with a hacky quoting policy.
                boolean entourageCompat = LC.calendar_entourage_compatible_timezones.booleanValue();
                w.write(quote(value, entourageCompat));
            } else {
                w.write(quote(maValue, false));
            }
        }
        
        public ICalTok getToken() { return mTok; }  // may be null
        public String getName() { return mName; }
        public String getValue() { return maValue; }
        long getLongValue() { return Long.parseLong(maValue); };
        int getIntValue() { return Integer.parseInt(maValue); };
        
        private ICalTok mTok;
        private String mName;
        private String maValue;

        /**
         * Sanitize a string to make it a valid param-value.  DQUOTE
         * is changed to a single quote and CTL chars are changed to question
         * marks ('?').  String is quoted if str is already quoted or if it
         * contains ',', ':' or ';'.
         * 
         * To workaround a bug in Outlook's MIME parser (see bug 12008), string
         * is quoted if any non-US-ASCII chars are present, e.g. non-English
         * names.  (These characters don't require quoting according to
         * RFC2445.)
         * 
         * Empty string is returned if str is null or is an empty string.
         * 
         * @param str
         * @return
         */
        private static String quote(String str, boolean force) {
            if (str == null) return "";
            int len = str.length();
            if (len == 0) return "";
            boolean needToQuote;
            int start, end;  // index of first and last char to examine
                             // end is last char + 1
            if (len >= 2 && str.charAt(0) == '"' && str.charAt(len - 1) == '"') {
                needToQuote = true;
                start = 1;
                end = len - 1;
            } else {
                needToQuote = false;
                start = 0;
                end = len;
            }
            StringBuilder sb = new StringBuilder(len + 2);
            sb.append('"');  // always start with quote
            for (int i = start; i < end; i++) {
                // Some chars require quoting, others require changing to a
                // valid char.  No char requires both.
                char ch = str.charAt(i);
                if ((ch >= 0x3C && ch <= 0x7E)    // "<=>?@ABC ..." (English letters)
                    || ch == 0x20                 // space
                    || (ch >= 0x2D && ch <= 0x39) // "-./0123456789"
                    || (ch >= 0x23 && ch <= 0x2B) // "#$%&'()*+"
                    || ch == 0x09                 // horizontal tab
                    || ch == 0x21) {              // '!'
                    // Char is okay.
                } else if (ch >= 0x80             // NON-US-ASCII and higher
                           || ch == 0x2C          // ','
                           || ch == 0x3A          // ':'
                           || ch == 0x3B) {       // ';'
                    // Chars 0x80 and above don't need to be quoted in RFC2445,
                    // but Outlook thinks differently. (bug 12008)
                    needToQuote = true;
                } else if (ch == 0x22) {          // '"'
                    // DQUOTE is not allowed.  Change to single quote.
                    ch = '\'';
                } else {
                    // ch is a CTL:
                    // 0x00 <= ch <= 0x08 or
                    // 0x0A <= ch <= 0x1F or
                    // ch == 0x7F
                    // CTL is invalid in a param-value, so change to a '?'.
                    ch = '?';
                }
                sb.append(ch);
            }
            sb.append('"');  // matches initial quote
            if (needToQuote || force)
                return sb.toString();
            else
                return sb.substring(1, sb.length() - 1);
        }

        public static String unquote(String str) {
            if (str != null && str.length() >= 2) {
                if ((str.charAt(0) == '\"') && (str.charAt(str.length() - 1) == '\"'))
                    return str.substring(1, str.length() - 1);
            }
            return str;
        }
    }

    static ZProperty findProp(List <ZProperty> list, ICalTok tok)
    {
        for (ZProperty prop : list) {
            if (prop.mTok == tok) {
                return prop;
            }
        }
        return null;
    }
    
    static ZParameter findParameter(List <ZParameter> list, ICalTok tok)
    {
        for (ZParameter param: list) {
            if (param.mTok == tok) {
                return param;
            }
        }
        return null;
    }
    
    static ZComponent findComponent(List <ZComponent> list, ICalTok tok)
    {
        for (ZComponent comp: list) {
            if (comp.mTok == tok) {
                return comp;
            }
        }
        return null;
    }
    
    public interface ZICalendarParseHandler extends ContentHandler {
        public boolean inZCalendar();
        public int getNumCals();
    }

    public static class DefaultContentHandler implements ZICalendarParseHandler {
        List<ZVCalendar> mCals = new ArrayList<ZVCalendar>(1);
        ZVCalendar mCurCal = null;
        List<ZComponent> mComponents = new ArrayList<ZComponent>();
        ZProperty mCurProperty = null;
        private int mNumCals;
        private boolean mInZCalendar;

        public List<ZVCalendar> getCals() { return mCals; }

        public void startCalendar() { 
            mInZCalendar = true;
            mCurCal = new ZVCalendar();
            mCals.add(mCurCal);
        }

        public void endCalendar() {
            mCurCal = null;
            mInZCalendar = false;
            mNumCals++;
        }

        public boolean inZCalendar() { return mInZCalendar; }
        public int getNumCals() { return mNumCals; }

        public void startComponent(String name) {
            ZComponent newComponent = new ZComponent(name);
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).mComponents.add(newComponent);
            } else {
                mCurCal.mComponents.add(newComponent); 
            }
            mComponents.add(newComponent);  
        }
        
        public void endComponent(String name) { 
            mComponents.remove(mComponents.size()-1);
        }

        public void startProperty(String name) {
            mCurProperty = new ZProperty(name);
            
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).mProperties.add(mCurProperty);
            } else {
                mCurCal.mProperties.add(mCurProperty);
            }
        }

        public void propertyValue(String value) throws ParserException {
            ICalTok token = mCurProperty.getToken();
            if (ICalTok.CATEGORIES.equals(token) || ICalTok.RESOURCES.equals(token))
                mCurProperty.setValueList(parseCommaSepText(value));
            else
                mCurProperty.setValue(unescape(value));
            if (mComponents.size() == 0) {
                if (ICalTok.VERSION.equals(mCurProperty.getToken())) {
                    if (sObsoleteVcalVersion.equals(value))
                        throw new ParserException("vCalendar 1.0 format not supported; use iCalendar instead");
                    if (!sIcalVersion.equals(value))
                        throw new ParserException("Unknow iCalendar version " + value);
                }
            }
        }

        public void endProperty(String name) { mCurProperty = null; }

        public void parameter(String name, String value) {
            ZParameter param = new ZParameter(name, value);
            if (mCurProperty != null) {
                mCurProperty.mParameters.add(param);
            } else {
                ZimbraLog.calendar.debug("ERROR: got parameter " + name + "," + value + " outside of Property");
            }
        }
    }

    public static class ZCalendarBuilder {

        public static ZVCalendar build(String icalStr) throws ServiceException {
            ByteArrayInputStream bais = null;
            try {
                bais = new ByteArrayInputStream(icalStr.getBytes(MimeConstants.P_CHARSET_UTF8));
            } catch (UnsupportedEncodingException e) {
                throw ServiceException.FAILURE("Can't get input stream from string", e);
            }
            try {
                return build(bais, MimeConstants.P_CHARSET_UTF8);
            } finally {
                try {
                    bais.close();
                } catch (IOException e) {}
            }
        }

        public static ZVCalendar build(InputStream is, String charset) throws ServiceException {
            List<ZVCalendar> list = buildMulti(is, charset);
            int len = list.size();
            if (len == 1) {
                return list.get(0);
            } else if (len > 1) {
                ZimbraLog.calendar.warn(
                        "Returning only the first ZCALENDAR after parsing " +
                        len);
                return list.get(0);
            } else {
                throw ServiceException.PARSE_ERROR("No ZCALENDAR found", null);
            }
        }

        public static List<ZVCalendar> buildMulti(InputStream is, String charset) throws ServiceException {
            DefaultContentHandler handler = new DefaultContentHandler();
            parse(is, charset, handler);
            return handler.getCals();
            
        }

        private static final byte[] BOM_UTF8 = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

        public static void parse(InputStream is, String charset, ZICalendarParseHandler handler) throws ServiceException {
            BufferedInputStream bis = new BufferedInputStream(is);
            // Skip UTF-8 Byte Order Mark.  (EF BB BF)
            if (MimeConstants.P_CHARSET_UTF8.equalsIgnoreCase(charset)) {
                byte[] buf = new byte[3];
                try {
                    boolean bomFound = false;
                    bis.mark(4);
                    if (bis.read(buf) == 3)
                        bomFound = buf[0] == BOM_UTF8[0] && buf[1] == BOM_UTF8[1] && buf[2] == BOM_UTF8[2];
                    if (!bomFound)
                        bis.reset();
                } catch (IOException e) {
                    throw ServiceException.FAILURE("Caught IOException during UTF-8 BOM check", e);
                }
            }
            bis.mark(32 * 1024);
            CalendarParser parser = new CalendarParserImpl();
            try {
                parser.parse(bis, charset, handler);
            } catch (IOException e) {
                throw ServiceException.FAILURE("Caught IOException parsing calendar: " + e, e);
            } catch (ParserException e) {
                StringBuilder s = new StringBuilder("Caught ParseException parsing calendar: " + e);
                try {
                    bis.reset();
                    byte[] ics = new byte[32 * 1024];
                    int bytesRead = bis.read(ics, 0, ics.length);
                    if (bytesRead > 0) {
                        String icsStr = new String(ics, 0, bytesRead, charset);
                        s.append(icsStr).append("\n");
                        if (bytesRead == ics.length) {
                            s.append("...\n");
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                ServiceException se = ServiceException.PARSE_ERROR(s.toString(), e);
                if (handler.inZCalendar() || handler.getNumCals() < 1) {
                    // Got parse error inside ZCALENDAR block.  Can't recover.
                    throw se;
                } else {
                    // Found garbage after END:ZCALENDAR.  Log warning and move on.
                    if (ZimbraLog.calendar.isDebugEnabled())
                        ZimbraLog.calendar.warn("Ignoring bad data at the end of text/calendar part: " + s.toString() , e);
                    else
                        ZimbraLog.calendar.warn("Ignoring bad data at the end of text/calendar part: " + e.getMessage());
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String str1 = ",foo,,bar,,b\\,az,,,";
        List<String> list = parseCommaSepText(str1);
        String str2 = toCommaSepText(list);
        if (!str1.equals(str2))
            System.err.println("Different!");

        try {
            /**
             * ,;"\ and \n must all be escaped.  
             */
            {
                String s;
                
                s = "This, is; my \"string\", and\\or \nI hope\r\nyou like it";
                System.out.println("Original: "+s+"\n\n\nEscaped: "+escape(s)+"\n\n\nUnescaped:"+unescape(escape(s)));

                System.out.println("\n\n\n");
                
                s = "\"Foo Bar Gub\"";
                System.out.println("Unquoted:"+s+"\nQuoted:"+ZParameter.unquote(s));
                
                System.out.println("\n\n\n");
                
                s = "Blah Bar Blah";
                System.out.println("Unquoted:"+s+"\nQuoted:"+ZParameter.unquote(s));
                System.out.println("\n\n\n");

                {
                    s =  "\"US & Canadia -- Foo\\Bar\"";
                    System.out.println("String = "+s);
                    ZParameter param = new ZParameter(ICalTok.TZID, s);
                    System.out.println("TZID   = "+param.getValue());
                    StringWriter writer = new StringWriter();
                    param.toICalendar(writer);
                    System.out.println("ICAL: "+writer.toString());
                    System.out.println("\n\n\n");
                }
                    
                
            }

            if (false) {
                File inFile = new File("c:\\test.ics");
                FileInputStream in = new FileInputStream(inFile);

                CalendarParser parser = new CalendarParserImpl();

                DefaultContentHandler handler = new DefaultContentHandler();
                parser.parse(in, "utf-8", handler);
                ZVCalendar cal = handler.getCals().get(0);
                System.out.println(cal.toString());
                Invite.createFromCalendar(null, null, cal, false);
            }
            
        } catch(Exception e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }
    }
}
