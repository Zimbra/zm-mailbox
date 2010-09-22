package com.zimbra.cs.util;

import org.junit.Assert;
import org.junit.Test;

public class BuildInfoTest {

    @Test
    public void compare() throws Exception {
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10", "5.0.9"));
        Assert.assertEquals(0,
                BuildInfo.Version.compare("5.0.10", "5.0.10"));
        Assert.assertEquals(-9,
                BuildInfo.Version.compare("5.0", "5.0.9"));
        Assert.assertEquals(2,
                BuildInfo.Version.compare("5.0.10_RC1", "5.0.10_BETA3"));
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10_GA", "5.0.10_RC2"));
        Assert.assertEquals(1,
                BuildInfo.Version.compare("5.0.10", "5.0.10_RC2"));
    }

}
