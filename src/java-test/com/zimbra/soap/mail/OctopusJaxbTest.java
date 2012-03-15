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
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.CheckDeviceStatusResponse;
import com.zimbra.soap.mail.type.ActivityInfo;
import com.zimbra.soap.mail.type.IdStatus;
import com.zimbra.soap.type.NamedValue;

/**
 * Unit test for {@link GetFolderRequest}.
 */
public class OctopusJaxbTest {

    @BeforeClass
    public static void init() throws Exception {
    }

    /**
     * Validating that method used to create response in CheckDeviceStatus
     * should work.
     * @throws Exception
     */
    @Test
    public void checkDeviceStatusResponse() throws Exception {
        IdStatus idStatus = IdStatus.fromIdAndStatus("idString", "statusString");
        CheckDeviceStatusResponse resp = new CheckDeviceStatusResponse(idStatus);
        Element response = JaxbUtil.jaxbToElement(resp, XMLElement.mFactory);
        Assert.assertNotNull("response object", response);
        resp = JaxbUtil.elementToJaxb(response);
        Assert.assertEquals("round tripped id", "idString",
                resp.getDevice().getId());
        Assert.assertEquals("round tripped status", "statusString",
                resp.getDevice().getStatus());
    }

    private static ActivityInfo toActivityInfo(String account, String op, 
            long timestamp, String itemId, int version,
            String itemName, Map<String,String> args) {
        ActivityInfo activity =
            ActivityInfo.fromOperationTimeStampItemId(op, timestamp, itemId);
        if (version > 0)
            activity.setVersion(version);
        if (account != null)
            activity.setEmail(account);
        activity.setArgs(args);
        return activity;
    }
    
    @Test
    public void activityInfoForGetActivityStream() throws Exception {
        Map<String,String> args = Maps.newHashMap();
        args.put("key1", "value1");
        args.put("key2", "value2");
        ActivityInfo ai = toActivityInfo("account", "op", 333L, "123-123-123:22", 44,
                "itemName", args);
        Assert.assertEquals("email", "account", ai.getEmail());
        Assert.assertEquals("operation", "op", ai.getOperation());
        Assert.assertEquals("operation", 333L, ai.getTimeStamp());
        Assert.assertEquals("item id", "123-123-123:22", ai.getItemId());
        Assert.assertEquals("version", Integer.valueOf(44), ai.getVersion());
        List<NamedValue> nvArgs = ai.getArgs();
        Assert.assertEquals("Number of args", 2, nvArgs.size());
        Assert.assertEquals("arg1 name prefix", "key",
                nvArgs.get(0).getName().substring(0, 3));
        Assert.assertEquals("arg1 value prefix", "value",
                nvArgs.get(0).getValue().substring(0, 5));
        Assert.assertEquals("arg2 name prefix", "key",
                nvArgs.get(1).getName().substring(0, 3));
        Assert.assertEquals("arg2 value prefix", "value",
                nvArgs.get(1).getValue().substring(0, 5));
        Element notify = XMLElement.create(SoapProtocol.SoapJS, "pink");
        Element activityElem = JaxbUtil.addChildElementFromJaxb(notify, MailConstants.E_A,
                MailConstants.NAMESPACE_STR, ai);
        ai = JaxbUtil.elementToJaxb(activityElem, ActivityInfo.class);
        Assert.assertEquals("round tripped account id", "account", ai.getEmail());
        Assert.assertEquals("round tripped operation", "op", ai.getOperation());
        Assert.assertEquals("round tripped operation", 333L, ai.getTimeStamp());
        Assert.assertEquals("round tripped item id", "123-123-123:22", ai.getItemId());
        Assert.assertEquals("round tripped version", Integer.valueOf(44), ai.getVersion());
        nvArgs = ai.getArgs();
        Assert.assertEquals("round tripped Number of args", 2, nvArgs.size());
        Assert.assertEquals("round tripped arg1 name prefix", "key",
                nvArgs.get(0).getName().substring(0, 3));
        Assert.assertEquals("round tripped arg1 value prefix", "value",
                nvArgs.get(0).getValue().substring(0, 5));
        Assert.assertEquals("round tripped arg2 name prefix", "key",
                nvArgs.get(1).getName().substring(0, 3));
        Assert.assertEquals("round tripped arg2 value prefix", "value",
                nvArgs.get(1).getValue().substring(0, 5));
    }

}
