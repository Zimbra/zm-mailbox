/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.formatter;

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

import org.dom4j.Element;
import org.dom4j.QName;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.UserServletUtil;

public final class ContactCSV {

    private static Log LOG = ZimbraLog.misc;
    private static final char DEFAULT_FIELD_SEPARATOR = ',';
    // Bug 32273 - Outlook prefers DOS line endings between records - although it doesn't mind either type of
    // ending for lines contained within a field.
    private static final String NEW_LINE = "\r\n";
    // CSV files intended for use in locales where ',' as the decimal separator
    // sometimes use ';' as a field separator instead of ','.
    private static final char[] SUPPORTED_SEPARATORS = { DEFAULT_FIELD_SEPARATOR, ';' };
    private enum ColType { SIMPLE, MULTIVALUE, NAME, TAG, DATE };

    private int lineNumber;
    private int currContactStartLineNum;
    private ArrayList<String> fieldNames; // Names of fields from first line in CSV file
    private final boolean detectFieldSeparator;
    private boolean knowFieldSeparator;
    private char fieldSeparator;

    private Mailbox mbox = null;
    private OperationContext octxt = null;

    private static Set<String> knownFields;
    private static Set<CsvFormat> knownFormats;
    private static Map <String,Character> delimiterInfo;
    private static Map <String,String> dateOrderInfo;
    private static CsvFormat defaultFormat;

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

    private ContactCSV() {
        this(DEFAULT_FIELD_SEPARATOR, true);
    }

    public ContactCSV(Mailbox mbox, OperationContext octxt) {
        this(DEFAULT_FIELD_SEPARATOR, true);
        this.mbox = mbox;
        this.octxt = octxt;
    }

    private ContactCSV(char defaultFieldSeparator, boolean detectFieldSeparator) {
        this.fieldSeparator = defaultFieldSeparator;
        this.detectFieldSeparator = detectFieldSeparator;
        // If we are not doing auto-detect, defaultFieldSeparator MUST be the separator
        this.knowFieldSeparator = !detectFieldSeparator;
    }

    /**
     * Implicit assumption, the first field name will not contain any of the supported separators.
     */
    private boolean isFieldSeparator(int testChar) {
        if (knowFieldSeparator) {
            return (testChar == fieldSeparator);
        }
        for (char possSep : SUPPORTED_SEPARATORS) {
            if (possSep == testChar) {
                LOG.debug("CSV Separator character used='%c'", possSep);
                knowFieldSeparator = true;
                fieldSeparator = possSep;
                return true;
            }
        }
        return false;
    }

    /**
     * read a line of fields into an array list. blank fields (,, or ,"",) will be null.
     */
    private boolean parseLine(BufferedReader reader, List<String> result, boolean parsingHeader)
            throws IOException, ParseException {
        currContactStartLineNum = lineNumber;
        if (parsingHeader && detectFieldSeparator) {
            knowFieldSeparator = false;
        }
        result.clear();
        int ch;
        boolean inField = false;
        while ((ch = reader.read()) != -1) {
            switch (ch) {
                case '"':
                    inField = true;
                    result.add(parseField(reader, true, -1));
                    break;
                case '\n':
                    lineNumber++;
                    if (result.isEmpty()) {
                        break;
                    } else {
                        return true;
                    }
                case '\r':
                    lineNumber++;
                    // peek for \n
                    reader.mark(1);
                    ch = reader.read();
                    if (ch != '\n') {
                        reader.reset();
                    }
                    if (result.isEmpty()) {
                        break;
                    } else {
                        return true;
                    }
                default:
                    if (isFieldSeparator(ch)) {
                        if (inField) {
                            inField = false;
                        } else {
                            result.add(null);
                        }
                    } else {
                        // start of field
                        result.add(parseField(reader, false, ch)); // eats trailing field separator
                        inField = false;
                    }
                    break;
            }
        }
        return !result.isEmpty();
    }

