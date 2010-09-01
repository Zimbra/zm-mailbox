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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Tag;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.QName;

public class ContactCSV {

    private static final String FORMAT_ZIMBRA_CSV = "zimbra-csv";

    private int mLineNumber;
    private int mCurrContactStartLineNum;
    private HashMap<String, Integer> mFieldCols;
    private ArrayList<String> mFields;

    /**
     * read a line of fields into an array list. blank fields (,, or ,"",) will be null. 
     */
    private boolean parseLine(BufferedReader reader, List<String> result, boolean parsingHeader) throws IOException, ParseException {
        mCurrContactStartLineNum = mLineNumber;
        result.clear();
        int ch;
        boolean inField = false;
        while ((ch = reader.read()) != -1) {
            switch (ch) {
                case '"':
                    inField = true;
                    result.add(parseField(reader, true, -1, parsingHeader, result.size()));
                    break;
                case ',':
                    if (inField) inField = false;
                    else result.add(null);
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
                default: // start of field
                    result.add(parseField(reader, false, ch, parsingHeader, result.size())); // eats trailing ','
                    inField = false;
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
            } else if (ch == ',' && !doubleQuotes) {
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
     */
    private void initFields(BufferedReader reader, String fmt) throws IOException, ParseException {
        mLineNumber = 1;
        mCurrContactStartLineNum = 1;
        mFields = new ArrayList<String>();

        if (!parseLine(reader, mFields, true))
            throw new ParseException("no column name definitions");

        // create mapping from CSV field name to column
        mFieldCols = new HashMap<String, Integer>(mFields.size());
        for (int i = 0; i < mFields.size(); i++) {
            String fieldName = mFields.get(i);
            if (fieldName == null || fieldName.equals(""))
                throw new ParseException("missing column name for column " + i);
            mFieldCols.put(fieldName.toLowerCase(), i);
        }
    }

    private String getField(String colName, List<String> csv) {
        Integer col = mFieldCols.get(colName.toLowerCase());
        if (col == null || col.intValue() >= csv.size())
            return null;
        else return csv.get(col);
    }

    private void addField(String colName, List<String> csv, String field, Map<String, String> contact) {
        String value = getField(colName.toLowerCase(), csv);
        if (field != null && value != null && value.length() > 0) {
            contact.put(field, value);
        }
    }

    private void addMultiValueField(List<String> names, List<String> csv, String field, Map<String,String> contact) {
        StringBuilder buf = new StringBuilder();
        for (String n : names) {
            String v = getField(n.toLowerCase(), csv);
            if (v != null) {
                if (buf.length() > 0)
                    buf.append("\n");
                buf.append(v);
            }
        }
        if (buf.length() > 0)
        	contact.put(field, buf.toString());
    }
    
    private void addNameField(String colName, List<String> csv, String field, Map<String,String> contact) {
        String value = getField(colName.toLowerCase(), csv);
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

        int comma = value.indexOf(',');
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
    private Map<String, String> toContact(List<String> csv, CsvFormat[] formats) throws ParseException {
        Map<String, String> contact = new HashMap<String, String>();

        // NOTE: We keep track of the fields we've seen to avoid dupes.
        Set<String> seenFields = new HashSet<String>();
        for (CsvFormat format : formats) {
            if (format.allFields()) {
                for (String field : mFields) {
                    String lfield = field.toLowerCase();
                    if (!seenFields.contains(lfield)) {
                        seenFields.add(lfield);
                        addField(field, csv, field, contact);
                    }
                }
            }
            else if (format.hasNoHeaders()) {
                int end = csv.size();
                end = (end > format.columns.size()) ? format.columns.size() : end;
                for (int i = 0; i < end; i++) {
                    String key = format.columns.get(i).field;
                    String val = csv.get(i);
                    if (key != null && val != null) {
                        String lfield = key.toLowerCase();
                        if (!seenFields.contains(lfield)) {
                            seenFields.add(lfield);
                            contact.put(key, val);
                        }
                    }
                }
            }
            else {
                for (CsvColumn col : format.columns) {
                    if (col.multivalue) {
                        addMultiValueField(col.names, csv, col.field, contact);
                    }
                    else if (col.isName) {
                        String lfield = col.name.toLowerCase();
                        if (!seenFields.contains(lfield)) {
                            seenFields.add(lfield);
                            addNameField(col.name, csv, col.field, contact);
                        }
                    }
                    else if (col.mapToTag) {
                        String tag = getField(col.name, csv);
                        if (tag != null)
                            contact.put(TAG, tag);
                    }
                    else {
                        String lfield = col.name.toLowerCase();
                        if (!seenFields.contains(lfield)) {
                            seenFields.add(lfield);
                            addField(col.name, csv, col.field, contact);
                        }
                    }
                }
            }
        }

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

    private List<Map<String, String>> getContactsInternal(BufferedReader reader, String fmt) throws ParseException {
        try {
            CsvFormat format = null;
            initFields(reader, fmt);
            
            if (fmt == null)
                format = guessFormat(mFields);
            else
                format = getFormat(fmt);

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            List<String> fields = new ArrayList<String>();

            // NOTE: In case we've guessed wrong, attempting to import Zimbra
            // NOTE: fields should help prevent data loss.
            CsvFormat[] formats = format.name.equals(FORMAT_ZIMBRA_CSV)
                ? new CsvFormat[] { format }
                : new CsvFormat[] { format, getFormat(FORMAT_ZIMBRA_CSV) }
            ;

            while (parseLine(reader, fields, false)) {
                Map<String, String> contact = toContact(fields, formats);
                if (contact.size() > 0)
                    result.add(contact);
            }
            return result;
        } catch (IOException ioe) {
            ZimbraLog.misc.debug("Encountered IOException", ioe);
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
    public static List<Map<String, String>> getContacts(BufferedReader reader, String fmt) throws ParseException {
        ContactCSV csv = new ContactCSV();
        return csv.getContactsInternal(reader, fmt);
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



    private static final QName FIELDS = QName.get("fields");
    private static final QName FIELD  = QName.get("field");
    private static final QName FORMAT = QName.get("format");
    private static final QName COLUMN = QName.get("column");
    
    private static final String ATTR_NAME  = "name";
    private static final String ATTR_FIELD = "field";
    private static final String ATTR_FLAG  = "flag";
    private static final String ATTR_TYPE  = "type";

    private static Set<String>    mKnownFields;
    private static Set<CsvFormat> mKnownFormats;
    private static CsvFormat      mDefaultFormat;
    
    private static void populateFields(Element fields) {
        mKnownFields = new HashSet<String>();
        
        for (Iterator elements = fields.elementIterator(FIELD); elements.hasNext(); ) {
            Element field = (Element) elements.next();
            mKnownFields.add(field.attributeValue(ATTR_NAME));
        }
    }

    private static class CsvColumn {
        String name;    // column name for this format
        String field;   // zimbra field that it maps to
        List<String> names;  // in case of multivalue mapping
        boolean multivalue;
        boolean isName;
        boolean mapToTag;
        CsvColumn(Element col) {
            names = new ArrayList<String>();
            name  = col.attributeValue(ATTR_NAME);
            field = col.attributeValue(ATTR_FIELD);
            String type = col.attributeValue(ATTR_TYPE);
            if (type == null) {
                return;
            } else if (type.equals("multivalue")) {
                multivalue = true;
                Collections.addAll(names, name.split(","));
                name = names.get(0);
            } else if (type.equals("name")) {
                isName = true;
            } else if (type.equals("tag")) {
                mapToTag = true;
            }
        }
    }
    
    private static class CsvFormat {
        String name;
        Set<String> flags;
        List<CsvColumn> columns;
        Map<String,String> forwardMapping;
        Map<String,String> reverseMapping;
        
        CsvFormat(Element fmt) {
            name = fmt.attributeValue(ATTR_NAME);
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
            ZimbraLog.misc.error("error initializing csv mapping file", e);
        }
    }
    
    private static void readMappingFile(String mappingFile) throws IOException, DocumentException {
        readMapping(new FileInputStream(mappingFile));
    }
    
    private static void readMapping(InputStream is) throws IOException, DocumentException {
        Element root = com.zimbra.common.soap.Element.getSAXReader().read(is).getRootElement();
        for (Iterator elements = root.elementIterator(); elements.hasNext(); ) {
            Element elem = (Element) elements.next();
            if (elem.getQName().equals(FIELDS))
                populateFields(elem);
            else if (elem.getQName().equals(FORMAT))
                addFormat(elem);
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
        return bestMatch;
    }
    
    private static CsvFormat getFormat(String fmt) throws ParseException {
        if (mKnownFormats == null || mDefaultFormat == null)
            throw new ParseException("missing config file "+LC.zimbra_csv_mapping_file.value());
        
        for (CsvFormat f : mKnownFormats)
            if (f.name.equals(fmt))
                return f;
        
        return mDefaultFormat;
    }
    
    public static void toCSV(String format, Iterator contacts, StringBuffer sb) throws ParseException {
        if (mKnownFormats == null)
            return;

        CsvFormat fmt = getFormat(format);

        if (fmt == null)
            return;
        
        if (fmt.allFields()) {
            ArrayList<Map> allContacts = new ArrayList<Map>();
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
            for (Map contactMap : allContacts)
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

    private static void addFieldValue(Map contact, String name, String field, StringBuffer sb) {
        String value = (String) contact.get((field == null) ? name : field);
        if (value == null) value = "";
        sb.append('"');
        sb.append(value.replaceAll("\"", "\"\""));
        sb.append('"');
    }

    private static void toCSVContact(List<String> fields, Map contact, StringBuffer sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first)
                sb.append(',');
            addFieldValue(contact, f, f, sb);
            first = false;
        }
        sb.append("\n");
    }

    private static void toCSVContact(CsvFormat fmt, Contact c, StringBuffer sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first)
                sb.append(',');
            if (col.mapToTag) {
            	try {
            		boolean firstTag = true;
                	sb.append('"');
                	for (Tag t : c.getTagList()) {
                		if (!firstTag)
                			sb.append(',');
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
    
    private static void addFieldDef(List<String> fields, StringBuffer sb) {
        boolean first = true;
        for (String f : fields) {
            if (!first)
                sb.append(',');
            sb.append('"');
            sb.append(f);
            sb.append('"');
            first = false;
        }
        sb.append("\n");
    }
    
    private static void addFieldDef(CsvFormat fmt, StringBuffer sb) {
        boolean first = true;
        for (CsvColumn col : fmt.columns) {
            if (!first)
                sb.append(',');
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
        for (String f : mKnownFields)
            writeLine(out, f);
        for (CsvFormat fmt : mKnownFormats) {
            writeLine(out, "=== Mapping " + fmt.name + " (" + fmt.flags + ")" + " ===");
            for (CsvColumn col : fmt.columns)
                writeLine(out, col.name + ": " + col.field);
        }
    }
    
    public static String[] getAllFormatNames() {
        Set<String> formats = new HashSet<String>();
        for (CsvFormat f : mKnownFormats)
            formats.add(f.name);
        return formats.toArray(new String[0]);
    }
    
    public static void main(String args[]) throws IOException, ParseException, DocumentException {
        //String mappingFile = LC.zimbra_csv_mapping_file.value();
        readMappingFile(args[0]);
        dump(System.out);
    }
}
