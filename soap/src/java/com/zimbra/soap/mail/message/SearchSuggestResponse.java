package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_SEARCH_SUGGEST_RESPONSE)
public class SearchSuggestResponse extends SearchHistoryResponse {
}
