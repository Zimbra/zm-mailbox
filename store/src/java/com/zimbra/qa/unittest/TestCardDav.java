/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.google.common.collect.Maps;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZFolder.View;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.qa.unittest.TestCalDav.HttpMethodExecutor;
import com.zimbra.qa.unittest.TestCalDav.MkColMethod;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.message.SearchResponse;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.type.SearchHit;

public class TestCardDav {

    @Rule
    public TestName testInfo = new TestName();
    private static String DAV1;
    private static String DAV2;

    private Account dav1;

    public static final Map<String,String> carddavNSMap;
    static {
        Map<String, String> aMap = Maps.newHashMapWithExpectedSize(2);
        aMap.put("D", DavElements.WEBDAV_NS_STRING);
        aMap.put("C", DavElements.CARDDAV_NS_STRING);
        aMap.put("CS", DavElements.CS_NS_STRING);
        aMap.put("A", DavElements.APPLE_NS_STRING);
        aMap.put("Y", DavElements.YAHOO_NS_STRING);
        carddavNSMap = Collections.unmodifiableMap(aMap);
    }

    private static String rachelVcard =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "PRODID:-//BusyMac LLC//BusyContacts 1.0.2//EN\n" +
            "FN:La Rochelle\n" +
            "N:Rochelle;La;;;\n" +
            "EMAIL;TYPE=internet:rachel@fun.org\n" +
            "CATEGORIES:BlueGroup\n" +
            "REV:2015-04-04T13:55:56Z\n" +
            "UID:07139DE2-EA7B-46CB-A970-C4DF7F72D9AE\n" +
            "X-BUSYMAC-MODIFIED-BY:Gren Elliot\n" +
            "X-CREATED:2015-04-04T13:55:25Z\n" +
            "END:VCARD\n";

    private static String blueGroupCreate =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "PRODID:-//BusyMac LLC//BusyContacts 1.0.2//EN\n" +
            "FN:BlueGroup\n" +
            "N:BlueGroup\n" +
            "REV:2015-04-04T13:55:56Z\n" +
            "UID:F53A6F96-566F-46CC-8D48-A5263FAB5E38\n" +
            "X-ADDRESSBOOKSERVER-KIND:group\n" +
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:07139DE2-EA7B-46CB-A970-C4DF7F72D9AE\n" +
            "END:VCARD\n";

    private static String parisVcard =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "FN:Paris Match\n" +
            "N:Match;Paris;;;\n" +
            "EMAIL;TYPE=internet:match@paris.fr\n" +
            "CATEGORIES:BlueGroup\n" +
            "REV:2015-04-04T13:56:50Z\n" +
            "UID:BE43F16D-336E-4C3E-BAE6-22B8F245A986\n" +
            "END:VCARD\n";

    private static String blueGroupModify =
            "BEGIN:VCARD\n" +
            "VERSION:3.0\n" +
            "PRODID:-//BusyMac LLC//BusyContacts 1.0.2//EN\n" +
            "FN:BlueGroup\n" +
            "N:BlueGroup\n" +
            "REV:2015-04-04T13:56:50Z\n" +
            "UID:F53A6F96-566F-46CC-8D48-A5263FAB5E38\n" +
            "X-ADDRESSBOOKSERVER-KIND:group\n" +
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:BE43F16D-336E-4C3E-BAE6-22B8F245A986\n" +
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:07139DE2-EA7B-46CB-A970-C4DF7F72D9AE\n" +
            "END:VCARD\n";

    public static String simpleVcard = "BEGIN:VCARD\r\n" +
                                        "VERSION:3.0\r\n" +
                                        "FN:TestCal\r\n" +
                                        "N:Dog;Scruffy\r\n" +
                                        "EMAIL;TYPE=INTERNET,PREF:scruffy@example.com\r\n" +
                                        "UID:SCRUFF1\r\n" +
                                        "END:VCARD\r\n";

