/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.mime;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.IncomingBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ParsedMessageOptionsTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        StoreManager.getInstance().startup();
    }

    @Test
    public void whenBufferNullContentType() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, null);
        assertEquals(blob.getFile(), parsedMessageOptions.getFile());
        assertEquals(blob.getDigest(), parsedMessageOptions.getDigest());
        assertEquals(blob.getRawSize(), parsedMessageOptions.getSize().longValue());
    }

    @Test
    public void whenBufferNotNullContentType() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        byte[] buffer = "test".getBytes();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, buffer);
        assertEquals(buffer, parsedMessageOptions.getRawData());
        assertEquals(blob.getDigest(), parsedMessageOptions.getDigest());
        assertEquals(blob.getRawSize(), parsedMessageOptions.getSize().longValue());
    }

    @Test
    public void testWithMimeMessage() throws Exception {
        String content =
                "Subject: Testing\r\n"
                        + "Content-Type: multipart/mixed; boundary=\"ABC\"\r\n" +
                        "--ABC\r\n" +
                        "Content-Type: multipart/mixed\r\n" +
                        "part1\r\n" +
                        "Content-Type:multipart/mixed\r\t" +
                        "part2\r\n" +
                        "--ABC\r\n";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions();
        parsedMessageOptions.setContent(mm);
        assertSame(mm, parsedMessageOptions.getMimeMessage());
    }

    @Test
    public void testSetContentByteArray() {
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions();
        byte[] bytes = "Hello".getBytes();
        parsedMessageOptions.setContent(bytes);
        assertArrayEquals(bytes, parsedMessageOptions.getRawData());
    }

    @Test
    public void testSetContentByFile() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, null);
        assertEquals(blob.getFile(), parsedMessageOptions.getFile());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetContentByteArrayAfterMimeMessage() throws Exception {
        String content =
                "Subject: Testing\r\n\r\nHello";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, null);
        parsedMessageOptions.setContent(mm);
        parsedMessageOptions.setContent("Hello".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetContentByteArrayAfterFile() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, null);
        parsedMessageOptions.setContent(blob.getFile());
        parsedMessageOptions.setContent("Hello".getBytes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetFileAfterContentByteArray() throws Exception {
        String content =
                "Subject: Testing\r\n\r\nHello";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, null);
        parsedMessageOptions.setContent(mm);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetContentMimeMessageAfterFile() throws Exception {
        String content =
                "Subject: Testing\r\n\r\nHello";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, "hello".getBytes());
        parsedMessageOptions.setContent(mm);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetContentByArrayWhenMimeMessageAndFileThere() throws Exception {
        String content =
                "Subject: Testing\r\n\r\nHello";
        MimeMessage mm = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(
                content.getBytes()));
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, "hello".getBytes());
        parsedMessageOptions.setContent("hello".getBytes());
        parsedMessageOptions.setContent(blob.getFile());
        parsedMessageOptions.setContent(mm);
    }

    @Test
    public void testReceiveDateAndAttachmentIndexing() throws Exception {
        StoreManager sm = StoreManager.getInstance();
        IncomingBlob incoming = sm.newIncomingBlob("test", null);
        Blob blob = incoming.getBlob();
        ParsedMessageOptions parsedMessageOptions = new ParsedMessageOptions(blob, "hello".getBytes(), 20L, true);
        long receivedDate = parsedMessageOptions.getReceivedDate();
        assertEquals(20L, receivedDate);
        assertTrue(parsedMessageOptions.getAttachmentIndexing());
    }
}


