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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
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
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;

public class ZCalendar {
    
    public static final String sZimbraProdID = "Zimbra-Calendar-Provider";
    
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
        
        X_MICROSOFT_CDO_ALLDAYEVENT, X_MICROSOFT_CDO_BUSYSTATUS, X_MICROSOFT_CDO_INTENDEDSTATUS;        
        
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
            addProperty(new ZProperty(ICalTok.VERSION, "2.0"));
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
            w.write("BEGIN:VCALENDAR");
            w.write(LINE_BREAK);
            for (ZProperty prop : mProperties)
                prop.toICalendar(w);
            
            for (ZComponent comp : mComponents)
                comp.toICalendar(w);

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
        
        ZComponent getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        Iterator<ZComponent> getComponentIterator() { return mComponents.iterator(); }
        ZProperty getProperty(ICalTok tok) { return findProp(mProperties, tok); }
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
            w.write("BEGIN:");
            String name = escape(mName);
            w.write(name);
            w.write(LINE_BREAK);
            
            for (ZProperty prop : mProperties) 
                prop.toICalendar(w);

            for (ZComponent comp : mComponents) 
                comp.toICalendar(w);
            
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
        
        ZParameter getParameter(ICalTok tok) { return findParameter(mParameters, tok); }
        
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
            StringWriter sw = new StringWriter();
            
            sw.write(escape(mName));
            for (ZParameter param: mParameters)
                param.toICalendar(sw);

            sw.write(':');
            if (mName.equals(ICalTok.RRULE.toString()) ||
                mName.equals(ICalTok.EXRULE.toString()))
            	sw.write(mValue);
            else
	            sw.write(escape(mValue));

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
        
        String getValue() { return mValue; }
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
        ZParameter(String name, String value) {
            setName(name);
            setValue(value);
            mTok = ICalTok.lookup(mName);
        }
        ZParameter(ICalTok tok, String value) {
            mTok = tok;
            mName = tok.toString();
            setValue(value);
        }
        ZParameter(ICalTok tok, boolean value) {
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
            w.write(';');
            w.write(escape(mName));
            w.write('=');
            if (maValue == null || maValue.length()==0) {
                w.write("\"\""); // bug 4941: cannot put a completely blank parameter value, will confuse parsers
            } else if (maValue.startsWith("\"") && maValue.endsWith("\"")) {
                w.write('\"');
                w.write(escape(maValue.substring(1, maValue.length()-1)));
                w.write('\"');
            } else if (ICalTok.TZID.equals(mTok)) {
                // Microsoft Entourage 2004 (Outlook-like program for Mac)
                // insists on quoting TZID parameter value (but not TZID
                // property value).  It's an Entourage bug, but we have to
                // keep it happy with a hacky quoting policy.
                w.write('\"');
                w.write(maValue);
                w.write('\"');
            } else {
                w.write(quote(maValue));
            }
        }
        
        String getValue() { return maValue; }
        long getLongValue() { return Long.parseLong(maValue); };
        int getIntValue() { return Integer.parseInt(maValue); };
        
        ICalTok mTok;
        String mName;
        String maValue;
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
        ZVCalendar mCal = null;
        ArrayList<ZComponent> mComponents = new ArrayList<ZComponent>();
        ZProperty mCurProperty = null;
        
        public void startCalendar() { 
            mCal = new ZVCalendar(); 
        }
        
        public void endCalendar() { }

        public void startComponent(String name) {
            ZComponent newComponent = new ZComponent(name);
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).mComponents.add(newComponent);
            } else {
                mCal.mComponents.add(newComponent); 
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
                mCal.mProperties.add(mCurProperty);
            }
        }

        public void propertyValue(String value) throws URISyntaxException, ParseException, IOException { 
            mCurProperty.mValue = value;
        }

        public void endProperty(String name) { mCurProperty = null; }

        public void parameter(String name, String value) throws URISyntaxException { 
            ZParameter param = new ZParameter(name, value);
            if (mCurProperty != null) {
                mCurProperty.mParameters.add(param);
            } else {
                ZimbraLog.calendar.debug("ERROR: got parameter "+name+","+value+" outside of Property");
            }
        }
    }
    
    public static class ZCalendarBuilder
    {
        public static ZVCalendar build(Reader reader) throws ServiceException {
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
            } catch(IOException e) {
                throw ServiceException.FAILURE("Caught IOException parsing calendar: "+e, e);
            } catch(ParserException e) {
                StringBuilder s = new StringBuilder("Caught ParseException parsing calendar: "+e);
                try {
                    reader.reset();
                    
                    Reader r = new UnfoldingReader(reader);
                    s.append('\n');
                    while(r.ready()) {
                        s.append((char)(r.read()));
                    }
                } catch(IOException ioe) {
                    ioe.printStackTrace();
                }
                
                throw ServiceException.FAILURE(s.toString(), e);
            }
            
            return handler.mCal;
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
            System.out.println(handler.mCal.toString());
//            createFromCalendar(handler.mCal);
            Invite.createFromCalendar(null, null, handler.mCal, false);
            }
            
        } catch(Exception e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }

    }
}
