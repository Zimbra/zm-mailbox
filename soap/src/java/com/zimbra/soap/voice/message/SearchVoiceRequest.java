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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.soap.voice.type.StorePrincipalSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Search voice messages and call logs
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceConstants.E_SEARCH_VOICE_REQUEST)
public class SearchVoiceRequest {

    /**
     * @zm-api-field-description Store Principal specification
     */
    @XmlElement(name=VoiceConstants.E_STOREPRINCIPAL /* storeprincipal */, required=false)
    private StorePrincipalSpec storePrincipal;

    /**
     * @zm-api-field-tag query-string
     * @zm-api-field-description Query [mandatory]
     * <br />
     * Currently the only supported query clauses are phone:, in:, after:, before:
     * <br />
     * <pre>
     *     phone:[(]{PHONE}[)] | ({PHONE} OR {PHONE} OR ...)
     *     in:[(]{FOLDERNAME}[)] | ({FOLDERNAME} OR {FOLDERNAME} OR ...)
     *     after:{DATE}
     *     before:{DATE}
     * </pre>
     * <table>
     * <tr> <td> <b> phone  </b> [required] </td>
     *      <td> Phone number of the account that the voicemail or calllog items are involved with.
     *           <br /> If no phone: clause is specified, no result will be returned.
     * </td> </tr>
     * <tr> <td> <b> in </b> [optional] </td>
     *      <td> Name of Folder that the voicemail or calllog items are in.
     *           <br /> If not specifed, default folders for the search are:
     *           <ul>
     *           <li> For voicemail type: <b>inbox</b> </li>
     *           <li> For calllog type: all calllog folders:<b>missedcall, answeredcall, placedcall</b> </li>
     *           <li> For trash type: <b>trash</b> </li>
     *           </ul>
     *      </td> </tr>
     * <tr> <td> <b> after </b> [optional] </td>
     *      <td> Retrieve voicemail or calllog items on or after the date.
     *           <br /> If no after: clause is specified, default is <b>01/01/1970 GMT</b>.
     *      </td> </tr>
     * <tr> <td> <b> before </b> [optional] </td>
     *      <td> Retrieve voicemail or calllog items on or before the date.
     *           <br /> If no before: clause is specified, default is the current day.
     *      </td> </tr>
     * </table>
     * Supported format for <b>DATE</b>: <b>mm/dd/yyyy</b>
     */
    @XmlAttribute(name=MailConstants.E_QUERY /* query */, required=true)
    private String query;

    /**
     * @zm-api-field-tag max-num-results
     * @zm-api-field-description The maximum number of results to return. It defaults to 10 if not specified, and is
     * capped by 1000
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-tag 0-based-offset
     * @zm-api-field-description Specifies the 0-based offset into the results list to return as the first result for
     * this search operation.
     * <br />
     * For example, limit=10 offset=30 will return the 31st through 40th results inclusive.
     * 
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-tag comma-sep-list:voicemail|calllog
     * @zm-api-field-description Comma-separated list of search types.  Legal values are: <b>voicemail|calllog</b>
     * <br /> (default is "voicemail")
     */
    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String searchTypes;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Sort by: <b>dateDesc|dateAsc|durDesc|durAsc|nameDesc|nameAsc</b> [default:"dateDesc"]
     * <ul>
     * <li> durDesc: duration decending </li>
     * <li> durAsc: deration ascending </li>
     * <li> rest are the same as those for the sortBy attribute in <b>&lt;SearchRequest xmlns="urn:zimbraMail></b> </li>
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    public SearchVoiceRequest() {
    }

    public void setStorePrincipal(StorePrincipalSpec storePrincipal) { this.storePrincipal = storePrincipal; }
    public void setQuery(String query) { this.query = query; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setSearchTypes(String searchTypes) { this.searchTypes = searchTypes; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public StorePrincipalSpec getStorePrincipal() { return storePrincipal; }
    public String getQuery() { return query; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getSearchTypes() { return searchTypes; }
    public String getSortBy() { return sortBy; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("storePrincipal", storePrincipal)
            .add("query", query)
            .add("limit", limit)
            .add("offset", offset)
            .add("searchTypes", searchTypes)
            .add("sortBy", sortBy);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
