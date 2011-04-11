package com.zimbra.cs.ldap;

import com.zimbra.common.service.ServiceException;

public interface IAttributes {
    public String getAttrString(String attrName) throws ServiceException;

}
