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

package com.zimbra.cs.service.formatter;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;

public class ContactCSV {

    private static Log sLog = ZimbraLog.misc;
    public static final char DEFAULT_FIELD_SEPARATOR = ',';
    // CSV files intended for use in locales where ',' as the decimal separator
    // sometimes use ';' as a field separator instead of ','.
    public static final char[] SUPPORTED_SEPARATORS = { DEFAULT_FIELD_SEPARATOR, ';' };
    enum ColType { SIMPLE, MULTIVALUE, NAME, TAG, DATE };

    private int mLineNumber;
    private int mCurrContactStartLineNum;
    private ArrayList<String> mFields;            // Names of fields from first line in CSV file
    private boolean mDetectFieldSeparator;
    private boolean mKnowFieldSeparator;
    private char mFieldSeparator;
    
    private static Set<String>            mKnownFields;
    private static Set<CsvFormat>         mKnownFormats;
    private static Map <String,Character> mDelimiterInfo;
    private static Map <String,String>    mDateOrderInfo;
    private static CsvFormat              mDefaultFormat;

    private static final QName DATEFORMATS = QName.get("dateformats");
    private static final QName DATEFORMAT = QName.get("dateformat");
    private static final QName DELIMITERS = QName.get("delimiters");
    private static final QName DELIMITER = QName.get("delimiter");
    private static final QName FIELDS = QName.get("fields");
    private static final QName FIELD  = QName.get("field");
    private static final QName FORMAT = QName.get("format");
    private static final QName COLUMN = QName.get("column");
    
    private static final String ATTR_CHAR  = "char";
    private static final String ATTR_NAME  = "name";
    private static final String ATTR_LOCALE  = "locale";
    private static final String ATTR_FIELD = "field";
    private static final String ATTR_FLAG  = "flag";
    private static final String ATTR_FORMAT  = "format";
    private static final String ATTR_ORDER  = "order";
    private static final String ATTR_TYPE  = "type";

    public ContactCSV() {
        this(DEFAULT_FIELD_SEPARATOR, true);
    }

    public ContactCSV(char defaultFieldSeparator, boolean detectFieldSeparator) {
        mFieldSeparator = defaultFieldSeparator;
        mDetectFieldSeparator = detectFieldSeparator;
        // If we are not doing auto-detect, defaultFieldSeparator MUST be the separator
        mKnowFieldSeparator = !detectFieldSeparator;
    }

    /**
     * Implicit assumption, the first field name will not contain any of the supported separators
     * @param testChar
     * @return
     */
    private boolean isFieldSeparator(int testChar) {
        if (mKnowFieldSeparator)
            return (testChar == mFieldSeparator);
        for (char possSep : SUPPORTED_SEPARATORS) {
            if (possSep == testChar) {
                if ((sLog.isDebugEnabled()) && (possSep != DEFAULT_FIELD_SEPARATOR))
                    sLog.debug("CSV Separator character used='%c'", possSep);
                mKnowFieldSeparator = true;
                mFieldSeparator = possSep;
                return true;
            }
        }
        return false;
    }

    /**
     * read a line of fields into an array list. blank fields (,, or ,"",) will be null. 
     */
    private boolean parseLine(BufferedReader reader, List<String> result, boolean parsingHeader) throws IOException, ParseException {
        mCurrContactStartLineNum = mLineNumber;
        if (parsingHeader && mDetectFieldSeparator)
            mKnowFieldSeparator = false;
        result.clear();
        int ch;
        boolean inField = false;
        while ((ch = reader.read()) != -1) {
            switch (ch) {
                case '"':
                    inField = true;
                    result.add(parseField(reader, true, -1, parsingHeader, result.size()));
                    break;
                case '\n':
                    mLineNumber++;
                    if (result.size() > 0)
                        return true;
                    else
                        break;
                case '\r':
                    mLineNumber++;
                    // peek for \n
                    reader.mark(1);
                    ch = reader.read();
                    if (ch != '\n') reader.reset();
                    if(result.size() > 0)
                        return true;
                    else
                        break;
                default:
                    if (isFieldSeparator(ch)) {
                        if (inField) inField = false;
                        else result.add(null);
                    } else {
                        // start of field
                        result.add(parseField(reader, false, ch, parsingHeader, result.size())); // eats trailing field separator
                        inField = false;
                    }
                    break;
            }
        }
        return result.size() > 0;
    }

