package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class SearchHistoryResponse {

    @XmlElementWrapper(name=MailConstants.E_SEARCH_HISTORY)
    @XmlElement(name=MailConstants.E_SEARCH, type=String.class)
    private List<String> searches;

    public List<String> getSearches() { return searches; }
    public void setSearches(List<String> searches)  { this.searches = searches; }

}
