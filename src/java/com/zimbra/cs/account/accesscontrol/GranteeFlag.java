package com.zimbra.cs.account.accesscontrol;

public class GranteeFlag {
    
    // allowed for admin rights
    public static final short F_ADMIN = 0x0001;
    
    public static final short F_INDIVIDUAL = 0x0002;
    public static final short F_GROUP      = 0x0004;
    public static final short F_AUTHUSER   = 0x0008;
    public static final short F_PUBLIC     = 0x0010;
}
