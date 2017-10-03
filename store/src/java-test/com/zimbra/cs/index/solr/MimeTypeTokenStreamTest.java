package com.zimbra.cs.index.solr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Strings;

public class MimeTypeTokenStreamTest extends SolrPluginTestBase {

    @Test
    public void testTokenize() throws Exception {
        List<String> types = Arrays.asList("image/jpeg", "text/plain", " text/foo/bar ",
                "aaa bbb ccc ddd/eee fff/ggg/hhh");
        List<String> tokens = new ArrayList<String>();
        for (String type: types) {
            tokens.addAll(getTokens("zmmimetype", type));
        }
        Assert.assertEquals(Arrays.asList("image/jpeg", "image", "any", "text/plain", "text", "any", "text/foo/bar", "text", "any",
                "aaa bbb ccc ddd/eee fff/ggg/hhh", "aaa bbb ccc ddd", "any"), tokens);
    }

    @Test
    public void testLimit() throws Exception {
        Assert.assertEquals(Arrays.asList("none"), getTokens("zmmimetype", "x"));
        Assert.assertEquals(Arrays.asList("none"), getTokens("zmmimetype", Strings.repeat("x", 257)));
        
        /* Before SOLR integration, there was also test for limiting the # of tokens generated.
         * It no longer applies because we are using a multivalued field.
         */
    }

}
