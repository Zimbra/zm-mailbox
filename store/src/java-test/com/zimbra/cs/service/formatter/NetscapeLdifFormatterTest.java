/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class NetscapeLdifFormatterTest {

    @Test
    public void testContactLDIFFormat() throws Exception {
        try {
            Map<String, String> contact = new HashMap<String, String>();
            contact.put("email", "user2@zimbra.com");
            contact.put("firstName", "user2");
            contact.put("nickname", "nick ");// test value gets base64 encoded if it contains last SPACE character
            contact.put("birthday", "--12-04");
            contact.put("homeStreet", "Lane 1,\nAirport rd");// test value gets base64 encoded if it contains Non SAFE-CHAR(\n)
            contact.put("mobilePhone", "<9876543210>");// test value gets base64 encoded if it contains Non SAFE-INIT-CHAR(<)

            StringBuilder sb = new StringBuilder();
            String[] galLdapAttrMap = { "(binary) userSMIMECertificate=userSMIMECertificate",
                "(certificate) userCertificate=userCertificate", "co=workCountry",
                "company=company", "description=notes",
                "displayName,cn=fullName,fullName2,fullName3,fullName4,fullName5,fullName6,fullName7,fullName8,fullName9,fullName10",
                "facsimileTelephoneNumber,fax=workFax", "givenName,gn=firstName",
                "homeTelephoneNumber,homePhone=homePhone", "initials=initials", "l=workCity",
                "mobileTelephoneNumber,mobile=mobilePhone",
                "msExchResourceSearchProperties=zimbraAccountCalendarUserType",
                "objectClass=objectClass", "ou=department", "pagerTelephoneNumber,pager=pager",
                "physicalDeliveryOfficeName=office", "postalCode=workPostalCode", "sn=lastName",
                "st=workState", "street,streetAddress=workStreet", "telephoneNumber=workPhone",
                "title=jobTitle", "whenChanged,modifyTimeStamp=modifyTimeStamp",
                "whenCreated,createTimeStamp=createTimeStamp",
                "zimbraCalResBuilding=zimbraCalResBuilding",
                "zimbraCalResCapacity,msExchResourceCapacity=zimbraCalResCapacity",
                "zimbraCalResContactEmail=zimbraCalResContactEmail",
                "zimbraCalResFloor=zimbraCalResFloor",
                "zimbraCalResLocationDisplayName=zimbraCalResLocationDisplayName",
                "zimbraCalResSite=zimbraCalResSite",
                "zimbraCalResType,msExchResourceSearchProperties=zimbraCalResType",
                "zimbraDistributionListSubscriptionPolicy=zimbraDistributionListSubscriptionPolicy",
                "zimbraDistributionListUnsubscriptionPolicy=zimbraDistributionListUnsubscriptionPolicy",
                "zimbraId=zimbraId",
                "zimbraMailDeliveryAddress,zimbraMailAlias,mail=email,email2,email3,email4,email5,email6,email7,email8,email9,email10,email11,email12,email13,email14,email15,email16",
                "zimbraMailForwardingAddress=member",
                "zimbraPhoneticCompany,ms-DS-Phonetic-Company-Name=phoneticCompany",
                "zimbraPhoneticFirstName,ms-DS-Phonetic-First-Name=phoneticFirstName",
                "zimbraPhoneticLastName,ms-DS-Phonetic-Last-Name=phoneticLastName" };
            NetscapeLdifFormatter formatter = new NetscapeLdifFormatter();
            formatter.toLDIFContact(contact, sb, galLdapAttrMap);
            String expectedResult = "dn: cn=user2,email=user2@zimbra.com\r\n"
                + "objectClass: top\r\n" + "objectClass: person\r\n"
                + "objectClass: organizationalPerson\r\n" + "objectClass: inetOrgPerson\r\n"
                + "cn: user2\r\n" + "givenName: user2\r\n" + "mobile:: PDk4NzY1NDMyMTA+\r\n"
                + "mail: user2@zimbra.com\r\n" + "birthmonth: 12\r\n" + "birthday: 4\r\n"
                + "mozillaNickname:: bmljayA=\r\n"
                + "mozillaHomeStreet:: TGFuZSAxLApBaXJwb3J0IHJk\r\n";
            Assert.assertEquals(expectedResult, sb.toString());
        } catch (Exception e) {
            Assert.fail("Exception should not be thrown");
        }
    }
}