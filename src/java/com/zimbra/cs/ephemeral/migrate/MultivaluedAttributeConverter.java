package com.zimbra.cs.ephemeral.migrate;


public abstract class MultivaluedAttributeConverter extends AttributeConverter {

    @Override
    public boolean isMultivalued() {
        return true;
    }
}
