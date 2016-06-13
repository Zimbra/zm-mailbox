/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.SignatureUtil;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.html.BrowserDefang;
import com.zimbra.cs.html.DefangFactory;

public class Signature extends AccountProperty implements Comparable {

    public Signature(Account acct, String name, String id, Map<String, Object> attrs, Provisioning prov) {
        super(acct, name, id, attrs, null, prov);
    }

    @Override
    public EntryType getEntryType() {
        return EntryType.SIGNATURE;
    }

    /**
     * this should only be used internally by the server. it doesn't modify the real id, just
     * the cached one.
     * @param id
     */
    public void setId(String id) {
        mId = id;
        getRawAttrs().put(Provisioning.A_zimbraSignatureId, id);
    }

    public static class SignatureContent {
        private String mMimeType;
        private String mContent;

        public SignatureContent(String mimeType, String content) {
            mMimeType = mimeType;
            mContent = content;
        }

        public String getMimeType() { return mMimeType; }
        public String getContent() { return mContent; }
    }

    public Set<SignatureContent> getContents() {
        Set<SignatureContent> contents = new HashSet<SignatureContent>();
        BrowserDefang defanger = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML);
        for (Iterator it = SignatureUtil.ATTR_TYPE_MAP.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();

            String content = getAttr((String)entry.getKey());
            if (content != null) {
                if (entry.getKey().equals(ZAttrProvisioning.A_zimbraPrefMailSignatureHTML)) {

                    StringReader reader = new StringReader(content);
                    try {
                        content = defanger.defang(reader, false);
                    } catch (IOException e) {
                       ZimbraLog.misc.info("Error sanitizing html signature: %s", content);
                    }

                }
                contents.add(new SignatureContent((String)entry.getValue(), content));
            }
        }
        return contents;
    }
}
