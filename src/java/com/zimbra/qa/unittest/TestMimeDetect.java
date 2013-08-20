/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.mime.MimeDetect;

public final class TestMimeDetect extends TestCase {

    public void testXLSMFileName() {
        assertEquals("application/vnd.ms-excel.sheet.macroEnabled.12", MimeDetect.getMimeDetect().detect("1.xlsm"));
    }

    public void testDOCMextension() {
        assertEquals("application/vnd.ms-word.document.macroEnabled.12", MimeDetect.getMimeDetect().detect("1.DOCM"));
    }

    public void testPPTMextension() {
        assertEquals("application/vnd.ms-powerpoint.presentation.macroEnabled.12",
                MimeDetect.getMimeDetect().detect("1.PPtM"));
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestMimeDetect.class);
    }
}
