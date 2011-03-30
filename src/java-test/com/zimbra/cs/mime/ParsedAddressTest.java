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
package com.zimbra.cs.mime;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link ParsedAddress}.
 *
 * @author ysasaki
 */
public final class ParsedAddressTest {

    @Test
    public void name() {
        runNameSuite("Steven", "Paul", "Jobs");
        runNameSuite("Steven", "P.", "ben Jobs");
        runNameSuite("F. Steve", "Paul", "Jobs");
        runNameSuite("F. S.", "Paul", "Jobs");
        runNameSuite("Steven", "X. Paul", "van der Jobs");
        runNameSuite("S.P.", "Jobs");
        runNameSuite("S.", "van der Woz", "del Jobs");
        runNameSuite("F. S. P.", "van der Woz", "Jobs");
    }

    @Test
    public void address() {
        new Helper("prefix lastname").first("Della").last("Gonzales")
            .runAddressTest("     \"Della Gonzales\" <rvhxhjyjxugp@example.com>");
        new Helper("trailing dash is delimiter").first("Stan").last("O'Neal").suffix("Chairman, CEO & President")
            .runAddressTest("     \"Stan O'Neal - Chairman, CEO & President\" <antifraudcentre@example.com>");
        new Helper("chinese square brackets").first("\u53B3\u9078\u60C5\u5831")
            .runAddressTest("=?iso-2022-jp?B?GyRCIVo4N0EqPnBKcyFbGyhC?=<teresa@example.com>");
        new Helper("1-character name").first("\u97FF")
            .runAddressTest("=?iso-2022-jp?B?GyRCNkEbKEI=?=<lovecherry_hibiki_15@example.co.jp>");
        //TODO failing
        //new Helper("asian last first").first("\u7D14").last("\u524D\u5DDD")
        //    .runAddressTest("=?iso-2022-jp?B?GyRCQTBAbhsoQiAbJEI9YxsoQg==?=<maekawa_junjun.pickpick1976@example.ne.jp>");
        //TODO failing
        //new Helper("email in name").first("\u6CB3\u5408\u9759\u9999")
        //    .runAddressTest("=?iso-2022-jp?B?IhskQjJPOWdARTlhGyhCIjxzd2VldF9zaXp1MDMwNUB5YWhvby5jby5qcD4=?=<sweet_sizu0305@example.co.jp>");
        new Helper("email in name").first("peerless_p_0@yahoo.co.jp")
            .runAddressTest("=?iso-2022-jp?B?cGVlcmxlc3NfcF8wQHlhaG9vLmNvLmpw?=<peerless_p_0@example.co.jp>");
        //TODO failing
        //new Helper("leading dash is delimiter").first("Janine").last("Gr\u00FCndel").suffix("Intergroove")
        //    .runAddressTest("=?iso-8859-1?Q?Janine_Gr=FCndel_-Intergroove-?= <JanineGruendel@example.com>");
        //TODO failing
        //new Helper("honorific middle name").first("\u0424\u0443\u043D\u043A\u0446\u0438\u0438").middle("\u0432\u043D.")
        //    .last("\u0430\u0443\u0434\u0438\u0442\u0430")
        //    .runAddressTest("=?koi8-r?B?5tXOy8PJySDXzi4gwdXEydTB?= <info@example.ru>");
        //TODO failing
        //new Helper("numeral not first name").first("\u043C\u0430\u044F")
        //    .runAddressTest("=?koi8-r?B?MjcgzcHRIDIwMDg=?= <webmaster@example.org>");
        //TODO failing
        //new Helper("honorific w/o dot").honorific("Dr").first("R G").last("Edwards")
        //    .runAddressTest("Dr R G Edwards <rich.edwards@example.com>");
        new Helper("suffix honorific w/o dot").first("Emerich").middle("R").last("Winkler").suffix("Jr")
            .runAddressTest("Emerich R Winkler Jr <erwinklerjr@example.ca>");
        //TODO failing
        //new Helper("nonstandard suffix honorific w/o dot").first("Farai").last("Aggrey").suffix("jnr")
        //    .runAddressTest("Farai Aggrey jnr <faraiggrey@example.com>");
        //TODO failing
        //new Helper("initial (no comma) as suffix").first("Luigi").last("Ottaviani").suffix("T")
        //    .runAddressTest("Luigi Ottaviani T <luigi.ottaviani@example.it>");
        //TODO failing
        //new Helper("honorific w/o dot").honorific("MRS").first("JENNIFER").last("PETERS")
        //    .runAddressTest("MRS JENNIFER PETERS <jenniferpeters2000@example.com>");
        new Helper("mis-quoted address").first("ymskjetuiuod@example.ru")
            .runAddressTest("\" <ymskjetuiuod@example.ru>");
        new Helper("oddball quoting").first("PROFECO@dogfood.zimbra.com")
            .runAddressTest("\":PROFECO\"@dogfood.zimbra.com: <buzon@example.mx>");
        new Helper("inlined slash as delimiter").first("\u6E1B\u80A5")
            .runAddressTest("\"=?BIG5?B?tO6qzi+rT7C3pOiqa7Ddp9o=?=\" <whyslim4@example.com>");
        new Helper("embedded quoted 2047").first("Francisco").last("Gon\u00E7alves")
            .runAddressTest("\"=?ISO-8859-1?Q?Francisco_Gon=E7alves?=\" <francis.goncalves@example.com>");
        //TODO failing
        //new Helper("nickname in parens").first("Anthony").last("Foiani")
        //    .runAddressTest("\"Anthony (Tony) Foiani\" <tfoiani@yahoo-example.com>");
        new Helper("numeral in suffix").first("B. Winston").last("Sandage").suffix("III")
            .runAddressTest("\"B. Winston Sandage, III\" <wsandage@example.com>");
        new Helper("extraneous backslashes").first("Benjamin").last("Wu").suffix("y!mail")
            .runAddressTest("\"Benjamin Wu \\(y!mail\\)\" <wub@example.com>");
        //TODO failing
        //new Helper("ampersand means company").first("Comercial")
        //    .runAddressTest("\"Comercial Ideas & Dise=?iso-8859-1?b?8Q==?=o\" <ideasydiseno@example.es>");
        new Helper("trailing !").first("Eli").last("Stevens").suffix("Y!")
            .runAddressTest("\"Eli Stevens (Y!)\" <elis@example.com>");
        new Helper("trailing comma").first("Ewald").last("de Bever").suffix("Mr")
            .runAddressTest("\"Ewald de Bever (Mr),\"<verbever1@example.com>");
        new Helper("Inc. means company").first("Flickerbox")
            .runAddressTest("\"Flickerbox, Inc.\" <paul@example.com>");
        new Helper("retain delimiters in suffix").first("Cory").last("Goligoski").suffix("MENLO PARK, CA")
            .runAddressTest("\"Goligoski, Cory (MENLO PARK, CA)\" <cory_r_goligoski@example.com>");
        //TODO failing
        //new Helper("honorific middle name").first("Hans").middle("Th.").last("Prade")
        //    .runAddressTest("\"Hans Th. Prade\" <h.prade@example.com>");
        //TODO failing
        //new Helper(".net means company").first("J.P.").last("Maxwell")
        //    .runAddressTest("\"J.P. Maxwell / tipit.net\" <jp@example.net>");
        new Helper("multi-dashes as delimiter").first("John").last("Katsaros").suffix("Internet Research Group")
            .runAddressTest("\"John Katsaros -- Internet Research Group\" <jkatsar1169@example.net>");
        new Helper("retain delimiters in suffix").first("Louis").last("Ferreira").suffix("BCX - Microsoft Competency")
            .runAddressTest("\"Louis Ferreira - BCX - Microsoft Competency\" <Louis.Ferreira@example.co.za>");
        //TODO failing
        //new Helper("ampersand means company").first("Macworld")
        //    .runAddressTest("\"Macworld Conference & Expo\" <macworldexpo@example.com>");
        new Helper("colon as delimiter").first("Montgomery").last("Research").suffix("Midmarket Strategies")
            .runAddressTest("\"Montgomery Research: Midmarket Strategies\" <yms@example.com>");
        //TODO failing
        //new Helper("skip duplicate (addr)").first("NG").middle("Jiang").last("Hao")
        //    .runAddressTest("\"NG Jiang Hao (jianghao@mjm.com.sg)\" <Jianghao@example.com.sg>");
        new Helper("Inc. means company").first("Nuevora")
            .runAddressTest("\"Nuevora, Inc.\" <accountspayable@example.com>");
        //TODO failing
        //new Helper("too-long honorific").first("Patric.").middle("W.").last("Chan")
        //    .runAddressTest("\"Patric. W. Chan \" <wikster1@example.net>");
        new Helper("roman numeral").first("Philip").middle("W.").last("Dalrymple").suffix("III")
            .runAddressTest("\"Philip W. Dalrymple III\" <pwd@example.com>");
        new Helper("multiple honorifics").honorific("Prof. Dr.").first("Karl-Heinz").last("Kunzelmann")
            .runAddressTest("\"Prof. Dr. Karl-Heinz Kunzelmann\" <karl-heinz@example.de>");
        new Helper("retain delimiters in suffix").first("Regina").last("Ciardiello").suffix("Editor, VSR")
            .runAddressTest("\"Regina Ciardiello, Editor, VSR\" <edgell@example.com>");
        new Helper("trailing apostrophe").first("Rene'").middle("N.").last("Godwin")
            .runAddressTest("\"Rene' N. Godwin\" <godwin4@example.net>");
        new Helper("nonduplicate addr").first("Reuven").last("Cohen").suffix("ruvnet@gmail.com")
            .runAddressTest("\"Reuven Cohen [ruvnet@gmail.com]\" <ruv@example.com>");
        new Helper("middle name abbr").first("Robert").middle("Q.H.").last("Bui")
            .runAddressTest("\"Robert Q.H. Bui\" <robertb@example.com.au>");
        new Helper("bar as delimiter").first("Sander").last("Manneke").suffix("Internet Today")
            .runAddressTest("\"Sander Manneke | Internet Today\" <s.manneke@example.nl>");
        new Helper("at-sign delimiter").first("Avis").last("Scheffler").suffix("P-f-i-z-e-r Supplies")
            .runAddressTest("\"Scheffler, Avis @ -P-f-i-z-e-r Supplies\" <turryeaProducts.Registration5876@example.com>");
        //TODO failing
        //new Helper(".com is company").first("SearchWebJobs")
        //    .runAddressTest("\"SearchWebJobs .com\" <jobs@example.com>");
        new Helper("roman numeral").first("William").middle("J.").last("Robb").suffix("III, M.D.")
            .runAddressTest("\"William J. Robb III, M.D.\" <wrobb@example.com>");
        new Helper("prefix lastname").first("Della").last("Gonzales")
            .runAddressTest("     \"Della Gonzales\" <rvhxhjyjxugp@example.com>");
        new Helper("suffix honorific w/o dot").first("Emerich").middle("R").last("Winkler").suffix("Jr")
            .runAddressTest("Emerich R Winkler Jr <erwinklerjr@example.ca>");
        new Helper("numeral in suffix").first("B. Winston").last("Sandage").suffix("III")
            .runAddressTest("\"B. Winston Sandage, III\" <wsandage@example.com>");
        new Helper("trailing !").first("Eli").last("Stevens").suffix("Y!")
            .runAddressTest("\"Eli Stevens (Y!)\" <elis@example.com>");
        new Helper("trailing comma").first("Ewald").last("de Bever").suffix("Mr")
            .runAddressTest("\"Ewald de Bever (Mr),\"<verbever1@example.com>");
        new Helper("retain delimiters in suffix").first("Cory").last("Goligoski").suffix("MENLO PARK, CA")
            .runAddressTest("\"Goligoski, Cory (MENLO PARK, CA)\" <cory_r_goligoski@example.com>");
        new Helper("terminal abbr").first("interWays").runAddressTest("\"interWays e.K.\" <info@example.de>");
        new Helper("multi-dashes as delimiter").first("John").last("Katsaros").suffix("Internet Research Group")
            .runAddressTest("\"John Katsaros -- Internet Research Group\" <jkatsar1169@example.net>");
        new Helper("retain delimiters in suffix").first("Louis").last("Ferreira").suffix("BCX - Microsoft Competency")
            .runAddressTest("\"Louis Ferreira - BCX - Microsoft Competency\" <Louis.Ferreira@example.co.za>");
        new Helper("roman numeral").first("Philip").middle("W.").last("Dalrymple").suffix("III")
            .runAddressTest("\"Philip W. Dalrymple III\" <pwd@example.com>");
        new Helper("multiple honorifics").honorific("Prof. Dr.").first("Karl-Heinz").last("Kunzelmann")
            .runAddressTest("\"Prof. Dr. Karl-Heinz Kunzelmann\" <karl-heinz@example.de>");
        new Helper("drailing apostrophe").first("Rene'").middle("N.").last("Godwin")
            .runAddressTest("\"Rene' N. Godwin\" <godwin4@example.net>");
        new Helper("middle name abbr").first("Robert").middle("Q.H.").last("Bui")
            .runAddressTest("\"Robert Q.H. Bui\" <robertb@example.com.au>");
        new Helper("roman numeral").first("William").middle("J.").last("Robb").suffix("III, M.D.")
            .runAddressTest("\"William J. Robb III, M.D.\" <wrobb@example.com>");
    }

