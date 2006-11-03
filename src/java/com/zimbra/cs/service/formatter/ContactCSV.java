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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.formatter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.zimbra.cs.mailbox.Contact;

public class ContactCSV {

    /* canonical list of CSV fields */

    public static final String CSV_Account = "Account";
    public static final String CSV_Alternate_Email_1 = "Alternate Email 1";
    public static final String CSV_Alternate_Email_2 = "Alternate Email 2";    
    public static final String CSV_Anniversary = "Anniversary";
    public static final String CSV_Assistant_s_Name = "Assistant's Name";
    public static final String CSV_Assistant_s_Phone = "Assistant's Phone";
    public static final String CSV_Billing_Information = "Billing Information";
    public static final String CSV_Birthday = "Birthday";
    public static final String CSV_Business_Address_PO_Box = "Business Address PO Box";
    public static final String CSV_Business_City = "Business City";
    public static final String CSV_Business_Country = "Business Country";
    public static final String CSV_Business_Fax = "Business Fax";
    public static final String CSV_Business_Phone = "Business Phone";
    public static final String CSV_Business_Phone_2 = "Business Phone 2";
    public static final String CSV_Business_Postal_Code = "Business Postal Code";
    public static final String CSV_Business_State = "Business State";
    public static final String CSV_Business_Street = "Business Street";
    public static final String CSV_Business_Street_2 = "Business Street 2";
    public static final String CSV_Business_Street_3 = "Business Street 3";
    public static final String CSV_Business_Website = "Business Website";    
    public static final String CSV_Callback = "Callback";
    public static final String CSV_Car_Phone = "Car Phone";
    public static final String CSV_Categories = "Categories";
    public static final String CSV_Children = "Children";
    public static final String CSV_Comments = "Comments";
    public static final String CSV_Company = "Company";
    public static final String CSV_Company_Main_Phone = "Company Main Phone";
    public static final String CSV_Dlist = "dlist";
    public static final String CSV_Department = "Department";
    public static final String CSV_Directory_Server = "Directory Server";
    public static final String CSV_E_mail_2_Address = "E-mail 2 Address";
    public static final String CSV_E_mail_2_Display_Name = "E-mail 2 Display Name";
    public static final String CSV_E_mail_2_Type = "E-mail 2 Type";
    public static final String CSV_E_mail_3_Address = "E-mail 3 Address";
    public static final String CSV_E_mail_3_Display_Name = "E-mail 3 Display Name";
    public static final String CSV_E_mail_3_Type = "E-mail 3 Type";
    public static final String CSV_E_mail_Address = "E-mail Address";
    public static final String CSV_Email = "Email";    
    public static final String CSV_E_mail = "E-mail";
    public static final String CSV_E_mail_2 = "E-mail 2";
    public static final String CSV_E_mail_Display_Name = "E-mail Display Name";
    public static final String CSV_E_mail_Type = "E-mail Type";
    public static final String CSV_Fax = "Fax";
    public static final String CSV_FileAs = "fileAs";        
    public static final String CSV_First = "First";    
    public static final String CSV_First_Name = "First Name";
    public static final String CSV_Gender = "Gender";
    public static final String CSV_Government_ID_Number = "Government ID Number";
    public static final String CSV_Hobby = "Hobby";
    public static final String CSV_Home = "Home";
    public static final String CSV_Home_Address = "Home Address";
    public static final String CSV_Home_Address_2 = "Home Address 2";
    public static final String CSV_Home_Address_PO_Box = "Home Address PO Box";
    public static final String CSV_Home_City = "Home City";
    public static final String CSV_Home_Country = "Home Country";
    public static final String CSV_Home_Fax = "Home Fax";
    public static final String CSV_Home_Phone = "Home Phone";
    public static final String CSV_Home_Phone_2 = "Home Phone 2";
    public static final String CSV_Home_Postal_Code = "Home Postal Code";
    public static final String CSV_Home_State = "Home State";
    public static final String CSV_Home_Street = "Home Street";
    public static final String CSV_Home_Street_2 = "Home Street 2";
    public static final String CSV_Home_Street_3 = "Home Street 3";
    public static final String CSV_Home_ZIP = "Home ZIP";
    public static final String CSV_Home_WebPage = "Home WebPage";
    public static final String CSV_ISDN = "ISDN";
    public static final String CSV_Initials = "Initials";
    public static final String CSV_Internet_Free_Busy = "Internet Free Busy";
    public static final String CSV_Job_Title = "Job Title";
    public static final String CSV_Keywords = "Keywords";
    public static final String CSV_Language = "Language";
    public static final String CSV_Last = "Last";    
    public static final String CSV_Last_Name = "Last Name";
    public static final String CSV_Location = "Location";
    public static final String CSV_Manager_s_Name = "Manager's Name";
    public static final String CSV_Middle = "Middle";
    public static final String CSV_Middle_Name = "Middle Name";
    public static final String CSV_Mileage = "Mileage";
    public static final String CSV_Mobile = "Mobile";
    public static final String CSV_Mobile_Phone = "Mobile Phone";
    public static final String CSV_Name = "Name";    
    public static final String CSV_Notes = "Notes";
    public static final String CSV_Office_Location = "Office Location";
    public static final String CSV_Organizational_ID_Number = "Organizational ID Number";
    public static final String CSV_Other = "Other";    
    public static final String CSV_Other_Address_PO_Box = "Other Address PO Box";
    public static final String CSV_Other_City = "Other City";
    public static final String CSV_Other_Country = "Other Country";
    public static final String CSV_Other_Fax = "Other Fax";
    public static final String CSV_Other_Phone = "Other Phone";
    public static final String CSV_Other_Postal_Code = "Other Postal Code";
    public static final String CSV_Other_State = "Other State";
    public static final String CSV_Other_Street = "Other Street";
    public static final String CSV_Other_Street_2 = "Other Street 2";
    public static final String CSV_Other_Street_3 = "Other Street 3";
    public static final String CSV_Pager = "Pager";
    public static final String CSV_Personal_Website = "Personal Website";
    public static final String CSV_Phone_Home = "Phone Home";
    public static final String CSV_Phone_Work = "Phone Work";
    public static final String CSV_Primary_Phone = "Primary Phone";
    public static final String CSV_Priority = "Priority";
    public static final String CSV_Private = "Private";
    public static final String CSV_Profession = "Profession";
    public static final String CSV_Radio_Phone = "Radio Phone";
    public static final String CSV_Referred_By = "Referred By";
    public static final String CSV_Sensitivity = "Sensitivity";
    public static final String CSV_Spouse = "Spouse";
    public static final String CSV_Suffix = "Suffix";
    public static final String CSV_TTY_TDD_Phone = "TTY/TDD Phone";
    public static final String CSV_Telex = "Telex";
    public static final String CSV_Title = "Title";
    public static final String CSV_User_1 = "User 1";
    public static final String CSV_User_2 = "User 2";
    public static final String CSV_User_3 = "User 3";
    public static final String CSV_User_4 = "User 4";
    public static final String CSV_Web_Page = "Web Page";
    public static final String CSV_Website = "Website";
    public static final String CSV_Website_2 = "Website 2";
    public static final String CSV_Work = "Work";    
    public static final String CSV_Work_Address = "Work Address";
    public static final String CSV_Work_Address_2 = "Work Address 2";
    public static final String CSV_Work_WebPage = "Work WebPage";    
    public static final String CSV_Work_ZIP = "Work ZIP";

