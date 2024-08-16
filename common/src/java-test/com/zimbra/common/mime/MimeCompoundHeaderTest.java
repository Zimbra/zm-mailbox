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
package com.zimbra.common.mime;

import org.junit.Assert;
import org.junit.Test;

public class MimeCompoundHeaderTest {
    private void test(boolean isContentType, String description, String raw, String value, String[] params) {
        MimeCompoundHeader mch = isContentType ? new ContentType(raw) : new ContentDisposition(raw);
        String primary = isContentType ? ((ContentType) mch).getContentType() : ((ContentDisposition) mch).getDisposition();
        Assert.assertEquals(description, value, primary);
        Assert.assertEquals(description, params.length, mch.getParameterCount() * 2);
        for (int i = 0; i < params.length; i += 2) {
            Assert.assertEquals(description, params[i + 1], mch.getParameter(params[i]));
        }
    }

    @Test public void testContentType() {
        test(true, "missing semicolon between params, standard line breaks",
                "text/plain; charset=US-ASCII;\r\n\tFormat=Flowed   DelSp=Yes\r\n",
                "text/plain", new String[] { "charset", "US-ASCII", "format", "Flowed" });
        test(true, "mixed encoded and non-encoded continuations",
                "application/x-stuff; title*0*=us-ascii'en'This%20is%20even%20more%20; title*1*=%2A%2A%2Afun%2A%2A%2A%20; title*2=\"isn't it!\"\n",
                "application/x-stuff", new String[] { "title", "This is even more ***fun*** isn't it!" });
        test(true, "downcasing value, implicit end-of-value at eol",
                "multipart/MIXED; charset=us-ascii;\n foo=\n  boundary=\"---\" \n",
                "multipart/mixed", new String[] { "charset", "us-ascii", "foo", "boundary=\"---\"" });
        test(true, "non-encoded continuation",
                "message/external-body; access-type=URL;\n URL*0=\"ftp://\";\n URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"\n",
                "message/external-body", new String[] { "access-type", "URL", "url", "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar" });
        test(true, "encoded param value",
                "application/x-stuff;\n\ttitle*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A",
                "application/x-stuff", new String[] { "title", "This is ***fun***" });
        test(true, "missing quotes around param value",
                "application/pdf;\n    x-unix-mode=0644;\n    name=Zimbra on Mac OS X success story.pdf",
                "application/pdf", new String[] { "x-unix-mode", "0644", "name", "Zimbra" });
        test(true, "invalid value",
                "c; name=TriplePlay_Converged_Network_v5.pdf;\n x-mac-creator=70727677; x-mac-type=50444620",
                "application/octet-stream", new String[] { "name", "TriplePlay_Converged_Network_v5.pdf", "x-mac-creator", "70727677", "x-mac-type", "50444620" });
        test(true, "'text' as value, backslashes in quoted-string, missing equals, missing param name, blank param, comments before param name, nested comments",
                "text;\n pflaum;=foo; name=\"spam\\\"bag\\\\wall\" \n\t((plain; text=missing); (pissed=off); where=myrtle);;a=b;c;=d;\n (a)foo=bar",
                "text/plain", new String[] { "pflaum", "", "name", "spam\"bag\\wall", "a", "b", "c", "", "foo", "bar" });
        test(true, "null input",
                null,
                "text/plain", new String[] {});
        test(true, "comments before and after value, param name, equals, and param value",
                " (morg) text/plain(whoppity)  ;(heep)(hop(hoo)) format(ig)=(nore)\"floo\"  (kell) \n (perm) \n\t(eeble) zoom (ig) = (nore)whop (mm)",
                "text/plain", new String[] { "format", "floo", "zoom", "whop" });
        test(true, "unquoted encoded-words, bad encoded-words in non-2231 values",
                "text/plain; filename==?us-ascii?q?boo_bah.pdf?=; note=\"   ?==?\"; bloop=\"=?x-unknown?a?text?=\" ",
                "text/plain", new String[] { "filename", "boo bah.pdf", "note", "   ?==?", "bloop", "=?x-unknown?a?text?=" });
        test(true, "tspecials in primary value",
                "audio/x-mpeg-3,name=VM_2012-01-24_11-12-54.mp3",
                "audio/x-mpeg-3", new String[] {});
        test(true, "extra slash in primary value",
                "text/plain/html; foo=bar",
                "text/plain", new String[] { "foo", "bar" });
        test(true, "trailing slash in primary value",
                "text/; foo=bar",
                "text/plain", new String[] { "foo", "bar" });
        test(true, "trailing double slash in primary value",
                "text//; foo=bar",
                "text/plain", new String[] { "foo", "bar" });
    }

