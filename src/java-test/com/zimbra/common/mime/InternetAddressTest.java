/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.common.mime;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class InternetAddressTest {

    private void test(String msg, String raw, String display, String email) {
        InternetAddress iaddr = new InternetAddress(raw);
        Assert.assertEquals(msg, display, iaddr.getPersonal());
        Assert.assertEquals(msg, email, iaddr.getAddress());
    }

    @Test
    public void parse() {
        test("standard address",
                "Bob the Builder <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("no display name", "bob@example.com", null, "bob@example.com");
        test("no addr-spec", "Bob the Builder", "Bob the Builder", null);
        test("no display name, but addr-spec in brackets",
                "<bob@example.com>",  null, "bob@example.com");
        test("bare non-ASCII character",
                "_Bob_, the Build\u00ear == <bob@example.com>",
                "_Bob_, the Build\u00ear ==", "bob@example.com");
    }

    @Test
    public void whitespace() {
        test("addr-spec in brackets with leading/trailing whitespace",
                "  <bob@example.com>  ",
                null, "bob@example.com");
        test("addr-spec in brackets with whitespace everywhere",
                "  < b o b @ e x a m p l e . c o m >  ",
                null, "bob@example.com");
        test("normalizing/compacting whitespace",
                "Bob\t the\tBuilder <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("eliding whitespace in local-part of unquoted addr-spec",
                "b ob@example.com (Bob the Builder)  ",
                "Bob the Builder", "bob@example.com");
        test("spaces around the address",
                "Bob the Builder < bob@example.com >",
                "Bob the Builder", "bob@example.com");
    }

    @Test
    public void quote() {
        test("stripping leading/trailing whitespace and useless quotes in address",
                "  Bob the Builder   <\"bob\"@\"example.com\">",
                "Bob the Builder", "bob@example.com");
        test("stripping leading/trailing whitespace and proper quoting in address",
                "  Bob the Builder   <\"b ob\"@\"example.com\">",
                "Bob the Builder", "\"b ob\"@example.com");
        test("stripping whitespace and useless quotes in address",
                "  Bob the Builder   <b ob@\"example.com\">",
                "Bob the Builder", "bob@example.com");
        test("joining quoted strings with normal text",
                "\"Bob\" the \"Builder\" <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("blank quoted strings",
                " \"\"    \"Bob the Builder\" <bob@example.com>",
                " Bob the Builder", "bob@example.com");
        test("stripping quotes from local-part of addr-spec",
                "\"bob\"@example.com (Bob the Builder)  ",
                "Bob the Builder", "bob@example.com");
        test("not stripping quotes from local-part of addr-spec",
                "\"b ob\"@example.com (Bob the Builder)  ",
                "Bob the Builder", "\"b ob\"@example.com");
    }

    @Test
    public void domainLiteral() {
        test("domain-literal in address",
                "Bob the Builder <bob@[127.0.0.1]>",
                "Bob the Builder", "bob@[127.0.0.1]");
        test("spaces in domain-literal in address",
                "Bob the Builder <bob@  [ 127.0 .0. 1 ] >",
                "Bob the Builder", "bob@[127.0.0.1]");
        test("quoted-pair in domain-literal in address",
                "Bob the Builder <bob@  [ 127.0 \\.0. 1 ] >",
                "Bob the Builder", "bob@[127.0.0.1]");
        test("spaces and quoted-pair in domain-literal in address with no display-name",
                "bob@  [ 127.0 \\.0. 1 ] ",
                null, "bob@[127.0.0.1]");
    }

    @Test
    public void comment() {
        test("ignoring comments",
                "Bob the (Big) Builder <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("ignoring comment with no addr-spec",
                "Bob the Builder (Bob the Builder)",
                "Bob the Builder", null);
        test("ignoring in-word comments",
                "Bob the Buil(Big)der <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("ignoring comments in address",
                "Bob the Builder <bob(Big)@(Bob)example.com>",
                "Bob the Builder", "bob@example.com");
        test("quotes around commas",
                "\"Bob, the Builder\" <bob@example.com>",
                "Bob, the Builder", "bob@example.com");
        test("display part in comment",
                "bob@example.com (Bob the Builder)  ",
                "Bob the Builder", "bob@example.com");
        test("comments before the address",
                "(Bob the Builder) <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("comments after the address",
                "<bob@example.com> (Bob the Builder)",
                "Bob the Builder", "bob@example.com");
        test("comments and spaces before a non-bracketed address",
                " (Bob the Builder)  bob@example.com",
                "Bob the Builder", "bob@example.com");
        test("display part in nested comment",
                "bob@example.com (Bob( the )Builder)  ",
                "Bob (the) Builder", "bob@example.com");
        test("ignoring all but last comment",
                "(Hambone) bob@example.com (Bob the Builder)  ",
                "Bob the Builder", "bob@example.com");
        test("trailing spaces in comments and a missing end-bracket",
                " ( Bob   the\tBuilder ) <bob@example.com",
                "Bob the Builder", "bob@example.com");
        test("joining quoted strings with normal text and dropping extra comments",
                "\"Bob the\" Builder <bob(Bob)@example.com> (Bobbles)",
                "Bob the Builder", "bob@example.com");
    }

    @Test
    public void rfc2047() {
        test("basic 2047 encoding",
                "=?us-ascii?Q?Bob_the=20Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");

        test("basic 2047 encoding in comments",
                "bob@example.com (=?us-ascii?Q?Bob_the=20Builder?=)",
                "Bob the Builder", "bob@example.com");
        test("unknown encoded-word charset",
                "=?x-unknown?Q?Bob_the=20Builder?= <bob@example.com>",
                "=?x-unknown?Q?Bob_the=20Builder?=", "bob@example.com");
        test("unknown encoded-word charset in comments",
                "bob@example.com (=?x-unknown?Q?Bob_the=20Builder?=)",
                "=?x-unknown?Q?Bob_the=20Builder?=", "bob@example.com");
        test("invalid encoded-word encoding",
                "=?us-ascii?x?Bob_the=20Builder?= <bob@example.com>",
                "=?us-ascii?x?Bob_the=20Builder?=", "bob@example.com");
        test("invalid encoded-word encoding in comments",
                "bob@example.com (=?us-ascii?x?Bob_the=20Builder?=)",
                "=?us-ascii?x?Bob_the=20Builder?=", "bob@example.com");
        test("joining 2047 encoded-words with straight text",
                "=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining 2047 encoded-words with straight text in comments",
                "bob@example.com (=?us-ascii?Q?Bob?= the =?us-ascii?Q?Builder?=)",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words",
                "=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words in comments",
                "bob@example.com (=?us-ascii?Q?Bob_th?= =?us-ascii?Q?e_Builder?=)",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words split by a comment",
                "=?us-ascii?Q?Bob_th?= (Bob) =?us-ascii?Q?e_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words split by a comment containing an encoded-word",
                "=?us-ascii?Q?Bob_th?= (=?us-ascii?Q?Bob=) =?us-ascii?Q?e_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words with an encoded trailing space",
                "=?us-ascii?q?Bob_?=\t=?us-ascii?Q?the_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words with no space in between",
                "=?us-ascii?Q?Bob_th?==?us-ascii?Q?e_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("joining two 2047 encoded-words with just a comment in between",
                "=?us-ascii?Q?Bob_th?=(Bob)=?us-ascii?Q?e_Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("2047 encoding inside of a word",
                "Bo=?us-ascii?Q?b_the=20Buil?=der <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("2047 encoding inside of a word in a comment",
                "bob@example.com (Bo=?us-ascii?Q?b_the=20Buil?=der)",
                "Bob the Builder", "bob@example.com");
        test("joining blank encoded-word and quoted-string",
                " =?us-ascii?q??=    \"Bob the Builder\" <bob@example.com>",
                " Bob the Builder", "bob@example.com");
        test("joining blank encoded-word with unknown charset and quoted-string",
                " =?x-unknown?q??=    \"Bob the Builder\" <bob@example.com>",
                " Bob the Builder", "bob@example.com");
        test("spaces inside encoded-word",
                "=?us-ascii?Q?Bob the Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("spaces inside encoded-word in comments",
                "bob@example.com (=?us-ascii?Q?Bob the Builder?=)",
                "Bob the Builder", "bob@example.com");
        test("encoded double spaces inside encoded-word",
                "=?us-ascii?Q?Bob_the__Builder?= <bob@example.com>",
                "Bob the  Builder", "bob@example.com");
        test("non-encoded double spaces inside encoded-word",
                "=?us-ascii?Q?Bob the  Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
        test("spaces at end of encoded-word",
                "=?us-ascii?Q?Bob the ?= Builder <bob@example.com>",
                "Bob the  Builder", "bob@example.com");
        test("open-brace in unterminated encoded-word",
                "=?us-ascii?Q?Bob the <bob@example.com>",
                "=?us-ascii?Q?Bob the", "bob@example.com");
        test("RFC 2231 language in encoded-word",
                "=?us-ascii*en?Q?Bob_the=20Builder?= <bob@example.com>",
                "Bob the Builder", "bob@example.com");
    }

    @Test
    public void parseHeader() {
        String src = "mine:=?us-ascii?Q?Bob_?=\t=?us-ascii?Q?the_Builder_1?= <bob@example.com>;,=?us-ascii?Q?Bob the Builder 2?= <bob@example.com>";
        List<InternetAddress> iaddrs = InternetAddress.parse(src);
        Assert.assertEquals(2, iaddrs.size());
        Assert.assertEquals("Bob the Builder 1", iaddrs.get(0).getPersonal());
        Assert.assertEquals("bob@example.com", iaddrs.get(0).getAddress());
        Assert.assertEquals("Bob the Builder 2", iaddrs.get(1).getPersonal());
        Assert.assertEquals("bob@example.com", iaddrs.get(1).getAddress());
    }

    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.1.2
     */
    @Test
    public void rfc2822a12() {
        String raw = "\"Joe Q. Public\" <john.q.public@example.com>";
        List<InternetAddress> iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(1, iaddrs.size());
        Assert.assertEquals("Joe Q. Public", iaddrs.get(0).getPersonal());
        Assert.assertEquals("john.q.public@example.com", iaddrs.get(0).getAddress());

        raw = "Mary Smith <mary@x.test>, jdoe@example.org, Who? <one@y.test>";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(3, iaddrs.size());
        Assert.assertEquals("Mary Smith", iaddrs.get(0).getPersonal());
        Assert.assertEquals("mary@x.test", iaddrs.get(0).getAddress());
        Assert.assertNull(iaddrs.get(1).getPersonal());
        Assert.assertEquals("jdoe@example.org", iaddrs.get(1).getAddress());
        Assert.assertEquals("Who?", iaddrs.get(2).getPersonal());
        Assert.assertEquals("one@y.test", iaddrs.get(2).getAddress());

        raw = "<boss@nil.test>, \"Giant; \\\"Big\\\" Box\" <sysservices@example.net>";
        iaddrs = InternetAddress.parse(raw);

    }

    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.1.3
     */
    @Test
    public void rfc2822a13() {
        String raw = "A Group:Chris Jones <c@a.test>,joe@where.test,John <jdoe@one.test>";
        List<InternetAddress> iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(3, iaddrs.size());
        Assert.assertEquals("Chris Jones", iaddrs.get(0).getPersonal());
        Assert.assertEquals("c@a.test", iaddrs.get(0).getAddress());
        Assert.assertNull(iaddrs.get(1).getPersonal());
        Assert.assertEquals("joe@where.test", iaddrs.get(1).getAddress());
        Assert.assertEquals("John", iaddrs.get(2).getPersonal());
        Assert.assertEquals("jdoe@one.test", iaddrs.get(2).getAddress());

        raw = "Undisclosed recipients:;";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(0, iaddrs.size());
    }
    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.5
     */
    @Test
    public void rfc2822a5() {
        String raw = "Pete(A wonderful \\) chap) <pete(his account)@silly.test(his host)>";
        List<InternetAddress> iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(1, iaddrs.size());
        Assert.assertEquals("Pete", iaddrs.get(0).getPersonal());
        Assert.assertEquals("pete@silly.test", iaddrs.get(0).getAddress());

        raw = "A Group(Some people)\n" +
            "    :Chris Jones <c@(Chris's host.)public.example>,\n" +
            "        joe@example.org,\n" +
            " John <jdoe@one.test> (my dear friend); (the end of the group)";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(3, iaddrs.size());
        Assert.assertEquals("Chris Jones", iaddrs.get(0).getPersonal());
        Assert.assertEquals("c@public.example", iaddrs.get(0).getAddress());
        Assert.assertNull(iaddrs.get(1).getPersonal());
        Assert.assertEquals("joe@example.org", iaddrs.get(1).getAddress());
        Assert.assertEquals("John", iaddrs.get(2).getPersonal());
        Assert.assertEquals("jdoe@one.test", iaddrs.get(2).getAddress());

        raw = "(Empty list)(start)Undisclosed recipients  :(nobody(that I know))  ;";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(0, iaddrs.size());
    }

    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.6.1
     */
    @Ignore("Can't handle routes in address yet")
    @Test
    public void rfc2822a61() {
        String raw = "Joe Q. Public <john.q.public@example.com>";
        List<InternetAddress> iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(1, iaddrs.size());
        Assert.assertEquals("Joe Q. Public", iaddrs.get(0).getPersonal());
        Assert.assertEquals("john.q.public@example.com", iaddrs.get(0).getAddress());

        raw = "Mary Smith <@machine.tld:mary@example.net>, , jdoe@test   . example";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(2, iaddrs.size());
        System.out.println(iaddrs.get(0).toString());
        Assert.assertEquals("Mary Smith", iaddrs.get(0).getPersonal()); //FIXME
        Assert.assertEquals("mary@example.net", iaddrs.get(0).getAddress());
        Assert.assertNull(iaddrs.get(1).getPersonal());
        Assert.assertEquals("jdoe@test.example", iaddrs.get(1).getAddress());
    }

    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.6.3
     */
    @Test
    public void rfc2822a63() {
        String raw = "John Doe <jdoe@machine(comment).  example>";
        List<InternetAddress> iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(1, iaddrs.size());
        Assert.assertEquals("John Doe", iaddrs.get(0).getPersonal());
        Assert.assertEquals("jdoe@machine.example", iaddrs.get(0).getAddress());

        raw = "Mary Smith\n     \n     <mary@example.net>";
        iaddrs = InternetAddress.parse(raw);
        Assert.assertEquals(1, iaddrs.size());
        Assert.assertEquals("Mary Smith", iaddrs.get(0).getPersonal());
        Assert.assertEquals("mary@example.net", iaddrs.get(0).getAddress());
    }
}