    private static final int OP_MAP = 1; 
    private static final int OP_EMAIL = 2;     
    private static final int OP_STREETADDRESS = 3;
    private static final int OP_NAME = 4;

    private static List<Mapping> sMappings = new ArrayList<Mapping>();
    
    private static class Mapping {
        private Object mCsvName;
        private String mContactName;
        private int mOp;
        boolean mIncludeInExport;
        
        Mapping(String csvName, String contactName, int op) {
            mCsvName = csvName;
            mContactName = contactName;
            mOp = op;
            mIncludeInExport = true;
        }

        Mapping(String csvNames[], String contactName, int op) {
            mCsvName = csvNames;
            mContactName = contactName;
            mOp = op;
            mIncludeInExport = true;            
        }

        public int getOp() { return mOp; }
        public boolean hasMultiple() { return mCsvName instanceof String[]; }
        public boolean includeInExport() { return mIncludeInExport; }
        public void setIncludeInExport(boolean value) { mIncludeInExport = value; }
        public String getCsvName() { return (String)mCsvName; }
        public String[] getCsvNames() { return (String[])mCsvName; }        
        public String getContactName() { return mContactName; }
    }

    private static Mapping addMapping(String csvName, String contactName, int op, boolean includeInExport) {
        Mapping m = new Mapping(csvName, contactName, op);
        m.setIncludeInExport(includeInExport);
        sMappings.add(m);
        return m;
    }