    @Test public void testContentDisposition() {
        test(false, "content-insensitive value, leading spaces, old-style RFC 2047 param values",
                "   \n  INline;\n filename=\"=?utf-8?Q?=E3=82=BD=E3=83=AB=E3=83=86=E3=82=A3=E3=83=AC=E3=82=A4.rtf?=\"\n  \n ",
                "inline", new String[] { "filename", "\u30bd\u30eb\u30c6\u30a3\u30ec\u30a4.rtf" });
        test(false, "default value, leading spaces, RFC 2231 encoding",
                "   \n  gropp;\n filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3%E3%83%AC%E3%82%A4.rtf\n  \n ",
                "attachment", new String[] { "filename", "\u30bd\u30eb\u30c6\u30a3\u30ec\u30a4.rtf" });
        test(false, "ISO-2022-JP",
                "attachment; filename*=iso-2022-jp'ja'%1B%24B%2327n9f%1B%28B.jpg",
                "attachment", new String[] { "filename", "\uff12\u6708\u53f7.jpg" });
        test(false, "encoded continuations",
                "attachment; filename*0*=ISO-8859-1''BASE%20INICIAL%20CAMPANHA%20PROVIS%C3O%20ABAIXO; filename*1*=%20DE%20ZERO%2009_10_06%20SUCHY.xls",
                "attachment", new String[] { "filename", "BASE INICIAL CAMPANHA PROVIS\u00c3O ABAIXO DE ZERO 09_10_06 SUCHY.xls" });
        test(false, "joined 2047 encoded-words",
                "attachment;\n filename=\"=?iso-8859-1?Q?BASE_INICIAL_CAMPANHA_PROVIS=C3O_ABAIXO_DE_ZERO_09=5F10=5F?=\n =?iso-8859-1?Q?06_SUCHY=2Exls?=\"",
                "attachment", new String[] { "filename", "BASE INICIAL CAMPANHA PROVIS\u00c3O ABAIXO DE ZERO 09_10_06 SUCHY.xls" });
        test(false, "misordered continuations, continuations overriding standard value",
                "inline;\n filename=\"1565 =?ISO-8859-1?Q?ST=C5ENDE_CAD_Netic_SKI=2Epdf?=\";\n filename*1*=%20%4E%65%74%69%63%20%53%4B%49%2E%70%64%66;\n filename*0*=ISO-8859-1''%31%35%36%35%20%53%54%C5%45%4E%44%45%20%43%41%44",
                "inline", new String[] { "filename", "1565 ST\u00c5ENDE CAD Netic SKI.pdf" });
        test(false, "misordered continuations, continuations overriding standard value",
                "inline;\n filename*1*=%20%4E%65%74%69%63%20%53%4B%49%2E%70%64%66;\n filename*0*=ISO-8859-1''%31%35%36%35%20%53%54%C5%45%4E%44%45%20%43%41%44\n filename=\"1565 =?ISO-8859-1?Q?H=C5MBURGER=2Epdf?=\"",
                "inline", new String[] { "filename", "1565 H\u00c5MBURGER.pdf" });
        test(false, "leading CFWS, missing semicolon after value, missing semicolon after quoted-string, trailing comment",
                "  \n inline\n foo=\"bar\"\n baz=whop\n (as)\n",
                "inline", new String[] { "foo", "bar", "baz", "whop" });
        test(false, "missing charset on first continuation",
                "attachment; foo*0=big; foo*1*=%20dog",
                "attachment", new String[] { "foo", "big dog" });
        test(false, "missing first continuation, out-of-order continuations",
                "attachment; foo*2*=%20dog; foo*1=big",
                "attachment", new String[] { "foo", "big dog" });
        test(false, "charset on subsequent continuation, out-of-order continuations",
                "attachment; foo*2*=%20dog; foo*1=iso-8859-1'en'big",
                "attachment", new String[] { "foo", "iso-8859-1'en'big dog" });
        test(false, "encoded continuation split across partials",
                "inline;\n filename*0*=ISO-2022-JP''%1B%24%42%24%33%24%73%24%4B%24%41%24%4F%21%22%40;\n filename*1*=%24%33%26%21%2A%1B%28%42%2E%70%64%66",
                "inline", new String[] { "filename", "\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\uff01.pdf" });
        test(false, "bare 8-bit chars in filename",
                "attachment; filename=\"About \u00E5 \u00E4 \u00F6 Downloads.pdf\"",
                "attachment", new String[] { "filename", "About \u00E5 \u00E4 \u00F6 Downloads.pdf" });
        test(false, "missing 2231 charset/language",
                "Content-Disposition: attachment; \r\nfilename*=Jarrod_Stiles%7a_resume_5579615_10344909272010946044.pdf",
                "attachment", new String[] { "filename", "Jarrod_Stilesz_resume_5579615_10344909272010946044.pdf" });
        test(false, "missing 2231 charset/language with subsequent param",
                "Content-Disposition: attachment; \r\nfilename*=Jarrod_Stiles%7a_resume_5579615_10344909272010946044.pdf; foo=bar",
                "attachment", new String[] { "filename", "Jarrod_Stilesz_resume_5579615_10344909272010946044.pdf", "foo", "bar" });
        test(false, "value continuation",
                "Content-Disposition: attachment; filename*0=\"foo.\"; filename*1=\"html\"",
                "attachment", new String[] { "filename", "foo.html"});
        test(false, "using RFC2231 encoded UTF-8, with double quotes around the parameter value.\n",
                "Content-Disposition: attachment; filename*=\"UTF-8''foo-%c3%a4.html\"",
                "attachment", new String[] {});
    }
    // FIXME: add tests for serialization (2231 or not, various charsets, quoting, folding, etc.)
}
