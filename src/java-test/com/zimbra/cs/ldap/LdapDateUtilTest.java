/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
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