    private static Mapping addMapping(String csvName, String contactName, int op) {
        return addMapping(csvName, contactName, op, true);
    }
    
    private static Mapping addMapping(String[] csvName, String contactName, int op, boolean includeInExport) {
        Mapping m = new Mapping(csvName, contactName, op);
        m.setIncludeInExport(includeInExport);
        sMappings.add(m);
        return m;
    }    

    private static Mapping addMapping(String[] csvName, String contactName, int op) {
        return addMapping(csvName, contactName, op, true);
    }    
    
    static {
        //CSV_Account
        //CSV_Anniversary
        //CSV_Assistant_s_Name
        //CSV_Assistant_s_Phone
        //CSV_Billing_Information
        addMapping(CSV_Birthday, Contact.A_birthday, OP_MAP);
        //CSV_Business_Address_PO_Box
        
        addMapping(CSV_Alternate_Email_1, Contact.A_email2, OP_MAP, false);
        addMapping(CSV_Alternate_Email_2, Contact.A_email3, OP_MAP, false);        
        
        
        addMapping(CSV_Business_City, Contact.A_workCity, OP_MAP);
        addMapping(CSV_Business_Country, Contact.A_workCountry, OP_MAP);
        addMapping(CSV_Business_Fax, Contact.A_workFax, OP_MAP);
        addMapping(CSV_Business_Phone, Contact.A_workPhone, OP_MAP);
        addMapping(CSV_Business_Phone_2, Contact.A_workPhone2, OP_MAP);
        addMapping(CSV_Business_Postal_Code, Contact.A_workPostalCode, OP_MAP);
        addMapping(CSV_Business_State, Contact.A_workState, OP_MAP);
        addMapping(CSV_Business_Website, Contact.A_workURL, OP_MAP, false);        
        addMapping(new String[] { CSV_Work_Address, CSV_Work_Address_2}, Contact.A_workStreet, OP_STREETADDRESS, false);
        addMapping(new String[] { CSV_Business_Street, CSV_Business_Street_2, CSV_Business_Street_3} , Contact.A_workStreet, OP_STREETADDRESS);
        addMapping(CSV_Callback, Contact.A_callbackPhone, OP_MAP);
        addMapping(CSV_Car_Phone, Contact.A_carPhone, OP_MAP);
        //CSV_Categories
        //CSV_Children
        addMapping(CSV_Comments, Contact.A_notes, OP_MAP, false);        
        addMapping(CSV_Company, Contact.A_company, OP_MAP);
        addMapping(CSV_Company_Main_Phone, Contact.A_companyPhone, OP_MAP);
        addMapping(CSV_Department, Contact.A_department, OP_MAP);
        //CSV_Directory_Server

        addMapping(CSV_Dlist, Contact.A_dlist, OP_MAP);
        
        addMapping(CSV_Email, Contact.A_email, OP_MAP, false);        
        addMapping(CSV_E_mail, Contact.A_email, OP_MAP, false);        
        addMapping(CSV_E_mail_2, Contact.A_email2, OP_MAP, false);
        
        addMapping(new String[] {CSV_E_mail_Address, CSV_E_mail_Display_Name, CSV_E_mail_Type}, Contact.A_email, OP_EMAIL);
        addMapping(new String[] {CSV_E_mail_2_Address, CSV_E_mail_2_Display_Name, CSV_E_mail_2_Type}, Contact.A_email2, OP_EMAIL);
        addMapping(new String[] {CSV_E_mail_3_Address, CSV_E_mail_3_Display_Name, CSV_E_mail_3_Type}, Contact.A_email3, OP_EMAIL);
        
        addMapping(CSV_Fax, Contact.A_workFax, OP_MAP, false);

        addMapping(CSV_FileAs, Contact.A_fileAs, OP_MAP);
        
        addMapping(CSV_First, Contact.A_firstName, OP_MAP, false);        
        addMapping(CSV_First_Name, Contact.A_firstName, OP_MAP);
        //CSV_Gender
        //CSV_Government_ID_Number
        //CSV_Hobby
        //CSV_Home_Address_PO_Box
        addMapping(CSV_Home, Contact.A_homePhone, OP_MAP, false);        
        addMapping(CSV_Home_City, Contact.A_homeCity, OP_MAP);
        addMapping(CSV_Home_Country, Contact.A_homeCountry, OP_MAP);
        addMapping(CSV_Home_Fax, Contact.A_homeFax, OP_MAP);
        addMapping(CSV_Home_Phone, Contact.A_homePhone, OP_MAP);
        addMapping(CSV_Home_Phone_2, Contact.A_homePhone2 , OP_MAP);
        addMapping(CSV_Home_Postal_Code, Contact.A_homePostalCode, OP_MAP);
        addMapping(CSV_Home_State, Contact.A_homeState, OP_MAP);
        addMapping(new String[] {CSV_Home_Street, CSV_Home_Street_2, CSV_Home_Street_3}, Contact.A_homeStreet, OP_STREETADDRESS);
        addMapping(new String[] {CSV_Home_Address, CSV_Home_Address_2}, Contact.A_homeStreet, OP_STREETADDRESS, false);
        addMapping(CSV_Home_ZIP, Contact.A_homePostalCode, OP_MAP, false);
        
        //CSV_ISDN
        addMapping(CSV_Initials, Contact.A_initials, OP_MAP);
        //CSV_Internet_Free_Busy
        addMapping(CSV_Job_Title, Contact.A_jobTitle, OP_MAP);
        //CSV_Keywords
        //CSV_Language
        addMapping(CSV_Last, Contact.A_lastName, OP_MAP, false);        
        addMapping(CSV_Last_Name, Contact.A_lastName, OP_MAP);
        //CSV_Location
        //CSV_Manager_s_Name
        addMapping(CSV_Middle, Contact.A_middleName, OP_MAP, false);        
        addMapping(CSV_Middle_Name, Contact.A_middleName, OP_MAP);
        //CSV_Mileage
        addMapping(CSV_Mobile_Phone, Contact.A_mobilePhone, OP_MAP);
        addMapping(CSV_Mobile, Contact.A_mobilePhone, OP_MAP, false);

        addMapping(CSV_Name, null, OP_NAME, false);
        
        addMapping(CSV_Notes, Contact.A_notes, OP_MAP);
        //CSV_Office_Location
        //CSV_Organizational_ID_Number
        //CSV_Other_Address_PO_Box
        addMapping(CSV_Other, Contact.A_otherPhone, OP_MAP, false);                
        addMapping(CSV_Other_City, Contact.A_otherCity, OP_MAP);
        addMapping(CSV_Other_Country, Contact.A_otherCountry, OP_MAP);
        addMapping(CSV_Other_Fax, Contact.A_otherFax, OP_MAP);
        addMapping(CSV_Other_Phone, Contact.A_otherPhone, OP_MAP);
        addMapping(CSV_Other_Postal_Code, Contact.A_otherPostalCode, OP_MAP);
        addMapping(CSV_Other_State, Contact.A_otherState, OP_MAP);
        addMapping(new String[] {CSV_Other_Street, CSV_Other_Street_2, CSV_Other_Street_3}, Contact.A_otherStreet, OP_STREETADDRESS);
        
        addMapping(CSV_Pager, Contact.A_pager, OP_MAP);
        addMapping(CSV_Personal_Website, Contact.A_homeURL, OP_MAP, false);        
        addMapping(CSV_Phone_Work, Contact.A_workPhone, OP_MAP, false);
        addMapping(CSV_Phone_Home, Contact.A_homePhone, OP_MAP, false);

        //addMapping(CSV_Primary_Phone, Contact.A_, OP_MAP);
        //CSV_Priority
        //CSV_Private
        //CSV_Profession
        //CSV_Radio_Phone
        //CSV_Referred_By
        //CSV_Sensitivity
        //CSV_Spouse
        addMapping(CSV_Suffix, Contact.A_nameSuffix, OP_MAP);
        //CSV_TTY_TDD_Phone
        //CSV_Telex
        //addMapping(CSV_Title, Contact.A_Title, OP_MAP); // need name title in our contacts
        //CSV_User_1
        //CSV_User_2
        //CSV_User_3
        //CSV_User_4
        addMapping(CSV_Web_Page, Contact.A_homeURL, OP_MAP);
        addMapping(CSV_Website, Contact.A_workURL, OP_MAP, false);
        addMapping(CSV_Website_2, Contact.A_homeURL, OP_MAP, false);
        addMapping(CSV_Home_WebPage, Contact.A_homeURL, OP_MAP, false);
        addMapping(CSV_Work, Contact.A_workPhone, OP_MAP, false);
        addMapping(CSV_Work_WebPage, Contact.A_workURL, OP_MAP, false);
        addMapping(CSV_Work_ZIP, Contact.A_workPostalCode, OP_MAP, false);
    };

