/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016, 2017 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.zmime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import com.zimbra.common.util.CharsetUtil;

import junit.framework.Assert;

public class ZInternetHeaderTest {

    private static String RAW_HEADER = "=?utf-8?B?V1NVUzog5pu05paw44OX44Ot44Kw44Op44Og44Gu54q25oWL?=\r\n " +
            "=?utf-8?B?44Gu5qaC6KaB44KSIEJXU1VTVk1TViDjgYvjgonlj5fkv6HjgZfjgb7j?=\r\n " +
            "=?utf-8?B?gZfjgZ8=?=";
    private static String RAW_HEADER_COMBINED = "=?utf-8?B?V1NVUzog5pu05paw44OX44Ot44Kw44Op44Og44Gu54q25oWL" +
            "44Gu5qaC6KaB44KSIEJXU1VTVk1TViDjgYvjgonlj5fkv6HjgZfjgb7j" +
            "gZfjgZ8=?=";
    private static String ZBUG536 = "=?iso-8859-1?B?QVBBRCAtIFN0YXRzIEFQQUQgLSBE6WJ1dCBldCBmaW4gZOljaXNpb24gZW50cmUgcG91ciBsZSBt\n" +
        "  b2lzIGRlIEp1aW4gMjAxOA==?=";
    private static String DECODED_HEADER = "WSUS: 更新プログラムの状態の概要を BWSUSVMSV から受信しました";
    private static String RAW_HEADER_FRENCH1 = "[FSU] Fwd: XXXXXX] =?UTF-8?Q?r=C3=A9ponse_=C3=A0?= la lettre du =?UTF-8?Q?pr=C3=A9sident=2E?=";
    private static String DECODED_FRENCH1 = "[FSU] Fwd: XXXXXX] réponse à la lettre du président.";
    private static String RAW_HEADER_FRENCH2 = "Je vais en =?utf-8?Q?v=C3=A9lo_=C3=A0_l'?= =?utf-8?Q?=C3=A9t=C3=A9?=";
    private static String DECODED_FRENCH2 = "Je vais en vélo à l'été";

    // RFC 2047 Section 8. Examples
    private static String RAW_Sec8_Ex2     = "(=?ISO-8859-1?Q?a?= b)";
    private static String DECODED_Sec8_Ex2 = "(a b)";
    private static String RAW_Sec8_Ex3     = "(=?ISO-8859-1?Q?a?= =?ISO-8859-1?Q?b?=)";
    private static String DECODED_Sec8_Ex3 = "(ab)";
    private static String RAW_Sec8_Ex4     = "(=?ISO-8859-1?Q?a?=  =?ISO-8859-1?Q?b?=)";
    private static String DECODED_Sec8_Ex4 = "(ab)";
    private static String RAW_Sec8_Ex5     = "(=?ISO-8859-1?Q?a?=\n  =?ISO-8859-1?Q?b?=)";
    private static String DECODED_Sec8_Ex5 = "(ab)";
    private static String RAW_Sec8_Ex6     = "(=?ISO-8859-1?Q?a_b?=)";
    private static String DECODED_Sec8_Ex6 = "(a b)";
    private static String RAW_Sec8_Ex7     = "(=?ISO-8859-1?Q?a?= =?ISO-8859-2?Q?_b?=)";
    private static String DECODED_Sec8_Ex7 = "(a b)";

    // 'encoded-word' that is incorrectly formed
    private static String RAW_INVALID1 = "(=?charset?Q?=?=\n =?charset?Q?AB?=)";
    private static String EXP_INVALID1 = "(=?charset?Q?==?charset?Q?AB?=)";
    private static String RAW_INVALID2 = "abc(=?charset?=\n =?UTF-8?Q?a?=)";
    private static String EXP_INVALID2 = "abc(=?charset?= =?UTF-8?Q?a?=)";
    private static String RAW_INVALID3 = "=?euc-jp?B?=1B?=";
    private static String EXP_INVALID3 = "=?euc-jp?B?=1B?=";
    private static String ZBUG1022 ="=?UTF-8?Q?Memo Manager Data Import ATA.SDOA.AMMP512.D042419.T0943.xm?=\n" +
        " =?UTF-8?Q?l APR 24-2019 08:10 AM (GMT - 5:00).?=";

