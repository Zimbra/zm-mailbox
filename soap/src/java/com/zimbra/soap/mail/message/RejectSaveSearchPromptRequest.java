package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_REJECT_SAVE_SEARCH_PROMPT_REQUEST)
public class RejectSaveSearchPromptRequest {

    public RejectSaveSearchPromptRequest() {}

    public RejectSaveSearchPromptRequest(String query) {
        this.query = query;
    }

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description The search query for which the prompt was rejected
     */
    @XmlAttribute(name=MailConstants.A_QUERY, required=true)
    private String query;

    public void setQuery(String query) { this.query = query; }
    public String getQuery() { return query; }
}
