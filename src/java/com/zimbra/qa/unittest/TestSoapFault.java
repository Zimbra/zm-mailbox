/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;
import com.zimbra.common.httpclient.HttpClientUtil;

public class TestSoapFault extends TestCase {
    @Test
    public void testSoapFaultTraceIpReveal() throws Exception {
        String batchRequestUrl = TestUtil.getSoapUrl() + "BatchRequest";
        String createAppointmentRequestUrl = TestUtil.getSoapUrl() + "createAppointmentRequest";
        String modifyContactRequestUrl = TestUtil.getSoapUrl() + "ModifyContactRequest";
        String noOpRequestUrl = TestUtil.getSoapUrl() + "NoOpRequest";
        String getMiniCalRequestUrl = TestUtil.getSoapUrl() + "GetMiniCalRequest";

        PostMethod batchRequestMethod = new PostMethod(batchRequestUrl);
        HttpClientUtil.executeMethod(batchRequestMethod);
        String response = batchRequestMethod.getResponseBodyAsString();
        Assert.assertFalse("Trace contains ip address.", response.contains(batchRequestUrl));

        PostMethod createAppointmentRequestMethod = new PostMethod(createAppointmentRequestUrl);
        HttpClientUtil.executeMethod(createAppointmentRequestMethod);
        response = createAppointmentRequestMethod.getResponseBodyAsString();
        Assert.assertFalse("Trace contains ip address.", response.contains(createAppointmentRequestUrl));

        PostMethod modifyContactRequestMethod = new PostMethod(modifyContactRequestUrl);
        HttpClientUtil.executeMethod(modifyContactRequestMethod);
        response = modifyContactRequestMethod.getResponseBodyAsString();
        Assert.assertFalse("Trace contains ip address.", response.contains(modifyContactRequestUrl));

        PostMethod noOpRequestMethod = new PostMethod(noOpRequestUrl);
        HttpClientUtil.executeMethod(noOpRequestMethod);
        response = noOpRequestMethod.getResponseBodyAsString();
        Assert.assertFalse("Trace contains ip address.", response.contains(noOpRequestUrl));

        PostMethod getMiniCalRequestMethod = new PostMethod(getMiniCalRequestUrl);
        HttpClientUtil.executeMethod(getMiniCalRequestMethod);
        response = getMiniCalRequestMethod.getResponseBodyAsString();
        Assert.assertFalse("Trace contains ip address.", response.contains(getMiniCalRequestUrl));
    }
}
