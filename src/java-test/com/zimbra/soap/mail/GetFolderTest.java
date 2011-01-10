/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.soap.mail;

import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.mail.message.GetFolderResponse;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.Grant;
import com.zimbra.soap.mail.type.Grant.GranteeType;

/**
 * Unit test for {@link GetFolderRequest}.
 */
public class GetFolderTest {

    private static Unmarshaller unmarshaller;
    private static Marshaller marshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetFolderResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
        marshaller = jaxb.createMarshaller();
    }

    /**
     * Motivated by Bug 55153 failure in ZGrant.java line 134:
     *      mGranteeType = GranteeType.fromString(grant.getGranteeType().toString());
     * @throws Exception
     */
    @Test
    public void unmarshallGetFolderResponseContainingGrant() throws Exception {
        GetFolderResponse result = (GetFolderResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetFolderResponseWithGrant.xml"));
        Folder top = result.getFolder();
        boolean foundGrant = false;
        for (Folder child : top.getSubfolders()) {
            List <Grant> myGrants = child.getGrants();
            if (myGrants.size() > 0) {
                foundGrant = true;
                Grant first = myGrants.get(0);
                GranteeType mGranteeType = GranteeType.fromString(
                        first.getGranteeType().toString());
                Assert.assertEquals(GranteeType.USER, mGranteeType);
            }
        }
        Assert.assertTrue("Should have processed a valid <grant>", foundGrant);
        result = (GetFolderResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetFolderResponseWithBadGrant.xml"));
        top = result.getFolder();
        foundGrant = false;
        for (Folder child : top.getSubfolders()) {
            List <Grant> myGrants = child.getGrants();
            if (myGrants.size() > 0) {
                foundGrant = true;
                Grant first = myGrants.get(0);
                GranteeType mGranteeType = first.getGranteeType();
                Assert.assertNull("There was no 'gt' attribute", mGranteeType);
            }
        }
        Assert.assertTrue("Should have processed a bad <grant>", foundGrant);
    }
}
