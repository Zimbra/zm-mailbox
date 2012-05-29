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
        Assert.assertEquals("Identity{a=[" +
                "Attr{name=zimbraPrefIdentityId, value=91e6d036-5d5e-4788-9bc2-5b65e8c2480c}, " +
                "Attr{name=zimbraPrefSaveToSent, value=TRUE}, " +
                "Attr{name=zimbraPrefForwardReplyPrefixChar, value=>}, " +
                "Attr{name=zimbraPrefSentMailFolder, value=sent}, " +
                "Attr{name=zimbraPrefFromDisplay, value=Demo User One}, " +
                "Attr{name=zimbraPrefForwardIncludeOriginalText, value=includeBody}, " +
                "Attr{name=zimbraPrefForwardReplyFormat, value=same}, " +
                "Attr{name=zimbraPrefMailSignatureStyle, value=outlook}, " +
                "Attr{name=zimbraPrefIdentityName, value=DEFAULT}, " +
                "Attr{name=zimbraCreateTimestamp, value=20120528071949Z}, " +
                "Attr{name=zimbraPrefReplyIncludeOriginalText, value=includeBody}, " +
                "Attr{name=zimbraPrefFromAddress, value=user1@tarka.local}, " +
                "Attr{name=zimbraPrefDefaultSignatureId, value=28fa4fec-a5fb-4dc8-acf9-df930bb13546}], " +
                "name=DEFAULT, id=91e6d036-5d5e-4788-9bc2-5b65e8c2480c}",
                identities.get(0).toString());
        Collection<String> sigHtml = result.getPrefsMultimap().get("zimbraPrefMailSignatureHTML");
        Assert.assertNotNull(sigHtml);
        // Full comparison failing on Jenkins system due to environmental charset issues
        String sig = sigHtml.iterator().next();
        // Assert.assertEquals("\u003Cstrong\u003Ef—— utf8\u003C/strong\u003E signature test" , sig);
        Assert.assertTrue("Signature", sig.endsWith("signature test"));
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
