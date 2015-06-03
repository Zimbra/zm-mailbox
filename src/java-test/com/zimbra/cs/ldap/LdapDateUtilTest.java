package com.zimbra.cs.ldap;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Test;

public class LdapDateUtilTest {

    @Test
    public void parseGeneralizedTime() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2007, 2, 18, 5, 1, 24);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124Z"));

        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2007, 2, 18, 5, 1, 24);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124Z"));

        cal.clear();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        cal.set(2007, 2, 18, 5, 1, 24);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124Z"));

        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.0Z"));

        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.00Z"));
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.000Z"));

        cal.set(Calendar.MILLISECOND, 100);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.1Z"));

        cal.set(Calendar.MILLISECOND, 310);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.31Z"));

        cal.set(Calendar.MILLISECOND, 597);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.597Z"));

        cal.set(Calendar.MILLISECOND, 478);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.4782Z"));

        cal.set(Calendar.MILLISECOND, 712);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.71288Z"));

        cal.set(Calendar.MILLISECOND, 876);
        Assert.assertEquals(cal.getTime(), LdapDateUtil.parseGeneralizedTime("20070318050124.876999Z"));
    }


}
