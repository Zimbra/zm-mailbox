/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.common.mailbox;

import java.util.Arrays;

import com.zimbra.common.service.ServiceException;

public final class ContactConstants {
    private ContactConstants() {
    }

    public static final int MAX_FIELD_NAME_LENGTH = 100;
    public static final int MAX_FIELD_VALUE_LENGTH = 10000000; // 10M
    public static final int MAX_FIELD_COUNT = 1000;

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
    public static final String A_assistantPhone = Attr.assistantPhone.name();
    public static final String A_birthday = Attr.birthday.name();
    public static final String A_anniversary = Attr.anniversary.name();
    public static final String A_callbackPhone = Attr.callbackPhone.name();
    public static final String A_canExpand = Attr.canExpand.name();
    public static final String A_carPhone = Attr.carPhone.name();
    public static final String A_company = Attr.company.name();
    public static final String A_description = Attr.description.name();
    public static final String A_dn = Attr.dn.name();
    public static final String A_phoneticCompany = Attr.phoneticCompany.name();
    public static final String A_companyPhone = Attr.companyPhone.name();
    public static final String A_department = Attr.department.name();
    public static final String A_dlist = Attr.dlist.name();
    public static final String A_email = Attr.email.name();
    public static final String A_email2 = Attr.email2.name();
    public static final String A_email3 = Attr.email3.name();
    public static final String A_fileAs = Attr.fileAs.name();
    public static final String A_firstName = Attr.firstName.name();
    public static final String A_phoneticFirstName = Attr.phoneticFirstName.name();
    public static final String A_fullName = Attr.fullName.name();
    public static final String A_groupMember = Attr.groupMember.name();
    public static final String A_homeCity = Attr.homeCity.name();
    public static final String A_homeCountry = Attr.homeCountry.name();
    public static final String A_homeFax = Attr.homeFax.name();
    public static final String A_homePhone = Attr.homePhone.name();
    public static final String A_homePhone2 = Attr.homePhone2.name();
    public static final String A_homePostalCode = Attr.homePostalCode.name();
    public static final String A_homeState = Attr.homeState.name();
    public static final String A_homeStreet = Attr.homeStreet.name();
    public static final String A_homeURL = Attr.homeURL.name();
    public static final String A_image = Attr.image.name();
    public static final String A_initials = Attr.initials.name();
    public static final String A_isMyCard = "isMyCard";
    public static final String A_jobTitle = Attr.jobTitle.name();
    public static final String A_lastName = Attr.lastName.name();
    public static final String A_phoneticLastName = Attr.phoneticLastName.name();
    public static final String A_maidenName = Attr.maidenName.name();
    public static final String A_member = Attr.member.name();
    public static final String A_middleName = Attr.middleName.name();
    public static final String A_mobilePhone = Attr.mobilePhone.name();
    public static final String A_namePrefix = Attr.namePrefix.name();
    public static final String A_nameSuffix = Attr.nameSuffix.name();
    public static final String A_nickname = Attr.nickname.name();
    public static final String A_notes = Attr.notes.name();
    public static final String A_office = Attr.office.name();
    public static final String A_otherCity = Attr.otherCity.name();
    public static final String A_otherCountry = Attr.otherCountry.name();
    public static final String A_otherFax = Attr.otherFax.name();
    public static final String A_otherPhone = Attr.otherPhone.name();
    public static final String A_otherPostalCode = Attr.otherPostalCode.name();
    public static final String A_otherState = Attr.otherState.name();
    public static final String A_otherStreet = Attr.otherStreet.name();
    public static final String A_otherURL = Attr.otherURL.name();
    public static final String A_pager = Attr.pager.name();
    public static final String A_tollFree = Attr.tollFree.name();
    public static final String A_userCertificate = Attr.userCertificate.name();
    public static final String A_userSMIMECertificate = Attr.userSMIMECertificate.name();
    public static final String A_workCity = Attr.workCity.name();
    public static final String A_workCountry = Attr.workCountry.name();
    public static final String A_workFax = Attr.workFax.name();
    public static final String A_workPhone = Attr.workPhone.name();
    public static final String A_workPhone2 = Attr.workPhone2.name();
    public static final String A_workPostalCode = Attr.workPostalCode.name();
    public static final String A_workState = Attr.workState.name();
    public static final String A_workStreet = Attr.workStreet.name();
    public static final String A_workURL = Attr.workURL.name();
    public static final String A_type = Attr.type.name();
    public static final String A_imAddress1 = Attr.imAddress1.name();
    public static final String A_imAddress2 = Attr.imAddress2.name();
    public static final String A_imAddress3 = Attr.imAddress3.name();
    public static final String A_zimbraId = Attr.zimbraId.name();

