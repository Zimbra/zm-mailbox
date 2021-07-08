/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.EntrySearchFilterInfo;
import com.zimbra.soap.account.type.MemberOfSelector;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.GalSearchType;
import com.zimbra.soap.type.ZmBoolean;

// MailConstants.A_SEARCH_TYPES (types) Used internally when calling SearchParams.parse, forced to be
// MailItem.Type.CONTACT
//
// soap.txt implies that MailConstants.A_GROUPBY (groupBy) is used but doesn't document it.  Elsewhere, groupBy
// is mentioned as being deprecated - being replaced by "types" - assuming it doesn't make sense.
//
// Removed following attributes which SearchParams.parse looks for as assuming they don't make sense for GAL:
// MailConstants.A_FETCH (fetch), MailConstants.A_MAX_INLINED_LENGTH (max) MailConstants.A_WANT_HTML (html),
// MailConstants.A_NEUTER (neuter), MailConstants.A_RECIPIENTS (recip), MailConstants.A_PREFETCH (prefetch),
// MailConstants.A_RESULT_MODE (resultMode), MailConstants.A_FIELD (field), MailConstants.A_HEADER (header),
// MailConstants.E_CAL_TZ (tz)

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Search Global Address List (GAL)
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SEARCH_GAL_REQUEST)
public class SearchGalRequest {

    /**
     * @zm-api-field-tag gal-search-ref-DN
     * @zm-api-field-description If set then search GAL by this ref, which is a dn.  If specified then
     * "name" attribute is ignored.
     */
    @XmlAttribute(name=AccountConstants.A_REF /* ref */, required=false)
    private String ref;

    /**
     * @zm-api-field-tag query-string
     * @zm-api-field-description Query string.  Note: ignored if <b>{gal-search-ref-DN}</b> is specified
     */
    @XmlAttribute(name=AccountConstants.E_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-description type of addresses to auto-complete on
     * <ul>
     * <li>     "account" for regular user accounts, aliases and distribution lists
     * <li>     "resource" for calendar resources
     * <li>     "group" for groups
     * <li>     "all" for combination of all types
     * </ul>
     * if omitted, defaults to "all"
     */
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=false)
    private GalSearchType type;

    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description flag whether the <b>{exp}</b> flag is needed in the response for group entries.
     * Default is unset.
     */
    @XmlAttribute(name=AccountConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * @zm-api-field-tag need-isOwner-flag-in-response
     * @zm-api-field-description Set this if the "isOwner" flag is needed in the response for group entries.
     * Default is unset.
     */
    @XmlAttribute(name=AccountConstants.A_NEED_IS_OWNER /* needIsOwner */, required=false)
    private ZmBoolean needIsOwner;

    /**
     * @zm-api-field-tag need-isMember-all|directOnly|none
     * @zm-api-field-description Specify if the "isMember" flag is needed in the response for group entries.
     * <table>
     * <tr> <td> <b>all</b> </td> <td> the isMember flag returned is set if the user is a direct or indirect member of
     *                                 the group, otherwise it is unset </td> </tr>
     * <tr> <td> <b>directOnly</b> </td> <td> the isMember flag returned is set if the user is a direct member of the
     *                                 group, otherwise it is unset </td> </tr>
     * <tr> <td> <b>none (default)</b> </td> <td> The isMember flag is not returned </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AccountConstants.A_NEED_IS_MEMBER /* needIsMember */, required=false)
    private MemberOfSelector needIsMember;

    /**
     * @zm-api-field-tag internal-use-only
     * @zm-api-field-description Internal attr, for proxied GSA search from GetSMIMEPublicCerts only
     */
    @XmlAttribute(name=AccountConstants.A_NEED_SMIME_CERTS /* needSMIMECerts */, required=false)
    private ZmBoolean needSMIMECerts;

    /**
     * @zm-api-field-tag gal-account-id
     * @zm-api-field-description GAL Account ID
     */
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    // TODO: Is this appropriate to SearchGalRequest?
    /**
     * @zm-api-field-tag
     * @zm-api-field-description "Quick" flag.
     * <br />
     * For performance reasons, the index system accumulates messages with not-indexed-yet state until a certain
     * threshold and indexes them as a batch. To return up-to-date search results, the index system also indexes those
     * pending messages right before a search. To lower latencies, this option gives a hint to the index system not to
     * trigger this catch-up index prior to the search by giving up the freshness of the search results, i.e. recent
     * messages may not be included in the search results.
     */
    @XmlAttribute(name=MailConstants.A_QUICK /* quick */, required=false)
    private ZmBoolean quick;