    /**
     * parse a field. It is assumed the leading '"' is already read. we stop at the first '"' that isn't followed by a '"'.
     * 
     * @param reader
     * @return the field, or null if the field was blank ("")
     * @throws IOException IOException from the reader.
     * @throws ParseException if we reach the end of file while parsing
     */
    private String parseField(BufferedReader reader, boolean doubleQuotes, int firstChar, boolean parsingHeader, int size) throws IOException, ParseException {
        StringBuffer sb = new StringBuffer();

        if (firstChar != -1) sb.append((char)firstChar);
        int ch;
        reader.mark(1);
        while ((ch = reader.read()) != -1) {
            if (ch == '"' && doubleQuotes) {
                reader.mark(1);
                ch = reader.read();
                if (ch != '"') {
                    reader.reset();
                    if (sb.length() == 0) return null;
                    else return sb.toString();
                }
                sb.append((char)ch);
            } else if (ch == mFieldSeparator && !doubleQuotes) {
                //reader.reset();
                return sb.toString();
            } else if ((ch == '\r' || ch == '\n') && !doubleQuotes) {
                    reader.reset();
                    return sb.toString();
            } else {
                sb.append((char)ch);
                if (ch == '\r') {
                    // peek for \n
                    reader.mark(1);
                    ch = reader.read();
                    if (ch == '\n')
                        sb.append((char)ch);
                    else
                        reader.reset();
                }
                if ((ch == '\r') || (ch == '\n'))
                    mLineNumber++;
            }
            reader.mark(1);
        }
        if (doubleQuotes)
            throw new ParseException("End of stream reached while parsing field.\nCurrent contact definition started at line "
                    + mCurrContactStartLineNum);
        else 
            return sb.toString();
    }

    /**
     * Reads the first line of .CSV data and use this information to perform some
     * initialisations.
     * @param reader is the stream of .CSV data
     * @throws IOException
     * @throws ParseException
     */
    private void initFields(BufferedReader reader) throws IOException, ParseException {
        mLineNumber = 1;
        mCurrContactStartLineNum = 1;
        mFields = new ArrayList<String>();

        if (!parseLine(reader, mFields, true))
            throw new ParseException("no column name definitions");

        // Remove Byte-order information if present
        String firstField = mFields.get(0);
        if  (   (firstField != null) && (firstField.length() >= 1) &&
                (firstField.charAt(0) == 0xfeff) ) {
            mFields.set(0, firstField.substring(1));
        }

        // check that there are no missing column names
        for (int i = 0; i < mFields.size(); i++) {
            String fieldName = mFields.get(i);
            if (fieldName == null || fieldName.equals(""))
                throw new ParseException("missing column name for column " + i);
        }
    }

    private void addMultiValueField(CsvColumn col, Map <String, String> fieldMap, ContactMap contact) {
        StringBuilder buf = new StringBuilder();
        for (String n : col.names) {
            String v = fieldMap.get(n.toLowerCase());
            if (v != null) {
                if (buf.length() > 0)
                    buf.append("\n");
                buf.append(v);
            }
        }
        if (buf.length() > 0)
            contact.put(col.field, buf.toString());
    }

