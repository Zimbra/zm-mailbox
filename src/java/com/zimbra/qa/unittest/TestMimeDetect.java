/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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