    private HashMap<String, Integer> mFieldCols;
    private ArrayList<String> mFields;

    /**
     * read a line of fields into an array list. blank fields (,, or ,"",) will be null. 
     */
    private boolean parseLine(BufferedReader reader, List<String> result, boolean parsingHeader) throws IOException, ParseException {
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
                    return result.size() > 0;
                case '\r':
                    // peek for \n
                    reader.mark(1);
                    ch = reader.read();
                    if (ch != '\n') reader.reset();
                    return result.size() > 0;
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
        boolean lastField = !parsingHeader && size == mFields.size()-1;
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
            } else if ((ch == '\r' || ch == '\n') && !doubleQuotes && (parsingHeader || lastField)) {
                reader.reset();
                return sb.toString();
            } else {
                sb.append((char)ch);                
            }
            reader.mark(1);            
        }
        if (doubleQuotes)
            throw new ParseException("end of stream reached while parsing field");
        else 
            return sb.toString();
    }
    
    private int getColumn(String field) {
        Integer col = mFieldCols.get(field.toLowerCase());
        return col == null ? -1 : col.intValue();
    }

    /**
     */
    private void initFields(BufferedReader reader) throws IOException, ParseException {
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
        String value = getField(colName, csv);
        if (value != null && value.length() > 0) {
            contact.put(field, value);
        }
    }

    private void addStreetField(String cols[], List<String> csv, String field, Map<String, String> contact) {
        StringBuffer sb = new StringBuffer();        
        for (int i=0; i < cols.length; i++) {
            String value = getField(cols[i], csv);
            if (value != null) {
                if (sb.length() != 0) sb.append("\n");
                sb.append(value);
            }
        }
        if (sb.length() > 0) contact.put(field, sb.toString());
    }

    private void addEmailField(String cols[], List<String> csv, String field, Map<String, String> contact) {
        String colAddress = cols[0];
        String colDisplayName = cols[1];
        String colType = cols[2];        
        String type = getField(colType, csv);
        //String displayName = (String) csv.get(colDisplayName);        
        if (type == null || type.equalsIgnoreCase("smtp")) {
            String address = getField(colAddress, csv);
            if (address != null && address.length() > 0 )
                contact.put(field, address);            
        }
    }

    private void addNameField(String name, List<String> csv, Map<String, String> contact) {
        String value = getField(name, csv);
        if (value != null) {
            if (value.indexOf(',') != -1) {
                String[] values = value.split(",\\s*", 2);
                if (values == null || values.length == 0)
                    contact.put(Contact.A_lastName, value);
                else {
                    if (values.length == 1) {
                        contact.put(Contact.A_lastName, values[0]);
                    } else {
                        contact.put(Contact.A_lastName, values[0]);
                        contact.put(Contact.A_firstName, values[1]);                    
                    }
                }
            } else {
                String[] values = value.split("\\s+", 2);
                if (values == null || values.length == 0)
                    contact.put(Contact.A_firstName, value);
                else {
                    if (values.length == 1) {
                        contact.put(Contact.A_lastName, values[0]);
                    } else {
                        contact.put(Contact.A_firstName, values[0]);
                        contact.put(Contact.A_lastName, values[1]);                    
                    }
                }
            }
        }
    }

    private Map<String, String> toContact(List<String> csv) {
        Map<String, String> contact = new HashMap<String, String>();

        for (Mapping mp : sMappings) {
            switch(mp.getOp()) {
                case OP_MAP:
                    addField(mp.getCsvName(), csv, mp.getContactName(), contact);
                    break;
                case OP_STREETADDRESS:
                    addStreetField(mp.getCsvNames(), csv, mp.getContactName(), contact);                    
                    break;
                case OP_EMAIL:
                    addEmailField(mp.getCsvNames(), csv, mp.getContactName(), contact);
                    break;
                case OP_NAME: 
                    addNameField(mp.getCsvName(), csv, contact);
                    break;
            }
        }
        return contact;
    }

    /**
     * return a list of maps, representing contacts
     * @param r
     * @return
     * @throws ParseException 
     * @throws IOException 
     */
    private List<Map<String, String>> getContactsInternal(BufferedReader reader) throws ParseException {
        try {
            initFields(reader);

            List<Map<String, String>> result = new ArrayList<Map<String, String>>();
            List<String> fields = new ArrayList<String>();
            
            while (parseLine(reader, fields, false)) {
                Map<String, String> contact = toContact(fields);
                if (contact.size() > 0)
                    result.add(contact);
            }
            return result;
        } catch (IOException ioe) {
            throw new ParseException(ioe.getMessage(), ioe);
        }
    }

    /**
     * return a list of maps, representing contacts
     * @param r
     * @return
     * @throws ParseException 
     * @throws IOException 
     */
    public static List<Map<String, String>> getContacts(BufferedReader reader) throws ParseException {
        ContactCSV csv = new ContactCSV();
        return csv.getContactsInternal(reader);
    }
    
    private static void addFieldDef(String field, StringBuffer sb) {
        if (sb.length() > 0) sb.append(",");
        sb.append('"');
        sb.append(field.replaceAll("\"", "\"\""));
        sb.append('"');        
    }

    private static void addFieldValue(Map contact, String field, StringBuffer sb, boolean isFirst) {
        if (!isFirst) sb.append(",");
        String value = (String) contact.get(field);
        if (value == null) value = "";
        sb.append('"');
        sb.append(value.replaceAll("\"", "\"\""));
        sb.append('"');        
    }
 
    private static void addStreetFieldValue(Map contact, String cols[], String field, StringBuffer sb, boolean isFirst) {
        // TODO: split them back into cols.length fields    
        for (int i = 0; i < cols.length; i++) {
            if (i == 0) {
                addFieldValue(contact, field, sb, isFirst);
            } else {
                sb.append(",\"\"");
            }
        }
    }

    private static void addEmailFieldValue(Map contact, String field, StringBuffer sb, boolean isFirst) {
        // address, displayName, type
        addFieldValue(contact, field, sb, isFirst);
        sb.append(",\"\"");
        sb.append(",\"\"");
    }

    private static void toCSVContact(Map contact, StringBuffer sb) {
        boolean isFirst = true;
        for (Mapping mp : sMappings) {
            if (!mp.includeInExport())
                continue;
            switch(mp.getOp()) {
                case OP_MAP:
                    addFieldValue(contact, mp.getContactName(), sb, isFirst);
                    break;
                case OP_STREETADDRESS:
                    addStreetFieldValue(contact, mp.getCsvNames(), mp.getContactName(), sb, isFirst);
                    break;
                case OP_EMAIL:
                    addEmailFieldValue(contact, mp.getContactName(), sb, isFirst);
                    break;
            }
            if (isFirst) isFirst = false;
        }
        sb.append("\n");        
    }

    public static void toCSV(List contacts, StringBuffer sb) {
        toCSV(contacts.iterator(), sb);
    }        

    public static void toCSV(Iterator contacts, StringBuffer sb) {
        for (Mapping mp : sMappings) {
            if (mp.includeInExport()) {
                if (mp.hasMultiple()) {
                    for (String name : mp.getCsvNames())
                        addFieldDef(name, sb);
                } else {
                    addFieldDef(mp.getCsvName(), sb);
                }
            }
        }

        sb.append("\n");
        
        while (contacts.hasNext()) {
            Object c = contacts.next();
            if (c instanceof Contact) {
                Contact contact = (Contact) c;
                toCSVContact(contact.getFields(), sb);
            } else if (c instanceof Map) {
                Map map = (Map) c;
                toCSVContact(map, sb);
            }
        }
    } 

    public static class ParseException extends Exception {
        ParseException(String msg) {
            super(msg);
        }
        
        ParseException(String msg, Throwable cause) {
            super(msg, cause);
        }        
    }

    private static void doFile(String fileName) throws IOException, ParseException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        //ContactCSV csv = new ContactCSV();
        
        List<Map<String, String>> list = ContactCSV.getContacts(reader);

        for (int n = 0; n < list.size(); n++) {
            Map<String, String> contact = list.get(n);
            TreeMap<String, String> tm = new TreeMap<String, String>(contact);
            System.out.println("contact: " + n);
            for (Map.Entry entry : tm.entrySet())
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("=========================");
        StringBuffer sb = new StringBuffer();
        toCSV(list, sb);
        System.out.println("-------------------------");
        System.out.println(sb);
        System.out.println("=========================");
                
    }

    public static void main(String args[]) throws IOException, ParseException {
        //doFile("/tmp/a.csv");        
        doFile("/tmp/b.csv");
        //doFile("/tmp/c.csv");
        //doFile("/tmp/d.csv");
        //doFile("/tmp/e.csv");
    }
    
}
