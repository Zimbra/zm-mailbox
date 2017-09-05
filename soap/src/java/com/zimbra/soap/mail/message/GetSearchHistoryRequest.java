package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_SEARCH_HISTORY_REQUEST)
public class GetSearchHistoryRequest {

    public GetSearchHistoryRequest() {}

    public GetSearchHistoryRequest(int limit) {
        this.limit = limit;
    }

    /**
     * @zm-api-field-description The maximum number of results to return. Defaults to 5 if not specified; capped at 100.
     */
    @XmlAttribute(name=MailConstants.A_LIMIT /* limit */, required=false)
    private Integer limit;

    public void setLimit(int limit) { this.limit = limit; }
    public Integer getLimit() { return limit; }
}
