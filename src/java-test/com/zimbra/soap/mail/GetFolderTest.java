/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.mail.message.GetFolderResponse;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.Grant;
import com.zimbra.soap.mail.type.Grant.GranteeType;
import com.zimbra.soap.mail.type.ItemType;
import com.zimbra.soap.mail.type.SearchFolder;

/**
 * Unit test for {@link GetFolderRequest}.
 */
public final class GetFolderTest {

    private static Unmarshaller unmarshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetFolderResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
    }

    /**
     * Motivated by Bug 55153 failure in ZGrant.java line 134:
     *      mGranteeType = GranteeType.fromString(grant.getGranteeType().toString());
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

    @Test
    public void unmarshallSearchFolderEmptyTypes() throws Exception {
        GetFolderResponse resp = (GetFolderResponse) unmarshaller.unmarshal(
                getClass().getResourceAsStream("GetFolderResponse-SearchFolderEmptyTypes.xml"));
        for (Folder folder : resp.getFolder().getSubfolders()) {
            if ("searchfolder-with-types".equals(folder.getName())) {
                Assert.assertEquals(EnumSet.of(ItemType.CONVERSATION, ItemType.DOCUMENT),
                        ((SearchFolder) folder).getTypes());
            } else if ("searchfolder-with-empty-types".equals(folder.getName())) {
                Assert.assertEquals(EnumSet.noneOf(ItemType.class), ((SearchFolder) folder).getTypes());
            }
        }

    }
}
