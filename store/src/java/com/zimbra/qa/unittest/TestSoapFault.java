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

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.zimbra.common.httpclient.HttpClientUtil;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestSoapFault extends TestCase {
    @Test
    public void testSoapFaultTraceIpReveal() throws Exception {
        String batchRequestUrl = TestUtil.getSoapUrl() + "BatchRequest";
        String createAppointmentRequestUrl = TestUtil.getSoapUrl() + "createAppointmentRequest";
        String modifyContactRequestUrl = TestUtil.getSoapUrl() + "ModifyContactRequest";
        String noOpRequestUrl = TestUtil.getSoapUrl() + "NoOpRequest";
        String getMiniCalRequestUrl = TestUtil.getSoapUrl() + "GetMiniCalRequest";

        HttpPost batchRequestMethod = new HttpPost(batchRequestUrl);
        HttpResponse httpResponse = HttpClientUtil.executeMethod(batchRequestMethod);
        String response = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertFalse("Trace contains ip address.", response.contains(batchRequestUrl));

        HttpPost createAppointmentRequestMethod = new HttpPost(createAppointmentRequestUrl);
         httpResponse = HttpClientUtil.executeMethod(createAppointmentRequestMethod);
        response = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertFalse("Trace contains ip address.", response.contains(createAppointmentRequestUrl));

        HttpPost modifyContactRequestMethod = new HttpPost(modifyContactRequestUrl);
        httpResponse = HttpClientUtil.executeMethod(modifyContactRequestMethod);
        response = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertFalse("Trace contains ip address.", response.contains(modifyContactRequestUrl));

        HttpPost noOpRequestMethod = new HttpPost(noOpRequestUrl);
        httpResponse = HttpClientUtil.executeMethod(noOpRequestMethod);
        response = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertFalse("Trace contains ip address.", response.contains(noOpRequestUrl));

        HttpPost getMiniCalRequestMethod = new HttpPost(getMiniCalRequestUrl);
        httpResponse = HttpClientUtil.executeMethod(getMiniCalRequestMethod);
        response = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertFalse("Trace contains ip address.", response.contains(getMiniCalRequestUrl));
    }
}