    /**
     *
     * @param testY   candidate year
     * @param testM   candidate month in range 1 to 12
     * @param testD   candidate day in range 1 to 31
     * @param leniant succeed even if the day and month might be ambiguous
     * @return Formatted date if certain date fields are OK and wouldn't be OK in a different order, else null
     */
    private String populateDateFieldsIfUnambiguous(int testY, int testM, int testD, boolean leniant) {
        if ((testY < 1600) || (testY > 4500)) {
            // Year does not fit within what Outlook allows, probably not what is intended
            return null;
        }
        int firstAllowableDay = leniant ? 1 : 13;
        if ((testD < firstAllowableDay) || (testD > 31)) {
            // Either an invalid date or could be a month number if guessed date string order wrong
            return null;
        }
        if ((testM < 1) || (testM > 12)) {
            // Not a valid month
            return null;
        }
        ICalTimeZone tzUTC = ICalTimeZone.getUTC();
        GregorianCalendar cal = new GregorianCalendar(tzUTC);
        cal.set(testY, testM - 1, 1, 0, 0);
        if (testD > cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            return  null;
        cal.set(Calendar.DAY_OF_MONTH, testD);
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd");
        ymd.setCalendar(cal);
        return ymd.format(cal.getTime());
    }

    /**
     * Ideally, want to store the value in "yyyy-mm-dd" format
     * @param value
     * @param field
     * @param contact
     */
    private void addDateField(String value, String field, String dateOrderKey, ContactMap contact) {
        if (field == null || value == null || value.length() == 0)
            return;
        // If we successfully parse "value" as a date, zimbraDateValue will be set.
        String zimbraDateValue = null;
        try {
            String[] splitFields = value.split("/");
            if (splitFields.length != 3)
                splitFields = value.split("-");
            if (splitFields.length == 3) {
                int dateFs[] = new int[3];
                dateFs[0] = Integer.parseInt(splitFields[0]);
                dateFs[1] = Integer.parseInt(splitFields[1]);
                dateFs[2] = Integer.parseInt(splitFields[2]);
                zimbraDateValue = populateDateFieldsIfUnambiguous(
                        dateFs[0], dateFs[1], dateFs[2], false);   // e.g. 2005/25/12
                if (zimbraDateValue == null)
                    zimbraDateValue = populateDateFieldsIfUnambiguous(
                            dateFs[2], dateFs[0], dateFs[1], false); // e.g. 12/25/2005
                if (zimbraDateValue == null)
                    zimbraDateValue = populateDateFieldsIfUnambiguous(
                            dateFs[2], dateFs[1], dateFs[0], false); // e.g. 25/12/2005
                if (zimbraDateValue == null)
                    zimbraDateValue = populateDateFieldsIfUnambiguous(
                            dateFs[1], dateFs[2], dateFs[0], false); // e.g. 25/2005/12
                if (zimbraDateValue == null)
                    zimbraDateValue = populateDateFieldsIfUnambiguous(
                            dateFs[0], dateFs[2], dateFs[1], false); // e.g. 2005/25/12
                if (zimbraDateValue == null)
                    zimbraDateValue = populateDateFieldsIfUnambiguous(
                            dateFs[1], dateFs[0], dateFs[2], false); // e.g. 12/2005/25
                if (zimbraDateValue == null) {
                    String dateOrder = mDateOrderInfo.get(dateOrderKey);
                    if (dateOrder != null) {
                        ICalTimeZone tzUTC = ICalTimeZone.getUTC();
                        GregorianCalendar cal = new GregorianCalendar(tzUTC);
                        int yNdx = dateOrder.indexOf('y');
                        int mNdx = dateOrder.indexOf('m');
                        int dNdx = dateOrder.indexOf('d');
                        if ((yNdx != -1) && (mNdx != -1) && (dNdx != -1)) {
                            zimbraDateValue = populateDateFieldsIfUnambiguous(
                                    dateFs[yNdx], dateFs[mNdx], dateFs[dNdx], true);
                        }
                    }
                }
            }
        } catch (NumberFormatException ioe) {
        }
        if (zimbraDateValue != null)
            contact.put(field, zimbraDateValue);
        else {
            // We were unable to recognise the date format for this value :-(
            if (Character.isDigit(value.charAt(0)))
                // Avoid later corruption from trying to process as a valid date.
                contact.put(field, new StringBuffer("'").append(value).append("'").toString());
            else
                contact.put(field, value);
        }
    }

    private void addNameField(String value, String field, ContactMap contact) {
        if (field == null || value == null)
            return;
        String[] nameFields = field.split(",");
        String firstNameField = nameFields[0];

        if (firstNameField == null)
            return;

        String middleNameField = null, lastNameField = null;
        if (nameFields.length == 2)       // firstName,lastName
            lastNameField = nameFields[1];
        else if (nameFields.length == 3) {  // firstName,middleName,lastName
            middleNameField = nameFields[1];
            lastNameField = nameFields[2];
        } else {
            // not sure how to parse this one.  just put everything in firstName field.
            contact.put(firstNameField, value);
            return;
        }

        int comma = value.indexOf(mFieldSeparator);
        if (comma > 0) {
            // Brown, James
            contact.put(lastNameField, value.substring(0, comma).trim());
            contact.put(firstNameField, value.substring(comma+1).trim());
            return;
        }
        
        int space = value.indexOf(' ');
        if (space == -1) {
            contact.put(firstNameField, value);
            return;
        }
        contact.put(firstNameField, value.substring(0, space).trim());
        space++;
        if (middleNameField != null) {
            int anotherSpace = value.indexOf(' ', space);
            if (anotherSpace > 0) {
                contact.put(middleNameField, value.substring(space, anotherSpace).trim());
                space = anotherSpace + 1;
            }
        }
        if (space < value.length())
            contact.put(lastNameField, value.substring(space).trim());
    }

private static final String TAG = "__tag";

public static String getTags(Map<String,String> csv) {
    	return csv.remove(TAG);
    }

    /**
     * 
     * @param csv is the list of fields in a record from a CSV file
     * @param formats is the list of CsvFormats to be considered applicable 
     * @return a map from field to value
     * @throws ParseException
     */
    private Map<String, String> toContact(List<String> csv, CsvFormat format) throws ParseException {
        ContactMap contactMap = new ContactMap();

        // NOTE: If there isn't a mapping for a field name defined in "format"
        // NOTE: a user defined attribute with that field name will be used.
        if (csv == null )
            return contactMap.getContacts();
        if (format.allFields()) {
            int end = csv.size();
            end = (end > mFields.size()) ? mFields.size() : end;
            for (int i = 0; i < end; i++)
                contactMap.put(mFields.get(i), csv.get(i));
        }
        else if (format.hasNoHeaders()) {
            int end = csv.size();
            end = (end > format.columns.size()) ? format.columns.size() : end;
            for (int i = 0; i < end; i++)
                contactMap.put(format.columns.get(i).field, csv.get(i));
        }
        else {
            /* Many CSV formats are output in a specific order and sometimes 
             * contain duplicate field names with mappings to different
             * Zimbra contact fields.
             */
            Map <CsvColumn, Map <String, String>> pendMV = new HashMap <CsvColumn, Map <String, String>>();
            List<CsvColumn> unseenColumns = new ArrayList<CsvColumn>();
            unseenColumns.addAll(format.columns);
            for (int ndx = 0;ndx < mFields.size();ndx++) {
                String csvFieldName = mFields.get(ndx);
                String fieldValue = (ndx >= csv.size()) ? null : csv.get(ndx );
                CsvColumn matchingCol = null;
                String matchingFieldLc = null;
                for (CsvColumn unseenC : unseenColumns) {
                    matchingFieldLc = unseenC.matchingLcCsvFieldName(csvFieldName);
                    if (matchingFieldLc == null)
                        continue;
                    if (unseenC.colType == ColType.MULTIVALUE) {
                        Map <String, String> currMV = pendMV.get(matchingCol);
                        if ((currMV != null) && currMV.get(matchingFieldLc) != null)
                            // already have field with this name that matches this column
                            continue;
                    }
                    matchingCol = unseenC;
                    break;
                }
                if (matchingCol == null) {
                    // unknown field - setup for adding as a user defined attribute
                    sLog.debug("Adding CSV contact attribute [%s=%s] - assuming is user defined.", csvFieldName, fieldValue);
                    contactMap.put(csvFieldName, fieldValue);
                    continue;
                }
                switch (matchingCol.colType) {
                    case NAME: 
                        addNameField(fieldValue, matchingCol.field, contactMap);
                        unseenColumns.remove(matchingCol);
                        break;
                    case DATE: 
                        addDateField(fieldValue, matchingCol.field, format.key(), contactMap);
                        unseenColumns.remove(matchingCol);
                        break;
                    case TAG: 
                        contactMap.put(TAG, fieldValue);
                        break;
                    case MULTIVALUE: 
                        for ( String cname : matchingCol.names) {
                            if (cname.toLowerCase().equals(matchingFieldLc)) {
                                Map <String, String> currMV = pendMV.get(matchingCol);
                                if (currMV == null) {
                                    currMV = new HashMap <String, String> ();
                                    pendMV.put(matchingCol, currMV);
                                }
                                currMV.put(matchingFieldLc, fieldValue);
                                if (currMV.size() >= matchingCol.names.size()) {
                                    addMultiValueField(matchingCol, currMV, contactMap);
                                    pendMV.remove(currMV);
                                    unseenColumns.remove(matchingCol);
                                }
                            }
                        }
                        break;
                    default:
                        contactMap.put(matchingCol.field, fieldValue);
                        unseenColumns.remove(matchingCol);
                }
            }
            // Process multi-value fields where only some constituent fields were present
            for (Map.Entry <CsvColumn, Map <String, String>> entry : pendMV.entrySet()) {
                addMultiValueField(entry.getKey(),entry.getValue(), contactMap);
            }
        }

        Map<String, String> contact = contactMap.getContacts();

        // Bug 50069 - Lines with single blank in them got imported as a blank contact
        // Initial fix idea was for parseField to return the trimmed version of the string
        // However, this from rfc4180 - Common Format and MIME Type for Comma-Separated
        // Values (CSV) Files :
        //     "Spaces are considered part of a field and should not be ignored."
        // suggests that might be an invalid thing to do, so now just reject the contact
        // if the whole line would collapse to an empty string with trim.
        if (contact.size() == 1) {
            boolean onlyBlank = true;
            for (String val : contact.values()) {
                if (!val.trim().equals("")) {
                    onlyBlank = false;
                    break;
                }
            }
            if (onlyBlank)
                contact = new HashMap<String, String>();
        }
        return contact;
    }

    private List<Map<String, String>> getContactsInternal(BufferedReader reader, String fmt, String locale) throws ParseException {
        try {
            CsvFormat format = null;
            initFields(reader);
            
            if (fmt == null)
                format = guessFormat(mFields);
            else
                format = getFormat(fmt, locale);

            sLog.debug("getContactsInternal requested format/locale=[%s/%s]: using %s", fmt, locale, format.toString());
            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            List<String> fields = new ArrayList<String>();

            while (parseLine(reader, fields, false)) {
                Map<String, String> contact = toContact(fields, format);
                if (contact.size() > 0)
                    result.add(contact);
            }
            return result;
        } catch (IOException ioe) {
            sLog.debug("Encountered IOException", ioe);
            throw new ParseException(ioe.getMessage() + " at line number " + mLineNumber, ioe);
        }
    }

    /**
     * return a list of maps, representing contacts
     * @param reader
     * @param fmt
     * @return
     * @throws ParseException 
     * @throws IOException 
     */
    public static List<Map<String, String>> getContacts(BufferedReader reader, String fmt, String locale) throws ParseException {
        ContactCSV csv = new ContactCSV();
        return csv.getContactsInternal(reader, fmt, locale);
    }

    public static List<Map<String, String>> getContacts(BufferedReader reader, String fmt) throws ParseException {
        return getContacts(reader, fmt, null);
    }

    @SuppressWarnings("serial")
    public static class ParseException extends Exception {
        ParseException(String msg) {
            super(msg);
        }
        
        ParseException(String msg, Throwable cause) {
            super(msg, cause);
        }        
    }

//    <delimiters default=","> 
//        <delimiter locale="fr" format="outlook-2003-csv" char=";" /> 
//        ...
//    </delimiters>

    private static void populateDelimiterInfo(Element delimiters) {
        mDelimiterInfo = new HashMap<String,Character>();
        
        for (Iterator elements = delimiters.elementIterator(DELIMITER); elements.hasNext(); ) {
            Element field = (Element) elements.next();
            String delim = field.attributeValue(ATTR_CHAR);
            if (delim == null || delim.isEmpty())
                continue;
            String format = field.attributeValue(ATTR_FORMAT);
            if (format == null || format.isEmpty())
                continue;
            String myLocale  = field.attributeValue(ATTR_LOCALE);
            if (myLocale != null && !myLocale.isEmpty())
                format = new StringBuffer(format).append("/").append(myLocale).toString();
            mDelimiterInfo.put(format, delim.charAt(0));
        }
    }

//    <dateformats> 
//        <dateformat format="yahoo-csv" order="mdy" /> 
//        ...
//    </dateformats>

    private static void populateDateFormatInfo(Element dateFormats) {
        mDateOrderInfo = new HashMap<String,String>();

        for (Iterator elements = dateFormats.elementIterator(DATEFORMAT); elements.hasNext(); ) {
            Element dateFormat = (Element) elements.next();
            String origOrder = dateFormat.attributeValue(ATTR_ORDER);
            if (origOrder == null || origOrder.isEmpty())
                continue;
            String order = origOrder.toLowerCase();
            if (! (order.equals("ymd") || order.equals("ydm") || order.equals("myd") ||
                    order.equals("mdy") || order.equals("dmy") || order.equals("dym")) ) {
                sLog.debug("invalid \"order\" %s in zimbra-contact-fields.xml", origOrder);
                continue;
            }
            String format = dateFormat.attributeValue(ATTR_FORMAT);
            if (format == null || format.isEmpty())
                continue;
            String myLocale  = dateFormat.attributeValue(ATTR_LOCALE);
            if (myLocale != null && !myLocale.isEmpty())
                format = new StringBuffer(format).append("/").append(myLocale).toString();
            mDateOrderInfo.put(format, order);
        }
    }

    private static void populateFields(Element fields) {
        mKnownFields = new HashSet<String>();
        
        for (Iterator elements = fields.elementIterator(FIELD); elements.hasNext(); ) {
            Element field = (Element) elements.next();
            mKnownFields.add(field.attributeValue(ATTR_NAME));
        }
    }

    /**
     * ContactMap gathers up
     * It wraps a Map<String, String> and enforces policies such as
     * a new entry must use a unique case-insensitive key
     */
    private static class ContactMap {
        private Map<String, String> contacts;
        private Set<String> seenFields;

        public ContactMap() {
            contacts = new HashMap<String, String>();
            seenFields = new HashSet<String>();
        }

        public boolean put(String key, String value) {
            if (key == null || value == null)
                return false;
            if (key.trim().equals(""))
                return false;
            if (value.length() <= 0)
                return false;
            String lkey = key.toLowerCase();
            if (seenFields.contains(lkey))
                return false;
            seenFields.add(lkey);

            contacts.put(key, value);
            return true;
        }

        public Map<String, String> getContacts() {
            return contacts;
        }
    }

    private static class CsvColumn {
        String name;    // column name for this format
        String field;   // zimbra field that it maps to
        List<String> names;  // in case of multivalue mapping
        ColType colType;
        CsvColumn(Element col) {
            names = new ArrayList<String>();
            name  = col.attributeValue(ATTR_NAME);
            field = col.attributeValue(ATTR_FIELD);
            colType = ColType.SIMPLE;
            String type = col.attributeValue(ATTR_TYPE);
            if (type == null) {
                return;
            } else if (type.equals("multivalue")) {
                colType = ColType.MULTIVALUE;
                Collections.addAll(names, name.split(","));
                name = names.get(0);
            } else if (type.equals("name")) {
                colType = ColType.NAME;
            } else if (type.equals("tag")) {
                colType = ColType.TAG;
            } else if (type.equals("date")) {
                colType = ColType.DATE;
            }
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(name).append(": ").append(field);
            switch (colType) {
            case NAME: sb.append(" (name)"); break;
            case TAG: sb.append(" (tag)"); break;
            case DATE: sb.append(" (date)"); break;
            case MULTIVALUE: 
                sb.append(" ").append("(multivalue cols=").append(names.toString()).append(")");break;
            }
            return sb.toString();
        }

        /**
         * 
         * @param fieldName is a field name from the first line of a .csv file
         * @return if <code>fieldName</code> matches this column, return the
         * lowercase version of the matching string, otherwise return null.
         */
        public String matchingLcCsvFieldName(String fieldName) {
            String lcFieldName = fieldName.toLowerCase();
            if (colType == ColType.MULTIVALUE) {
                for (String colName: names) {
                    if (colName.toLowerCase().equals(lcFieldName)) {
                        return lcFieldName;
                    }
                }
            }
            else if (name.toLowerCase().equals(lcFieldName)) {
                return lcFieldName;
            }
            return null;
        }
    }

    private static class CsvFormat implements Comparable <CsvFormat> {
        String name;
        String locale;
        Set<String> flags;
        List<CsvColumn> columns;
        Map<String,String> forwardMapping;
        Map<String,String> reverseMapping;
        
        CsvFormat(Element fmt) {
            name = fmt.attributeValue(ATTR_NAME);
            locale = fmt.attributeValue(ATTR_LOCALE);
            String f = fmt.attributeValue(ATTR_FLAG);
            flags = new HashSet<String>();
            if (f != null)
                Collections.addAll(flags, f.toLowerCase().split(","));
            columns = new ArrayList<CsvColumn>();
            forwardMapping = new HashMap<String,String>();
            reverseMapping = new HashMap<String,String>();
        }
        void add(Element col) {
            CsvColumn newColumn = new CsvColumn(col);
            columns.add(newColumn);
            forwardMapping.put(newColumn.name,  newColumn.field);
            reverseMapping.put(newColumn.field, newColumn.name);
        }
        boolean hasFlag(String f) {
            return flags.contains(f.toLowerCase());
        }
        boolean hasNoHeaders() {
            return hasFlag("noheader");
        }
        boolean allFields() {
            return hasFlag("allfields");
        }

        public String toString() {
            StringBuffer sb = new StringBuffer(name);
            if (locale != null) sb.append(" locale=").append(locale);
            if (!flags.isEmpty()) sb.append(" flags=").append(flags);
            return sb.toString();
        }

        public String key() {
            String myKey = name;
            if (locale != null)
                myKey = new StringBuffer(myKey).append("/").append(locale).toString();
            return myKey;
        }

        @Override
        public int compareTo(CsvFormat o) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;

            if ( this == o ) return EQUAL;

            int nameSame = this.name.compareTo(o.name);
            if (nameSame != EQUAL)
                return nameSame;
            if (locale == null) {
                if (o.locale == null)
                    return nameSame;
                return AFTER;
            } else {
                if (o.locale == null)
                    return BEFORE;
                return (this.locale.compareTo(o.locale));
            }
        }
    }
    