    // Comcast specific fields
    public static final String A_homeAddress = Attr.homeAddress.name();
    public static final String A_workAddress = Attr.workAddress.name();
    public static final String A_workEmail1 = Attr.workEmail1.name();
    public static final String A_workEmail2 = Attr.workEmail2.name();
    public static final String A_workEmail3 = Attr.workEmail3.name();
    public static final String A_workMobile = Attr.workMobile.name();
    public static final String A_workIM1 = Attr.workIM1.name();
    public static final String A_workIM2 = Attr.workIM2.name();
    public static final String A_workAltPhone = Attr.workAltPhone.name();
    public static final String A_otherDepartment = Attr.otherDepartment.name();
    public static final String A_otherOffice = Attr.otherOffice.name();
    public static final String A_otherProfession = Attr.otherProfession.name();
    public static final String A_otherAddress = Attr.otherAddress.name();
    public static final String A_otherMgrName = Attr.otherMgrName.name();
    public static final String A_otherAsstName = Attr.otherAsstName.name();
    public static final String A_otherAnniversary = Attr.otherAnniversary.name();
    public static final String A_otherCustom1 = Attr.otherCustom1.name();
    public static final String A_otherCustom2 = Attr.otherCustom2.name();
    public static final String A_otherCustom3 = Attr.otherCustom3.name();
    public static final String A_otherCustom4 = Attr.otherCustom4.name();
    // end

    public static final String A_custom1 = "custom1";
    public static final String A_custom2 = "custom2";
    public static final String A_custom3 = "custom3";
    public static final String A_custom4 = "custom4";

    // vCard fields that need to be preserved
    public static final String A_vCardUID    = "vcardUID";
    public static final String A_vCardURL    = "vcardURL";
    public static final String A_vCardXProps = "vcardXProps";


    public static final String TYPE_GROUP = "group";

    // move to ZimbraSoap?
    public static final String GROUP_MEMBER_TYPE_CONTACT_REF = "C";
    public static final String GROUP_MEMBER_TYPE_GAL_REF = "G";
    public static final String GROUP_MEMBER_TYPE_INLINE = "I";

    //outlook contact attributes besides the standard zimbra ones


    /**
     * "well known attrs". keep in sync with ContactConstants.A_* above.
     */
    public enum Attr {
        assistantPhone,
        birthday,
        anniversary,
        callbackPhone,
        canExpand,
        carPhone,
        company,
        dn,
        phoneticCompany,
        companyPhone,
        description,
        department,
        dlist,  //TODO deprecate? or use use?  don't touch/keep for backward compatibility?
        email,
        email2,
        email3,
        fileAs,
        firstName,
        phoneticFirstName,
        fullName,
        groupMember,
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
        phoneticLastName,
        maidenName,
        member,
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
        userCertificate,
        userSMIMECertificate,
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
        vCardXProps,
        zimbraId,
        outlookProps;


        public static Attr fromString(String s) throws ServiceException {
            try {
                return Attr.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("invalid attr: " + s +
                        ", valid values: " + Arrays.asList(Attr.values()), e);
            }
        }
    }
}
