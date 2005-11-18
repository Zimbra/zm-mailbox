package com.zimbra.cs.mailbox.calendar;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.service.ServiceException;

import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserImpl;
import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.FoldingWriter;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.RDate;

public class ZCalendar {
    
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
        
        PUBLISH, REQUEST,
        
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
    private static class Calendar
    {
        List <Component> mComponents = new ArrayList();
        List <Property> mProperties = new ArrayList();
        
        Calendar() { }
        
        public void addProperty(Property prop) { mProperties.add(prop); }
        public void addComponent(Component comp) { mComponents.add(comp); }
        
        Component getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        Property getProperty(ICalTok tok) { return findProp(mProperties, tok); }
        String getPropVal(ICalTok tok, String defaultValue) {
            Property prop = getProperty(tok);
            if (prop != null) 
                return prop.mValue;
            
            return defaultValue;
        }
        long getPropLongVal(ICalTok tok, long defaultValue) {
            Property prop = getProperty(tok);
            if (prop != null) 
                return Long.parseLong(prop.mValue);
            
            return defaultValue;
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer("BEGIN:VCALENDAR\n");
            String INDENT = "\t";
            for (Property prop : mProperties) {
                toRet.append(prop.toString(INDENT));
            }
            
            for (Component comp : mComponents) {
                toRet.append(comp.toString(INDENT));
            }
            toRet.append("END:VCALENDAR");
            return toRet.toString();
        }
        