    @Test
    public void getSortString() {
        List<ParsedAddress> addrs = new ArrayList<ParsedAddress>();
        addrs.add(new ParsedAddress("user1 <user1@zimbra.com>"));
        addrs.add(new ParsedAddress("user2@zimbra.com"));
        addrs.add(new ParsedAddress("user3 <user3@zimbra.com>"));
        Assert.assertEquals("user1, user2@zimbra.com, user3", ParsedAddress.getSortString(addrs));
    }

    private void runNameSuite(String first, String last) {
        new Helper("first last").first(first).last(last)
            .runNameTest(first + ' ' + last);
        new Helper("hon. first last").honorific( "Mr.").first(first).last(last)
            .runNameTest("Mr. " + first + ' ' + last);
        new Helper("first last, suf").first(first).last(last).suffix("MD")
            .runNameTest(first + ' ' + last + ", MD");
        new Helper("first last num").first(first).last(last).suffix("III")
            .runNameTest(first + ' ' + last + " III");
        new Helper("first last (suf)").first(first).last(last).suffix("dec'd")
            .runNameTest(first + ' ' + last + " (dec'd)");
        new Helper("first last [suf]").first(first).last(last).suffix("CPA")
            .runNameTest(first + ' ' + last + " [CPA]");

        new Helper("last, first").first(first).last(last)
            .runNameTest(last + ", " + first);
        new Helper("last, hon. first").first(first).last(last).honorific("Prof.")
            .runNameTest(last + ", Prof. " + first);
        new Helper("last, first, suf").first(first).last(last).suffix("M.D.")
            .runNameTest(last + ", " + first + ", M.D.");
        new Helper("last, first suf").first(first).last(last).suffix("Jr.")
            .runNameTest(last + ", " + first + " Jr.");
        new Helper("last, first (suf)").first(first).last(last).suffix("dec'd")
            .runNameTest(last + ", " + first + " (dec'd)");
        new Helper("last, first [suf]").first(first).last(last).suffix("CPA")
            .runNameTest(last + ", " + first + " [CPA]");
    }

