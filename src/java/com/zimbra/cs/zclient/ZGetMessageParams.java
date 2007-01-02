/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.util.StringUtil;

public class ZGetMessageParams {

    private String mId;
    private boolean mMarkRead;
    private boolean mWantHtml;
    private boolean mNeuterImages;
    private boolean mRawContent;
    private String mPart;

    public ZGetMessageParams() { }

    public boolean isWantHtml() {
        return mWantHtml;
    }

    public void setWantHtml(boolean wantHtml) {
        mWantHtml = wantHtml;
    }


    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public boolean isMarkRead() {
        return mMarkRead;
    }

    public void setMarkRead(boolean markRead) {
        mMarkRead = markRead;
    }

    public boolean isNeuterImages() {
        return mNeuterImages;
    }

    public void setNeuterImages(boolean neuterImages) {
        this.mNeuterImages = neuterImages;
    }

    public boolean isRawContent() {
        return mRawContent;
    }

    public void setRawContent(boolean rawContent) {
        mRawContent = rawContent;
    }

    public String getPart() {
        return mPart;
    }

    public void setPart(String part) {
        mPart = part;
    }

    public int hashCode() {
        if (mPart != null)
            return (mId+mPart).hashCode();
        else
            return mId.hashCode();
    }

    /**
     *
     * @return true if get message params are equal.
     */
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (! (obj instanceof ZGetMessageParams)) return false;

        ZGetMessageParams that = (ZGetMessageParams) obj;

        return this.mNeuterImages == that.mNeuterImages &&
                this.mRawContent == that.mRawContent &&
                this.mWantHtml == that.mWantHtml &&
                this.mMarkRead == that.mMarkRead &&
                StringUtil.equal(this.mId, that.mId) &&
                StringUtil.equal(this.mPart, that.mPart);
    }

    public ZGetMessageParams(ZGetMessageParams that) {
        this.mId = that.mId;
        this.mWantHtml = that.mWantHtml;
        this.mMarkRead = that.mMarkRead;
        this.mNeuterImages = that.mNeuterImages;
        this.mRawContent = that.mRawContent;
        this.mPart = that.mPart;
    }
}
