package com.zimbra.cs.account;

import java.util.Map;

public abstract class MailTarget extends NamedEntry {
    
    protected String mDomain;
    protected String mUnicodeDomain;
    protected String mUnicodeName;
    
    public MailTarget(String name, String id, Map<String,Object> attrs, Map<String, Object> defaults) {
        super(name, id, attrs, defaults);
        int index = name.indexOf('@');
        if (index != -1)  {
            String local = name.substring(0, index);
            mDomain = name.substring(index+1);
            mUnicodeDomain = IDNUtil.toUnicodeDomainName(mDomain);
            mUnicodeName = local + "@" + mUnicodeDomain;
        } else
            mUnicodeName = name;
    }

    /**
     * @return the domain name for this account (foo.com), or null if an admin account. 
     */
    public String getDomainName() {
        return mDomain;
    }
    
    public String getUnicodeDomainName() {
        return mUnicodeDomain;
    }
    
    public String getUnicodeName() {
        return mUnicodeName;
    }

}
