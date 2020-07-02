/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.voice.type.CallLogItem;
import com.zimbra.soap.voice.type.VoiceCallItem;
import com.zimbra.soap.voice.type.VoiceFolderSummary;
import com.zimbra.soap.voice.type.VoiceMailItem;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceConstants.E_SEARCH_VOICE_RESPONSE)
public class SearchVoiceResponse {

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Actual sortBy used by the server
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=true)
    private String sortBy;

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description Integer specifying the 0-based offset into the results list returned as the first
     * result for this search operation.  It is always the same as the offset attribute in the request.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=true)
    private int offset;

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Set if there are more search results remaining
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=true)
    private ZmBoolean more;

    /**
     * @zm-api-field-description Voice folder summary
     * <br />
     * For certain search combinations, server is able to compute a folder inventory.  If folder inventory is
     * available on the server, a <b>&lt;vfi></b> tag is included for that folder.
     * <br />
     * Given the lack of a notification mechanism in the voice API infrastructure, clients can use this information to
     * refresh the message count in the accordion if it is available.
     */
    @XmlElement(name=VoiceConstants.E_VOICE_FOLDER_INVENTORY /* vfi */, required=false)
    private List<VoiceFolderSummary> voiceFolderSummaries = Lists.newArrayList();

    /**
     * @zm-api-field-description Matching items
     */
    @XmlElements({
        @XmlElement(name=VoiceConstants.E_VOICEMSG /* vm */, type=VoiceMailItem.class),
        @XmlElement(name=VoiceConstants.E_CALLLOG /* cl */, type=CallLogItem.class)
    })
    private List<VoiceCallItem> elements = Lists.newArrayList();

    public SearchVoiceResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setOffset(int offset) { this.offset = offset; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setVoiceFolderSummaries(Iterable <VoiceFolderSummary> voiceFolderSummaries) {
        this.voiceFolderSummaries.clear();
        if (voiceFolderSummaries != null) {
            Iterables.addAll(this.voiceFolderSummaries, voiceFolderSummaries);
        }
    }

    public void addVoiceFolderSummary(VoiceFolderSummary voiceFolderSummary) {
        this.voiceFolderSummaries.add(voiceFolderSummary);
    }

    public void setElements(Iterable <VoiceCallItem> elements) {
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements, elements);
        }
    }

    public void addElement(VoiceCallItem element) {
        this.elements.add(element);
    }

    public String getSortBy() { return sortBy; }
    public int getOffset() { return offset; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public List<VoiceFolderSummary> getVoiceFolderSummaries() {
        return voiceFolderSummaries;
    }
    public List<VoiceCallItem> getElements() {
        return elements;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("voiceFolderSummaries", voiceFolderSummaries)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
