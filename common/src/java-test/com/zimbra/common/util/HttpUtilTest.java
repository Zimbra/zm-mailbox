/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.HttpUtil.Browser;

/**
 * Unit test for {@link HttpUtil}.
 *
 * @author ysasaki
 */
public final class HttpUtilTest {

    @Test
    public void encodeFilename() {
        String filename = "document.pdf";
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));

        filename = "\u65e5\u672c\u8a9e.pdf";
        Assert.assertEquals("%E6%97%A5%E6%9C%AC%E8%AA%9E.pdf", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));

        filename = "\u65e5 \u672c \u8a9e.pdf";
        Assert.assertEquals("%E6%97%A5%20%E6%9C%AC%20%E8%AA%9E.pdf", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"=?utf-8?B?5pelIOacrCDoqp4ucGRm?=\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"=?utf-8?B?5pelIOacrCDoqp4ucGRm?=\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));
    }

    @Test
    public void testUrlEscape() {
        Assert.assertEquals("%20", HttpUtil.urlEscape(" "));
        Assert.assertEquals("%22", HttpUtil.urlEscape("\""));
        Assert.assertEquals("%23", HttpUtil.urlEscape("#"));
        Assert.assertEquals("%25", HttpUtil.urlEscape("%"));
        Assert.assertEquals("%26", HttpUtil.urlEscape("&"));
        Assert.assertEquals("%3C", HttpUtil.urlEscape("<"));
        Assert.assertEquals("%3E", HttpUtil.urlEscape(">"));
        Assert.assertEquals("%3F", HttpUtil.urlEscape("?"));
        Assert.assertEquals("%5B", HttpUtil.urlEscape("["));
        Assert.assertEquals("%5C", HttpUtil.urlEscape("\\"));
        Assert.assertEquals("%5D", HttpUtil.urlEscape("]"));
        Assert.assertEquals("%5E", HttpUtil.urlEscape("^"));
        Assert.assertEquals("%60", HttpUtil.urlEscape("`"));
        Assert.assertEquals("%7B", HttpUtil.urlEscape("{"));
        Assert.assertEquals("%7C", HttpUtil.urlEscape("|"));
        Assert.assertEquals("%7D", HttpUtil.urlEscape("}"));
        Assert.assertEquals("%2B", HttpUtil.urlEscape("+"));
    }

    @Test
    public void testUrlUnescape() {
        Assert.assertEquals("\"", HttpUtil.urlUnescape("%22"));
        Assert.assertEquals("#", HttpUtil.urlUnescape("%23"));
        Assert.assertEquals("%", HttpUtil.urlUnescape("%25"));
        Assert.assertEquals("&", HttpUtil.urlUnescape("%26"));
        Assert.assertEquals("<", HttpUtil.urlUnescape("%3C"));
        Assert.assertEquals(">", HttpUtil.urlUnescape("%3E"));
        Assert.assertEquals("?", HttpUtil.urlUnescape("%3F"));
        Assert.assertEquals("[", HttpUtil.urlUnescape("%5B"));
        Assert.assertEquals("\\", HttpUtil.urlUnescape("%5C"));
        Assert.assertEquals("]", HttpUtil.urlUnescape("%5D"));
        Assert.assertEquals("^", HttpUtil.urlUnescape("%5E"));
        Assert.assertEquals("`", HttpUtil.urlUnescape("%60"));
        Assert.assertEquals("{", HttpUtil.urlUnescape("%7B"));
        Assert.assertEquals("|", HttpUtil.urlUnescape("%7C"));
        Assert.assertEquals("}", HttpUtil.urlUnescape("%7D"));
        Assert.assertEquals("+", HttpUtil.urlUnescape("%2B"));
        Assert.assertEquals("+", HttpUtil.urlUnescape("+"));
    }

    /**
     * Tests IE detection using a subset of IE's possible user agent strings
     */
    @Test
    public void testIEDetection() {
        // 10.6
        String IE_10_6 = "Mozilla/5.0 (compatible; MSIE 10.6; Windows NT 6.1; Trident/5.0; InfoPath.2; SLCC1; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET CLR 2.0.50727) 3gpp-gba UNTRUSTED/1.0";
        Browser browser = HttpUtil.guessBrowser(IE_10_6);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(10, HttpUtil.guessBrowserMajorVersion(IE_10_6));

        String IE_10_0 = "Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.1; WOW64; Trident/6.0)";
        browser = HttpUtil.guessBrowser(IE_10_0);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(10, HttpUtil.guessBrowserMajorVersion(IE_10_0));

        String IE_9_0 = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; InfoPath.2; .NET CLR 1.1.4322; .NET4.0C; Tablet PC 2.0)";
        browser = HttpUtil.guessBrowser(IE_9_0);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(IE_9_0));

        String IE_8_0 = "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; InfoPath.2)";
        browser = HttpUtil.guessBrowser(IE_8_0);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(8, HttpUtil.guessBrowserMajorVersion(IE_8_0));

        String IE_7_0b = "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.2; .NET CLR 3.0.04506.30)";
        browser = HttpUtil.guessBrowser(IE_7_0b);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(7, HttpUtil.guessBrowserMajorVersion(IE_7_0b));

        String IE_7_0 = "Mozilla/5.0 (compatible; MSIE 7.0; Windows NT 6.0; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; c .NET CLR 3.0.04506; .NET CLR 3.5.30707; InfoPath.1; el-GR)";
        browser = HttpUtil.guessBrowser(IE_7_0);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(7, HttpUtil.guessBrowserMajorVersion(IE_7_0));

        String IE_6_1 = "Mozilla/4.0 (compatible; MSIE 6.1; Windows XP; .NET CLR 1.1.4322; .NET CLR 2.0.50727)";
        browser = HttpUtil.guessBrowser(IE_6_1);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(IE_6_1));

        String IE_6_0_1 = "Mozilla/4.0 (compatible; MSIE 6.01; Windows NT 6.0)";
        browser = HttpUtil.guessBrowser(IE_6_0_1);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(IE_6_0_1));

        String IE_6_0b = "Mozilla/4.0 (compatible; MSIE 6.0b; Windows NT 5.1)";
        browser = HttpUtil.guessBrowser(IE_6_0b);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(IE_6_0b));

        String IE_6_0 = "Mozilla/5.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)";
        browser = HttpUtil.guessBrowser(IE_6_0);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(IE_6_0));

        String IE_5_5b1 = "Mozilla/4.0 (compatible; MSIE 5.5b1; Mac_PowerPC)";
        browser = HttpUtil.guessBrowser(IE_5_5b1);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(IE_5_5b1));

        String IE_5_5 = "Mozilla/4.0 (compatible; MSIE 5.50; Windows NT; SiteKiosk 4.9; SiteCoach 1.0)";
        browser = HttpUtil.guessBrowser(IE_5_5);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(IE_5_5));

        String IE_5_2_3 = "Mozilla/4.0 (compatible; MSIE 5.23; Mac_PowerPC)";
        browser = HttpUtil.guessBrowser(IE_5_2_3);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(IE_5_2_3));

        String IE_11 = "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; rv:11.0";
        browser = HttpUtil.guessBrowser(IE_11);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(11, HttpUtil.guessBrowserMajorVersion(IE_11));

    }
    /**
     * Tests for detection of firefox using a subset of its available user agent strings
     */
    @Test
    public void testFirefoxDetection() {

        String FF_10_0_a4 = "Mozilla/6.0 (Macintosh; I; Intel Mac OS X 11_7_9; de-LI; rv:1.9b4) Gecko/2012010317 Firefox/10.0a4";
        Browser browser = HttpUtil.guessBrowser(FF_10_0_a4);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(10, HttpUtil.guessBrowserMajorVersion(FF_10_0_a4));

        String FF_9_0_a2 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:9.0a2) Gecko/20111101 Firefox/9.0a2";
        browser = HttpUtil.guessBrowser(FF_9_0_a2);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(FF_9_0_a2));

        String FF_9_0_1 = "Mozilla/5.0 (Windows NT 6.2; rv:9.0.1) Gecko/20100101 Firefox/9.0.1";
        browser = HttpUtil.guessBrowser(FF_9_0_1);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(FF_9_0_1));

        String FF_9_0 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:9.0) Gecko/20100101 Firefox/9.0";
        browser = HttpUtil.guessBrowser(FF_9_0);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(FF_9_0));

        String FF_6_0a2 = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:6.0a2) Gecko/20110613 Firefox/6.0a2";
        browser = HttpUtil.guessBrowser(FF_6_0a2);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(FF_6_0a2));

        String FF_6_0 = "Mozilla/5.0 (Windows NT 5.1; rv:6.0) Gecko/20100101 Firefox/6.0 FirePHP/0.6";
        browser = HttpUtil.guessBrowser(FF_6_0);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(FF_6_0));

        String FF_5_0a2 = "Mozilla/5.0 (X11; Linux i686 on x86_64; rv:5.0a2) Gecko/20110524 Firefox/5.0a2";
        browser = HttpUtil.guessBrowser(FF_5_0a2);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(FF_5_0a2));

        String FF_5_0_1 = "mozilla/3.0 (Windows NT 6.1; rv:2.0.1) Gecko/20100101 Firefox/5.0.1";
        browser = HttpUtil.guessBrowser(FF_5_0_1);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(FF_5_0_1));

        // The original firefox doesn't have a version.. Need to make sure we don't explode, but
        // I doubt this code will ever get called
        String FF = "Mozilla/5.0 (X11; U; Gentoo Linux x86_64; pl-PL) Gecko Firefox";
        browser = HttpUtil.guessBrowser(FF);
        Assert.assertEquals(Browser.FIREFOX, browser);
        Assert.assertEquals(-1, HttpUtil.guessBrowserMajorVersion(FF));
    }

    /**
     * Tests chrome type and version detection
     */
    @Test
    public void testChromeDetection() {
        String CH_19_0_1 = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21";
        Browser browser = HttpUtil.guessBrowser(CH_19_0_1);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(19, HttpUtil.guessBrowserMajorVersion(CH_19_0_1));

        String CH_18_6 = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.2 (KHTML, like Gecko) Chrome/18.6.872.0 Safari/535.2 UNTRUSTED/1.0 3gpp-gba UNTRUSTED/1.0";
        browser = HttpUtil.guessBrowser(CH_18_6);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(18, HttpUtil.guessBrowserMajorVersion(CH_18_6));

        String CH_17_0 = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.65 Safari/535.11";
        browser = HttpUtil.guessBrowser(CH_17_0);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(17, HttpUtil.guessBrowserMajorVersion(CH_17_0));

        String CH_10_0 = "Mozilla/5.0 (X11; U; Linux i686; en-US) AppleWebKit/534.15 (KHTML, like Gecko) Ubuntu/10.10 Chromium/10.0.611.0 Chrome/10.0.611.0 Safari/534.15";
        browser = HttpUtil.guessBrowser(CH_10_0);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(10, HttpUtil.guessBrowserMajorVersion(CH_10_0));

        String CH_8_0_5 = "Mozilla/5.0 (Windows; U; Windows NT 6.1; de-DE) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10";
        browser = HttpUtil.guessBrowser(CH_8_0_5);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(8, HttpUtil.guessBrowserMajorVersion(CH_8_0_5));

        String CH_0 = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.27 Safari/525.13";
        browser = HttpUtil.guessBrowser(CH_0);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(0, HttpUtil.guessBrowserMajorVersion(CH_0));

        String CH = "Mozilla/5.0 (Macintosh; U; Mac OS X 10_6_1; en-US) AppleWebKit/530.5 (KHTML, like Gecko) Chrome/ Safari/530.5";
        browser = HttpUtil.guessBrowser(CH);
        Assert.assertEquals(Browser.CHROME, browser);
        Assert.assertEquals(-1, HttpUtil.guessBrowserMajorVersion(CH));
    }

    /**
     * Checks out safari detection
     */
    @Test
    public void testSafariDetection() {
        String SF_5_0_5 = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; de-at) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";
        Browser browser = HttpUtil.guessBrowser(SF_5_0_5);
        Assert.assertEquals(Browser.SAFARI, browser);
        Assert.assertEquals(5, HttpUtil.guessBrowserMajorVersion(SF_5_0_5));

        String SF_4_1 ="Mozilla/5.0 (Windows; U; Windows NT 5.0; en-en) AppleWebKit/533.16 (KHTML, like Gecko) Version/4.1 Safari/533.16";
        browser = HttpUtil.guessBrowser(SF_4_1);
        Assert.assertEquals(Browser.SAFARI, browser);
        Assert.assertEquals(4, HttpUtil.guessBrowserMajorVersion(SF_4_1));

        String SF_3_2_3 = "Mozilla/5.0 (Windows; U; Windows NT 5.1; cs-CZ) AppleWebKit/525.28.3 (KHTML, like Gecko) Version/3.2.3 Safari/525.29";
        browser = HttpUtil.guessBrowser(SF_3_2_3);
        Assert.assertEquals(Browser.SAFARI, browser);
        Assert.assertEquals(3, HttpUtil.guessBrowserMajorVersion(SF_3_2_3));

        String SF_2_0_2 = "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; nl-nl) AppleWebKit/416.12 (KHTML, like Gecko) Safari/416.13";
        browser = HttpUtil.guessBrowser(SF_2_0_2);
        Assert.assertEquals(Browser.SAFARI, browser);
        Assert.assertEquals(2, HttpUtil.guessBrowserMajorVersion(SF_2_0_2));

        // Note, anything under 2 will be detected as 2. If for some reason we need to tell these
        // browsers apart we can do it by the build numbers, but they are probably pretty old and not worth
        // the code complication to figure that out.
        String SF_1_3_2 = "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; sv-se) AppleWebKit/312.8 (KHTML, like Gecko) Safari/312.5";
        browser = HttpUtil.guessBrowser(SF_1_3_2);
        Assert.assertEquals(Browser.SAFARI, browser);
        Assert.assertEquals(2, HttpUtil.guessBrowserMajorVersion(SF_1_3_2));
    }

    /**
     * Tests out opera detection
     */
    @Test
    public void testOperaDetection() {
        String OP_12_0 = "Opera/9.80 (Windows NT 6.1; U; es-ES) Presto/2.9.181 Version/12.00";
        Browser browser = HttpUtil.guessBrowser(OP_12_0);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(12, HttpUtil.guessBrowserMajorVersion(OP_12_0));

        String OP_11_52 = "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52";
        browser = HttpUtil.guessBrowser(OP_11_52);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(11, HttpUtil.guessBrowserMajorVersion(OP_11_52));

        String OP_10_70 = "Opera/9.80 (Windows NT 6.1; U; pl) Presto/2.6.31 Version/10.70";
        browser = HttpUtil.guessBrowser(OP_10_70);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(10, HttpUtil.guessBrowserMajorVersion(OP_10_70));

        // This is the switch where version isn't there
        String OP_9_99 = "Opera/9.99 (Windows NT 5.1; U; pl) Presto/9.9.9";
        browser = HttpUtil.guessBrowser(OP_9_99);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(OP_9_99));

        String OP_8_53 = "Opera/8.53 (Windows NT 5.2; U; en)";
        browser = HttpUtil.guessBrowser(OP_8_53);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(8, HttpUtil.guessBrowserMajorVersion(OP_8_53));

        String OP_AS_IE = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1) Opera 7.20 [de]";
        browser = HttpUtil.guessBrowser(OP_AS_IE);
        Assert.assertEquals(Browser.IE, browser);
        Assert.assertEquals(6, HttpUtil.guessBrowserMajorVersion(OP_AS_IE));

        String OP_NINTENDO = "Opera/9.30 (Nintendo Wii; U; ; 2071; Wii Shop Channel/1.0; en)";
        browser = HttpUtil.guessBrowser(OP_NINTENDO);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(9, HttpUtil.guessBrowserMajorVersion(OP_NINTENDO));

        String OP = "Mozilla/5.0 (Macintosh; ; Intel Mac OS X; fr; rv:1.8.1.1) Gecko/20061204 Opera";
        browser = HttpUtil.guessBrowser(OP);
        Assert.assertEquals(Browser.OPERA, browser);
        Assert.assertEquals(-1, HttpUtil.guessBrowserMajorVersion(OP));

    }

    @Test
    public void testIEDisposition() {
        String ua = "Mozilla/5.0 (compatible; MSIE 10.6; Windows NT 6.1; Trident/5.0; InfoPath.2; SLCC1; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729; .NET CLR 2.0.50727) 3gpp-gba UNTRUSTED/1.0";

        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt";
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithFilename = "/path/info/ascii.txt";
        String pathInfoWithoutFilename = "/path/info/";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithoutFilename);
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename*=UTF-8''space%22%20space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        Assert.assertEquals("attachement; filename*=UTF-8''Wikip%C3%A9dia.txt", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename*=UTF-8''%ED%95%9C%EA%B5%AD%EC%96%B4%2520%EC%88%98%EC%8B%A0%EC%9E%90.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));


    }

    @Test
    public void testChrome11plusDisposition() {
        String ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21";

        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt";
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithFilename = "/path/info/ascii.txt";
        String pathInfoWithoutFilename = "/path/info/";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithoutFilename);
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename*=UTF-8''space%22%20space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        Assert.assertEquals("attachement; filename*=UTF-8''Wikip%C3%A9dia.txt", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename*=UTF-8''%ED%95%9C%EA%B5%AD%EC%96%B4%2520%EC%88%98%EC%8B%A0%EC%9E%90.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }
    @Test
    public void testChrome10minusDispositionNoPath() {
        String ua = "Mozilla/5.0 (Windows; U; Windows NT 6.1; de-DE) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10";
        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt"; // note, we can't upload a file with this name in chrome.
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithFilename = "/path/info/ascii.txt";
        String pathInfoWithoutFilename = "/path/info/";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithoutFilename).anyTimes();
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename=space\" space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        // Note, this should work as the \u00E9 is just extended ascii (iso8859) which appears to be valid for a web header (but not a mail header)
        Assert.assertEquals("attachement; filename=Wikip\u00E9dia.txt", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename=???%20???.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }

    @Test
    public void testChrome10minusDispositionWithPath() {
        String ua = "Mozilla/5.0 (Windows; U; Windows NT 6.1; de-DE) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.224 Safari/534.10";
        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt"; // note, we can't upload a file with this name in chrome.
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        // with the filename needs to be non-ascii to trip the test correctly
        String pathInfoWithFilename = "/path/info/Wikip\u00E9dia.txt";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithFilename).anyTimes();
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename=space\" space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        // Note: Here's where the main difference is between with and without a path that matches. If there is a non-ascii filename
        // that happens to match what we got in the pathInfo of the request, we'll leave the name out so the browser
        // uses the pathInfo version
        Assert.assertEquals("attachement; ", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename=???%20???.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }

    @Test
    public void testSafariWithoutPath() {
        String ua = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; de-at) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";

        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt";
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithoutFilename = "/path/info/";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithoutFilename).anyTimes();
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename=space\" space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        Assert.assertEquals("attachement; filename=Wikip\u00E9dia.txt", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename=???%20???.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }
    @Test
    public void testSafariWithPath() {
        String ua = "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_8; de-at) AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5 Safari/533.21.1";

        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt";
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithFilename = "/path/info/Wikip\u00E9dia.txt";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithFilename).anyTimes();
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename=space\" space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        Assert.assertEquals("attachement; ", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename=???%20???.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }
    @Test
    public void testFireFox() {
        String ua = "Mozilla/6.0 (Macintosh; I; Intel Mac OS X 11_7_9; de-LI; rv:1.9b4) Gecko/2012010317 Firefox/10.0a4";

        String asciiFilename = "ascii.txt";
        String asciiQuote = "space\" space.txt";
        String iso8859Filename =  "Wikip\u00E9dia.txt";
        String unicodeFilename = "\uD55C\uAD6D\uC5B4%20\uC218\uC2E0\uC790.pdf";

        String pathInfoWithoutFilename = "/path/info/";

        // Use a servlet request mock
        HttpServletRequest request = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(request.getPathInfo()).andReturn(pathInfoWithoutFilename);
        EasyMock.expect(request.getHeader(EasyMock.eq("User-Agent"))).andReturn(ua).anyTimes();

        Object [] mocks = {
            request
        };

        EasyMock.replay(mocks);

        Assert.assertEquals("attachement; filename=\"ascii.txt\"", HttpUtil.createContentDisposition(request,  "attachement", asciiFilename));
        Assert.assertEquals("attachement; filename*=UTF-8''space%22%20space.txt", HttpUtil.createContentDisposition(request,  "attachement", asciiQuote));
        Assert.assertEquals("attachement; filename*=UTF-8''Wikip%C3%A9dia.txt", HttpUtil.createContentDisposition(request,  "attachement", iso8859Filename));
        Assert.assertEquals("attachement; filename*=UTF-8''%ED%95%9C%EA%B5%AD%EC%96%B4%2520%EC%88%98%EC%8B%A0%EC%9E%90.pdf", HttpUtil.createContentDisposition(request,  "attachement", unicodeFilename));

    }

    @Test
    public void testSanitizeUrl() {
        Assert.assertEquals("http://example.com/", HttpUtil.sanitizeURL("http://username:password@example.com/"));
    }
}
