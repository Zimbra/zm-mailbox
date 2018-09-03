package com.zimbra.cs.ephemeral.migrate;

import com.zimbra.common.service.ServiceException;

public class InvalidAttributeException extends ServiceException {
    private String attribute;

    public InvalidAttributeException (String attribute) {
        super(String.format("Invalid attribute specified: %s", attribute), ServiceException.NOT_FOUND, RECEIVERS_FAULT);
        this.attribute = attribute;
    }

    public String getAttribute() {
        return this.attribute;
    }
}
