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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraHttpConnectionManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.admin.message.ParseSAMLMetadataRequest;
import com.zimbra.soap.admin.message.ParseSAMLMetadataResponse;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.SAMLIdpCertificateInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Admin SOAP handler for {@code ParseSAMLMetadataRequest} (ZCS-19567).
 *
 * <p>Accepts a SAML IdP metadata document either as a remote {@code url} (fetched server-side) or as inline XML
 * {@code content}, parses it, and returns the extracted fields: IdP Entity ID, SSO URL, SLO URL, NameID format and
 * the declared signing/encryption X.509 certificates (with SHA-256 thumbprint and validity window).</p>
 *
 * <p>The metadata XML is parsed with a hardened {@link DocumentBuilderFactory} (DTDs and external entities disabled)
 * to protect against XXE / entity-expansion attacks.</p>
 */
public class ParseSAMLMetadata extends AdminDocumentHandler {

    /** SAML 2.0 metadata namespace. */
    private static final String MD_NS = "urn:oasis:names:tc:SAML:2.0:metadata";
    /** XML digital signature namespace (KeyInfo / X509Data / X509Certificate). */

    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    /** Maximum size (bytes) of a fetched metadata document. */
    private static final int MAX_METADATA_BYTES = 5 * 1024 * 1024;

    @Override
    public com.zimbra.common.soap.Element handle(com.zimbra.common.soap.Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        ParseSAMLMetadataRequest req = zsc.elementToJaxb(request);

        // Authorization: if a domain is supplied, the caller must be able to modify it; otherwise administrative
        // authentication (already enforced by the admin SOAP service) is sufficient for this stateless parse.
        DomainSelector domainSel = req.getDomain();
        if (domainSel != null) {
            DomainBy domainBy = domainSel.getBy().toKeyDomainBy();
            Domain domain = Provisioning.getInstance().get(domainBy, domainSel.getKey());
            if (domain == null) {
                throw AccountServiceException.NO_SUCH_DOMAIN(domainSel.getKey());
            }
            checkDomainRight(zsc, domain, Admin.R_modifyDomain);
        }

        String url = StringUtil.isNullOrEmpty(req.getUrl()) ? null : req.getUrl().trim();
        String content = StringUtil.isNullOrEmpty(req.getContent()) ? null : req.getContent();

        if (url == null && content == null) {
            throw ServiceException.INVALID_REQUEST("either 'url' attribute or <content> element is required", null);
        }
        if (url != null && content != null) {
            throw ServiceException.INVALID_REQUEST("'url' attribute and <content> element are mutually exclusive",
                    null);
        }

        byte[] metadataBytes;
        if (url != null) {
            metadataBytes = fetchMetadata(url);
        } else {
            metadataBytes = content.getBytes(StandardCharsets.UTF_8);
            if (metadataBytes.length > MAX_METADATA_BYTES) {
                throw ServiceException.INVALID_REQUEST(
                        "IdP metadata document exceeds maximum size of " + MAX_METADATA_BYTES + " bytes", null);
            }
        }

        ParseSAMLMetadataResponse resp = new ParseSAMLMetadataResponse();
        parseMetadata(metadataBytes, resp);
        return zsc.jaxbToElement(resp);
    }