    /**
     * Created this test file using an external Python script.  Each line of the file contains the following elements,
     * each separated by a single TAB character:
     *   messageId
     *   decoded-subject-header
     *   encoded-subject-header
     * Both the decoded-subject-header and encoded-subject-header are stored in the file as a base64-encoded chunk.
     * This is because they may span multiple lines and the decoded-subject-headers may contain new lines or tabs.
     */
    private static final InputStream HEADER_TEST_DATA = ZInternetHeaderTest.class.getResourceAsStream("ZInternetHeaderTest.dat");

    @Test
    public void testMultilineUtf8Subject() {
        String decodedHeader = ZInternetHeader.decode(RAW_HEADER);
        Assert.assertEquals(DECODED_HEADER, decodedHeader);
    }

    @Test
    public void testCombinedUtf8Subject() {
        String decodedHeader = ZInternetHeader.decode(RAW_HEADER_COMBINED);
        Assert.assertEquals(DECODED_HEADER.length(), decodedHeader.length());
        Assert.assertEquals(DECODED_HEADER, decodedHeader);
    }

    @Test
    public void testDecodeFromResource ()
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(HEADER_TEST_DATA));
        try {
            String line = reader.readLine();
            String[] parts;
            String encoded, decoded, decodedHeader;
            while (line != null) {
                parts = line.split("\t");
                decoded = new String(Base64.decodeBase64(parts[1]), CharsetUtil.normalizeCharset("utf-8"));
                encoded = new String(Base64.decodeBase64(parts[2]), CharsetUtil.normalizeCharset("utf-8"));
                decodedHeader = ZInternetHeader.decode(encoded);
                Assert.assertEquals(
                        String.format("Decoding failed, messageId=%s, expected=\"%s\", actual=\"%s\"",
                                parts[0],
                                decoded,
                                decodedHeader),
                        decoded,
                        decodedHeader);
                line = reader.readLine();
            }
        }
        finally {
            reader.close();
        }
    }

    @Test
    public void testFrench() {
        String decodedHeader;
        decodedHeader = ZInternetHeader.decode(RAW_HEADER_FRENCH1);
        Assert.assertEquals(DECODED_FRENCH1, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_HEADER_FRENCH2);
        Assert.assertEquals(DECODED_FRENCH2, decodedHeader);
    }

    @Test
    public void testRFC2047Sec8Ex() {
        String decodedHeader = null;
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex2);
        Assert.assertEquals(DECODED_Sec8_Ex2, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex3);
        Assert.assertEquals(DECODED_Sec8_Ex3, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex4);
        Assert.assertEquals(DECODED_Sec8_Ex4, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex5);
        Assert.assertEquals(DECODED_Sec8_Ex5, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex6);
        Assert.assertEquals(DECODED_Sec8_Ex6, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_Sec8_Ex7);
        Assert.assertEquals(DECODED_Sec8_Ex7, decodedHeader);
    }

    @Test
    public void testInvalidFormat() {
        String decodedHeader = null;
        decodedHeader = ZInternetHeader.decode(RAW_INVALID1);
        Assert.assertEquals(EXP_INVALID1, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_INVALID2);
        Assert.assertEquals(EXP_INVALID2, decodedHeader);
        decodedHeader = ZInternetHeader.decode(RAW_INVALID3);
        Assert.assertEquals(EXP_INVALID3, decodedHeader);
    }

    @Test
    public void testAscii() {
        String decodedHeader;
        decodedHeader = ZInternetHeader.decode("=?us-ascii?Q?a b c?=");
        Assert.assertEquals("a b c", decodedHeader);
    }

    @Test
    public void testMultilineZBUG536Subject() {
        String decodedHeader = ZInternetHeader.decode(ZBUG536);
        Assert.assertEquals("APAD - Stats APAD - Début et fin décision entre pour le mois de Juin 2018", decodedHeader);
    }

    @Test
    public void testMultilineUTF8ZBUG1022Subject() {
        String decodedHeader = ZInternetHeader.decode(ZBUG1022);
        Assert.assertEquals("Memo Manager Data Import ATA.SDOA.AMMP512.D042419.T0943.xml APR 24-2019 08:10 AM (GMT - 5:00).", decodedHeader);
    }
}