    private void runNameSuite(String first, String middle, String last) {
        runNameSuite(first, last);

        new Helper("first middle last").first(first).middle(middle).last(last)
            .runNameTest(first + ' ' + middle + ' ' + last);
        new Helper("hon. first middle last").honorific("Gov.").first(first).middle(middle).last(last)
            .runNameTest("Gov. " + first + ' ' + middle + ' ' + last);
        new Helper("first middle last, abbr").first(first).middle(middle).last(last).suffix("M.D.")
            .runNameTest(first + ' ' + middle + ' ' + last + " M.D.");
        new Helper("first middle last num").first(first).middle(middle).last(last).suffix("III")
            .runNameTest(first + ' ' + middle + ' ' + last + " III");
        new Helper("first middle last (suf)").first(first).middle(middle).last(last).suffix("dec'd")
            .runNameTest(first + ' ' + middle + ' ' + last + " (dec'd)");
        new Helper("first middle last [suf]").first(first).middle(middle).last(last).suffix("CPA")
            .runNameTest(first + ' ' + middle + ' ' + last + " [CPA]");

        new Helper("last, first middle").first(first).middle(middle).last(last)
            .runNameTest(last + ", " + first + ' ' + middle);
        new Helper("last, hon. first middle").honorific("Dr.").first(first).middle(middle).last(last)
            .runNameTest(last + ", Dr. " + first + ' ' + middle);
        new Helper("last, first middle, suf").first(first).middle(middle).last(last).suffix("Ph.D.")
            .runNameTest(last + ", " + first + ' ' + middle + ", Ph.D.");
        new Helper("last, first middle (suf)").first(first).middle(middle).last(last).suffix("dec'd")
            .runNameTest(last + ", " + first + ' ' + middle + " (dec'd)");
        new Helper("last, first middle [suf]").first(first).middle(middle).last(last).suffix("CPA")
            .runNameTest(last + ", " + first + ' ' + middle + " [CPA]");
        new Helper("last, first middle \\(suf\\)").first(first).middle(middle).last(last).suffix("ITC")
            .runNameTest(last + ", " + first + ' ' + middle + " \\(ITC\\)");
    }

