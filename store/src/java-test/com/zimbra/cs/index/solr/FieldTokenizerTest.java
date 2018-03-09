package com.zimbra.cs.index.solr;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;

public class FieldTokenizerTest extends SolrPluginTestBase {

    @Test
    public void tokens() throws Exception {
        StringBuilder src = new StringBuilder();
        List<String> tokens = new ArrayList<String>();
        tokens.addAll(getTokens("zmheaders",String.format("%s:%s", "test1", "val1 val2 val3    val4-test\t  val5")));
        tokens.addAll(getTokens("zmheaders",String.format("%s:%s", "#test2", "2val1 2val2:_123 2val3")));
        tokens.addAll(getTokens("zmheaders",String.format("%s:%s", "test3", "zzz")));
        tokens.addAll(getTokens("zmheaders",String.format("%s:%s", "#calendarItemClass", "public")));
        tokens.addAll(getTokens("zmheaders",String.format("%s:%s", "zimbraCalResCapacity", "10")));

        assertEquals(Arrays.asList(
            "test1:val1", "test1:val2", "test1:val3", "test1:val4", "test1:test", "test1:val5",
            "#test2:2val1", "#test2:2val2:_123", "#test2:2val3", "test3:zzz", "#calendaritemclass:public", "zimbracalrescapacity:10"),
            tokens);
    }

    @Test
    public void limit() throws Exception {
        StringBuilder src = new StringBuilder();
        src.append(String.format("%s:%s",Strings.repeat("k", 50), Strings.repeat("v", 50)));
        Assert.assertEquals(Collections.emptyList(), getTokens("zmheaders", src.toString()));

        src = new StringBuilder();
        src.append("k:");
        for (int i = 0; i < 1001; i++) {
            src.append(" v");
        }
        Assert.assertEquals(1000, getTokens("zmheaders", src.toString()).size());
    }


}
