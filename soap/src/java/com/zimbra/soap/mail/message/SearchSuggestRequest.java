package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_SEARCH_SUGGEST_REQUEST)
public class SearchSuggestRequest {

    public SearchSuggestRequest() {}

    public SearchSuggestRequest(String query) {
        this.query = query;
    }

    public SearchSuggestRequest(String query, int limit) {
        this.query = query;
        this.limit = limit;
    }

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description The search query to autocomplete
     */
    @XmlAttribute(name=MailConstants.A_QUERY, required=true)
    private String query;

    /**
     * @zm-api-field-description The maximum number of results to return. Ddefaults to 5 if not specified; capped at 100.
     */
    @XmlAttribute(name=MailConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    public void setLimit(int limit) { this.limit = limit; }
    public Integer getLimit() { return limit; }

    public void setQuery(String query) { this.query = query; }
    public String getQuery() { return query; }

}
