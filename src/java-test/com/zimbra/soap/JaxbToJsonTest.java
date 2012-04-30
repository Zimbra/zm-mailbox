/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.dom4j.QName;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.JaxbToElementTest;
import com.zimbra.soap.account.message.GetInfoResponse;
import com.zimbra.soap.admin.message.AuthResponse;
import com.zimbra.soap.admin.message.CreateXMbxSearchRequest;
import com.zimbra.soap.admin.message.VerifyIndexResponse;
import com.zimbra.soap.account.message.GetDistributionListMembersResponse;
import com.zimbra.soap.jaxb.EnumAttribEnumElem;
import com.zimbra.soap.jaxb.EnumAttribs;
import com.zimbra.soap.jaxb.EnumElemList;
import com.zimbra.soap.jaxb.KVPairs;
import com.zimbra.soap.jaxb.NamespaceDeltaElem;
import com.zimbra.soap.jaxb.StringAttrStringElem;
import com.zimbra.soap.jaxb.StringAttribIntValue;
import com.zimbra.soap.jaxb.UniqueTester;
import com.zimbra.soap.jaxb.ViewEnum;
import com.zimbra.soap.jaxb.WrappedEnumElemList;
import com.zimbra.soap.json.JacksonUtil;
import com.zimbra.soap.mail.message.DiffDocumentResponse;
import com.zimbra.soap.mail.message.GetFilterRulesResponse;
import com.zimbra.soap.mail.message.NoOpResponse;
import com.zimbra.soap.mail.type.AppointmentData;
import com.zimbra.soap.mail.type.CalendaringDataInterface;
import com.zimbra.soap.mail.type.DispositionAndText;
import com.zimbra.soap.mail.type.FilterAction;
import com.zimbra.soap.mail.type.FilterRule;
import com.zimbra.soap.mail.type.FilterTest;
import com.zimbra.soap.mail.type.FilterTests;
import com.zimbra.soap.mail.type.InstanceDataInfo;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.JSONElement;
import com.zimbra.common.soap.Element.XMLElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.XMbxSearchConstants;

public class JaxbToJsonTest {
    @Rule public TestName testName = new TestName();