    private static void addFormat(Element format) {
        CsvFormat fmt = new CsvFormat(format);
        
        for (Iterator elements = format.elementIterator(COLUMN); elements.hasNext(); ) {
            Element col = (Element) elements.next();
            fmt.add(col);
        }
        
        if (mKnownFormats == null)
            mKnownFormats = new HashSet<CsvFormat>();

        mKnownFormats.add(fmt);
        if (fmt.hasFlag("default"))
            mDefaultFormat = fmt;
    }
    
    static {
        try {
            readMappingFile(LC.zimbra_csv_mapping_file.value());
        } catch (Exception e) {
            sLog.error("error initializing csv mapping file", e);
        }
    }
    
    private static void readMappingFile(String mappingFile) throws IOException, DocumentException {
        readMapping(new FileInputStream(mappingFile));
    }
    
    private static void readMapping(InputStream is) throws IOException, DocumentException {
        mDelimiterInfo = new HashMap<String,Character>();
        mDateOrderInfo = new HashMap<String,String>();
        Element root = com.zimbra.common.soap.Element.getSAXReader().read(is).getRootElement();
        for (Iterator elements = root.elementIterator(); elements.hasNext(); ) {
            Element elem = (Element) elements.next();
            if (elem.getQName().equals(FIELDS))
                populateFields(elem);
            else if (elem.getQName().equals(FORMAT))
                addFormat(elem);
            else if (elem.getQName().equals(DELIMITERS))
                populateDelimiterInfo(elem);
            else if (elem.getQName().equals(DATEFORMATS))
                populateDateFormatInfo(elem);
        }
    }