    // Based on SortBy which is NOT an enum and appears to support runtime construction
    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description SortBy setting.
     * <br />
     * Default value is <b>"dateDesc"</b>
     * <br />
     * Possible values:
     * <b>none|dateAsc|dateDesc|subjAsc|subjDesc|nameAsc|nameDesc|rcptAsc|rcptDesc|attachAsc|attachDesc|flagAsc|flagDesc|
      priorityAsc|priorityDesc</b>
     * If <b>{sort-by}</b> is "none" then cursors MUST NOT be used, and some searches are impossible (searches that
     * require intersection of complex sub-ops). Server will throw an IllegalArgumentException if the search is
     * invalid.
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;


    /**
     * @zm-api-field-tag query-limit
     * @zm-api-field-description The maximum number of results to return. It defaults to 10 if not specified, and is
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-tag query-offset
     * @zm-api-field-description Specifies the 0-based offset into the results list to return as the first result for
     * this search operation.
     * <br />
     * For example, limit=10 offset=30 will return the 31st through 40th results inclusive.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-tag locale-name
     * @zm-api-field-description Client locale identification.
     * <br />
     * Value is of the form LL-CC[-V+] where:
     * <br />
     * LL is two character language code
     * <br />
     * CC is two character country code
     * <br />
     * V+ is optional variant identifier string
     * <br />
     * <br />
     * See:
     * <br />
     * ISO Language Codes: http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt
     * <br />
     * ISO Country Codes: http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html
     */
    @XmlElement(name=MailConstants.E_LOCALE /* locale */, required=false)
    private String locale;

    /**
     * @zm-api-field-description Cursor specification
     */
    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private CursorInfo cursor;

    /**
     * @zm-api-field-description query string
     */
    @XmlElement(name=MailConstants.E_QUERY /* query */, required=false)
    private String query; 

    /**
     * @zm-api-field-description Search filter specification
     */
    @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER /* searchFilter */, required=false)
    private EntrySearchFilterInfo searchFilter;

    public SearchGalRequest() {
    }

    public void setRef(String ref) { this.ref = ref; }
    public void setName(String name) { this.name = name; }
    public void setType(GalSearchType type) { this.type = type; }
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    public void setNeedIsOwner(Boolean needIsOwner) { this.needIsOwner = ZmBoolean.fromBool(needIsOwner); }
    public void setNeedIsMember(MemberOfSelector needIsMember) { this.needIsMember = needIsMember; }
    public void setNeedSMIMECerts(Boolean needSMIMECerts) { this.needSMIMECerts = ZmBoolean.fromBool(needSMIMECerts); }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setQuick(Boolean quick) { this.quick = ZmBoolean.fromBool(quick); }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setCursor(CursorInfo cursor) { this.cursor = cursor; }
    public void setSearchFilter(EntrySearchFilterInfo searchFilter) { this.searchFilter = searchFilter; }
    public void setQuery(String query) { this.query = query; };
    public String getRef() { return ref; }
    public String getName() { return name; }
    public GalSearchType getType() { return type; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public Boolean getNeedIsOwner() { return ZmBoolean.toBool(needIsOwner); }
    public MemberOfSelector getNeedIsMember() { return needIsMember; }
    public Boolean getNeedSMIMECerts() { return ZmBoolean.toBool(needSMIMECerts); }
    public String getGalAccountId() { return galAccountId; }
    public Boolean getQuick() { return ZmBoolean.toBool(quick); }
    public String getSortBy() { return sortBy; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public String getLocale() { return locale; }
    public CursorInfo getCursor() { return cursor; }
    public EntrySearchFilterInfo getSearchFilter() { return searchFilter; }
    public String getQuery() { return query; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("ref", ref)
            .add("name", name)
            .add("type", type)
            .add("needCanExpand", needCanExpand)
            .add("needIsOwner", needIsOwner)
            .add("needIsMember", needIsMember)
            .add("needSMIMECerts", needSMIMECerts)
            .add("galAccountId", galAccountId)
            .add("quick", quick)
            .add("sortBy", sortBy)
            .add("limit", limit)
            .add("offset", offset)
            .add("locale", locale)
            .add("cursor", cursor)
            .add("searchFilter", searchFilter)
            .add("query", query);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
