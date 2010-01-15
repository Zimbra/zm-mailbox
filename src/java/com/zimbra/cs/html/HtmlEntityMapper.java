/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.html;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts HTML-specific entities (e.g. &nbsp;) to equivalent unicode character entities (&#!60;)
 * and vice-versa.  Also converts unicode characters to HTML-entity equivalents
 * 
 * This is useful for converting HTML to XML and vice-versa.
 */
public final class HtmlEntityMapper {
    private static final Map<String, Integer> sHtmlEntityToUnicodeMap  = new HashMap<String, Integer>();
    private static final Map<Integer, String> sUnicodeToHtmlEntityMap = new HashMap<Integer, String>();
    
    private static final Pattern htmlEntityPat = Pattern.compile("&([A-Za-z0-9]{2,6});");
    private static final Pattern numEntityPat = Pattern.compile("&#([0-9]{1,5});");
    
    /**
     * Given an input string, replace all HTML-entities with the numberic versions
     *    &nbsp; --> &#160;
     * 
     * @param s
     * @return
     */
    public static String htmlEntitiesToNumeric(String s) {
        String prefix = "&#";
        Pattern regex = htmlEntityPat;
        Map<String,Integer> map = sHtmlEntityToUnicodeMap;
        
        StringBuilder toRet = new StringBuilder();
        
        // while text left {
        //   find first substring that matches regex
        //   have one?
        //       YES!
        //          copy stuff before match into output
        //          in entity map?
        //             NO
        //                put orig string into output
        //             YES
        //                put replacement string into output
        //          move input to end of regex match
        //       NO!
        //          copy rest of string into output
        //          move to end of input
        //     
        
        int curIdx = 0;
        
        Matcher m = regex.matcher(s);

        while (curIdx < s.length()) {
            if (m.find(curIdx)) {
                toRet.append(s.substring(curIdx, m.start()));
                String id = m.group(1);
                if (map.containsKey(id)) {
                    toRet.append(prefix).append(map.get(id)).append(";");
                } else {
                    toRet.append(m.group(0));
                }
                curIdx=m.end();
            } else {
                toRet.append(s.substring(curIdx));
                curIdx = s.length();
            }
        }
        return toRet.toString();
    }
    
    /**
     * Given an input string, replace all numberic entities with known HTML versions
     *     &#160; --> &nbsp;
     *  
     * TODO FIXME: Does NOT currently support hexadecimal entities      
     *     
     * @param s
     * @return
     */
    public static String numericEntitiesToHtml(String s) {
        String prefix = "&";
        Pattern regex = numEntityPat;
        Map<Integer,String> map = sUnicodeToHtmlEntityMap;
        
        StringBuilder toRet = new StringBuilder();
        
        // while text left {
        //   find first substring that matches regex
        //   have one?
        //       YES!
        //          copy stuff before match into output
        //          in entity map?
        //             NO
        //                put orig string into output
        //             YES
        //                put replacement string into output
        //          move input to end of regex match
        //       NO!
        //          copy rest of string into output
        //          move to end of input
        //     
        
        int curIdx = 0;
        
        Matcher m = regex.matcher(s);

        while (curIdx < s.length()) {
            if (m.find(curIdx)) {
                toRet.append(s.substring(curIdx, m.start()));
                boolean ok = false;
                try {
                    Integer id = Integer.parseInt(m.group(1));
                    if (map.containsKey(id)) {
                        toRet.append(prefix).append(map.get(id)).append(";");
                        ok = true;
                    }
                } catch (Exception ex) {
                }
                if (!ok) 
                    toRet.append(m.group(0));
                curIdx=m.end();
            } else {
                toRet.append(s.substring(curIdx));
                curIdx = s.length();
            }
        }
        return toRet.toString();
    }
    
    
    /**
     * Given a Unicode string, replace all the unicode chars with known HTML entities
     *     (char 160) --> &nbsp;
     * @param s
     * @return
     */
    public static String unicodeToHtmlEntity(String s) {
        StringBuilder toRet = new StringBuilder();
        for (char c : s.toCharArray()) {
            String rep = sUnicodeToHtmlEntityMap.get((int)c);
            if (rep != null) 
                toRet.append("&").append(rep).append(";");
            else
                toRet.append(c);
        }
        return toRet.toString();
    }
    
    private static void test(String test1) {
        System.out.println(test1);
        String result = htmlEntitiesToNumeric(test1);
        System.out.println(result);
        String result2 = numericEntitiesToHtml(result);
        System.out.println(result2);
        if (!result2.equals(test1)) {
            System.out.println("Final result NOT EQUAL to initial!\n");
        } else {
            System.out.println("OK!\n");
        }
    }
    
    private static void addToMap(String s, Integer code) {
        sHtmlEntityToUnicodeMap.put(s, code);
        sUnicodeToHtmlEntityMap.put(code, s);
    }
    
    static {
        addToMap("OElig",338);
        addToMap("oelig",339);
        addToMap("Scaron",352);
        addToMap("scaron",353);
        addToMap("Yuml",376);
        addToMap("circ",710);
        addToMap("tilde",732);
        addToMap("ensp",8194);
        addToMap("emsp",8195);
        addToMap("thinsp",8201);
        addToMap("zwnj",8204);
        addToMap("zwj",8205);
        addToMap("lrm",8206);
        addToMap("rlm",8207);
        addToMap("ndash",8211);
        addToMap("mdash",8212);
        addToMap("lsquo",8216);
        addToMap("rsquo",8217);
        addToMap("sbquo",8218);
        addToMap("ldquo",8220);
        addToMap("rdquo",8221);
        addToMap("bdquo",8222);
        addToMap("dagger",8224);
        addToMap("Dagger",8225);
        addToMap("permil",8240);
        addToMap("lsaquo",8249);
        addToMap("rsaquo",8250);
        addToMap("euro",8364);
        addToMap("nbsp",160);
        addToMap("iexcl",161);
        addToMap("cent",162);
        addToMap("pound",163);
        addToMap("curren",164);
        addToMap("yen",165);
        addToMap("brvbar",166);
        addToMap("sect",167);
        addToMap("uml",168);
        addToMap("copy",169);
        addToMap("ordf",170);
        addToMap("laquo",171);
        addToMap("not",172);
        addToMap("shy",173);
        addToMap("reg",174);
        addToMap("macr",175);
        addToMap("deg",176);
        addToMap("plusmn",177);
        addToMap("sup2",178);
        addToMap("sup3",179);
        addToMap("acute",180);
        addToMap("micro",181);
        addToMap("para",182);
        addToMap("middot",183);
        addToMap("cedil",184);
        addToMap("sup1",185);
        addToMap("ordm",186);
        addToMap("raquo",187);
        addToMap("frac14",188);
        addToMap("frac12",189);
        addToMap("frac34",190);
        addToMap("iquest",191);
        addToMap("Agrave",192);
        addToMap("Aacute",193);
        addToMap("Acirc",194);
        addToMap("Atilde",195);
        addToMap("Auml",196);
        addToMap("Aring",197);
        addToMap("AElig",198);
        addToMap("Ccedil",199);
        addToMap("Egrave",200);
        addToMap("Eacute",201);
        addToMap("Ecirc",202);
        addToMap("Euml",203);
        addToMap("Igrave",204);
        addToMap("Iacute",205);
        addToMap("Icirc",206);
        addToMap("Iuml",207);
        addToMap("ETH",208);
        addToMap("Ntilde",209);
        addToMap("Ograve",210);
        addToMap("Oacute",211);
        addToMap("Ocirc",212);
        addToMap("Otilde",213);
        addToMap("Ouml",214);
        addToMap("times",215);
        addToMap("Oslash",216);
        addToMap("Ugrave",217);
        addToMap("Uacute",218);
        addToMap("Ucirc",219);
        addToMap("Uuml",220);
        addToMap("Yacute",221);
        addToMap("THORN",222);
        addToMap("szlig",223);
        addToMap("agrave",224);
        addToMap("aacute",225);
        addToMap("acirc",226);
        addToMap("atilde",227);
        addToMap("auml",228);
        addToMap("aring",229);
        addToMap("aelig",230);
        addToMap("ccedil",231);
        addToMap("egrave",232);
        addToMap("eacute",233);
        addToMap("ecirc",234);
        addToMap("euml",235);
        addToMap("igrave",236);
        addToMap("iacute",237);
        addToMap("icirc",238);
        addToMap("iuml",239);
        addToMap("eth",240);
        addToMap("ntilde",241);
        addToMap("ograve",242);
        addToMap("oacute",243);
        addToMap("ocirc",244);
        addToMap("otilde",245);
        addToMap("ouml",246);
        addToMap("divide",247);
        addToMap("oslash",248);
        addToMap("ugrave",249);
        addToMap("uacute",250);
        addToMap("ucirc",251);
        addToMap("uuml",252);
        addToMap("yacute",253);
        addToMap("thorn",254);
        addToMap("yuml",255);
        addToMap("fnof",402);
        addToMap("Alpha",913);
        addToMap("Beta",914);
        addToMap("Gamma",915);
        addToMap("Delta",916);
        addToMap("Epsilon",917);
        addToMap("Zeta",918);
        addToMap("Eta",919);
        addToMap("Theta",920);
        addToMap("Iota",921);
        addToMap("Kappa",922);
        addToMap("Lambda",923);
        addToMap("Mu",924);
        addToMap("Nu",925);
        addToMap("Xi",926);
        addToMap("Omicron",927);
        addToMap("Pi",928);
        addToMap("Rho",929);
        addToMap("Sigma",931);
        addToMap("Tau",932);
        addToMap("Upsilon",933);
        addToMap("Phi",934);
        addToMap("Chi",935);
        addToMap("Psi",936);
        addToMap("Omega",937);
        addToMap("alpha",945);
        addToMap("beta",946);
        addToMap("gamma",947);
        addToMap("delta",948);
        addToMap("epsilon",949);
        addToMap("zeta",950);
        addToMap("eta",951);
        addToMap("theta",952);
        addToMap("iota",953);
        addToMap("kappa",954);
        addToMap("lambda",955);
        addToMap("mu",956);
        addToMap("nu",957);
        addToMap("xi",958);
        addToMap("omicron",959);
        addToMap("pi",960);
        addToMap("rho",961);
        addToMap("sigmaf",962);
        addToMap("sigma",963);
        addToMap("tau",964);
        addToMap("upsilon",965);
        addToMap("phi",966);
        addToMap("chi",967);
        addToMap("psi",968);
        addToMap("omega",969);
        addToMap("thetasym",977);
        addToMap("upsih",978);
        addToMap("piv",982);
        addToMap("bull",8226);
        addToMap("hellip",8230);
        addToMap("prime",8242);
        addToMap("Prime",8243);
        addToMap("oline",8254);
        addToMap("frasl",8260);
        addToMap("weierp",8472);
        addToMap("image",8465);
        addToMap("real",8476);
        addToMap("trade",8482);
        addToMap("alefsym",8501);
        addToMap("larr",8592);
        addToMap("uarr",8593);
        addToMap("rarr",8594);
        addToMap("darr",8595);
        addToMap("harr",8596);
        addToMap("crarr",8629);
        addToMap("lArr",8656);
        addToMap("uArr",8657);
        addToMap("rArr",8658);
        addToMap("dArr",8659);
        addToMap("hArr",8660);
        addToMap("forall",8704);
        addToMap("part",8706);
        addToMap("exist",8707);
        addToMap("empty",8709);
        addToMap("nabla",8711);
        addToMap("isin",8712);
        addToMap("notin",8713);
        addToMap("ni",8715);
        addToMap("prod",8719);
        addToMap("sum",8721);
        addToMap("minus",8722);
        addToMap("lowast",8727);
        addToMap("radic",8730);
        addToMap("prop",8733);
        addToMap("infin",8734);
        addToMap("ang",8736);
        addToMap("and",8743);
        addToMap("or",8744);
        addToMap("cap",8745);
        addToMap("cup",8746);
        addToMap("int",8747);
        addToMap("there4",8756);
        addToMap("sim",8764);
        addToMap("cong",8773);
        addToMap("asymp",8776);
        addToMap("ne",8800);
        addToMap("equiv",8801);
        addToMap("le",8804);
        addToMap("ge",8805);
        addToMap("sub",8834);
        addToMap("sup",8835);
        addToMap("nsub",8836);
        addToMap("sube",8838);
        addToMap("supe",8839);
        addToMap("oplus",8853);
        addToMap("otimes",8855);
        addToMap("perp",8869);
        addToMap("sdot",8901);
        addToMap("lceil",8968);
        addToMap("rceil",8969);
        addToMap("lfloor",8970);
        addToMap("rfloor",8971);
        addToMap("lang",9001);
        addToMap("rang",9002);
        addToMap("loz",9674);
        addToMap("spades",9824);
        addToMap("clubs",9827);
        addToMap("hearts",9829);
        addToMap("diams",9830);
    }
    
    public static void main(String[] argv) {
        test("abc&nbsp;def&nbsp;&nbsp;ghi&nbsp;&foo;&#1234;zug");
        test("abc&nbsp;def&#z231;&#9999999999;&#99999;&1321asdd;&nbsp;&nbsp;ghi&nbsp;&foo;&le;&sube;&#1234;zug&or;");
    }
}
