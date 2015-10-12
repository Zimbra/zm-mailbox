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
    private static String DECODED_HEADER = "WSUS: 更新プログラムの状態の概要を BWSUSVMSV から受信しました";

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
}
