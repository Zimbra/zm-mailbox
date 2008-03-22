package com.zimbra.cs.account.ldap;

public class LdapFilter {

    private static final String FILTER_ACCOUNT_OBJECTCLASS = "(objectclass=zimbraAccount)";
    private static final String FILTER_CALENDAR_RESOURCE_OBJECTCLASS = "(objectclass=zimbraCalendarResource)";
    private static final String FILTER_DISTRIBUTION_LIST_OBJECTCLASS = "(objectclass=zimbraDistributionList)";
    
    public static String accountByForeignPrincipal(String foreignPrincipal) {
        return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String accountById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String accountByName(String name) {
        return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String adminAccountByName(String namingRdnAttr, String name) {
        return "(&(" + namingRdnAttr + "=" + name + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    public static String calendarResourceById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    public static String calendarResourceByName(String name) {
        return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    public static String distributionListById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    public static String distributionListByName(String name) {
        return "(&(zimbraMailAlias=" + name + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
