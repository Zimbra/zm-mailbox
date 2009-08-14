/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.posisoft.jdavmail.JDAVContact;
import com.posisoft.jdavmail.JDAVContactGroup;
import com.posisoft.jdavmail.JDAVContact.Fields;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.mailbox.ContactConstants.Attr;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mime.ParsedContact;

public class LiveData extends DataSourceMapping {
    private long localDate, remoteDate;
    private int flags;
    private String remoteFolderId;
    private static final String METADATA_KEY_DATE_LOCAL = "dl";
    private static final String METADATA_KEY_DATE_REMOTE = "dr";
    private static final String METADATA_KEY_FLAGS = "f";
    private static final String METADATA_KEY_FLAGS_OLD = "fgr";
    private static final String METADATA_KEY_FOLDER_REMOTE = "fr";
    
    public LiveData(DataSource ds, DataSourceItem dsi) throws ServiceException {
        super(ds, dsi);
    }
    
    public LiveData(DataSource ds, int itemID) throws ServiceException {
        super(ds, itemID);
    }
    
    public LiveData(DataSource ds, String remoteID) throws ServiceException {
        super(ds, remoteID);
    }
    
    public LiveData(DataSource ds, int folderId, int localId, String remoteId)
        throws  ServiceException {
        super(ds, folderId, localId, remoteId);
    }
    
    public LiveData(DataSource ds, int folderId, int localId, long localDate,
        String remoteId, String remoteFolderId, long remoteDate, int flags)
        throws ServiceException {
        super(ds, folderId, localId, remoteId);
        setDates(localDate, remoteDate);
        setRemoteFolderId(remoteFolderId);
        setFlags(flags);
    }
    
    long getLocalDate() { return localDate; }
    
    long getRemoteDate() { return remoteDate; }

    int getFlags() { return flags; }
    
    String getRemoteFolderId() { return remoteFolderId; }

    public void setDates(long localDate, long remoteDate) throws ServiceException {
        this.localDate = localDate;
        dsi.md.put(METADATA_KEY_DATE_LOCAL, Long.toString(localDate));
        this.remoteDate = remoteDate;
        dsi.md.put(METADATA_KEY_DATE_REMOTE, Long.toString(remoteDate));
    }
    
    public void setRemoteFolderId(String remoteFolderId) throws ServiceException {
        this.remoteFolderId = remoteFolderId;
        dsi.md.put(METADATA_KEY_FOLDER_REMOTE, remoteFolderId);
    }

    public void setFlags(int flags) throws ServiceException {
        this.flags = flags;
        dsi.md.put(METADATA_KEY_FLAGS, Integer.toString(flags));
    }

    protected void parseMetaData() throws ServiceException {
        localDate = dsi.md.getLong(METADATA_KEY_DATE_LOCAL, 0);
        remoteDate = dsi.md.getLong(METADATA_KEY_DATE_REMOTE, 0);
        flags = (int)dsi.md.getLong(METADATA_KEY_FLAGS, -1);
        if (flags == -1)
            flags = (int)dsi.md.getLong(METADATA_KEY_FLAGS_OLD, 0);
        remoteFolderId = dsi.md.get(METADATA_KEY_FOLDER_REMOTE, "");
    }
    
    public static JDAVContact getJDAVContact(Contact contact) throws ServiceException {
        return new JDAVContact(getJDAVFields(contact));
    }
    
    public static JDAVContactGroup getJDAVContactGroup(Contact contact) throws ServiceException {
        return new JDAVContactGroup("TODO");
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
        String im = null;
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
                case bday: fields.put(ContactConstants.A_birthday, val); break;
                case o: fields.put(ContactConstants.A_company, val); break;
                case mail: mail = val; break;
                case othermail: fields.put(ContactConstants.A_email2, val); break;
                case givenName: fields.put(ContactConstants.A_firstName, val); break;
                case homeCity: fields.put(ContactConstants.A_homeCity, val); break;
                case homeCountry: fields.put(ContactConstants.A_homeCountry, val); break;
                case homeFax: fields.put(ContactConstants.A_homeFax, val); break;
                case homePhone: fields.put(ContactConstants.A_homePhone, val); break;
                case homePostalCode: fields.put(ContactConstants.A_homePostalCode, val); break;
                case homeState: fields.put(ContactConstants.A_homeState, val); break;
                case homeStreet: fields.put(ContactConstants.A_homeStreet, val); break;
                case sn: fields.put(ContactConstants.A_lastName, val); break;
                case mobile: fields.put(ContactConstants.A_mobilePhone, val); break;
                case nickname: fields.put(ContactConstants.A_nickname, val); break;
                case notes: fields.put(ContactConstants.A_notes, val); break;
                case wp: fields.put(ContactConstants.A_otherURL, val); break;
                case pager: fields.put(ContactConstants.A_pager, val); break;
                case l: fields.put(ContactConstants.A_workCity, val); break;
                case co: fields.put(ContactConstants.A_workCountry, val); break;
                case facsimiletelephoneNumber: fields.put(ContactConstants.A_workFax, val); break;
                case telephoneNumber: fields.put(ContactConstants.A_workPhone, val); break;
                case postalcode: fields.put(ContactConstants.A_workPostalCode, val); break;
                case st: fields.put(ContactConstants.A_workState, val); break;
                case street: fields.put(ContactConstants.A_workStreet, val); break;
                case msgrAddress: im = val; fields.put(ContactConstants.A_imAddress1, "msn://" + val); break;
                case busmail: fields.put(ContactConstants.A_workEmail1, val); break;
                case otherTelephone: fields.put(ContactConstants.A_workAltPhone, val); break;
                case group: group = true; break;
                }
            } catch (Exception e) {
            }
        }
        if (group) {
            fields.put(ContactConstants.A_fileAs, ContactConstants.FA_EXPLICIT + ":" +
                fields.get(ContactConstants.A_nickname));
            fields.put(ContactConstants.A_dlist, mail);
            fields.put(ContactConstants.A_type, ContactConstants.TYPE_GROUP);
        } else {
            if (!fields.containsKey(ContactConstants.A_firstName) &&
                !fields.containsKey(ContactConstants.A_lastName)) {
                String fileAs;

                if ((fileAs = fields.get(ContactConstants.A_fullName)) != null ||
                    (fileAs = fields.get(ContactConstants.A_nickname)) != null ||
                    (fileAs = fields.get(ContactConstants.A_email)) != null ||
                    (fileAs = fields.get(ContactConstants.A_email2)) != null ||
                    (fileAs = fields.get(ContactConstants.A_workEmail1)) != null ||
                    (fileAs = im) != null)
                    fields.put(ContactConstants.A_fileAs, ContactConstants.FA_EXPLICIT + ":" + fileAs);
            }
            fields.put(ContactConstants.A_email, mail);
        }
        return contact == null ? new ParsedContact(fields) :
            new ParsedContact(fields, contact.getContentStream());
    }
}