    private static String smallBusyMacAttach =
            "BEGIN:VCARD\r\n" +
            "VERSION:3.0\r\n" +
            "PRODID:-//BusyMac LLC//BusyContacts 1.0.2//EN\r\n" +
            "FN:John Smith\r\n" +
            "N:Smith;John;;;\r\n" +
            "REV:2015-04-05T09:51:09Z\r\n" +
            "UID:99E01E16-03B3-4487-AAEF-AEB496852C06\r\n" +
            "X-BUSYMAC-ATTACH;ENCODING=b;X-FILENAME=favicon.ico:AAABAAEAEBAAAAEAIABoBAAA\r\n" +
            " FgAAACgAAAAQAAAAIAAAAAEAIAAAAAAAQAQAABMLAAATCwAAAAAAAAAAAAAAAAAAw4cAY8OHAM\r\n" +
            " nDhwD8w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD8w4cAycOHAGMAAAAAw4cA\r\n" +
            " Y8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/Dhw\r\n" +
            " D/w4cAY8OHAMnDhwD/w4cA/7yYSv/y5Mb/8uXH//Llx//z5sr/8+bK//Pmyv/z58v/8+bK/8qq\r\n" +
            " Y//DhwD/w4cA/8OHAMnDhwDhw4cA/8OHAP++q4D///////////////7////+//////////////\r\n" +
            " /////////Yyan/w4cA/8OHAP/DhwDhw4cA4cOHAP/DhwD/t4QR/9/azv//////5t3K/9StVv/b\r\n" +
            " t2b/27dm/9u3Z//cuGn/wpAh/8OHAP/DhwD/w4cA4cOHAOHDhwD/w4cA/8OHAP+2jzr/+fj2//\r\n" +
            " n49f/BnU7/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAOHDhwDhw4cA/8OHAP/DhwD/\r\n" +
            " w4cA/7ihbf//////8u/p/8GRJv/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwDhw4cA4cOHAP\r\n" +
            " /DhwD/w4cA/8OHAP/BhgP/0siz///////d1L//wYgI/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA\r\n" +
            " 4cOHAOHDhwD/w4cA/8OHAP/DhwD/w4cA/7eIIP/n49v//////8e0iP/DhwD/w4cA/8OHAP/Dhw\r\n" +
            " D/w4cA/8OHAOHDhwDhw4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/rItA//39/P/6+vj/w6BQ/8OH\r\n" +
            " AP/DhwD/w4cA/8OHAP/DhwDhw4cA4cOHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP+8p3r//v\r\n" +
            " 79/+3p4v+8ix3/w4cA/8OHAP/DhwD/w4cA4cOHAOHDhwD/w4cA/8CHB//VsFz/3rxx/926bf/c\r\n" +
            " uWv/xadh//Ht5///////1suz/7+HCv/DhwD/w4cA/8OHAOHDhwDhw4cA/8OHAP+wjT//+/r5//\r\n" +
            " /////////////////////+/v7///////7+/v+8n17/w4cA/8OHAP/DhwDhw4cAycOHAP/DhwD/\r\n" +
            " t4gd/+bYuP/16tP/9OjP//Toz//06M//8+fN//Pozv/t4MH/vZIx/8OHAP/DhwD/w4cAycOHAG\r\n" +
            " DDhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA\r\n" +
            " /8OHAGAAAAAAw4cAWsOHAMnDhwD8w4cA/8OHAP/DhwD/w4cA/8OHAP/DhwD/w4cA/8OHAP/Dhw\r\n" +
            " D8w4cAycOHAFoAAAAAgAEAAAAAAAAAAAAAAABoQAAAAAAAAPC/AAAAAAAAAAAAAAAAAAAiQAAA\r\n" +
            " AAAAAAAAAAAAAAAAAAAAAAAAgAEAAA==\r\n" +
            "X-BUSYMAC-MODIFIED-BY:Gren Elliot\r\n" +
            "X-CUSTOM:one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen\r\n" +
            "X-CUSTOM:Here are my simple\\nmultiline\\nnotes\r\n" +
            "X-CUSTOM;TYPE=pref:semi-colon\\;seperated\\;\"stuff\"\\;here\r\n" +
            "X-CUSTOM:comma\\,\"stuff\"\\,'there'\\,too\r\n" +
            "X-HOBBY:my backslash\\\\ hobbies\r\n" +
            "X-CREATED:2015-04-05T09:50:44Z\r\n" +
            "END:VCARD\r\n";