    /**
     * parse a field. It is assumed the leading '"' is already read. we stop at the first '"' that isn't followed by a '"'.
     * @param doubleQuotes is true if we're within a quoted string.
     * @param firstChar is the first character of the field unless its value is -1
     *
     * @return the field, or null if the field was blank ("")
     * @throws IOException IOException from the reader.
     * @throws ParseException if we reach the end of file while parsing
     */
    private String parseField(BufferedReader reader, boolean doubleQuotes, int firstChar)
            throws IOException, ParseException {
        StringBuilder result = new StringBuilder();

        if (firstChar != -1) {
            result.append((char) firstChar);
        }
        int ch;
        reader.mark(1);
        while ((ch = reader.read()) != -1) {
            if (ch == '"' && doubleQuotes) {
                reader.mark(1);
                ch = reader.read();
                if (ch != '"') {
                    reader.reset();
                    if (result.length() == 0) {
                        return null;
                    } else {
                        return result.toString();
                    }
                }
                result.append((char) ch);
            } else if (!doubleQuotes && isFieldSeparator(ch)) {
                //reader.reset();
                return result.toString();
            } else if ((ch == '\r' || ch == '\n') && !doubleQuotes) {
                reader.reset();
                return result.toString();
            } else {
                result.append((char) ch);
                if (ch == '\r') {
                    // peek for \n
                    reader.mark(1);
                    ch = reader.read();
                    if (ch == '\n') {
                        result.append((char) ch);
                    } else {
                        reader.reset();
                    }
                }
                if ((ch == '\r') || (ch == '\n')) {
                    lineNumber++;
                }
            }
            reader.mark(1);
        }
        if (doubleQuotes) {
            throw new ParseException("End of stream reached while parsing field.\nCurrent contact definition started at line "
                    + currContactStartLineNum);
        } else {
            return result.toString();
        }
    }

    /**
     * Reads the first line of .CSV data and use this information to perform some initializations.
     *
     * @param reader is the stream of .CSV data
     */
    private void initFields(BufferedReader reader) throws IOException, ParseException {
        lineNumber = 1;
        currContactStartLineNum = 1;
        fieldNames = new ArrayList<String>();

        if (!parseLine(reader, fieldNames, true)) {
            throw new ParseException("no column name definitions");
        }

        // Remove Byte-order information if present
        String firstField = fieldNames.get(0);
        if  ((firstField != null) && (firstField.length() >= 1) && (firstField.charAt(0) == 0xfeff)) {
            fieldNames.set(0, firstField.substring(1));
        }

        if (fieldNames.size() > ContactConstants.MAX_FIELD_COUNT) {
            throw new ParseException("too many columns: " + fieldNames.size());
        }

        // check that there are no missing column names
        for (int i = 0; i < fieldNames.size(); i++) {
            String field = fieldNames.get(i);
            if (Strings.isNullOrEmpty(field)) {
                throw new ParseException("missing column name for column[" + i + "]");
            }
            if (field.length() > ContactConstants.MAX_FIELD_NAME_LENGTH) {
                // doesn't look a CSV file
                throw new ParseException(String.format("invalid format - header field %d of %d is too long (length=%d)",
                        i + 1, fieldNames.size(), field.length()));
            }
        }
    }

