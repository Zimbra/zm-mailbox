package com.zimbra.cs.account.ldap;

public class LdapFilter {

    private static final String FILTER_ACCOUNT_OBJECTCLASS = "(objectclass=zimbraAccount)";
    private static final String FILTER_CALENDAR_RESOURCE_OBJECTCLASS = "(objectclass=zimbraCalendarResource)";
    private static final String FILTER_DISTRIBUTION_LIST_OBJECTCLASS = "(objectclass=zimbraDistributionList)";
    
    /*
     * account
     */
    public static String allNonSystemAccounts() {
        StringBuilder buf = new StringBuilder();
        buf.append("(&");
        buf.append("(!(zimbraIsSystemResource=TRUE))");
        buf.append("(objectclass=zimbraAccount)(!(objectclass=zimbraCalendarResource))");
        buf.append(")");

        return buf.toString();
    }
    
    public static String accountByForeignPrincipal(String foreignPrincipal) {
        return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String accountById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String accountByName(String name) {
        return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    public static String adminAccountByRDN(String namingRdnAttr, String name) {
        return "(&(" + namingRdnAttr + "=" + name + ")" + FILTER_ACCOUNT_OBJECTCLASS + ")";
    }
    
    /*
     * calendar resource
     */
    public static String calendarResourceByForeignPrincipal(String foreignPrincipal) {
        return "(&(zimbraForeignPrincipal=" + foreignPrincipal + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    public static String calendarResourceById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    public static String calendarResourceByName(String name) {
        return "(&(|(zimbraMailDeliveryAddress=" + name + ")(zimbraMailAlias=" + name + "))" + FILTER_CALENDAR_RESOURCE_OBJECTCLASS + ")";
    }
    
    /*
     * cos
     */
    public static String allCoses() {
        return "(objectclass=zimbraCOS)";
    }
    
    public static String cosById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraCOS))";
    }
    
    public static String cosesByMailHostPool(String server) {
        return "(&(objectclass=zimbraCOS)(zimbraMailHostPool=" + server + "))";
    }
    
    /*
     * data source
     */
    public static String allDataSources() {
        return "(objectclass=zimbraDataSource)";
    }
    
    public static String dataSourceById(String id) {
        return "(&(objectclass=zimbraDataSource)(zimbraDataSourceId=" + id + "))";
    }
    
    public static String dataSourceByName(String name) {
        return "(&(objectclass=zimbraDataSource)(zimbraDataSourceName=" + name + "))";
    }
    
    /*
     * distribution list
     */
    public static String distributionListById(String id) {
        return "(&(zimbraId=" + id + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    public static String distributionListByName(String name) {
        return "(&(zimbraMailAlias=" + name + ")" + FILTER_DISTRIBUTION_LIST_OBJECTCLASS + ")";
    }
    
    /*
     * domain
     */
    public static String allDomains() {
        return "(objectclass=zimbraDomain)";
    }
    
    public static String domainById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByName(String name) {
        return "(&(zimbraDomainName=" + name + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByKrb5Realm(String krb5Realm) {
        return "(&(zimbraAuthKerberos5Realm=" + krb5Realm + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainByVirtualHostame(String virtualHostname) {
        return "(&(zimbraVirtualHostname=" + virtualHostname + ")(objectclass=zimbraDomain))";
    }
    
    public static String domainLabel() {
        return "objectclass=dcObject";
    }
    
    /*
     * identity
     */
    public static String allIdentities() {
        return "(objectclass=zimbraIdentity)";
    }
    
    public static String identityByName(String name) {
        return "(&(objectclass=zimbraIdentity)(zimbraPrefIdentityName=" + name + "))";
    }
    
    /*
     * mime enrty
     */
    public static String allMimeEntries() {
        return "(objectclass=zimbraMimeEntry)";
    }
    
    public static String mimeEntryByMimeType(String mimeType) {
        return "(zimbraMimeType=" + mimeType + ")";
    }
    
    /*
     * server
     */
    public static String allServers() {
        return "(objectclass=zimbraServer)";
    }
    
    public static String serverById(String id) {
        return "(&(zimbraId=" + id + ")(objectclass=zimbraServer))";
    }
    
    public static String serverByService(String service) {
        return "(&(objectclass=zimbraServer)(zimbraServiceEnabled=" + service + "))";
    }
    
    /*
     * signature
     */
    public static String allSignatures() {
        return "(objectclass=zimbraSignature)";
    }
    
    public static String signatureById(String id) {
        return "(&(objectclass=zimbraSignature)(zimbraSignatureId=" + id +"))";
    }
    
    /*
     * zimlet
     */
    public static String allZimlets() {
        return "(objectclass=zimbraZimletEntry)";
    }

}
