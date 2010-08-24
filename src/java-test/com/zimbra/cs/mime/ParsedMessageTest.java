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
package com.zimbra.cs.mime;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.document.Document;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.analysis.RFC822AddressTokenStream;

/**
 * Unit test for {@link ParsedMessage}.
 *
 * @author ysasaki
 */
public class ParsedMessageTest {

    @BeforeClass
    public static void init() {
        Provisioning.setInstance(new MockProvisioning());
    }

    /**
     * @see http://tools.ietf.org/html/rfc2822#appendix-A.5
     */
    @Test
    public void rfc2822a5() throws Exception {
        String raw =
            "From: Pete(A wonderful \\) chap) <pete(his account)@(comment)silly.test(his host)>\n" +
            "To: Chris <c@(xxx bbb)public.example>,\n" +
            "         joe@example.org,\n" +
            "  John <jdoe@one.test> (my dear friend); (the end of the group)\n" +
            "Cc:(Empty list)(start)Undisclosed recipients  :(nobody(that I know))  ;\n" +
            "Date: Thu,\n" +
            "      13\n" +
            "        Feb\n" +
            "          1969\n" +
            "      23:32\n" +
            "               -0330 (Newfoundland Time)\n" +
            "Message-ID:              <testabcd.1234@silly.test>\n" +
            "\n" +
            "Testing.";

        ParsedMessage msg = new ParsedMessage(raw.getBytes(), false);
        List<IndexDocument> docs = msg.getLuceneDocuments();
        Assert.assertEquals(1, docs.size());
        Document doc = (Document) docs.get(0).getWrappedDocument();

        RFC822AddressTokenStream from = (RFC822AddressTokenStream) doc.getField(
                LuceneFields.L_H_FROM).tokenStreamValue();
        Assert.assertEquals(Arrays.asList("pete", "wonderful", "chap", "pete",
                "his", "account", "comment", "silly.test", "his", "host",
                "pete@silly.test", "pete", "@silly.test", "silly.test"),
                from.getAllTokens());

        RFC822AddressTokenStream to = (RFC822AddressTokenStream) doc.getField(
                LuceneFields.L_H_TO).tokenStreamValue();
        Assert.assertEquals(Arrays.asList("chris", "c@", "c", "xxx", "bbb",
                "public.example", "joe@example.org", "joe", "@example.org",
                "example.org", "example", "@example", "john", "jdoe@one.test",
                "jdoe", "@one.test", "one.test", "my", "dear", "friend", "the",
                "end", "of", "the", "group", "c@public.example", "c",
                "@public.example", "public.example"),
                to.getAllTokens());

        RFC822AddressTokenStream cc = (RFC822AddressTokenStream) doc.getField(
                LuceneFields.L_H_CC).tokenStreamValue();
        Assert.assertEquals(Arrays.asList("empty", "list", "start",
                "undisclosed", "recipients", "nobody", "that", "know"),
                cc.getAllTokens());

        RFC822AddressTokenStream xEnvFrom = (RFC822AddressTokenStream) doc.getField(
                LuceneFields.L_H_X_ENV_FROM).tokenStreamValue();
        Assert.assertEquals(0, xEnvFrom.getAllTokens().size());

        RFC822AddressTokenStream xEnvTo = (RFC822AddressTokenStream) doc.getField(
                LuceneFields.L_H_X_ENV_TO).tokenStreamValue();
        Assert.assertEquals(0, xEnvTo.getAllTokens().size());
    }

}
