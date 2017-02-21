/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.common.util.StringUtil;

public class ZGetMessageParams {

    private String mId;
    private boolean mMarkRead;
    private boolean mWantHtml;
    private boolean mNeuterImages;
    private boolean mRawContent;
    private String mPart;
    private Integer mMax;
    private String mRequestHeaders;
    
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
    
    public Integer getMax() {
        return mMax;
    }
    
    public void setMax(Integer max) {
        mMax = max;
    }

    public int hashCode() {
        if (mPart != null)
            return (mId+mPart).hashCode();
        else
            return mId.hashCode();
    }

    public void setReqHeaders(String reqHeaders) {
        this.mRequestHeaders = reqHeaders;
    }

    public String getReqHeaders() {
        return this.mRequestHeaders;
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
                StringUtil.equal(this.mPart, that.mPart) &&
                this.mMax == that.mMax;
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