    private static CsvFormat guessFormat(List<String> keys) throws ParseException {
        if (mKnownFormats == null || mDefaultFormat == null)
            throw new ParseException("missing config file "+LC.zimbra_csv_mapping_file.value());
        
        int numMatchedFields, numBestMatch;
        CsvFormat bestMatch;
        
        numBestMatch = 0;
        bestMatch = mDefaultFormat;
        
        for (CsvFormat f : mKnownFormats) {
            numMatchedFields = 0;
            for (CsvColumn col : f.columns)
                if (keys.contains(col.name))
                    numMatchedFields++;
            if (numMatchedFields > numBestMatch) {
                numBestMatch = numMatchedFields;
                bestMatch = f;
            }
        }
        if (sLog.isDebugEnabled())
            sLog.debug("Best matching format='%s'", bestMatch.toString());
        return bestMatch;
    }
    
    /**
     * Will try to match both <code>fmt</code> and <code>locale</code> first
     * If no match found, will try to match just <code>fmt</code>
     * If still no match found, returns the default format.
     * 
     * @param fmt
     * @param locale
     * @return the best matching format
     * @throws ParseException
     */
    private static CsvFormat getFormat(String fmt, String locale) throws ParseException {
        if (mKnownFormats == null || mDefaultFormat == null)
            throw new ParseException("missing config file "+LC.zimbra_csv_mapping_file.value());
        
        if (locale != null) {
            for (CsvFormat f : mKnownFormats)
                if ((f.locale != null) && f.name.equals(fmt) && (f.locale.equals(locale)))
                    return f;
        }
        for (CsvFormat f : mKnownFormats)
            if (f.name.equals(fmt) && (f.locale == null))
                return f;
        
        return mDefaultFormat;
    }

