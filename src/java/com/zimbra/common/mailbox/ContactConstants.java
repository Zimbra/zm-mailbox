/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
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

package com.zimbra.common.mailbox;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;

public class ContactConstants {

    /** "File as" setting: &nbsp;<tt>Last, First</tt> */
    public static final int FA_LAST_C_FIRST = 1;
    /** "File as" setting: &nbsp;<tt>First Last</tt> */
    public static final int FA_FIRST_LAST = 2;
    /** "File as" setting: &nbsp;<tt>Company</tt> */
    public static final int FA_COMPANY = 3;
    /** "File as" setting: &nbsp;<tt>Last, First (Company)</tt> */
    public static final int FA_LAST_C_FIRST_COMPANY = 4;
    /** "File as" setting: &nbsp;<tt>First Last (Company)</tt> */
    public static final int FA_FIRST_LAST_COMPANY = 5;
    /** "File as" setting: &nbsp;<tt>Company (Last, First)</tt> */
    public static final int FA_COMPANY_LAST_C_FIRST = 6;
    /** "File as" setting: &nbsp;<tt>Company (First Last)</tt> */
    public static final int FA_COMPANY_FIRST_LAST = 7;
    /** "File as" setting: <i>[explicitly specified "file as" string]</i> */
    public static final int FA_EXPLICIT = 8;

    /** The default "file as" setting: {@link #ContactConstants.A_LAST_C_FIRST}. */
    public static final int FA_DEFAULT = FA_LAST_C_FIRST;
    public static final int FA_MAXIMUM = FA_EXPLICIT;

    // these are the "well known attrs". keep in sync with Attr enum below
    public static final String A_assistantPhone = "assistantPhone";
    public static final String A_birthday = "birthday";
    public static final String A_callbackPhone = "callbackPhone";
    public static final String A_carPhone = "carPhone";
    public static final String A_company = "company";
    public static final String A_companyPhone = "companyPhone";
    public static final String A_department = "department";
    public static final String A_dlist = "dlist";
    public static final String A_email = "email";
    public static final String A_email2 = "email2";
    public static final String A_email3 = "email3";
    public static final String A_fileAs = "fileAs";
    public static final String A_firstName = "firstName";
    public static final String A_fullName = "fullName";
    public static final String A_homeCity = "homeCity";
    public static final String A_homeCountry = "homeCountry";
    public static final String A_homeFax = "homeFax";
    public static final String A_homePhone = "homePhone";
    public static final String A_homePhone2 = "homePhone2";
    public static final String A_homePostalCode = "homePostalCode";
    public static final String A_homeState = "homeState";
    public static final String A_homeStreet = "homeStreet";
    public static final String A_homeURL = "homeURL";
    public static final String A_image = "image";
    public static final String A_initials = "initials";
    public static final String A_isMyCard = "isMyCard";
    public static final String A_jobTitle = "jobTitle";
    public static final String A_lastName = "lastName";
    public static final String A_middleName = "middleName";
    public static final String A_mobilePhone = "mobilePhone";
    public static final String A_namePrefix = "namePrefix";
    public static final String A_nameSuffix = "nameSuffix";
    public static final String A_nickname = "nickname";
    public static final String A_notes = "notes";
    public static final String A_office = "office";
    public static final String A_otherCity = "otherCity";
    public static final String A_otherCountry = "otherCountry";
    public static final String A_otherFax = "otherFax";
    public static final String A_otherPhone = "otherPhone";
    public static final String A_otherPostalCode = "otherPostalCode";
    public static final String A_otherState = "otherState";
    public static final String A_otherStreet = "otherStreet";
    public static final String A_otherURL = "otherURL";
    public static final String A_pager = "pager";
    public static final String A_workCity = "workCity";
    public static final String A_workCountry = "workCountry";
    public static final String A_workFax = "workFax";
    public static final String A_workPhone = "workPhone";
    public static final String A_workPhone2 = "workPhone2";
    public static final String A_workPostalCode = "workPostalCode";
    public static final String A_workState = "workState";
    public static final String A_workStreet = "workStreet";
    public static final String A_workURL = "workURL";
    public static final String A_type = "type";
    public static final String A_imAddress1 = "imAddress1";
    public static final String A_imAddress2 = "imAddress2";
    public static final String A_imAddress3 = "imAddress3";
    // Comcast specific fields
    public static final String A_homeAddress = "homeAddress";
    public static final String A_workAddress = "workAddress";
    public static final String A_workEmail1 = "workEmail1";
    public static final String A_workEmail2 = "workEmail2";
    public static final String A_workEmail3 = "workEmail3";
    public static final String A_workMobile = "workMobile";
    public static final String A_workIM1 = "workIM1";
    public static final String A_workIM2 = "workIM2";
    public static final String A_workAltPhone = "workAltPhone";
    public static final String A_otherDepartment = "otherDepartment";
    public static final String A_otherOffice = "otherOffice";
    public static final String A_otherProfession = "otherProfession";
    public static final String A_otherAddress = "otherAddress";
    public static final String A_otherMgrName = "otherMgrName";
    public static final String A_otherAsstName = "otherAsstName";
    public static final String A_otherAnniversary = "otherAnniversary";
    public static final String A_otherCustom1 = "otherCustom1";
    public static final String A_otherCustom2 = "otherCustom2";
    public static final String A_otherCustom3 = "otherCustom3";
    public static final String A_otherCustom4 = "otherCustom4";
    // end
    
    // vCard fields that need to be preserved
    public static final String A_vCardUID    = "vcardUID";
    public static final String A_vCardXProps = "vcardXProps";

    public static final String TYPE_GROUP = "group";

 // these are the "well known attrs". keep in sync with ContactConstants.A_* above.
    public enum Attr {
        assistantPhone,
        birthday,
        callbackPhone,
        carPhone,
        company,
        companyPhone,
        description,
        department,
        dlist,
        email,
        email2,
        email3,
        fileAs,
        firstName,
        fullName,
        homeCity,
        homeCountry,
        homeFax,
        homePhone,
        homePhone2,
        homePostalCode,
        homeState,
        homeStreet,
        homeURL,
        image,
        initials,
        jobTitle,
        lastName,
        middleName,
        mobilePhone,
        namePrefix,
        nameSuffix,
        nickname,
        notes,
        office,
        otherCity,
        otherCountry,
        otherFax,
        otherPhone,
        otherPostalCode,
        otherState,
        otherStreet,
        otherURL,
        pager,
        tollFree,
        workCity,
        workCountry,
        workFax,
        workPhone,
        workPhone2,
        workPostalCode,
        workState,
        workStreet,
        workURL,
        type,
        homeAddress,
        imAddress1,
        imAddress2,
        imAddress3,
        workAddress,
        workEmail1,
        workEmail2,
        workEmail3,
        workMobile,
        workIM1,
        workIM2,
        workAltPhone,
        otherDepartment,
        otherOffice,
        otherProfession,
        otherAddress,
        otherMgrName,
        otherAsstName,
        otherAnniversary,
        otherCustom1,
        otherCustom2,
        otherCustom3,
        otherCustom4,
        vCardUID,
        vCardXProps;

        public static Attr fromString(String s) throws ServiceException {
            try {
                return Attr.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid attr: "+s+", valid values: "+Arrays.asList(Attr.values()), e);
            }
        }
    }
}
