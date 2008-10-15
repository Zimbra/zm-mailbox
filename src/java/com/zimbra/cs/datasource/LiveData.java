/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.posisoft.jdavmail.JDAVContact;
import com.posisoft.jdavmail.JDAVContactGroup;
import com.posisoft.jdavmail.JDAVContact.Fields;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import static com.zimbra.cs.mailbox.Contact.*;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mime.ParsedContact;

public class LiveData {
    public LiveData() {
    }
    
    public static JDAVContact getJDAVContact(Contact contact) throws ServiceException {
        return new JDAVContact(getJDAVFields(contact));
    }
    
    public static void updateJDAVContact(JDAVContact jcontact, Contact contact) throws ServiceException {
        String nickName = jcontact.getField(Fields.nickname);
        
        jcontact.setFields(getJDAVFields(contact));
        jcontact.setField(Fields.nickname, nickName);
    }
    
    private static Map<String, String> getJDAVFields(Contact contact) throws ServiceException {
        Map<String, String> fields = new HashMap<String, String>();
        String fullName = null;

        for (Map.Entry<String, String> entry : contact.getFields().entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            
            try {
                switch (Attr.fromString(key)) {
                case birthday: fields.put(Fields.bday.toString(), val); break;
                case company: fields.put(Fields.o.toString(), val); break;
                case companyPhone: fields.put(Fields.telephoneNumber.toString(), val); break;
                case email: fields.put(Fields.mail.toString(), val); break;
                case email2: fields.put(Fields.othermail.toString(), val); break;
                case firstName: fields.put(Fields.givenName.toString(), val); break;
                case fullName: fullName = val; break;
                case homeCity: fields.put(Fields.homeCity.toString(), val); break;
                case homeCountry: fields.put(Fields.homeCountry.toString(), val); break;
                case homeFax: fields.put(Fields.homeFax.toString(), val); break;
                case homePhone: fields.put(Fields.homePhone.toString(), val); break;
                case homePostalCode: fields.put(Fields.homePostalCode.toString(), val); break;
                case homeState: fields.put(Fields.homeState.toString(), val); break;
                case homeStreet: fields.put(Fields.homeStreet.toString(), val); break;
                case lastName: fields.put(Fields.sn.toString(), val); break;
                case mobilePhone: fields.put(Fields.mobile.toString(), val); break;
                case nickname: fields.put(Fields.nickname.toString(), val); break;
                case notes: fields.put(Fields.notes.toString(), val); break;
                case otherURL: fields.put(Fields.wp.toString(), val); break;
                case pager: fields.put(Fields.pager.toString(), val); break;
                case workCity: fields.put(Fields.l.toString(), val); break;
                case workCountry: fields.put(Fields.co.toString(), val); break;
                case workFax: fields.put(Fields.facsimiletelephoneNumber.toString(), val); break;
                case workPhone: fields.put(Fields.telephoneNumber.toString(), val); break;
                case workPostalCode: fields.put(Fields.postalcode.toString(), val); break;
                case workState: fields.put(Fields.st.toString(), val); break;
                case workStreet: fields.put(Fields.street.toString(), val); break;
                case imAddress1:
                case imAddress2:
                case imAddress3:
                    if (val.startsWith("msn://"))
                        fields.put(Fields.msgrAddress.toString(), val.substring(6));
                    break;
                case workEmail1: fields.put(Fields.busmail.toString(), val); break;
                case workAltPhone: fields.put(Fields.otherTelephone.toString(), val); break;
                case dlist: fields.put(Fields.mail.toString(), val); break;
                }
            } catch (Exception e) {
            }
        }
        if (fullName != null && fields.get(Fields.givenName.toString()) == null &&
            fields.get(Fields.sn.toString()) == null) {
            int idx = fullName.lastIndexOf(' ');
            
            if (idx != -1) {
                fields.put(Fields.givenName.toString(), fullName.substring(0, idx - 1));
                fields.put(Fields.sn.toString(), fullName.substring(idx));
            }
        }
        if (fields.get(Fields.nickname) == null) {
            String s;
            
            if ((s = fields.get(Fields.mail)) != null ||
                (s = fields.get(Fields.busmail)) != null ||
                (s = fields.get(Fields.othermail)) != null)
                fields.put(Fields.nickname.toString(), s);
            else
                fields.put(Fields.nickname.toString(), "nickname");
        }
        return fields;
    }
    
    public static ParsedContact getParsedContact(JDAVContact jcontact,
        Contact contact) throws ServiceException {
        boolean group = false;
        String mail = "";
        Map jFields = jcontact.getFields();
        Map<String, String> fields = new HashMap<String, String>();

        if (contact != null)
            fields.putAll(contact.getFields());
        for (Iterator itr = jFields.entrySet().iterator(); itr.hasNext(); ) {
            Map.Entry entry = (Map.Entry)itr.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            
            try {
                switch (Fields.valueOf(key)) {
                case bday: fields.put(Contact.A_birthday, val); break;
                case o: fields.put(Contact.A_company, val); break;
                case mail: mail = val; break;
                case othermail: fields.put(Contact.A_email2, val); break;
                case givenName: fields.put(Contact.A_firstName, val); break;
                case homeCity: fields.put(Contact.A_homeCity, val); break;
                case homeCountry: fields.put(Contact.A_homeCountry, val); break;
                case homeFax: fields.put(Contact.A_homeFax, val); break;
                case homePhone: fields.put(Contact.A_homePhone, val); break;
                case homePostalCode: fields.put(Contact.A_homePostalCode, val); break;
                case homeState: fields.put(Contact.A_homeState, val); break;
                case homeStreet: fields.put(Contact.A_homeStreet, val); break;
                case sn: fields.put(Contact.A_lastName, val); break;
                case mobile: fields.put(Contact.A_mobilePhone, val); break;
                case nickname: fields.put(Contact.A_nickname, val); break;
                case notes: fields.put(Contact.A_notes, val); break;
                case wp: fields.put(Contact.A_otherURL, val); break;
                case pager: fields.put(Contact.A_pager, val); break;
                case l: fields.put(Contact.A_workCity, val); break;
                case co: fields.put(Contact.A_workCountry, val); break;
                case facsimiletelephoneNumber: fields.put(Contact.A_workFax, val); break;
                case telephoneNumber: fields.put(Contact.A_workPhone, val); break;
                case postalcode: fields.put(Contact.A_workPostalCode, val); break;
                case st: fields.put(Contact.A_workState, val); break;
                case street: fields.put(Contact.A_workStreet, val); break;
                case msgrAddress: fields.put(Contact.A_imAddress1, "msn://" + val); break;
                case busmail: fields.put(Contact.A_workEmail1, val); break;
                case otherTelephone: fields.put(Contact.A_workAltPhone, val); break;
                case group: group = true; break;
                }
            } catch (Exception e) {
            }
        }
        if (group) {
            fields.put(Contact.A_fileAs, Contact.FA_EXPLICIT + ":" +
                fields.get(Contact.A_nickname));
            fields.put(Contact.A_dlist, mail);
            fields.put(Contact.A_type, Contact.TYPE_GROUP);
        } else {
            fields.put(Contact.A_email, mail);
        }
        return contact == null ? new ParsedContact(fields) :
            new ParsedContact(fields, contact.getContent());
    }
}