    private static final class Helper {

        private String description;
        private String honorific;
        private String first;
        private String middle;
        private String last;
        private String suffix;

        Helper(String desc) {
            description = desc;
        }

        Helper honorific(String value) {
            honorific = value;
            return this;
        }

        Helper first(String value) {
            first = value;
            return this;
        }

        Helper middle(String value) {
            middle = value;
            return this;
        }

        Helper last(String value) {
            last = value;
            return this;
        }

        Helper suffix(String value) {
            suffix = value;
            return this;
        }

        private void runNameTest(String name) {
            verify(new ParsedAddress(null, name).parse());
        }

        private void runAddressTest(String addr) {
            verify(new ParsedAddress(addr).parse());
        }

        private void verify(ParsedAddress pa) {
            if (honorific != null || pa.honorific != null) {
                Assert.assertEquals(description, honorific, pa.honorific);
            }
            if (first != null || pa.firstName != null) {
                Assert.assertEquals(description, first, pa.firstName);
            }
            if (middle != null || pa.middleName != null) {
                Assert.assertEquals(description, middle, pa.middleName);
            }
            if (last != null || pa.lastName != null) {
                Assert.assertEquals(description, last, pa.lastName);
            }
            if (suffix != null || pa.suffix != null) {
                Assert.assertEquals(description, suffix, pa.suffix);
            }
        }
    }
}
