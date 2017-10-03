package com.zimbra.cs.index.solr;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.mime.ParsedMessage;
public class RFC822AddressTokenizerTest extends SolrPluginTestBase {

    @Test
    public void single() throws Exception {
        String src = "user@domain.com";
        Assert.assertEquals(Arrays.asList("user@domain.com", "user", "@domain.com", "domain.com", "domain", "@domain"),
                getTokensWithoutReversals("zmaddress", src));

        src = "\"Tim Brown\" <first.last@sub.domain.com>";
        Assert.assertEquals(Arrays.asList("tim", "brown", "first.last@sub.domain.com", "first.last", "first", "last",
                "@sub.domain.com", "sub.domain.com", "domain", "@domain"), getTokensWithoutReversals("zmaddress", src));
    }

    @Test
    public void multi() throws Exception {
        String src = "\"User 1\" <user.1@zimbra.com>, \"User Two\" <user.2@zimbra.com>, \"User Three\" <user.3@zimbra.com>";

        Assert.assertEquals(Arrays.asList(
                "user", "1", "user.1@zimbra.com", "user.1", "user", "1",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra",
                "user", "two", "user.2@zimbra.com", "user.2", "user", "2",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra",
                "user", "three", "user.3@zimbra.com", "user.3", "user", "3",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra"),
                getTokensWithoutReversals("zmaddress", src));
    }

    @Test
    public void comment() throws Exception {
        String src = "Pete(A wonderful \\) chap) <pete(his account)@silly.test(his host)>";
        Assert.assertEquals(Arrays.asList("pete", "a", "wonderful", "chap", "pete", "his", "account", "@silly.test",
                "his", "host", "pete@silly.test", "pete", "@silly.test", "silly.test"),
                getTokensWithoutReversals("zmaddress", src));
    }

    @Test
    public void topPrivateDomain() throws Exception {
        String src = "support@zimbra.com";
        Assert.assertEquals(Arrays.asList("support@zimbra.com", "support", "@zimbra.com", "zimbra.com", "zimbra",
                "@zimbra"), getTokensWithoutReversals("zmaddress", src));

        src = "support@zimbra.vmware.co.jp";
        Assert.assertEquals(Arrays.asList("support@zimbra.vmware.co.jp", "support", "@zimbra.vmware.co.jp",
                "zimbra.vmware.co.jp", "vmware", "@vmware"), getTokensWithoutReversals("zmaddress", src));

        src = "test@co.jp";
        Assert.assertEquals(Arrays.asList("test@co.jp", "test", "@co.jp", "co.jp"),
                getTokensWithoutReversals("zmaddress", src));
    }

    @Test
    public void limit() throws Exception {
        String src = "<" + Strings.repeat("x.", 600) + "x@zimbra.com>";
        Assert.assertEquals(512, getTokensWithoutReversals("zmaddress", src).size());
    }

    @Test
    public void japanese() throws Exception {
        String src = "=?utf-8?B?5qOu44CA5qyh6YOO?= <jiro.mori@zimbra.com>";
        Assert.assertEquals(Arrays.asList("\u68ee", "\u6b21\u90ce", "jiro.mori@zimbra.com", "jiro.mori", "jiro", "mori",
                "@zimbra.com", "zimbra.com", "zimbra", "@zimbra"),  getTokensWithoutReversals("zmaddress", src));
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
        SolrInputDocument doc = docs.get(0).toInputDocument();

        Assert.assertEquals(Arrays.asList("pete", "a", "wonderful", "chap", "pete", "his", "account", "comment",
                "silly.test", "his", "host", "pete@silly.test", "pete", "@silly.test", "silly.test"),
                getTokensWithoutReversals("zmaddress", (String) doc.getFieldValue("from")));

        Assert.assertEquals(Arrays.asList("chris", "c@", "c", "xxx", "bbb", "public.example", "joe@example.org", "joe",
                "@example.org", "example.org", "example", "@example", "john", "jdoe@one.test", "jdoe", "@one.test",
                "one.test", "my", "dear", "friend", "the", "end", "of", "the", "group", "c@public.example", "c",
                "@public.example", "public.example"),
                getTokensWithoutReversals("zmaddress", (String) doc.getFieldValue("to")));

        Assert.assertEquals(Arrays.asList("empty", "list", "start", "undisclosed", "recipients", "nobody", "that", "i",
                "know"),
                getTokensWithoutReversals("zmaddress", (String) doc.getFieldValue("cc")));

        assertTrue(Strings.isNullOrEmpty((String) doc.getFieldValue("env_from")));
        assertTrue(Strings.isNullOrEmpty((String) doc.getFieldValue("env_to")));
    }


}
