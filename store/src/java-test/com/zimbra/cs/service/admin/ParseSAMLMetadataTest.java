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

package com.zimbra.cs.service.admin;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.admin.message.ParseSAMLMetadataResponse;
import com.zimbra.soap.admin.type.SAMLIdpCertificateInfo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**

 * Unit tests for {@link ParseSAMLMetadata} - exercises the IdP metadata XML extraction logic directly (the
 * inline-content path, which needs neither a SOAP context nor provisioning). Uses a sample Keycloak IdP metadata
 * document that advertises multiple SSO/SLO bindings.
 */
public class ParseSAMLMetadataTest {

    private static final String ENTITY_ID = "https://keycloak.zimbradev.com/realms/zimbra";

    private static final String SAML_ENDPOINT = "https://keycloak.zimbradev.com/realms/zimbra/protocol/saml";

    // The signing certificate from the sample metadata (base64 DER, single line, as it appears in ds:X509Certificate).
    private static final String TEST_CERT =
            "MIICmzCCAYMCBgGeFxriADANBgkqhkiG9w0BAQsFADARMQ8wDQYDVQQDDAZ6aW1icmEwHhcNMjYwNTExMTI1MzE4WhcN"
            + "MzYwNTExMTI1NDU4WjARMQ8wDQYDVQQDDAZ6aW1icmEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC4nJW/"
            + "XmO+8zHtqpfO/KkX1KciLKzQB17civmferv1PrD6cTZ9PEBrwHeHFj+rVuDVMbx2QwM0EDpbSx2LjdjbOaYM3ODpwl6"
            + "ForneL4Ib7e2zp0HgC+/HRF7BRl3peLEkzATZ4FPkcL/0dw/tISuyZqPMptCm5tAl/o+iT1p+JHQNZ/pTsa2ZDKbbS4"
            + "wujeuxT5GXIzKEpa0lrkOxg6mKq9HeUoS1M36O9lDivZDnK9M9qg1jUZbrMxBy0kCF2h3R8ld6u+IZWZcg7QOG10hpJ"
            + "e9nUsCAdYcrSLZmoSvuHKcvMoWoYCtSsyTu6byK4q9pZ1RR+D35eoYsnioqWKtbAgMBAAEwDQYJKoZIhvcNAQELBQAD"
            + "ggEBAE8/oMEfEnZauULeVSUXMjWmTtymkBTO1G3sgox9TEGwo9uU4wA3TEG/Hr06pN7+JWZOA3D9KHtdO9gKLQBBRRK"
            + "ScQ+V5txN7sBkNDCGV3wV3pY9JDMPsgoqM7qOv23alnWrmV0WQg+AuYkR8cot1QOVflPnqbU/NW3smc9HMAab5yh6BF"
            + "aKEy13coYhA2o0OYgajITamszUYkpMjbOkCAwaGQGG3wmjY/2fUWJW82Mrwr1hY9fJTHPb2lAZxfeoTWThOVc8Dy5Vf"
            + "UtvoFAQ9IPJhi46Fc5CKcAAAkeKwixLDzaeD4IrJUlZravBLJH/zCWgMs8dd531e1SlzIe0x2U=";

    private static final String IDP_METADATA =
            "<md:EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\""
            + " xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\""
            + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
            + " xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" entityID=\"" + ENTITY_ID + "\">"
            + "<md:IDPSSODescriptor WantAuthnRequestsSigned=\"true\""
            + " protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
            + "<md:KeyDescriptor use=\"signing\"><ds:KeyInfo>"
            + "<ds:KeyName>7ocJh5tFqdpnvtIy4IxgozmCEJ3jl07lOyDpg38DOtE</ds:KeyName>"
            + "<ds:X509Data><ds:X509Certificate>" + TEST_CERT + "</ds:X509Certificate></ds:X509Data>"
            + "</ds:KeyInfo></md:KeyDescriptor>"
            + "<md:ArtifactResolutionService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\""
            + " Location=\"" + SAML_ENDPOINT + "/resolve\" index=\"0\"/>"
            + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat>"
            + "<md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:transient</md:NameIDFormat>"
            + "<md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat>"
            + "<md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat>"
            + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "<md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\" Location=\""
                    + SAML_ENDPOINT + "\"/>"
            + "</md:IDPSSODescriptor></md:EntityDescriptor>";