    private static final Logger LOG = Logger.getLogger(JaxbToJsonTest.class);

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        LOG.setLevel(Level.INFO);
    }

    private void logInfo(String format, Object ... objects) {
        if (LOG.isInfoEnabled()) {
            LOG.info(testName.getMethodName() + ":" + String.format(format, objects));
        }
    }

    public static String streamToString(InputStream stream, Charset cs)
    throws IOException {
        try {
            Reader reader = new BufferedReader(
                    new InputStreamReader(stream, cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }

    @BeforeClass
    public static void init() throws Exception {
    }

    /**
     * the element referenced MailConstants.E_FRAG is treated in XML as an element with content but no attributes.
     * However in JSON, it is just treated like an ordinary attribute.
     * So, in JSON we DO want:
     *    "fr": "Here is some wonderful text and some more", 
     * We do NOT want it to be treated as if it was an element which would look like:
     *    "fr": [{
     *           "_content": "Here is some wonderful text and some more"
     *         }],
     */
    @Test
    public void bug61264_AttributeDispositionCONTENThandling() throws Exception {
        StringBuilder sb;
        final String uid = "uidString";
        final String frag = "Fragment text";
        final String name = "name Attribute";

        // From stacktrace of where the SOAP response gets assembled:
        //     at com.zimbra.common.soap.Element.output(Element.java:432)
        //     at com.zimbra.soap.SoapServlet.sendResponse(SoapServlet.java:349)
        //     at com.zimbra.soap.SoapServlet.doWork(SoapServlet.java:307)
        //     at com.zimbra.soap.SoapServlet.doPost(SoapServlet.java:206)

        // Bug 61264 is about issues with code that used JAXB which replaced code in GetCalendarItemSummaries
        // which started with an element created using:
        //     calItemElem = lc.createElement(isAppointment ? MailConstants.E_APPOINTMENT : MailConstants.E_TASK);
        // At present, that code has been reverted to be element based.

        // For comparison purposes, create an JSONElement tree and a XMLElement tree
        Element jsoncalItemElem = JSONElement.mFactory.createElement(MailConstants.E_APPOINTMENT);
        jsoncalItemElem.addAttribute(MailConstants.A_UID, uid);
        jsoncalItemElem.addAttribute("x_uid", uid);
        Element instElt = jsoncalItemElem.addElement(MailConstants.E_INSTANCE);
        instElt.addAttribute(MailConstants.E_FRAG, frag, Element.Disposition.CONTENT);
        instElt.addAttribute(MailConstants.A_CAL_IS_EXCEPTION, true);
        instElt.addAttribute(MailConstants.A_NAME, name);

        Element xmlcalItemElem = XMLElement.mFactory.createElement(MailConstants.E_APPOINTMENT);
        xmlcalItemElem.addAttribute(MailConstants.A_UID, uid);
        xmlcalItemElem.addAttribute("x_uid", uid);
        Element xmlinstElt = xmlcalItemElem.addElement(MailConstants.E_INSTANCE);
        xmlinstElt.addAttribute(MailConstants.E_FRAG, frag, Element.Disposition.CONTENT);
        xmlinstElt.addAttribute(MailConstants.A_CAL_IS_EXCEPTION, true);
        xmlinstElt.addAttribute(MailConstants.A_NAME, name);

        CalendaringDataInterface calData = null;
        calData = new AppointmentData(uid, uid);
        InstanceDataInfo instance = new InstanceDataInfo();
        calData.addCalendaringInstance(instance);
        instance.setIsException(true);
        instance.setName(name);
        instance.setFragment(frag);

        Element jsonJaxbElem = JaxbUtil.jaxbToNamedElement(MailConstants.E_APPOINTMENT,
                    MailConstants.NAMESPACE_STR,
                    calData, JSONElement.mFactory);

        Element xmlJaxbElem = JaxbUtil.jaxbToNamedElement(MailConstants.E_APPOINTMENT,
                    MailConstants.NAMESPACE_STR,
                    calData, XMLElement.mFactory);

        // As AppointmentData doesn't have an XmlRootElement, this gives a poor choice for the root name.
        //     Element jacksonJaxbElem = JacksonUtil.jaxbToJSONElement(calData);
        // This is probably a closer analog to JSONElement.mFactory.createElement(MailConstants.E_APPOINTMENT);
        //     Element jacksonJaxbElem = JacksonUtil.jaxbToJSONElement(calData, new QName("appt", null));
        Element jacksonJaxbElem = JacksonUtil.jaxbToJSONElement(calData,
                new QName(MailConstants.E_APPOINTMENT, MailConstants.NAMESPACE));

        Element parent4legacyJson = JSONElement.mFactory.createElement(new QName("legacy-json", MailConstants.NAMESPACE));
        Element parent4jackson = JSONElement.mFactory.createElement(new QName("jacksonjson", MailConstants.NAMESPACE));
        parent4legacyJson.addElement(jsoncalItemElem);
        parent4jackson.addElement(jacksonJaxbElem);

        sb = new StringBuilder();
        xmlcalItemElem.output(sb);
        logInfo("bug61264 - XML from XMLElement\n%1$s", sb.toString());

        sb = new StringBuilder();
        xmlJaxbElem.output(sb);
        logInfo("bug61264 - XML from JAXB\n%1$s", sb.toString());

        sb = new StringBuilder();  // something that is appendable for Element.out(Appendable) to play with
        jsoncalItemElem.output(sb);
        String jsonFromElement = sb.toString();
        logInfo("bug61264 - JSON from JSONElement\n%1$s", jsonFromElement);

        sb = new StringBuilder();
        jsonJaxbElem.output(sb);
        logInfo("bug61264 - JSON from JAXB\n%1$s", sb.toString());

        sb = new StringBuilder();
        jacksonJaxbElem.output(sb);
        logInfo("bug61264 - JSON from JAXB using Jackson\n%1$s", sb.toString());
        sb = new StringBuilder();
        parent4legacyJson.output(sb);
        logInfo("bug61264 - JSON from JAXB child using Jackson\n%1$s", sb.toString());
        sb = new StringBuilder();
        parent4jackson.output(sb);
        logInfo("bug61264 - JSON from JSONElement child\n%1$s", sb.toString());
        Assert.assertEquals("UID", uid, jacksonJaxbElem.getAttribute(MailConstants.A_UID));
        Assert.assertEquals("x_uid", uid, jacksonJaxbElem.getAttribute("x_uid"));
        Element instE = jacksonJaxbElem.getElement(MailConstants.E_INSTANCE);
        Assert.assertNotNull("instance elem", instE);
        Assert.assertEquals("fragment", frag, instE.getAttribute(MailConstants.E_FRAG));
        Assert.assertTrue("is exception", instE.getAttributeBool(MailConstants.A_CAL_IS_EXCEPTION));
        Assert.assertEquals("name", name, instE.getAttribute(MailConstants.A_NAME));
    }

    private void jacksonSerializeCheck(ObjectMapper mapper, String tag, Object obj)
    throws ServiceException {
        String json = JacksonUtil.jaxbToJsonString(mapper, obj);
        StringBuilder fullTag = new StringBuilder("JacksonPlay ")
            .append(obj.getClass().getName()).append(" ").append(tag).append(" ");
        logInfo(fullTag.toString() + 
                "JAXB --> Jackson --> String\n" + json);
        try {
            Element jsonElemFromJackson = JacksonUtil.jacksonJsonToElement(json, obj);
            logInfo(fullTag.toString() + 
                    "JAXB --> Jackson --> String ---> Element ---> prettyPrint\n" +
                    jsonElemFromJackson.prettyPrint());
        } catch (ServiceException e) {
            logInfo(fullTag.toString() + "\nProblem with Element.parseJSON");
            e.printStackTrace();
        }
        try {
            Element jsonE = JaxbUtil.jaxbToElement(obj, JSONElement.mFactory);
            logInfo(fullTag.toString() + 
                    "JAXB --> jaxbToElement --> Element ---> prettyPrint\n" +
                    jsonE.prettyPrint());
        } catch (ServiceException e) {
            logInfo(fullTag.toString() + "\nProblem with jaxbToElement");
            e.printStackTrace();
        }
        logInfo("===============================================");
    }

    @Test
    public void XmlElementAnnotation() throws Exception {
        final String msg = "ver ndx message";
        // ---------------------------------  For Comparison - Element handling
        Element legacyElem = JSONElement.mFactory.createElement(AdminConstants.VERIFY_INDEX_RESPONSE);
        legacyElem.addElement(AdminConstants.E_STATUS).addText(String.valueOf(true));
        legacyElem.addElement(AdminConstants.E_MESSAGE).addText(msg);
        logInfo("VerifyIndexResponse JSONElement ---> prettyPrint\n%1$s", legacyElem.prettyPrint());

        VerifyIndexResponse viResp = new VerifyIndexResponse(true, msg);
        Element elem = JacksonUtil.jaxbToJSONElement(viResp);
        Assert.assertEquals("status", true, elem.getAttributeBool(AdminConstants.E_STATUS));
        Assert.assertEquals("message", msg, elem.getAttribute(AdminConstants.E_MESSAGE));
        logInfo("VerifyIndexResponse JSONElement from JAXB ---> prettyPrint\n%1$s", elem.prettyPrint());
    }

    /**
     * XmlValue should map to an attribute with name "_content"
     * At present, classes that want this feature need the annotation @JsonProperty("_content"),
     * otherwise, the name "value" is used.
     *    "chunk": [ { "disp": "disposition 1", "_content": "text 1\nIn the sun" },
     *               { "disp": "disPosition 2", "_content": "text 2" }],
     */
    @Test
    public void XmlValueAnnotation() throws Exception {
        String dispos1 = "disposition 1";
        String text1 = "text 1\nIn the sun";
        String dispos2 = "disPosition 2";
        String text2 = "text 2";
        // ---------------------------------  For Comparison - Element handling where the JAXB has an @XmlValue
        Element legacyElem = JSONElement.mFactory.createElement(MailConstants.DIFF_DOCUMENT_RESPONSE);
        legacyElem.addElement(MailConstants.E_CHUNK).addAttribute(MailConstants.A_DISP, dispos1).setText(text1);
        legacyElem.addElement(MailConstants.E_CHUNK).addAttribute(MailConstants.A_DISP, dispos2).setText(text2);
        logInfo("DiffDocumentResponse JSONElement ---> prettyPrint\n%1$s", legacyElem.prettyPrint());
        // --------------------------------- @XmlValue handling test - need @JsonProperty("_content") annotation
        DiffDocumentResponse ddResp = new DiffDocumentResponse();
        ddResp.addChunk(DispositionAndText.create(dispos1, text1));
        ddResp.addChunk(DispositionAndText.create(dispos2, text2));
        Element elem = JacksonUtil.jaxbToJSONElement(ddResp);
        logInfo("DiffDocumentResponse JSONElement from JAXB ---> prettyPrint\n%1$s", elem.prettyPrint());
        List<Element> chunks = elem.listElements();
        Assert.assertEquals("Number of child elements", 2, chunks.size());
        Element chunk1 = chunks.get(0);
        Assert.assertEquals("1st chunk disposition", dispos1, chunk1.getAttribute(MailConstants.A_DISP));
        Assert.assertEquals("1st chunk value", text1, chunk1.getText());
        Element chunk2 = chunks.get(1);
        Assert.assertEquals("2nd chunk disposition", dispos2, chunk2.getAttribute(MailConstants.A_DISP));
        Assert.assertEquals("2nd chunk value", text2, chunk2.getText());
    }

    /**
     * Want JSON for KeyValuePairs to look something like :
     *   "_attrs": {
     *     "query": "Kitchen",
     *     "accounts": "*"
     *   },
     */
    @Test
    public void keyValuePair() throws Exception {
        final String notifMsg = "Search task %taskId% completed with status %status%. \nImported: %numMsgs% messages. \nSearch query used: %query%.";
        
        Element legacyElem = JSONElement.mFactory.createElement(XMbxSearchConstants.CREATE_XMBX_SEARCH_REQUEST);
        legacyElem.addKeyValuePair("query", "Kitchen");
        legacyElem.addKeyValuePair("accounts", "*");
        legacyElem.addKeyValuePair("limit", "0");
        legacyElem.addKeyValuePair("notificationMessage", notifMsg);
        logInfo("CreateXmbxSearchRequest JSONElement ---> prettyPrint\n%1$s", legacyElem.prettyPrint());
        // CreateXMbxSearchRequest extends AdminKeyValuePairs which uses a serializer to cope with KeyValuePairs
        //     @JsonSerialize(using=KeyAndValueListSerializer.class)
        //     @JsonProperty("_attrs")
        //     @XmlElement(name=AdminConstants.E_A)
        //     private List<KeyValuePair> keyValuePairs;
        CreateXMbxSearchRequest jaxb = new CreateXMbxSearchRequest();
        jaxb.addKeyValuePair(new KeyValuePair("query", "Kitchen"));
        jaxb.addKeyValuePair(new KeyValuePair("accounts", "*"));
        jaxb.addKeyValuePair(new KeyValuePair("limit", "0"));
        jaxb.addKeyValuePair(new KeyValuePair("notificationMessage", notifMsg));
        Element elem = JacksonUtil.jaxbToJSONElement(jaxb, XMbxSearchConstants.CREATE_XMBX_SEARCH_REQUEST);
        logInfo("CreateXmbxSearchRequest JSONElement from JAXB ---> prettyPrint\n%1$s", elem.prettyPrint());
        List<com.zimbra.common.soap.Element.KeyValuePair> kvps = elem.listKeyValuePairs();
        Assert.assertEquals("Number of keyValuePairs ", 4, kvps.size());
        com.zimbra.common.soap.Element.KeyValuePair kvp4 = kvps.get(3);
        Assert.assertEquals("KeyValuePair notificationMessage key", "notificationMessage", kvp4.getKey());
        Assert.assertEquals("KeyValuePair notificationMessage value", notifMsg, kvp4.getValue());
    }

    @Test
    public void contentList() throws Exception {
        Element legacyElem = JSONElement.mFactory.createElement(AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        legacyElem.addElement(AccountConstants.E_DLM).setText("dlmember1@no.where");
        legacyElem.addElement(AccountConstants.E_DLM).setText("dlmember2@no.where");
        legacyElem.addElement(AccountConstants.E_DLM).setText("dlmember3@no.where");
        legacyElem.addAttribute(AccountConstants.A_MORE, false);
        legacyElem.addAttribute(AccountConstants.A_TOTAL, 23);
        logInfo("GetDistributionListMembersResponse JSONElement ---> prettyPrint\n%1$s", legacyElem.prettyPrint());
        // GetDistributionListMembersResponse has:
        //      @XmlElement(name=AccountConstants.E_DLM, required=false)
        //      @JsonSerialize(using=ContentListSerializer.class)
        //      private List<String> dlMembers = Lists.newArrayList();
        GetDistributionListMembersResponse jaxb = new GetDistributionListMembersResponse();
        jaxb.setMore(false);
        jaxb.setTotal(23);
        jaxb.addDlMember("dlmember1@no.where");
        jaxb.addDlMember("dlmember2@no.where");
        jaxb.addDlMember("dlmember3@no.where");
        Element elem = JacksonUtil.jaxbToJSONElement(jaxb, AccountConstants.GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE);
        logInfo("GetDistributionListMembersResponse JSONElement from JAXB ---> prettyPrint\n%1$s", elem.prettyPrint());
        List<Element> dlMembers = elem.listElements(AccountConstants.E_DLM);
        Assert.assertEquals("Number of dlMembers", 3, dlMembers.size());
        Element dlMem3 = dlMembers.get(2);
        Assert.assertEquals("dlMember 3", "dlmember3@no.where", dlMem3.getText());
        Assert.assertEquals("total", 23, elem.getAttributeInt(AccountConstants.A_TOTAL));
        Assert.assertEquals("more", false, elem.getAttributeBool(AccountConstants.A_MORE));
    }

    /**
     * By default JAXB renders true and false in XML as "true" and "false"
     * For Zimbra SOAP the XML flavor uses "1" and "0"
     * The JSON flavor uses "true" and "false"
     * @throws Exception
     */
    @Test
    public void booleanMarshal() throws Exception {
        Element legacy0Elem = JSONElement.mFactory.createElement(MailConstants.NO_OP_RESPONSE);
        legacy0Elem.addAttribute(MailConstants.A_WAIT_DISALLOWED, false);
        logInfo("NoOpResponse JSONElement ---> prettyPrint\n%1$s", legacy0Elem.prettyPrint());
        Element legacy1Elem = JSONElement.mFactory.createElement(MailConstants.NO_OP_RESPONSE);
        legacy1Elem.addAttribute(MailConstants.A_WAIT_DISALLOWED, true);
        Element legacyFalseElem = JSONElement.mFactory.createElement(MailConstants.NO_OP_RESPONSE);
        legacyFalseElem.addAttribute(MailConstants.A_WAIT_DISALLOWED, "false");
        Element legacyTrueElem = JSONElement.mFactory.createElement(MailConstants.NO_OP_RESPONSE);
        legacyTrueElem.addAttribute(MailConstants.A_WAIT_DISALLOWED, "true");
        // NoOpResponse has:
        //     @XmlAttribute(name=MailConstants.A_WAIT_DISALLOWED, required=false)
        //     private ZmBoolean waitDisallowed;
        NoOpResponse jaxb = NoOpResponse.create(false);
        Element xmlElem = JaxbUtil.jaxbToElement(jaxb, Element.XMLElement.mFactory);
        logInfo("XMLElement from JAXB (false)---> prettyPrint\n%1$s", xmlElem.prettyPrint());
        Assert.assertEquals("false Value of 'waitDisallowed'",
                "0", xmlElem.getAttribute(MailConstants.A_WAIT_DISALLOWED));
        Element jsonElem = JacksonUtil.jaxbToJSONElement(jaxb, MailConstants.NO_OP_RESPONSE);
        logInfo("NoOpResponse JSONElement from JAXB (false)---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        Assert.assertEquals("false Value of 'waitDisallowed'",
                "false", jsonElem.getAttribute(MailConstants.A_WAIT_DISALLOWED));

        // ensure that marshaling where Boolean has value true works
        jaxb.setWaitDisallowed(true);
        xmlElem = JaxbUtil.jaxbToElement(jaxb, Element.XMLElement.mFactory);
        logInfo("XMLElement from JAXB (true) ---> prettyPrint\n%1$s", xmlElem.prettyPrint());
        Assert.assertEquals("true Value of 'waitDisallowed'",
                "1", xmlElem.getAttribute(MailConstants.A_WAIT_DISALLOWED));
        jsonElem = JacksonUtil.jaxbToJSONElement(jaxb, MailConstants.NO_OP_RESPONSE);
        logInfo("NoOpResponse JSONElement from JAXB (true) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        Assert.assertEquals("true Value of 'waitDisallowed'",
                "true", jsonElem.getAttribute(MailConstants.A_WAIT_DISALLOWED));
        // ensure that unmarshaling where XML Boolean representation is "1" for true works
        jaxb = JaxbUtil.elementToJaxb(legacy1Elem);
        Assert.assertEquals("legacy1Elem waitDisallowed value", Boolean.TRUE, jaxb.getWaitDisallowed());
        // ensure that unmarshaling where XML Boolean representation is "0" for false works
        jaxb = JaxbUtil.elementToJaxb(legacy0Elem);
        Assert.assertEquals("legacy0Elem waitDisallowed value", Boolean.FALSE, jaxb.getWaitDisallowed());
        // ensure that unmarshaling where XML Boolean representation is "false" works
        jaxb = JaxbUtil.elementToJaxb(legacyFalseElem);
        Assert.assertEquals("legacyFalseElem waitDisallowed value", Boolean.FALSE, jaxb.getWaitDisallowed());
        // ensure that unmarshaling where XML Boolean representation is "true" works
        jaxb = JaxbUtil.elementToJaxb(legacyTrueElem);
        Assert.assertEquals("legacyTrueElem waitDisallowed value", Boolean.TRUE, jaxb.getWaitDisallowed());
    }

    /*
     * 
<GetFilterRulesResponse xmlns="urn:zimbraMail">
  <filterRules>
    <filterRule name="filter1.1318830338466.3" active="0">
      <filterTests condition="anyof">
        <headerTest index="0" value="0" stringComparison="contains"
header="X-Spam-Score"/>
      </filterTests>
      <filterActions>
        <actionFlag index="0" flagName="flagged"/>
        <actionStop index="1"/>
      </filterActions>
    </filterRule>
  </filterRules>
</GetFilterRulesResponse>
     */
    private Element mkFilterRulesResponse(Element.ElementFactory factory) {
        Element legacyElem = factory.createElement(MailConstants.GET_FILTER_RULES_RESPONSE);
        Element filterRulesE = legacyElem.addElement(MailConstants.E_FILTER_RULES);
        Element filterRuleE = filterRulesE.addElement(MailConstants.E_FILTER_RULE);
        filterRuleE.addAttribute(MailConstants.A_NAME, "filter.bug65572");
        filterRuleE.addAttribute(MailConstants.A_ACTIVE, false);
        Element filterTestsE = filterRuleE.addElement(MailConstants.E_FILTER_TESTS);
        filterTestsE.addAttribute(MailConstants.A_CONDITION, "anyof");
        Element hdrTestE = filterTestsE.addElement(MailConstants.E_HEADER_TEST);
        hdrTestE.addAttribute(MailConstants.A_INDEX, 0);
        hdrTestE.addAttribute(MailConstants.A_HEADER, "X-Spam-Score");
        hdrTestE.addAttribute(MailConstants.A_CASE_SENSITIVE, false);  /* not actually in above test */
        hdrTestE.addAttribute(MailConstants.A_STRING_COMPARISON, "contains");
        hdrTestE.addAttribute(MailConstants.A_VALUE, "0");
        Element filterActionsE = filterRuleE.addElement(MailConstants.E_FILTER_ACTIONS);
        Element actionFlagE = filterActionsE.addElement(MailConstants.E_ACTION_FLAG);
        actionFlagE.addAttribute(MailConstants.A_FLAG_NAME, "flagged");
        actionFlagE.addAttribute(MailConstants.A_INDEX, 0);
        Element actionStopE = filterActionsE.addElement(MailConstants.E_ACTION_STOP);
        actionStopE.addAttribute(MailConstants.A_INDEX, 1);
        return legacyElem;
    }

    @Test
    public void bug65572_BooleanAndXmlElements() throws Exception {
        Element legacyXmlElem = mkFilterRulesResponse(XMLElement.mFactory);
        Element legacyJsonElem = mkFilterRulesResponse(JSONElement.mFactory);

        GetFilterRulesResponse jaxb = new GetFilterRulesResponse();
        FilterTests tests = FilterTests.createForCondition("anyof");
        FilterTest.HeaderTest hdrTest = FilterTest.HeaderTest.createForIndexNegative(0, null);
        hdrTest.setHeaders("X-Spam-Score");
        hdrTest.setCaseSensitive(false);
        hdrTest.setStringComparison("contains");
        hdrTest.setValue("0");
        tests.addTest(hdrTest);
        FilterAction.FlagAction flagAction = new FilterAction.FlagAction("flagged");
        flagAction.setIndex(0);
        FilterAction.StopAction stopAction = new FilterAction.StopAction();
        stopAction.setIndex(1);
        FilterRule rule1 = FilterRule.createForNameFilterTestsAndActiveSetting("filter.bug65572", tests, false);
        rule1.addFilterAction(flagAction);
        rule1.addFilterAction(stopAction);
        jaxb.addFilterRule(rule1);
        Element xmlElem = JaxbUtil.jaxbToElement(jaxb, Element.XMLElement.mFactory);
        logInfo("legacyXMLElement ---> prettyPrint\n%1$s", legacyXmlElem.prettyPrint());
        logInfo("XMLElement from JAXB ---> prettyPrint\n%1$s", xmlElem.prettyPrint());
        Assert.assertEquals("XML", legacyXmlElem.prettyPrint(), xmlElem.prettyPrint());
        Element jsonElem = JacksonUtil.jaxbToJSONElement(jaxb, MailConstants.GET_FILTER_RULES_RESPONSE);
        logInfo("GetFilterRulesResponse legacyJSONElement ---> prettyPrint\n%1$s", legacyJsonElem.prettyPrint());
        logInfo("GetFilterRulesResponse JSONElement from JAXB ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        Assert.assertEquals("JSON", legacyJsonElem.prettyPrint(), jsonElem.prettyPrint());
    }

    /**
     * If you just use a pair of annotation introspectors (JacksonAnnotationIntrospector/JaxbAnnotationIntrospector)
     * then "XmlEnumValue"'s are ignored - AnnotationIntrospector.Pair's findEnumValue(Enum<?> e) method won't
     * call the secondary's findEnumValue unless primary's findEnumValue returns null.
     * (if we made JaxbAnnotationIntrospector the primary, this would work but other things wouldn't)
     * To fix this, the current code makes use of ZmPairAnnotationIntrospector which overrides findEnumValue
     * Desired JSON :
     *     {
     *       "fold1": "virtual conversation",
     *       "fold2": [{
     *           "_content": ""
     *         }],
     *       "_jsns": "urn:zimbraTest"
     *     }
     */
    @Test
    public void xmlEnumValuesInAttrAndElem() throws Exception {
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("enum-tester", "urn:zimbraTest"));
        jsonElem.addAttribute("fold1", ViewEnum.VIRTUAL_CONVERSATION.toString());
        jsonElem.addElement("fold2").addText(ViewEnum.UNKNOWN.toString());
        EnumAttribEnumElem tstr = new EnumAttribEnumElem();
        tstr.setFold1(ViewEnum.VIRTUAL_CONVERSATION);
        tstr.setFold2(ViewEnum.UNKNOWN);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr, QName.get("enum-tester", "urn:zimbraTest"));
        EnumAttribEnumElem roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, EnumAttribEnumElem.class);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        // logInfo("non-Zimbra Jackson from JAXB ---> prettyPrint\n%1$s", getSimpleJsonJaxbString(tstr));
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        // Want 'virtual conversation' not 'VIRTUAL_CONVERSATION'
        Assert.assertEquals("fold1 value", ViewEnum.VIRTUAL_CONVERSATION.toString(), jsonJaxbElem.getAttribute("fold1"));
        // Want '' not 'UNKNOWN'
        Assert.assertEquals("fold2 value", ViewEnum.UNKNOWN.toString(), jsonJaxbElem.getElement("fold2").getText());
        Assert.assertEquals("roundtripped fold1", ViewEnum.VIRTUAL_CONVERSATION, roundtripped.getFold1());
        Assert.assertEquals("roundtripped fold2", ViewEnum.UNKNOWN, roundtripped.getFold2());
    }

    /**
     * If you just use a pair of annotation introspectors (JacksonAnnotationIntrospector/JaxbAnnotationIntrospector)
     * then "XmlEnumValue"'s are ignored - AnnotationIntrospector.Pair's findEnumValue(Enum<?> e) method won't
     * call the secondary's findEnumValue unless primary's findEnumValue returns null.
     * (if we made JaxbAnnotationIntrospector the primary, this would work but other things wouldn't)
     * To fix this, the current code makes use of ZmPairAnnotationIntrospector which overrides findEnumValue
     * Desired JSON :
     *     {
     *       "fold1": "virtual conversation",
     *       "fold2": "",
     *       "_jsns": "urn:zimbraTest"
     *     }
     */
    @Test
    public void xmlEnumValuesInAttributes() throws Exception {
        EnumAttribs tstr = new EnumAttribs();
        tstr.setFold1(ViewEnum.VIRTUAL_CONVERSATION);
        tstr.setFold2(ViewEnum.UNKNOWN);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr, QName.get("enum-tester", "urn:zimbraTest"));
        EnumAttribs roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, EnumAttribs.class);
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        // Want 'virtual conversation' not 'VIRTUAL_CONVERSATION'
        Assert.assertEquals("fold1 value", ViewEnum.VIRTUAL_CONVERSATION.toString(), jsonJaxbElem.getAttribute("fold1"));
        // Want '' not 'UNKNOWN'
        Assert.assertEquals("fold2 value", ViewEnum.UNKNOWN.toString(), jsonJaxbElem.getAttribute("fold2"));
        Assert.assertEquals("roundtripped fold1", ViewEnum.VIRTUAL_CONVERSATION, roundtripped.getFold1());
        Assert.assertEquals("roundtripped fold2", ViewEnum.UNKNOWN, roundtripped.getFold2());
    }

    /**
     * Testing String attribute and String element.
     * Also testing differing namespaces on package, root element and field element
     * Desired JSON :
     *      {
     *        "attribute-1": "My attribute ONE",
     *        "element1": [{
     *            "_content": "My element ONE",
     *            "_jsns": "urn:ZimbraTest3"
     *          }],
     *        "_jsns": "urn:ZimbraTest2"
     *      }
     */
    @Test
    public void stringAttrAndElem() throws Exception {
        final String attr1Val = "My attribute ONE";
        final String elem1Val = "My element ONE";
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("string-tester", "urn:zimbraTest2"));
        jsonElem.addAttribute("attribute-1", attr1Val);
        jsonElem.addElement(QName.get("element1", "urn:zimbraTest3")).addText(elem1Val);
        StringAttrStringElem tstr = new StringAttrStringElem();
        tstr.setAttr1(attr1Val);
        tstr.setElem1(elem1Val);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        Element xmlElem = JaxbUtil.jaxbToElement(tstr, Element.XMLElement.mFactory, true, false);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        logInfo("XmlElement (for comparison) ---> prettyPrint\n%1$s", xmlElem.prettyPrint());
        StringAttrStringElem roundtrippedX = JaxbUtil.elementToJaxb(xmlElem, StringAttrStringElem.class);
        StringAttrStringElem roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, StringAttrStringElem.class);
        Assert.assertEquals("JSONElement attr1", attr1Val, jsonJaxbElem.getAttribute("attribute-1"));
        Assert.assertEquals("JSONElement elem1", elem1Val, jsonJaxbElem.getElement("element1").getText());
        Assert.assertEquals("roundtrippedX attr1", attr1Val, roundtrippedX.getAttr1());
        Assert.assertEquals("roundtrippedX elem1", elem1Val, roundtrippedX.getElem1());
        Assert.assertEquals("roundtripped attr1", attr1Val, roundtripped.getAttr1());
        Assert.assertEquals("roundtripped elem1", elem1Val, roundtripped.getElem1());
    }

    @Test
    public void elemsInDiffNamespace() throws Exception {
        final String attr1Val = "My attribute ONE";
        final String elem1Val = "My element ONE";
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("ns-delta", "urn:zimbraTest4"));
        Element saseElem = jsonElem.addElement(QName.get("strAttrStrElem", "urn:zimbraTest5"));
        saseElem.addAttribute("attribute-1", attr1Val);
        saseElem.addElement(QName.get("element1", "urn:zimbraTest3")).addText(elem1Val);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        NamespaceDeltaElem tstr = new NamespaceDeltaElem();
        StringAttrStringElem tstrSase = new StringAttrStringElem();
        tstrSase.setAttr1(attr1Val);
        tstrSase.setElem1(elem1Val);
        tstr.setSase(tstrSase);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        NamespaceDeltaElem roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, NamespaceDeltaElem.class);
        Element saseJsonJaxbElem = jsonJaxbElem.getElement(QName.get("strAttrStrElem", "urn:zimbraTest5"));
        Assert.assertEquals("JSONElement attr1", attr1Val, saseJsonJaxbElem.getAttribute("attribute-1"));
        Assert.assertEquals("JSONElement elem1", elem1Val, saseJsonJaxbElem.getElement("element1").getText());
        logInfo("roundtripped attr1=%1$s", roundtripped.getSase().getAttr1());
        logInfo("roundtripped elem1=%1$s", roundtripped.getSase().getElem1());
        Assert.assertEquals("roundtripped attr1", attr1Val, roundtripped.getSase().getAttr1());
        Assert.assertEquals("roundtripped elem1", elem1Val, roundtripped.getSase().getElem1());
    }

    /**
     * Desired JSON :
     *      {
     *        "authToken": [{
     *            "_content": "authenticationtoken"
     *          }],
     *        "lifetime": [{
     *            "_content": 3000
     *          }],
     *        "_jsns": "urn:zimbraAdmin"
     *      }
     */
    @Test
    public void stringElemAndlongElem() throws Exception {
        final long lifetime = 3000;
        final String lifetimeStr = new Long(lifetime).toString();
        final String authToken = "authenticationtoken";
        AuthResponse tstr = new AuthResponse();
        tstr.setAuthToken(authToken);
        tstr.setLifetime(lifetime);
        Element xmlElem = JaxbUtil.jaxbToElement(tstr, Element.XMLElement.mFactory, true, false);
        AuthResponse roundtrippedX = JaxbUtil.elementToJaxb(xmlElem, AuthResponse.class);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        AuthResponse roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, AuthResponse.class);
        logInfo("XmlElement (for comparison) ---> prettyPrint\n%1$s", xmlElem.prettyPrint());
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        Assert.assertEquals("JSONElement lifetime", lifetimeStr, jsonJaxbElem.getElement("lifetime").getText());
        Assert.assertEquals("JSONElement authToken", authToken, jsonJaxbElem.getElement("authToken").getText());
        Assert.assertEquals("roundtripped lifetime", lifetime, roundtripped.getLifetime());
        Assert.assertEquals("roundtripped authToken", authToken, roundtripped.getAuthToken());
        Assert.assertEquals("roundtrippedX lifetime", lifetime, roundtrippedX.getLifetime());
        Assert.assertEquals("roundtrippedX authToken", authToken, roundtrippedX.getAuthToken());
    }

    /**
     * Desired JSON :
     *      {
     *        "attr1": "Attribute ONE",
     *        "_content": "3",
     *        "_jsns": "urn:zimbraTest"
     *      }
     */
    @Test
    public void stringAttrintValue() throws Exception {
        final String attrib1 = "Attribute ONE";
        final int value = 3;
        final String valueStr = new Integer(value).toString();
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("string-attr-int-value", "urn:zimbraTest"));
        jsonElem.addAttribute("attr1", attrib1);
        jsonElem.addText(valueStr);
        StringAttribIntValue tstr = new StringAttribIntValue(attrib1, value);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        StringAttribIntValue roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, StringAttribIntValue.class);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        Assert.assertEquals("attr1", attrib1, jsonJaxbElem.getAttribute("attr1"));
        Assert.assertEquals("XmlValue", valueStr, jsonJaxbElem.getText());
        Assert.assertEquals("roundtripped attrib1", attrib1, roundtripped.getAttrib1());
        Assert.assertEquals("roundtripped value", value, roundtripped.getMyValue());
    }

    /**
     * A JSON element added via {@code addElement} is always serialized as an array, because there could be
     * further {@code addElement} calls with the same element name.  On the other hand, a JSON element added via
     * {@code addUniqueElement} won't be serialized as an array as their is an implicit assumption that there will
     * be only one element with that name.
     * Desired JSON :
     * {
     *   "unique-str-elem": {
     *      "_content": "Unique element ONE",
     *      "_jsns": "urn:zimbraTest1"
     *    },
     *    "non-unique-elem": [{
     *        "_content": "Unique element ONE",
     *        "_jsns": "urn:zimbraTest1"
     *      }],
     *    "unique-complex-elem": {
     *      "attr1": "Attribute ONE",
     *      "_content": "3"
     *    },
     *    "_jsns": "urn:zimbraTest"
     * }
     */
    @Test
    public void uniqeElementHandling() throws Exception {
        final String elem1 = "Unique element ONE";
        final String attrib1 = "Attribute ONE";
        final int value = 3;
        final String valueStr = new Integer(value).toString();
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("unique-tester", "urn:zimbraTest"));
        jsonElem.addUniqueElement(QName.get("unique-str-elem", "urn:zimbraTest1")).setText(elem1);
        jsonElem.addElement(QName.get("non-unique-elem", "urn:zimbraTest1")).setText(elem1);
        jsonElem.addUniqueElement("unique-complex-elem").addAttribute("attr1", attrib1).setText(valueStr);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        UniqueTester tstr = new UniqueTester();
        tstr.setUniqueStrElem(elem1);
        tstr.setNonUniqueStrElem(elem1);
        StringAttribIntValue complex = new StringAttribIntValue(attrib1, value);
        tstr.setUniqueComplexElem(complex);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        UniqueTester roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, UniqueTester.class);
        Assert.assertEquals("roundtripped non-unique elem", elem1, roundtripped.getNonUniqueStrElem());
        Assert.assertEquals("roundtripped unique elem", elem1, roundtripped.getUniqueStrElem());
        StringAttribIntValue roundtrippedComplex = tstr.getUniqueComplexElem();
        Assert.assertEquals("roundtripped complex attrib1", attrib1, roundtrippedComplex.getAttrib1());
        Assert.assertEquals("roundtripped complex value", value, roundtrippedComplex.getMyValue());
    }

    /**
     * Desired JSON :
     *      {
     *        "_attrs": {
     *          "key1": "value1",
     *          "key2": "value2"
     *        },
     *        "_jsns": "urn:zimbraTest"
     *      }
     */
    @Test
    public void keyValuePairs() throws Exception {
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("key-value-pairs", "urn:zimbraTest"));
        jsonElem.addKeyValuePair("key1", "value1");
        jsonElem.addKeyValuePair("key2", "value2");
        KVPairs kvPairs = new KVPairs();
        kvPairs.addKeyValuePair(new KeyValuePair("key1", "value1"));
        kvPairs.addKeyValuePair(new KeyValuePair("key2", "value2"));
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(kvPairs);
        KVPairs roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, KVPairs.class);
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        List<com.zimbra.common.soap.Element.KeyValuePair> elemKVPs = jsonJaxbElem.listKeyValuePairs();
        Assert.assertEquals("elemKVP num", 2, elemKVPs.size());
        List<KeyValuePair> kvps = roundtripped.getKeyValuePairs();
        Assert.assertEquals("roundtripped kvps num", 2, kvps.size());
    }

    /**
     *  Testing form:
     *      {@code @XmlElement(name="enum-entry", required=false)
     *      private List<ViewEnum> entries = Lists.newArrayList();}
     * Desired JSON :
     *      {
     *        "enum-entry": [
     *          {
     *            "_content": "appointment"
     *          },
     *          {
     *            "_content": ""
     *          },
     *          {
     *            "_content": "document"
     *          }],
     *        "_jsns": "urn:zimbraTest"
     */
    @Test
    public void enumElemList() throws Exception {
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("enum-elem-list", "urn:zimbraTest"));
        jsonElem.addElement("enum-entry").addText(ViewEnum.APPOINTMENT.toString());
        jsonElem.addElement("enum-entry").addText(ViewEnum.UNKNOWN.toString());
        jsonElem.addElement("enum-entry").addText(ViewEnum.DOCUMENT.toString());
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        EnumElemList tstr = new EnumElemList();
        tstr.addEntry(ViewEnum.APPOINTMENT);
        tstr.addEntry(ViewEnum.UNKNOWN);
        tstr.addEntry(ViewEnum.DOCUMENT);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        EnumElemList roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, EnumElemList.class);
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        List<Element> jsonElems = jsonJaxbElem.listElements();
        List<ViewEnum> entries = roundtripped.getEntries();
        Assert.assertEquals("jsonElems num", 3, jsonElems.size());
        Assert.assertEquals("entries num", 3, entries.size());
        Assert.assertTrue("has APPOINTMENT", entries.contains(ViewEnum.APPOINTMENT));
        Assert.assertTrue("has UNKNOWN", entries.contains(ViewEnum.UNKNOWN));
        Assert.assertTrue("has DOCUMENT", entries.contains(ViewEnum.DOCUMENT));
    }

    @Test
    public void emptyEnumElemList() throws Exception {
        EnumElemList tstr = new EnumElemList();
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        logInfo("JSONElement from JAXB FOR EMPTY LIST ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        EnumElemList roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, EnumElemList.class);
        List<Element> jsonElems = jsonJaxbElem.listElements();
        List<ViewEnum> entries = roundtripped.getEntries();
        Assert.assertEquals("jsonElems num", 0, jsonElems.size());
        Assert.assertEquals("entries num", 0, entries.size());
    }

    /**
     *  Testing form:
     *      {@code @XmlElementWrapper(name="wrapper", required=false)
     *      @XmlElement(name="enum-entry", required=false)
     *      private List<ViewEnum> entries = Lists.newArrayList();}
     * Desired JSON :
     *      {
     *        "wrapper": [{
     *            "enum-entry": [
     *              {
     *                "_content": "appointment"
     *              },
     *              {
     *                "_content": "document"
     *              }]
     *          }],
     *        "_jsns": "urn:zimbraTest"
     *      }
     */
    @Test
    public void wrappedEnumElemList() throws Exception {
        Element jsonElem = JSONElement.mFactory.createElement(QName.get("wrapped-enum-elem-list", "urn:zimbraTest"));
        Element wrapElem = jsonElem.addElement("wrapper");
        wrapElem.addElement("enum-entry").addText(ViewEnum.APPOINTMENT.toString());
        wrapElem.addElement("enum-entry").addText(ViewEnum.DOCUMENT.toString());
        logInfo("JSONElement (for comparison) ---> prettyPrint\n%1$s", jsonElem.prettyPrint());
        WrappedEnumElemList tstr = new WrappedEnumElemList();
        tstr.addEntry(ViewEnum.APPOINTMENT);
        tstr.addEntry(ViewEnum.DOCUMENT);
        Element jsonJaxbElem = JacksonUtil.jaxbToJSONElement(tstr);
        WrappedEnumElemList roundtripped = JaxbUtil.elementToJaxb(jsonJaxbElem, WrappedEnumElemList.class);
        logInfo("JSONElement from JAXB ---> prettyPrint\n%1$s", jsonJaxbElem.prettyPrint());
        Element wElem = jsonJaxbElem.getElement("wrapper");
        List<Element> jsonElems = wElem.listElements();
        List<ViewEnum> entries = roundtripped.getEntries();
        Assert.assertEquals("jsonElems num", 2, jsonElems.size());
        Assert.assertEquals("entries num", 2, entries.size());
        Assert.assertTrue("has APPOINTMENT", entries.contains(ViewEnum.APPOINTMENT));
        Assert.assertTrue("has DOCUMENT", entries.contains(ViewEnum.DOCUMENT));
    }

    // Used for experiments
    // @Test
    public void JacksonPlay() throws Exception {
        ObjectMapper mapper = JacksonUtil.getWrapRootObjectMapper();
        // ---------------------------------
        // rather than getting a StringWriter or a string, use JsonNode which is a bit of an analog to Element?
        // JsonNode foobar = mapper.valueToTree(resp);
        // Stolen from main in "Element"
        logInfo("Getting attribute 'a' \n" + Element.parseJSON("{ '_attrs' : {'a':'b'}}").getAttribute("a", null));
        logInfo("Round tripped _attrs using Element\n" +
            Element.parseJSON("{ '_attrs' : {'a':'b','c':'d'},'_jsns':'urn:zimbraAdmin'}").prettyPrint());
        GetInfoResponse giResp = (GetInfoResponse) JaxbUtil.elementToJaxb(
                        Element.parseXML(JaxbToElementTest.getTestInfoResponseXml()));
        jacksonSerializeCheck(mapper, "GetInfoResponse TEST", giResp);
    }

    public String getSimpleJsonJaxbString(Object obj) {
            try {
                return JacksonUtil.jaxbToJsonString(getSimpleJsonJaxbMapper(), obj);
            } catch (ServiceException e) {
                e.printStackTrace();
                return null;
            }
    }

    public ObjectMapper getSimpleJsonJaxbMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(
                AnnotationIntrospector.pair(new JacksonAnnotationIntrospector(), new JaxbAnnotationIntrospector()));
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        return mapper;
    }

}
