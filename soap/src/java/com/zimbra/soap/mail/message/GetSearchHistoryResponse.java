package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlRootElement(name=MailConstants.E_GET_SEARCH_HISTORY_RESPONSE)
public class GetSearchHistoryResponse extends SearchHistoryResponse {

    public GetSearchHistoryResponse() {}
}
