/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.common.mime.shim;

import javax.mail.internet.InternetHeaders;

import org.junit.Test;
import org.testng.Assert;

public class JavaMailInternetHeadersTest {

    private static final String CTYPE_1 = "text/plain";
    private static final String CTYPE_2 = "multipart/mixed; boundary=asdkjflasdgfl";
    private static final String FROM_1  = "bob@example.com";
    private static final String FROM_2  = "steve@example.com";

    @Test
    public void getDuplicateHeader() throws Exception {
        InternetHeaders headers = new JavaMailInternetHeaders();
        headers.addHeader("Content-Type", CTYPE_1);
        headers.addHeader("Content-Type", CTYPE_2);
        headers.addHeader("From", FROM_1);
        headers.addHeader("From", FROM_2);

        Assert.assertEquals(headers.getHeader("FROM", null), FROM_1, "select first From");
        Assert.assertEquals(headers.getHeader("content-TYPE", null), CTYPE_2, "select last Content-Type");
    }
}