    // iOS/11.0 (15A372)
    private static String iosContactsPropfind =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<A:propfind xmlns:A=\"DAV:\">\n" +
            "  <A:prop>\n" +
            "    <A:add-member/>\n" +
            "    <F:bulk-requests xmlns:F=\"http://me.com/_namespace/\"/>\n" +
            "    <A:current-user-privilege-set/>\n" +
            "    <A:displayname/>\n" +
            "    <D:max-image-size xmlns:D=\"urn:ietf:params:xml:ns:carddav\"/>\n" +
            "    <D:max-resource-size xmlns:D=\"urn:ietf:params:xml:ns:carddav\"/>\n" +
            "    <C:me-card xmlns:C=\"http://calendarserver.org/ns/\"/>\n" +
            "    <A:owner/>\n" +
            "    <C:push-transports xmlns:C=\"http://calendarserver.org/ns/\"/>\n" +
            "    <C:pushkey xmlns:C=\"http://calendarserver.org/ns/\"/>\n" +
            "    <A:quota-available-bytes/>\n" +
            "    <A:quota-used-bytes/>\n" +
            "    <A:resource-id/>\n" +
            "    <A:resourcetype/>\n" +
            "    <A:supported-report-set/>\n" +
            "    <A:sync-token/>\n" +
            "  </A:prop>\n" +
            "</A:propfind>\n";

    @Before
    public void setUp() throws Exception {
        if (!TestUtil.fromRunUnitTests) {
            TestUtil.cliSetup();
        }
        DAV1 = "carddav-" + testInfo.getMethodName().toLowerCase() + "-dav1";
        DAV2 = "carddav-" + testInfo.getMethodName().toLowerCase() + "-dav2";
        cleanUp();
        dav1 = TestUtil.createAccount(DAV1);
    }

