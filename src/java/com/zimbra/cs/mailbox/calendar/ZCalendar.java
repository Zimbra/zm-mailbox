/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.mailbox.calendar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;

public class ZCalendar {
    
    public static final String sZimbraProdID = "Zimbra-Calendar-Provider";
    public static final String sIcalVersion = "2.0";
    
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
        
        X_MICROSOFT_CDO_ALLDAYEVENT, X_MICROSOFT_CDO_BUSYSTATUS, X_MICROSOFT_CDO_INTENDEDSTATUS,

        // ZCO Custom values
        X_ZIMBRA_STATUS, X_ZIMBRA_STATUS_WAITING, X_ZIMBRA_STATUS_DEFERRED,
        X_ZIMBRA_PARTSTAT_WAITING, X_ZIMBRA_PARTSTAT_DEFERRED;        

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

        public void toICalendar(Writer w, boolean forceOlsonTZID) throws IOException {
            w.write("BEGIN:VCALENDAR");
            w.write(LINE_BREAK);
            for (ZProperty prop : mProperties)
                prop.toICalendar(w, forceOlsonTZID);
            
            for (ZComponent comp : mComponents)
                comp.toICalendar(w, forceOlsonTZID);

            w.write("END:VCALENDAR");
        }

        // Add DESCRIPTION property to components that take that property,
        // if the property is not set.
        public void addDescription(String desc) {
            if (desc == null || desc.length() < 1) return;
            ZProperty descProp = new ZProperty(ICalTok.DESCRIPTION, desc);
            for (ZComponent comp : mComponents) {
                ICalTok name = comp.getTok();
                if (ICalTok.VEVENT.equals(name) ||
                    ICalTok.VTODO.equals(name) ||
                    ICalTok.VJOURNAL.equals(name)) {
                    ZProperty prop = comp.getProperty(ICalTok.DESCRIPTION);
                    if (prop == null) {
                        comp.addProperty(descProp);
                    } else {
                        String val = prop.getValue();
                        if (val == null || val.length() < 1)
                            prop.setValue(desc);
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
        ZComponent(String name) {
            mName = name.toUpperCase();
            mTok = ICalTok.lookup(mName);
        }
        
        ZComponent(ICalTok tok) {
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

        public void toICalendar(Writer w, boolean forceOlsonTZID) throws IOException {
            w.write("BEGIN:");
            String name = escape(mName);
            w.write(name);
            w.write(LINE_BREAK);
            
            for (ZProperty prop : mProperties) 
                prop.toICalendar(w, forceOlsonTZID);

            for (ZComponent comp : mComponents) 
                comp.toICalendar(w, forceOlsonTZID);
            
            w.write("END:");
            w.write(name);
            w.write(LINE_BREAK);
        }        
    }

    // these are the characters that MUST be escaped: , ; " \n and \ -- note that \
    // becomes \\\\ here because it is double-unescaped during the compile process!
    private static final Pattern MUST_ESCAPE = Pattern.compile("[,;\"\n\\\\]");
    private static final Pattern SIMPLE_ESCAPE = Pattern.compile("([,;\"\\\\])");
    private static final Pattern NEWLINE_ESCAPE = Pattern.compile("[\r\n]");
    
    /**
     * ,;"\ and \n must all be escaped.  
     */
    public static String escape(String str) {
        if (str!= null && MUST_ESCAPE.matcher(str).find()) {
            // escape ([,;"])'s
            String toRet = SIMPLE_ESCAPE.matcher(str).replaceAll("\\\\$1");
            
            // escape
            return NEWLINE_ESCAPE.matcher(toRet).replaceAll("\\\\n");
            
        }

        return str;
    }
    
    private static final Pattern SIMPLE_ESCAPED = Pattern.compile("\\\\([,;\"\\\\])");
    private static final Pattern NEWLINE_ESCAPED = Pattern.compile("\\\\n");


    public static String unescape(String str) {
        if (str != null && str.indexOf('\\') >= 0) {
            String toRet = SIMPLE_ESCAPED.matcher(str).replaceAll("$1"); 
            return NEWLINE_ESCAPED.matcher(toRet).replaceAll("\n"); 
        }
        return str;
    }


    // From RFC2445, Section 4.1 Content Lines:
    //
    // param-value = paramtext / quoted-string
    // paramtext = *SAFE-CHAR
    // quoted-string = DQUOTE *QSAFE-CHAR DQUOTE
    // NON-US-ASCII = %x80-F8
    // QSAFE-CHAR = WSP / %x21 / %x23-7E / NON-US-ASCII
    // ; Any character except CTLs and DQUOTE
    // SAFE-CHAR  = WSP / %x21 / %x23-2B / %x2D-39 / %x3C-7E
    //              / NON-US-ASCII
    // ; Any character except CTLs, DQUOTE, ";", ":", ","
    // CTL = %x00-08 / %x0A-1F / %x7F
    //
    // Thus a parameter value cannot contain CTLs or DQUOTE.
    // When a value has to be quoted, there is no need to escape
    // DQUOTE because it may not occur in the value.
    //
    private static final Pattern MUST_QUOTE = Pattern.compile("[;:,]");

    public static String quote(String str) {
        if (str != null && MUST_QUOTE.matcher(str).find())
        	return "\"" + str + "\"";
        else
	        return str;
    }

    public static String unquote(String str) {
        if (str != null && str.length()>2) {
            if ((str.charAt(0) == '\"') && (str.charAt(str.length()-1) == '\"'))
                return str.substring(1, str.length()-1);
        }
        return str;
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
            mName = unescape(name.toUpperCase());
        }
        public void setValue(String value) {
            mValue = unescape(value);
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
                return unquote(param.getValue());
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

        public void toICalendar(Writer w, boolean forceOlsonTZID) throws IOException {
            StringWriter sw = new StringWriter();
            
            sw.write(escape(mName));
            for (ZParameter param: mParameters)
                param.toICalendar(sw, forceOlsonTZID);

            sw.write(':');
            if (mValue != null) {
                String value = mValue;
                boolean noEscape = false;
                if (mTok != null) {
                    switch (mTok) {
                    case RRULE:
                    case EXRULE:
                    case RDATE:
                    case EXDATE:
                        noEscape = true;
                        break;
                    }
                    if (forceOlsonTZID && mTok.equals(ICalTok.TZID)) {
                        // bug 15549: Apple iCal refuses to work with anything other than Olson TZIDs.
                        value = TZIDMapper.toOlson(value);
                    }
                }
                if (noEscape)
                	sw.write(value);
                else
    	            sw.write(escape(value));
            }

            // Write with folding.
            String rawval = sw.toString();
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
            w.write(LINE_BREAK);
        }

        public ICalTok getToken() { return mTok; }  // may be null
        public String getName() { return mName; }
        public String getValue() { return mValue; }
        long getLongValue() { return Long.parseLong(mValue); };
        int getIntValue() { return Integer.parseInt(mValue); };
        boolean getBoolValue() { return mValue.equalsIgnoreCase("TRUE"); }
        
        ICalTok mTok;
        String mName;
        String mValue;
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
            mName = unescape(name.toUpperCase());
        }
        public void setValue(String value) {
            maValue = unescape(unquote(value));
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

        public void toICalendar(Writer w, boolean forceOlsonTZID) throws IOException {
            w.write(';');
            w.write(escape(mName));
            w.write('=');
            if (maValue == null || maValue.length()==0) {
                w.write("\"\""); // bug 4941: cannot put a completely blank parameter value, will confuse parsers
            } else if (ICalTok.CN.equals(mTok)) {
                // Outlook special:
                // Outlook's MIME parser chokes when CN value containing
                // characters with high bit set isn't quoted, even though it is
                // not necessary to quote according to RFC2445.
                w.write(sanitizeParamValue(maValue));
            } else if (maValue.startsWith("\"") && maValue.endsWith("\"")) {
                w.write('\"');
                w.write(escape(maValue.substring(1, maValue.length()-1)));
                w.write('\"');
            } else if (ICalTok.TZID.equals(mTok)) {
                String value = maValue;
                if (forceOlsonTZID) {
                    // bug 15549: Apple iCal refuses to work with anything other than Olson TZIDs.
                    value = TZIDMapper.toOlson(value);
                }

                // Microsoft Entourage 2004 (Outlook-like program for Mac)
                // insists on quoting TZID parameter value (but not TZID
                // property value).  It's an Entourage bug, but we have to
                // keep it happy with a hacky quoting policy.
                boolean entourageCompat = LC.calendar_entourage_compatible_timezones.booleanValue();
                if (entourageCompat) {
                    w.write('\"');
                    w.write(value);
                    w.write('\"');
                } else {
                    w.write(quote(value));
                }
            } else {
                w.write(quote(maValue));
            }
        }
        
        public ICalTok getToken() { return mTok; }  // may be null
        public String getName() { return mName; }
        public String getValue() { return maValue; }
        long getLongValue() { return Long.parseLong(maValue); };
        int getIntValue() { return Integer.parseInt(maValue); };
        
        ICalTok mTok;
        String mName;
        String maValue;

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
        private static String sanitizeParamValue(String str) {
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
            if (needToQuote)
                return sb.toString();
            else
                return sb.substring(1, sb.length() - 1);
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
    
//    private static class UnfoldingReader : extends FilterReader
//    {
//        Reader mIn;
//        
//        char[] mBuf = new char[3];
//        
//        boolean buffering = false;
//        boolean atNl = false;
//        int bufPos = 0;
//        
//        
//        
//        UnfoldingReader(Reader in) {
//            mIn = in;
//        }
//        
//        int read() {
//            int read = read();
//            
//            // buffering?
//            //    yes:
//            //       at nl?
//            //         yes:
//            //           is space?
//            //             yes:
//            //               eat 1 space, done buffering
//            //             no:
//            //               don't eat, done buffering
//            //         no:
//            //           is it a '\n'?
//            //              yes: 
//            //                 at nl=true.  still buffering
//            //              no:
//            //                 don't eat.  done buffering
//            //    no:
//            //      is it a '\r'?
//            //          buffering, not at nl
//            //      is it a '\n'?
//            //          buffering.  at nl
//            //      return it
//            //
//            if (buffering) {
//                if (atNl) {
//                    if ((char)read == ' ') {
//                        
//                    }
//                }
//            } else {
//                
//            }
//            
//            
//        }
//        
//        int read(char[] cbuf, int off, int len) {
//            
//            for (int i = 0; i < len; i++) {
//                int read = read();
//                if (read == -1)
//                    return i;
//                
//                cbuf[i+off] = (char)read;
//            }
//            return len;
//        }
//        
//        
//    }
//    
//    
//    private static class Parser 
//    {
//        ZVCalendar mCal = null;
//        ArrayList<ZComponent> mComponents = new ArrayList();
//        ZProperty mCurProperty = null;
//        StringBuffer curLine = null;
//        
//        StreamTokenizer mTokenizer;
//        
//n        static enum Token {
//            BEGIN, VCALENDAR, END, COLON;
//
//            Token lookup(String str) {
//                if (str.equals(":")) 
//                    return COLON;
//                
//                return Token.valueOf(str);
//            }
//        }
//
//        static ZVCalendar parse(Reader in) throws ServiceException {
//            Parser p = new Parser(in);
//            return p.mCal;
//        }
//        
//        private Parser(Reader in) throws ServiceException {
//            mTokenizer = new StreamTokenizer(in);
//            mTokenizer.wordChars(32, 127);
//            mTokenizer.whitespaceChars(0, 20);
//            mTokenizer.eolIsSignificant(true);
//            mTokenizer.quoteChar('"');
//            mTokenizer.ordinaryChar(';');
//            mTokenizer.ordinaryChar(':');
//            mTokenizer.ordinaryChar('=');
//        }
//        
//        private int nextToken()
//        {
//            int toRet = nextToken();
//            
//        }
//        
//        private void parseError(String expected) throws ServiceException 
//        {
//            throw ServiceException.PARSE_ERROR("Expected \""+expected+"\" at l, cause)
//        }
//       
//        private void expectToken(String token) throws ServiceException
//        {
//            if (tokeniser.nextToken() != StreamTokenizer.TT_WORD) {
//                throw ServiceException.PARSE_ERROR("Expected \""+token, cause)
//            
//        }
//            
//            
//        
//        private void expectToken(Integer ch) throws ServiceException
//        {
//
//        }
//        
//        
//    }

    private static class ZContentHandler implements ContentHandler
    {
        List<ZVCalendar> mCals = new ArrayList<ZVCalendar>(1);
        ZVCalendar mCurCal = null;
        List<ZComponent> mComponents = new ArrayList<ZComponent>();
        ZProperty mCurProperty = null;
        private int mNumCals;
        private boolean mInZCalendar;

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

        public void propertyValue(String value) { 
            mCurProperty.mValue = value;
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
        public static ZVCalendar build(Reader reader) throws ServiceException {
            List<ZVCalendar> list = buildMulti(reader);
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

        public static List<ZVCalendar> buildMulti(Reader reader) throws ServiceException {
            BufferedReader br = new BufferedReader(reader);
            reader = br;
            try {
                reader.mark(32000);
            } catch(IOException e) {
                e.printStackTrace();
            }

            CalendarParser parser = new CalendarParserImpl();
            ZContentHandler handler = new ZContentHandler();
            try {
                parser.parse(new UnfoldingReader(reader), handler);
            } catch (IOException e) {
                throw ServiceException.FAILURE("Caught IOException parsing calendar: " + e, e);
            } catch (ParserException e) {
                StringBuilder s = new StringBuilder("Caught ParseException parsing calendar: " + e);
                try {
                    reader.reset();
                    s.append('\n');
                    int charRead;
                    while ((charRead = reader.read()) != -1)
                        s.append((char) charRead);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                ServiceException se = ServiceException.PARSE_ERROR(s.toString(), e);
                if (handler.inZCalendar() || handler.getNumCals() < 1) {
                    // Got parse error inside ZCALENDAR block.  Can't recover.
                    throw se;
                } else {
                    // Found garbage after END:ZCALENDAR.  Log warning and move on.
                    ZimbraLog.calendar.warn("Ignoring bad data at the end of text/calendar part: " + s.toString() , e);
                }
            }

            return handler.mCals;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
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
                System.out.println("Unquoted:"+s+"\nQuoted:"+unquote(s));
                
                System.out.println("\n\n\n");
                
                s = "Blah Bar Blah";
                System.out.println("Unquoted:"+s+"\nQuoted:"+unquote(s));
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
                FileReader in = new FileReader(inFile);

                CalendarParser parser = new CalendarParserImpl();

                ZContentHandler handler = new ZContentHandler();
                parser.parse(new UnfoldingReader(in), handler);
                ZVCalendar cal = handler.mCals.get(0);
                System.out.println(cal.toString());
                Invite.createFromCalendar(null, null, cal, false);
            }
            
        } catch(Exception e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }

    }
}
