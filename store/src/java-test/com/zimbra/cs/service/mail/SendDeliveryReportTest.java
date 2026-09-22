/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.mail;

import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SendDeliveryReportTest {

    @Test
    public void testGetRefHeader() throws Exception {
        Method method = com.zimbra.cs.service.mail.SendDeliveryReport.class.getDeclaredMethod(
                "getRefHeader",
                String[].class,
                String.class);

        method.setAccessible(true);
        String[] refs = {"<oldref@test.com>"};
        String result = (String) method.invoke(
                null,
                refs,
                "<msg@test.com>");

        assertEquals("<oldref@test.com> <msg@test.com>", result);
    }

    @Test
    public void testGetRefHeaderWithoutRefs() throws Exception {
        Method method = com.zimbra.cs.service.mail.SendDeliveryReport.class.getDeclaredMethod(
                "getRefHeader",
                String[].class,
                String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(
                null,
                null,
                "<msg@test.com>");

        assertEquals("<msg@test.com>", result);
    }

    @Test
    public void testGetRefHeaderTrim() throws Exception {
        Method method = com.zimbra.cs.service.mail.SendDeliveryReport.class.getDeclaredMethod(
                "getRefHeader",
                String[].class,
                String.class);

        method.setAccessible(true);
        String[] refs = {"      <oldref@test.com>      "};
        String result = (String) method.invoke(
                null,
                refs,
                "      <msg@test.com>      ");

        assertEquals("<oldref@test.com> <msg@test.com>", result);
    }
}