    @After
    public void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(DAV1);
        TestUtil.deleteAccountIfExists(DAV2);
    }

    public static Document doIosContactsPropOnAddressbookHomeSet(Account acct)
            throws IOException, XmlParseException {
        TestCalDav.PropFindMethod method = new TestCalDav.PropFindMethod(
                TestCalDav.getFullUrl(UrlNamespace.getAddressbookHomeSetUrl(acct.getName())));
        method.addHeader("Depth", "1");
        method.addHeader("Brief", "t");
        method.addHeader("Prefer", "return=minimal");
        method.addHeader("Accept", "*/*");
        Document doc = TestCalDav.doMethodYieldingMultiStatus(method, acct, iosContactsPropfind);
        return doc;
    }

    @Test
    public void badBasicAuthToContacts() throws Exception {
        assertNotNull("Test account object", dav1);
        String calFolderUrl = TestCalDav.getFolderUrl(dav1, "Contacts");
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet method = new HttpGet(calFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(method, dav1, "badPassword");
        HttpMethodExecutor.execute(client, method, HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    public void mkcol4addressBook() throws Exception {
        String xml = "<D:mkcol xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">" +
                "     <D:set>" +
                "       <D:prop>" +
                "         <D:resourcetype>" +
                "           <D:collection/>" +
                "           <C:addressbook/>" +
                "         </D:resourcetype>" +
                "         <D:displayname>OtherContacts</D:displayname>" +
                "         <C:addressbook-description xml:lang=\"en\">Extra Contacts</C:addressbook-description>" +
                "       </D:prop>" +
                "     </D:set>" +
                "</D:mkcol>";
        StringBuilder url = TestCalDav.getLocalServerRoot();
        url.append(DavServlet.DAV_PATH).append("/").append(dav1.getName()).append("/OtherContacts/");
        MkColMethod method = new MkColMethod(url.toString());
        TestCalDav.addBasicAuthHeaderForUser(method, dav1);
        HttpClient client = HttpClientBuilder.create().build();
        method.addHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        method.setEntity(new ByteArrayEntity(xml.getBytes(), org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_XML)));
        HttpMethodExecutor.execute(client, method, HttpStatus.SC_MULTI_STATUS);

        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(dav1.getName());
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        options.setNoSession(true);
        ZMailbox mbox = ZMailbox.getMailbox(options);
        ZFolder folder = mbox.getFolderByPath("/OtherContacts");
        assertEquals("OtherContacts", folder.getName());
        assertEquals("OtherContacts default view", View.contact, folder.getDefaultView());
    }

    @Test
    public void createContactWithIfNoneMatchTesting() throws ServiceException, IOException {
        assertNotNull("Test account object", dav1);
        String davBaseName = "SCRUFF1.vcf";  // Based on UID
        String contactsFolderUrl = TestCalDav.getFolderUrl(dav1, "Contacts");
        String url = String.format("%s%s", contactsFolderUrl, davBaseName);
        HttpClient client = HttpClientBuilder.create().build();
        HttpPut putMethod = new HttpPut(url);
        TestCalDav.addBasicAuthHeaderForUser(putMethod, dav1);
        putMethod.addHeader("Content-Type", "text/vcard");

        putMethod.setEntity(
                new ByteArrayEntity(simpleVcard.getBytes(), org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        // Bug 84246 this used to fail with 409 Conflict because we used to require an If-None-Match header
        HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_CREATED);

        // Check that trying to put the same thing again when we don't expect it to exist (i.e. Using If-None-Match
        // header) will fail.
        putMethod = new HttpPut(url);
        TestCalDav.addBasicAuthHeaderForUser(putMethod, dav1);
        putMethod.addHeader("Content-Type", "text/vcard");
        putMethod.addHeader(DavProtocol.HEADER_IF_NONE_MATCH, "*");
        putMethod.setEntity(
                new ByteArrayEntity(simpleVcard.getBytes(), org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_PRECONDITION_FAILED);
    }

    @Test
    public void appleStyleGroup() throws ServiceException, IOException {
        String contactsFolderUrl = TestCalDav.getFolderUrl(dav1, "Contacts");
        HttpClient client = HttpClientBuilder.create().build();

        HttpPost postMethod = new HttpPost(contactsFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(postMethod, dav1);
        postMethod.addHeader("Content-Type", "text/vcard");
        postMethod.setEntity(
                new ByteArrayEntity(rachelVcard.getBytes(), org.apache.http.entity.ContentType.create( MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_CREATED);

        postMethod = new HttpPost(contactsFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(postMethod, dav1);
        postMethod.addHeader("Content-Type", "text/vcard");
        postMethod.setEntity(
                new ByteArrayEntity(blueGroupCreate.getBytes(), org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor exe = HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_CREATED);
        exe.getNonNullHeaderValue("Location", "When creating Group");

        postMethod = new HttpPost(contactsFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(postMethod, dav1);
        postMethod.addHeader("Content-Type", "text/vcard");
        postMethod.setEntity(new ByteArrayEntity(parisVcard.getBytes(),
            org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_CREATED);

        String url = String.format("%s%s", contactsFolderUrl, "F53A6F96-566F-46CC-8D48-A5263FAB5E38.vcf");
        HttpPut putMethod = new HttpPut(url);
        TestCalDav.addBasicAuthHeaderForUser(putMethod, dav1);
        putMethod.addHeader("Content-Type", "text/vcard");
        putMethod.setEntity(new ByteArrayEntity(blueGroupModify.getBytes(),
            org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_NO_CONTENT);

        HttpGet getMethod = new HttpGet(url);
        TestCalDav.addBasicAuthHeaderForUser(getMethod, dav1);
        getMethod.addHeader("Content-Type", "text/vcard");
        exe = HttpMethodExecutor.execute(client, getMethod, HttpStatus.SC_OK);
        String respBody = exe.getResponseAsString();
        String [] expecteds = {
            "X-ADDRESSBOOKSERVER-KIND:group",
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:BE43F16D-336E-4C3E-BAE6-22B8F245A986",
            "X-ADDRESSBOOKSERVER-MEMBER:urn:uuid:07139DE2-EA7B-46CB-A970-C4DF7F72D9AE" };
        for (String expected : expecteds) {
            assertTrue(String.format("GET should contain '%s'\nBODY=%s", expected, respBody),
                    respBody.contains(expected));
        }

        // members are actually stored in a different way.  Make sure it isn't a fluke
        // that the GET response contained the correct members by checking that the members
        // appear where expected in a search hit.
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy("dateDesc");
        searchRequest.setLimit(8);
        searchRequest.setSearchTypes("contact");
        searchRequest.setQuery("in:Contacts");
        ZMailbox mbox = TestUtil.getZMailbox(DAV1);
        SearchResponse searchResp = mbox.invokeJaxb(searchRequest);
        assertNotNull("JAXB SearchResponse object", searchResp);
        List<SearchHit> hits = searchResp.getSearchHits();
        assertNotNull("JAXB SearchResponse hits", hits);
        assertEquals("JAXB SearchResponse hits", 3, hits.size());
        boolean seenGroup = false;
        for (SearchHit hit : hits) {
            ContactInfo contactInfo = (ContactInfo) hit;
            if ("BlueGroup".equals(contactInfo.getFileAs())) {
                seenGroup = true;
                assertEquals("Number of members of group in search hit",
                        2, contactInfo.getContactGroupMembers().size());
            }
            ZimbraLog.test.info("Hit %s class=%s", hit, hit.getClass().getName());
        }
        assertTrue("Seen group", seenGroup);
    }

    @Test
    public void xBusyMacAttach() throws ServiceException, IOException {
        String contactsFolderUrl = TestCalDav.getFolderUrl(dav1, "Contacts");
        HttpClient client = HttpClientBuilder.create().build();

        HttpPost postMethod = new HttpPost(contactsFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(postMethod, dav1);
        postMethod.addHeader("Content-Type", "text/vcard");
        postMethod.setEntity(new ByteArrayEntity(smallBusyMacAttach.getBytes(),
            org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor exe = HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_CREATED);
        String location = exe.getNonNullHeaderValue("Location", "When creating VCARD");
        String url = String.format("%s%s", contactsFolderUrl, location.substring(location.lastIndexOf('/') + 1));
        HttpGet getMethod = new HttpGet(url);
        TestCalDav.addBasicAuthHeaderForUser(getMethod, dav1);
        getMethod.addHeader("Content-Type", "text/vcard");
        exe = HttpMethodExecutor.execute(client, getMethod, HttpStatus.SC_OK);
        String respBody = exe.getResponseAsString();
        String [] expecteds = {
            "\r\nX-BUSYMAC-ATTACH;X-FILENAME=favicon.ico;ENCODING=B:AAABAAEAEBAAAAEAIABoBA\r\n",
            "\r\n AAFgAAACgAAAAQAAAAIAAAAAEAIAAAAAAAQAQAABMLAAATCwAAAAAAAAAAAAAAAAAAw4cAY8\r\n",
            "\r\nX-BUSYMAC-MODIFIED-BY:Gren Elliot\r\n",
            "\r\nX-CUSTOM:one two three four five six seven eight nine ten eleven twelve t\r\n hirteen fourteen fifteen",
            "\r\nX-CUSTOM:Here are my simple\\Nmultiline\\Nnotes\r\n",
            "\r\nX-CUSTOM;TYPE=pref:semi-colon\\;seperated\\;\"stuff\"\\;here\r\n",
            "\r\nX-CUSTOM:comma\\,\"stuff\"\\,'there'\\,too\r\n",
            "\r\nX-HOBBY:my backslash\\\\ hobbies\r\n",
            "\r\nX-CREATED:2015-04-05T09:50:44Z\r\n" };
        for (String expected : expecteds) {
            assertTrue(String.format("GET should contain '%s'\nBODY=%s", expected, respBody),
                    respBody.contains(expected));
        }

        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy("dateDesc");
        searchRequest.setLimit(8);
        searchRequest.setSearchTypes("contact");
        searchRequest.setQuery("in:Contacts");
        ZMailbox mbox = TestUtil.getZMailbox(DAV1);
        SearchResponse searchResp = mbox.invokeJaxb(searchRequest);
        assertNotNull("JAXB SearchResponse object", searchResp);
        List<SearchHit> hits = searchResp.getSearchHits();
        assertNotNull("JAXB SearchResponse hits", hits);
        assertEquals("JAXB SearchResponse hits", 1, hits.size());
    }

    private String sharedContactFolderName() throws ServiceException {
        ZMailbox sharerZmbox = TestUtil.getZMailbox(DAV2);
        return String.format("%s's Contacts", sharerZmbox.getName());
    }

    private void shareContacts() throws ServiceException {
        ZMailbox sharerZmbox = TestUtil.getZMailbox(DAV2);
        ZMailbox shareeZmbox = TestUtil.getZMailbox(DAV1);
        TestUtil.createMountpoint(sharerZmbox, "Contacts", shareeZmbox, sharedContactFolderName());
    }

    @Test(timeout=100000)
    public void createInSharedAddressBook() throws ServiceException, IOException {
        Account dav2 = TestUtil.createAccount(DAV2);
        assertNotNull("Test account object", dav2);
        shareContacts();
        String contactsFolderUrl = TestCalDav.getFolderUrl(dav1, sharedContactFolderName());
        HttpClient client = HttpClientBuilder.create().build();

        HttpPost postMethod = new HttpPost(contactsFolderUrl);
        TestCalDav.addBasicAuthHeaderForUser(postMethod, dav1);
        postMethod.addHeader("Content-Type", "text/vcard");
        postMethod.setEntity(new ByteArrayEntity(parisVcard.getBytes(),
            org.apache.http.entity.ContentType.create(MimeConstants.CT_TEXT_VCARD)));
        HttpMethodExecutor exe = HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_CREATED);
        String location =
                exe.getNonNullHeaderValue("Location", "When creating VCARD in shared address book");
        String url = String.format("%s%s",contactsFolderUrl,
                location.substring(location.lastIndexOf('/') + 1));
        HttpGet getMethod = new HttpGet(url);
        TestCalDav.addBasicAuthHeaderForUser(getMethod, dav1);
        getMethod.addHeader("Content-Type", "text/vcard");
        exe = HttpMethodExecutor.execute(client, getMethod, HttpStatus.SC_OK);
    }

    @Test(timeout=100000)
    public void iosContactsPropfindABHome() throws ServiceException, IOException {
        Account dav2 = TestUtil.createAccount(DAV2);
        assertNotNull("Test account object", dav2);
        shareContacts();
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(TestCalDav.NamespaceContextForXPath.forCardDAV());
        Document propfindResponseDoc = doIosContactsPropOnAddressbookHomeSet(dav1);
        try {
            String mpResp = "/D:multistatus/D:response";
            String collectionXp = "D:propstat/D:prop/D:resourcetype/D:collection";
            String abXp = "D:propstat/D:prop/D:resourcetype/C:addressbook";
            String mpXp = "D:propstat/D:prop/D:resourcetype/Y:mountpoint";
            XPathExpression respNodesExpression = xpath.compile(mpResp);
            XPathExpression hrefTextExpression = xpath.compile("D:href/text()");
            XPathExpression collectionExpression = xpath.compile(collectionXp);
            XPathExpression abExpression = xpath.compile(abXp);
            XPathExpression mpExpression = xpath.compile(mpXp);
            NodeList responseNodes = (NodeList) respNodesExpression.evaluate(
                    propfindResponseDoc, XPathConstants.NODESET);
            assertNotNull("No response nodes found in multistatus response to PROPFIND", responseNodes);
            boolean seenShare = false;
            String expectedShareHref = UrlNamespace.getFolderUrl(dav1.getName(), sharedContactFolderName())
                    .replaceAll(" ", "%20").replaceAll("@", "%40");
            for (int ndx = 0; ndx < responseNodes.getLength(); ndx++) {
                org.w3c.dom.Element respNode = (org.w3c.dom.Element) responseNodes.item(ndx);
                String text = (String) hrefTextExpression.evaluate(respNode, XPathConstants.STRING);
                if (expectedShareHref.equals(text)) {
                    seenShare = true;
                    NodeList colNodes = (NodeList) collectionExpression.evaluate(
                            respNode, XPathConstants.NODESET);
                    assertNotNull(String.format("No %s/%s elements present for shared addressbook",
                            mpResp, collectionXp), colNodes);
                    assertEquals(String.format("Number of %s/%s elements present for shared addressbook",
                            mpResp, collectionXp), 1, colNodes.getLength());

                    NodeList abNodes = (NodeList) abExpression.evaluate(respNode, XPathConstants.NODESET);
                    assertNotNull(String.format("No %s/%s elements present for shared addressbook",
                            mpResp, abXp), abNodes);
                    assertEquals(String.format("Number of %s/%s elements present for shared addressbook",
                            mpResp, abXp), 1, abNodes.getLength());

                    NodeList mpNodes = (NodeList) mpExpression.evaluate(respNode, XPathConstants.NODESET);
                    assertNotNull(String.format("No %s/%s elements present for shared addressbook",
                            mpResp, mpXp), mpNodes);
                    assertEquals(String.format("Number of %s/%s elements present for shared addressbook",
                            mpResp, mpXp), 1, mpNodes.getLength());
                    break;
                }
            }
            assertTrue("Should have been response node in multistatus for shared addressbook", seenShare);
        } catch (XPathExpressionException e) {
            ZimbraLog.test.warn("xpath problem", e);
            fail("Problem with XPath expression");
        }
    }
}
