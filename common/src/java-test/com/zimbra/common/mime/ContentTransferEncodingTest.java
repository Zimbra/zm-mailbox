/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.mime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;

public class ContentTransferEncodingTest {

    void testQPDecode(String msg, String input, String output) throws IOException {
        InputStream bais = new ByteArrayInputStream(input.getBytes());
        InputStream is = new ContentTransferEncoding.QuotedPrintableDecoderStream(bais);
        Assert.assertArrayEquals(msg, ByteUtil.getContent(is, -1), output.getBytes());
    }

    @Test
    public void qpdecode() throws IOException {
        testQPDecode("empty", "", "");
        testQPDecode("no transform", "the dog\r\nthe cat\r\n", "the dog\r\nthe cat\r\n");
        testQPDecode("no transform, no CRLF", "the dog is brown", "the dog is brown");

        testQPDecode("1 char wsp", "burger barn\r\n", "burger barn\r\n");
        testQPDecode("2 chars wsp", "burger  barn\r\n", "burger  barn\r\n");
        testQPDecode("3 chars wsp", "burger   barn\r\n", "burger   barn\r\n");
        testQPDecode("5 chars wsp", "burger    \tbarn\r\n", "burger    \tbarn\r\n");
        testQPDecode("trailing wsp", "burger   \r\n  barn", "burger\r\n  barn");
        testQPDecode("varied trailing wsp", "burger \t \r\n  barn", "burger\r\n  barn");
        testQPDecode("varied embedded wsp", "burger \tbarn\r\n", "burger \tbarn\r\n");
        testQPDecode("varied embedded wsp", "burger \t barn\r\n", "burger \t barn\r\n");
        testQPDecode("embedded and trailing wsp", "burger   \r\n  barn ", "burger\r\n  barn");
        testQPDecode("1 char terminal wsp", "burger ", "burger");
        testQPDecode("2 chars terminal wsp", "burger \t", "burger");
        testQPDecode("3 chars terminal wsp", "burger \t", "burger");
        testQPDecode("5 chars terminal wsp", "burger \t  \t", "burger");
        testQPDecode("bare CR ending wsp", "burger \t  \t\r barn", "burger \t  \t\r barn");
        testQPDecode("bare CR EOF ending wsp", "burger \t  \t\r", "burger \t  \t\r");

        testQPDecode("encoded char", "e =3D mc^2", "e = mc^2");
        testQPDecode("wrong case encoded char", "e =3d mc^2", "e = mc^2");
        testQPDecode("unnecessarily encoded char", "e=20=3D=20mc^2", "e = mc^2");
        testQPDecode("misencoded char", "e = 3D mc^2", "e = 3D mc^2");
        testQPDecode("misencoded before encoded char", "e ==3D mc^2", "e == mc^2");
        testQPDecode("invalid encoded char", "e =GD mc^2", "e =GD mc^2");
        testQPDecode("half misencoded before encoded char", "e =5=3D mc^2", "e =5= mc^2");

        testQPDecode("soft break", "bea=\r\ngle\r\n", "beagle\r\n");
        testQPDecode("soft break bare LF", "bea=\ngle\n", "beagle\n");
        testQPDecode("double soft break", "bea=\r\n=\r\ngle\r\n", "beagle\r\n");
    }
}
