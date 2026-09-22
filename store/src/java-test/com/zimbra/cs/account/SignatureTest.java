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

package com.zimbra.cs.account;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Signature.SignatureContent;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.account.ToXML;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link Signature} backed by a real {@link Account} from the
 * in-memory {@link MockProvisioning} harness. Covers entry type, id mutation through
 * raw attrs, plain/HTML content extraction, and the HTML defang path in getContents().
 */
public class SignatureTest {

    private Provisioning prov;

    private Account account;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // Overwrite-on-duplicate contract: safe to recreate the fixture per-method.
        prov.createAccount("sig@example.com", "secret", new HashMap<String, Object>());
        account = prov.get(AccountBy.name, "sig@example.com");
    }

    private Signature newSignature(String name, String id, Map<String, Object> attrs) throws Exception {
        return new Signature(account, name, id, attrs, prov);
    }

    @Test
    public void getEntryTypeAnySignatureIsSignature() throws Exception {
        // Arrange
        Signature sig = newSignature("work", "sig-id-1", new HashMap<String, Object>());

        // Act
        Entry.EntryType type = sig.getEntryType();

        // Assert
        assertEquals(Entry.EntryType.SIGNATURE, type);
    }

    @Test
    public void getAccountIdConstructedWithAccountReturnsAccountId() throws Exception {
        // Arrange
        Signature sig = newSignature("work", "sig-id-2", new HashMap<String, Object>());

        // Act
        String acctId = sig.getAccountId();

        // Assert — the signature is tied to the owning account.
        assertEquals(account.getId(), acctId);
    }

    @Test
    public void setIdNewValueUpdatesIdAndRawAttr() throws Exception {
        // Arrange
        Signature sig = newSignature("work", "orig-id", new HashMap<String, Object>());

        // Act
        sig.setId("changed-id");

        // Assert — cached id changed and the raw attr reflects it.
        assertEquals("changed-id", sig.getId());
        assertEquals("changed-id", sig.getRawAttrs().get(Provisioning.A_zimbraSignatureId));
    }

    @Test
    public void getContentsPlainTextSignatureYieldsTextPlainContent() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignature, "Regards, Bob");
        Signature sig = newSignature("plain", "sig-plain", attrs);

        // Act
        Set<SignatureContent> contents = sig.getContents();

        // Assert — exactly one content, text/plain, unmodified body.
        assertEquals(1, contents.size());
        SignatureContent only = contents.iterator().next();
        assertEquals("text/plain", only.getMimeType());
        assertEquals("Regards, Bob", only.getContent());
    }

    @Test
    public void getContentsHtmlSignatureYieldsTextHtmlContentDefanged() throws Exception {
        // Arrange — HTML body goes through the defanger.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML, "<b>Regards</b>");
        Signature sig = newSignature("html", "sig-html", attrs);

        // Act
        Set<SignatureContent> contents = sig.getContents();

        // Assert — single text/html entry that preserves the safe bold markup.
        assertEquals(1, contents.size());
        SignatureContent only = contents.iterator().next();
        assertEquals("text/html", only.getMimeType());
        assertNotNull(only.getContent());
        assertTrue("safe markup must survive defang", only.getContent().contains("Regards"));
    }

    @Test
    public void getContentsHtmlWithScriptDefangStripsScript() throws Exception {
        // Arrange — a script tag must not survive defanging.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML,
                "<div>Hi<script>alert('x')</script></div>");
        Signature sig = newSignature("htmlScript", "sig-script", attrs);

        // Act
        SignatureContent only = sig.getContents().iterator().next();

        // Assert — the executable script content is sanitized away.
        assertEquals("text/html", only.getMimeType());
        assertFalse("script payload must be stripped", only.getContent().contains("alert('x')"));
    }

    @Test
    public void getContentsBothPlainAndHtmlYieldsTwoContents() throws Exception {
        // Arrange — set both signature attrs.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignature, "plain body");
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML, "<i>html body</i>");
        Signature sig = newSignature("both", "sig-both", attrs);

        // Act
        Set<SignatureContent> contents = sig.getContents();

        // Assert — both representations present with both mime types.
        assertEquals(2, contents.size());
        boolean sawPlain = false;
        boolean sawHtml = false;
        for (SignatureContent c : contents) {
            if ("text/plain".equals(c.getMimeType())) {
                sawPlain = true;
            } else if ("text/html".equals(c.getMimeType())) {
                sawHtml = true;
            }
        }
        assertTrue("text/plain content expected", sawPlain);
        assertTrue("text/html content expected", sawHtml);
    }

    @Test
    public void getContentsNoSignatureAttrsYieldsEmptySet() throws Exception {
        // Arrange — no signature content set.
        Signature sig = newSignature("empty", "sig-empty", new HashMap<String, Object>());

        // Act
        Set<SignatureContent> contents = sig.getContents();

        // Assert — nothing to emit.
        assertTrue("no signature attrs means no contents", contents.isEmpty());
    }

    @Test
    public void signatureContentConstructorExposesMimeTypeAndContent() {
        // Arrange / Act
        SignatureContent content = new SignatureContent("text/plain", "body text");

        // Assert — value object faithfully stores both fields.
        assertEquals("text/plain", content.getMimeType());
        assertEquals("body text", content.getContent());
    }

    @Test
    public void getNameConstructedSignatureReturnsProvidedName() throws Exception {
        // Arrange
        Signature sig = newSignature("MySig", "sig-name", new HashMap<String, Object>());

        // Act / Assert
        assertEquals("MySig", sig.getName());
    }

    // ------------------------------------------------------------------
    // ToXML.encodeSignature -- drive getContents() through the production SOAP encoder and
    // assert on the emitted <signature>/<content> element tree (name/id attrs + per-mime-type
    // content bodies, including the HTML defang path). No network/LDAP required.
    // ------------------------------------------------------------------

    /* Finds the single content element whose type attr matches, or null. */
    private static Element contentOfType(Element signatureElem, String mimeType) throws Exception {
        for (Element content : signatureElem.listElements(AccountConstants.E_CONTENT)) {
            if (mimeType.equals(content.getAttribute(AccountConstants.A_TYPE))) {
                return content;
            }
        }
        return null;
    }

    @Test
    public void encodeSignaturePlainAndHtmlEmitsNameIdAndBothContents() throws Exception {
        // Arrange -- a signature with both plain and HTML bodies.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignature, "Regards, Bob");
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML, "<b>Regards</b>");
        Signature sig = newSignature("enc-both", "sig-enc-both", attrs);
        Element parent = new Element.XMLElement("GetSignaturesResponse");

        // Act -- encodeSignature calls sig.getContents() internally and serializes the result.
        Element sigElem = ToXML.encodeSignature(parent, sig);

        // Assert -- name/id attrs plus one <content> per mime type with the expected bodies.
        assertEquals("enc-both", sigElem.getAttribute(AccountConstants.A_NAME));
        assertEquals("sig-enc-both", sigElem.getAttribute(AccountConstants.A_ID));
        List<Element> contents = sigElem.listElements(AccountConstants.E_CONTENT);
        assertEquals(2, contents.size());
        assertEquals("Regards, Bob", contentOfType(sigElem, "text/plain").getText());
        Element html = contentOfType(sigElem, "text/html");
        assertNotNull("an HTML content element must be emitted", html);
        assertTrue("safe markup must survive defang", html.getText().contains("Regards"));
    }

    @Test
    public void encodeSignatureHtmlWithScriptEmitsDefangedHtmlContent() throws Exception {
        // Arrange -- an HTML signature carrying a script payload that must be stripped.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML,
                "<div>Hi<script>alert('x')</script></div>");
        Signature sig = newSignature("enc-script", "sig-enc-script", attrs);
        Element parent = new Element.XMLElement("GetSignaturesResponse");

        // Act
        Element sigElem = ToXML.encodeSignature(parent, sig);

        // Assert -- exactly one text/html content whose body has been defanged.
        List<Element> contents = sigElem.listElements(AccountConstants.E_CONTENT);
        assertEquals(1, contents.size());
        Element html = contents.get(0);
        assertEquals("text/html", html.getAttribute(AccountConstants.A_TYPE));
        assertFalse("script payload must be stripped from the encoded content",
                html.getText().contains("alert('x')"));
    }
}
