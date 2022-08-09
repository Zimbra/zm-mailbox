package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public final class DateTimeUtilTest {

    @Test
    public void checkWithinTimeTest() {
        Timestamp ts = new Timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(59));
        Assert.assertEquals(true, DateTimeUtil.checkWithinTime(ts, 1, TimeUnit.HOURS));
    }

    @Test
    public void checkOutsideTimeTest() {
        Timestamp ts = new Timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(61));
        Assert.assertEquals(false, DateTimeUtil.checkWithinTime(ts, 1, TimeUnit.HOURS));
    }

    @Test
    public void checkWithExactTimeTest() {
        Timestamp ts = new Timestamp(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(60));
        Assert.assertEquals(true, DateTimeUtil.checkWithinTime(ts, 1, TimeUnit.HOURS));
    }
}