    @Test
    public void extractsAllFieldsFromIdpMetadata() throws Exception {
        ParseSAMLMetadataResponse resp = new ParseSAMLMetadataResponse();
        new ParseSAMLMetadata().parseMetadata(IDP_METADATA.getBytes(StandardCharsets.UTF_8), resp);

        assertEquals(ENTITY_ID, resp.getEntityID());

        // All four NameID formats are returned, in document order.
        List<String> nameIdFormats = resp.getNameIdFormat();
        assertEquals(4, nameIdFormats.size());
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", nameIdFormats.get(0));
        assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:transient", nameIdFormats.get(1));
        assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified", nameIdFormats.get(2));
        assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", nameIdFormats.get(3));

        // All four SSO bindings are returned as Binding=Location, in document order.
        List<String> sso = resp.getSsoURL();
        assertEquals(4, sso.size());
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST=" + SAML_ENDPOINT, sso.get(0));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect=" + SAML_ENDPOINT, sso.get(1));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:SOAP=" + SAML_ENDPOINT, sso.get(2));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact=" + SAML_ENDPOINT, sso.get(3));

        // All four SLO bindings are returned as Binding=Location, in document order.
        List<String> slo = resp.getSloURL();
        assertEquals(4, slo.size());
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST=" + SAML_ENDPOINT, slo.get(0));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect=" + SAML_ENDPOINT, slo.get(1));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact=" + SAML_ENDPOINT, slo.get(2));
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:SOAP=" + SAML_ENDPOINT, slo.get(3));

        assertEquals(1, resp.getCertificates().size());
        SAMLIdpCertificateInfo cert = resp.getCertificates().get(0);
        assertEquals("signing", cert.getUse());

        // The certificate value is returned in PEM (BEGIN/END) format.
        String pem = cert.getValue();
        assertTrue("should start with PEM header", pem.startsWith("-----BEGIN CERTIFICATE-----\n"));
        assertTrue("should end with PEM footer", pem.endsWith("-----END CERTIFICATE-----"));
        // Stripping the PEM armor and whitespace yields the original single-line base64.
        String stripped = pem.replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "").replaceAll("\\s", "");
        assertEquals(TEST_CERT, stripped);

        assertNotNull(cert.getThumbprint());
        assertEquals(64, cert.getThumbprint().length());
        assertTrue(cert.getThumbprint().matches("[0-9A-F]+"));
        assertNotNull(cert.getNotBefore());
        assertTrue("expiry should be UTC ISO-8601", cert.getNotAfter().endsWith("Z"));
        assertTrue(cert.getSubjectDN().contains("zimbra"));
    }

    @Test
    public void responseMarshalsToSoapElement() throws Exception {
        ParseSAMLMetadataResponse resp = new ParseSAMLMetadataResponse();
        new ParseSAMLMetadata().parseMetadata(IDP_METADATA.getBytes(StandardCharsets.UTF_8), resp);

        // Confirms the repeated ssoURL/sloURL elements and the @XmlValue certificate element serialize through
        // Zimbra's JAXB layer.
        Element el = JaxbUtil.jaxbToElement(resp);
        assertEquals("ParseSAMLMetadataResponse", el.getName());
        assertEquals(ENTITY_ID, el.getElement("entityID").getText());
        assertEquals(4, el.listElements("ssoURL").size());
        assertEquals(4, el.listElements("sloURL").size());
        assertEquals(4, el.listElements("nameIdFormat").size());
        assertEquals("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST=" + SAML_ENDPOINT,
                el.listElements("ssoURL").get(0).getText());
        Element certEl = el.getElement("certificate");
        assertEquals("signing", certEl.getAttribute("use"));
        assertEquals(64, certEl.getAttribute("thumbprint").length());
        assertTrue(certEl.getText().contains("-----BEGIN CERTIFICATE-----"));
    }

    @Test
    public void rejectsNonIdpMetadata() throws Exception {
        String spOnly =
                "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"sp\">"
                + "  <md:SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"/>"
                + "</md:EntityDescriptor>";
        try {
            new ParseSAMLMetadata().parseMetadata(spOnly.getBytes(StandardCharsets.UTF_8),
                    new ParseSAMLMetadataResponse());
            fail("expected ServiceException for metadata without IDPSSODescriptor");
        } catch (ServiceException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("IDPSSODescriptor"));
        }
    }

    @Test
    public void rejectsMalformedXml() throws Exception {
        try {
            new ParseSAMLMetadata().parseMetadata("<not-xml".getBytes(StandardCharsets.UTF_8),
                    new ParseSAMLMetadataResponse());
            fail("expected ServiceException for malformed XML");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void rejectsXxeDoctype() throws Exception {
        // A DOCTYPE with an external entity must be rejected outright by the hardened parser (disallow-doctype-decl).
        String xxe =
                "<?xml version=\"1.0\"?>"
                + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                + "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"&xxe;\">"
                + "  <md:IDPSSODescriptor/></md:EntityDescriptor>";
        try {
            new ParseSAMLMetadata().parseMetadata(xxe.getBytes(StandardCharsets.UTF_8),
                    new ParseSAMLMetadataResponse());
            fail("expected ServiceException - DOCTYPE declarations must be disallowed");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }
}
