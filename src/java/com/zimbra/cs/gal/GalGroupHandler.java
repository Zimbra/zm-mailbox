package com.zimbra.cs.gal;

import java.util.HashMap;

import javax.naming.directory.Attributes;

public interface GalGroupHandler {

    public boolean isGroup(Attributes ldapAttrs);
}