    /**
     * Fetch the metadata document from a remote http(s) URL.
     * @param url the remote URL from which to fetch the IdP metadata XML document
     * @return the raw bytes of the IdP metadata XML document
     */
    private byte[] fetchMetadata(String url) throws ServiceException {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw ServiceException.INVALID_REQUEST("invalid metadata URL: " + url, e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw ServiceException.INVALID_REQUEST("only http/https metadata URLs are supported: " + url, null);
        }

        HttpClientBuilder clientBuilder = ZimbraHttpConnectionManager.getExternalHttpConnMgr().newHttpClient();
        HttpClient client = clientBuilder.build();
        HttpGet get = new HttpGet(url);
        HttpResponse httpResp = null;
        try {
            httpResp = HttpClientUtil.executeMethod(client, get);
            int statusCode = httpResp.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw ServiceException.RESOURCE_UNREACHABLE(
                        "failed to fetch IdP metadata (HTTP " + statusCode + ") from " + url, null);
            }
            // getContent closes the stream and throws IOException if the document exceeds MAX_METADATA_BYTES.
            InputStream is = httpResp.getEntity().getContent();
            byte[] data = ByteUtil.getContent(is, -1, (long) MAX_METADATA_BYTES);
            if (data == null || data.length == 0) {
                throw ServiceException.FAILURE("empty IdP metadata response from " + url, null);
            }
            return data;
        } catch (HttpException | IOException e) {
            throw ServiceException.RESOURCE_UNREACHABLE("failed to fetch IdP metadata from " + url, e);
        } finally {
            if (httpResp != null) {
                EntityUtils.consumeQuietly(httpResp.getEntity());
            }
        }
    }

    /**
     * Parse the SAML metadata document and populate the response. Package-visible for unit testing.
     * @param metadataBytes the raw bytes of the SAML metadata XML document to parse
     * @param resp          the response object to populate with the parsed metadata values
     */
    void parseMetadata(byte[] metadataBytes, ParseSAMLMetadataResponse resp) throws ServiceException {
        Document doc;
        try {
            DocumentBuilder builder = newSecureDocumentBuilderFactory().newDocumentBuilder();
            doc = builder.parse(new ByteArrayInputStream(metadataBytes));
        } catch (Exception e) {
            throw ServiceException.INVALID_REQUEST("unable to parse SAML metadata XML: " + e.getMessage(), e);
        }

        NodeList idpNodes = doc.getElementsByTagNameNS(MD_NS, "IDPSSODescriptor");
        if (idpNodes.getLength() == 0) {
            throw ServiceException.INVALID_REQUEST(
                    "SAML metadata does not contain an IDPSSODescriptor (not an IdP metadata document)", null);
        }
        Element idp = (Element) idpNodes.item(0);

        resp.setEntityID(resolveEntityId(idp, doc));
        for (String endpoint : collectEndpoints(idp, "SingleSignOnService")) {
            resp.addSsoURL(endpoint);
        }
        for (String endpoint : collectEndpoints(idp, "SingleLogoutService")) {
            resp.addSloURL(endpoint);
        }
        NodeList nameIdFormats = idp.getElementsByTagNameNS(MD_NS, "NameIDFormat");
        for (int i = 0; i < nameIdFormats.getLength(); i++) {
            String text = nameIdFormats.item(i).getTextContent();
            if (!StringUtil.isNullOrEmpty(text)) {
                resp.addNameIdFormat(text.trim());
            }
        }

        NodeList keyDescriptors = idp.getElementsByTagNameNS(MD_NS, "KeyDescriptor");
        for (int i = 0; i < keyDescriptors.getLength(); i++) {
            Element keyDescriptor = (Element) keyDescriptors.item(i);
            String use = keyDescriptor.getAttribute("use");
            NodeList certNodes = keyDescriptor.getElementsByTagNameNS(DS_NS, "X509Certificate");
            for (int j = 0; j < certNodes.getLength(); j++) {
                String certText = certNodes.item(j).getTextContent();
                if (!StringUtil.isNullOrEmpty(certText)) {
                    resp.addCertificate(buildCertificateInfo(use, certText));
                }
            }
        }
    }

    /**
     * The entityID lives on the ancestor {@code <EntityDescriptor>} of the IDPSSODescriptor.
     * @param idp the {@code IDPSSODescriptor} {@link Element} from which to begin the ancestor
     *            traversal
     * @param doc the root {@link Document} used as a fallback to locate the
     *            {@code <EntityDescriptor>} if ancestor traversal yields no result
     * @return the {@code entityID} attribute value if found, or {@code null} if no
     *         {@code <EntityDescriptor>} with a non-empty {@code entityID} exists in the document
     */
    private String resolveEntityId(Element idp, Document doc) {
        Node node = idp.getParentNode();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE && "EntityDescriptor".equals(node.getLocalName())
                    && MD_NS.equals(node.getNamespaceURI())) {
                String entityId = ((Element) node).getAttribute("entityID");
                if (!StringUtil.isNullOrEmpty(entityId)) {
                    return entityId;
                }
            }
            node = node.getParentNode();
        }
        // Fallback: first EntityDescriptor in the document.
        NodeList entities = doc.getElementsByTagNameNS(MD_NS, "EntityDescriptor");
        if (entities.getLength() > 0) {
            String entityId = ((Element) entities.item(0)).getAttribute("entityID");
            return StringUtil.isNullOrEmpty(entityId) ? null : entityId;
        }
        return null;
    }

    /**
     * Collect every advertised endpoint (SingleSignOnService / SingleLogoutService), one entry per binding, each
     * formatted as {@code Binding=Location}.
     * @param idp          the {@code IDPSSODescriptor} {@link Element} from which to collect
     *                     the service endpoints
     * @param endpointName the local name of the endpoint element to collect, typically
     *                     {@code SingleSignOnService} or {@code SingleLogoutService}
     * @return a {@link List} of {@code binding=location} strings, one per advertised endpoint;
     *         never {@code null}, but may be empty if no matching endpoints are found or all
     *         have missing {@code Location} attributes

     */
    private List<String> collectEndpoints(Element idp, String endpointName) {
        List<String> result = new ArrayList<String>();
        NodeList endpoints = idp.getElementsByTagNameNS(MD_NS, endpointName);
        for (int i = 0; i < endpoints.getLength(); i++) {
            Element endpoint = (Element) endpoints.item(i);
            String binding = endpoint.getAttribute("Binding");
            String location = endpoint.getAttribute("Location");
            if (StringUtil.isNullOrEmpty(location)) {
                continue;
            }
            result.add((StringUtil.isNullOrEmpty(binding) ? "" : binding) + "=" + location);
        }
        return result;
    }

    private SAMLIdpCertificateInfo buildCertificateInfo(String use, String base64Cert) throws ServiceException {
        SAMLIdpCertificateInfo info = new SAMLIdpCertificateInfo();
        if (!StringUtil.isNullOrEmpty(use)) {
            info.setUse(use);
        }
        String cleaned = base64Cert.replaceAll("\\s", "");
        info.setValue(toPem(cleaned));
        try {
            byte[] der = Base64.decodeBase64(cleaned);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(der));

            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            info.setThumbprint(toHex(sha256.digest(cert.getEncoded())));
            info.setNotBefore(formatInstant(cert.getNotBefore()));
            info.setNotAfter(formatInstant(cert.getNotAfter()));
            info.setSubjectDN(cert.getSubjectX500Principal().getName());
            info.setIssuerDN(cert.getIssuerX500Principal().getName());
        } catch (CertificateException | java.security.NoSuchAlgorithmException e) {
            // Return the raw certificate value even when the details can't be derived, but log the reason.
            ZimbraLog.security.warn("ParseSAMLMetadata: unable to decode X.509 certificate from IdP metadata", e);
        }
        return info;
    }

    private static String formatInstant(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    /**
     * Wrap a single-line base64 DER certificate in standard PEM {@code -----BEGIN/END CERTIFICATE-----} headers,
     * with the body split into 64-character lines.
     * @param base64Der the raw single-line base64 DER-encoded certificate string, as extracted
     *                  from the {@code <ds:X509Certificate>} element in the SAML metadata XML;
     *                  must not be {@code null}
     * @return a PEM-formatted certificate string with {@code -----BEGIN CERTIFICATE-----} and
     *         {@code -----END CERTIFICATE-----} headers and the body wrapped at 64 characters
     *         per line
     */
    private static String toPem(String base64Der) {
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN CERTIFICATE-----\n");
        for (int i = 0; i < base64Der.length(); i += 64) {
            sb.append(base64Der, i, Math.min(i + 64, base64Der.length())).append('\n');
        }
        sb.append("-----END CERTIFICATE-----");
        return sb.toString();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private static DocumentBuilderFactory newSecureDocumentBuilderFactory() throws ServiceException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setNamespaceAware(true);
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);
        } catch (Exception e) {
            throw ServiceException.FAILURE("unable to configure secure XML parser", e);
        }
        return dbf;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_modifyDomain);
        notes.add("Requires modify-domain right on the target domain when a <domain> is specified; "
                + "otherwise administrative authentication is sufficient.");
    }
}
