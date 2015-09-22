package com.zimbra.cs.account;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;

public interface TrustedDeviceToken {

    public Integer getId();

    public Long getExpires();

    public void setExpires(long expires);

    public void setDelete();

    public void encode(HttpServletResponse resp, Element el, boolean secure) throws ServiceException;

    public boolean isExpired();
}
