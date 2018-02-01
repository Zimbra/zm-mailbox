/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.ldap.LdapDateUtil;
import com.zimbra.cs.ldap.LdapUtil;

public class GalSyncToken {
    public GalSyncToken(String token) {
        parse(token);
        parseLdapTimestamp();
    }

    public GalSyncToken(String ldapTs, String accountId, int changeId) {
        mLdapTimestamp = ldapTs;
        mChangeIdMap = new HashMap<String,String>();
        mChangeIdMap.put(accountId, "" + changeId);
    }

    private String mLdapTimestamp;
    private HashMap<String,String> mChangeIdMap;
    private String intLdapCreateDateTs = "";
    private String extLdapCreateDateTs = "";
    private String intMaxModificationDateLdapTs = "";
    private String extMaxModificationDateLdapTs = "";
    private int intLdapMatchCount = 0;
    private int extLdapMatchCount = 0;
    private boolean intLdapHasMore = true;
    private boolean extLdapHasMore = true;

    private void parse(String token) {
        mChangeIdMap = new HashMap<String,String>();
        int pos = token.indexOf(':');
        if (pos == -1) {
            // old style LDAP timestamp token
            mLdapTimestamp = token;
            return;
        }
        mLdapTimestamp = token.substring(0, pos);
        boolean finished = false;
        while (!finished) {
            token = token.substring(pos+1);
            int sep = token.indexOf(':');
            if (sep == -1)
                return;
            String key = token.substring(0, sep);
            String value = null;
            pos = token.indexOf(':', sep+1);
            if (pos == -1) {
                finished = true;
                value = token.substring(sep+1);
            } else {
                value = token.substring(sep+1, pos);
            }
            mChangeIdMap.put(key, value);
        }
    }

    private void parseLdapTimestamp() {
      if (!StringUtils.isEmpty(mLdapTimestamp)) {
         int pos = mLdapTimestamp.indexOf('_');
         if ( pos != -1) {
            String[] parsedToken = mLdapTimestamp.split("_");
            if (parsedToken.length >= 4) {
                intLdapCreateDateTs = parsedToken[0];
                intLdapMatchCount = Integer.parseInt(parsedToken[1]);
                intLdapHasMore = BooleanUtils.toBoolean(Integer.parseInt(parsedToken[2]));
                intMaxModificationDateLdapTs = parsedToken[3];
            }

            if (parsedToken.length == 8) {
                extLdapCreateDateTs = parsedToken[4];
                extLdapMatchCount = Integer.parseInt(parsedToken[5]);
                extLdapHasMore = BooleanUtils.toBoolean(Integer.parseInt(parsedToken[6]));
                extMaxModificationDateLdapTs = parsedToken[7];
            }
         } //else its internal gal sync only.no ldap sync.
      }
    }

    public String getIntMaxLdapTs() {
        return intMaxModificationDateLdapTs;
    }

    public void setIntMaxLdapTs(String intMaxLdapTs) {
        this.intMaxModificationDateLdapTs = intMaxLdapTs;
    }

    public String getExtMaxLdapTs() {
        return extMaxModificationDateLdapTs;
    }

    public void setExtMaxLdapTs(String extMaxLdapTs) {
        this.extMaxModificationDateLdapTs = extMaxLdapTs;
    }

    public String getLdapTimestamp() {
        return mLdapTimestamp;
    }

    public String getLdapTimestamp(String format) throws ServiceException {
        return getLdapTimestamp(format, mLdapTimestamp);
    }

    public String getLdapTimestamp(String format, String ldapTimestamp) throws ServiceException {
        // mLdapTimestamp should be always in this format
        SimpleDateFormat fmt = new SimpleDateFormat(format);
        Date ts = LdapDateUtil.parseGeneralizedTime(ldapTimestamp);
        if (ts == null) {
            return ldapTimestamp;
        } else {
            if (format.endsWith("'Z'")) {
                //make sure we're returning the correct timezone
                //we previously used SimpleDateFormat on both ends; so we were using local timezone but ignoring it
                fmt.setTimeZone(TimeZone.getTimeZone("Zulu"));
            }
            return fmt.format(ts);
        }
    }

    public String getIntLdapTs() {
        return intLdapCreateDateTs;
    }

    public void setIntLdapTs(String intLdapTs) {
       this.intLdapCreateDateTs = intLdapTs;
    }

    public String getExtLdapTs() {
       return extLdapCreateDateTs;
    }

    public void setExtLdapTs(String extLdapTs) {
       this.extLdapCreateDateTs = extLdapTs;
    }

    public int getIntLdapMatchCount() {
       return intLdapMatchCount;
    }

    public void setIntLdapMatchCount(int intLdapMatchCount) {
       this.intLdapMatchCount = intLdapMatchCount;
    }

    public int getExtLdapMatchCount() {
       return extLdapMatchCount;
    }

    public void setExtLdapMatchCount(int extLdapMatchCount) {
      this.extLdapMatchCount = extLdapMatchCount;
    }

    public boolean intLdapHasMore() {
        return intLdapHasMore;
    }

    public void setIntLdapHasMore(boolean intLdapHasMore) {
        this.intLdapHasMore = intLdapHasMore;
    }

    public boolean extLdapHasMore() {
         return extLdapHasMore;
    }

    public void setExtLdapHasMore(boolean extLdapHasMore) {
        this.extLdapHasMore = extLdapHasMore;
    }

    public int getChangeId(String accountId) {
        String cid = mChangeIdMap.get(accountId);
        if (cid != null)
            return Integer.parseInt(cid);
        return 0;
    }

    public boolean doMailboxSync() {
        return mLdapTimestamp.length() == 0 || mChangeIdMap.size() > 0;
    }

    public boolean isEmpty() {
        return mLdapTimestamp.length() == 0 && mChangeIdMap.size() == 0;
    }

    public void merge(GalSyncToken that) {
        ZimbraLog.gal.debug("merging token %s with %s", this, that);
        mLdapTimestamp = LdapUtil.getEarlierTimestamp(this.mLdapTimestamp, that.mLdapTimestamp);
        for (String aid : that.mChangeIdMap.keySet()) {
            // Get higher modsequence in the merged token if ldaptime stamps same.
            // if that = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:11"
            // if this = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:12"
            // then merged = "20180131045916.000Z:b1010a37-e08d-45d4-b69b-1ea411a75138:12"
            String strThatVal = that.mChangeIdMap.get(aid);
            String strThisVal = this.mChangeIdMap.get(aid);
            if (StringUtils.isNotBlank(strThatVal) && StringUtils.isNotBlank(strThisVal) &&
                    StringUtils.isNumeric(strThatVal) && StringUtils.isNumeric(strThisVal)) {
                int thisVal = Integer.parseInt(this.mChangeIdMap.get(aid));
                int thatVal = Integer.parseInt(that.mChangeIdMap.get(aid));
                strThatVal = thisVal > thatVal ? strThisVal : strThatVal;
            }
            mChangeIdMap.put(aid, strThatVal);
        }
        ZimbraLog.gal.debug("result: %s", this);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(mLdapTimestamp);
        for (String aid : mChangeIdMap.keySet())
            buf.append(":").append(aid).append(":").append(mChangeIdMap.get(aid));
        return buf.toString();
    }
}