    private void addMultiValueField(CsvColumn col, Map <String, String> fieldMap, ContactMap contact) {
        if ("birthday".equals(col.field) || "anniversary".equals(col.field)) {
            addMultiValueDateField(col, fieldMap, contact);
            return;
        }
        StringBuilder buf = new StringBuilder();
        for (String n : col.names) {
            String v = fieldMap.get(n.toLowerCase());
            if (v != null) {
                if (buf.length() > 0) {
                    buf.append("\n");
                }
                buf.append(v);
            }
        }
        if (buf.length() > 0) {
            contact.put(col.field, buf.toString());
        }
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
        if (field == null || value == null || value.length() == 0) {
            return;
        }
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
                // e.g. 2005/25/12
                zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[0], dateFs[1], dateFs[2], false);
                if (zimbraDateValue == null) {
                    // e.g. 12/25/2005
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[2], dateFs[0], dateFs[1], false);
                }
                if (zimbraDateValue == null) {
                    // e.g. 25/12/2005
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[2], dateFs[1], dateFs[0], false);
                }
                if (zimbraDateValue == null) {
                    // e.g. 25/2005/12
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[1], dateFs[2], dateFs[0], false);
                }
                if (zimbraDateValue == null) {
                    // e.g. 2005/25/12
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[0], dateFs[2], dateFs[1], false);
                }
                if (zimbraDateValue == null) {
                    // e.g. 12/2005/25
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[1], dateFs[0], dateFs[2], false);
                }
                if ((zimbraDateValue == null) && (dateFs[0] > 31)) {
                    // e.g. 2005/06/01 - realistically, no-one uses YDM, so only possibility is YMD
                    zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[0], dateFs[1], dateFs[2], true);
                }
                if (zimbraDateValue == null) {
                    String dateOrder = dateOrderInfo.get(dateOrderKey);
                    if (dateOrder != null) {
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
        if (zimbraDateValue != null) {
            contact.put(field, zimbraDateValue);
        } else {
            // We were unable to recognise the date format for this value :-(
            if (Character.isDigit(value.charAt(0))) {
                // Avoid later corruption from trying to process as a valid date.
                contact.put(field, "'" + value + "'");
            } else {
                contact.put(field, value);
            }
        }
    }

    /**
     * Multi-valued fields that map to dates should be in month/day/year order
     */
    private void addMultiValueDateField(CsvColumn col, Map <String, String> fieldMap, ContactMap contact) {
        String zimbraDateValue = null;
        if (col.names.size() == 3) {
            int dateFs[] = new int[3];
            int ndx = 0;
            try {
                for (String n : col.names) {
                    String v = fieldMap.get(n.toLowerCase());
                    if (v != null) {
                        dateFs[ndx] = Integer.parseInt(v);
                    }
                    ndx++;
                }
                zimbraDateValue = populateDateFieldsIfUnambiguous(dateFs[2], dateFs[0], dateFs[1], true);
            } catch (NumberFormatException ioe) {
            }
        }
        if (zimbraDateValue != null) {
            contact.put(col.field, zimbraDateValue);
        } else {
            StringBuilder buf = new StringBuilder();
            for (String n : col.names) {
                String v = fieldMap.get(n.toLowerCase());
                if (v != null) {
                    if (buf.length() > 0) {
                        buf.append("-");
                    }
                    buf.append(v);
                }
            }
            if (buf.length() == 0) {
                return;
            }
            zimbraDateValue = buf.toString();
            // We were unable to recognise the date format for this value :-(
            if (Character.isDigit(zimbraDateValue.charAt(0))) {
                // Avoid later corruption from trying to process as a valid date.
                contact.put(col.field, "'" + zimbraDateValue + "'");
            } else {
                contact.put(col.field, zimbraDateValue);
            }
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
        if (nameFields.length == 2) { // firstName,lastName
            lastNameField = nameFields[1];
        } else if (nameFields.length == 3) {  // firstName,middleName,lastName
            middleNameField = nameFields[1];
            lastNameField = nameFields[2];
        } else {
            // not sure how to parse this one.  just put everything in firstName field.
            contact.put(firstNameField, value);
            return;
        }

        int comma = value.indexOf(fieldSeparator);
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
        if (space < value.length()) {
            contact.put(lastNameField, value.substring(space).trim());
        }
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
        if (csv == null) {
            return contactMap.getContacts();
        }
        if (format.allFields()) {
            int end = csv.size();
            end = (end > fieldNames.size()) ? fieldNames.size() : end;
            for (int i = 0; i < end; i++) {
                contactMap.put(fieldNames.get(i), csv.get(i));
            }
        } else if (format.hasNoHeaders()) {
            int end = csv.size();
            end = (end > format.columns.size()) ? format.columns.size() : end;
            for (int i = 0; i < end; i++) {
                contactMap.put(format.columns.get(i).field, csv.get(i));
            }
        } else {
            /* Many CSV formats are output in a specific order and sometimes
             * contain duplicate field names with mappings to different
             * Zimbra contact fields.
             */
            Map <CsvColumn, Map <String, String>> pendMV = new HashMap <CsvColumn, Map <String, String>>();
            List<CsvColumn> unseenColumns = new ArrayList<CsvColumn>();
            unseenColumns.addAll(format.columns);
            for (int ndx = 0; ndx < fieldNames.size(); ndx++) {
                String csvFieldName = fieldNames.get(ndx);
                String fieldValue = (ndx >= csv.size()) ? null : csv.get(ndx );
                CsvColumn matchingCol = null;
                String matchingFieldLc = null;
                for (CsvColumn unseenC : unseenColumns) {
                    matchingFieldLc = unseenC.matchingLcCsvFieldName(csvFieldName);
                    if (matchingFieldLc == null) {
                        continue;
                    }
                    if (unseenC.colType == ColType.MULTIVALUE) {
                        Map <String, String> currMV = pendMV.get(matchingCol);
                        if ((currMV != null) && currMV.get(matchingFieldLc) != null) {
                            // already have field with this name that matches this column
                            continue;
                        }
                    }
                    matchingCol = unseenC;
                    break;
                }
                if (matchingCol == null) {
                    // unknown field - setup for adding as a user defined attribute
                    LOG.debug("Adding CSV contact attribute [%s=%s] - assuming is user defined.", csvFieldName, fieldValue);
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

// HMMM
    private List<Map<String, String>> getContactsInternal(BufferedReader reader, String fmt, String locale) throws ParseException {
        try {
            CsvFormat format = null;
            initFields(reader);

            if (fmt == null) {
                format = guessFormat(fieldNames);
            } else {
                format = getFormat(fmt, locale);
            }

            LOG.debug("getContactsInternal requested format/locale=[%s/%s]: using %s", fmt, locale, format);
            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            List<String> fields = new ArrayList<String>();

            while (parseLine(reader, fields, false)) {
                Map<String, String> contact = toContact(fields, format);
                if (contact.size() > 0)
                    result.add(contact);
            }
            return result;
        } catch (IOException e) {
            LOG.debug("Encountered IOException", e);
            throw new ParseException(e.getMessage() + " at line number " + lineNumber, e);
        }
    }

    /**
     * return a list of maps, representing contacts
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

    // <delimiters default=",">
    //   <delimiter locale="fr" format="outlook-2003-csv" char=";" />
    //    ...
    // </delimiters>
    private static void populateDelimiterInfo(Element delimiters) {
        delimiterInfo = new HashMap<String,Character>();

        for (Iterator elements = delimiters.elementIterator(DELIMITER); elements.hasNext(); ) {
            Element field = (Element) elements.next();
            String delim = field.attributeValue(ATTR_CHAR);
            if (delim == null || delim.isEmpty()) {
                continue;
            }
            String format = field.attributeValue(ATTR_FORMAT);
            if (format == null || format.isEmpty()) {
                continue;
            }
            String myLocale  = field.attributeValue(ATTR_LOCALE);
            if (myLocale != null && !myLocale.isEmpty()) {
                format = format + "/" + myLocale;
            }
            delimiterInfo.put(format, delim.charAt(0));
        }
    }

    // <dateformats>
    //   <dateformat format="yahoo-csv" order="mdy" />
    //   ...
    // </dateformats>
    private static void populateDateFormatInfo(Element dateFormats) {
        dateOrderInfo = new HashMap<String,String>();

        for (Iterator elements = dateFormats.elementIterator(DATEFORMAT); elements.hasNext(); ) {
            Element dateFormat = (Element) elements.next();
            String origOrder = dateFormat.attributeValue(ATTR_ORDER);
            if (origOrder == null || origOrder.isEmpty()) {
                continue;
            }
            String order = origOrder.toLowerCase();
            if (! (order.equals("ymd") || order.equals("ydm") || order.equals("myd") ||
                    order.equals("mdy") || order.equals("dmy") || order.equals("dym")) ) {
                LOG.debug("invalid \"order\" %s in zimbra-contact-fields.xml", origOrder);
                continue;
            }
            String format = dateFormat.attributeValue(ATTR_FORMAT);
            if (format == null || format.isEmpty()) {
                continue;
            }
            String myLocale  = dateFormat.attributeValue(ATTR_LOCALE);
            if (myLocale != null && !myLocale.isEmpty()) {
                format = format + "/" + myLocale;
            }
            dateOrderInfo.put(format, order);
        }
    }

    private static void populateFields(Element fields) {
        knownFields = new HashSet<String>();

        for (Iterator elements = fields.elementIterator(FIELD); elements.hasNext(); ) {
            Element field = (Element) elements.next();
            knownFields.add(field.attributeValue(ATTR_NAME));
        }
    }

    /**
     * ContactMap gathers up
     * It wraps a Map<String, String> and enforces policies such as
     * a new entry must use a unique case-insensitive key
     */
    private static class ContactMap {
        private final Map<String, String> contacts;
        private final Set<String> seenFields;

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

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
                .add("name", name).add("field", field).add("type", colType);
            if (colType == ColType.MULTIVALUE) {
                helper.add("cols", names);
            }
            return helper.toString();
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

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("name", name).add("locale", locale).add("flags", flags).toString();
        }

        public String key() {
            String myKey = name;
            if (locale != null) {
                myKey = myKey + "/" + locale;
            }
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

        if (knownFormats == null) {
            knownFormats = new HashSet<CsvFormat>();
        }
        knownFormats.add(fmt);
        if (fmt.hasFlag("default")) {
            defaultFormat = fmt;
        }
    }

    static {
        try {
            readMappingFile(LC.zimbra_csv_mapping_file.value());
        } catch (Exception e) {
            LOG.error("error initializing csv mapping file", e);
        }
    }

    private static void readMappingFile(String mappingFile) throws IOException, XmlParseException {
        try (FileInputStream fis = new FileInputStream(mappingFile)) {
            readMapping(fis);
        }
    }

    private static void readMapping(InputStream is) throws XmlParseException {
        delimiterInfo = new HashMap<String,Character>();
        dateOrderInfo = new HashMap<String,String>();
        Element root = W3cDomUtil.parseXMLToDom4jDocUsingSecureProcessing(is).getRootElement();
        for (Iterator elements = root.elementIterator(); elements.hasNext(); ) {
            Element elem = (Element) elements.next();
            if (elem.getQName().equals(FIELDS)) {
                populateFields(elem);
            } else if (elem.getQName().equals(FORMAT)) {
                addFormat(elem);
            } else if (elem.getQName().equals(DELIMITERS)) {
                populateDelimiterInfo(elem);
            } else if (elem.getQName().equals(DATEFORMATS)) {
                populateDateFormatInfo(elem);
            }
        }
    }

    private static CsvFormat guessFormat(List<String> keys) throws ParseException {
        if (knownFormats == null || defaultFormat == null) {
            throw new ParseException("missing config file " + LC.zimbra_csv_mapping_file.value());
        }

        int numMatchedFields;
        int numBestMatch = 0;
        CsvFormat bestMatch = defaultFormat;

        for (CsvFormat f : knownFormats) {
            numMatchedFields = 0;
            for (CsvColumn col : f.columns) {
                if (keys.contains(col.name)) {
                    numMatchedFields++;
                }
            }
            if (numMatchedFields > numBestMatch) {
                numBestMatch = numMatchedFields;
                bestMatch = f;
            }
        }
        LOG.debug("Best matching format='%s'", bestMatch);
        return bestMatch;
    }

    /**
     * Will try to match both <code>fmt</code> and <code>locale</code> first
     * If no match found, will try to match just <code>fmt</code>
     * If still no match found, returns the default format.
     *
     * @return the best matching format
     */
    private static CsvFormat getFormat(String fmt, String locale) throws ParseException {
        if (knownFormats == null || defaultFormat == null) {
            throw new ParseException("missing config file " + LC.zimbra_csv_mapping_file.value());
        }

        if (locale != null) {
            for (CsvFormat f : knownFormats) {
                if ((f.locale != null) && f.name.equals(fmt) && (f.locale.equals(locale))) {
                    return f;
                }
            }
        }
        for (CsvFormat f : knownFormats) {
            if (f.name.equals(fmt) && (f.locale == null)) {
                return f;
            }
        }
        return defaultFormat;
    }

    public void toCSV(String format, String locale, Character separator, Iterator<? extends MailItem> contacts,
            StringBuilder sb) throws ParseException, ServiceException {
        if (knownFormats == null) {
            return;
        }

        CsvFormat fmt = getFormat(format, locale);
        if (separator != null) {
            fieldSeparator = separator;
        } else {
            String delimKey = fmt.key();
            Character formatDefaultDelim = delimiterInfo.get(delimKey);
            if (formatDefaultDelim != null) {
                LOG.debug("toCSV choosing %c from <delimiter> matching %s", formatDefaultDelim, delimKey);
                fieldSeparator = formatDefaultDelim;
            }
        }
        LOG.debug("toCSV Requested=[format=\"%s\" locale=\"%s\" delim=\"%c\"] Actual=[%s delim=\"%c\"]",
                format, locale, separator, fmt, fieldSeparator);
        if (fmt == null) {
            return;
        }
        if (fmt.allFields()) {
            ArrayList<Map <String, String>> allContacts = new ArrayList<Map <String, String>>();
            HashSet<String> fields = new HashSet<String>();
            UserServletUtil.populateContactFields(contacts, mbox, octxt, allContacts, fields);
            ArrayList<String> allFields = new ArrayList<String>();
            allFields.addAll(fields);
            Collections.sort(allFields);
            addFieldDef(allFields, sb);
            for (Map <String, String> contactMap : allContacts) {
                toCSVContact(allFields, contactMap, sb);
            }
            return;
        }

        if (!fmt.hasNoHeaders()) {
            addFieldDef(fmt, sb);
        }
        while (contacts.hasNext()) {
            Object c = contacts.next();
            if (c instanceof Contact) {
                toCSVContact(fmt, (Contact) c, sb);
            }
        }
    }

    private static void addFieldValue(Map <String, String> contact, String name, String field, StringBuilder sb) {
        String value = contact.get((field == null) ? name : field);
        if (value == null) {
            value = "";
        }
        sb.append('"').append(value.replaceAll("\"", "\"\"")).append('"');
    }

    private void toCSVContact(List<String> fields, Map <String,String> contact, StringBuilder sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first) {
                sb.append(fieldSeparator);
            }
            addFieldValue(contact, f, f, sb);
            first = false;
        }
        sb.append(NEW_LINE);
    }

    private void toCSVContact(CsvFormat fmt, Contact c, StringBuilder sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first) {
                sb.append(fieldSeparator);
            }
            if (col.colType == ColType.TAG) {
                boolean firstTag = true;
                sb.append('"');
                for (String tname : c.getTags()) {
                    if (!firstTag) {
                        sb.append(fieldSeparator);
                    }
                    sb.append(tname.replace("\\", "\\\\").replace("\"", "\\\""));
                    firstTag = false;
                }
                sb.append('"');
            } else {
                addFieldValue(c.getFields(), col.name, col.field, sb);
            }
            first = false;
        }
        sb.append(NEW_LINE);
    }

    private void addFieldDef(List<String> fields, StringBuilder sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first) {
                sb.append(fieldSeparator);
            }
            sb.append('"').append(f).append('"');
            first = false;
        }
        sb.append(NEW_LINE);
    }

    private void addFieldDef(CsvFormat fmt, StringBuilder sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first) {
                sb.append(fieldSeparator);
            }
            sb.append('"').append(col.name).append('"');
            first = false;
        }
        sb.append(NEW_LINE);
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write(line.getBytes());
        out.write('\n');
    }

    private static void dump(OutputStream out) throws IOException {
        writeLine(out, "=== Fields ===");
        for (String f : new TreeSet<String>(knownFields)) {
            writeLine(out, f);
        }
        for (CsvFormat fmt : new TreeSet<CsvFormat>(knownFormats)) {
            StringBuilder sb = new StringBuilder("=== Mapping ");
            sb.append(fmt.toString()).append(" ===");
            writeLine(out, sb.toString());
            for (CsvColumn col : fmt.columns) {
                if (col.field != null) {
                    if (!knownFields.contains(col.field)) {
                        LOG.debug("Mapping '%s' references unknown field='%s'\n", fmt.name, col.field);
                    }
                }
                writeLine(out, col.toString());
            }
        }
    }

    public static String[] getAllFormatNames() {
        Set<String> formats = new HashSet<String>();
        for (CsvFormat f : knownFormats) {
            formats.add(f.name);
        }
        return formats.toArray(new String[0]);
    }

    public static void main(String args[]) throws IOException, XmlParseException {
        ZimbraLog.toolSetupLog4jConsole("INFO", true, false);
        //String mappingFile = LC.zimbra_csv_mapping_file.value();
        if (args.length > 0) {
            knownFormats = new HashSet<CsvFormat>();
            readMappingFile(args[0]);
        }
        dump(System.out);
        writeLine(System.out, "");
        System.out.print("All Format Names:");
        for (String fmtName : getAllFormatNames()) {
            System.out.print(" " + fmtName);
        }
        System.out.println();
    }
}