    public void toCSV(String format, String locale, Character separator, Iterator<? extends MailItem> contacts, StringBuffer sb) throws ParseException {
        if (mKnownFormats == null)
            return;

        CsvFormat fmt = getFormat(format, locale);
        if (separator != null) {
            mFieldSeparator = separator;
        } else {
            String delimKey = fmt.key();
            Character formatDefaultDelim = mDelimiterInfo.get(delimKey);
            if (formatDefaultDelim != null) {
                sLog.debug("toCSV choosing %c from <delimiter> matching %s", formatDefaultDelim, delimKey);
                mFieldSeparator = formatDefaultDelim;
            }
        }
        sLog.debug("toCSV Requested=[format=\"%s\" locale=\"%s\" delim=\"%c\"] Actual=[%s delim=\"%c\"]",
                format, locale, separator, fmt.toString(), mFieldSeparator);

        if (fmt == null)
            return;
        
        if (fmt.allFields()) {
            ArrayList<Map <String, String>> allContacts = new ArrayList<Map <String, String>>();
            HashSet<String> fields = new HashSet<String>();
            while (contacts.hasNext()) {
                Object obj = contacts.next();
                if (obj instanceof Contact) {
                    Contact c = (Contact) obj;
                    allContacts.add(c.getFields());
                    fields.addAll(c.getFields().keySet());
                }
            }
            ArrayList<String> allFields = new ArrayList<String>();
            allFields.addAll(fields);
            Collections.sort(allFields);
            addFieldDef(allFields, sb);
            for (Map <String, String> contactMap : allContacts)
                toCSVContact(allFields, contactMap, sb);
            return;
        }
        
        if (!fmt.hasNoHeaders())
            addFieldDef(fmt, sb);

        while (contacts.hasNext()) {
            Object c = contacts.next();
            if (c instanceof Contact)
                toCSVContact(fmt, (Contact)c, sb);
        }
        
    }