        public void toICalendar(Writer w) throws IOException {
            w.write("BEGIN:VCALENDAR\n");
            for (Property prop : mProperties)
                prop.toICalendar(w);
            
            for (Component comp : mComponents)
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
    private static class Component
    {
        Component(String name) {
            mName = name.toUpperCase();
            mTok = ICalTok.lookup(mName);
        }
        
        Component(ICalTok tok) {
            mTok = tok;
            mName = tok.toString();
        }
        
        private String mName;
        private ICalTok mTok;
        
        public String getName() { return mName; }
        public ICalTok getTok() { return mTok; }
        
        List <Property> mProperties = new ArrayList();
        List <Component> mComponents = new ArrayList();
        
        public void addProperty(Property prop) { mProperties.add(prop); }
        public void addComponent(Component comp) { mComponents.add(comp); }
        
        Component getComponent(ICalTok tok) { return findComponent(mComponents, tok); }
        Property getProperty(ICalTok tok) { return findProp(mProperties, tok); }
        String getPropVal(ICalTok tok, String defaultValue) {
            Property prop = getProperty(tok);
            if (prop != null) 
                return prop.mValue;
            
            return defaultValue;
        }
        long getPropLongVal(ICalTok tok, long defaultValue) {
            Property prop = getProperty(tok);
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
            for (Property prop : mProperties) {
                toRet.append(prop.toString(NEW_INDENT));
            }
            for (Component comp : mComponents) {
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
            
            for (Property prop : mProperties) 
                prop.toICalendar(w);

            for (Component comp : mComponents) 
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
    private static class Property 
    {
        Property(String name) {
            setName(name);
            mTok = ICalTok.lookup(mName);
        }
        Property(ICalTok tok) {
            mTok = tok;
            mName = tok.toString();
        }
        Property(ICalTok tok, String value) {
            mTok = tok;
            mName = tok.toString();
            setValue(value);
        }
        Property(ICalTok tok, boolean value) {
            mTok = tok;
            mName = tok.toString();
            mValue = value ? "TRUE" : "FALSE";
        }
        Property(ICalTok tok, long value) {
            mTok = tok;
            mName = tok.toString();
            mValue = Long.toString(value);
        }
        Property(ICalTok tok, int value) {
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
        
        
        List <Parameter> mParameters = new ArrayList();
        
        public void addParameter(Parameter param) { mParameters.add(param); }
        
        Parameter getParameter(ICalTok tok) { return findParameter(mParameters, tok); }
        
        public String paramVal(ICalTok tok, String defaultValue) { 
            Parameter param = getParameter(tok);
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
            for (Parameter param: mParameters) {
                toRet.append(param.toString(NEW_INDENT));
            }
            toRet.append(NEW_INDENT).append("VALUE=\"").append(mValue).append("\"\n");
            toRet.append(INDENT).append("END:").append(mName).append('\n');
            return toRet.toString();
        }
        
        public void toICalendar(Writer w) throws IOException {
            w.write(escape(mName));
            for (Parameter param: mParameters)
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
    private static class Parameter
    {
        Parameter(String name, String value) {
            setName(name);
            setValue(value);
            mTok = ICalTok.lookup(mName);
        }
        Parameter(ICalTok tok, String value) {
            mTok = tok;
            mName = tok.toString();
            setValue(value);
        }
        Parameter(ICalTok tok, boolean value) {
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
    
    private static Property findProp(List <Property> list, ICalTok tok)
    {
        for (Property prop : list) {
            if (prop.mTok == tok) {
                return prop;
            }
        }
        return null;
    }
    
    private static Parameter findParameter(List <Parameter> list, ICalTok tok)
    {
        for (Parameter param: list) {
            if (param.mTok == tok) {
                return param;
            }
        }
        return null;
    }
    
    private static Component findComponent(List <Component> list, ICalTok tok)
    {
        for (Component comp: list) {
            if (comp.mTok == tok) {
                return comp;
            }
        }
        return null;
    }

    private static class ZContentHandler implements ContentHandler
    {
        Calendar mCal = null;
        ArrayList<Component> mComponents = new ArrayList();
        Property mCurProperty = null;
        
        public void startCalendar() { 
            mCal = new Calendar(); 
        }
        
        public void endCalendar() { }

        public void startComponent(String name) {
//            System.out.println("StartComponent("+name+")");
            Component newComponent = new Component(name);
            if (mComponents.size() > 0) {
                mComponents.get(mComponents.size()-1).mComponents.add(newComponent);
            } else {
                mCal.mComponents.add(newComponent); 
            }
            mComponents.add(newComponent);  
        }
        
        public void endComponent(String name) { 
//            System.out.println("EndComponent("+name+")");
            mComponents.remove(mComponents.size()-1);
        }

        public void startProperty(String name) { 
            mCurProperty = new Property(name);
            
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
            Parameter param = new Parameter(name, value);
            if (mCurProperty != null) {
                mCurProperty.mParameters.add(param);
            } else {
                System.out.println("ERROR: got parameter "+name+","+value+" outside of Property");
            }
        }
    }
    
    private static final long MSEC_PER_HOUR = 1000 * 60 * 60;
    private static final long MSEC_PER_MIN = 1000 * 60;
    private static final long MSEC_PER_SEC = 1000;    
    
    /**
     * For TZOFFSETTO: [+-]HHMM(SS)?
     * @param utcOffset
     * @return
     */
    static int tzOffsetToTime(String utcOffset) {
        int toRet = 0;
        
        toRet += (Integer.parseInt(utcOffset.substring(1,3)) * MSEC_PER_HOUR);
        toRet += (Integer.parseInt(utcOffset.substring(3,5)) * MSEC_PER_MIN);
        if (utcOffset.length() >= 7) {
            toRet += (Integer.parseInt(utcOffset.substring(5,7)) * MSEC_PER_SEC);
        }
        if (utcOffset.charAt(0) == '-') {
            toRet *= -1;
        }
        return toRet;
    }
    
    static ZOrganizer organizerFromProperty(Property prop) {
        String cn = prop.paramVal(ICalTok.CN, null);
        
        return new ZOrganizer(cn, prop.mValue);
    }
    
    static ZAttendee attendeeFromProperty(Property prop) {
        String cn = prop.paramVal(ICalTok.CN, null);
        String role = prop.paramVal(ICalTok.ROLE, null);
        String partstat = prop.paramVal(ICalTok.PARTSTAT, null);
        String rsvpStr = prop.paramVal(ICalTok.ROLE, "FALSE");
        boolean rsvp = false;
        if (rsvpStr.equalsIgnoreCase("TRUE")) {
            rsvp = true;
        }
        
        ZAttendee toRet = new ZAttendee(prop.mValue, cn, role, partstat, rsvp);
        return toRet;
    }
    
    static Property organizerToProperty(ZOrganizer org) {
        Property toRet = new Property(ICalTok.ORGANIZER, "MAILTO:"+org.getAddress());
        if (org.hasCn()) {
            toRet.addParameter(new Parameter(ICalTok.CN, org.getCn()));
        }
        return toRet;
    }
    
    static Property atToProperty(ZAttendee at) throws ServiceException {
        Property toRet = new Property(ICalTok.ATTENDEE, "MAILTO:"+at.getAddress());
        if (at.hasCn()) 
            toRet.addParameter(new Parameter(ICalTok.CN, at.getCn()));
        if (at.hasPartStat())
            toRet.addParameter(new Parameter(ICalTok.PARTSTAT, IcalXmlStrMap.sPartStatMap.toIcal(at.getPartStat())));
        if (at.hasRsvp())
            toRet.addParameter(new Parameter(ICalTok.RSVP, at.getRsvp()));
        if (at.hasRole())
            toRet.addParameter(new Parameter(ICalTok.ROLE, IcalXmlStrMap.sRoleMap.toIcal(at.getRole())));
        
        return toRet;
    }
    
    static Property recurIdToProperty(RecurId recurId) {
        Property toRet = new Property(ICalTok.RECURRENCE_ID, recurId.toString());
        return toRet;
    }
    
    static Component inviteToVEvent(Invite inv) throws ServiceException
    {
        Component event = new Component(ICalTok.VEVENT);
        
        event.addProperty(new Property(ICalTok.UID, inv.getUid()));
        
        IRecurrence recur = inv.getRecurrence();
        if (recur != null) {
            for (Iterator iter = recur.addRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_INSTANCE:
                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
                    // FIXME
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    event.addProperty(new Property(ICalTok.RRULE, srr.getRecur().toString()));
                    break;
                }
                
            }
            for (Iterator iter = recur.subRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_INSTANCE:
                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
                    // FIXME
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    event.addProperty(new Property(ICalTok.EXRULE, srr.getRecur().toString()));
                    break;
                }
            }
        }
        
        
        // ORGANIZER
        ZOrganizer org = inv.getOrganizer();
        if (org != null)
            event.addProperty(organizerToProperty(org));
        
        // allDay
        if (inv.isAllDayEvent())
            event.addProperty(new Property(ICalTok.X_MICROSOFT_CDO_ALLDAYEVENT, true));
        
        // SUMMARY (aka Name or Subject)
        String name = inv.getName();
        if (name != null && name.length()>0)
            event.addProperty(new Property(ICalTok.SUMMARY, name));
        
        // DESCRIPTION
        String fragment = inv.getFragment();
        if (fragment != null && fragment.length()>0)
            event.addProperty(new Property(ICalTok.DESCRIPTION, fragment));
        
        // COMMENT
        String comment = inv.getComment();
        if (comment != null && comment.length()>0) 
            event.addProperty(new Property(ICalTok.COMMENT, comment));
        
        // DTSTART
        event.addProperty(new Property(ICalTok.DTSTART, inv.getStartTime().toString()));
        
        // DTEND
        ParsedDateTime dtend = inv.getEndTime();
        if (dtend != null) 
            event.addProperty(new Property(ICalTok.DTEND, inv.getEndTime().toString()));
        
        // DURATION
        ParsedDuration dur = inv.getDuration();
        if (dur != null)
            event.addProperty(new Property(ICalTok.DURATION, dur.toString()));
        
        // LOCATION
        String location = inv.getLocation();
        if (location != null)
            event.addProperty(new Property(ICalTok.LOCATION, location.toString()));
        
        // STATUS
        event.addProperty(new Property(ICalTok.STATUS, IcalXmlStrMap.sStatusMap.toIcal(inv.getStatus())));
        
        // Microsoft Outlook compatibility for free-busy status
        {
            String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(inv.getFreeBusy());
            event.addProperty(new Property(ICalTok.X_MICROSOFT_CDO_BUSYSTATUS, outlookFreeBusy));
            event.addProperty(new Property(ICalTok.X_MICROSOFT_CDO_INTENDEDSTATUS, outlookFreeBusy));
        }
        
        // TRANSPARENCY
        event.addProperty(new Property(ICalTok.TRANSP, IcalXmlStrMap.sTranspMap.toIcal(inv.getTransparency())));
        
        // ATTENDEES
        for (ZAttendee at : (List<ZAttendee>)inv.getAttendees()) 
            event.addProperty(atToProperty(at));
        
        // RECURRENCE-ID
        RecurId recurId = inv.getRecurId();
        if (recurId != null) 
            event.addProperty(recurIdToProperty(recurId));
        
        // DTSTAMP
        ParsedDateTime dtStamp = ParsedDateTime.fromUTCTime(inv.getDTStamp());
        event.addProperty(new Property(ICalTok.DTSTAMP, dtStamp.toString()));
        
        // SEQUENCE
        event.addProperty(new Property(ICalTok.SEQUENCE, inv.getSeqNo()));
        
        return event;
    }
    
    static List<Invite> createFromCalendar(Calendar cal)
    {
        List <Invite> toRet = new ArrayList();
        
        TimeZoneMap tzmap = new TimeZoneMap(ICalTimeZone.getUTC());
        
        String methodStr = cal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString());
        
        for (Component comp : cal.mComponents) {
            switch(comp.mTok) {
            case VTIMEZONE:
                String tzname = comp.getPropVal(ICalTok.TZID, null);
                
                Component daylight = comp.getComponent(ICalTok.DAYLIGHT);
                
                String daydtStart = null;
                int dayoffsetTime = 0;
                String dayrrule = null;
                
                String stddtStart = null;
                int stdoffsetTime = 0;
                String stdrrule = null;
                
                if (daylight != null) {
                    daydtStart = daylight.getPropVal(ICalTok.DTSTART, null);
                    String daytzOffsetTo = daylight.getPropVal(ICalTok.TZOFFSETTO, null);
                    dayoffsetTime = tzOffsetToTime(daytzOffsetTo);  
                    dayrrule = daylight.getPropVal(ICalTok.RRULE, null);
                }
                
                Component standard = comp.getComponent(ICalTok.STANDARD); 
                if (standard != null) {
                    stddtStart = standard.getPropVal(ICalTok.DTSTART, null);
                    String stdtzOffsetTo = standard.getPropVal(ICalTok.TZOFFSETTO, null);
                    stdoffsetTime = tzOffsetToTime(stdtzOffsetTo);  
                    stdrrule = standard.getPropVal(ICalTok.RRULE, null);
                }
                
                ICalTimeZone tz = new ICalTimeZone(tzname, 
                        stdoffsetTime, stddtStart, stdrrule,
                        dayoffsetTime, daydtStart, dayrrule);

                tzmap.add(tz);
                break;
            case VEVENT:
                try {
                    Invite newInv = new Invite(tzmap);
                    newInv.setMethod(methodStr);
                    
                    toRet.add(newInv);
                    
                    ArrayList addRecurs = new ArrayList();
                    ArrayList subRecurs = new ArrayList();
                    
                    for (Property prop : comp.mProperties) {
                        System.out.println(prop);

                        if (prop.mTok == null) 
                            continue;
                        
                        switch (prop.mTok) {
                        case ORGANIZER:
                            newInv.setOrganizer(organizerFromProperty(prop));
                            break;
                        case ATTENDEE:
                            newInv.addAttendee(attendeeFromProperty(prop));
                            break;
                        case DTSTAMP:
                            ParsedDateTime dtstamp = ParsedDateTime.parse(prop.mValue, tzmap);
                            newInv.setDtStamp(dtstamp.getUtcTime());
                            break;
                        case RECURRENCE_ID:
                            ParsedDateTime rid = ParsedDateTime.parse(prop.getValue(), tzmap);
                            newInv.setRecurId(new RecurId(rid, RecurId.RANGE_NONE));
                            break;
                        case SEQUENCE:
                            newInv.setSeqNo(prop.getIntValue());
                            break;
                        case DTSTART:
                            ParsedDateTime dtstart = ParsedDateTime.parse(prop.mValue, tzmap);
                            newInv.setDtStart(dtstart);
                            break;
                        case DTEND:
                            ParsedDateTime dtend = ParsedDateTime.parse(prop.mValue, tzmap);
                            System.out.println(dtend.toString());
                            newInv.setDtEnd(dtend);
                            break;
                        case DURATION:
                            ParsedDuration dur = ParsedDuration.parse(prop.getValue());
                            newInv.setDuration(dur);
                            break;
                        case LOCATION:
                            newInv.setLocation(prop.getValue());
                            break;
                        case SUMMARY:
                            newInv.setName(prop.mValue);
                            break;
                        case DESCRIPTION:
                            newInv.setFragment(prop.mValue);
                            break;
                        case COMMENT:
                            newInv.setComment(prop.getValue());
                            break;
                        case UID:
                            newInv.setUid(prop.getValue());
                            break;
                        case RRULE:
                            ZRecur recur = new ZRecur(prop.getValue());
                            addRecurs.add(recur);
                            newInv.setIsRecurrence(true);
                            break;
                        case RDATE:
                            break;
                        case EXRULE:
                            ZRecur exrecur = new ZRecur(prop.getValue());
//                            ExRule exrule = new ExRule(exrecur);
                            subRecurs.add(exrecur);
                            newInv.setIsRecurrence(true);                            
                            break;
                        case EXDATE:
                            break;
                        case STATUS:
                            String status = IcalXmlStrMap.sStatusMap.toXml(prop.getValue());
                            if (status != null)
                                newInv.setStatus(status);
                            break;
                        case TRANSP:
                            String transp = IcalXmlStrMap.sTranspMap.toXml(prop.getValue());
                            if (transp!=null) {
                                newInv.setTransparency(transp);
                            }
                            break;
                        case X_MICROSOFT_CDO_ALLDAYEVENT:
                            if (prop.getBoolValue()) 
                                newInv.setIsAllDayEvent(true);
                            break;
                        case X_MICROSOFT_CDO_BUSYSTATUS:
                            String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(prop.getValue());
                            if (fb != null)
                                newInv.setFreeBusy(fb);
                            break;
                        }
                    }
                    
                    ParsedDuration duration = newInv.getDuration();
                    
                    if (duration == null) {
                        ParsedDateTime end = newInv.getEndTime();
                        if (end != null) {
                            duration = end.difference(newInv.getStartTime());
                        }
                    }
                    
                    ArrayList /* IInstanceGeneratingRule */ addRules = new ArrayList();
                    if (addRecurs.size() > 0) {
                        for (Iterator iter = addRecurs.iterator(); iter.hasNext();) {
                            Object next = iter.next();
                            if (next instanceof ZRecur) {
                                ZRecur cur = (ZRecur)next;
                                addRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, new InviteInfo(newInv)));
                            } else {
                                RDate cur = (RDate)next;
                                // TODO add the dates here!
                            }
                        }
                    }
                    ArrayList /* IInstanceGeneratingRule */  subRules = new ArrayList();
                    if (subRules.size() > 0) {
                        for (Iterator iter = subRules.iterator(); iter.hasNext();) {
                            Object next = iter.next();
                            if (next instanceof ZRecur) {
                                ZRecur cur = (ZRecur)iter.next();
                                subRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, new InviteInfo(newInv)));
                            } else {
                                ExDate cur = (ExDate)next;
                                // TODO add the dates here!
                            }
                        }
                    }
                    
                    if (newInv.hasRecurId()) {
                        if (addRules.size() > 0) {
                            newInv.setRecurrence(new Recurrence.ExceptionRule(newInv.getRecurId(),  
                                    newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                        }
                    } else {
                        if (addRules.size() > 0) { // since exclusions can't affect DtStart, just ignore them if there are no add rules
                            newInv.setRecurrence(new Recurrence.RecurrenceRule(newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                        }
                    }
                    
                    if (newInv.getAttendees().size() > 1) {
                        newInv.setHasOtherAttendees(true);
                    }
                    
                    System.out.println("\n\n\n\n"+newInv.toICalendar().toString());

                    
                    StringWriter sw = new StringWriter();
                    
                    Writer w = new FoldingWriter(sw);
                    
                    inviteToVEvent(newInv).toICalendar(w);
                    
                    System.out.println("\n\n\n\n"+sw.toString());
                    
                } catch (ServiceException e) {
                    System.out.println(e);
                    e.printStackTrace();
                } catch (ParseException e) {
                    System.out.println(e);
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
                
                break;
            }
        }
        
        return toRet;
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
            createFromCalendar(handler.mCal);
            
        } catch(Exception e) {
            System.out.println("Caught exception: "+e);
            e.printStackTrace();
        }

    }
}
