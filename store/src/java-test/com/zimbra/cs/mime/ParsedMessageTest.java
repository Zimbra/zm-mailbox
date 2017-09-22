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
package com.zimbra.cs.mime;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link ParsedMessage}.
 *
 * @author ysasaki
 */
public final class ParsedMessageTest {

    @BeforeClass
    public static void init() {
        System.setProperty("log4j.configuration", "log4j-test.properties");
        Provisioning.setInstance(new MockProvisioning());
    }

    @Test
    public void normalize() {
        testNormalize("normal subject", "foo", "foo", false);
        testNormalize("leading whitespace", " foo", "foo", false);
        testNormalize("trailing whitespace", "foo\t", "foo", false);
        testNormalize("leading and trailing whitespace", "  foo\t", "foo", false);
        testNormalize("compressing whitespace", "foo  bar", "foo bar", false);
        testNormalize("missing subject", null, "", false);
        testNormalize("blank subject", "", "", false);
        testNormalize("nothing but whitespace", "  \t ", "", false);
        testNormalize("mlist prefix", "[bar] foo", "foo", false);
        testNormalize("only a mlist prefix", "[foo]", "[foo]", false);
        testNormalize("broken mlist prefix", "[bar[] foo", "[bar[] foo", false);
        testNormalize("keep only the last mlist prefix", "[bar][baz][foo]", "[foo]", false);
        testNormalize("re: prefix", "re: foo", "foo", true);
        testNormalize("no space after re: prefix", "re:foo", "foo", true);
        testNormalize("re: prefix with leading whitespace", "  re: foo", "foo", true);
        testNormalize("re and [fwd", "re: [fwd: [fwd: re: [fwd: babylon]]]", "babylon", true);
        testNormalize("alternative prefixes", "Ad: Re: Ad: Re: Ad: x", "x", true);
        testNormalize("mlist prefixes, std prefixes, mixed-case fwd trailers",
                "[foo] Fwd: [bar] Re: fw: b (fWd)  (fwd)", "b", true);
        testNormalize("character mixed in with prefixes, mixed-case fwd trailers",
                "[foo] Fwd: [bar] Re: d fw: b (fWd)  (fwd)", "d fw: b", true);
        testNormalize("intermixed prefixes", "Fwd: [Imap-protocol] Re: so long, and thanks for all the fish!",
                "so long, and thanks for all the fish!", true);
    }

    private void testNormalize(String description, String raw, String expected, boolean reply) {
        Pair<String, Boolean> result = ParsedMessage.trimPrefixes(raw);
        String actual = ParsedMessage.compressWhitespace(result.getFirst());
        Assert.assertEquals("[PREFIX] " + description, expected, actual);
        Assert.assertEquals("[REPLY] " + description, reply, result.getSecond());
        Assert.assertEquals("[NORMALIZE] " + description, expected, ParsedMessage.normalize(raw));
    }


    @Test
    public void encryptedFragment() throws Exception {
        String msgWasEncrypted = L10nUtil.getMessage(L10nUtil.MsgKey.encryptedMessageFragment);
        if (msgWasEncrypted == null) {
            ZimbraLog.misc.error("'encryptedMessageFragment' key missing from ZsMsg.properties");
            msgWasEncrypted = "";
        }

        byte[] raw = ByteStreams.toByteArray(getClass().getResourceAsStream("smime-encrypted.txt"));
        ParsedMessage pm = new ParsedMessage(raw, false);
        Assert.assertEquals("encrypted-message fragment", msgWasEncrypted, pm.getFragment(null));

        raw = ByteStreams.toByteArray(getClass().getResourceAsStream("smime-signed.txt"));
        pm = new ParsedMessage(raw, false);
        Assert.assertFalse("normal message fragment", pm.getFragment(null).equals(msgWasEncrypted));
    }
}
