/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
