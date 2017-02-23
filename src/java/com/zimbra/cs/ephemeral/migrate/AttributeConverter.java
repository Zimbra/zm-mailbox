package com.zimbra.cs.ephemeral.migrate;

import com.zimbra.cs.ephemeral.EphemeralInput;

public abstract class AttributeConverter {

    public abstract EphemeralInput convert(String attrName, Object ldapValue);
    public abstract boolean isMultivalued();

}
