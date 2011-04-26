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
package com.zimbra.cs.mime;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailServiceException;

/**
 * Unit test for {@link ParsedContact}.
 *
 * @author ysasaki
 */
public final class ParsedContactTest {

    @Test
    public void tooBigField() throws Exception {
        try {
            new ParsedContact(Collections.singletonMap(Strings.repeat("k", 101), "v"));
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

        try {
            new ParsedContact(Collections.singletonMap("k", Strings.repeat("v", 10000001)));
            Assert.fail();
        } catch (MailServiceException e) {
            Assert.assertEquals(MailServiceException.CONTACT_TOO_BIG, e.getCode());
        }

        Map<String, String> fields = new HashMap<String, String>();
        for (int i = 0; i < 1001; i++) {
           fields.put("k" + i, "v" + i);
        }
        try {
            new ParsedContact(fields);
            Assert.fail();
        } catch (ServiceException e) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }

    }

}
