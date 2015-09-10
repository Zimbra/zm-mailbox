package com.zimbra.cs.account.auth.twofactor;

public interface AppSpecificPasswordData {

    public String getName();

    public String getPassword();

    public Long getDateCreated();

    public Long getDateLastUsed();
}