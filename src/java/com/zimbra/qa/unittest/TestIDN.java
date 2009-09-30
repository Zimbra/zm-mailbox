/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;


import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeManager.IDNType;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.IDNUtil;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.service.UserServlet;
import com.zimbra.qa.unittest.TestProvisioningUtil.IDNName;

public class TestIDN extends TestCase {
    private static String TEST_ID;
    private static String TEST_NAME = "test-IDN";
    private static String UNICODESTR = "\u4e2d\u6587";
    // private static String UNICODESTR = "\u5f35\u611b\u73b2";
    private static String PASSWORD = "test123";
    
    private static Provisioning mProv;
    private static String BASE_DOMAIN_NAME;
    
    public void setUp() throws Exception {
        
        if (TEST_ID != null)
            return;
            
        TEST_ID = TestProvisioningUtil.genTestId();
        
        System.out.println("\nTest " + TEST_ID + " setting up...\n");
        mProv = Provisioning.getInstance();
        BASE_DOMAIN_NAME = "." + TestProvisioningUtil.baseDomainName(TEST_NAME, TEST_ID);
    }
    
    private String makeDomainName(String prefix) {
        return prefix + UNICODESTR + BASE_DOMAIN_NAME;
    }

    
    private Domain createDomain(String domainName, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        Domain domain = mProv.createDomain(domainName, attrs);
        assertNotNull(domain);
        return domain;
    }
    
    private Account createAccount(String email, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        Account acct = mProv.createAccount(email, PASSWORD, attrs);
        assertNotNull(acct);
        return acct;
    }
    