    private static void addFieldValue(Map <String, String> contact, String name, String field, StringBuffer sb) {
        String value = (String) contact.get((field == null) ? name : field);
        if (value == null) value = "";
        sb.append('"');
        sb.append(value.replaceAll("\"", "\"\""));
        sb.append('"');
    }

    private void toCSVContact(List<String> fields, Map <String,String> contact, StringBuffer sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first)
                sb.append(mFieldSeparator);
            addFieldValue(contact, f, f, sb);
            first = false;
        }
        sb.append("\n");
    }

    private void toCSVContact(CsvFormat fmt, Contact c, StringBuffer sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first)
                sb.append(mFieldSeparator);
            if (col.colType == ColType.TAG) {
            	try {
            		boolean firstTag = true;
                	sb.append('"');
                	for (Tag t : c.getTagList()) {
                		if (!firstTag)
                			sb.append(mFieldSeparator);
                		sb.append(t.getName());
                		firstTag = false;
                	}
                	sb.append('"');
            	} catch (ServiceException se) {
            	}
            } else
            	addFieldValue(c.getFields(), col.name, col.field, sb);
            first = false;
        }
        sb.append("\n");
    }
    
    private void addFieldDef(List<String> fields, StringBuffer sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first)
                sb.append(mFieldSeparator);
            sb.append('"');
            sb.append(f);
            sb.append('"');
            first = false;
        }
        sb.append("\n");
    }
    
    private void addFieldDef(CsvFormat fmt, StringBuffer sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first)
                sb.append(mFieldSeparator);
            sb.append('"');
            sb.append(col.name);
            sb.append('"');
            first = false;
        }
        sb.append("\n");
    }
    
    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes());
        out.write('\n');
    }
    
    private static void dump(OutputStream out) throws IOException {
        writeLine(out, "=== Fields ===");
        for (String f : new TreeSet <String> (mKnownFields))
            writeLine(out, f);
        for (CsvFormat fmt : new TreeSet <CsvFormat>(mKnownFormats)) {
            StringBuffer sb = new StringBuffer("=== Mapping ");
            sb.append(fmt.toString());
            sb.append(" ===");
            writeLine(out, sb.toString());
            for (CsvColumn col : fmt.columns) {
                if (col.field != null) {
                    if (!mKnownFields.contains(col.field)) {
                        sLog.debug("Mapping '%s' references unknown field='%s'\n", fmt.name, col.field);
                    }
                }
                writeLine(out, col.toString());
            }
        }
    }
    
    public static String[] getAllFormatNames() {
        Set<String> formats = new HashSet<String>();
        for (CsvFormat f : mKnownFormats)
            formats.add(f.name);
        return formats.toArray(new String[0]);
    }
    
    public static void main(String args[]) throws IOException, ParseException, DocumentException {
        ZimbraLog.toolSetupLog4jConsole("INFO", true, false);
        //String mappingFile = LC.zimbra_csv_mapping_file.value();
        if (args.length > 0) {
            mKnownFormats = new HashSet<CsvFormat>();
            readMappingFile(args[0]);
        }
        dump(System.out);
        writeLine(System.out, "");
        System.out.print("All Format Names:");
        for (String fmtName : getAllFormatNames())
            System.out.print(" " + fmtName);
        System.out.println();
    }
}
