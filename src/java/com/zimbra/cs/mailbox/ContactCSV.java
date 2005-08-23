/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class ContactCSV {

    /* canonical list of CSV fields */

    public static final String CSV_Account = "Account";
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
    public static final String CSV_Callback = "Callback";
    public static final String CSV_Car_Phone = "Car Phone";
    public static final String CSV_Categories = "Categories";
    public static final String CSV_Children = "Children";
    public static final String CSV_Company = "Company";
    public static final String CSV_Company_Main_Phone = "Company Main Phone";
    public static final String CSV_Department = "Department";
    public static final String CSV_Directory_Server = "Directory Server";
    public static final String CSV_E_mail_2_Address = "E-mail 2 Address";
    public static final String CSV_E_mail_2_Display_Name = "E-mail 2 Display Name";
    public static final String CSV_E_mail_2_Type = "E-mail 2 Type";
    public static final String CSV_E_mail_3_Address = "E-mail 3 Address";
    public static final String CSV_E_mail_3_Display_Name = "E-mail 3 Display Name";
    public static final String CSV_E_mail_3_Type = "E-mail 3 Type";
    public static final String CSV_E_mail_Address = "E-mail Address";
    public static final String CSV_E_mail_Display_Name = "E-mail Display Name";
    public static final String CSV_E_mail_Type = "E-mail Type";
    public static final String CSV_First_Name = "First Name";
    public static final String CSV_Gender = "Gender";
    public static final String CSV_Government_ID_Number = "Government ID Number";
    public static final String CSV_Hobby = "Hobby";
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
    public static final String CSV_ISDN = "ISDN";
    public static final String CSV_Initials = "Initials";
    public static final String CSV_Internet_Free_Busy = "Internet Free Busy";
    public static final String CSV_Job_Title = "Job Title";
    public static final String CSV_Keywords = "Keywords";
    public static final String CSV_Language = "Language";
    public static final String CSV_Last_Name = "Last Name";
    public static final String CSV_Location = "Location";
    public static final String CSV_Manager_s_Name = "Manager's Name";
    public static final String CSV_Middle_Name = "Middle Name";
    public static final String CSV_Mileage = "Mileage";
    public static final String CSV_Mobile_Phone = "Mobile Phone";
    public static final String CSV_Notes = "Notes";
    public static final String CSV_Office_Location = "Office Location";
    public static final String CSV_Organizational_ID_Number = "Organizational ID Number";
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

    private static final int OP_MAP = 1; 
    private static final int OP_EMAIL = 2;     
    private static final int OP_STREETADDRESS = 3;         

    private static List sMappings = new ArrayList();
    
    private static class Mapping {
        private Object mCsvName;
        private String mContactName;
        private int mOp;
        
        Mapping(String csvName, String contactName, int op) {
            mCsvName = csvName;
            mContactName = contactName;
            mOp = op;
        }

        Mapping(String csvNames[], String contactName, int op) {
            mCsvName = csvNames;
            mContactName = contactName;
            mOp = op;
        }

        public int getOp() { return mOp; }
        public boolean hasMultiple() { return mCsvName instanceof String[]; }
        public String getCsvName() { return (String)mCsvName; }
        public String[] getCsvNames() { return (String[])mCsvName; }        
        public String getContactName() { return mContactName; }
    }

    private static void addMapping(String csvName, String contactName, int op) {
        sMappings.add(new Mapping(csvName, contactName, OP_MAP));
    }
    
    private static void addMapping(String[] csvName, String contactName, int op) {
        sMappings.add(new Mapping(csvName, contactName, op));
    }    
    
    static {
        //CSV_Account
        //CSV_Anniversary
        //CSV_Assistant_s_Name
        //CSV_Assistant_s_Phone
        //CSV_Billing_Information
        //CSV_Birthday
        //CSV_Business_Address_PO_Box
        addMapping(CSV_Business_City, Contact.A_workCity, OP_MAP);
        addMapping(CSV_Business_Country, Contact.A_workCountry, OP_MAP);
        addMapping(CSV_Business_Fax, Contact.A_workFax, OP_MAP);
        addMapping(CSV_Business_Phone, Contact.A_workPhone, OP_MAP);
        addMapping(CSV_Business_Phone_2, Contact.A_workPhone2, OP_MAP);
        addMapping(CSV_Business_Postal_Code, Contact.A_workPostalCode, OP_MAP);
        addMapping(CSV_Business_State, Contact.A_workState, OP_MAP);
        addMapping(new String[] { CSV_Business_Street, CSV_Business_Street_2, CSV_Business_Street_3} , Contact.A_workStreet, OP_STREETADDRESS);
        addMapping(CSV_Callback, Contact.A_callbackPhone, OP_MAP);
        addMapping(CSV_Car_Phone, Contact.A_carPhone, OP_MAP);
        //CSV_Categories
        //CSV_Children
        addMapping(CSV_Company, Contact.A_company, OP_MAP);
        addMapping(CSV_Company_Main_Phone, Contact.A_companyPhone, OP_MAP);
        addMapping(CSV_Department, Contact.A_department, OP_MAP);
        //CSV_Directory_Server

        addMapping(new String[] {CSV_E_mail_Address, CSV_E_mail_Display_Name, CSV_E_mail_Type}, Contact.A_email, OP_EMAIL);
        addMapping(new String[] {CSV_E_mail_2_Address, CSV_E_mail_2_Display_Name, CSV_E_mail_2_Type}, Contact.A_email2, OP_EMAIL);
        addMapping(new String[] {CSV_E_mail_3_Address, CSV_E_mail_3_Display_Name, CSV_E_mail_3_Type}, Contact.A_email3, OP_EMAIL);
        
        addMapping(CSV_First_Name, Contact.A_firstName, OP_MAP);
        //CSV_Gender
        //CSV_Government_ID_Number
        //CSV_Hobby
        //CSV_Home_Address_PO_Box
        addMapping(CSV_Home_City, Contact.A_homeCity, OP_MAP);
        addMapping(CSV_Home_Country, Contact.A_homeCountry, OP_MAP);
        addMapping(CSV_Home_Fax, Contact.A_homeFax, OP_MAP);
        addMapping(CSV_Home_Phone, Contact.A_homePhone, OP_MAP);
        addMapping(CSV_Home_Phone_2, Contact.A_homePhone2 , OP_MAP);
        addMapping(CSV_Home_Postal_Code, Contact.A_homePostalCode, OP_MAP);
        addMapping(CSV_Home_State, Contact.A_homeState, OP_MAP);
        addMapping(new String[] {CSV_Home_Street, CSV_Home_Street_2, CSV_Home_Street_3}, Contact.A_homeStreet, OP_STREETADDRESS);
        
        //CSV_ISDN
        addMapping(CSV_Initials, Contact.A_initials, OP_MAP);
        //CSV_Internet_Free_Busy
        addMapping(CSV_Job_Title, Contact.A_jobTitle, OP_MAP);
        //CSV_Keywords
        //CSV_Language
        addMapping(CSV_Last_Name, Contact.A_lastName, OP_MAP);
        //CSV_Location
        //CSV_Manager_s_Name
        addMapping(CSV_Middle_Name, Contact.A_middleName, OP_MAP);
        //CSV_Mileage
        addMapping(CSV_Mobile_Phone, Contact.A_mobilePhone, OP_MAP);
        addMapping(CSV_Notes, Contact.A_notes, OP_MAP);
        //CSV_Office_Location
        //CSV_Organizational_ID_Number
        //CSV_Other_Address_PO_Box
        addMapping(CSV_Other_City, Contact.A_otherCity, OP_MAP);
        addMapping(CSV_Other_Country, Contact.A_otherCountry, OP_MAP);
        addMapping(CSV_Other_Fax, Contact.A_otherFax, OP_MAP);
        addMapping(CSV_Other_Phone, Contact.A_otherPhone, OP_MAP);
        addMapping(CSV_Other_Postal_Code, Contact.A_otherPostalCode, OP_MAP);
        addMapping(CSV_Other_State, Contact.A_otherState, OP_MAP);
        addMapping(new String[] {CSV_Other_Street, CSV_Other_Street_2, CSV_Other_Street_3}, Contact.A_otherStreet, OP_STREETADDRESS);
        
        addMapping(CSV_Pager, Contact.A_pager, OP_MAP);
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
        addMapping(CSV_Title, Contact.A_jobTitle, OP_MAP); // reconcile with CSV_jobTitle
        //CSV_User_1
        //CSV_User_2
        //CSV_User_3
        //CSV_User_4
        addMapping(CSV_Web_Page, Contact.A_workURL, OP_MAP);
    };

    private HashMap mFieldCols;
    private ArrayList mFields;
    private int mNumFields;

    /**
     * read a line of fields into an array list. blank fields (,, or ,"",) will be null. 
     */
    private static boolean parseLine(BufferedReader reader, List result) throws IOException, ParseException {
        result.clear();
        int ch;
        boolean inField = false;
        while ((ch = reader.read()) != -1) {
            switch (ch) {
                case '"':
                    inField = true;
                    result.add(parseField(reader, true, -1));
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
                    result.add(parseField(reader, false, ch)); // eats trailing ','
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
    private static String parseField(BufferedReader reader, boolean doubleQuotes, int firstChar) throws IOException, ParseException {
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
                break;
            } else if (ch == ',' && !doubleQuotes) {
                //reader.reset();
                return sb.toString();
            } else if (ch == '\r' || ch == '\n') {
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
        Integer col = (Integer) mFieldCols.get(field);
        return col == null ? -1 : col.intValue();
    }

    /**
     */
    private void initFields(BufferedReader reader) throws IOException, ParseException {
        mFields = new ArrayList();
        
        parseLine(reader, mFields);
        
        // create mapping from CSV field name to column
        mFieldCols = new HashMap(mFields.size());
        for (int i=0; i < mFields.size(); i++)
            mFieldCols.put(mFields.get(i), new Integer(i));
        
        mNumFields = mFields.size();
    }

    private String getField(String colName, List csv) {
        Integer col = (Integer) mFieldCols.get(colName);
        if (col == null || col.intValue() >= csv.size()) return null;
        else return (String) csv.get(col.intValue());
    }

    private void addField(String colName, List csv, String field, Map contact) {
        String value = getField(colName, csv);
        if (value != null && value.length() > 0) contact.put(field, value);
    }

    private void addStreetField(String cols[], List csv, String field, Map contact) {
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

    private void addEmailField(String cols[], List csv, String field, Map contact) {
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

    private Map toContact(List csv) {
        Map contact = new HashMap();
        
        for (Iterator it = sMappings.iterator(); it.hasNext(); ) {
            Mapping mp = (Mapping) it.next();
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
    private List getContactsInternal(BufferedReader reader) throws ParseException {
        try {
            initFields(reader);

            List result = new ArrayList();
            List fields = new ArrayList();
            
            while(parseLine(reader, fields)) {
                Map contact = toContact(fields);
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
    public static List getContacts(BufferedReader reader) throws ParseException {
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
        for (int i=0; i < cols.length; i++) {
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
        for (Iterator it = sMappings.iterator(); it.hasNext(); ) {
            Mapping mp = (Mapping) it.next();
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

        for (Iterator it = sMappings.iterator(); it.hasNext(); ) {
            Mapping mp = (Mapping) it.next();
            if (mp.hasMultiple()) {
                String names[] = mp.getCsvNames();
                for (int i=0; i < names.length; i++)
                    addFieldDef(names[i], sb);
            } else {
                addFieldDef(mp.getCsvName(), sb);
            }
        }

        sb.append("\n");
        
        for (Iterator it = contacts.iterator(); it.hasNext(); ) {
            Object c = it.next();
            if (c instanceof Contact) {
                Contact contact = (Contact) c;
                toCSVContact(contact.getAttrs(), sb);
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
        
        List list = ContactCSV.getContacts(reader);

        for (int n=0; n < list.size(); n++) {
            Map contact = (Map)list.get(n);
            TreeMap tm = new TreeMap(contact);
            System.out.println("contact: "+n);
            for (Iterator it = tm.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Entry) it.next();
                System.out.println("  "+entry.getKey()+": "+entry.getValue());
            }
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
