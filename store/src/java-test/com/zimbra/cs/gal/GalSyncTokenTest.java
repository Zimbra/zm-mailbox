/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.gal;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;

public class GalSyncTokenTest {

    @Test
    public void testSyncTokenTimestampZulu() throws ServiceException {
        String dateWithHour = "20150610220447";
        String fullTimestamp = dateWithHour + ".000Z";
        GalSyncToken token = new GalSyncToken(fullTimestamp);
        Assert.assertEquals(fullTimestamp, token.getLdapTimestamp());
        Assert.assertEquals(dateWithHour + "Z", token.getLdapTimestamp("yyyyMMddHHmmss'Z'"));
    }

    @Test
    public void testSyncTokenMerge() throws ServiceException {
        String t1 = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:11";
        String t2 = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:12";
        GalSyncToken token1 = new GalSyncToken(t1);
        GalSyncToken token2 = new GalSyncToken(t2);
        token2.merge(token1);
        Assert.assertEquals("GalSync tokens not merged correctly.", t2, token2.toString());
        t1 = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:13";
        t2 = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:14";
        token1 = new GalSyncToken(t1);
        token2 = new GalSyncToken(t2);
        token1.merge(token2);
        Assert.assertEquals("GalSync tokens not merged correctly.", t2, token1.toString());
    }
}
