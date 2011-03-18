package com.zimbra.cs.filter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link FilterUtil}.
 */
public class FilterUtilTest {

    @Test
    public void truncateBody() throws Exception {
        // truncate a body containing a multi-byte char
        String body = FilterUtil.truncateBodyIfRequired("Andr\u00e9", 5);

        Assert.assertTrue("truncated body should not have a partial char at the end", "Andr".equals(body));
    }
}
