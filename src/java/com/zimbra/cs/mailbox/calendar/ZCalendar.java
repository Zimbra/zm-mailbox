package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.zimbra.cs.service.ServiceException;

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
        RANGE, RDATE, RECUR, RECURRENCE_ID, RELATED, RELATED_TO,
        RELTYPE, REPEAT, RESOURCES, ROLE, RRULE, RSVP,
        SENT_BY, SEQUENCE, STATUS, SUMMARY, TEXT, TIME,
        TRANSP, TRIGGER, TZID, TZNAME, TZOFFSETFROM,
        TZOFFSETTO, TZURL, UID, URI, URL, UTC_OFFSET,
        VALARM, VALUE, VERSION, VEVENT, VFREEBUSY, VJOURNAL,
        VTIMEZONE, VTODO, 
        
        PUBLISH, REQUEST, REPLY, ADD, CANCEL, REFRESH, COUNTER, DECLINECOUNTER,
        
        STANDARD, DAYLIGHT,
        
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
    
    
    /**
     * @author tim
     * 
     * Calendar has
     *    Components
     *    Properties
     */
    public static class ZVCalendar
    {
        List <ZComponent> mComponents = new ArrayList();
        List <ZProperty> mProperties = new ArrayList();
        
        public ZVCalendar() { 
            addProperty(new ZProperty(ICalTok.PRODID, sZimbraProdID));
            addProperty(new ZProperty(ICalTok.VERSION, "2.0"));
        }
        
        public void addProperty(ZProperty prop) { mProperties.add(prop); }
        public void addComponent(ZComponent comp) { mComponents.add(comp); }
        
        public ZComponent getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
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
            StringBuffer toRet = new StringBuffer("BEGIN:VCALENDAR\n");
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
            w.write("BEGIN:VCALENDAR\n");
            for (ZProperty prop : mProperties)
                prop.toICalendar(w);
            
            for (ZComponent comp : mComponents)
                comp.toICalendar(w);

            w.write("END:VCALENDAR");
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
        private ICalTok mTok;
        
        public String getName() { return mName; }
        public ICalTok getTok() { return mTok; }
        
        List <ZProperty> mProperties = new ArrayList();
        List <ZComponent> mComponents = new ArrayList();
        
        public void addProperty(ZProperty prop) { mProperties.add(prop); }
        public void addComponent(ZComponent comp) { mComponents.add(comp); }
        
        ZComponent getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        ZProperty getProperty(ICalTok tok) { return findProp(mProperties, tok); }
        String getPropVal(ICalTok tok, String defaultValue) {
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
            w.write('\n');
            
            for (ZProperty prop : mProperties) 
                prop.toICalendar(w);

            for (ZComponent comp : mComponents) 
                comp.toICalendar(w);
            
            w.write("END:");
            w.write(name);
            w.write('\n');
        }        
    }

    private static final Pattern CHECK_ESCAPE = Pattern.compile("[,;\"\n\\\\]");
    private static final Pattern CHECK_UNESCAPE = Pattern.compile("\\\\");
    private static final Pattern ESCAPE_PATTERN_1 = Pattern.compile("([,;\"])");
    private static final Pattern ESCAPE_PATTERN_2 = Pattern.compile("[\r\n]+");
    private static final Pattern ESCAPE_PATTERN_3 = Pattern.compile("\\\\");
    
    /**
     * Convenience method for escaping special characters.
     * @param aValue a string value to escape
     * @return an escaped representation of the specified
     * string
     */
    public static String escape(final String aValue) {
        if (aValue != null && CHECK_ESCAPE.matcher(aValue).find()) {
            return ESCAPE_PATTERN_1.matcher(
                    ESCAPE_PATTERN_2.matcher(
                            ESCAPE_PATTERN_3.matcher(aValue).replaceAll("\\\\\\\\"))
                        .replaceAll("\\\\n"))
                .replaceAll("\\\\$1");
        }

        return aValue;
    }
    
    private static final Pattern UNESCAPE_PATTERN_1 = Pattern.compile("\\\\([,;\"])");
    private static final Pattern UNESCAPE_PATTERN_2 = Pattern.compile("\\\\n", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNESCAPE_PATTERN_3 = Pattern.compile("\\\\\\\\");
    
    /**
     * Convenience method for replacing escaped special characters
     * with their original form.
     * @param aValue a string value to unescape
     * @return a string representation of the specified
     * string with escaped characters replaced with their
     * original form
     */
    public static String unescape(final String aValue) {
        if (aValue != null && CHECK_UNESCAPE.matcher(aValue).find()) {
            return UNESCAPE_PATTERN_3.matcher(
                    UNESCAPE_PATTERN_2.matcher(
                            UNESCAPE_PATTERN_1.matcher(aValue).replaceAll("$1"))
                        .replaceAll("\n"))
                .replaceAll("\\\\");
        }
        return aValue;
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
        
        
        List <ZParameter> mParameters = new ArrayList();
        
        public void addParameter(ZParameter param) { mParameters.add(param); }
        
        ZParameter getParameter(ICalTok tok) { return findParameter(mParameters, tok); }
        
        public String paramVal(ICalTok tok, String defaultValue) { 
            ZParameter param = getParameter(tok);
            if (param != null) {
                return param.mValue;
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
        
        public void toICalendar(Writer w) throws IOException {
            w.write(escape(mName));
            for (ZParameter param: mParameters)
                param.toICalendar(w);

            w.write(':');
            w.write(escape(mValue));
            w.write('\n');
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
            mValue = value ? "TRUE" : "FALSE";
        }
        
        public void setName(String name) {
            mName = unescape(name.toUpperCase());
        }
        public void setValue(String value) {
            mValue = unescape(value);
        }

        public String toString() {
            return toString("");
        }
        
        public String toString(String INDENT) {
            StringBuffer toRet = new StringBuffer(INDENT).append("PARAM:").append(mName).append('(').append(mTok).append(')').append(':').append(mValue).append('\n');
            return toRet.toString();
        }
        public void toICalendar(Writer w) throws IOException {
            w.write(';');
            w.write(escape(mName));
            w.write('=');
            if (mValue.startsWith("\"") && mValue.endsWith("\"")) {
                w.write('\"');
                w.write(escape(mValue.substring(1, mValue.length()-1)));
                w.write('\"');
            } else {
                w.write(escape(mValue));
            }
        }
        
        String getValue() { return mValue; }
        long getLongValue() { return Long.parseLong(mValue); };
        int getIntValue() { return Integer.parseInt(mValue); };
        
        ICalTok mTok;
        String mName;
        String mValue;
    }
    
    private static ZProperty findProp(List <ZProperty> list, ICalTok tok)
    {
        for (ZProperty prop : list) {
            if (prop.mTok == tok) {
                return prop;
            }
        }
        return null;
    }
    
    private static ZParameter findParameter(List <ZParameter> list, ICalTok tok)
    {
        for (ZParameter param: list) {
            if (param.mTok == tok) {
                return param;
            }
        }
        return null;
    }
    
    private static ZComponent findComponent(List <ZComponent> list, ICalTok tok)
    {
        for (ZComponent comp: list) {
            if (comp.mTok == tok) {
                return comp;
            }
        }
        return null;
    }

    private static class ZContentHandler implements ContentHandler
    {
        ZVCalendar mCal = null;
        ArrayList<ZComponent> mComponents = new ArrayList();
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
                System.out.println("ERROR: got parameter "+name+","+value+" outside of Property");
            }
        }
    }
    
    public static class ZCalendarBuilder
    {
        public static ZVCalendar build(Reader reader) throws ServiceException {
            CalendarParser parser = new CalendarParserImpl();
            
            ZContentHandler handler = new ZContentHandler();
            
            try {
                parser.parse(new UnfoldingReader(reader), handler);
            } catch(IOException e) {
                throw ServiceException.FAILURE("Caught IOException parsing calendar: "+e, e);
            } catch(ParserException e) {
                throw ServiceException.FAILURE("Caught ParseException parsing calendar: "+e, e);
            }
            
            return handler.mCal;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            File inFile = new File("c:\\test.ics");
            FileReader in = new FileReader(inFile);
            
            CalendarParser parser = new CalendarParserImpl();
            
            ZContentHandler handler = new ZContentHandler();
            parser.parse(new UnfoldingReader(in), handler);
            System.out.println(handler.mCal.toString());
//            createFromCalendar(handler.mCal);
            Invite.createFromCalendar(null, null, handler.mCal, false);
            
        } catch(Exception e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }

    }
}
