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
package com.zimbra.soap.account;

import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.account.type.Identity;

/**
 * Unit test for {@link GetInfoResponse}.
 *
 * @author ysasaki
 */
public class GetInfoResponseTest {

    private static Unmarshaller unmarshaller;

    @BeforeClass
    public static void init() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(GetInfoResponse.class);
        unmarshaller = jaxb.createUnmarshaller();
    }

    private void checkAsserts(GetInfoResponse result) {
        List<Identity> identities =  result.getIdentities();
        Assert.assertEquals(1, identities.size());
        Assert.assertEquals("Identity{name=DEFAULT, id=add0c6cd-7d8a-467b-ae6e-67b9644d9c2a, attrs=[" +
                "Attr{name=zimbraPrefIdentityId, value=add0c6cd-7d8a-467b-ae6e-67b9644d9c2a}, " +
                "Attr{name=zimbraPrefForwardReplyPrefixChar, value=>}, " +
                "Attr{name=zimbraPrefSaveToSent, value=TRUE}, " +
                "Attr{name=zimbraPrefSentMailFolder, value=sent}, " +
                "Attr{name=zimbraPrefFromDisplay, value=Demo User One}, " +
                "Attr{name=zimbraPrefForwardIncludeOriginalText, value=includeBody}, " +
                "Attr{name=zimbraPrefForwardReplyFormat, value=text}, " +
                "Attr{name=zimbraPrefMailSignatureStyle, value=outlook}, " +
                "Attr{name=zimbraPrefIdentityName, value=DEFAULT}, " +
                "Attr{name=zimbraPrefReplyIncludeOriginalText, value=includeBody}, " +
                "Attr{name=zimbraCreateTimestamp, value=20101007221807Z}, " +
                "Attr{name=zimbraPrefFromAddress, value=user1@ysasaki.local}]}",
                identities.get(0).toString());
        Collection<String> sigHtml = result.getPrefsMultimap().get("zimbraPrefMailSignatureHTML");
        Assert.assertNotNull(sigHtml);
        Assert.assertEquals("<font face=\"verdana, helvetica, sans-serif\" size=\"2\">f\u00f3\u00f3 utf8 test</font>",sigHtml.iterator().next());
    }
    
    @Test
    public void unmarshall() throws Exception {
        checkAsserts((GetInfoResponse) unmarshaller.unmarshal(
                        getClass().getResourceAsStream("GetInfoResponse.xml")));
    }
    
    @Test
    public void jaxbUtilUnmarshall() throws Exception {
        //same as unmarshall but use JaxbUtil; this provokes/tests issues with utf8 conversion
        checkAsserts((GetInfoResponse) JaxbUtil.elementToJaxb(
                        Element.parseXML(getClass().getResourceAsStream("GetInfoResponse.xml"))));
    }

}