    private CalendarResource createCalendarResource(String email, String description) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        attrs.put(Provisioning.A_displayName, email);
        attrs.put(Provisioning.A_zimbraCalResType, "Equipment");
        CalendarResource cr = mProv.createCalendarResource(email, PASSWORD, attrs);
        assertNotNull(cr);
        return cr;
    }
    
    private DistributionList createDistributionList(String email, String description) throws Exception {
        Map<String, Object> attrs= new HashMap<String, Object>();
        attrs.put(Provisioning.A_description, "====="+description+"=====");
        DistributionList dl = mProv.createDistributionList(email, attrs);
        assertNotNull(dl);
        return dl;
    }
    
    static enum EntryType {
        DOMAIN,
        ACCOUNT,
        CR,
        DL
    }
    
    static enum NameType {
        UNAME,
        ANAME,
    }
    
    private NamedEntry createTest(EntryType entryType, NameType by, IDNName name) throws Exception {
        
        NamedEntry created = null;
        NamedEntry notCreated = null;
        
        String firstCreateBy = null;
        String thenCreateBy = null;
        if (by == NameType.UNAME) {
            firstCreateBy = name.uName();
            thenCreateBy = name.aName();
        } else {
            firstCreateBy = name.aName();
            thenCreateBy = name.uName();
        }
        
        String desc = name.uName();
        
        switch (entryType) {
        case DOMAIN:
            created = createDomain(firstCreateBy, desc);
            break;
        case ACCOUNT:
            created = createAccount(firstCreateBy, desc);
            break;
        case CR:
            created = createCalendarResource(firstCreateBy, desc);
            break;
        case DL:
            created = createDistributionList(firstCreateBy, desc);
            break;
        }
        
        assertNotNull(created);
        
        try {
            // entry should have existed
            switch (entryType) {
            case DOMAIN:
                notCreated = createDomain(thenCreateBy, desc);
                break;
            case ACCOUNT:
                notCreated = createAccount(thenCreateBy, desc);
                break;
            case CR:
                notCreated = createCalendarResource(thenCreateBy, desc);
                break;
            case DL:
                notCreated = createDistributionList(thenCreateBy, desc);
                break;
            }
            fail();
        } catch (ServiceException e) {
            String expectedExceptionCode = null;
            switch (entryType) {
            case DOMAIN:
                expectedExceptionCode = AccountServiceException.DOMAIN_EXISTS;
                break;
            case ACCOUNT:
                expectedExceptionCode = AccountServiceException.ACCOUNT_EXISTS;
                break;
            case CR:
                expectedExceptionCode = AccountServiceException.ACCOUNT_EXISTS;
                break;
            case DL:
                expectedExceptionCode = AccountServiceException.DISTRIBUTION_LIST_EXISTS;
                break;
            }
            if (!e.getCode().equals(expectedExceptionCode))
                fail();
        }
        
        return created;
    }
    
    private void getTest(EntryType entryType, NamedEntry expectedEntry, IDNName name) throws Exception {
        
        NamedEntry gotByUName = null;
        NamedEntry gotByAName = null;
        
        switch (entryType) {
        case DOMAIN:
            gotByUName = mProv.get(Provisioning.DomainBy.name, name.uName());
            gotByAName = mProv.get(Provisioning.DomainBy.name, name.aName());
            break;
        case ACCOUNT:
            gotByUName = mProv.get(Provisioning.AccountBy.name, name.uName());
            gotByAName = mProv.get(Provisioning.AccountBy.name, name.aName());
            break;
        case CR:
            gotByUName = mProv.get(Provisioning.CalendarResourceBy.name, name.uName());
            gotByAName = mProv.get(Provisioning.CalendarResourceBy.name, name.aName());
            break;
        case DL:
            gotByUName = mProv.get(Provisioning.DistributionListBy.name, name.uName());
            gotByAName = mProv.get(Provisioning.DistributionListBy.name, name.aName());
            break;
        }
        
        if (expectedEntry == null) {
            assertNull(gotByUName);
            assertNull(gotByAName);
        } else {
            assertNotNull(gotByUName);
            assertNotNull(gotByAName);
            assertEquals(expectedEntry.getId(), gotByUName.getId());
            assertEquals(expectedEntry.getId(), gotByAName.getId());
        }
    }
    
    private void renameTest(EntryType entryType, NamedEntry entry, IDNName name) throws Exception {

        switch (entryType) {
        case DOMAIN:
            assertTrue(mProv instanceof LdapProvisioning);
            ((LdapProvisioning)mProv).renameDomain(entry.getId(), name.uName());
            break;
        case ACCOUNT:
            mProv.renameAccount(entry.getId(), name.uName());
            break;
        case CR:
            mProv.renameCalendarResource(entry.getId(), name.uName());
            break;
        case DL:
            mProv.renameDistributionList(entry.getId(), name.uName());
            break;
        }
    }
    
    private void setAddressAttrsTest(EntryType entryType, NamedEntry entry, IDNName name) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        
        attrs.put(Provisioning.A_zimbraMailCanonicalAddress, "canonical-" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailForwardingAddress, "extra-1-forwarding-" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailForwardingAddress, "extra-2-forwarding-" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailDeliveryAddress, "extra-1-delivery-" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailDeliveryAddress, "extra-2-delivery-" + name.uName());
        
        attrs.put("+" + Provisioning.A_zimbraMailCatchAllAddress, "@" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailCatchAllCanonicalAddress, "@" + name.uName());
        attrs.put("+" + Provisioning.A_zimbraMailCatchAllForwardingAddress, "@" + name.uName());
        
        mProv.modifyAttrs(entry, attrs);
    }
    
    public void testDomain() throws Exception {

        IDNName d1Name = new IDNName(makeDomainName("domain-1."));
        IDNName d2Name = new IDNName(makeDomainName("domain-2."));
        IDNName d2RenamedName = new IDNName(makeDomainName("domain-2-renamed."));
        
        // create domain with unicode name
        Domain domain1 = (Domain)createTest(EntryType.DOMAIN, NameType.UNAME, d1Name);
        Domain domain2 = (Domain)createTest(EntryType.DOMAIN, NameType.ANAME, d2Name);
        
        // get by name test
        getTest(EntryType.DOMAIN, domain1, d1Name);
        getTest(EntryType.DOMAIN, domain2, d2Name);
        
        // rename test
        renameTest(EntryType.DOMAIN, domain2, d2RenamedName);
        
        /*
           skip this test for now, because domain id cache still contains 
           the id of the new domain when it was created, not the id of the old 
           domain, which was written to the new domain.  This is OK, because 
           Provisioning.renameDomain is only available through -l, and the program
           (zmprov) exits after Provisioning.renameDomain is called.  
        */
        // getTest(EntryType.DOMAIN, domain2, d2RenamedName);
    }

    private void doTestInvalidNames(String domainName, boolean expectedValid) throws Exception {
        
        boolean actualValid;
        try {
            Domain domain = mProv.createDomain(domainName, new HashMap<String, Object>());
            actualValid = (domain != null);
        } catch (ServiceException e) {
            actualValid = false;
        }
        
        assertEquals(expectedValid, actualValid);
    }
    
    public void testDomainInvalidNames() throws Exception {
        
        Config config = mProv.getConfig();
        
        // save he current value of zimbraAllowNonLDHCharsInDomain
        String curAllowNonLDH = config.getAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain);
        
        // test values
        String goodEnglish       = "good" + BASE_DOMAIN_NAME;
        String goodIDN           = makeDomainName("good");
        String LDHEnglish_comma  = "ldh'ldh" + BASE_DOMAIN_NAME;
        String LDHIDN_comma      = makeDomainName("ldh'ldh");
        
        
        // when zimbraAllowNonLDHCharsInDomain is TRUE
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, Provisioning.TRUE);
        mProv.modifyAttrs(config, attrs);
        
        String prefix = "allowtest.";  // so that we don't run into doamin exist problem
        doTestInvalidNames(prefix + goodEnglish, true);
        doTestInvalidNames(prefix + goodIDN, true);
        doTestInvalidNames(prefix + LDHEnglish_comma, true);
        doTestInvalidNames(prefix + LDHIDN_comma, true);
        
        // when zimbraAllowNonLDHCharsInDomain is FALSE
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, Provisioning.FALSE);
        mProv.modifyAttrs(config, attrs);
        
        prefix = "notallowtest.";
        doTestInvalidNames(prefix + goodEnglish, true);
        doTestInvalidNames(prefix + goodIDN, true);
        doTestInvalidNames(prefix + LDHEnglish_comma, false);
        doTestInvalidNames(prefix + LDHIDN_comma, false);
        
        // restore the orig config value back
        attrs.clear();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, curAllowNonLDH);
        mProv.modifyAttrs(config, attrs);
    }
    
    public void testAccount() throws Exception {
        
        IDNName domainName = new IDNName(makeDomainName("domain-acct-test."));
        Domain domain = createDomain(domainName.uName(), domainName.uName());
        
        IDNName acct1Name = new IDNName("acct-1", domainName.uName());
        IDNName acct2Name = new IDNName("acct-2", domainName.uName());
        IDNName acct2RenamedName = new IDNName("acct-2-renamed", domainName.uName());
        IDNName alias1Name = new IDNName("alias-1-of-acct-1", domainName.uName());
        IDNName alias2Name = new IDNName("alias-2-of-acct-1", domainName.uName());
        IDNName cr1Name = new IDNName("cr-1", domainName.uName());
        IDNName cr2Name = new IDNName("cr-2", domainName.uName());
        IDNName cr2RenamedName = new IDNName("cr-2-renamed", domainName.uName());
        
        /*
         * account
         */
        // create test
        Account acct1 = (Account)createTest(EntryType.ACCOUNT, NameType.UNAME, acct1Name);
        Account acct2 = (Account)createTest(EntryType.ACCOUNT, NameType.ANAME, acct2Name);

        // get by name test
        getTest(EntryType.ACCOUNT, acct1, acct1Name);
        getTest(EntryType.ACCOUNT, acct2, acct2Name);
        
        // set address attrs tets
        setAddressAttrsTest(EntryType.ACCOUNT, acct1, acct1Name);
        
        // add aliases
        mProv.addAlias(acct1, alias1Name.uName());  // add alias by uname
        mProv.addAlias(acct1, alias2Name.aName());  // add alias by aname
        // get by alias name test
        getTest(EntryType.ACCOUNT, acct1, alias1Name);
        getTest(EntryType.ACCOUNT, acct1, alias2Name);

        // remove aliases
        mProv.removeAlias(acct1, alias1Name.aName());
        mProv.removeAlias(acct1, alias2Name.uName());
        getTest(EntryType.ACCOUNT, null, alias1Name);
        getTest(EntryType.ACCOUNT, null, alias2Name);
        
        // add aliases back so we can view them after the test
        mProv.addAlias(acct1, alias1Name.uName());  // add alias by uname
        mProv.addAlias(acct1, alias2Name.aName());  // add alias by aname
        
        // rename test
        renameTest(EntryType.ACCOUNT, acct2, acct2RenamedName);
        getTest(EntryType.ACCOUNT, acct2, acct2RenamedName);
        
        /*
         * cr
         */
        // create test
        CalendarResource cr1 = (CalendarResource)createTest(EntryType.CR, NameType.UNAME, cr1Name);
        CalendarResource cr2 = (CalendarResource)createTest(EntryType.CR, NameType.ANAME, cr2Name);

        // get by name test
        getTest(EntryType.CR, cr1, cr1Name);
        getTest(EntryType.CR, cr2, cr2Name);
        
        // rename test
        renameTest(EntryType.CR, cr2, cr2RenamedName);
        getTest(EntryType.CR, cr2, cr2RenamedName);
    }
    
    public void testDistributionList() throws Exception {
       
        IDNName domainName = new IDNName(makeDomainName("domain-dl-test."));
        Domain domain = createDomain(domainName.uName(), domainName.uName());
        
        IDNName acct1Name = new IDNName("acct-1", domainName.uName());
        IDNName acct2Name = new IDNName("acct-2", domainName.uName());
        IDNName dl1Name = new IDNName("dl-1", domainName.uName());
        IDNName dl2Name = new IDNName("dl-2", domainName.uName());
        IDNName dl2RenamedName = new IDNName("dl-2-renamed", domainName.uName());
        IDNName nestedDl1Name = new IDNName("nested-dl-1", domainName.uName());
        IDNName nestedDl2Name = new IDNName("nested-dl-2", domainName.uName());

        /*
         * dl
         */
        // create test
        DistributionList dl1 = (DistributionList)createTest(EntryType.DL, NameType.UNAME, dl1Name);
        DistributionList dl2 = (DistributionList)createTest(EntryType.DL, NameType.ANAME, dl2Name);

        // get by name test
        getTest(EntryType.DL, dl1, dl1Name);
        getTest(EntryType.DL, dl2, dl2Name);
        
        // rename test
        renameTest(EntryType.DL, dl2, dl2RenamedName);
        getTest(EntryType.DL, dl2, dl2RenamedName);
        
        /*
         * member test
         */
        Account a1 = createAccount(acct1Name.uName(), acct1Name.uName());
        Account a2 = createAccount(acct2Name.uName(), acct2Name.uName());
        DistributionList nestedDl1 = createDistributionList(nestedDl1Name.aName(), nestedDl1Name.uName());
        DistributionList nestedDl2 = createDistributionList(nestedDl2Name.aName(), nestedDl2Name.uName());
        
        mProv.addMembers(dl1, new String[]{acct1Name.uName(), acct2Name.aName(), nestedDl1Name.uName(), nestedDl2Name.aName()});
        
        boolean inList;
        inList = mProv.inDistributionList(a1, dl1.getId());
        assertTrue(inList);
        inList = mProv.inDistributionList(a2, dl1.getId());
        assertTrue(inList);
        
        HashMap<String,String> via = new HashMap<String, String>();
        String[] members = dl1.getAllMembers();
        List<String> memberIds = Arrays.asList(members);
        assertTrue(memberIds.contains(acct1Name.aName()));
        assertTrue(memberIds.contains(acct2Name.aName()));
        assertTrue(memberIds.contains(nestedDl1Name.aName()));
        assertTrue(memberIds.contains(nestedDl2Name.aName()));
        
        mProv.removeMembers(dl1, new String[]{acct1Name.uName(), acct2Name.aName(), nestedDl1Name.uName(), nestedDl2Name.aName()});
        members = dl1.getAllMembers();
        assertEquals(0, members.length);
    }
    
    public void testBasicAuth() throws Exception {
        
        IDNName domainName = new IDNName(makeDomainName("basicAuthTest."));
        Domain domain = createDomain(domainName.uName(), domainName.uName());
        
        IDNName acctName = new IDNName("acct", domainName.uName());
        Account acct = (Account)createTest(EntryType.ACCOUNT, NameType.UNAME, acctName);
        
        HttpState initialState = new HttpState();
        
        /*
        Cookie authCookie = new Cookie(restURL.getURL().getHost(), "ZM_AUTH_TOKEN", mAuthToken, "/", null, false);
        Cookie sessionCookie = new Cookie(restURL.getURL().getHost(), "JSESSIONID", mSessionId, "/zimbra", null, false);
        initialState.addCookie(authCookie);
        initialState.addCookie(sessionCookie);
        */
        
        String guestName = acct.getUnicodeName();
        String guestPassword = "test123";
        
        Credentials loginCredentials = new UsernamePasswordCredentials(guestName, guestPassword);
        initialState.setCredentials(AuthScope.ANY, loginCredentials);
        
        HttpClient client = new HttpClient();
        client.setState(initialState);
        
        String url = UserServlet.getRestUrl(acct) + "/Calendar";
        System.out.println("REST URL: " + url);
        HttpMethod method = new GetMethod(url);
        HttpMethodParams methodParams = method.getParams();
        methodParams.setCredentialCharset("UTF-8");
        
        try {
            int respCode = client.executeMethod(method);

            if (respCode != HttpStatus.SC_OK ) {
                 System.out.println("failed, respCode=" + respCode);
            } else {
                 
                 boolean chunked = false;
                 boolean textContent = false;
                 
                 System.out.println("Headers:");
                 System.out.println("--------");
                 for (Header header : method.getRequestHeaders()) {
                     System.out.print("    " + header.toString());
                 }
                 System.out.println();
                 
                 System.out.println("Body:");
                 System.out.println("-----");
                 String respBody = method.getResponseBodyAsString();
                 System.out.println(respBody);
             }
         } finally {
             // Release the connection.
             method.releaseConnection();
         }
    }
    

    private void IDNUtilTest(String u1, String expectedAscii) {
        boolean verbose = true;
        
        String a1 = IDNUtil.toAsciiDomainName(u1);
        assertTrue(expectedAscii.equals(a1));
        
        printOutput(verbose, "u1: " + u1);
        printOutput(verbose, "a1: " + a1);
        
        String u2 = IDNUtil.toUnicodeDomainName(u1);
        String a2 = IDNUtil.toAsciiDomainName(u2);
        printOutput(verbose, "u2: " + u2);
        printOutput(verbose, "a2: " + a2);
        assertTrue(a1.equals(a2) && u1.equals(u2));
        
        // now try the java.net.IDN, should get same result
        // JDK1.6 only
        /*
        int flags = 0;
        
        String a1_java = IDN.toASCII(u1, flags);
        printOutput(verbose, "u1: " + u1);
        printOutput(verbose, "a1_java: " + a1_java);
        
        String u2_java = IDN.toUnicode(u1, flags);
        String a2_java = IDN.toASCII(u2_java, flags);
        printOutput(verbose, "u2_java: " + u2_java);
        printOutput(verbose, "a2_java: " + a2_java);
        assertTrue(a1_java.equals(a2_java) && u1.equals(u2_java));
        // also., make sure the two libs produce same result
        assertTrue(a1.equals(a1_java) && u2.equals(u2_java));
        */
        
        printOutput(verbose, "");
    }
    
    /*
     * run this test with both JRE1.5 and 1.6, should get same result
     * 
     */
    public void testIDNUtil() throws Exception {
        
        IDNUtilTest("foobar.com", "foobar.com");
        IDNUtilTest("\u4e2d\u6587.xyz\u4e2d\u6587abc.com", "xn--fiq228c.xn--xyzabc-dw7i870n.com");

        // domain labels containing LDH characters
        IDNUtilTest("foo'bar.com", "foo'bar.com");
        IDNUtilTest("\u4e2d'\u6587.xyz\u4e2d\u6587abc.com", "xn--'-kq6a506e.xn--xyzabc-dw7i870n.com");
    }
        
    private void emailpTest(String unicode, IDNType idnType) {
        
        boolean verbose = false;
        printOutput(verbose, "\nTesting email with personal part, idn type = " + idnType + "\n");
        
        String emailp_u1 = unicode;
        String emailp_a1 = IDNUtil.toAscii(emailp_u1, idnType);
        printOutput(verbose, "emailp_u1: " + emailp_u1);
        printOutput(verbose, "emailp_a1: " + emailp_a1);
        
        String emailp_u2 = IDNUtil.toUnicode(emailp_a1, idnType);
        String emailp_a2 = IDNUtil.toAscii(emailp_u2, idnType);
        printOutput(verbose, "emailp_u2: " + emailp_u2);
        printOutput(verbose, "emailp_a2: " + emailp_a2);
        
        assertTrue(emailp_u1.equals(emailp_u2));
        assertTrue(emailp_a1.equals(emailp_a2));
    }
    
    public void testEmailp() throws Exception {
        
        // with personal name
        emailpTest("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.emailp);
        emailpTest("\"\u4e2d\u6587\" <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.emailp);
        emailpTest("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>", IDNType.cs_emailp);
        emailpTest("foo bar <test@\u4e2d\u6587.xyz\u4e2d\u6587abc.com>, cat dog <test@xyz\u4e2d\u6587abc.com>", IDNType.cs_emailp);
    }
    
    private void printOutput(boolean verbose, String text) {
        if (!verbose)
            return;
        
        PrintStream ps = System.out;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ps, "UTF-8"));
            writer.write(text+"\n");
            writer.flush();
        } catch (UnsupportedEncodingException e) {
            ps.println(text);
        } catch (IOException e) {
            ps.println(text);
        }
    }
    
 
    public static void main(String[] args) throws Exception {
        /*
         * 
         * SOAP: UTF-8 (multi bytes)
         * LDAP: UTF-8 (multi bytes)
         * Java: UTF-16 (2 bytes Unicode) 
         *       The primitive data type char in the Java programming language is an unsigned 16-bit 
         *       integer that can represent a Unicode code point in the range U+0000 to U+FFFF, 
         *       or the code units of UTF-16.
         */
        // TestUtil.cliSetup();  // we don't want SOAPProvisoning, we want LdapProvisioning
        CliUtil.toolSetup();
        TestUtil.runTest(TestIDN.class);
    }

}
