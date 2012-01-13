/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.account;

import com.zimbra.common.service.ServiceException;

import java.util.Arrays;

/**
 * AUTO-GENERATED. DO NOT EDIT.
 * 
 * @author schemers
 *
 */
public class ZAttrProvisioning {

    ///// BEGIN-AUTO-GEN-REPLACE

    /* build: 8.0.0_BETA1_1111 administrator 20120111-1420 */

    public static enum AccountCalendarUserType {
        RESOURCE("RESOURCE"),
        USER("USER");
        private String mValue;
        private AccountCalendarUserType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static AccountCalendarUserType fromString(String s) throws ServiceException {
            for (AccountCalendarUserType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isRESOURCE() { return this == RESOURCE;}
        public boolean isUSER() { return this == USER;}
    }

    public static enum AccountStatus {
        maintenance("maintenance"),
        pending("pending"),
        active("active"),
        closed("closed"),
        locked("locked"),
        lockout("lockout");
        private String mValue;
        private AccountStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static AccountStatus fromString(String s) throws ServiceException {
            for (AccountStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isMaintenance() { return this == maintenance;}
        public boolean isPending() { return this == pending;}
        public boolean isActive() { return this == active;}
        public boolean isClosed() { return this == closed;}
        public boolean isLocked() { return this == locked;}
        public boolean isLockout() { return this == lockout;}
    }

    public static enum AdminAccessControlMech {
        acl("acl"),
        global("global");
        private String mValue;
        private AdminAccessControlMech(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static AdminAccessControlMech fromString(String s) throws ServiceException {
            for (AdminAccessControlMech value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isAcl() { return this == acl;}
        public boolean isGlobal() { return this == global;}
    }

    public static enum AutoProvAuthMech {
        KRB5("KRB5"),
        SPNEGO("SPNEGO"),
        LDAP("LDAP"),
        PREAUTH("PREAUTH");
        private String mValue;
        private AutoProvAuthMech(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static AutoProvAuthMech fromString(String s) throws ServiceException {
            for (AutoProvAuthMech value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isKRB5() { return this == KRB5;}
        public boolean isSPNEGO() { return this == SPNEGO;}
        public boolean isLDAP() { return this == LDAP;}
        public boolean isPREAUTH() { return this == PREAUTH;}
    }

    public static enum AutoProvMode {
        EAGER("EAGER"),
        LAZY("LAZY"),
        MANUAL("MANUAL");
        private String mValue;
        private AutoProvMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static AutoProvMode fromString(String s) throws ServiceException {
            for (AutoProvMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isEAGER() { return this == EAGER;}
        public boolean isLAZY() { return this == LAZY;}
        public boolean isMANUAL() { return this == MANUAL;}
    }

    public static enum BackupMode {
        Standard("Standard"),
        Auto_Grouped("Auto-Grouped");
        private String mValue;
        private BackupMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static BackupMode fromString(String s) throws ServiceException {
            for (BackupMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isStandard() { return this == Standard;}
        public boolean isAuto_Grouped() { return this == Auto_Grouped;}
    }

    public static enum CalendarCompatibilityMode {
        standard("standard"),
        exchange("exchange");
        private String mValue;
        private CalendarCompatibilityMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static CalendarCompatibilityMode fromString(String s) throws ServiceException {
            for (CalendarCompatibilityMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isStandard() { return this == standard;}
        public boolean isExchange() { return this == exchange;}
    }

    public static enum CalResType {
        Equipment("Equipment"),
        Location("Location");
        private String mValue;
        private CalResType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static CalResType fromString(String s) throws ServiceException {
            for (CalResType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isEquipment() { return this == Equipment;}
        public boolean isLocation() { return this == Location;}
    }

    public static enum ClusterType {
        Veritas("Veritas"),
        RedHat("RedHat"),
        none("none");
        private String mValue;
        private ClusterType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ClusterType fromString(String s) throws ServiceException {
            for (ClusterType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isVeritas() { return this == Veritas;}
        public boolean isRedHat() { return this == RedHat;}
        public boolean isNone() { return this == none;}
    }

    public static enum DataSourceAuthMechanism {
        GSSAPI("GSSAPI"),
        PLAIN("PLAIN"),
        CRAM_MD5("CRAM-MD5");
        private String mValue;
        private DataSourceAuthMechanism(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DataSourceAuthMechanism fromString(String s) throws ServiceException {
            for (DataSourceAuthMechanism value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isGSSAPI() { return this == GSSAPI;}
        public boolean isPLAIN() { return this == PLAIN;}
        public boolean isCRAM_MD5() { return this == CRAM_MD5;}
    }

    public static enum DataSourceConnectionType {
        tls_if_available("tls_if_available"),
        tls("tls"),
        ssl("ssl"),
        cleartext("cleartext");
        private String mValue;
        private DataSourceConnectionType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DataSourceConnectionType fromString(String s) throws ServiceException {
            for (DataSourceConnectionType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isTls_if_available() { return this == tls_if_available;}
        public boolean isTls() { return this == tls;}
        public boolean isSsl() { return this == ssl;}
        public boolean isCleartext() { return this == cleartext;}
    }

    public static enum DistributionListSubscriptionPolicy {
        APPROVAL("APPROVAL"),
        ACCEPT("ACCEPT"),
        REJECT("REJECT");
        private String mValue;
        private DistributionListSubscriptionPolicy(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DistributionListSubscriptionPolicy fromString(String s) throws ServiceException {
            for (DistributionListSubscriptionPolicy value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isAPPROVAL() { return this == APPROVAL;}
        public boolean isACCEPT() { return this == ACCEPT;}
        public boolean isREJECT() { return this == REJECT;}
    }

    public static enum DistributionListUnsubscriptionPolicy {
        APPROVAL("APPROVAL"),
        ACCEPT("ACCEPT"),
        REJECT("REJECT");
        private String mValue;
        private DistributionListUnsubscriptionPolicy(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DistributionListUnsubscriptionPolicy fromString(String s) throws ServiceException {
            for (DistributionListUnsubscriptionPolicy value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isAPPROVAL() { return this == APPROVAL;}
        public boolean isACCEPT() { return this == ACCEPT;}
        public boolean isREJECT() { return this == REJECT;}
    }

    public static enum DomainAggregateQuotaPolicy {
        BLOCKSEND("BLOCKSEND"),
        BLOCKSENDRECEIVE("BLOCKSENDRECEIVE"),
        ALLOWSENDRECEIVE("ALLOWSENDRECEIVE");
        private String mValue;
        private DomainAggregateQuotaPolicy(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DomainAggregateQuotaPolicy fromString(String s) throws ServiceException {
            for (DomainAggregateQuotaPolicy value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBLOCKSEND() { return this == BLOCKSEND;}
        public boolean isBLOCKSENDRECEIVE() { return this == BLOCKSENDRECEIVE;}
        public boolean isALLOWSENDRECEIVE() { return this == ALLOWSENDRECEIVE;}
    }

    public static enum DomainStatus {
        maintenance("maintenance"),
        active("active"),
        closed("closed"),
        locked("locked"),
        suspended("suspended"),
        shutdown("shutdown");
        private String mValue;
        private DomainStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DomainStatus fromString(String s) throws ServiceException {
            for (DomainStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isMaintenance() { return this == maintenance;}
        public boolean isActive() { return this == active;}
        public boolean isClosed() { return this == closed;}
        public boolean isLocked() { return this == locked;}
        public boolean isSuspended() { return this == suspended;}
        public boolean isShutdown() { return this == shutdown;}
    }

    public static enum DomainType {
        alias("alias"),
        local("local");
        private String mValue;
        private DomainType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static DomainType fromString(String s) throws ServiceException {
            for (DomainType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isAlias() { return this == alias;}
        public boolean isLocal() { return this == local;}
    }

    public static enum FeatureSocialFiltersEnabled {
        Facebook("Facebook"),
        LinkedIn("LinkedIn"),
        SocialCast("SocialCast"),
        Twitter("Twitter");
        private String mValue;
        private FeatureSocialFiltersEnabled(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static FeatureSocialFiltersEnabled fromString(String s) throws ServiceException {
            for (FeatureSocialFiltersEnabled value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isFacebook() { return this == Facebook;}
        public boolean isLinkedIn() { return this == LinkedIn;}
        public boolean isSocialCast() { return this == SocialCast;}
        public boolean isTwitter() { return this == Twitter;}
    }

    public static enum FreebusyExchangeAuthScheme {
        form("form"),
        basic("basic");
        private String mValue;
        private FreebusyExchangeAuthScheme(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static FreebusyExchangeAuthScheme fromString(String s) throws ServiceException {
            for (FreebusyExchangeAuthScheme value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isForm() { return this == form;}
        public boolean isBasic() { return this == basic;}
    }

    public static enum FreebusyExchangeServerType {
        webdav("webdav"),
        ews("ews");
        private String mValue;
        private FreebusyExchangeServerType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static FreebusyExchangeServerType fromString(String s) throws ServiceException {
            for (FreebusyExchangeServerType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isWebdav() { return this == webdav;}
        public boolean isEws() { return this == ews;}
    }

    public static enum GalLdapAuthMech {
        simple("simple"),
        kerberos5("kerberos5"),
        none("none");
        private String mValue;
        private GalLdapAuthMech(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalLdapAuthMech fromString(String s) throws ServiceException {
            for (GalLdapAuthMech value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isSimple() { return this == simple;}
        public boolean isKerberos5() { return this == kerberos5;}
        public boolean isNone() { return this == none;}
    }

    public static enum GalMode {
        both("both"),
        ldap("ldap"),
        zimbra("zimbra");
        private String mValue;
        private GalMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalMode fromString(String s) throws ServiceException {
            for (GalMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBoth() { return this == both;}
        public boolean isLdap() { return this == ldap;}
        public boolean isZimbra() { return this == zimbra;}
    }

    public static enum GalStatus {
        enabled("enabled"),
        disabled("disabled");
        private String mValue;
        private GalStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalStatus fromString(String s) throws ServiceException {
            for (GalStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isEnabled() { return this == enabled;}
        public boolean isDisabled() { return this == disabled;}
    }

    public static enum GalSyncLdapAuthMech {
        simple("simple"),
        kerberos5("kerberos5"),
        none("none");
        private String mValue;
        private GalSyncLdapAuthMech(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalSyncLdapAuthMech fromString(String s) throws ServiceException {
            for (GalSyncLdapAuthMech value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isSimple() { return this == simple;}
        public boolean isKerberos5() { return this == kerberos5;}
        public boolean isNone() { return this == none;}
    }

    public static enum GalTokenizeAutoCompleteKey {
        or("or"),
        and("and");
        private String mValue;
        private GalTokenizeAutoCompleteKey(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalTokenizeAutoCompleteKey fromString(String s) throws ServiceException {
            for (GalTokenizeAutoCompleteKey value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOr() { return this == or;}
        public boolean isAnd() { return this == and;}
    }

    public static enum GalTokenizeSearchKey {
        or("or"),
        and("and");
        private String mValue;
        private GalTokenizeSearchKey(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalTokenizeSearchKey fromString(String s) throws ServiceException {
            for (GalTokenizeSearchKey value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOr() { return this == or;}
        public boolean isAnd() { return this == and;}
    }

    public static enum GalType {
        ldap("ldap"),
        zimbra("zimbra");
        private String mValue;
        private GalType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static GalType fromString(String s) throws ServiceException {
            for (GalType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isLdap() { return this == ldap;}
        public boolean isZimbra() { return this == zimbra;}
    }

    public static enum IMService {
        zimbra("zimbra"),
        yahoo("yahoo");
        private String mValue;
        private IMService(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static IMService fromString(String s) throws ServiceException {
            for (IMService value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isZimbra() { return this == zimbra;}
        public boolean isYahoo() { return this == yahoo;}
    }

    public static enum IPMode {
        ipv6("ipv6"),
        ipv4("ipv4"),
        both("both");
        private String mValue;
        private IPMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static IPMode fromString(String s) throws ServiceException {
            for (IPMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isIpv6() { return this == ipv6;}
        public boolean isIpv4() { return this == ipv4;}
        public boolean isBoth() { return this == both;}
    }

    public static enum MailMode {
        https("https"),
        both("both"),
        http("http"),
        mixed("mixed"),
        redirect("redirect");
        private String mValue;
        private MailMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MailMode fromString(String s) throws ServiceException {
            for (MailMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isHttps() { return this == https;}
        public boolean isBoth() { return this == both;}
        public boolean isHttp() { return this == http;}
        public boolean isMixed() { return this == mixed;}
        public boolean isRedirect() { return this == redirect;}
    }

    public static enum MailReferMode {
        reverse_proxied("reverse-proxied"),
        wronghost("wronghost"),
        always("always");
        private String mValue;
        private MailReferMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MailReferMode fromString(String s) throws ServiceException {
            for (MailReferMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isReverse_proxied() { return this == reverse_proxied;}
        public boolean isWronghost() { return this == wronghost;}
        public boolean isAlways() { return this == always;}
    }

    public static enum MailSSLClientCertMode {
        Disabled("Disabled"),
        NeedClientAuth("NeedClientAuth"),
        WantClientAuth("WantClientAuth");
        private String mValue;
        private MailSSLClientCertMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MailSSLClientCertMode fromString(String s) throws ServiceException {
            for (MailSSLClientCertMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isDisabled() { return this == Disabled;}
        public boolean isNeedClientAuth() { return this == NeedClientAuth;}
        public boolean isWantClientAuth() { return this == WantClientAuth;}
    }

    public static enum MailStatus {
        enabled("enabled"),
        disabled("disabled");
        private String mValue;
        private MailStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MailStatus fromString(String s) throws ServiceException {
            for (MailStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isEnabled() { return this == enabled;}
        public boolean isDisabled() { return this == disabled;}
    }

    public static enum MailThreadingAlgorithm {
        references("references"),
        subject("subject"),
        none("none"),
        strict("strict"),
        subjrefs("subjrefs");
        private String mValue;
        private MailThreadingAlgorithm(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MailThreadingAlgorithm fromString(String s) throws ServiceException {
            for (MailThreadingAlgorithm value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isReferences() { return this == references;}
        public boolean isSubject() { return this == subject;}
        public boolean isNone() { return this == none;}
        public boolean isStrict() { return this == strict;}
        public boolean isSubjrefs() { return this == subjrefs;}
    }

    public static enum MtaSaslAuthEnable {
        yes("yes"),
        no("no");
        private String mValue;
        private MtaSaslAuthEnable(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MtaSaslAuthEnable fromString(String s) throws ServiceException {
            for (MtaSaslAuthEnable value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isYes() { return this == yes;}
        public boolean isNo() { return this == no;}
    }

    public static enum MtaTlsSecurityLevel {
        may("may"),
        none("none");
        private String mValue;
        private MtaTlsSecurityLevel(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static MtaTlsSecurityLevel fromString(String s) throws ServiceException {
            for (MtaTlsSecurityLevel value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isMay() { return this == may;}
        public boolean isNone() { return this == none;}
    }

    public static enum PrefBriefcaseReadingPaneLocation {
        bottom("bottom"),
        off("off"),
        right("right");
        private String mValue;
        private PrefBriefcaseReadingPaneLocation(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefBriefcaseReadingPaneLocation fromString(String s) throws ServiceException {
            for (PrefBriefcaseReadingPaneLocation value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBottom() { return this == bottom;}
        public boolean isOff() { return this == off;}
        public boolean isRight() { return this == right;}
    }

    public static enum PrefCalendarApptVisibility {
        public_("public"),
        private_("private");
        private String mValue;
        private PrefCalendarApptVisibility(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefCalendarApptVisibility fromString(String s) throws ServiceException {
            for (PrefCalendarApptVisibility value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isPublic_() { return this == public_;}
        public boolean isPrivate_() { return this == private_;}
    }

    public static enum PrefCalendarInitialView {
        workWeek("workWeek"),
        schedule("schedule"),
        month("month"),
        list("list"),
        day("day"),
        week("week");
        private String mValue;
        private PrefCalendarInitialView(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefCalendarInitialView fromString(String s) throws ServiceException {
            for (PrefCalendarInitialView value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isWorkWeek() { return this == workWeek;}
        public boolean isSchedule() { return this == schedule;}
        public boolean isMonth() { return this == month;}
        public boolean isList() { return this == list;}
        public boolean isDay() { return this == day;}
        public boolean isWeek() { return this == week;}
    }

    public static enum PrefClientType {
        standard("standard"),
        advanced("advanced");
        private String mValue;
        private PrefClientType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefClientType fromString(String s) throws ServiceException {
            for (PrefClientType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isStandard() { return this == standard;}
        public boolean isAdvanced() { return this == advanced;}
    }

    public static enum PrefComposeDirection {
        RTL("RTL"),
        LTR("LTR");
        private String mValue;
        private PrefComposeDirection(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefComposeDirection fromString(String s) throws ServiceException {
            for (PrefComposeDirection value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isRTL() { return this == RTL;}
        public boolean isLTR() { return this == LTR;}
    }

    public static enum PrefComposeFormat {
        text("text"),
        html("html");
        private String mValue;
        private PrefComposeFormat(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefComposeFormat fromString(String s) throws ServiceException {
            for (PrefComposeFormat value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isText() { return this == text;}
        public boolean isHtml() { return this == html;}
    }

    public static enum PrefContactsInitialView {
        list("list"),
        cards("cards");
        private String mValue;
        private PrefContactsInitialView(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefContactsInitialView fromString(String s) throws ServiceException {
            for (PrefContactsInitialView value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isList() { return this == list;}
        public boolean isCards() { return this == cards;}
    }

    public static enum PrefConversationOrder {
        dateDesc("dateDesc"),
        dateAsc("dateAsc");
        private String mValue;
        private PrefConversationOrder(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefConversationOrder fromString(String s) throws ServiceException {
            for (PrefConversationOrder value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isDateDesc() { return this == dateDesc;}
        public boolean isDateAsc() { return this == dateAsc;}
    }

    public static enum PrefConvReadingPaneLocation {
        bottom("bottom"),
        off("off"),
        right("right");
        private String mValue;
        private PrefConvReadingPaneLocation(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefConvReadingPaneLocation fromString(String s) throws ServiceException {
            for (PrefConvReadingPaneLocation value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBottom() { return this == bottom;}
        public boolean isOff() { return this == off;}
        public boolean isRight() { return this == right;}
    }

    public static enum PrefDedupeMessagesSentToSelf {
        secondCopyifOnToOrCC("secondCopyifOnToOrCC"),
        dedupeNone("dedupeNone"),
        dedupeAll("dedupeAll");
        private String mValue;
        private PrefDedupeMessagesSentToSelf(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefDedupeMessagesSentToSelf fromString(String s) throws ServiceException {
            for (PrefDedupeMessagesSentToSelf value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isSecondCopyifOnToOrCC() { return this == secondCopyifOnToOrCC;}
        public boolean isDedupeNone() { return this == dedupeNone;}
        public boolean isDedupeAll() { return this == dedupeAll;}
    }

    public static enum PrefExternalSendersType {
        ALLNOTINAB("ALLNOTINAB"),
        ALL("ALL");
        private String mValue;
        private PrefExternalSendersType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefExternalSendersType fromString(String s) throws ServiceException {
            for (PrefExternalSendersType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isALLNOTINAB() { return this == ALLNOTINAB;}
        public boolean isALL() { return this == ALL;}
    }

    public static enum PrefFileSharingApplication {
        briefcase("briefcase"),
        octopus("octopus");
        private String mValue;
        private PrefFileSharingApplication(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefFileSharingApplication fromString(String s) throws ServiceException {
            for (PrefFileSharingApplication value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBriefcase() { return this == briefcase;}
        public boolean isOctopus() { return this == octopus;}
    }

    public static enum PrefForwardIncludeOriginalText {
        includeBodyWithPrefix("includeBodyWithPrefix"),
        includeBodyOnly("includeBodyOnly"),
        includeBody("includeBody"),
        includeAsAttachment("includeAsAttachment"),
        includeBodyAndHeaders("includeBodyAndHeaders"),
        includeBodyAndHeadersWithPrefix("includeBodyAndHeadersWithPrefix");
        private String mValue;
        private PrefForwardIncludeOriginalText(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefForwardIncludeOriginalText fromString(String s) throws ServiceException {
            for (PrefForwardIncludeOriginalText value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isIncludeBodyWithPrefix() { return this == includeBodyWithPrefix;}
        public boolean isIncludeBodyOnly() { return this == includeBodyOnly;}
        public boolean isIncludeBody() { return this == includeBody;}
        public boolean isIncludeAsAttachment() { return this == includeAsAttachment;}
        public boolean isIncludeBodyAndHeaders() { return this == includeBodyAndHeaders;}
        public boolean isIncludeBodyAndHeadersWithPrefix() { return this == includeBodyAndHeadersWithPrefix;}
    }

    public static enum PrefForwardReplyFormat {
        text("text"),
        html("html"),
        same("same");
        private String mValue;
        private PrefForwardReplyFormat(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefForwardReplyFormat fromString(String s) throws ServiceException {
            for (PrefForwardReplyFormat value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isText() { return this == text;}
        public boolean isHtml() { return this == html;}
        public boolean isSame() { return this == same;}
    }

    public static enum PrefGetMailAction {
        update("update"),
        default_("default");
        private String mValue;
        private PrefGetMailAction(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefGetMailAction fromString(String s) throws ServiceException {
            for (PrefGetMailAction value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isUpdate() { return this == update;}
        public boolean isDefault_() { return this == default_;}
    }

    public static enum PrefGroupMailBy {
        message("message"),
        conversation("conversation");
        private String mValue;
        private PrefGroupMailBy(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefGroupMailBy fromString(String s) throws ServiceException {
            for (PrefGroupMailBy value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isMessage() { return this == message;}
        public boolean isConversation() { return this == conversation;}
    }

    public static enum PrefIMIdleStatus {
        away("away"),
        invisible("invisible"),
        xa("xa"),
        offline("offline");
        private String mValue;
        private PrefIMIdleStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefIMIdleStatus fromString(String s) throws ServiceException {
            for (PrefIMIdleStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isAway() { return this == away;}
        public boolean isInvisible() { return this == invisible;}
        public boolean isXa() { return this == xa;}
        public boolean isOffline() { return this == offline;}
    }

    public static enum PrefMailSelectAfterDelete {
        previous("previous"),
        adaptive("adaptive"),
        next("next");
        private String mValue;
        private PrefMailSelectAfterDelete(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefMailSelectAfterDelete fromString(String s) throws ServiceException {
            for (PrefMailSelectAfterDelete value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isPrevious() { return this == previous;}
        public boolean isAdaptive() { return this == adaptive;}
        public boolean isNext() { return this == next;}
    }

    public static enum PrefMailSendReadReceipts {
        never("never"),
        prompt("prompt"),
        always("always");
        private String mValue;
        private PrefMailSendReadReceipts(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefMailSendReadReceipts fromString(String s) throws ServiceException {
            for (PrefMailSendReadReceipts value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isNever() { return this == never;}
        public boolean isPrompt() { return this == prompt;}
        public boolean isAlways() { return this == always;}
    }

    public static enum PrefMailSignatureStyle {
        outlook("outlook"),
        internet("internet");
        private String mValue;
        private PrefMailSignatureStyle(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefMailSignatureStyle fromString(String s) throws ServiceException {
            for (PrefMailSignatureStyle value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOutlook() { return this == outlook;}
        public boolean isInternet() { return this == internet;}
    }

    public static enum PrefOutOfOfficeFreeBusyStatus {
        OUTOFOFFICE("OUTOFOFFICE"),
        BUSY("BUSY");
        private String mValue;
        private PrefOutOfOfficeFreeBusyStatus(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefOutOfOfficeFreeBusyStatus fromString(String s) throws ServiceException {
            for (PrefOutOfOfficeFreeBusyStatus value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOUTOFOFFICE() { return this == OUTOFOFFICE;}
        public boolean isBUSY() { return this == BUSY;}
    }

    public static enum PrefPop3DeleteOption {
        delete("delete"),
        trash("trash"),
        read("read"),
        keep("keep");
        private String mValue;
        private PrefPop3DeleteOption(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefPop3DeleteOption fromString(String s) throws ServiceException {
            for (PrefPop3DeleteOption value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isDelete() { return this == delete;}
        public boolean isTrash() { return this == trash;}
        public boolean isRead() { return this == read;}
        public boolean isKeep() { return this == keep;}
    }

    public static enum PrefReadingPaneLocation {
        bottom("bottom"),
        off("off"),
        right("right");
        private String mValue;
        private PrefReadingPaneLocation(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefReadingPaneLocation fromString(String s) throws ServiceException {
            for (PrefReadingPaneLocation value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBottom() { return this == bottom;}
        public boolean isOff() { return this == off;}
        public boolean isRight() { return this == right;}
    }

    public static enum PrefReplyIncludeOriginalText {
        includeBodyWithPrefix("includeBodyWithPrefix"),
        includeSmartAndHeadersWithPrefix("includeSmartAndHeadersWithPrefix"),
        includeBodyOnly("includeBodyOnly"),
        includeBody("includeBody"),
        includeSmartWithPrefix("includeSmartWithPrefix"),
        includeAsAttachment("includeAsAttachment"),
        includeSmart("includeSmart"),
        includeBodyAndHeaders("includeBodyAndHeaders"),
        includeSmartAndHeaders("includeSmartAndHeaders"),
        includeNone("includeNone"),
        includeBodyAndHeadersWithPrefix("includeBodyAndHeadersWithPrefix");
        private String mValue;
        private PrefReplyIncludeOriginalText(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefReplyIncludeOriginalText fromString(String s) throws ServiceException {
            for (PrefReplyIncludeOriginalText value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isIncludeBodyWithPrefix() { return this == includeBodyWithPrefix;}
        public boolean isIncludeSmartAndHeadersWithPrefix() { return this == includeSmartAndHeadersWithPrefix;}
        public boolean isIncludeBodyOnly() { return this == includeBodyOnly;}
        public boolean isIncludeBody() { return this == includeBody;}
        public boolean isIncludeSmartWithPrefix() { return this == includeSmartWithPrefix;}
        public boolean isIncludeAsAttachment() { return this == includeAsAttachment;}
        public boolean isIncludeSmart() { return this == includeSmart;}
        public boolean isIncludeBodyAndHeaders() { return this == includeBodyAndHeaders;}
        public boolean isIncludeSmartAndHeaders() { return this == includeSmartAndHeaders;}
        public boolean isIncludeNone() { return this == includeNone;}
        public boolean isIncludeBodyAndHeadersWithPrefix() { return this == includeBodyAndHeadersWithPrefix;}
    }

    public static enum PrefTasksFilterBy {
        DEFERRED("DEFERRED"),
        WAITING("WAITING"),
        TODO("TODO"),
        INPROGRESS("INPROGRESS"),
        COMPLETED("COMPLETED"),
        NOTSTARTED("NOTSTARTED");
        private String mValue;
        private PrefTasksFilterBy(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefTasksFilterBy fromString(String s) throws ServiceException {
            for (PrefTasksFilterBy value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isDEFERRED() { return this == DEFERRED;}
        public boolean isWAITING() { return this == WAITING;}
        public boolean isTODO() { return this == TODO;}
        public boolean isINPROGRESS() { return this == INPROGRESS;}
        public boolean isCOMPLETED() { return this == COMPLETED;}
        public boolean isNOTSTARTED() { return this == NOTSTARTED;}
    }

    public static enum PrefTasksReadingPaneLocation {
        bottom("bottom"),
        off("off"),
        right("right");
        private String mValue;
        private PrefTasksReadingPaneLocation(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static PrefTasksReadingPaneLocation fromString(String s) throws ServiceException {
            for (PrefTasksReadingPaneLocation value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isBottom() { return this == bottom;}
        public boolean isOff() { return this == off;}
        public boolean isRight() { return this == right;}
    }

    public static enum ReverseProxyClientCertMode {
        optional("optional"),
        off("off"),
        on("on");
        private String mValue;
        private ReverseProxyClientCertMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ReverseProxyClientCertMode fromString(String s) throws ServiceException {
            for (ReverseProxyClientCertMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOptional() { return this == optional;}
        public boolean isOff() { return this == off;}
        public boolean isOn() { return this == on;}
    }

    public static enum ReverseProxyImapStartTlsMode {
        off("off"),
        on("on"),
        only("only");
        private String mValue;
        private ReverseProxyImapStartTlsMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ReverseProxyImapStartTlsMode fromString(String s) throws ServiceException {
            for (ReverseProxyImapStartTlsMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOff() { return this == off;}
        public boolean isOn() { return this == on;}
        public boolean isOnly() { return this == only;}
    }

    public static enum ReverseProxyLogLevel {
        warn("warn"),
        error("error"),
        crit("crit"),
        debug("debug"),
        notice("notice"),
        info("info");
        private String mValue;
        private ReverseProxyLogLevel(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ReverseProxyLogLevel fromString(String s) throws ServiceException {
            for (ReverseProxyLogLevel value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isWarn() { return this == warn;}
        public boolean isError() { return this == error;}
        public boolean isCrit() { return this == crit;}
        public boolean isDebug() { return this == debug;}
        public boolean isNotice() { return this == notice;}
        public boolean isInfo() { return this == info;}
    }

    public static enum ReverseProxyMailMode {
        https("https"),
        both("both"),
        http("http"),
        mixed("mixed"),
        redirect("redirect");
        private String mValue;
        private ReverseProxyMailMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ReverseProxyMailMode fromString(String s) throws ServiceException {
            for (ReverseProxyMailMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isHttps() { return this == https;}
        public boolean isBoth() { return this == both;}
        public boolean isHttp() { return this == http;}
        public boolean isMixed() { return this == mixed;}
        public boolean isRedirect() { return this == redirect;}
    }

    public static enum ReverseProxyPop3StartTlsMode {
        off("off"),
        on("on"),
        only("only");
        private String mValue;
        private ReverseProxyPop3StartTlsMode(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ReverseProxyPop3StartTlsMode fromString(String s) throws ServiceException {
            for (ReverseProxyPop3StartTlsMode value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOff() { return this == off;}
        public boolean isOn() { return this == on;}
        public boolean isOnly() { return this == only;}
    }

    public static enum ShareNotificationMtaConnectionType {
        CLEARTEXT("CLEARTEXT"),
        SSL("SSL"),
        STARTTLS("STARTTLS");
        private String mValue;
        private ShareNotificationMtaConnectionType(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static ShareNotificationMtaConnectionType fromString(String s) throws ServiceException {
            for (ShareNotificationMtaConnectionType value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isCLEARTEXT() { return this == CLEARTEXT;}
        public boolean isSSL() { return this == SSL;}
        public boolean isSTARTTLS() { return this == STARTTLS;}
    }

    public static enum TableMaintenanceOperation {
        OPTIMIZE("OPTIMIZE"),
        ANALYZE("ANALYZE");
        private String mValue;
        private TableMaintenanceOperation(String value) { mValue = value; }
        public String toString() { return mValue; }
        public static TableMaintenanceOperation fromString(String s) throws ServiceException {
            for (TableMaintenanceOperation value : values()) {
                if (value.mValue.equals(s)) return value;
             }
             throw ServiceException.INVALID_REQUEST("invalid value: "+s+", valid values: "+ Arrays.asList(values()), null);
        }
        public boolean isOPTIMIZE() { return this == OPTIMIZE;}
        public boolean isANALYZE() { return this == ANALYZE;}
    }

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBadHeaderAdmin = "amavisBadHeaderAdmin";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBadHeaderLover = "amavisBadHeaderLover";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBadHeaderQuarantineTo = "amavisBadHeaderQuarantineTo";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBannedAdmin = "amavisBannedAdmin";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBannedFilesLover = "amavisBannedFilesLover";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBannedQuarantineTo = "amavisBannedQuarantineTo";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBannedRuleNames = "amavisBannedRuleNames";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBlacklistSender = "amavisBlacklistSender";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBypassBannedChecks = "amavisBypassBannedChecks";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBypassHeaderChecks = "amavisBypassHeaderChecks";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBypassSpamChecks = "amavisBypassSpamChecks";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisBypassVirusChecks = "amavisBypassVirusChecks";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisLocal = "amavisLocal";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisMessageSizeLimit = "amavisMessageSizeLimit";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisNewVirusAdmin = "amavisNewVirusAdmin";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamAdmin = "amavisSpamAdmin";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamKillLevel = "amavisSpamKillLevel";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamLover = "amavisSpamLover";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamModifiesSubj = "amavisSpamModifiesSubj";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamQuarantineTo = "amavisSpamQuarantineTo";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamTag2Level = "amavisSpamTag2Level";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisSpamTagLevel = "amavisSpamTagLevel";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisVirusAdmin = "amavisVirusAdmin";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisVirusLover = "amavisVirusLover";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisVirusQuarantineTo = "amavisVirusQuarantineTo";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisWarnBadHeaderRecip = "amavisWarnBadHeaderRecip";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisWarnBannedRecip = "amavisWarnBannedRecip";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisWarnVirusRecip = "amavisWarnVirusRecip";

    /**
     */
    @ZAttr(id=-1)
    public static final String A_amavisWhitelistSender = "amavisWhitelistSender";

    /**
     * RFC2256: ISO-3166 country 2-letter code
     */
    @ZAttr(id=-1)
    public static final String A_c = "c";

    /**
     * RFC2256: common name(s) for which the entity is known by
     */
    @ZAttr(id=-1)
    public static final String A_cn = "cn";

    /**
     * RFC1274: friendly country name
     */
    @ZAttr(id=-1)
    public static final String A_co = "co";

    /**
     * From Microsoft Schema
     */
    @ZAttr(id=-1)
    public static final String A_company = "company";

    /**
     * RFC2256: descriptive information
     */
    @ZAttr(id=-1)
    public static final String A_description = "description";

    /**
     * RFC2256: destination indicator
     */
    @ZAttr(id=-1)
    public static final String A_destinationIndicator = "destinationIndicator";

    /**
     * RFC2798: preferred name to be used when displaying entries
     */
    @ZAttr(id=-1)
    public static final String A_displayName = "displayName";

    /**
     * RFC2256: Facsimile (Fax) Telephone Number
     */
    @ZAttr(id=-1)
    public static final String A_facsimileTelephoneNumber = "facsimileTelephoneNumber";

    /**
     * RFC2256: first name(s) for which the entity is known by
     */
    @ZAttr(id=-1)
    public static final String A_givenName = "givenName";

    /**
     * RFC2256: first name(s) for which the entity is known by
     */
    @ZAttr(id=-1)
    public static final String A_gn = "gn";

    /**
     * RFC1274: home telephone number
     */
    @ZAttr(id=-1)
    public static final String A_homePhone = "homePhone";

    /**
     * RFC2256: initials of some or all of names, but not the surname(s).
     */
    @ZAttr(id=-1)
    public static final String A_initials = "initials";

    /**
     * RFC2256: international ISDN number
     */
    @ZAttr(id=-1)
    public static final String A_internationaliSDNNumber = "internationaliSDNNumber";

    /**
     * RFC2256: locality which this object resides in
     */
    @ZAttr(id=-1)
    public static final String A_l = "l";

    /**
     * RFC1274: RFC822 Mailbox
     */
    @ZAttr(id=-1)
    public static final String A_mail = "mail";

    /**
     * Identifies an URL associated with each member of a group
     */
    @ZAttr(id=-1)
    public static final String A_memberURL = "memberURL";

    /**
     * RFC1274: mobile telephone number
     */
    @ZAttr(id=-1)
    public static final String A_mobile = "mobile";

    /**
     * RFC2256: organization this object belongs to
     */
    @ZAttr(id=-1)
    public static final String A_o = "o";

    /**
     * RFC2256: object classes of the entity
     */
    @ZAttr(id=-1)
    public static final String A_objectClass = "objectClass";

    /**
     * RFC2256: organizational unit this object belongs to
     */
    @ZAttr(id=-1)
    public static final String A_ou = "ou";

    /**
     * RFC1274: pager telephone number
     */
    @ZAttr(id=-1)
    public static final String A_pager = "pager";

    /**
     * &#039;RFC2256: Physical Delivery Office Name
     */
    @ZAttr(id=-1)
    public static final String A_physicalDeliveryOfficeName = "physicalDeliveryOfficeName";

    /**
     * RFC2256: postal address
     */
    @ZAttr(id=-1)
    public static final String A_postalAddress = "postalAddress";

    /**
     * RFC2256: postal code
     */
    @ZAttr(id=-1)
    public static final String A_postalCode = "postalCode";

    /**
     * RFC2256: Post Office Box
     */
    @ZAttr(id=-1)
    public static final String A_postOfficeBox = "postOfficeBox";

    /**
     * RFC2256: preferred delivery method
     */
    @ZAttr(id=-1)
    public static final String A_preferredDeliveryMethod = "preferredDeliveryMethod";

    /**
     * RFC2256: registered postal address
     */
    @ZAttr(id=-1)
    public static final String A_registeredAddress = "registeredAddress";

    /**
     * RFC2256: last (family) name(s) for which the entity is known by
     */
    @ZAttr(id=-1)
    public static final String A_sn = "sn";

    /**
     * RFC2256: state or province which this object resides in
     */
    @ZAttr(id=-1)
    public static final String A_st = "st";

    /**
     * RFC2256: street address of this object
     */
    @ZAttr(id=-1)
    public static final String A_street = "street";

    /**
     * RFC2256: street address of this object
     */
    @ZAttr(id=-1)
    public static final String A_streetAddress = "streetAddress";

    /**
     * RFC2256: Telephone Number
     */
    @ZAttr(id=-1)
    public static final String A_telephoneNumber = "telephoneNumber";

    /**
     * RFC2256: Teletex Terminal Identifier
     */
    @ZAttr(id=-1)
    public static final String A_teletexTerminalIdentifier = "teletexTerminalIdentifier";

    /**
     * RFC2256: Telex Number
     */
    @ZAttr(id=-1)
    public static final String A_telexNumber = "telexNumber";

    /**
     * RFC2256: title associated with the entity
     */
    @ZAttr(id=-1)
    public static final String A_title = "title";

    /**
     * RFC1274: user identifier
     */
    @ZAttr(id=-1)
    public static final String A_uid = "uid";

    /**
     * RFC2256: X.509 user certificate
     */
    @ZAttr(id=-1)
    public static final String A_userCertificate = "userCertificate";

    /**
     * RFC2256/2307: password of user. Stored encoded as SSHA (salted-SHA1)
     */
    @ZAttr(id=-1)
    public static final String A_userPassword = "userPassword";

    /**
     * RFC2798: PKCS#7 SignedData used to support S/MIME
     */
    @ZAttr(id=-1)
    public static final String A_userSMIMECertificate = "userSMIMECertificate";

    /**
     * RFC2256: X.121 Address
     */
    @ZAttr(id=-1)
    public static final String A_x121Address = "x121Address";

    /**
     * calendar user type - USER (default) or RESOURCE
     */
    @ZAttr(id=313)
    public static final String A_zimbraAccountCalendarUserType = "zimbraAccountCalendarUserType";

    /**
     * Deprecated since: 5.0. deprecated in favor of the accountInfo flag.
     * Orig desc: additional account attrs that get returned to a client
     */
    @ZAttr(id=112)
    public static final String A_zimbraAccountClientAttr = "zimbraAccountClientAttr";

    /**
     * Object classes to add when creating a zimbra account object. Useful if
     * you want to add sambaSamAccount etc to zimbra accounts.
     */
    @ZAttr(id=438)
    public static final String A_zimbraAccountExtraObjectClass = "zimbraAccountExtraObjectClass";

    /**
     * account status. active - active lockout - no login until lockout
     * duration is over, mail delivery OK. locked - no login, mail delivery
     * OK. maintenance - no login, no delivery(lmtp server returns 4.x.x
     * Persistent Transient Failure). pending - no login, no delivery(lmtp
     * server returns 5.x.x Permanent Failure), Account behavior is like
     * closed, except that when the status is being set to pending, account
     * addresses are not removed from distribution lists. The use case is for
     * hosted. New account creation based on invites that are not completed
     * until user accepts TOS on account creation confirmation page. closed -
     * no login, no delivery(lmtp server returns 5.x.x Permanent Failure),
     * all addresses (account main email and all aliases) of the account are
     * removed from all distribution lists.
     */
    @ZAttr(id=2)
    public static final String A_zimbraAccountStatus = "zimbraAccountStatus";

    /**
     * Zimbra access control list
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=659)
    public static final String A_zimbraACE = "zimbraACE";

    /**
     * access control mechanism for admin access acl: ACL based access
     * control (a.k.a. delegated admin). global: allows only global admins.
     *
     * @since ZCS 6.0.9
     */
    @ZAttr(id=1101)
    public static final String A_zimbraAdminAccessControlMech = "zimbraAdminAccessControlMech";

    /**
     * lifetime of newly created admin auth tokens. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     */
    @ZAttr(id=109)
    public static final String A_zimbraAdminAuthTokenLifetime = "zimbraAdminAuthTokenLifetime";

    /**
     * whether to show catchall addresses in admin console
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=746)
    public static final String A_zimbraAdminConsoleCatchAllAddressEnabled = "zimbraAdminConsoleCatchAllAddressEnabled";

    /**
     * enable MX check feature for domain
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=743)
    public static final String A_zimbraAdminConsoleDNSCheckEnabled = "zimbraAdminConsoleDNSCheckEnabled";

    /**
     * whether configuring external LDAP auth is enabled in admin console
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=774)
    public static final String A_zimbraAdminConsoleLDAPAuthEnabled = "zimbraAdminConsoleLDAPAuthEnabled";

    /**
     * admin console login message
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=772)
    public static final String A_zimbraAdminConsoleLoginMessage = "zimbraAdminConsoleLoginMessage";

    /**
     * login URL for admin console to send the user to upon explicit logging
     * in
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=696)
    public static final String A_zimbraAdminConsoleLoginURL = "zimbraAdminConsoleLoginURL";

    /**
     * logout URL for admin console to send the user to upon explicit logging
     * out
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=684)
    public static final String A_zimbraAdminConsoleLogoutURL = "zimbraAdminConsoleLogoutURL";

    /**
     * whether to allow skin management in admin console
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=751)
    public static final String A_zimbraAdminConsoleSkinEnabled = "zimbraAdminConsoleSkinEnabled";

    /**
     * UI components available for the authed admin in admin console
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=761)
    public static final String A_zimbraAdminConsoleUIComponents = "zimbraAdminConsoleUIComponents";

    /**
     * Zimlet Util will set this attribute based on the value in zimlet
     * definition XML file
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=803)
    public static final String A_zimbraAdminExtDisableUIUndeploy = "zimbraAdminExtDisableUIUndeploy";

    /**
     * number of admin initiated imap import handler threads
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1113)
    public static final String A_zimbraAdminImapImportNumThreads = "zimbraAdminImapImportNumThreads";

    /**
     * SSL port for admin UI
     */
    @ZAttr(id=155)
    public static final String A_zimbraAdminPort = "zimbraAdminPort";

    /**
     * SSL proxy port for admin console UI
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1322)
    public static final String A_zimbraAdminProxyPort = "zimbraAdminProxyPort";

    /**
     * admin saved searches
     */
    @ZAttr(id=446)
    public static final String A_zimbraAdminSavedSearches = "zimbraAdminSavedSearches";

    /**
     * URL prefix for where the zimbraAdmin app resides on this server
     */
    @ZAttr(id=497)
    public static final String A_zimbraAdminURL = "zimbraAdminURL";

    /**
     * last calculated aggregate quota usage for the domain in bytes
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1328)
    public static final String A_zimbraAggregateQuotaLastUsage = "zimbraAggregateQuotaLastUsage";

    /**
     * zimbraId of alias target
     */
    @ZAttr(id=40)
    public static final String A_zimbraAliasTargetId = "zimbraAliasTargetId";

    /**
     * Whether this account can use any from address. Not changeable by
     * domain admin to allow arbitrary addresses
     */
    @ZAttr(id=427)
    public static final String A_zimbraAllowAnyFromAddress = "zimbraAllowAnyFromAddress";

    /**
     * Addresses that this account can as from address if
     * arbitrary-addresses-allowed setting is not set
     */
    @ZAttr(id=428)
    public static final String A_zimbraAllowFromAddress = "zimbraAllowFromAddress";

    /**
     * whether creating domains, and renaming domains to a name, containing
     * non-LDH (letter, digit, hyphen) characters is allowed
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1052)
    public static final String A_zimbraAllowNonLDHCharsInDomain = "zimbraAllowNonLDHCharsInDomain";

    /**
     * When a virus is detected quarantine message to this account
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1100)
    public static final String A_zimbraAmavisQuarantineAccount = "zimbraAmavisQuarantineAccount";

    /**
     * Mailboxes in which the current account in archived. Multi-value attr
     * with eg values { user-2006@example.com.archive,
     * user-2007@example.com.archive } that tells us that user@example.com
     * has been archived into those two mailboxes.
     */
    @ZAttr(id=429)
    public static final String A_zimbraArchiveAccount = "zimbraArchiveAccount";

    /**
     * An account or CoS setting that works with the name template that
     * allows you to dictate the date format used in the name template. This
     * is a Java SimpleDateFormat specifier. The default is an LDAP
     * generalized time format:
     */
    @ZAttr(id=432)
    public static final String A_zimbraArchiveAccountDateTemplate = "zimbraArchiveAccountDateTemplate";

    /**
     * An account or CoS setting - typically only in CoS - that tells the
     * archiving system how to derive the archive mailbox name. ID, USER,
     * DATE, and DOMAIN are expanded.
     */
    @ZAttr(id=431)
    public static final String A_zimbraArchiveAccountNameTemplate = "zimbraArchiveAccountNameTemplate";

    /**
     * whether account archiving is enabled
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1206)
    public static final String A_zimbraArchiveEnabled = "zimbraArchiveEnabled";

    /**
     * Address to which archive message bounces should be sent. Typically
     * could be an admin account. This is global across all domains.
     */
    @ZAttr(id=430)
    public static final String A_zimbraArchiveMailFrom = "zimbraArchiveMailFrom";

    /**
     * block all attachment downloading
     */
    @ZAttr(id=115)
    public static final String A_zimbraAttachmentsBlocked = "zimbraAttachmentsBlocked";

    /**
     * Maximum number of characters that will be indexed for a given MIME
     * part.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=582)
    public static final String A_zimbraAttachmentsIndexedTextLimit = "zimbraAttachmentsIndexedTextLimit";

    /**
     * whether or not to index attachments
     */
    @ZAttr(id=173)
    public static final String A_zimbraAttachmentsIndexingEnabled = "zimbraAttachmentsIndexingEnabled";

    /**
     * Class to use to scan attachments during compose
     */
    @ZAttr(id=238)
    public static final String A_zimbraAttachmentsScanClass = "zimbraAttachmentsScanClass";

    /**
     * Whether to scan attachments during compose
     */
    @ZAttr(id=237)
    public static final String A_zimbraAttachmentsScanEnabled = "zimbraAttachmentsScanEnabled";

    /**
     * Data for class that scans attachments during compose
     */
    @ZAttr(id=239)
    public static final String A_zimbraAttachmentsScanURL = "zimbraAttachmentsScanURL";

    /**
     * view all attachments in html only
     */
    @ZAttr(id=116)
    public static final String A_zimbraAttachmentsViewInHtmlOnly = "zimbraAttachmentsViewInHtmlOnly";

    /**
     * fallback to local auth if external mech fails
     */
    @ZAttr(id=257)
    public static final String A_zimbraAuthFallbackToLocal = "zimbraAuthFallbackToLocal";

    /**
     * kerberos5 realm for kerberos5 auth mech
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=548)
    public static final String A_zimbraAuthKerberos5Realm = "zimbraAuthKerberos5Realm";

    /**
     * LDAP bind dn for ldap auth mech
     */
    @ZAttr(id=44)
    public static final String A_zimbraAuthLdapBindDn = "zimbraAuthLdapBindDn";

    /**
     * explicit mapping to an external LDAP dn for a given account
     */
    @ZAttr(id=256)
    public static final String A_zimbraAuthLdapExternalDn = "zimbraAuthLdapExternalDn";

    /**
     * LDAP search base for ldap auth mech
     */
    @ZAttr(id=252)
    public static final String A_zimbraAuthLdapSearchBase = "zimbraAuthLdapSearchBase";

    /**
     * LDAP search bind dn for ldap auth mech
     */
    @ZAttr(id=253)
    public static final String A_zimbraAuthLdapSearchBindDn = "zimbraAuthLdapSearchBindDn";

    /**
     * LDAP search bind password for ldap auth mech
     */
    @ZAttr(id=254)
    public static final String A_zimbraAuthLdapSearchBindPassword = "zimbraAuthLdapSearchBindPassword";

    /**
     * LDAP search filter for ldap auth mech
     */
    @ZAttr(id=255)
    public static final String A_zimbraAuthLdapSearchFilter = "zimbraAuthLdapSearchFilter";

    /**
     * whether to use startTLS for external LDAP auth
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=654)
    public static final String A_zimbraAuthLdapStartTlsEnabled = "zimbraAuthLdapStartTlsEnabled";

    /**
     * LDAP URL for ldap auth mech
     */
    @ZAttr(id=43)
    public static final String A_zimbraAuthLdapURL = "zimbraAuthLdapURL";

    /**
     * mechanism to use for verifying password. Valid values are zimbra,
     * ldap, ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     */
    @ZAttr(id=42)
    public static final String A_zimbraAuthMech = "zimbraAuthMech";

    /**
     * mechanism to use for verifying password for admin. See zimbraAuthMech
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1252)
    public static final String A_zimbraAuthMechAdmin = "zimbraAuthMechAdmin";

    /**
     * auth token secret key
     */
    @ZAttr(id=100)
    public static final String A_zimbraAuthTokenKey = "zimbraAuthTokenKey";

    /**
     * lifetime of newly created auth tokens. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     */
    @ZAttr(id=108)
    public static final String A_zimbraAuthTokenLifetime = "zimbraAuthTokenLifetime";

    /**
     * if set, this value gets stored in the auth token and compared on every
     * request. Changing it will invalidate all outstanding auth tokens. It
     * also gets changed when an account password is changed.
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1044)
    public static final String A_zimbraAuthTokenValidityValue = "zimbraAuthTokenValidityValue";

    /**
     * Whether auth token validity value checking should be performed during
     * auth token validation. See description for
     * zimbraAuthTokenValidityValue.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1094)
    public static final String A_zimbraAuthTokenValidityValueEnabled = "zimbraAuthTokenValidityValueEnabled";

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute name in the external directory that contains localpart of
     * the account name. If not specified, localpart of the account name is
     * the principal user used to authenticated to Zimbra.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1230)
    public static final String A_zimbraAutoProvAccountNameMap = "zimbraAutoProvAccountNameMap";

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional
     * Attribute map for mapping attribute values from the external entry to
     * Zimbra account attributes. Values are in the format of {external
     * attribute}={zimbra attribute}. If not set, no attributes from the
     * external directory will be populated in Zimbra directory. Invalid
     * mapping configuration will cause the account creation to fail.
     * Examples of bad mapping: - invalid external attribute name. - invalid
     * Zimbra attribute name. - external attribute has multiple values but
     * the zimbra attribute is single-valued. - syntax violation. e.g. Value
     * on the external attribute is a String but the Zimbra attribute is
     * declared an integer.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1231)
    public static final String A_zimbraAutoProvAttrMap = "zimbraAutoProvAttrMap";

    /**
     * EAGER mode: N/A LAZY mode: required MANUAL mode: N/A Auth mechanisms
     * enabled for auto provision in LAZY mode. When a user authenticates via
     * one of the external auth mechanisms enabled in this attribute, and
     * when the user account does not yet exist in Zimbra directory, an
     * account entry will be automatically created in Zimbra directory.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1222)
    public static final String A_zimbraAutoProvAuthMech = "zimbraAutoProvAuthMech";

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Max number of
     * accounts to process in each interval for EAGER auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1234)
    public static final String A_zimbraAutoProvBatchSize = "zimbraAutoProvBatchSize";

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A Timestamp when the external domain is last
     * polled for EAGER auto provision. The poll (LDAP search) for the next
     * iteration will fetch external entries with create timestamp later than
     * the timestamp recorded from the previous iteration.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1235)
    public static final String A_zimbraAutoProvLastPolledTimestamp = "zimbraAutoProvLastPolledTimestamp";

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: required LDAP search bind
     * DN for auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1225)
    public static final String A_zimbraAutoProvLdapAdminBindDn = "zimbraAutoProvLdapAdminBindDn";

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * search bind password for auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1226)
    public static final String A_zimbraAutoProvLdapAdminBindPassword = "zimbraAutoProvLdapAdminBindPassword";

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapSearchFilter) LDAP external DN template for account
     * auto provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1229)
    public static final String A_zimbraAutoProvLdapBindDn = "zimbraAutoProvLdapBindDn";

    /**
     * EAGER mode: required LAZY mode: required (if using
     * zimbraAutoProvLdapSearchFilter), MANUAL mode: required LDAP search
     * base for auto provision, used in conjunction with
     * zimbraAutoProvLdapSearchFilter. If not set, LDAP root DSE will be
     * used.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1227)
    public static final String A_zimbraAutoProvLdapSearchBase = "zimbraAutoProvLdapSearchBase";

    /**
     * EAGER mode: required LAZY mode: optional (if not using
     * zimbraAutoProvLdapBindDn) MANUAL mode: optional (if not using
     * zimbraAutoProvLdapBindDn) LDAP search filter template for account auto
     * provisioning. For LAZY and MANUAL modes, either
     * zimbraAutoProvLdapSearchFilter or zimbraAutoProvLdapBindDn has to be
     * set. If both are set, zimbraAutoProvLdapSearchFilter will take
     * precedence. Supported place holders: %n = username with @ (or without,
     * if no @ was specified) %u = username with @ removed %d = domain as
     * foo.com %D = domain as dc=foo,dc=com
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1228)
    public static final String A_zimbraAutoProvLdapSearchFilter = "zimbraAutoProvLdapSearchFilter";

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Default
     * is FALSE. Whether to use startTLS when accessing the external LDAP
     * server for auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1224)
    public static final String A_zimbraAutoProvLdapStartTlsEnabled = "zimbraAutoProvLdapStartTlsEnabled";

    /**
     * EAGER mode: required LAZY mode: required MANUAL mode: required LDAP
     * URL of the external LDAP source for auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1223)
    public static final String A_zimbraAutoProvLdapURL = "zimbraAutoProvLdapURL";

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Class
     * name of auto provision listener. The class must implement the
     * com.zimbra.cs.account.Account.AutoProvisionListener interface. The
     * singleton listener instance is invoked after each account is auto
     * created in Zimbra. Listener can be plugged in as a server extension to
     * handle tasks like updating the account auto provision status in the
     * external LDAP directory.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1233)
    public static final String A_zimbraAutoProvListenerClass = "zimbraAutoProvListenerClass";

    /**
     * EAGER mode: for Zimbra internal use only - do not change it. LAZY
     * mode: N/A MANUAL mode: N/A For EAGER auto provision, a domain can be
     * scheduled on multiple server. To avoid conflict, only one server can
     * perform provisioning for a domain at one time. This attribute servers
     * a lock for the test-and-set LDAP operation to synchronize EAGER auto
     * provision attempts between servers.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1236)
    public static final String A_zimbraAutoProvLock = "zimbraAutoProvLock";

    /**
     * Auto provision modes enabled. Multiple modes can be enabled on a
     * domain. EAGER: A server maintenance thread automatically polls the
     * configured external auto provision LDAP source at a configured
     * interval for entries due to be auto provisioned in Zimbra, and then
     * auto creates the accounts in Zimbra directory. LAZY: auto creates the
     * Zimbra account when user first login via one of the external auth
     * mechanisms enabled for auto provisioning. Auth mechanisms enabled for
     * auto provisioning are configured in zimbraAutoProvAuthMech. MANUAL:
     * admin to search from the configured external auto provision LDAP
     * source and select an entry from the search result to create the
     * corresponding Zimbra account for the external entry. In all cases,
     * localpart of the Zimbra account is mapped from an attribute on the
     * external entry based on zimbraAutoProvAccountNameMap. The Zimbra
     * account is populated with attributes mapped from the external entry
     * based on zimbraAutoProvAttrMap.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1221)
    public static final String A_zimbraAutoProvMode = "zimbraAutoProvMode";

    /**
     * Template used to construct the subject of the notification message
     * sent to the user when the user&#039;s account is auto provisioned.
     * Supported variables: ${ACCOUNT_ADDRESS}, ${ACCOUNT_DISPLAY_NAME}
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1357)
    public static final String A_zimbraAutoProvNotificationBody = "zimbraAutoProvNotificationBody";

    /**
     * EAGER mode: optional LAZY mode: optional MANUAL mode: optional Email
     * address to put in the From header for the notification email to the
     * newly created account. If not set, no notification email will sent to
     * the newly created account.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1232)
    public static final String A_zimbraAutoProvNotificationFromAddress = "zimbraAutoProvNotificationFromAddress";

    /**
     * Template used to construct the subject of the notification message
     * sent to the user when the user&#039;s account is auto provisioned.
     * Supported variables: ${ACCOUNT_ADDRESS}, ${ACCOUNT_DISPLAY_NAME}
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1356)
    public static final String A_zimbraAutoProvNotificationSubject = "zimbraAutoProvNotificationSubject";

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Interval between
     * successive polling and provisioning accounts in EAGER mode. The actual
     * interval may take longer since it can be affected by two other
     * factors: zimbraAutoProvBatchSize and number of domains configured in
     * zimbraAutoProvScheduledDomains. At each interval, the auto provision
     * thread iterates through all domains in zimbraAutoProvScheduledDomains
     * and auto creates up to domain.zimbraAutoProvBatchSize accounts. If
     * that process takes longer than zimbraAutoProvPollingInterval then the
     * next iteration will start immediately instead of waiting for
     * zimbraAutoProvPollingInterval amount of time. If set to 0 when server
     * starts up, the auto provision thread will not start. If changed from a
     * non-0 value to 0 while server is running, the auto provision thread
     * will be shutdown. If changed from 0 to a non-0 value while server is
     * running, the auto provision thread will be started. . Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1238)
    public static final String A_zimbraAutoProvPollingInterval = "zimbraAutoProvPollingInterval";

    /**
     * EAGER mode: required LAZY mode: N/A MANUAL mode: N/A Domain scheduled
     * for eager auto provision on this server. Scheduled domains must have
     * EAGER mode enabled in zimbraAutoProvMode. Multiple domains can be
     * scheduled on a server for EAGER auto provision. Also, a domain can be
     * scheduled on multiple servers for EAGER auto provision.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1237)
    public static final String A_zimbraAutoProvScheduledDomains = "zimbraAutoProvScheduledDomains";

    /**
     * Use null return path for envelope MAIL FROM when sending out of office
     * and new mail notifications. If false, use account address for envelope
     */
    @ZAttr(id=502)
    public static final String A_zimbraAutoSubmittedNullReturnPath = "zimbraAutoSubmittedNullReturnPath";

    /**
     * Locales available for this account
     */
    @ZAttr(id=487)
    public static final String A_zimbraAvailableLocale = "zimbraAvailableLocale";

    /**
     * Skins available for this account. Fallback order is: 1. the normal
     * account/cos inheritance 2. if not set on account/cos, use the value on
     * the domain of the account
     */
    @ZAttr(id=364)
    public static final String A_zimbraAvailableSkin = "zimbraAvailableSkin";

    /**
     * length of each interval in auto-grouped backup
     */
    @ZAttr(id=513)
    public static final String A_zimbraBackupAutoGroupedInterval = "zimbraBackupAutoGroupedInterval";

    /**
     * number of groups to auto-group backups over
     */
    @ZAttr(id=514)
    public static final String A_zimbraBackupAutoGroupedNumGroups = "zimbraBackupAutoGroupedNumGroups";

    /**
     * if true, limit the number of mailboxes in auto-grouped backup to total
     * mailboxes divided by auto-group days
     */
    @ZAttr(id=515)
    public static final String A_zimbraBackupAutoGroupedThrottled = "zimbraBackupAutoGroupedThrottled";

    /**
     * Minimum percentage or TB/GB/MB/KB/bytes of free space on backup target
     * to allow a full or auto-grouped backup to start; 0 = no minimum is
     * enforced. Examples: 25%, 10GB
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1111)
    public static final String A_zimbraBackupMinFreeSpace = "zimbraBackupMinFreeSpace";

    /**
     * backup mode
     */
    @ZAttr(id=512)
    public static final String A_zimbraBackupMode = "zimbraBackupMode";

    /**
     * Backup report email recipients
     */
    @ZAttr(id=459)
    public static final String A_zimbraBackupReportEmailRecipients = "zimbraBackupReportEmailRecipients";

    /**
     * Backup report email From address
     */
    @ZAttr(id=460)
    public static final String A_zimbraBackupReportEmailSender = "zimbraBackupReportEmailSender";

    /**
     * Backup report email subject prefix
     */
    @ZAttr(id=461)
    public static final String A_zimbraBackupReportEmailSubjectPrefix = "zimbraBackupReportEmailSubjectPrefix";

    /**
     * if true, do not backup blobs (HSM or not) during a full backup
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1004)
    public static final String A_zimbraBackupSkipBlobs = "zimbraBackupSkipBlobs";

    /**
     * if true, do not backup blobs on secondary (HSM) volumes during a full
     * backup
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1005)
    public static final String A_zimbraBackupSkipHsmBlobs = "zimbraBackupSkipHsmBlobs";

    /**
     * if true, do not backup search index during a full backup
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1003)
    public static final String A_zimbraBackupSkipSearchIndex = "zimbraBackupSkipSearchIndex";

    /**
     * Default backup target path
     */
    @ZAttr(id=458)
    public static final String A_zimbraBackupTarget = "zimbraBackupTarget";

    /**
     * Realm for the basic auth challenge (WWW-Authenticate) header
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1098)
    public static final String A_zimbraBasicAuthRealm = "zimbraBasicAuthRealm";

    /**
     * Batch size to use when indexing data
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=619)
    public static final String A_zimbraBatchedIndexingSize = "zimbraBatchedIndexingSize";

    /**
     * alternate location for calendar and task folders
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=651)
    public static final String A_zimbraCalendarCalDavAlternateCalendarHomeSet = "zimbraCalendarCalDavAlternateCalendarHomeSet";

    /**
     * Whether to allow password sent to non-secured port from CalDAV
     * clients. If it set to TRUE the server will allow access from CalDAV
     * client to zimbraMailPort. If it set to FALSE the server will return an
     * error if a request is made from CalDAV client to zimbraMailPort.
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=820)
    public static final String A_zimbraCalendarCalDavClearTextPasswordEnabled = "zimbraCalendarCalDavClearTextPasswordEnabled";

    /**
     * Id of calendar folder to advertise as the default calendar to CalDAV
     * client.
     *
     * @since ZCS 6.0.6
     */
    @ZAttr(id=1078)
    public static final String A_zimbraCalendarCalDavDefaultCalendarId = "zimbraCalendarCalDavDefaultCalendarId";

    /**
     * set true to turn off handling free/busy lookup for CalDAV
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=690)
    public static final String A_zimbraCalendarCalDavDisableFreebusy = "zimbraCalendarCalDavDisableFreebusy";

    /**
     * set true to turn off handling scheduling message for CalDAV
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=652)
    public static final String A_zimbraCalendarCalDavDisableScheduling = "zimbraCalendarCalDavDisableScheduling";

    /**
     * CalDAV shared folder cache duration. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=817)
    public static final String A_zimbraCalendarCalDavSharedFolderCacheDuration = "zimbraCalendarCalDavSharedFolderCacheDuration";

    /**
     * see description of zimbraCalendarCalDavSyncStart. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=816)
    public static final String A_zimbraCalendarCalDavSyncEnd = "zimbraCalendarCalDavSyncEnd";

    /**
     * zimbraCalendarCalDavSyncStart and zimbraCalendarCalDavSyncEnd limits
     * the window of appointment data available via CalDAV. for example when
     * zimbraCalendarCalDavSyncStart is set to 30 days, and
     * zimbraCalendarCalDavSyncEnd is set to 1 years, then the appointments
     * between (now - 30 days) and (now + 1 year) will be available via
     * CalDAV. When they are unset all the appointments are available via
     * CalDAV. . Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=815)
    public static final String A_zimbraCalendarCalDavSyncStart = "zimbraCalendarCalDavSyncStart";

    /**
     * When set to TRUE, Calendar folders and Todo folders in Zimbra will be
     * advertised as Calendar only and Todo only via CalDAV. When set to
     * FALSE, Calendar folders will be able to store both appointments and
     * tasks, and Todo folders will not be advertised as CalDAV enabled.
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=794)
    public static final String A_zimbraCalendarCalDavUseDistinctAppointmentAndToDoCollection = "zimbraCalendarCalDavUseDistinctAppointmentAndToDoCollection";

    /**
     * compatibility mode for calendar server
     */
    @ZAttr(id=243)
    public static final String A_zimbraCalendarCompatibilityMode = "zimbraCalendarCompatibilityMode";

    /**
     * whether to retain exception instances when the recurrence series is
     * changed to new time; set to FALSE for Exchange compatibility
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1240)
    public static final String A_zimbraCalendarKeepExceptionsOnSeriesTimeChange = "zimbraCalendarKeepExceptionsOnSeriesTimeChange";

    /**
     * list of disabled fields in calendar location web UI
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1218)
    public static final String A_zimbraCalendarLocationDisabledFields = "zimbraCalendarLocationDisabledFields";

    /**
     * maximum number of revisions to keep for calendar items (appointments
     * and tasks). 0 means unlimited.
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=709)
    public static final String A_zimbraCalendarMaxRevisions = "zimbraCalendarMaxRevisions";

    /**
     * Maximum number of days a DAILY recurrence rule can span; 0 means
     * unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=661)
    public static final String A_zimbraCalendarRecurrenceDailyMaxDays = "zimbraCalendarRecurrenceDailyMaxDays";

    /**
     * Maximum number of instances expanded per recurrence rule; 0 means
     * unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=660)
    public static final String A_zimbraCalendarRecurrenceMaxInstances = "zimbraCalendarRecurrenceMaxInstances";

    /**
     * Maximum number of months a MONTHLY recurrence rule can span; 0 means
     * unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=663)
    public static final String A_zimbraCalendarRecurrenceMonthlyMaxMonths = "zimbraCalendarRecurrenceMonthlyMaxMonths";

    /**
     * Maximum number of years a recurrence rule can span for frequencies
     * other than DAILY/WEEKLY/MONTHLY/YEARLY; 0 means unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=665)
    public static final String A_zimbraCalendarRecurrenceOtherFrequencyMaxYears = "zimbraCalendarRecurrenceOtherFrequencyMaxYears";

    /**
     * Maximum number of weeks a WEEKLY recurrence rule can span; 0 means
     * unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=662)
    public static final String A_zimbraCalendarRecurrenceWeeklyMaxWeeks = "zimbraCalendarRecurrenceWeeklyMaxWeeks";

    /**
     * Maximum number of years a YEARLY recurrence rule can span; 0 means
     * unlimited
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=664)
    public static final String A_zimbraCalendarRecurrenceYearlyMaxYears = "zimbraCalendarRecurrenceYearlyMaxYears";

    /**
     * email address identifying the default device for receiving reminders
     * for appointments and tasks
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1140)
    public static final String A_zimbraCalendarReminderDeviceEmail = "zimbraCalendarReminderDeviceEmail";

    /**
     * whether calendar resources can be double booked
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1087)
    public static final String A_zimbraCalendarResourceDoubleBookingAllowed = "zimbraCalendarResourceDoubleBookingAllowed";

    /**
     * Object classes to add when creating a zimbra calendar resource object.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=753)
    public static final String A_zimbraCalendarResourceExtraObjectClass = "zimbraCalendarResourceExtraObjectClass";

    /**
     * whether to show Find Locations and Find Resources tabs for editing
     * appointments
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1092)
    public static final String A_zimbraCalendarShowResourceTabs = "zimbraCalendarShowResourceTabs";

    /**
     * Whether this calendar resource accepts/declines meeting invites
     * automatically; default TRUE
     */
    @ZAttr(id=315)
    public static final String A_zimbraCalResAutoAcceptDecline = "zimbraCalResAutoAcceptDecline";

    /**
     * Whether this calendar resource declines invite if already busy;
     * default TRUE
     */
    @ZAttr(id=322)
    public static final String A_zimbraCalResAutoDeclineIfBusy = "zimbraCalResAutoDeclineIfBusy";

    /**
     * Whether this calendar resource declines invites to recurring
     * appointments; default FALSE
     */
    @ZAttr(id=323)
    public static final String A_zimbraCalResAutoDeclineRecurring = "zimbraCalResAutoDeclineRecurring";

    /**
     * building number or name
     */
    @ZAttr(id=327)
    public static final String A_zimbraCalResBuilding = "zimbraCalResBuilding";

    /**
     * capacity
     */
    @ZAttr(id=330)
    public static final String A_zimbraCalResCapacity = "zimbraCalResCapacity";

    /**
     * email of contact in charge of resource
     */
    @ZAttr(id=332)
    public static final String A_zimbraCalResContactEmail = "zimbraCalResContactEmail";

    /**
     * name of contact in charge of resource
     */
    @ZAttr(id=331)
    public static final String A_zimbraCalResContactName = "zimbraCalResContactName";

    /**
     * phone number of contact in charge of resource
     */
    @ZAttr(id=333)
    public static final String A_zimbraCalResContactPhone = "zimbraCalResContactPhone";

    /**
     * floor number or name
     */
    @ZAttr(id=328)
    public static final String A_zimbraCalResFloor = "zimbraCalResFloor";

    /**
     * display name for resource location
     */
    @ZAttr(id=324)
    public static final String A_zimbraCalResLocationDisplayName = "zimbraCalResLocationDisplayName";

    /**
     * Maximum number of conflicting instances allowed before declining
     * schedule request for a recurring appointments; default 0 (means
     * decline on any conflict)
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=808)
    public static final String A_zimbraCalResMaxNumConflictsAllowed = "zimbraCalResMaxNumConflictsAllowed";

    /**
     * Maximum percent of conflicting instances allowed before declining
     * schedule request for a recurring appointment; default 0 (means decline
     * on any conflict)
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=809)
    public static final String A_zimbraCalResMaxPercentConflictsAllowed = "zimbraCalResMaxPercentConflictsAllowed";

    /**
     * room number or name
     */
    @ZAttr(id=329)
    public static final String A_zimbraCalResRoom = "zimbraCalResRoom";

    /**
     * site name
     */
    @ZAttr(id=326)
    public static final String A_zimbraCalResSite = "zimbraCalResSite";

    /**
     * calendar resource type - Location or Equipment
     */
    @ZAttr(id=314)
    public static final String A_zimbraCalResType = "zimbraCalResType";

    /**
     * When creating self-signed SSL certs during an install, we also create
     * a local Certificate Authority (CA) to sign these SSL certs. This local
     * CA-s own cert is then added to different applications &quot;trusted
     * CA-s&quot; list/store. This attribute should not be used in a system
     * with real certs issued by well known CAs.
     */
    @ZAttr(id=280)
    public static final String A_zimbraCertAuthorityCertSelfSigned = "zimbraCertAuthorityCertSelfSigned";

    /**
     * Please see the documentation for the attribute
     * zimbraCertAuthorityCertSelfSigned. In addition, please note that this
     * attribute is provided at install for convenience during a test install
     * without real certs issued by well known CAs. If you choose to create
     * your own CA for your production uses, please note that it is a bad
     * idea to store your CA-s private key in LDAP, as this data maybe read
     * from zimbraGlobalConfig in the clear.
     */
    @ZAttr(id=279)
    public static final String A_zimbraCertAuthorityKeySelfSigned = "zimbraCertAuthorityKeySelfSigned";

    /**
     * change password URL
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=777)
    public static final String A_zimbraChangePasswordURL = "zimbraChangePasswordURL";

    /**
     * zimbraId of child accounts
     */
    @ZAttr(id=449)
    public static final String A_zimbraChildAccount = "zimbraChildAccount";

    /**
     * Deprecated since: 5.0.0. deprecated in favor of user-settable
     * attribute zimbraPrefChildVisibleAccount . Orig desc: zimbraId of
     * visible child accounts
     */
    @ZAttr(id=450)
    public static final String A_zimbraChildVisibleAccount = "zimbraChildVisibleAccount";

    /**
     * Type of HA cluster software in use; &quot;none&quot; by default,
     * &quot;RedHat&quot; for Red Hat cluster or &quot;Veritas&quot; for
     * Veritas Cluster Server from Symantec
     */
    @ZAttr(id=508)
    public static final String A_zimbraClusterType = "zimbraClusterType";

    /**
     * Names of additional components that have been installed
     */
    @ZAttr(id=242)
    public static final String A_zimbraComponentAvailable = "zimbraComponentAvailable";

    /**
     * attribute constraints TODO: fill all the constraints
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=766)
    public static final String A_zimbraConstraint = "zimbraConstraint";

    /**
     * Deprecated since: 6.0.7. deprecated in favor of
     * zimbraContactEmailFields, for bug 45475. Orig desc: Comma separates
     * list of attributes in contact object to search for email addresses
     * when generating auto-complete contact list. The same set of fields are
     * used for GAL contacts as well because LDAP attributes for GAL objects
     * are mapped to Contact compatible attributes via zimbraGalLdapAttrMap.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=760)
    public static final String A_zimbraContactAutoCompleteEmailFields = "zimbraContactAutoCompleteEmailFields";

    /**
     * maximum number of contact entries to return from an auto complete
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=827)
    public static final String A_zimbraContactAutoCompleteMaxResults = "zimbraContactAutoCompleteMaxResults";

    /**
     * Comma separates list of attributes in contact object to search for
     * email addresses when generating auto-complete contact list. The same
     * set of fields are used for GAL contacts as well because LDAP
     * attributes for GAL objects are mapped to Contact compatible attributes
     * via zimbraGalLdapAttrMap.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1088)
    public static final String A_zimbraContactEmailFields = "zimbraContactEmailFields";

    /**
     * Comma separated list of Contact attributes that should be hidden from
     * clients and export of contacts.
     *
     * @since ZCS 6.0.6
     */
    @ZAttr(id=1086)
    public static final String A_zimbraContactHiddenAttributes = "zimbraContactHiddenAttributes";

    /**
     * Maximum number of contacts allowed in mailbox. 0 means no limit.
     */
    @ZAttr(id=107)
    public static final String A_zimbraContactMaxNumEntries = "zimbraContactMaxNumEntries";

    /**
     * Deprecated since: 6.0.6. Deprecated per bug 40081. Orig desc: How
     * often do we refresh contact ranking table from address book and GAL to
     * get friendly name for the email address. Use 0 to disable the
     * refresh.. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1023)
    public static final String A_zimbraContactRankingTableRefreshInterval = "zimbraContactRankingTableRefreshInterval";

    /**
     * Size of the contact ranking table. Ranking table is used to keep track
     * of most heavily used contacts in outgoing email. Contacts in the
     * ranking table are given the priority when generating the auto-complete
     * contact list.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=758)
    public static final String A_zimbraContactRankingTableSize = "zimbraContactRankingTableSize";

    /**
     * convertd URL
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=776)
    public static final String A_zimbraConvertdURL = "zimbraConvertdURL";

    /**
     * Object classes to add when creating a zimbra cos object.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=754)
    public static final String A_zimbraCosExtraObjectClass = "zimbraCosExtraObjectClass";

    /**
     * COS zimbraID
     */
    @ZAttr(id=14)
    public static final String A_zimbraCOSId = "zimbraCOSId";

    /**
     * Deprecated since: 5.0. deprecated in favor of the accountInherited
     * flag. Orig desc: zimbraCOS attrs that get inherited in a zimbraAccount
     */
    @ZAttr(id=21)
    public static final String A_zimbraCOSInheritedAttr = "zimbraCOSInheritedAttr";

    /**
     * time object was created
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=790)
    public static final String A_zimbraCreateTimestamp = "zimbraCreateTimestamp";

    /**
     * set to 1 or 3 to specify customer care account tier level
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=605)
    public static final String A_zimbraCustomerCareTier = "zimbraCustomerCareTier";

    /**
     * Custom RFC822 header names (case-sensitive) allowed to specify in
     * SendMsgRequest
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1265)
    public static final String A_zimbraCustomMimeHeaderNameAllowed = "zimbraCustomMimeHeaderNameAllowed";

    /**
     * SQL statements that take longer than this duration to execute will be
     * logged to the sqltrace category in mailbox.log.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1038)
    public static final String A_zimbraDatabaseSlowSqlThreshold = "zimbraDatabaseSlowSqlThreshold";

    /**
     * properties for data source
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=718)
    public static final String A_zimbraDataSourceAttribute = "zimbraDataSourceAttribute";

    /**
     * Which SASL authentication mechanism to use for authenticating to IMAP
     * server.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1107)
    public static final String A_zimbraDataSourceAuthMechanism = "zimbraDataSourceAuthMechanism";

    /**
     * authorizationId for SASL authentication
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1108)
    public static final String A_zimbraDataSourceAuthorizationId = "zimbraDataSourceAuthorizationId";

    /**
     * The time interval between automated data imports for a Caldav data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=788)
    public static final String A_zimbraDataSourceCaldavPollingInterval = "zimbraDataSourceCaldavPollingInterval";

    /**
     * The time interval between automated data imports for a remote calendar
     * data source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=819)
    public static final String A_zimbraDataSourceCalendarPollingInterval = "zimbraDataSourceCalendarPollingInterval";

    /**
     * Which security layer to use for connection (cleartext, ssl, tls, or
     * tls if available). If not set on data source, fallback to the value on
     * global config.
     */
    @ZAttr(id=425)
    public static final String A_zimbraDataSourceConnectionType = "zimbraDataSourceConnectionType";

    /**
     * Connect timeout in seconds for the data source
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1083)
    public static final String A_zimbraDataSourceConnectTimeout = "zimbraDataSourceConnectTimeout";

    /**
     * domain name of data source
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1037)
    public static final String A_zimbraDataSourceDomain = "zimbraDataSourceDomain";

    /**
     * email address for the data source
     */
    @ZAttr(id=495)
    public static final String A_zimbraDataSourceEmailAddress = "zimbraDataSourceEmailAddress";

    /**
     * Whether or not the data source is enabled
     */
    @ZAttr(id=419)
    public static final String A_zimbraDataSourceEnabled = "zimbraDataSourceEnabled";

    /**
     * Whether to enable debug trace of this data source
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=683)
    public static final String A_zimbraDataSourceEnableTrace = "zimbraDataSourceEnableTrace";

    /**
     * Timestamp of the first sync error for a data source. This value is
     * unset after a successful sync.
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1030)
    public static final String A_zimbraDataSourceFailingSince = "zimbraDataSourceFailingSince";

    /**
     * Local folder id to store retreived data in
     */
    @ZAttr(id=424)
    public static final String A_zimbraDataSourceFolderId = "zimbraDataSourceFolderId";

    /**
     * The time interval between automated data imports for a GAL data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=826)
    public static final String A_zimbraDataSourceGalPollingInterval = "zimbraDataSourceGalPollingInterval";

    /**
     * Host name of server
     */
    @ZAttr(id=420)
    public static final String A_zimbraDataSourceHost = "zimbraDataSourceHost";

    /**
     * Unique ID for a data source
     */
    @ZAttr(id=417)
    public static final String A_zimbraDataSourceId = "zimbraDataSourceId";

    /**
     * The time interval between automated data imports for an Imap data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=768)
    public static final String A_zimbraDataSourceImapPollingInterval = "zimbraDataSourceImapPollingInterval";

    /**
     * DataImport class used by this data source object
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=717)
    public static final String A_zimbraDataSourceImportClassName = "zimbraDataSourceImportClassName";

    /**
     * indicates that this datasource is used for one way (incoming) import
     * vs. two-way sync
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1106)
    public static final String A_zimbraDataSourceImportOnly = "zimbraDataSourceImportOnly";

    /**
     * If TRUE, the data source is hidden from the UI.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1126)
    public static final String A_zimbraDataSourceIsInternal = "zimbraDataSourceIsInternal";

    /**
     * If the last data source sync failed, contains the error message. If
     * the last data source sync succeeded, this attribute is unset.
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1029)
    public static final String A_zimbraDataSourceLastError = "zimbraDataSourceLastError";

    /**
     * Specifies whether imported POP3 messages should be left on the server
     * or deleted.
     */
    @ZAttr(id=434)
    public static final String A_zimbraDataSourceLeaveOnServer = "zimbraDataSourceLeaveOnServer";

    /**
     * The time interval between automated data imports for a Live data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=769)
    public static final String A_zimbraDataSourceLivePollingInterval = "zimbraDataSourceLivePollingInterval";

    /**
     * Maximum number of data sources allowed on an account
     */
    @ZAttr(id=426)
    public static final String A_zimbraDataSourceMaxNumEntries = "zimbraDataSourceMaxNumEntries";

    /**
     * Message content data exceeding this size will not be included in IMAP
     * trace file
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1033)
    public static final String A_zimbraDataSourceMaxTraceSize = "zimbraDataSourceMaxTraceSize";

    /**
     * Shortest allowed duration for zimbraDataSourcePollingInterval.. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=525)
    public static final String A_zimbraDataSourceMinPollingInterval = "zimbraDataSourceMinPollingInterval";

    /**
     * Descriptive name of the data source
     */
    @ZAttr(id=418)
    public static final String A_zimbraDataSourceName = "zimbraDataSourceName";

    /**
     * Password on server
     */
    @ZAttr(id=423)
    public static final String A_zimbraDataSourcePassword = "zimbraDataSourcePassword";

    /**
     * Prior to 6.0.0: The time interval between automated data imports for a
     * data source, or all data sources owned by an account. If unset or 0,
     * the data source will not be scheduled for automated polling. Since
     * 6.0.0: Deprecated on account/cos since 6.0.0. Values on account/cos
     * are migrated to protocol specific
     * zimbraDataSource{proto}PollingInterval attributes. 1. if
     * zimbraDataSourcePollingInterval is set on data source, use it 2.
     * otherwise use the zimbraDataSource{Proto}PollingInterval on
     * account/cos 3. if zimbraDataSource{Proto}PollingInterval is not set on
     * account/cos, use 0, which means no automated polling. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=455)
    public static final String A_zimbraDataSourcePollingInterval = "zimbraDataSourcePollingInterval";

    /**
     * The time interval between automated data imports for a Pop3 data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=767)
    public static final String A_zimbraDataSourcePop3PollingInterval = "zimbraDataSourcePop3PollingInterval";

    /**
     * Port number of server
     */
    @ZAttr(id=421)
    public static final String A_zimbraDataSourcePort = "zimbraDataSourcePort";

    /**
     * Read timeout in seconds
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1084)
    public static final String A_zimbraDataSourceReadTimeout = "zimbraDataSourceReadTimeout";

    /**
     * The time interval between automated data imports for a Rss data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=770)
    public static final String A_zimbraDataSourceRssPollingInterval = "zimbraDataSourceRssPollingInterval";

    /**
     * type of data source (pop3, imap, caldav, etc)
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=716)
    public static final String A_zimbraDataSourceType = "zimbraDataSourceType";

    /**
     * when forwarding or replying to messages sent to this data source,
     * whether or not to use the email address of the data source for the
     * from address and the designated signature/replyTo of the data source
     * for the outgoing message.
     */
    @ZAttr(id=496)
    public static final String A_zimbraDataSourceUseAddressForForwardReply = "zimbraDataSourceUseAddressForForwardReply";

    /**
     * Username on server
     */
    @ZAttr(id=422)
    public static final String A_zimbraDataSourceUsername = "zimbraDataSourceUsername";

    /**
     * The time interval between automated data imports for a Yahoo address
     * book data source. If unset or 0, the data source will not be scheduled
     * for automated polling. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=789)
    public static final String A_zimbraDataSourceYabPollingInterval = "zimbraDataSourceYabPollingInterval";

    /**
     * For selective enabling of debug logging
     */
    @ZAttr(id=365)
    public static final String A_zimbraDebugInfo = "zimbraDebugInfo";

    /**
     * name of the default domain for accounts when authenticating without a
     * domain
     */
    @ZAttr(id=172)
    public static final String A_zimbraDefaultDomainName = "zimbraDefaultDomainName";

    /**
     * Default flags on folder. These are set when a new folder is created,
     * has no effect on existing folders. Possible values are: * -
     * \Subscribed b - \ExcludeFB # - \Checked i - \NoInherit y - \SyncFolder
     * ~ - \Sync o - \Noinferiors g - \Global
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1210)
    public static final String A_zimbraDefaultFolderFlags = "zimbraDefaultFolderFlags";

    /**
     * Email address to put in from header for the share info email. If not
     * set, email address of the authenticated admin account will be used.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=811)
    public static final String A_zimbraDistributionListSendShareMessageFromAddress = "zimbraDistributionListSendShareMessageFromAddress";

    /**
     * Whether to send an email with all the shares of the group when a new
     * member is added to the group. If not set, default is to send the
     * email.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=810)
    public static final String A_zimbraDistributionListSendShareMessageToNewMembers = "zimbraDistributionListSendShareMessageToNewMembers";

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1275)
    public static final String A_zimbraDistributionListSubscriptionPolicy = "zimbraDistributionListSubscriptionPolicy";

    /**
     * distribution subscription policy. ACCEPT: always accept, REJECT:
     * always reject, APPROVAL: require owners approval.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1276)
    public static final String A_zimbraDistributionListUnsubscriptionPolicy = "zimbraDistributionListUnsubscriptionPolicy";

    /**
     * This attribute is used for DNS check by customers that configure their
     * MX to point at spam relays or other non-zimbra inbox smtp servers
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=744)
    public static final String A_zimbraDNSCheckHostname = "zimbraDNSCheckHostname";

    /**
     * maximum amount of mail quota a domain admin can set on a user
     */
    @ZAttr(id=398)
    public static final String A_zimbraDomainAdminMaxMailQuota = "zimbraDomainAdminMaxMailQuota";

    /**
     * Deprecated since: 5.0. deprecated in favor of the
     * domainAdminAdminModifiable flag. Orig desc: account attributes that a
     * domain administrator is allowed to modify
     */
    @ZAttr(id=300)
    public static final String A_zimbraDomainAdminModifiableAttr = "zimbraDomainAdminModifiableAttr";

    /**
     * maximum aggregate quota for the domain in bytes
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1327)
    public static final String A_zimbraDomainAggregateQuota = "zimbraDomainAggregateQuota";

    /**
     * policy for a domain whose quota usage is above
     * zimbraDomainAggregateQuota
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1329)
    public static final String A_zimbraDomainAggregateQuotaPolicy = "zimbraDomainAggregateQuotaPolicy";

    /**
     * email recipients to be notified when zimbraAggregateQuotaLastUsage
     * reaches zimbraDomainAggregateQuotaWarnPercent of the
     * zimbraDomainAggregateQuota
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1331)
    public static final String A_zimbraDomainAggregateQuotaWarnEmailRecipient = "zimbraDomainAggregateQuotaWarnEmailRecipient";

    /**
     * percentage threshold for domain aggregate quota warnings
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1330)
    public static final String A_zimbraDomainAggregateQuotaWarnPercent = "zimbraDomainAggregateQuotaWarnPercent";

    /**
     * zimbraId of domain alias target
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public static final String A_zimbraDomainAliasTargetId = "zimbraDomainAliasTargetId";

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain. Values are in the format of
     * {zimbraId-of-a-cos}:{max-accounts}
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=714)
    public static final String A_zimbraDomainCOSMaxAccounts = "zimbraDomainCOSMaxAccounts";

    /**
     * COS zimbraID
     */
    @ZAttr(id=299)
    public static final String A_zimbraDomainDefaultCOSId = "zimbraDomainDefaultCOSId";

    /**
     * id of the default COS for external user accounts
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1247)
    public static final String A_zimbraDomainDefaultExternalUserCOSId = "zimbraDomainDefaultExternalUserCOSId";

    /**
     * Object classes to add when creating a zimbra domain object.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=755)
    public static final String A_zimbraDomainExtraObjectClass = "zimbraDomainExtraObjectClass";

    /**
     * maximum number of accounts allowed to have specified features in a
     * domain
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=715)
    public static final String A_zimbraDomainFeatureMaxAccounts = "zimbraDomainFeatureMaxAccounts";

    /**
     * ZimbraID of the domain that this component is registered under
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=741)
    public static final String A_zimbraDomainId = "zimbraDomainId";

    /**
     * Deprecated since: 5.0. deprecated in favor of the domainInherited
     * flag. Orig desc: zimbraDomain attrs that get inherited from global
     * config
     */
    @ZAttr(id=63)
    public static final String A_zimbraDomainInheritedAttr = "zimbraDomainInheritedAttr";

    /**
     * enable domain mandatory mail signature
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1069)
    public static final String A_zimbraDomainMandatoryMailSignatureEnabled = "zimbraDomainMandatoryMailSignatureEnabled";

    /**
     * domain mandatory mail html signature
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1071)
    public static final String A_zimbraDomainMandatoryMailSignatureHTML = "zimbraDomainMandatoryMailSignatureHTML";

    /**
     * domain mandatory mail plain text signature
     *
     * @since ZCS 6.0.4
     */
    @ZAttr(id=1070)
    public static final String A_zimbraDomainMandatoryMailSignatureText = "zimbraDomainMandatoryMailSignatureText";

    /**
     * maximum number of accounts allowed in a domain
     */
    @ZAttr(id=400)
    public static final String A_zimbraDomainMaxAccounts = "zimbraDomainMaxAccounts";

    /**
     * name of the domain
     */
    @ZAttr(id=19)
    public static final String A_zimbraDomainName = "zimbraDomainName";

    /**
     * domain rename info/status
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=536)
    public static final String A_zimbraDomainRenameInfo = "zimbraDomainRenameInfo";

    /**
     * domain status. enum values are akin to those of zimbraAccountStatus
     * but the status affects all accounts on the domain. See table below for
     * how zimbraDomainStatus affects account status. active - see
     * zimbraAccountStatus maintenance - see zimbraAccountStatus locked - see
     * zimbraAccountStatus closed - see zimbraAccountStatus suspended -
     * maintenance + no creating/deleting/modifying accounts/DLs under the
     * domain. shutdown - suspended + cannot modify domain attrs + cannot
     * delete the domain Indicating server is doing major and lengthy
     * maintenance work on the domain, e.g. renaming the domain and moving
     * LDAP entries. Modification and deletion of the domain can only be done
     * internally by the server when it is safe to release the domain, they
     * cannot be done in admin console or zmprov. How zimbraDomainStatus
     * affects account behavior : -------------------------------------
     * zimbraDomainStatus account behavior
     * ------------------------------------- active zimbraAccountStatus
     * locked zimbraAccountStatus if it is maintenance or pending or closed,
     * else locked maintenance zimbraAccountStatus if it is pending or
     * closed, else maintenance suspended zimbraAccountStatus if it is
     * pending or closed, else maintenance shutdown zimbraAccountStatus if it
     * is pending or closed, else maintenance closed closed
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=535)
    public static final String A_zimbraDomainStatus = "zimbraDomainStatus";

    /**
     * should be one of: local, alias
     */
    @ZAttr(id=212)
    public static final String A_zimbraDomainType = "zimbraDomainType";

    /**
     * enable/disable dumpster
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1128)
    public static final String A_zimbraDumpsterEnabled = "zimbraDumpsterEnabled";

    /**
     * disables purging from dumpster when set to FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1315)
    public static final String A_zimbraDumpsterPurgeEnabled = "zimbraDumpsterPurgeEnabled";

    /**
     * limits how much of a dumpster data is viewable by the end user, based
     * on the age since being put in dumpster. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1314)
    public static final String A_zimbraDumpsterUserVisibleAge = "zimbraDumpsterUserVisibleAge";

    /**
     * URL for posting error report popped up in WEB client
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1075)
    public static final String A_zimbraErrorReportUrl = "zimbraErrorReportUrl";

    /**
     * Indicates the account should be excluded from Crossmailbox searchers.
     */
    @ZAttr(id=501)
    public static final String A_zimbraExcludeFromCMBSearch = "zimbraExcludeFromCMBSearch";

    /**
     * the handler class for getting all groups an account belongs to in the
     * external directory
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1251)
    public static final String A_zimbraExternalGroupHandlerClass = "zimbraExternalGroupHandlerClass";

    /**
     * LDAP search base for searching external LDAP groups
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1249)
    public static final String A_zimbraExternalGroupLdapSearchBase = "zimbraExternalGroupLdapSearchBase";

    /**
     * LDAP search filter for searching external LDAP groups
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1250)
    public static final String A_zimbraExternalGroupLdapSearchFilter = "zimbraExternalGroupLdapSearchFilter";

    /**
     * external imap hostname
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=786)
    public static final String A_zimbraExternalImapHostname = "zimbraExternalImapHostname";

    /**
     * external imap port
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=782)
    public static final String A_zimbraExternalImapPort = "zimbraExternalImapPort";

    /**
     * external imap SSL hostname
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=787)
    public static final String A_zimbraExternalImapSSLHostname = "zimbraExternalImapSSLHostname";

    /**
     * external imap SSL port
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=783)
    public static final String A_zimbraExternalImapSSLPort = "zimbraExternalImapSSLPort";

    /**
     * external pop3 hostname
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=784)
    public static final String A_zimbraExternalPop3Hostname = "zimbraExternalPop3Hostname";

    /**
     * external pop3 port
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=780)
    public static final String A_zimbraExternalPop3Port = "zimbraExternalPop3Port";

    /**
     * external pop3 SSL hostname
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=785)
    public static final String A_zimbraExternalPop3SSLHostname = "zimbraExternalPop3SSLHostname";

    /**
     * external pop3 SSL port
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=781)
    public static final String A_zimbraExternalPop3SSLPort = "zimbraExternalPop3SSLPort";

    /**
     * whether checking against zimbraExternalShareWhitelistDomain for
     * external user sharing is enabled
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1264)
    public static final String A_zimbraExternalShareDomainWhitelistEnabled = "zimbraExternalShareDomainWhitelistEnabled";

    /**
     * Duration for which the URL sent in the share invitation email to an
     * external user is valid. A value of 0 indicates that the URL never
     * expires. . Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1349)
    public static final String A_zimbraExternalShareInvitationUrlExpiration = "zimbraExternalShareInvitationUrlExpiration";

    /**
     * Maximum allowed lifetime of shares to external users. A value of 0
     * indicates that there&#039;s no limit on an external share&#039;s
     * lifetime. . Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1260)
    public static final String A_zimbraExternalShareLifetime = "zimbraExternalShareLifetime";

    /**
     * list of external domains that users can share files and folders with
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1263)
    public static final String A_zimbraExternalShareWhitelistDomain = "zimbraExternalShareWhitelistDomain";

    /**
     * switch for turning external user sharing on/off
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1261)
    public static final String A_zimbraExternalSharingEnabled = "zimbraExternalSharingEnabled";

    /**
     * External email address of an external user. Applicable only when
     * zimbraIsExternalVirtualAccount is set to TRUE.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1244)
    public static final String A_zimbraExternalUserMailAddress = "zimbraExternalUserMailAddress";

    /**
     * whether email features and tabs are enabled in the web client if
     * accessed from the admin console
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1170)
    public static final String A_zimbraFeatureAdminMailEnabled = "zimbraFeatureAdminMailEnabled";

    /**
     * Deprecated since: 8.0.0. Deprecated as of bug 56924. Orig desc:
     * advanced search button enabled
     */
    @ZAttr(id=138)
    public static final String A_zimbraFeatureAdvancedSearchEnabled = "zimbraFeatureAdvancedSearchEnabled";

    /**
     * whether or not to enable rerouting spam messages to Junk folder in
     * ZCS, exposing Junk folder and actions in the web UI, and exposing Junk
     * folder to IMAP clients.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1168)
    public static final String A_zimbraFeatureAntispamEnabled = "zimbraFeatureAntispamEnabled";

    /**
     * Docs features enabled in briefcase
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1055)
    public static final String A_zimbraFeatureBriefcaseDocsEnabled = "zimbraFeatureBriefcaseDocsEnabled";

    /**
     * whether to allow use of briefcase feature
     */
    @ZAttr(id=498)
    public static final String A_zimbraFeatureBriefcasesEnabled = "zimbraFeatureBriefcasesEnabled";

    /**
     * Slides features enabled in briefcase
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1054)
    public static final String A_zimbraFeatureBriefcaseSlidesEnabled = "zimbraFeatureBriefcaseSlidesEnabled";

    /**
     * Spreadsheet features enabled in briefcase
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1053)
    public static final String A_zimbraFeatureBriefcaseSpreadsheetEnabled = "zimbraFeatureBriefcaseSpreadsheetEnabled";

    /**
     * calendar features
     */
    @ZAttr(id=136)
    public static final String A_zimbraFeatureCalendarEnabled = "zimbraFeatureCalendarEnabled";

    /**
     * whether receiving reminders on the designated device for appointments
     * and tasks is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1150)
    public static final String A_zimbraFeatureCalendarReminderDeviceEmailEnabled = "zimbraFeatureCalendarReminderDeviceEmailEnabled";

    /**
     * calendar upsell enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=531)
    public static final String A_zimbraFeatureCalendarUpsellEnabled = "zimbraFeatureCalendarUpsellEnabled";

    /**
     * calendar upsell URL
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=532)
    public static final String A_zimbraFeatureCalendarUpsellURL = "zimbraFeatureCalendarUpsellURL";

    /**
     * password changing
     */
    @ZAttr(id=141)
    public static final String A_zimbraFeatureChangePasswordEnabled = "zimbraFeatureChangePasswordEnabled";

    /**
     * whether or not compose messages in a new windows is allowed
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=584)
    public static final String A_zimbraFeatureComposeInNewWindowEnabled = "zimbraFeatureComposeInNewWindowEnabled";

    /**
     * whether a confirmation page should be display after an operation is
     * done in the UI
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=806)
    public static final String A_zimbraFeatureConfirmationPageEnabled = "zimbraFeatureConfirmationPageEnabled";

    /**
     * whether detailed contact search UI is enabled
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1164)
    public static final String A_zimbraFeatureContactsDetailedSearchEnabled = "zimbraFeatureContactsDetailedSearchEnabled";

    /**
     * contact features
     */
    @ZAttr(id=135)
    public static final String A_zimbraFeatureContactsEnabled = "zimbraFeatureContactsEnabled";

    /**
     * address book upsell enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=529)
    public static final String A_zimbraFeatureContactsUpsellEnabled = "zimbraFeatureContactsUpsellEnabled";

    /**
     * address book upsell URL
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=530)
    public static final String A_zimbraFeatureContactsUpsellURL = "zimbraFeatureContactsUpsellURL";

    /**
     * conversations
     */
    @ZAttr(id=140)
    public static final String A_zimbraFeatureConversationsEnabled = "zimbraFeatureConversationsEnabled";

    /**
     * enable end-user mail discarding defined in mail filters features
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=773)
    public static final String A_zimbraFeatureDiscardInFiltersEnabled = "zimbraFeatureDiscardInFiltersEnabled";

    /**
     * whether expanding distribution list members feature is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1134)
    public static final String A_zimbraFeatureDistributionListExpandMembersEnabled = "zimbraFeatureDistributionListExpandMembersEnabled";

    /**
     * whether export folder feature is enabled
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1185)
    public static final String A_zimbraFeatureExportFolderEnabled = "zimbraFeatureExportFolderEnabled";

    /**
     * filter prefs enabled
     */
    @ZAttr(id=143)
    public static final String A_zimbraFeatureFiltersEnabled = "zimbraFeatureFiltersEnabled";

    /**
     * whether to allow use of flagging feature
     */
    @ZAttr(id=499)
    public static final String A_zimbraFeatureFlaggingEnabled = "zimbraFeatureFlaggingEnabled";

    /**
     * whether free busy view is enabled in the web UI
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1143)
    public static final String A_zimbraFeatureFreeBusyViewEnabled = "zimbraFeatureFreeBusyViewEnabled";

    /**
     * enable auto-completion from the GAL, zimbraFeatureGalEnabled also has
     * to be enabled for the auto-completion feature
     */
    @ZAttr(id=359)
    public static final String A_zimbraFeatureGalAutoCompleteEnabled = "zimbraFeatureGalAutoCompleteEnabled";

    /**
     * whether GAL features are enabled
     */
    @ZAttr(id=149)
    public static final String A_zimbraFeatureGalEnabled = "zimbraFeatureGalEnabled";

    /**
     * whether GAL sync feature is enabled
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=711)
    public static final String A_zimbraFeatureGalSyncEnabled = "zimbraFeatureGalSyncEnabled";

    /**
     * group calendar features. if set to FALSE, calendar works as a personal
     * calendar and attendees and scheduling etc are turned off in web UI
     */
    @ZAttr(id=481)
    public static final String A_zimbraFeatureGroupCalendarEnabled = "zimbraFeatureGroupCalendarEnabled";

    /**
     * enabled html composing
     */
    @ZAttr(id=219)
    public static final String A_zimbraFeatureHtmlComposeEnabled = "zimbraFeatureHtmlComposeEnabled";

    /**
     * whether to allow use of identities feature
     */
    @ZAttr(id=415)
    public static final String A_zimbraFeatureIdentitiesEnabled = "zimbraFeatureIdentitiesEnabled";

    /**
     * whether user is allowed to retrieve mail from an external IMAP data
     * source
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=568)
    public static final String A_zimbraFeatureImapDataSourceEnabled = "zimbraFeatureImapDataSourceEnabled";

    /**
     * IM features
     */
    @ZAttr(id=305)
    public static final String A_zimbraFeatureIMEnabled = "zimbraFeatureIMEnabled";

    /**
     * Deprecated since: 7.1.0. deprecated in favor of
     * zimbraFeatureImportFolderEnabled and zimbraFeatureExportFolderEnabled
     * for bug 53745. Orig desc: whether import export folder feature is
     * enabled
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=750)
    public static final String A_zimbraFeatureImportExportFolderEnabled = "zimbraFeatureImportExportFolderEnabled";

    /**
     * whether import folder feature is enabled
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1184)
    public static final String A_zimbraFeatureImportFolderEnabled = "zimbraFeatureImportFolderEnabled";

    /**
     * preference to set initial search
     */
    @ZAttr(id=142)
    public static final String A_zimbraFeatureInitialSearchPreferenceEnabled = "zimbraFeatureInitialSearchPreferenceEnabled";

    /**
     * Enable instant notifications
     */
    @ZAttr(id=521)
    public static final String A_zimbraFeatureInstantNotify = "zimbraFeatureInstantNotify";

    /**
     * email features enabled
     */
    @ZAttr(id=489)
    public static final String A_zimbraFeatureMailEnabled = "zimbraFeatureMailEnabled";

    /**
     * enable end-user mail forwarding features
     */
    @ZAttr(id=342)
    public static final String A_zimbraFeatureMailForwardingEnabled = "zimbraFeatureMailForwardingEnabled";

    /**
     * enable end-user mail forwarding defined in mail filters features
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=704)
    public static final String A_zimbraFeatureMailForwardingInFiltersEnabled = "zimbraFeatureMailForwardingInFiltersEnabled";

    /**
     * Deprecated since: 5.0. done via skin template overrides. Orig desc:
     * whether user is allowed to set mail polling interval
     */
    @ZAttr(id=441)
    public static final String A_zimbraFeatureMailPollingIntervalPreferenceEnabled = "zimbraFeatureMailPollingIntervalPreferenceEnabled";

    /**
     * mail priority feature
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=566)
    public static final String A_zimbraFeatureMailPriorityEnabled = "zimbraFeatureMailPriorityEnabled";

    /**
     * whether the send later feature is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1137)
    public static final String A_zimbraFeatureMailSendLaterEnabled = "zimbraFeatureMailSendLaterEnabled";

    /**
     * email upsell enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=527)
    public static final String A_zimbraFeatureMailUpsellEnabled = "zimbraFeatureMailUpsellEnabled";

    /**
     * email upsell URL
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=528)
    public static final String A_zimbraFeatureMailUpsellURL = "zimbraFeatureMailUpsellURL";

    /**
     * whether to allow end user to publish and remove S/MIME certificates to
     * their GAL entry in the web UI
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1183)
    public static final String A_zimbraFeatureManageSMIMECertificateEnabled = "zimbraFeatureManageSMIMECertificateEnabled";

    /**
     * enable end-user to manage zimlets
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1051)
    public static final String A_zimbraFeatureManageZimlets = "zimbraFeatureManageZimlets";

    /**
     * enable/disable MAPI (Microsoft Outlook) Connector
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1127)
    public static final String A_zimbraFeatureMAPIConnectorEnabled = "zimbraFeatureMAPIConnectorEnabled";

    /**
     * whether to enforce mobile policy
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=833)
    public static final String A_zimbraFeatureMobilePolicyEnabled = "zimbraFeatureMobilePolicyEnabled";

    /**
     * whether to permit mobile sync
     */
    @ZAttr(id=347)
    public static final String A_zimbraFeatureMobileSyncEnabled = "zimbraFeatureMobileSyncEnabled";

    /**
     * Whether user can create address books
     *
     * @since ZCS 5.0.4
     */
    @ZAttr(id=631)
    public static final String A_zimbraFeatureNewAddrBookEnabled = "zimbraFeatureNewAddrBookEnabled";

    /**
     * Whether new mail notification feature should be allowed for this
     * account or in this cos
     */
    @ZAttr(id=367)
    public static final String A_zimbraFeatureNewMailNotificationEnabled = "zimbraFeatureNewMailNotificationEnabled";

    /**
     * Deprecated since: 7.0.0. Deprecated per bugs 50465, 56201. Orig desc:
     * Whether notebook feature should be allowed for this account or in this
     * cos
     */
    @ZAttr(id=356)
    public static final String A_zimbraFeatureNotebookEnabled = "zimbraFeatureNotebookEnabled";

    /**
     * whether or not open a new msg/conv in a new windows is allowed
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=585)
    public static final String A_zimbraFeatureOpenMailInNewWindowEnabled = "zimbraFeatureOpenMailInNewWindowEnabled";

    /**
     * whether an account can modify its zimbraPref* attributes
     */
    @ZAttr(id=451)
    public static final String A_zimbraFeatureOptionsEnabled = "zimbraFeatureOptionsEnabled";

    /**
     * Whether out of office reply feature should be allowed for this account
     * or in this cos
     */
    @ZAttr(id=366)
    public static final String A_zimbraFeatureOutOfOfficeReplyEnabled = "zimbraFeatureOutOfOfficeReplyEnabled";

    /**
     * Deprecated since: 8.0.0. Deprecated per bug 56924. Orig desc: whether
     * people search feature is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1109)
    public static final String A_zimbraFeaturePeopleSearchEnabled = "zimbraFeaturePeopleSearchEnabled";

    /**
     * whether user is allowed to retrieve mail from an external POP3 data
     * source
     */
    @ZAttr(id=416)
    public static final String A_zimbraFeaturePop3DataSourceEnabled = "zimbraFeaturePop3DataSourceEnabled";

    /**
     * portal features
     */
    @ZAttr(id=447)
    public static final String A_zimbraFeaturePortalEnabled = "zimbraFeaturePortalEnabled";

    /**
     * whether priority inbox feature is enabled
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1271)
    public static final String A_zimbraFeaturePriorityInboxEnabled = "zimbraFeaturePriorityInboxEnabled";

    /**
     * whether the web UI shows UI elements related to read receipts
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=821)
    public static final String A_zimbraFeatureReadReceiptsEnabled = "zimbraFeatureReadReceiptsEnabled";

    /**
     * saved search feature
     */
    @ZAttr(id=139)
    public static final String A_zimbraFeatureSavedSearchesEnabled = "zimbraFeatureSavedSearchesEnabled";

    /**
     * enabled sharing
     */
    @ZAttr(id=335)
    public static final String A_zimbraFeatureSharingEnabled = "zimbraFeatureSharingEnabled";

    /**
     * Deprecated since: 6.0.0_GA. deprecated. Orig desc: keyboard shortcuts
     * aliases features
     */
    @ZAttr(id=452)
    public static final String A_zimbraFeatureShortcutAliasesEnabled = "zimbraFeatureShortcutAliasesEnabled";

    /**
     * whether to allow use of signature feature
     */
    @ZAttr(id=494)
    public static final String A_zimbraFeatureSignaturesEnabled = "zimbraFeatureSignaturesEnabled";

    /**
     * Whether changing skin is allowed for this account or in this cos
     */
    @ZAttr(id=354)
    public static final String A_zimbraFeatureSkinChangeEnabled = "zimbraFeatureSkinChangeEnabled";

    /**
     * whether S/MIME feature is enabled. Note: SMIME is a Network feature,
     * this attribute is effective only if SMIME is permitted by license.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1186)
    public static final String A_zimbraFeatureSMIMEEnabled = "zimbraFeatureSMIMEEnabled";

    /**
     * message social filters enabled in the web client UI
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1272)
    public static final String A_zimbraFeatureSocialFiltersEnabled = "zimbraFeatureSocialFiltersEnabled";

    /**
     * tagging feature
     */
    @ZAttr(id=137)
    public static final String A_zimbraFeatureTaggingEnabled = "zimbraFeatureTaggingEnabled";

    /**
     * whether to allow use of tasks feature
     */
    @ZAttr(id=436)
    public static final String A_zimbraFeatureTasksEnabled = "zimbraFeatureTasksEnabled";

    /**
     * option to view attachments in html
     */
    @ZAttr(id=312)
    public static final String A_zimbraFeatureViewInHtmlEnabled = "zimbraFeatureViewInHtmlEnabled";

    /**
     * whether or not changing voicemail pin is enabled
     *
     * @since ZCS 5.0.19
     */
    @ZAttr(id=1050)
    public static final String A_zimbraFeatureVoiceChangePinEnabled = "zimbraFeatureVoiceChangePinEnabled";

    /**
     * Voicemail features enabled
     */
    @ZAttr(id=445)
    public static final String A_zimbraFeatureVoiceEnabled = "zimbraFeatureVoiceEnabled";

    /**
     * voice upsell enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=533)
    public static final String A_zimbraFeatureVoiceUpsellEnabled = "zimbraFeatureVoiceUpsellEnabled";

    /**
     * voice upsell URL
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=534)
    public static final String A_zimbraFeatureVoiceUpsellURL = "zimbraFeatureVoiceUpsellURL";

    /**
     * Deprecated since: 6.0.0_GA. deprecated per bug 40170. Orig desc:
     * whether web search feature is enabled
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=602)
    public static final String A_zimbraFeatureWebSearchEnabled = "zimbraFeatureWebSearchEnabled";

    /**
     * Zimbra Assistant enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=544)
    public static final String A_zimbraFeatureZimbraAssistantEnabled = "zimbraFeatureZimbraAssistantEnabled";

    /**
     * template for constructing the body of a file deletion warning message
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1313)
    public static final String A_zimbraFileDeletionNotificationBody = "zimbraFileDeletionNotificationBody";

    /**
     * template for constructing the subject of a file deletion warning
     * message
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1312)
    public static final String A_zimbraFileDeletionNotificationSubject = "zimbraFileDeletionNotificationSubject";

    /**
     * template for constructing the body of a file expiration warning
     * message
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1311)
    public static final String A_zimbraFileExpirationWarningBody = "zimbraFileExpirationWarningBody";

    /**
     * template for constructing the subject of a file expiration warning
     * message
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1310)
    public static final String A_zimbraFileExpirationWarningSubject = "zimbraFileExpirationWarningSubject";

    /**
     * Period of inactivity after which file owner receives a deletion
     * warning email. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1308)
    public static final String A_zimbraFileExpirationWarningThreshold = "zimbraFileExpirationWarningThreshold";

    /**
     * Maximum allowed lifetime of file shares to external users. A value of
     * 0 indicates that there&#039;s no limit on an external file
     * share&#039;s lifetime. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1363)
    public static final String A_zimbraFileExternalShareLifetime = "zimbraFileExternalShareLifetime";

    /**
     * Period of inactivity after which a file gets deleted. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1309)
    public static final String A_zimbraFileLifetime = "zimbraFileLifetime";

    /**
     * Maximum allowed lifetime of public file shares. A value of 0 indicates
     * that there&#039;s no limit on a public file share&#039;s lifetime. .
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1364)
    public static final String A_zimbraFilePublicShareLifetime = "zimbraFilePublicShareLifetime";

    /**
     * Maximum allowed lifetime of file shares to internal users or groups. A
     * value of 0 indicates that there&#039;s no limit on an internal file
     * share&#039;s lifetime. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1362)
    public static final String A_zimbraFileShareLifetime = "zimbraFileShareLifetime";

    /**
     * Maximum size in bytes for attachments
     */
    @ZAttr(id=227)
    public static final String A_zimbraFileUploadMaxSize = "zimbraFileUploadMaxSize";

    /**
     * Maximum size in bytes for each attachment.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1350)
    public static final String A_zimbraFileUploadMaxSizePerFile = "zimbraFileUploadMaxSizePerFile";

    /**
     * whether file versioning is enabled
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1324)
    public static final String A_zimbraFileVersioningEnabled = "zimbraFileVersioningEnabled";

    /**
     * how long a file version is kept around. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1325)
    public static final String A_zimbraFileVersionLifetime = "zimbraFileVersionLifetime";

    /**
     * Maximum number of messages that can be processed in a single
     * ApplyFilterRules operation.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1158)
    public static final String A_zimbraFilterBatchSize = "zimbraFilterBatchSize";

    /**
     * The amount of time to sleep between every two messages during
     * ApplyFilterRules. Increasing this value will even out server load at
     * the expense of slowing down the operation. . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1159)
    public static final String A_zimbraFilterSleepInterval = "zimbraFilterSleepInterval";

    /**
     * foreign name for mapping an external name to a zimbra domain on domain
     * level, it is in the format of {application}:{foreign name}
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1135)
    public static final String A_zimbraForeignName = "zimbraForeignName";

    /**
     * handler for foreign name mapping, it is in the format of
     * {application}:{class name}[:{params}]
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1136)
    public static final String A_zimbraForeignNameHandler = "zimbraForeignNameHandler";

    /**
     * mapping to foreign principal identifier
     */
    @ZAttr(id=295)
    public static final String A_zimbraForeignPrincipal = "zimbraForeignPrincipal";

    /**
     * Exchange user password for free/busy lookup and propagation
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=609)
    public static final String A_zimbraFreebusyExchangeAuthPassword = "zimbraFreebusyExchangeAuthPassword";

    /**
     * auth scheme to use
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=611)
    public static final String A_zimbraFreebusyExchangeAuthScheme = "zimbraFreebusyExchangeAuthScheme";

    /**
     * Exchange username for free/busy lookup and propagation
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=608)
    public static final String A_zimbraFreebusyExchangeAuthUsername = "zimbraFreebusyExchangeAuthUsername";

    /**
     * The duration of f/b block pushed to Exchange server.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public static final String A_zimbraFreebusyExchangeCachedInterval = "zimbraFreebusyExchangeCachedInterval";

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server..
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public static final String A_zimbraFreebusyExchangeCachedIntervalStart = "zimbraFreebusyExchangeCachedIntervalStart";

    /**
     * Can be set to either webdav for Exchange 2007 or older, or ews for
     * 2010 and newer
     *
     * @since ZCS 6.0.11
     */
    @ZAttr(id=1174)
    public static final String A_zimbraFreebusyExchangeServerType = "zimbraFreebusyExchangeServerType";

    /**
     * URL to Exchange server for free/busy lookup and propagation
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=607)
    public static final String A_zimbraFreebusyExchangeURL = "zimbraFreebusyExchangeURL";

    /**
     * O and OU used in legacyExchangeDN attribute
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=610)
    public static final String A_zimbraFreebusyExchangeUserOrg = "zimbraFreebusyExchangeUserOrg";

    /**
     * URLs of external Zimbra servers for free/busy lookup in the form of
     * http[s]://[user:pass@]host:port
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1253)
    public static final String A_zimbraFreebusyExternalZimbraURL = "zimbraFreebusyExternalZimbraURL";

    /**
     * when set to TRUE, free/busy for the account is not calculated from
     * local mailbox.
     *
     * @since ZCS 5.0.11
     */
    @ZAttr(id=752)
    public static final String A_zimbraFreebusyLocalMailboxNotActive = "zimbraFreebusyLocalMailboxNotActive";

    /**
     * The interval to wait when the server encounters problems while
     * propagating Zimbra users free/busy information to external provider
     * such as Exchange. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1026)
    public static final String A_zimbraFreebusyPropagationRetryInterval = "zimbraFreebusyPropagationRetryInterval";

    /**
     * zimbraId of GAL sync accounts
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=831)
    public static final String A_zimbraGalAccountId = "zimbraGalAccountId";

    /**
     * When set to TRUE, GAL search will always include local calendar
     * resources regardless of zimbraGalMode.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1093)
    public static final String A_zimbraGalAlwaysIncludeLocalCalendarResources = "zimbraGalAlwaysIncludeLocalCalendarResources";

    /**
     * LDAP search filter for external GAL auto-complete queries
     */
    @ZAttr(id=360)
    public static final String A_zimbraGalAutoCompleteLdapFilter = "zimbraGalAutoCompleteLdapFilter";

    /**
     * whether to indicate if an email address on a message is a GAL group
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1153)
    public static final String A_zimbraGalGroupIndicatorEnabled = "zimbraGalGroupIndicatorEnabled";

    /**
     * LDAP search base for internal GAL queries (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains)
     */
    @ZAttr(id=358)
    public static final String A_zimbraGalInternalSearchBase = "zimbraGalInternalSearchBase";

    /**
     * the last time at which a syncing attempt failed
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=829)
    public static final String A_zimbraGalLastFailedSyncTimestamp = "zimbraGalLastFailedSyncTimestamp";

    /**
     * the last time at which this GAL data source was successfully synced
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=828)
    public static final String A_zimbraGalLastSuccessfulSyncTimestamp = "zimbraGalLastSuccessfulSyncTimestamp";

    /**
     * LDAP Gal attribute to contact attr mapping
     */
    @ZAttr(id=153)
    public static final String A_zimbraGalLdapAttrMap = "zimbraGalLdapAttrMap";

    /**
     * external LDAP GAL authentication mechanism none: anonymous binding
     * simple: zimbraGalLdapBindDn and zimbraGalLdapBindPassword has to be
     * set kerberos5: zimbraGalLdapKerberos5Principal and
     * zimbraGalLdapKerberos5Keytab has to be set
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=549)
    public static final String A_zimbraGalLdapAuthMech = "zimbraGalLdapAuthMech";

    /**
     * LDAP bind dn for external GAL queries
     */
    @ZAttr(id=49)
    public static final String A_zimbraGalLdapBindDn = "zimbraGalLdapBindDn";

    /**
     * LDAP bind password for external GAL queries
     */
    @ZAttr(id=50)
    public static final String A_zimbraGalLdapBindPassword = "zimbraGalLdapBindPassword";

    /**
     * LDAP search filter for external GAL search queries
     */
    @ZAttr(id=51)
    public static final String A_zimbraGalLdapFilter = "zimbraGalLdapFilter";

    /**
     * LDAP search filter definitions for GAL queries
     */
    @ZAttr(id=52)
    public static final String A_zimbraGalLdapFilterDef = "zimbraGalLdapFilterDef";

    /**
     * the handler class for mapping groups from GAL source to zimbra GAL
     * contacts for external GAL
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1112)
    public static final String A_zimbraGalLdapGroupHandlerClass = "zimbraGalLdapGroupHandlerClass";

    /**
     * kerberos5 keytab file path for external GAL queries
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=551)
    public static final String A_zimbraGalLdapKerberos5Keytab = "zimbraGalLdapKerberos5Keytab";

    /**
     * kerberos5 principal for external GAL queries
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=550)
    public static final String A_zimbraGalLdapKerberos5Principal = "zimbraGalLdapKerberos5Principal";

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled.
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=583)
    public static final String A_zimbraGalLdapPageSize = "zimbraGalLdapPageSize";

    /**
     * LDAP search base for external GAL queries
     */
    @ZAttr(id=48)
    public static final String A_zimbraGalLdapSearchBase = "zimbraGalLdapSearchBase";

    /**
     * whether to use startTLS for external GAL. startTLS will be used for
     * external GAL access only if this attribute is true and
     * zimbraGalLdapURL(or zimbraGalSyncLdapURL for sync) does not contain a
     * ldaps URL.
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=655)
    public static final String A_zimbraGalLdapStartTlsEnabled = "zimbraGalLdapStartTlsEnabled";

    /**
     * LDAP URL for external GAL queries
     */
    @ZAttr(id=47)
    public static final String A_zimbraGalLdapURL = "zimbraGalLdapURL";

    /**
     * LDAP Gal attribute to contact value mapping. Each value is in the
     * format of {gal contact filed}: {regex} {replacement}
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1110)
    public static final String A_zimbraGalLdapValueMap = "zimbraGalLdapValueMap";

    /**
     * maximum number of gal entries to return from a search
     */
    @ZAttr(id=53)
    public static final String A_zimbraGalMaxResults = "zimbraGalMaxResults";

    /**
     * valid modes are &quot;zimbra&quot; (query internal directory only),
     * &quot;ldap&quot; (query external directory only), or &quot;both&quot;
     * (query internal and external directory)
     */
    @ZAttr(id=46)
    public static final String A_zimbraGalMode = "zimbraGalMode";

    /**
     * GAL data source status
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=830)
    public static final String A_zimbraGalStatus = "zimbraGalStatus";

    /**
     * whether to use gal sync account for autocomplete
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1027)
    public static final String A_zimbraGalSyncAccountBasedAutoCompleteEnabled = "zimbraGalSyncAccountBasedAutoCompleteEnabled";

    /**
     * List of attributes that will be ignored when determining whether a GAL
     * contact has been modified. Any change in other attribute values will
     * make the contact &quot;dirty&quot; and the contact will show as
     * modified in the next GAL sync response. By default modifyTimeStamp is
     * always included in ignored attributes. Then if the only change in GAL
     * contact is modifyTimeStamp, the contact will not be shown as modified
     * in the next GAL sync response from the client, thus minimizing the
     * need to download the GAL contact again when none of the meaningful
     * attributes have changed.
     *
     * @since ZCS 6.0.10
     */
    @ZAttr(id=1145)
    public static final String A_zimbraGalSyncIgnoredAttributes = "zimbraGalSyncIgnoredAttributes";

    /**
     * LDAP search base for internal GAL sync (special values:
     * &quot;ROOT&quot; for top, &quot;DOMAIN&quot; for domain only,
     * &quot;SUBDOMAINS&quot; for domain and subdomains) If not set fallback
     * to zimbraGalInternalSearchBase
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=598)
    public static final String A_zimbraGalSyncInternalSearchBase = "zimbraGalSyncInternalSearchBase";

    /**
     * external LDAP GAL authentication mechanism for GAL sync none:
     * anonymous binding simple: zimbraGalLdapBindDn and
     * zimbraGalLdapBindPassword has to be set kerberos5:
     * zimbraGalLdapKerberos5Principal and zimbraGalLdapKerberos5Keytab has
     * to be set if not set fallback to zimbraGalLdapAuthMech
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=592)
    public static final String A_zimbraGalSyncLdapAuthMech = "zimbraGalSyncLdapAuthMech";

    /**
     * LDAP bind dn for external GAL sync queries, if not set fallback to
     * zimbraGalLdapBindDn
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=593)
    public static final String A_zimbraGalSyncLdapBindDn = "zimbraGalSyncLdapBindDn";

    /**
     * LDAP bind password for external GAL sync queries, if not set fallback
     * to zimbraGalLdapBindPassword
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=594)
    public static final String A_zimbraGalSyncLdapBindPassword = "zimbraGalSyncLdapBindPassword";

    /**
     * LDAP search filter for external GAL sync queries, if not set fallback
     * to zimbraGalLdapFilter
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=591)
    public static final String A_zimbraGalSyncLdapFilter = "zimbraGalSyncLdapFilter";

    /**
     * kerberos5 keytab file path for external GAL sync queries, if not set
     * fallback to zimbraGalLdapKerberos5Keytab
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=596)
    public static final String A_zimbraGalSyncLdapKerberos5Keytab = "zimbraGalSyncLdapKerberos5Keytab";

    /**
     * kerberos5 principal for external GAL sync queries, if not set fallback
     * to zimbraGalLdapKerberos5Principal
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=595)
    public static final String A_zimbraGalSyncLdapKerberos5Principal = "zimbraGalSyncLdapKerberos5Principal";

    /**
     * LDAP page size for paged search control while accessing LDAP server
     * for GAL sync. This applies to both Zimbra and external LDAP servers. A
     * value of 0 means paging is not enabled. If not set fallback to
     * zimbraGalLdapPageSize
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=597)
    public static final String A_zimbraGalSyncLdapPageSize = "zimbraGalSyncLdapPageSize";

    /**
     * LDAP search base for external GAL sync queries, if not set fallback to
     * zimbraGalLdapSearchBase
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=590)
    public static final String A_zimbraGalSyncLdapSearchBase = "zimbraGalSyncLdapSearchBase";

    /**
     * whether to use startTLS for external GAL sync, if not set fallback to
     * zimbraGalLdapStartTlsEnabled
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=656)
    public static final String A_zimbraGalSyncLdapStartTlsEnabled = "zimbraGalSyncLdapStartTlsEnabled";

    /**
     * LDAP URL for external GAL sync, if not set fallback to
     * zimbraGalLdapURL
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=589)
    public static final String A_zimbraGalSyncLdapURL = "zimbraGalSyncLdapURL";

    /**
     * Maximum number of concurrent GAL sync requests allowed on the system /
     * domain.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1154)
    public static final String A_zimbraGalSyncMaxConcurrentClients = "zimbraGalSyncMaxConcurrentClients";

    /**
     * LDAP generalized time format for external GAL sync
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1019)
    public static final String A_zimbraGalSyncTimestampFormat = "zimbraGalSyncTimestampFormat";

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * auto complete, if not set, key is not tokenized
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=599)
    public static final String A_zimbraGalTokenizeAutoCompleteKey = "zimbraGalTokenizeAutoCompleteKey";

    /**
     * whether to tokenize key and AND or OR the tokenized queries for GAL
     * search, if not set, key is not tokenized
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=600)
    public static final String A_zimbraGalTokenizeSearchKey = "zimbraGalTokenizeSearchKey";

    /**
     * type of this GAl data source. zimbra - zimbra internal GAL. ldap -
     * external LDAP GAL.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=850)
    public static final String A_zimbraGalType = "zimbraGalType";

    /**
     * Object classes added on the global config entry. Unlike other
     * zimbra***ExtraObjectClass attributes, object classes specified in this
     * attributes will not be automatically added to the global config entry.
     * Extra object class on the global config entry must be added using
     * &quot;zmprov mcf +objectClass {object class}&quot;, then recorded in
     * this attributes.
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1254)
    public static final String A_zimbraGlobalConfigExtraObjectClass = "zimbraGlobalConfigExtraObjectClass";

    /**
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * Zimbra Systems Unique Group ID
     */
    @ZAttr(id=325)
    public static final String A_zimbraGroupId = "zimbraGroupId";

    /**
     * help URL for admin
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=674)
    public static final String A_zimbraHelpAdminURL = "zimbraHelpAdminURL";

    /**
     * help URL for advanced client
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=676)
    public static final String A_zimbraHelpAdvancedURL = "zimbraHelpAdvancedURL";

    /**
     * help URL for delegated admin
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=675)
    public static final String A_zimbraHelpDelegatedURL = "zimbraHelpDelegatedURL";

    /**
     * help URL for standard client
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=677)
    public static final String A_zimbraHelpStandardURL = "zimbraHelpStandardURL";

    /**
     * hide entry in Global Address List
     */
    @ZAttr(id=353)
    public static final String A_zimbraHideInGal = "zimbraHideInGal";

    /**
     * Deprecated since: 6.0.0_BETA2. deprecated in favor for
     * zimbraHsmPolicy. Orig desc: Minimum age of mail items whose filesystem
     * data will be moved to secondary storage.. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     */
    @ZAttr(id=8)
    public static final String A_zimbraHsmAge = "zimbraHsmAge";

    /**
     * Maximum number of items to move during a single HSM operation. If the
     * limit is exceeded, the HSM operation is repeated until all qualifying
     * items are moved.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1316)
    public static final String A_zimbraHsmBatchSize = "zimbraHsmBatchSize";

    /**
     * The policy that determines which mail items get moved to secondary
     * storage during HSM. Each value specifies a comma-separated list of
     * item types and the search query used to select items to move. See the
     * spec for &lt;SearchRequest&gt; for the complete list of item types and
     * query.txt for the search query spec.
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1024)
    public static final String A_zimbraHsmPolicy = "zimbraHsmPolicy";

    /**
     * Whether to enable http debug handler on a server
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1043)
    public static final String A_zimbraHttpDebugHandlerEnabled = "zimbraHttpDebugHandlerEnabled";

    /**
     * number of http handler threads
     */
    @ZAttr(id=518)
    public static final String A_zimbraHttpNumThreads = "zimbraHttpNumThreads";

    /**
     * the http proxy URL to connect to when making outgoing connections
     * (Zimlet proxy, RSS/ATOM feeds, etc)
     */
    @ZAttr(id=388)
    public static final String A_zimbraHttpProxyURL = "zimbraHttpProxyURL";

    /**
     * Deprecated since: 5.0. not applicable for jetty. Orig desc: number of
     * https handler threads
     */
    @ZAttr(id=519)
    public static final String A_zimbraHttpSSLNumThreads = "zimbraHttpSSLNumThreads";

    /**
     * Zimbra Systems Unique ID
     */
    @ZAttr(id=1)
    public static final String A_zimbraId = "zimbraId";

    /**
     * maximum number of identities allowed on an account
     */
    @ZAttr(id=414)
    public static final String A_zimbraIdentityMaxNumEntries = "zimbraIdentityMaxNumEntries";

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     */
    @ZAttr(id=178)
    public static final String A_zimbraImapAdvertisedName = "zimbraImapAdvertisedName";

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     */
    @ZAttr(id=179)
    public static final String A_zimbraImapBindAddress = "zimbraImapBindAddress";

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     */
    @ZAttr(id=268)
    public static final String A_zimbraImapBindOnStartup = "zimbraImapBindOnStartup";

    /**
     * port number on which IMAP server should listen
     */
    @ZAttr(id=180)
    public static final String A_zimbraImapBindPort = "zimbraImapBindPort";

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     */
    @ZAttr(id=185)
    public static final String A_zimbraImapCleartextLoginEnabled = "zimbraImapCleartextLoginEnabled";

    /**
     * disabled IMAP capabilities. Capabilities are listed on the CAPABILITY
     * line, also known in RFCs as extensions
     */
    @ZAttr(id=443)
    public static final String A_zimbraImapDisabledCapability = "zimbraImapDisabledCapability";

    /**
     * whether IMAP is enabled for an account
     */
    @ZAttr(id=174)
    public static final String A_zimbraImapEnabled = "zimbraImapEnabled";

    /**
     * Whether to expose version on IMAP banner
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=693)
    public static final String A_zimbraImapExposeVersionOnBanner = "zimbraImapExposeVersionOnBanner";

    /**
     * Maximum number of concurrent IMAP connections allowed. New connections
     * exceeding this limit are rejected.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1156)
    public static final String A_zimbraImapMaxConnections = "zimbraImapMaxConnections";

    /**
     * maximum size of IMAP request in bytes excluding literal data
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1085)
    public static final String A_zimbraImapMaxRequestSize = "zimbraImapMaxRequestSize";

    /**
     * number of handler threads
     */
    @ZAttr(id=181)
    public static final String A_zimbraImapNumThreads = "zimbraImapNumThreads";

    /**
     * port number on which IMAP proxy server should listen
     */
    @ZAttr(id=348)
    public static final String A_zimbraImapProxyBindPort = "zimbraImapProxyBindPort";

    /**
     * whether IMAP SASL GSSAPI is enabled for a given server
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=555)
    public static final String A_zimbraImapSaslGssapiEnabled = "zimbraImapSaslGssapiEnabled";

    /**
     * whether IMAP is enabled for a server
     */
    @ZAttr(id=176)
    public static final String A_zimbraImapServerEnabled = "zimbraImapServerEnabled";

    /**
     * number of seconds to wait before forcing IMAP server shutdown
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1080)
    public static final String A_zimbraImapShutdownGraceSeconds = "zimbraImapShutdownGraceSeconds";

    /**
     * interface address(es) on which IMAP server should listen; if empty,
     * binds to all interfaces
     */
    @ZAttr(id=182)
    public static final String A_zimbraImapSSLBindAddress = "zimbraImapSSLBindAddress";

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     */
    @ZAttr(id=269)
    public static final String A_zimbraImapSSLBindOnStartup = "zimbraImapSSLBindOnStartup";

    /**
     * port number on which IMAP SSL server should listen on
     */
    @ZAttr(id=183)
    public static final String A_zimbraImapSSLBindPort = "zimbraImapSSLBindPort";

    /**
     * disabled IMAP SSL capabilities. Capabilities are listed on the
     * CAPABILITY line, also known in RFCs as extensions
     */
    @ZAttr(id=444)
    public static final String A_zimbraImapSSLDisabledCapability = "zimbraImapSSLDisabledCapability";

    /**
     * port number on which IMAPS proxy server should listen
     */
    @ZAttr(id=349)
    public static final String A_zimbraImapSSLProxyBindPort = "zimbraImapSSLProxyBindPort";

    /**
     * whether IMAP SSL server is enabled for a given server
     */
    @ZAttr(id=184)
    public static final String A_zimbraImapSSLServerEnabled = "zimbraImapSSLServerEnabled";

    /**
     * available IM interop gateways
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=571)
    public static final String A_zimbraIMAvailableInteropGateways = "zimbraIMAvailableInteropGateways";

    /**
     * interface address(es) on which IM server should listen; if empty,
     * binds to all interfaces
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=567)
    public static final String A_zimbraIMBindAddress = "zimbraIMBindAddress";

    /**
     * Deprecated since: 6.0.0_GA. deprecated per bug 40069. Orig desc: IM
     * service
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=762)
    public static final String A_zimbraIMService = "zimbraIMService";

    /**
     * Deprecated since: 5.0. Installed skin list is a per server property,
     * the list is now generated by directory scan of skin files. Orig desc:
     * Skins installed and available on all servers (this is global config
     * only)
     */
    @ZAttr(id=368)
    public static final String A_zimbraInstalledSkin = "zimbraInstalledSkin";

    /**
     * The address to which legal intercept messages will be sent.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=614)
    public static final String A_zimbraInterceptAddress = "zimbraInterceptAddress";

    /**
     * Template used to construct the body of a legal intercept message.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=618)
    public static final String A_zimbraInterceptBody = "zimbraInterceptBody";

    /**
     * Template used to construct the sender of a legal intercept message.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=616)
    public static final String A_zimbraInterceptFrom = "zimbraInterceptFrom";

    /**
     * Specifies whether legal intercept messages should contain the entire
     * original message or just the headers.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=615)
    public static final String A_zimbraInterceptSendHeadersOnly = "zimbraInterceptSendHeadersOnly";

    /**
     * Template used to construct the subject of a legal intercept message.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=617)
    public static final String A_zimbraInterceptSubject = "zimbraInterceptSubject";

    /**
     * additional domains considered as internal w.r.t. recipient
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1319)
    public static final String A_zimbraInternalSendersDomain = "zimbraInternalSendersDomain";

    /**
     * supported IP mode
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1171)
    public static final String A_zimbraIPMode = "zimbraIPMode";

    /**
     * if the dynamic group can be a legitimate grantee for folder grantees;
     * and a legitimate grantee or target for delegated admin grants
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1242)
    public static final String A_zimbraIsACLGroup = "zimbraIsACLGroup";

    /**
     * set to true for admin accounts
     */
    @ZAttr(id=31)
    public static final String A_zimbraIsAdminAccount = "zimbraIsAdminAccount";

    /**
     * set to true for admin groups
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=802)
    public static final String A_zimbraIsAdminGroup = "zimbraIsAdminGroup";

    /**
     * set to true for customer care accounts
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=601)
    public static final String A_zimbraIsCustomerCareAccount = "zimbraIsCustomerCareAccount";

    /**
     * set to true for delegated admin accounts
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=852)
    public static final String A_zimbraIsDelegatedAdminAccount = "zimbraIsDelegatedAdminAccount";

    /**
     * set to true for domain admin accounts
     */
    @ZAttr(id=298)
    public static final String A_zimbraIsDomainAdminAccount = "zimbraIsDomainAdminAccount";

    /**
     * whether it is an external user account
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1243)
    public static final String A_zimbraIsExternalVirtualAccount = "zimbraIsExternalVirtualAccount";

    /**
     * true if this server is the monitor host
     */
    @ZAttr(id=132)
    public static final String A_zimbraIsMonitorHost = "zimbraIsMonitorHost";

    /**
     * Indicates the account is an account used by the system such as spam
     * accounts or Notebook accounts. System accounts cannot be deleted in
     * admin console.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1214)
    public static final String A_zimbraIsSystemAccount = "zimbraIsSystemAccount";

    /**
     * Indicates the account is a resource used by the system. System
     * resource accounts are not counted against license quota.
     */
    @ZAttr(id=376)
    public static final String A_zimbraIsSystemResource = "zimbraIsSystemResource";

    /**
     * Whether to index junk messages
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=579)
    public static final String A_zimbraJunkMessagesIndexingEnabled = "zimbraJunkMessagesIndexingEnabled";

    /**
     * rough estimate of when the user last logged in. see
     * zimbraLastLogonTimestampFrequency
     */
    @ZAttr(id=113)
    public static final String A_zimbraLastLogonTimestamp = "zimbraLastLogonTimestamp";

    /**
     * how often the zimbraLastLogonTimestamp is updated. if set to 0,
     * updating zimbraLastLogonTimestamp is completely disabled . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=114)
    public static final String A_zimbraLastLogonTimestampFrequency = "zimbraLastLogonTimestampFrequency";

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     */
    @ZAttr(id=23)
    public static final String A_zimbraLmtpAdvertisedName = "zimbraLmtpAdvertisedName";

    /**
     * interface address(es) on which LMTP server should listen; if empty,
     * binds to all interfaces
     */
    @ZAttr(id=25)
    public static final String A_zimbraLmtpBindAddress = "zimbraLmtpBindAddress";

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     */
    @ZAttr(id=270)
    public static final String A_zimbraLmtpBindOnStartup = "zimbraLmtpBindOnStartup";

    /**
     * port number on which LMTP server should listen
     */
    @ZAttr(id=24)
    public static final String A_zimbraLmtpBindPort = "zimbraLmtpBindPort";

    /**
     * Whether to expose version on LMTP banner
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=691)
    public static final String A_zimbraLmtpExposeVersionOnBanner = "zimbraLmtpExposeVersionOnBanner";

    /**
     * number of handler threads, should match MTA concurrency setting for
     * this server
     */
    @ZAttr(id=26)
    public static final String A_zimbraLmtpNumThreads = "zimbraLmtpNumThreads";

    /**
     * If true, a permanent failure (552) is returned when the user is over
     * quota. If false, a temporary failure (452) is returned.
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=657)
    public static final String A_zimbraLmtpPermanentFailureWhenOverQuota = "zimbraLmtpPermanentFailureWhenOverQuota";

    /**
     * whether LMTP server is enabled for a given server
     *
     * @since ZCS 5.0.4
     */
    @ZAttr(id=630)
    public static final String A_zimbraLmtpServerEnabled = "zimbraLmtpServerEnabled";

    /**
     * number of seconds to wait before forcing LMTP server shutdown
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1082)
    public static final String A_zimbraLmtpShutdownGraceSeconds = "zimbraLmtpShutdownGraceSeconds";

    /**
     * locale of entry, e.g. en_US
     */
    @ZAttr(id=345)
    public static final String A_zimbraLocale = "zimbraLocale";

    /**
     * destination for syslog messages
     */
    @ZAttr(id=250)
    public static final String A_zimbraLogHostname = "zimbraLogHostname";

    /**
     * lifetime of raw log rows in consolidated logger tables. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=263)
    public static final String A_zimbraLogRawLifetime = "zimbraLogRawLifetime";

    /**
     * lifetime of summarized log rows in consolidated logger tables. Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=264)
    public static final String A_zimbraLogSummaryLifetime = "zimbraLogSummaryLifetime";

    /**
     * whether mailbox server should log to syslog
     */
    @ZAttr(id=520)
    public static final String A_zimbraLogToSyslog = "zimbraLogToSyslog";

    /**
     * RFC822 email address of this recipient for accepting mail
     */
    @ZAttr(id=3)
    public static final String A_zimbraMailAddress = "zimbraMailAddress";

    /**
     * optional regex used by web client to validate email address
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1241)
    public static final String A_zimbraMailAddressValidationRegex = "zimbraMailAddressValidationRegex";

    /**
     * RFC822 email address of this recipient for accepting mail
     */
    @ZAttr(id=20)
    public static final String A_zimbraMailAlias = "zimbraMailAlias";

    /**
     * If TRUE, a mailbox that exceeds its quota is still allowed to receive
     * mail, but is not allowed to send.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1099)
    public static final String A_zimbraMailAllowReceiveButNotSendWhenOverQuota = "zimbraMailAllowReceiveButNotSendWhenOverQuota";

    /**
     * Maximum number of entries for per user black list. This restricts the
     * number of values that can be set on the amavisBlacklistSender
     * attribute of an account. If set to 0, the per user white list feature
     * is disabled.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=799)
    public static final String A_zimbraMailBlacklistMaxNumEntries = "zimbraMailBlacklistMaxNumEntries";

    /**
     * serverId:mboxId of mailbox before being moved
     */
    @ZAttr(id=346)
    public static final String A_zimbraMailboxLocationBeforeMove = "zimbraMailboxLocationBeforeMove";

    /**
     * if true, exclude blobs (HSM or not) from mailbox move
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1007)
    public static final String A_zimbraMailboxMoveSkipBlobs = "zimbraMailboxMoveSkipBlobs";

    /**
     * if true, exclude blobs on secondary (HSM) volumes from mailbox move
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1008)
    public static final String A_zimbraMailboxMoveSkipHsmBlobs = "zimbraMailboxMoveSkipHsmBlobs";

    /**
     * if true, exclude search index from mailbox move
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1006)
    public static final String A_zimbraMailboxMoveSkipSearchIndex = "zimbraMailboxMoveSkipSearchIndex";

    /**
     * temp directory for mailbox move
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1175)
    public static final String A_zimbraMailboxMoveTempDir = "zimbraMailboxMoveTempDir";

    /**
     * RFC822 email address for senders outbound messages
     */
    @ZAttr(id=213)
    public static final String A_zimbraMailCanonicalAddress = "zimbraMailCanonicalAddress";

    /**
     * Address to catch all messages to specified domain
     */
    @ZAttr(id=214)
    public static final String A_zimbraMailCatchAllAddress = "zimbraMailCatchAllAddress";

    /**
     * Catch all address to rewrite to
     */
    @ZAttr(id=216)
    public static final String A_zimbraMailCatchAllCanonicalAddress = "zimbraMailCatchAllCanonicalAddress";

    /**
     * Address to deliver catch all messages to
     */
    @ZAttr(id=215)
    public static final String A_zimbraMailCatchAllForwardingAddress = "zimbraMailCatchAllForwardingAddress";

    /**
     * Whether to allow password sent to non-secured port when zimbraMailMode
     * is mixed. If it set to TRUE the server will allow login with clear
     * text AuthRequests and change password with clear text
     * ChangePasswordRequest. If it set to FALSE the server will return an
     * error if an attempt is made to ChangePasswordRequest or AuthRequest.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=791)
    public static final String A_zimbraMailClearTextPasswordEnabled = "zimbraMailClearTextPasswordEnabled";

    /**
     * Maximum size in bytes for the &lt;content &gt; element in SOAP. Mail
     * content larger than this limit will be truncated.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=807)
    public static final String A_zimbraMailContentMaxSize = "zimbraMailContentMaxSize";

    /**
     * RFC822 email address of this recipient for local delivery
     */
    @ZAttr(id=13)
    public static final String A_zimbraMailDeliveryAddress = "zimbraMailDeliveryAddress";

    /**
     * Incoming messages larger than this number of bytes are streamed to
     * disk during LMTP delivery, instead of being read into memory. This
     * limits memory consumption at the expense of higher disk utilization.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=565)
    public static final String A_zimbraMailDiskStreamingThreshold = "zimbraMailDiskStreamingThreshold";

    /**
     * Maximum mailbox quota for the domain in bytes. The effective quota for
     * a mailbox would be the minimum of this and zimbraMailQuota.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1326)
    public static final String A_zimbraMailDomainQuota = "zimbraMailDomainQuota";

    /**
     * Retention period of messages in the dumpster. 0 means that all
     * messages will be retained. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1133)
    public static final String A_zimbraMailDumpsterLifetime = "zimbraMailDumpsterLifetime";

    /**
     * Maximum number of messages to delete during a single transaction when
     * emptying a large folder.
     *
     * @since ZCS 6.0.8
     */
    @ZAttr(id=1097)
    public static final String A_zimbraMailEmptyFolderBatchSize = "zimbraMailEmptyFolderBatchSize";

    /**
     * Deprecated since: 8.0.0. Empty folder operation now always deletes
     * items in batches, hence a threshold is no longer applicable.. Orig
     * desc: Folders that contain more than this many messages will be
     * emptied in batches of size zimbraMailEmptyFolderBatchSize.
     *
     * @since ZCS 6.0.13
     */
    @ZAttr(id=1208)
    public static final String A_zimbraMailEmptyFolderBatchThreshold = "zimbraMailEmptyFolderBatchThreshold";

    /**
     * Number of bytes to buffer in memory per file descriptor in the cache.
     * Larger values result in fewer disk reads, but increase memory
     * consumption.
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1035)
    public static final String A_zimbraMailFileDescriptorBufferSize = "zimbraMailFileDescriptorBufferSize";

    /**
     * Maximum number of file descriptors that are opened for accessing
     * message content.
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1034)
    public static final String A_zimbraMailFileDescriptorCacheSize = "zimbraMailFileDescriptorCacheSize";

    /**
     * RFC822 forwarding address for an account
     */
    @ZAttr(id=12)
    public static final String A_zimbraMailForwardingAddress = "zimbraMailForwardingAddress";

    /**
     * max number of chars in zimbraPrefMailForwardingAddress
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1039)
    public static final String A_zimbraMailForwardingAddressMaxLength = "zimbraMailForwardingAddressMaxLength";

    /**
     * max number of email addresses in zimbraPrefMailForwardingAddress
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1040)
    public static final String A_zimbraMailForwardingAddressMaxNumAddrs = "zimbraMailForwardingAddressMaxNumAddrs";

    /**
     * max size in KB of text emails that will automatically highlight
     * objects
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1213)
    public static final String A_zimbraMailHighlightObjectsMaxSize = "zimbraMailHighlightObjectsMaxSize";

    /**
     * the server hosting the account&#039;s mailbox
     */
    @ZAttr(id=4)
    public static final String A_zimbraMailHost = "zimbraMailHost";

    /**
     * servers that an account can be initially provisioned on
     */
    @ZAttr(id=125)
    public static final String A_zimbraMailHostPool = "zimbraMailHostPool";

    /**
     * idle timeout. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     */
    @ZAttr(id=147)
    public static final String A_zimbraMailIdleSessionTimeout = "zimbraMailIdleSessionTimeout";

    /**
     * When set to true, robots.txt on mailboxd will be set up to keep web
     * crawlers out
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1161)
    public static final String A_zimbraMailKeepOutWebCrawlers = "zimbraMailKeepOutWebCrawlers";

    /**
     * Deprecated since: 5.0.7. deprecated per bug 28842. Orig desc: The id
     * of the last purged mailbox.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=543)
    public static final String A_zimbraMailLastPurgedMailboxId = "zimbraMailLastPurgedMailboxId";

    /**
     * lifetime of a mail message regardless of location. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=106)
    public static final String A_zimbraMailMessageLifetime = "zimbraMailMessageLifetime";

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval. Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=110)
    public static final String A_zimbraMailMinPollingInterval = "zimbraMailMinPollingInterval";

    /**
     * whether to run HTTP or HTTPS or both/mixed mode or redirect mode. See
     * also related attributes zimbraMailPort and zimbraMailSSLPort
     */
    @ZAttr(id=308)
    public static final String A_zimbraMailMode = "zimbraMailMode";

    /**
     * sieve script generated from user outgoing filter rules
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1130)
    public static final String A_zimbraMailOutgoingSieveScript = "zimbraMailOutgoingSieveScript";

    /**
     * HTTP port for end-user UI
     */
    @ZAttr(id=154)
    public static final String A_zimbraMailPort = "zimbraMailPort";

    /**
     * The max number of unsuccessful attempts to connect to the current
     * server (as an upstream). If this number is reached, proxy will refuse
     * to connect to the current server, wait for
     * zimbraMailProxyReconnectTimeout and then try to reconnect. Default
     * value is 1. Setting this to 0 means turning this check off.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1358)
    public static final String A_zimbraMailProxyMaxFails = "zimbraMailProxyMaxFails";

    /**
     * HTTP proxy port
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=626)
    public static final String A_zimbraMailProxyPort = "zimbraMailProxyPort";

    /**
     * the time in sec that proxy will reconnect the current server (as an
     * upstream) after connection errors happened before
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1268)
    public static final String A_zimbraMailProxyReconnectTimeout = "zimbraMailProxyReconnectTimeout";

    /**
     * Maximum number of messages to delete from a folder during a single
     * purge operation. If the limit is exceeded, the mailbox is purged again
     * at the end of the purge cycle until all qualifying messages are
     * purged.
     *
     * @since ZCS 6.0.8
     */
    @ZAttr(id=1096)
    public static final String A_zimbraMailPurgeBatchSize = "zimbraMailPurgeBatchSize";

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=542)
    public static final String A_zimbraMailPurgeSleepInterval = "zimbraMailPurgeSleepInterval";

    /**
     * System purge policy, encoded as metadata. Users can apply these policy
     * elements to their folders and tags. If the system policy changes, user
     * settings are automatically updated with the change.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1239)
    public static final String A_zimbraMailPurgeSystemPolicy = "zimbraMailPurgeSystemPolicy";

    /**
     * If TRUE, a message is purged from Spam based on the date that it was
     * moved to the Spam folder. If FALSE, a message is purged from Spam
     * based on the date that it was added to the mailbox.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1117)
    public static final String A_zimbraMailPurgeUseChangeDateForSpam = "zimbraMailPurgeUseChangeDateForSpam";

    /**
     * If TRUE, a message is purged from trash based on the date that it was
     * moved to the Trash folder. If FALSE, a message is purged from Trash
     * based on the date that it was added to the mailbox.
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=748)
    public static final String A_zimbraMailPurgeUseChangeDateForTrash = "zimbraMailPurgeUseChangeDateForTrash";

    /**
     * mail quota in bytes
     */
    @ZAttr(id=16)
    public static final String A_zimbraMailQuota = "zimbraMailQuota";

    /**
     * If TRUE, the envelope sender of a message redirected by mail filters
     * will be set to the users address. If FALSE, the envelope sender will
     * be set to the From address of the redirected message.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=764)
    public static final String A_zimbraMailRedirectSetEnvelopeSender = "zimbraMailRedirectSetEnvelopeSender";

    /**
     * whether to send back a refer tag in an auth response to force a client
     * redirect. always - always send refer wronghost - send refer if only if
     * the account being authenticated does not live on this mail host
     * reverse-proxied - reverse proxy is in place and should never send
     * refer
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=613)
    public static final String A_zimbraMailReferMode = "zimbraMailReferMode";

    /**
     * sieve script generated from user filter rules
     */
    @ZAttr(id=32)
    public static final String A_zimbraMailSieveScript = "zimbraMailSieveScript";

    /**
     * maximum length of mail signature, 0 means unlimited. If not set,
     * default is 1024
     */
    @ZAttr(id=454)
    public static final String A_zimbraMailSignatureMaxLength = "zimbraMailSignatureMaxLength";

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefJunkLifetime, which is user-modifiable. The
     * shorter duration is used. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     */
    @ZAttr(id=105)
    public static final String A_zimbraMailSpamLifetime = "zimbraMailSpamLifetime";

    /**
     * enable authentication via X.509 Client Certificate. Disabled: client
     * authentication is disabled. NeedClientAuth: client authentication is
     * required during SSL handshake on the SSL mutual authentication
     * port(see zimbraMailSSLClientCertPort). The SSL handshake will fail if
     * the client does not present a certificate to authenticate.
     * WantClientAuth: client authentication is requested during SSL
     * handshake on the SSL mutual authentication port(see
     * zimbraMailSSLClientCertPort). The SSL handshake will still proceed if
     * the client does not present a certificate to authenticate. In the case
     * when client does not send a certificate, user will be redirected to
     * the usual entry page of the requested webapp, where username/password
     * is prompted.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1190)
    public static final String A_zimbraMailSSLClientCertMode = "zimbraMailSSLClientCertMode";

    /**
     * SSL port requesting client certificate for end-user UI
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1199)
    public static final String A_zimbraMailSSLClientCertPort = "zimbraMailSSLClientCertPort";

    /**
     * Map from a certificate field to a Zimbra account key that can uniquely
     * identify a Zimbra account for client certificate authentication. Value
     * is a comma-separated list of mapping rules, each mapping maps a
     * certificate field to a Zimbra account key. Each is attempted in
     * sequence until a unique account can be resolved. e.g. a value can be:
     * SUBJECTALTNAME_OTHERNAME_UPN=zimbraForeignPrincipal,(uid=%{SUBJECT_CN})
     * value: comma-separated mapping-rule mapping-rule:
     * {cert-field-to-zimbra-key-map} | {LDAP-filter}
     * cert-field-to-zimbra-key-map: {certificate-field}={Zimbra-account-key}
     * certificate-field: SUBJECT_{an RDN attr, e.g. CN}: a RND in DN of
     * Subject SUBJECT_DN: entire DN of Subject SUBJECTALTNAME_OTHERNAME_UPN:
     * UPN(aka Principal Name) in otherName in subjectAltName extension
     * SUBJECTALTNAME_RFC822NAME: rfc822Name in subjectAltName extension
     * Zimbra-account-key: name: primary name or any of the aliases of an
     * account zimbraId: zimbraId of an account zimbraForeignPrincipal:
     * zimbraForeignPrincipal of an account. The matching value on the
     * zimbraForeignPrincipal must be prefixed with &quot;cert
     * {supported-certificate-filed}:&quot; e.g. cert
     * SUBJECTALTNAME_OTHERNAME_UPN:123456@mydomain LDAP-filter: An LDAP
     * filter template with placeholders to be substituted by certificate
     * field values. (objectClass=zimbraAccount) is internally ANDed with the
     * supplied filter. e.g.
     * (|(uid=%{SUBJECT_CN})(mail=%{SUBJECTALTNAME_RFC822NAME})) Note: it is
     * recommended not to use LDAP-filter rule, as it will trigger an LDAP
     * search for each cert auth request. LDAP-filter is disabled by default.
     * To enable it globally, set
     * zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled on global config
     * to TRUE. If LDAP-filter is not enabled, all client certificate
     * authentication will fail on domains configured with LDAP-filter.
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1215)
    public static final String A_zimbraMailSSLClientCertPrincipalMap = "zimbraMailSSLClientCertPrincipalMap";

    /**
     * whether to enable LDAP-filter in zimbraMailSSLClientCertPrincipalMap
     *
     * @since ZCS 7.1.2
     */
    @ZAttr(id=1216)
    public static final String A_zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled = "zimbraMailSSLClientCertPrincipalMapLdapFilterEnabled";

    /**
     * SSL port for end-user UI
     */
    @ZAttr(id=166)
    public static final String A_zimbraMailSSLPort = "zimbraMailSSLPort";

    /**
     * SSL client certificate port for HTTP proxy
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1212)
    public static final String A_zimbraMailSSLProxyClientCertPort = "zimbraMailSSLProxyClientCertPort";

    /**
     * SSL port HTTP proxy
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=627)
    public static final String A_zimbraMailSSLProxyPort = "zimbraMailSSLProxyPort";

    /**
     * mail delivery status (enabled/disabled)
     */
    @ZAttr(id=15)
    public static final String A_zimbraMailStatus = "zimbraMailStatus";

    /**
     * The algorithm to use when aggregating new messages into conversations.
     * Possible values are: - &quot;none&quot;: no conversation threading is
     * performed. - &quot;subject&quot;: the message will be threaded based
     * solely on its normalized subject. - &quot;strict&quot;: only the
     * threading message headers (References, In-Reply-To, Message-ID, and
     * Resent-Message-ID) are used to correlate messages. No checking of
     * normalized subjects is performed. - &quot;references&quot;: the same
     * logic as &quot;strict&quot; with the constraints slightly altered so
     * that the non-standard Thread-Index header is considered when threading
     * messages and that a reply message lacking References and In-Reply-To
     * headers will fall back to using subject-based threading. -
     * &quot;subjrefs&quot;: the same logic as &quot;references&quot; with
     * the further caveat that changes in the normalized subject will break a
     * thread in two.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1160)
    public static final String A_zimbraMailThreadingAlgorithm = "zimbraMailThreadingAlgorithm";

    /**
     * where to deliver parameter for use in postfix transport_maps
     */
    @ZAttr(id=247)
    public static final String A_zimbraMailTransport = "zimbraMailTransport";

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     */
    @ZAttr(id=104)
    public static final String A_zimbraMailTrashLifetime = "zimbraMailTrashLifetime";

    /**
     * In our web app, AJAX and standard html client, we have support for
     * adding the HTTP client IP address as X-Originating-IP in an outbound
     * message. We also use the HTTP client IP address in our logging. In the
     * case of standard client making connections to the SOAP layer, the JSP
     * layer tells the SOAP layer in a http header what the remote HTTP
     * client address is. In the case where nginx or some other proxy layer
     * is fronting our webapps, the proxy tells the SOAP/JSP layers in a http
     * header what the real HTTP client s address is. Our SOAP/JSP layers
     * will trust the client/proxy only if the IP address of the client/proxy
     * is one of the IPs listed in this attribute.
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1025)
    public static final String A_zimbraMailTrustedIP = "zimbraMailTrustedIP";

    /**
     * Maximum number of entries for zimbraPrefMailTrustedSenderList.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1139)
    public static final String A_zimbraMailTrustedSenderListMaxNumEntries = "zimbraMailTrustedSenderListMaxNumEntries";

    /**
     * Deprecated since: 6.0.7. Deprecated per bug 43497. The number of
     * uncompressed files on disk will never exceed
     * zimbraMailFileDescriptorCacheSize.. Orig desc: max number of bytes
     * stored in the uncompressed blob cache on disk
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=825)
    public static final String A_zimbraMailUncompressedCacheMaxBytes = "zimbraMailUncompressedCacheMaxBytes";

    /**
     * Deprecated since: 6.0.7. Deprecated per bug 43497. The number of
     * uncompressed files on disk will never exceed
     * zimbraMailFileDescriptorCacheSize.. Orig desc: max number of files in
     * the uncompressed blob cache on disk
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=824)
    public static final String A_zimbraMailUncompressedCacheMaxFiles = "zimbraMailUncompressedCacheMaxFiles";

    /**
     * URL prefix for where the zimbra app resides on this server
     */
    @ZAttr(id=340)
    public static final String A_zimbraMailURL = "zimbraMailURL";

    /**
     * Used to control whether Java NIO direct buffers are used. Value is
     * propagated to Jetty configuration. In the future, other NIO pieces
     * (IMAP/POP/LMTP) will also honor this.
     *
     * @since ZCS 5.0.22
     */
    @ZAttr(id=1002)
    public static final String A_zimbraMailUseDirectBuffers = "zimbraMailUseDirectBuffers";

    /**
     * Maximum number of entries for per user white list. This restricts the
     * number of values that can be set on the amavisWhitelistSender
     * attribute of an account. If set to 0, the per user white list feature
     * is disabled.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=798)
    public static final String A_zimbraMailWhitelistMaxNumEntries = "zimbraMailWhitelistMaxNumEntries";

    /**
     * max number of contacts per page, Web client (not server) verifies that
     * zimbraPrefContactsPerPage should not exceed this attribute.
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1012)
    public static final String A_zimbraMaxContactsPerPage = "zimbraMaxContactsPerPage";

    /**
     * max number of messages/conversations per page, Web client (not server)
     * verifies that zimbraPrefMailItemsPerPage should not exceed this
     * attribute.
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1011)
    public static final String A_zimbraMaxMailItemsPerPage = "zimbraMaxMailItemsPerPage";

    /**
     * max number of voice items per page, Web client (not server) verifies
     * that zimbraPrefVoiceItemsPerPage should not exceed this attribute.
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1013)
    public static final String A_zimbraMaxVoiceItemsPerPage = "zimbraMaxVoiceItemsPerPage";

    /**
     * dynamic group membership
     */
    @ZAttr(id=11)
    public static final String A_zimbraMemberOf = "zimbraMemberOf";

    /**
     * interface address(es) on which memcached server
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=581)
    public static final String A_zimbraMemcachedBindAddress = "zimbraMemcachedBindAddress";

    /**
     * port number on which memcached server should listen
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=580)
    public static final String A_zimbraMemcachedBindPort = "zimbraMemcachedBindPort";

    /**
     * if true, use binary protocol of memcached; if false, use ascii
     * protocol
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1015)
    public static final String A_zimbraMemcachedClientBinaryProtocolEnabled = "zimbraMemcachedClientBinaryProtocolEnabled";

    /**
     * default expiration time in seconds for memcached values; default is 1
     * day
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1017)
    public static final String A_zimbraMemcachedClientExpirySeconds = "zimbraMemcachedClientExpirySeconds";

    /**
     * memcached hash algorithm
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1016)
    public static final String A_zimbraMemcachedClientHashAlgorithm = "zimbraMemcachedClientHashAlgorithm";

    /**
     * list of host:port for memcached servers; set to empty value to disable
     * the use of memcached
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1014)
    public static final String A_zimbraMemcachedClientServerList = "zimbraMemcachedClientServerList";

    /**
     * default timeout in milliseconds for async memcached operations
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1018)
    public static final String A_zimbraMemcachedClientTimeoutMillis = "zimbraMemcachedClientTimeoutMillis";

    /**
     * Maximum number of JavaMail MimeMessage objects in the message cache.
     */
    @ZAttr(id=297)
    public static final String A_zimbraMessageCacheSize = "zimbraMessageCacheSize";

    /**
     * Number of Message-Id header values to keep in the LMTP dedupe cache.
     * Subsequent attempts to deliver a message with a matching Message-Id to
     * the same mailbox will be ignored. A value of 0 disables deduping.
     */
    @ZAttr(id=334)
    public static final String A_zimbraMessageIdDedupeCacheSize = "zimbraMessageIdDedupeCacheSize";

    /**
     * Timeout for a Message-Id entry in the LMTP dedupe cache. A value of 0
     * indicates no timeout. zimbraMessageIdDedupeCacheSize limit is ignored
     * when this is set to a non-zero value. . Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 7.1.4
     */
    @ZAttr(id=1340)
    public static final String A_zimbraMessageIdDedupeCacheTimeout = "zimbraMessageIdDedupeCacheTimeout";

    /**
     * interface address(es) on which milter server should listen; if not
     * specified, binds to 127.0.0.1
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1115)
    public static final String A_zimbraMilterBindAddress = "zimbraMilterBindAddress";

    /**
     * port number on which milter server should listen
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1114)
    public static final String A_zimbraMilterBindPort = "zimbraMilterBindPort";

    /**
     * Maximum number of concurrent MILTER connections allowed. New
     * connections exceeding this limit are rejected.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1157)
    public static final String A_zimbraMilterMaxConnections = "zimbraMilterMaxConnections";

    /**
     * number of milter handler threads
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1144)
    public static final String A_zimbraMilterNumThreads = "zimbraMilterNumThreads";

    /**
     * whether milter server is enabled for a given server
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1116)
    public static final String A_zimbraMilterServerEnabled = "zimbraMilterServerEnabled";

    /**
     * the file extension (without the .)
     */
    @ZAttr(id=160)
    public static final String A_zimbraMimeFileExtension = "zimbraMimeFileExtension";

    /**
     * the handler class for the mime type
     */
    @ZAttr(id=159)
    public static final String A_zimbraMimeHandlerClass = "zimbraMimeHandlerClass";

    /**
     * the name of the zimbra extension where the handler class for the mime
     * type lives
     */
    @ZAttr(id=293)
    public static final String A_zimbraMimeHandlerExtension = "zimbraMimeHandlerExtension";

    /**
     * whether or not indexing is enabled for this type
     */
    @ZAttr(id=158)
    public static final String A_zimbraMimeIndexingEnabled = "zimbraMimeIndexingEnabled";

    /**
     * The priority that this MIME type will be chosen, in the case that more
     * than one MIME type object matches a given type or filename extension.
     */
    @ZAttr(id=503)
    public static final String A_zimbraMimePriority = "zimbraMimePriority";

    /**
     * the MIME type (type/substype) or a regular expression
     */
    @ZAttr(id=157)
    public static final String A_zimbraMimeType = "zimbraMimeType";

    /**
     * whether the Bluetooth capabilities are allowed on the device. The
     * available options are Disable, HandsfreeOnly, and Allow. 0 - DISABLE 1
     * - HANDSFREE 2 - ALLOW ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowBluetooth value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1285)
    public static final String A_zimbraMobilePolicyAllowBluetooth = "zimbraMobilePolicyAllowBluetooth";

    /**
     * whether Microsoft Pocket Internet Explorer is allowed on the mobile
     * phone. This parameter doesn&#039;t affect third-party browsers.
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowBrowser value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1300)
    public static final String A_zimbraMobilePolicyAllowBrowser = "zimbraMobilePolicyAllowBrowser";

    /**
     * whether to allow camera on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowCamera value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1278)
    public static final String A_zimbraMobilePolicyAllowCamera = "zimbraMobilePolicyAllowCamera";

    /**
     * whether the device user can configure a personal e-mail account on the
     * mobile phone. This parameter doesn&#039;t control access to e-mails
     * using third-party mobile phone e-mail programs. ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowConsumerEmail value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1301)
    public static final String A_zimbraMobilePolicyAllowConsumerEmail = "zimbraMobilePolicyAllowConsumerEmail";

    /**
     * whether the device can synchronize with a desktop computer through a
     * cable; ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowDesktopSync value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1288)
    public static final String A_zimbraMobilePolicyAllowDesktopSync = "zimbraMobilePolicyAllowDesktopSync";

    /**
     * whether HTML e-mail is enabled on the device. If set to 0, all e-mail
     * will be converted to plain text before synchronization occurs. ignored
     * if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowHTMLEmail value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1290)
    public static final String A_zimbraMobilePolicyAllowHTMLEmail = "zimbraMobilePolicyAllowHTMLEmail";

    /**
     * whether the mobile device can be used as a modem to connect a computer
     * to the Internet; ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowInternetSharing value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1303)
    public static final String A_zimbraMobilePolicyAllowInternetSharing = "zimbraMobilePolicyAllowInternetSharing";

    /**
     * whether infrared connections are allowed to the device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or zimbraMobilePolicyAllowIrDA
     * value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1286)
    public static final String A_zimbraMobilePolicyAllowIrDA = "zimbraMobilePolicyAllowIrDA";

    /**
     * whether to allow non-provisionable devices; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=834)
    public static final String A_zimbraMobilePolicyAllowNonProvisionableDevices = "zimbraMobilePolicyAllowNonProvisionableDevices";

    /**
     * whether to allow partial policy enforcement on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=835)
    public static final String A_zimbraMobilePolicyAllowPartialProvisioning = "zimbraMobilePolicyAllowPartialProvisioning";

    /**
     * whether the user can configure a POP3 or IMAP4 e-mail account on the
     * device. This parameter doesn&#039;t control access by third-party
     * e-mail programs. ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowPOPIMAPEmail value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1284)
    public static final String A_zimbraMobilePolicyAllowPOPIMAPEmail = "zimbraMobilePolicyAllowPOPIMAPEmail";

    /**
     * whether the mobile device can initiate a remote desktop connection;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowRemoteDesktop value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1302)
    public static final String A_zimbraMobilePolicyAllowRemoteDesktop = "zimbraMobilePolicyAllowRemoteDesktop";

    /**
     * whether to allow simple password; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=839)
    public static final String A_zimbraMobilePolicyAllowSimpleDevicePassword = "zimbraMobilePolicyAllowSimpleDevicePassword";

    /**
     * whether the messaging application on the device can negotiate the
     * encryption algorithm if a recipient&#039;s certificate doesn&#039;t
     * support the specified encryption algorithm; 0 - BlockNegotiation 1 -
     * OnlyStrongAlgorithmNegotiation 2 - AllowAnyAlgorithmNegotiation
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowSMIMEEncryptionAlgorithmNegotiation value is
     * set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1298)
    public static final String A_zimbraMobilePolicyAllowSMIMEEncryptionAlgorithmNegotiation = "zimbraMobilePolicyAllowSMIMEEncryptionAlgorithmNegotiation";

    /**
     * whether S/MIME software certificates are allowed; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowSMIMESoftCerts value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1299)
    public static final String A_zimbraMobilePolicyAllowSMIMESoftCerts = "zimbraMobilePolicyAllowSMIMESoftCerts";

    /**
     * whether to allow removable storage on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowStorageCard value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1277)
    public static final String A_zimbraMobilePolicyAllowStorageCard = "zimbraMobilePolicyAllowStorageCard";

    /**
     * whether text messaging is allowed from the device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowTextMessaging value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1283)
    public static final String A_zimbraMobilePolicyAllowTextMessaging = "zimbraMobilePolicyAllowTextMessaging";

    /**
     * whether unsigned applications are allowed on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowUnsignedApplications value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1280)
    public static final String A_zimbraMobilePolicyAllowUnsignedApplications = "zimbraMobilePolicyAllowUnsignedApplications";

    /**
     * whether unsigned installation packages are allowed on device; ignored
     * if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyAllowUnsignedInstallationPackages value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1281)
    public static final String A_zimbraMobilePolicyAllowUnsignedInstallationPackages = "zimbraMobilePolicyAllowUnsignedInstallationPackages";

    /**
     * whether wireless Internet access is allowed on the device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or zimbraMobilePolicyAllowWiFi
     * value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1282)
    public static final String A_zimbraMobilePolicyAllowWiFi = "zimbraMobilePolicyAllowWiFi";

    /**
     * whether to require alpha-numeric password as device pin; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=840)
    public static final String A_zimbraMobilePolicyAlphanumericDevicePasswordRequired = "zimbraMobilePolicyAlphanumericDevicePasswordRequired";

    /**
     * approved application for the mobile device the value contains a SHA1
     * hash (typically 40 characters long) for the application file (.exe,
     * .dll etc) ignored if zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1305)
    public static final String A_zimbraMobilePolicyApprovedApplication = "zimbraMobilePolicyApprovedApplication";

    /**
     * require data encryption on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=847)
    public static final String A_zimbraMobilePolicyDeviceEncryptionEnabled = "zimbraMobilePolicyDeviceEncryptionEnabled";

    /**
     * whether to force pin on device; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=837)
    public static final String A_zimbraMobilePolicyDevicePasswordEnabled = "zimbraMobilePolicyDevicePasswordEnabled";

    /**
     * number of days before device pin must expire; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=842)
    public static final String A_zimbraMobilePolicyDevicePasswordExpiration = "zimbraMobilePolicyDevicePasswordExpiration";

    /**
     * number of previously used password stored in history; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordExpiration=0
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=843)
    public static final String A_zimbraMobilePolicyDevicePasswordHistory = "zimbraMobilePolicyDevicePasswordHistory";

    /**
     * the maximum range of calendar days that can be synchronized to the
     * device; 0 - PAST ALL 4 - Two Weeks 5 - One Month 6 - Three Months 7 -
     * Six Months ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyMaxCalendarAgeFilter value is set to -1, 1, 2 or, 3
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1289)
    public static final String A_zimbraMobilePolicyMaxCalendarAgeFilter = "zimbraMobilePolicyMaxCalendarAgeFilter";

    /**
     * number of consecutive incorrect pin input before device is wiped;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=845)
    public static final String A_zimbraMobilePolicyMaxDevicePasswordFailedAttempts = "zimbraMobilePolicyMaxDevicePasswordFailedAttempts";

    /**
     * the maximum number of days of e-mail items to synchronize to the
     * device; 0 - PAST ALL 1 - One Day 2 - Three Days 3 - One Week 4 - Two
     * Weeks 5 - One Month ignored if zimbraFeatureMobilePolicyEnabled=FALSE
     * or zimbraMobilePolicyMaxEmailAgeFilter value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1291)
    public static final String A_zimbraMobilePolicyMaxEmailAgeFilter = "zimbraMobilePolicyMaxEmailAgeFilter";

    /**
     * the maximum size at which e-mail messages are truncated when
     * synchronized to the device; The value is specified in kilobytes (KB).
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1292)
    public static final String A_zimbraMobilePolicyMaxEmailBodyTruncationSize = "zimbraMobilePolicyMaxEmailBodyTruncationSize";

    /**
     * the maximum size at which HTML-formatted e-mail messages are
     * synchronized to the devices. The value is specified in KB. ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1293)
    public static final String A_zimbraMobilePolicyMaxEmailHTMLBodyTruncationSize = "zimbraMobilePolicyMaxEmailHTMLBodyTruncationSize";

    /**
     * max idle time in minutes before device is locked; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=844)
    public static final String A_zimbraMobilePolicyMaxInactivityTimeDeviceLock = "zimbraMobilePolicyMaxInactivityTimeDeviceLock";

    /**
     * least number of complex characters must be included in device pin;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=841)
    public static final String A_zimbraMobilePolicyMinDevicePasswordComplexCharacters = "zimbraMobilePolicyMinDevicePasswordComplexCharacters";

    /**
     * min length for device pin; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=838)
    public static final String A_zimbraMobilePolicyMinDevicePasswordLength = "zimbraMobilePolicyMinDevicePasswordLength";

    /**
     * support device pin recovery; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=846)
    public static final String A_zimbraMobilePolicyPasswordRecoveryEnabled = "zimbraMobilePolicyPasswordRecoveryEnabled";

    /**
     * time interval in minutes before forcing device to refresh policy;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=836)
    public static final String A_zimbraMobilePolicyRefreshInterval = "zimbraMobilePolicyRefreshInterval";

    /**
     * whether encryption on device is required; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireDeviceEncryption value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1279)
    public static final String A_zimbraMobilePolicyRequireDeviceEncryption = "zimbraMobilePolicyRequireDeviceEncryption";

    /**
     * whether you must encrypt S/MIME messages; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireEncryptedSMIMEMessages value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1295)
    public static final String A_zimbraMobilePolicyRequireEncryptedSMIMEMessages = "zimbraMobilePolicyRequireEncryptedSMIMEMessages";

    /**
     * what required algorithm must be used when encrypting a message;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireEncryptionSMIMEAlgorithm value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1297)
    public static final String A_zimbraMobilePolicyRequireEncryptionSMIMEAlgorithm = "zimbraMobilePolicyRequireEncryptionSMIMEAlgorithm";

    /**
     * whether the mobile device must synchronize manually while roaming;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireManualSyncWhenRoaming value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1287)
    public static final String A_zimbraMobilePolicyRequireManualSyncWhenRoaming = "zimbraMobilePolicyRequireManualSyncWhenRoaming";

    /**
     * what required algorithm must be used when signing a message; ignored
     * if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireSignedSMIMEAlgorithm value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1296)
    public static final String A_zimbraMobilePolicyRequireSignedSMIMEAlgorithm = "zimbraMobilePolicyRequireSignedSMIMEAlgorithm";

    /**
     * whether the device must send signed S/MIME messages; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobilePolicyRequireSignedSMIMEMessages value is set to -1
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1294)
    public static final String A_zimbraMobilePolicyRequireSignedSMIMEMessages = "zimbraMobilePolicyRequireSignedSMIMEMessages";

    /**
     * when set to TRUE, suppresses DeviceEncryptionEnabled to be sent down
     * to the device; Some devices choke when DeviceEncryptionEnabled policy
     * is downloaded irrespective of their value set to 0 or, 1 ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1306)
    public static final String A_zimbraMobilePolicySuppressDeviceEncryption = "zimbraMobilePolicySuppressDeviceEncryption";

    /**
     * application that can&#039;t be run in device ROM; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1304)
    public static final String A_zimbraMobilePolicyUnapprovedInROMApplication = "zimbraMobilePolicyUnapprovedInROMApplication";

    /**
     * indicates whether the application can forward original email as RFC
     * 822 .eml attachment. Note: this setting is applicable only to the
     * devices using activesync smart forward for forwarding email messages.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1205)
    public static final String A_zimbraMobileSmartForwardRFC822Enabled = "zimbraMobileSmartForwardRFC822Enabled";

    /**
     * mta anti spam lock method.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=612)
    public static final String A_zimbraMtaAntiSpamLockMethod = "zimbraMtaAntiSpamLockMethod";

    /**
     * Deprecated since: 6.0.0_BETA1. deprecated in favor of
     * zimbraMtaTlsSecurityLevel and zimbraMtaSaslAuthEnable. Orig desc:
     * Value for postconf smtpd_tls_security_level
     */
    @ZAttr(id=194)
    public static final String A_zimbraMtaAuthEnabled = "zimbraMtaAuthEnabled";

    /**
     * Host running SOAP service for use by MTA auth. Setting this sets
     * zimbraMtaAuthURL via attr callback mechanism.
     */
    @ZAttr(id=309)
    public static final String A_zimbraMtaAuthHost = "zimbraMtaAuthHost";

    /**
     * whether this server is a mta auth target
     */
    @ZAttr(id=505)
    public static final String A_zimbraMtaAuthTarget = "zimbraMtaAuthTarget";

    /**
     * URL at which this MTA (via zimbra saslauthd) should authenticate. Set
     * by setting zimbraMtaAuthHost.
     */
    @ZAttr(id=310)
    public static final String A_zimbraMtaAuthURL = "zimbraMtaAuthURL";

    /**
     * Attachment file extensions that are blocked
     */
    @ZAttr(id=195)
    public static final String A_zimbraMtaBlockedExtension = "zimbraMtaBlockedExtension";

    /**
     * Whether to email admin on detection of attachment with blocked
     * extension
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1031)
    public static final String A_zimbraMtaBlockedExtensionWarnAdmin = "zimbraMtaBlockedExtensionWarnAdmin";

    /**
     * Whether to email recipient on detection of attachment with blocked
     * extension
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1032)
    public static final String A_zimbraMtaBlockedExtensionWarnRecipient = "zimbraMtaBlockedExtensionWarnRecipient";

    /**
     * Commonly blocked attachment file extensions
     */
    @ZAttr(id=196)
    public static final String A_zimbraMtaCommonBlockedExtension = "zimbraMtaCommonBlockedExtension";

    /**
     * Value for postconf disable_dns_lookups (note enable v. disable)
     */
    @ZAttr(id=197)
    public static final String A_zimbraMtaDnsLookupsEnabled = "zimbraMtaDnsLookupsEnabled";

    /**
     * Value for postconf message_size_limit
     */
    @ZAttr(id=198)
    public static final String A_zimbraMtaMaxMessageSize = "zimbraMtaMaxMessageSize";

    /**
     * value of postfix mydestination
     */
    @ZAttr(id=524)
    public static final String A_zimbraMtaMyDestination = "zimbraMtaMyDestination";

    /**
     * value of postfix myhostname
     */
    @ZAttr(id=509)
    public static final String A_zimbraMtaMyHostname = "zimbraMtaMyHostname";

    /**
     * value of postfix mynetworks
     */
    @ZAttr(id=311)
    public static final String A_zimbraMtaMyNetworks = "zimbraMtaMyNetworks";

    /**
     * value of postfix myorigin
     */
    @ZAttr(id=510)
    public static final String A_zimbraMtaMyOrigin = "zimbraMtaMyOrigin";

    /**
     * value for postfix non_smtpd_milters
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=673)
    public static final String A_zimbraMtaNonSmtpdMilters = "zimbraMtaNonSmtpdMilters";

    /**
     * Value for postconf recipient_delimiter. Also used by ZCS LMTP server
     * to check if it should accept messages to addresses with extensions.
     */
    @ZAttr(id=306)
    public static final String A_zimbraMtaRecipientDelimiter = "zimbraMtaRecipientDelimiter";

    /**
     * Value for postconf relayhost. Note: there can be only one value on
     * this attribute, see bug 50697.
     */
    @ZAttr(id=199)
    public static final String A_zimbraMtaRelayHost = "zimbraMtaRelayHost";

    /**
     * restrictions to reject some suspect SMTP clients
     */
    @ZAttr(id=226)
    public static final String A_zimbraMtaRestriction = "zimbraMtaRestriction";

    /**
     * Value for postconf smtpd_sasl_auth_enable
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=796)
    public static final String A_zimbraMtaSaslAuthEnable = "zimbraMtaSaslAuthEnable";

    /**
     * value for postfix smtpd_milters
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=672)
    public static final String A_zimbraMtaSmtpdMilters = "zimbraMtaSmtpdMilters";

    /**
     * Value for postconf smtpd_tls_auth_only
     */
    @ZAttr(id=200)
    public static final String A_zimbraMtaTlsAuthOnly = "zimbraMtaTlsAuthOnly";

    /**
     * Value for postconf smtpd_tls_security_level
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=795)
    public static final String A_zimbraMtaTlsSecurityLevel = "zimbraMtaTlsSecurityLevel";

    /**
     * certificate to be used for validating the SAML assertions received
     * from myonelogin (tricipher)
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1169)
    public static final String A_zimbraMyoneloginSamlSigningCert = "zimbraMyoneloginSamlSigningCert";

    /**
     * A signed activation key that authorizes this installation.
     */
    @ZAttr(id=375)
    public static final String A_zimbraNetworkActivation = "zimbraNetworkActivation";

    /**
     * Contents of a signed Zimbra license key - an XML string.
     */
    @ZAttr(id=374)
    public static final String A_zimbraNetworkLicense = "zimbraNetworkLicense";

    /**
     * template used to construct the body of an email notification message
     */
    @ZAttr(id=152)
    public static final String A_zimbraNewMailNotificationBody = "zimbraNewMailNotificationBody";

    /**
     * template used to construct the sender of an email notification message
     */
    @ZAttr(id=150)
    public static final String A_zimbraNewMailNotificationFrom = "zimbraNewMailNotificationFrom";

    /**
     * template used to construct the subject of an email notification
     * message
     */
    @ZAttr(id=151)
    public static final String A_zimbraNewMailNotificationSubject = "zimbraNewMailNotificationSubject";

    /**
     * Deprecated since: 7.0.0. See bug 39647. Orig desc: Account for storing
     * templates and providing space for public wiki
     */
    @ZAttr(id=363)
    public static final String A_zimbraNotebookAccount = "zimbraNotebookAccount";

    /**
     * Deprecated since: 6.0.0_BETA1. deprecated. Orig desc: The size of Wiki
     * / Notebook folder cache on the server.
     */
    @ZAttr(id=370)
    public static final String A_zimbraNotebookFolderCacheSize = "zimbraNotebookFolderCacheSize";

    /**
     * Deprecated since: 6.0.0_BETA1. deprecated. Orig desc: The maximum
     * number of cached templates in each Wiki / Notebook folder cache.
     */
    @ZAttr(id=371)
    public static final String A_zimbraNotebookMaxCachedTemplatesPerFolder = "zimbraNotebookMaxCachedTemplatesPerFolder";

    /**
     * maximum number of revisions to keep for wiki pages and documents. 0
     * means unlimited.
     */
    @ZAttr(id=482)
    public static final String A_zimbraNotebookMaxRevisions = "zimbraNotebookMaxRevisions";

    /**
     * The size of composed Wiki / Notebook page cache on the server.
     */
    @ZAttr(id=369)
    public static final String A_zimbraNotebookPageCacheSize = "zimbraNotebookPageCacheSize";

    /**
     * whether to strip off potentially harming HTML tags in Wiki and HTML
     * Documents.
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=646)
    public static final String A_zimbraNotebookSanitizeHtml = "zimbraNotebookSanitizeHtml";

    /**
     * administrative notes
     */
    @ZAttr(id=9)
    public static final String A_zimbraNotes = "zimbraNotes";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which notification server
     * should listen; if empty, binds to all interfaces.
     */
    @ZAttr(id=317)
    public static final String A_zimbraNotifyBindAddress = "zimbraNotifyBindAddress";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     */
    @ZAttr(id=318)
    public static final String A_zimbraNotifyBindPort = "zimbraNotifyBindPort";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether notification server should be enabled.
     */
    @ZAttr(id=316)
    public static final String A_zimbraNotifyServerEnabled = "zimbraNotifyServerEnabled";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Network interface on which SSL notification server
     * should listen; if empty, binds to all interfaces
     */
    @ZAttr(id=320)
    public static final String A_zimbraNotifySSLBindAddress = "zimbraNotifySSLBindAddress";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Port number on which notification server should
     * listen.
     */
    @ZAttr(id=321)
    public static final String A_zimbraNotifySSLBindPort = "zimbraNotifySSLBindPort";

    /**
     * Deprecated since: 4.0. was experimental and never part of any shipping
     * feature. Orig desc: Whether SSL notification server should be enabled.
     */
    @ZAttr(id=319)
    public static final String A_zimbraNotifySSLServerEnabled = "zimbraNotifySSLServerEnabled";

    /**
     * OAuth consumer ids and secrets. It is in the format of
     * {consumer-id]:{secrets}
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1131)
    public static final String A_zimbraOAuthConsumerCredentials = "zimbraOAuthConsumerCredentials";

    /**
     * the handler class for the object type
     */
    @ZAttr(id=164)
    public static final String A_zimbraObjectHandlerClass = "zimbraObjectHandlerClass";

    /**
     * config for this type
     */
    @ZAttr(id=165)
    public static final String A_zimbraObjectHandlerConfig = "zimbraObjectHandlerConfig";

    /**
     * whether or not indexing is enabled for this type
     */
    @ZAttr(id=162)
    public static final String A_zimbraObjectIndexingEnabled = "zimbraObjectIndexingEnabled";

    /**
     * whether or not store is matched for this type
     */
    @ZAttr(id=163)
    public static final String A_zimbraObjectStoreMatched = "zimbraObjectStoreMatched";

    /**
     * the object type
     */
    @ZAttr(id=161)
    public static final String A_zimbraObjectType = "zimbraObjectType";

    /**
     * allowed OpenID Provider Endpoint URLs for authentication
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1191)
    public static final String A_zimbraOpenidConsumerAllowedOPEndpointURL = "zimbraOpenidConsumerAllowedOPEndpointURL";

    /**
     * whether stateless mode (not establishing an association with the
     * OpenID Provider) in OpenID Consumer is enabled
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1189)
    public static final String A_zimbraOpenidConsumerStatelessModeEnabled = "zimbraOpenidConsumerStatelessModeEnabled";

    /**
     * regex of allowed characters in password
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1163)
    public static final String A_zimbraPasswordAllowedChars = "zimbraPasswordAllowedChars";

    /**
     * regex of allowed punctuation characters in password
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1256)
    public static final String A_zimbraPasswordAllowedPunctuationChars = "zimbraPasswordAllowedPunctuationChars";

    /**
     * registered change password listener name
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=586)
    public static final String A_zimbraPasswordChangeListener = "zimbraPasswordChangeListener";

    /**
     * whether or not to enforce password history. Number of unique passwords
     * a user must have before being allowed to re-use an old one. A value of
     * 0 means no password history.
     */
    @ZAttr(id=37)
    public static final String A_zimbraPasswordEnforceHistory = "zimbraPasswordEnforceHistory";

    /**
     * historical password values
     */
    @ZAttr(id=38)
    public static final String A_zimbraPasswordHistory = "zimbraPasswordHistory";

    /**
     * user is unable to change password
     */
    @ZAttr(id=45)
    public static final String A_zimbraPasswordLocked = "zimbraPasswordLocked";

    /**
     * how long an account is locked out. Use 0 to lockout an account until
     * admin resets it. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     */
    @ZAttr(id=379)
    public static final String A_zimbraPasswordLockoutDuration = "zimbraPasswordLockoutDuration";

    /**
     * whether or not account lockout is enabled.
     */
    @ZAttr(id=378)
    public static final String A_zimbraPasswordLockoutEnabled = "zimbraPasswordLockoutEnabled";

    /**
     * the duration after which old consecutive failed login attempts are
     * purged from the list, even though no successful authentication has
     * occurred. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     */
    @ZAttr(id=381)
    public static final String A_zimbraPasswordLockoutFailureLifetime = "zimbraPasswordLockoutFailureLifetime";

    /**
     * this attribute contains the timestamps of each of the consecutive
     * authentication failures made on an account
     */
    @ZAttr(id=383)
    public static final String A_zimbraPasswordLockoutFailureTime = "zimbraPasswordLockoutFailureTime";

    /**
     * the time at which an account was locked
     */
    @ZAttr(id=382)
    public static final String A_zimbraPasswordLockoutLockedTime = "zimbraPasswordLockoutLockedTime";

    /**
     * number of consecutive failed login attempts until an account is locked
     * out
     */
    @ZAttr(id=380)
    public static final String A_zimbraPasswordLockoutMaxFailures = "zimbraPasswordLockoutMaxFailures";

    /**
     * maximum days between password changes
     */
    @ZAttr(id=36)
    public static final String A_zimbraPasswordMaxAge = "zimbraPasswordMaxAge";

    /**
     * max length of a password
     */
    @ZAttr(id=34)
    public static final String A_zimbraPasswordMaxLength = "zimbraPasswordMaxLength";

    /**
     * minimum days between password changes
     */
    @ZAttr(id=35)
    public static final String A_zimbraPasswordMinAge = "zimbraPasswordMinAge";

    /**
     * minimum number of alphabet characters required in a password
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1162)
    public static final String A_zimbraPasswordMinAlphaChars = "zimbraPasswordMinAlphaChars";

    /**
     * minimum number of numeric or ascii punctuation characters required in
     * a password
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1255)
    public static final String A_zimbraPasswordMinDigitsOrPuncs = "zimbraPasswordMinDigitsOrPuncs";

    /**
     * minimum length of a password
     */
    @ZAttr(id=33)
    public static final String A_zimbraPasswordMinLength = "zimbraPasswordMinLength";

    /**
     * minimum number of lower case characters required in a password
     */
    @ZAttr(id=390)
    public static final String A_zimbraPasswordMinLowerCaseChars = "zimbraPasswordMinLowerCaseChars";

    /**
     * minimum number of numeric characters required in a password
     */
    @ZAttr(id=392)
    public static final String A_zimbraPasswordMinNumericChars = "zimbraPasswordMinNumericChars";

    /**
     * minimum number of ascii punctuation characters required in a password
     */
    @ZAttr(id=391)
    public static final String A_zimbraPasswordMinPunctuationChars = "zimbraPasswordMinPunctuationChars";

    /**
     * minimum number of upper case characters required in a password
     */
    @ZAttr(id=389)
    public static final String A_zimbraPasswordMinUpperCaseChars = "zimbraPasswordMinUpperCaseChars";

    /**
     * time password was last changed
     */
    @ZAttr(id=39)
    public static final String A_zimbraPasswordModifiedTime = "zimbraPasswordModifiedTime";

    /**
     * must change password on auth
     */
    @ZAttr(id=41)
    public static final String A_zimbraPasswordMustChange = "zimbraPasswordMustChange";

    /**
     * phonetic company name
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1149)
    public static final String A_zimbraPhoneticCompany = "zimbraPhoneticCompany";

    /**
     * phonetic first name
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1147)
    public static final String A_zimbraPhoneticFirstName = "zimbraPhoneticFirstName";

    /**
     * phonetic last name
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1148)
    public static final String A_zimbraPhoneticLastName = "zimbraPhoneticLastName";

    /**
     * name to use in greeting and sign-off; if empty, uses hostname
     */
    @ZAttr(id=93)
    public static final String A_zimbraPop3AdvertisedName = "zimbraPop3AdvertisedName";

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     */
    @ZAttr(id=95)
    public static final String A_zimbraPop3BindAddress = "zimbraPop3BindAddress";

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     */
    @ZAttr(id=271)
    public static final String A_zimbraPop3BindOnStartup = "zimbraPop3BindOnStartup";

    /**
     * port number on which POP3 server should listen
     */
    @ZAttr(id=94)
    public static final String A_zimbraPop3BindPort = "zimbraPop3BindPort";

    /**
     * whether or not to allow cleartext logins over a non SSL/TLS connection
     */
    @ZAttr(id=189)
    public static final String A_zimbraPop3CleartextLoginEnabled = "zimbraPop3CleartextLoginEnabled";

    /**
     * whether POP3 is enabled for an account
     */
    @ZAttr(id=175)
    public static final String A_zimbraPop3Enabled = "zimbraPop3Enabled";

    /**
     * Whether to expose version on POP3 banner
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=692)
    public static final String A_zimbraPop3ExposeVersionOnBanner = "zimbraPop3ExposeVersionOnBanner";

    /**
     * Maximum number of concurrent POP3 connections allowed. New connections
     * exceeding this limit are rejected.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1155)
    public static final String A_zimbraPop3MaxConnections = "zimbraPop3MaxConnections";

    /**
     * number of handler threads
     */
    @ZAttr(id=96)
    public static final String A_zimbraPop3NumThreads = "zimbraPop3NumThreads";

    /**
     * port number on which POP3 proxy server should listen
     */
    @ZAttr(id=350)
    public static final String A_zimbraPop3ProxyBindPort = "zimbraPop3ProxyBindPort";

    /**
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=554)
    public static final String A_zimbraPop3SaslGssapiEnabled = "zimbraPop3SaslGssapiEnabled";

    /**
     * whether POP3 is enabled for a server
     */
    @ZAttr(id=177)
    public static final String A_zimbraPop3ServerEnabled = "zimbraPop3ServerEnabled";

    /**
     * number of seconds to wait before forcing POP3 server shutdown
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1081)
    public static final String A_zimbraPop3ShutdownGraceSeconds = "zimbraPop3ShutdownGraceSeconds";

    /**
     * interface address(es) on which POP3 server should listen; if empty,
     * binds to all interfaces
     */
    @ZAttr(id=186)
    public static final String A_zimbraPop3SSLBindAddress = "zimbraPop3SSLBindAddress";

    /**
     * Whether to bind to port on startup irrespective of whether the server
     * is enabled. Useful when port to bind is privileged and must be bound
     * early.
     */
    @ZAttr(id=272)
    public static final String A_zimbraPop3SSLBindOnStartup = "zimbraPop3SSLBindOnStartup";

    /**
     * port number on which POP3 server should listen
     */
    @ZAttr(id=187)
    public static final String A_zimbraPop3SSLBindPort = "zimbraPop3SSLBindPort";

    /**
     * port number on which POP3S proxy server should listen
     */
    @ZAttr(id=351)
    public static final String A_zimbraPop3SSLProxyBindPort = "zimbraPop3SSLProxyBindPort";

    /**
     * whether POP3 SSL server is enabled for a server
     */
    @ZAttr(id=188)
    public static final String A_zimbraPop3SSLServerEnabled = "zimbraPop3SSLServerEnabled";

    /**
     * portal name
     */
    @ZAttr(id=448)
    public static final String A_zimbraPortalName = "zimbraPortalName";

    /**
     * preauth secret key
     */
    @ZAttr(id=307)
    public static final String A_zimbraPreAuthKey = "zimbraPreAuthKey";

    /**
     * whether or not account tree is expanded
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1048)
    public static final String A_zimbraPrefAccountTreeOpen = "zimbraPrefAccountTreeOpen";

    /**
     * whether to display a warning when users try to navigate away from the
     * admin console
     *
     * @since ZCS 6.0.0_RC1
     */
    @ZAttr(id=1036)
    public static final String A_zimbraPrefAdminConsoleWarnOnExit = "zimbraPrefAdminConsoleWarnOnExit";

    /**
     * After login, whether the advanced client should enforce minimum
     * display resolution
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=678)
    public static final String A_zimbraPrefAdvancedClientEnforceMinDisplay = "zimbraPrefAdvancedClientEnforceMinDisplay";

    /**
     * Addresses of the account that can be used by allowed delegated senders
     * as From and Sender address.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1333)
    public static final String A_zimbraPrefAllowAddressForDelegatedSender = "zimbraPrefAllowAddressForDelegatedSender";

    /**
     * Use the iCal style delegation model for shared calendars for CalDAV
     * interface when set to TRUE.
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1028)
    public static final String A_zimbraPrefAppleIcalDelegationEnabled = "zimbraPrefAppleIcalDelegationEnabled";

    /**
     * whether or not new address in outgoing email are auto added to address
     * book
     */
    @ZAttr(id=131)
    public static final String A_zimbraPrefAutoAddAddressEnabled = "zimbraPrefAutoAddAddressEnabled";

    /**
     * whether actionable address objects result from autocomplete is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1146)
    public static final String A_zimbraPrefAutocompleteAddressBubblesEnabled = "zimbraPrefAutocompleteAddressBubblesEnabled";

    /**
     * whether to end auto-complete on comma
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1091)
    public static final String A_zimbraPrefAutoCompleteQuickCompletionOnComma = "zimbraPrefAutoCompleteQuickCompletionOnComma";

    /**
     * time to wait before auto saving a draft. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=561)
    public static final String A_zimbraPrefAutoSaveDraftInterval = "zimbraPrefAutoSaveDraftInterval";

    /**
     * address that we will bcc when using sending mail with this identity
     * (deprecatedSince 5.0 in identity)
     */
    @ZAttr(id=411)
    public static final String A_zimbraPrefBccAddress = "zimbraPrefBccAddress";

    /**
     * where the reading pane is displayed for briefcase
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1152)
    public static final String A_zimbraPrefBriefcaseReadingPaneLocation = "zimbraPrefBriefcaseReadingPaneLocation";

    /**
     * calendar manual accept reply signature for account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1202)
    public static final String A_zimbraPrefCalendarAcceptSignatureId = "zimbraPrefCalendarAcceptSignatureId";

    /**
     * whether to allow a cancel email sent to organizer of appointment
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=702)
    public static final String A_zimbraPrefCalendarAllowCancelEmailToSelf = "zimbraPrefCalendarAllowCancelEmailToSelf";

    /**
     * whether calendar invite part in a forwarded email is auto-added to
     * calendar
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=686)
    public static final String A_zimbraPrefCalendarAllowForwardedInvite = "zimbraPrefCalendarAllowForwardedInvite";

    /**
     * whether calendar invite part with PUBLISH method is auto-added to
     * calendar
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=688)
    public static final String A_zimbraPrefCalendarAllowPublishMethodInvite = "zimbraPrefCalendarAllowPublishMethodInvite";

    /**
     * always show the mini calendar
     */
    @ZAttr(id=276)
    public static final String A_zimbraPrefCalendarAlwaysShowMiniCal = "zimbraPrefCalendarAlwaysShowMiniCal";

    /**
     * Whether to allow attendees to make local edits to appointments. The
     * change is only on the attendees copy of the message and changes from
     * the organizer will overwrite the local changes.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1089)
    public static final String A_zimbraPrefCalendarApptAllowAtendeeEdit = "zimbraPrefCalendarApptAllowAtendeeEdit";

    /**
     * number of minutes (0 = never) before appt to show reminder dialog
     */
    @ZAttr(id=341)
    public static final String A_zimbraPrefCalendarApptReminderWarningTime = "zimbraPrefCalendarApptReminderWarningTime";

    /**
     * default visibility of the appointment when starting a new appointment
     * in the UI
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=832)
    public static final String A_zimbraPrefCalendarApptVisibility = "zimbraPrefCalendarApptVisibility";

    /**
     * calendar auto accept reply signature for account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1192)
    public static final String A_zimbraPrefCalendarAutoAcceptSignatureId = "zimbraPrefCalendarAutoAcceptSignatureId";

    /**
     * automatically add appointments when invited
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=848)
    public static final String A_zimbraPrefCalendarAutoAddInvites = "zimbraPrefCalendarAutoAddInvites";

    /**
     * calendar auto decline reply signature id for
     * account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1193)
    public static final String A_zimbraPrefCalendarAutoDeclineSignatureId = "zimbraPrefCalendarAutoDeclineSignatureId";

    /**
     * calendar auto deny reply signature id for account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1194)
    public static final String A_zimbraPrefCalendarAutoDenySignatureId = "zimbraPrefCalendarAutoDenySignatureId";

    /**
     * hour of day that the day view should end at, non-inclusive (16=4pm, 24
     * = midnight, etc)
     */
    @ZAttr(id=440)
    public static final String A_zimbraPrefCalendarDayHourEnd = "zimbraPrefCalendarDayHourEnd";

    /**
     * hour of day that the day view should start at (1=1 AM, 8=8 AM, etc)
     */
    @ZAttr(id=439)
    public static final String A_zimbraPrefCalendarDayHourStart = "zimbraPrefCalendarDayHourStart";

    /**
     * calendar manual decline reply signature id for
     * account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1204)
    public static final String A_zimbraPrefCalendarDeclineSignatureId = "zimbraPrefCalendarDeclineSignatureId";

    /**
     * default appointment duration. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1187)
    public static final String A_zimbraPrefCalendarDefaultApptDuration = "zimbraPrefCalendarDefaultApptDuration";

    /**
     * first day of week to show in calendar (0=sunday, 6=saturday)
     */
    @ZAttr(id=261)
    public static final String A_zimbraPrefCalendarFirstDayOfWeek = "zimbraPrefCalendarFirstDayOfWeek";

    /**
     * Forward a copy of calendar invites received to these users.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=851)
    public static final String A_zimbraPrefCalendarForwardInvitesTo = "zimbraPrefCalendarForwardInvitesTo";

    /**
     * comma-sep list of calendars that are initially checked
     */
    @ZAttr(id=275)
    public static final String A_zimbraPrefCalendarInitialCheckedCalendars = "zimbraPrefCalendarInitialCheckedCalendars";

    /**
     * initial calendar view to use
     */
    @ZAttr(id=240)
    public static final String A_zimbraPrefCalendarInitialView = "zimbraPrefCalendarInitialView";

    /**
     * If set to true, user is notified by email of changes made to her
     * calendar by others via delegated calendar access.
     */
    @ZAttr(id=273)
    public static final String A_zimbraPrefCalendarNotifyDelegatedChanges = "zimbraPrefCalendarNotifyDelegatedChanges";

    /**
     * device information entered by the user for receiving reminders for
     * appointments and tasks
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1307)
    public static final String A_zimbraPrefCalendarReminderDeviceInfo = "zimbraPrefCalendarReminderDeviceInfo";

    /**
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: When to send the first reminder for an event.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=573)
    public static final String A_zimbraPrefCalendarReminderDuration1 = "zimbraPrefCalendarReminderDuration1";

    /**
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: When to send the second reminder for an event.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=574)
    public static final String A_zimbraPrefCalendarReminderDuration2 = "zimbraPrefCalendarReminderDuration2";

    /**
     * RFC822 email address for receiving reminders for appointments and
     * tasks
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=575)
    public static final String A_zimbraPrefCalendarReminderEmail = "zimbraPrefCalendarReminderEmail";

    /**
     * Flash title when on appointment reminder notification
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=682)
    public static final String A_zimbraPrefCalendarReminderFlashTitle = "zimbraPrefCalendarReminderFlashTitle";

    /**
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: The mobile device (phone) the reminder goes to.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=577)
    public static final String A_zimbraPrefCalendarReminderMobile = "zimbraPrefCalendarReminderMobile";

    /**
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: whether or not email reminders for appointments and
     * tasks are enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=576)
    public static final String A_zimbraPrefCalendarReminderSendEmail = "zimbraPrefCalendarReminderSendEmail";

    /**
     * whether audible alert is enabled when appointment notification is
     * played
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=667)
    public static final String A_zimbraPrefCalendarReminderSoundsEnabled = "zimbraPrefCalendarReminderSoundsEnabled";

    /**
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: Send a reminder via YIM
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=578)
    public static final String A_zimbraPrefCalendarReminderYMessenger = "zimbraPrefCalendarReminderYMessenger";

    /**
     * if an invite is received from an organizer who does not have
     * permission to invite this user to a meeting, send an auto-decline
     * reply
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=849)
    public static final String A_zimbraPrefCalendarSendInviteDeniedAutoReply = "zimbraPrefCalendarSendInviteDeniedAutoReply";

    /**
     * whether to show declined meetings in calendar
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1196)
    public static final String A_zimbraPrefCalendarShowDeclinedMeetings = "zimbraPrefCalendarShowDeclinedMeetings";

    /**
     * whether to pop-up reminder for past due appointments in the UI
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1022)
    public static final String A_zimbraPrefCalendarShowPastDueReminders = "zimbraPrefCalendarShowPastDueReminders";

    /**
     * calendar manual tentative accept reply signature id for
     * account/identity/dataSource
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1203)
    public static final String A_zimbraPrefCalendarTentativeSignatureId = "zimbraPrefCalendarTentativeSignatureId";

    /**
     * whether to enable toaster notification for new mail
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=813)
    public static final String A_zimbraPrefCalendarToasterEnabled = "zimbraPrefCalendarToasterEnabled";

    /**
     * whether or not use quick add dialog or go into full appt edit view
     */
    @ZAttr(id=274)
    public static final String A_zimbraPrefCalendarUseQuickAdd = "zimbraPrefCalendarUseQuickAdd";

    /**
     * time interval to display on calendar views. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1195)
    public static final String A_zimbraPrefCalendarViewTimeInterval = "zimbraPrefCalendarViewTimeInterval";

    /**
     * working hours for each day of the week
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1103)
    public static final String A_zimbraPrefCalendarWorkingHours = "zimbraPrefCalendarWorkingHours";

    /**
     * zimbraId of visible child accounts
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=553)
    public static final String A_zimbraPrefChildVisibleAccount = "zimbraPrefChildVisibleAccount";

    /**
     * user preference of client type
     */
    @ZAttr(id=453)
    public static final String A_zimbraPrefClientType = "zimbraPrefClientType";

    /**
     * direction for composing messages in the web client UI
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1273)
    public static final String A_zimbraPrefComposeDirection = "zimbraPrefComposeDirection";

    /**
     * whether or not to compose in html or text.
     */
    @ZAttr(id=217)
    public static final String A_zimbraPrefComposeFormat = "zimbraPrefComposeFormat";

    /**
     * whether or not compose messages in a new windows by default
     */
    @ZAttr(id=209)
    public static final String A_zimbraPrefComposeInNewWindow = "zimbraPrefComposeInNewWindow";

    /**
     * Deprecated since: 8.0.0. Since 8.0.0, the contact group can contain
     * member references, but member references are not searchable.. Orig
     * desc: Disables autocomplete matching against the members email
     * address.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1090)
    public static final String A_zimbraPrefContactsDisableAutocompleteOnContactGroupMembers = "zimbraPrefContactsDisableAutocompleteOnContactGroupMembers";

    /**
     * Deprecated since: 8.0.0. deprecated now that Zimbra supports keeping
     * member references in a contact group. Orig desc: Expand the contact
     * groups in Apple Address Book format to Zimbra format over CardDAV.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1102)
    public static final String A_zimbraPrefContactsExpandAppleContactGroups = "zimbraPrefContactsExpandAppleContactGroups";

    /**
     * Deprecated since: 6.0.5. We do not support cards view any more. See
     * bug 47439. Orig desc: initial contact view to use
     */
    @ZAttr(id=167)
    public static final String A_zimbraPrefContactsInitialView = "zimbraPrefContactsInitialView";

    /**
     * number of contacts per page
     */
    @ZAttr(id=148)
    public static final String A_zimbraPrefContactsPerPage = "zimbraPrefContactsPerPage";

    /**
     * order of messages displayed within a conversation
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=818)
    public static final String A_zimbraPrefConversationOrder = "zimbraPrefConversationOrder";

    /**
     * where the message reading pane is displayed in conv view
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1010)
    public static final String A_zimbraPrefConvReadingPaneLocation = "zimbraPrefConvReadingPaneLocation";

    /**
     * dedupeNone|secondCopyIfOnToOrCC|moveSentMessageToInbox|dedupeAll
     */
    @ZAttr(id=144)
    public static final String A_zimbraPrefDedupeMessagesSentToSelf = "zimbraPrefDedupeMessagesSentToSelf";

    /**
     * default font size
     *
     * @since ZCS 6.0.8
     */
    @ZAttr(id=1095)
    public static final String A_zimbraPrefDefaultPrintFontSize = "zimbraPrefDefaultPrintFontSize";

    /**
     * default mail signature for account/identity/dataSource
     */
    @ZAttr(id=492)
    public static final String A_zimbraPrefDefaultSignatureId = "zimbraPrefDefaultSignatureId";

    /**
     * whether meeting invite emails are moved to Trash folder upon
     * accept/decline
     */
    @ZAttr(id=470)
    public static final String A_zimbraPrefDeleteInviteOnReply = "zimbraPrefDeleteInviteOnReply";

    /**
     * zimlets user does not want to see in the UI
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1076)
    public static final String A_zimbraPrefDisabledZimlets = "zimbraPrefDisabledZimlets";

    /**
     * whether to display external images in HTML mail
     */
    @ZAttr(id=511)
    public static final String A_zimbraPrefDisplayExternalImages = "zimbraPrefDisplayExternalImages";

    /**
     * Specifies the meaning of an external sender. &quot;ALL&quot; means
     * users whose domain doesn&#039;t match the recipient&#039;s or
     * zimbraInternalSendersDomain. &quot;ALLNOTINAB&quot; means
     * &quot;ALL&quot; minus users who are in the recipient&#039;s address
     * book.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1320)
    public static final String A_zimbraPrefExternalSendersType = "zimbraPrefExternalSendersType";

    /**
     * indicates which application to use for file sharing
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1197)
    public static final String A_zimbraPrefFileSharingApplication = "zimbraPrefFileSharingApplication";

    /**
     * whether folder color is enabled
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=771)
    public static final String A_zimbraPrefFolderColorEnabled = "zimbraPrefFolderColorEnabled";

    /**
     * whether or not folder tree is expanded
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=637)
    public static final String A_zimbraPrefFolderTreeOpen = "zimbraPrefFolderTreeOpen";

    /**
     * the font for the web client
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1246)
    public static final String A_zimbraPrefFont = "zimbraPrefFont";

    /**
     * what part of the original message to include during forwards
     * (deprecatedSince 5.0 in identity). The value includeBody has been
     * deprecated since 6.0.6, use includeBodyAndHeaders instead.
     */
    @ZAttr(id=134)
    public static final String A_zimbraPrefForwardIncludeOriginalText = "zimbraPrefForwardIncludeOriginalText";

    /**
     * what format we reply/forward messages in (deprecatedSince 5.0 in
     * identity)
     */
    @ZAttr(id=413)
    public static final String A_zimbraPrefForwardReplyFormat = "zimbraPrefForwardReplyFormat";

    /**
     * whether or not to use same format (text or html) of message we are
     * replying to
     */
    @ZAttr(id=218)
    public static final String A_zimbraPrefForwardReplyInOriginalFormat = "zimbraPrefForwardReplyInOriginalFormat";

    /**
     * prefix character to use during forward/reply (deprecatedSince 5.0 in
     * identity)
     */
    @ZAttr(id=130)
    public static final String A_zimbraPrefForwardReplyPrefixChar = "zimbraPrefForwardReplyPrefixChar";

    /**
     * forward/reply signature id for account/identity/dataSource
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1125)
    public static final String A_zimbraPrefForwardReplySignatureId = "zimbraPrefForwardReplySignatureId";

    /**
     * email address to put in from header
     */
    @ZAttr(id=403)
    public static final String A_zimbraPrefFromAddress = "zimbraPrefFromAddress";

    /**
     * personal part of email address put in from header
     */
    @ZAttr(id=402)
    public static final String A_zimbraPrefFromDisplay = "zimbraPrefFromDisplay";

    /**
     * whether end-user wants auto-complete from GAL. Feature must also be
     * enabled.
     */
    @ZAttr(id=372)
    public static final String A_zimbraPrefGalAutoCompleteEnabled = "zimbraPrefGalAutoCompleteEnabled";

    /**
     * whether end-user wants search from GAL. Feature must also be enabled
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=635)
    public static final String A_zimbraPrefGalSearchEnabled = "zimbraPrefGalSearchEnabled";

    /**
     * action to perform for the get mail button in UI
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1067)
    public static final String A_zimbraPrefGetMailAction = "zimbraPrefGetMailAction";

    /**
     * how to group mail by default
     */
    @ZAttr(id=54)
    public static final String A_zimbraPrefGroupMailBy = "zimbraPrefGroupMailBy";

    /**
     * default font color
     */
    @ZAttr(id=260)
    public static final String A_zimbraPrefHtmlEditorDefaultFontColor = "zimbraPrefHtmlEditorDefaultFontColor";

    /**
     * default font family
     */
    @ZAttr(id=258)
    public static final String A_zimbraPrefHtmlEditorDefaultFontFamily = "zimbraPrefHtmlEditorDefaultFontFamily";

    /**
     * default font size
     */
    @ZAttr(id=259)
    public static final String A_zimbraPrefHtmlEditorDefaultFontSize = "zimbraPrefHtmlEditorDefaultFontSize";

    /**
     * Unique ID for an identity
     */
    @ZAttr(id=433)
    public static final String A_zimbraPrefIdentityId = "zimbraPrefIdentityId";

    /**
     * name of the identity
     */
    @ZAttr(id=412)
    public static final String A_zimbraPrefIdentityName = "zimbraPrefIdentityName";

    /**
     * whether or not the IMAP server exports search folders
     */
    @ZAttr(id=241)
    public static final String A_zimbraPrefImapSearchFoldersEnabled = "zimbraPrefImapSearchFoldersEnabled";

    /**
     * whether to login to the IM client automatically
     */
    @ZAttr(id=488)
    public static final String A_zimbraPrefIMAutoLogin = "zimbraPrefIMAutoLogin";

    /**
     * IM buddy list sort order
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=705)
    public static final String A_zimbraPrefIMBuddyListSort = "zimbraPrefIMBuddyListSort";

    /**
     * Custom IM status messages
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=645)
    public static final String A_zimbraPrefIMCustomStatusMessage = "zimbraPrefIMCustomStatusMessage";

    /**
     * Flash IM icon on new messages
     */
    @ZAttr(id=462)
    public static final String A_zimbraPrefIMFlashIcon = "zimbraPrefIMFlashIcon";

    /**
     * Flash title bar when a new IM arrives
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=679)
    public static final String A_zimbraPrefIMFlashTitle = "zimbraPrefIMFlashTitle";

    /**
     * whether to hide IM blocked buddies
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=707)
    public static final String A_zimbraPrefIMHideBlockedBuddies = "zimbraPrefIMHideBlockedBuddies";

    /**
     * whether to hide IM offline buddies
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=706)
    public static final String A_zimbraPrefIMHideOfflineBuddies = "zimbraPrefIMHideOfflineBuddies";

    /**
     * IM idle status
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=560)
    public static final String A_zimbraPrefIMIdleStatus = "zimbraPrefIMIdleStatus";

    /**
     * IM session idle timeout in minutes
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=559)
    public static final String A_zimbraPrefIMIdleTimeout = "zimbraPrefIMIdleTimeout";

    /**
     * Enable instant notifications
     */
    @ZAttr(id=517)
    public static final String A_zimbraPrefIMInstantNotify = "zimbraPrefIMInstantNotify";

    /**
     * whether to log IM chats to the Chats folder
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=556)
    public static final String A_zimbraPrefIMLogChats = "zimbraPrefIMLogChats";

    /**
     * whether IM log chats is enabled
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=552)
    public static final String A_zimbraPrefIMLogChatsEnabled = "zimbraPrefIMLogChatsEnabled";

    /**
     * Notify for presence modifications
     */
    @ZAttr(id=463)
    public static final String A_zimbraPrefIMNotifyPresence = "zimbraPrefIMNotifyPresence";

    /**
     * Notify for status change
     */
    @ZAttr(id=464)
    public static final String A_zimbraPrefIMNotifyStatus = "zimbraPrefIMNotifyStatus";

    /**
     * whether to report IM idle status
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=558)
    public static final String A_zimbraPrefIMReportIdle = "zimbraPrefIMReportIdle";

    /**
     * whether sounds is enabled in IM
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=570)
    public static final String A_zimbraPrefIMSoundsEnabled = "zimbraPrefIMSoundsEnabled";

    /**
     * whether to enable toaster notification for IM
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=814)
    public static final String A_zimbraPrefIMToasterEnabled = "zimbraPrefIMToasterEnabled";

    /**
     * last used yahoo id
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=757)
    public static final String A_zimbraPrefIMYahooId = "zimbraPrefIMYahooId";

    /**
     * Retention period of read messages in the Inbox folder. 0 means that
     * all messages will be retained. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=538)
    public static final String A_zimbraPrefInboxReadLifetime = "zimbraPrefInboxReadLifetime";

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=537)
    public static final String A_zimbraPrefInboxUnreadLifetime = "zimbraPrefInboxUnreadLifetime";

    /**
     * whether to include shared items in search
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1338)
    public static final String A_zimbraPrefIncludeSharedItemsInSearch = "zimbraPrefIncludeSharedItemsInSearch";

    /**
     * whether or not to include spam in search by default
     */
    @ZAttr(id=55)
    public static final String A_zimbraPrefIncludeSpamInSearch = "zimbraPrefIncludeSpamInSearch";

    /**
     * whether or not to include trash in search by default
     */
    @ZAttr(id=56)
    public static final String A_zimbraPrefIncludeTrashInSearch = "zimbraPrefIncludeTrashInSearch";

    /**
     * number of messages/conversations per virtual page
     *
     * @since ZCS 6.0.6
     */
    @ZAttr(id=1079)
    public static final String A_zimbraPrefItemsPerVirtualPage = "zimbraPrefItemsPerVirtualPage";

    /**
     * Retention period of messages in the Junk folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailSpamLifetime, which is admin-modifiable.
     * The shorter duration is used. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=540)
    public static final String A_zimbraPrefJunkLifetime = "zimbraPrefJunkLifetime";

    /**
     * optional account descriptive label
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=603)
    public static final String A_zimbraPrefLabel = "zimbraPrefLabel";

    /**
     * list view columns in web client
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=694)
    public static final String A_zimbraPrefListViewColumns = "zimbraPrefListViewColumns";

    /**
     * user locale preference, e.g. en_US Whenever the server looks for the
     * user locale, it will first look for zimbraPrefLocale, if it is not set
     * then it will fallback to the current mechanism of looking for
     * zimbraLocale in the various places for a user. zimbraLocale is the non
     * end-user attribute that specifies which locale an object defaults to,
     * it is not an end-user setting.
     */
    @ZAttr(id=442)
    public static final String A_zimbraPrefLocale = "zimbraPrefLocale";

    /**
     * Default Charset for mail composing and parsing text
     */
    @ZAttr(id=469)
    public static final String A_zimbraPrefMailDefaultCharset = "zimbraPrefMailDefaultCharset";

    /**
     * Flash icon when a new email arrives
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=681)
    public static final String A_zimbraPrefMailFlashIcon = "zimbraPrefMailFlashIcon";

    /**
     * Flash title bar when a new email arrives
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=680)
    public static final String A_zimbraPrefMailFlashTitle = "zimbraPrefMailFlashTitle";

    /**
     * a list of comma separated folder ids of all folders used to count for
     * showing a new message indicator icon for the account, useful in UIs
     * managing multiple accounts: desktop and family mailboxes.
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1072)
    public static final String A_zimbraPrefMailFoldersCheckedForNewMsgIndicator = "zimbraPrefMailFoldersCheckedForNewMsgIndicator";

    /**
     * RFC822 forwarding address for an account
     */
    @ZAttr(id=343)
    public static final String A_zimbraPrefMailForwardingAddress = "zimbraPrefMailForwardingAddress";

    /**
     * initial search done by dhtml client
     */
    @ZAttr(id=102)
    public static final String A_zimbraPrefMailInitialSearch = "zimbraPrefMailInitialSearch";

    /**
     * number of messages/conversations per page
     */
    @ZAttr(id=57)
    public static final String A_zimbraPrefMailItemsPerPage = "zimbraPrefMailItemsPerPage";

    /**
     * whether or not to deliver mail locally
     */
    @ZAttr(id=344)
    public static final String A_zimbraPrefMailLocalDeliveryDisabled = "zimbraPrefMailLocalDeliveryDisabled";

    /**
     * interval at which the web client polls the server for new messages.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=111)
    public static final String A_zimbraPrefMailPollingInterval = "zimbraPrefMailPollingInterval";

    /**
     * whether web UI should always request read receipts for outgoing
     * messages
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1217)
    public static final String A_zimbraPrefMailRequestReadReceipts = "zimbraPrefMailRequestReadReceipts";

    /**
     * After deleting a message in list, which message should be selected
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1046)
    public static final String A_zimbraPrefMailSelectAfterDelete = "zimbraPrefMailSelectAfterDelete";

    /**
     * whether to send read receipt
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=822)
    public static final String A_zimbraPrefMailSendReadReceipts = "zimbraPrefMailSendReadReceipts";

    /**
     * mail text signature (deprecatedSince 5.0 in identity)
     */
    @ZAttr(id=17)
    public static final String A_zimbraPrefMailSignature = "zimbraPrefMailSignature";

    /**
     * contact id associated with the signature
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1129)
    public static final String A_zimbraPrefMailSignatureContactId = "zimbraPrefMailSignatureContactId";

    /**
     * mail signature enabled (deprecatedSince 5.0 in identity)
     */
    @ZAttr(id=18)
    public static final String A_zimbraPrefMailSignatureEnabled = "zimbraPrefMailSignatureEnabled";

    /**
     * mail html signature
     */
    @ZAttr(id=516)
    public static final String A_zimbraPrefMailSignatureHTML = "zimbraPrefMailSignatureHTML";

    /**
     * mail signature style outlook|internet (deprecatedSince 5.0 in
     * identity)
     */
    @ZAttr(id=156)
    public static final String A_zimbraPrefMailSignatureStyle = "zimbraPrefMailSignatureStyle";

    /**
     * Deprecated since: 7.1.1. deprecated in favor of userCertificate and
     * userSMIMECertificate. Orig desc: user&#039;s S/MIME public keys
     * (certificates)
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1172)
    public static final String A_zimbraPrefMailSMIMECertificate = "zimbraPrefMailSMIMECertificate";

    /**
     * whether audible alert is enabled when a new email arrives
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=666)
    public static final String A_zimbraPrefMailSoundsEnabled = "zimbraPrefMailSoundsEnabled";

    /**
     * whether to enable toaster notification for new mail
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=812)
    public static final String A_zimbraPrefMailToasterEnabled = "zimbraPrefMailToasterEnabled";

    /**
     * Trusted sender email addresses or domains. External images in emails
     * sent by trusted senders are automatically loaded in the message view.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1138)
    public static final String A_zimbraPrefMailTrustedSenderList = "zimbraPrefMailTrustedSenderList";

    /**
     * whether mandatory spell check is enabled
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=749)
    public static final String A_zimbraPrefMandatorySpellCheckEnabled = "zimbraPrefMandatorySpellCheckEnabled";

    /**
     * whether and mark a message as read -1: Do not mark read 0: Mark read
     * 1..n: Mark read after this many seconds
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=650)
    public static final String A_zimbraPrefMarkMsgRead = "zimbraPrefMarkMsgRead";

    /**
     * Account-level switch that enables message deduping. See
     * zimbraMessageIdDedupeCacheSize for more details.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1198)
    public static final String A_zimbraPrefMessageIdDedupingEnabled = "zimbraPrefMessageIdDedupingEnabled";

    /**
     * whether client prefers text/html or text/plain
     */
    @ZAttr(id=145)
    public static final String A_zimbraPrefMessageViewHtmlPreferred = "zimbraPrefMessageViewHtmlPreferred";

    /**
     * RFC822 email address for email notifications
     */
    @ZAttr(id=127)
    public static final String A_zimbraPrefNewMailNotificationAddress = "zimbraPrefNewMailNotificationAddress";

    /**
     * whether or not new mail notification is enabled
     */
    @ZAttr(id=126)
    public static final String A_zimbraPrefNewMailNotificationEnabled = "zimbraPrefNewMailNotificationEnabled";

    /**
     * whether or not the client opens a new msg/conv in a new window (via
     * dbl-click)
     */
    @ZAttr(id=500)
    public static final String A_zimbraPrefOpenMailInNewWindow = "zimbraPrefOpenMailInNewWindow";

    /**
     * server remembers addresses to which notifications have been sent for
     * this interval, and does not send duplicate notifications in this
     * interval. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     */
    @ZAttr(id=386)
    public static final String A_zimbraPrefOutOfOfficeCacheDuration = "zimbraPrefOutOfOfficeCacheDuration";

    /**
     * per RFC 3834 no out of office notifications are sent if recipients
     * address is not directly specified in the To/CC headers - for this
     * check, we check to see if To/CC contained accounts address, aliases,
     * canonical address. But when external accounts are forwarded to Zimbra,
     * and you want notifications sent to messages that contain their
     * external address in To/Cc, add those address, then you can specify
     * those external addresses here.
     */
    @ZAttr(id=387)
    public static final String A_zimbraPrefOutOfOfficeDirectAddress = "zimbraPrefOutOfOfficeDirectAddress";

    /**
     * out of office message to external senders
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1317)
    public static final String A_zimbraPrefOutOfOfficeExternalReply = "zimbraPrefOutOfOfficeExternalReply";

    /**
     * If TRUE, send zimbraPrefOutOfOfficeExternalReply to external senders.
     * External senders are specified by zimbraInternalSendersDomain and
     * zimbraPrefExternalSendersType.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1318)
    public static final String A_zimbraPrefOutOfOfficeExternalReplyEnabled = "zimbraPrefOutOfOfficeExternalReplyEnabled";

    /**
     * free/busy status while out of office
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1334)
    public static final String A_zimbraPrefOutOfOfficeFreeBusyStatus = "zimbraPrefOutOfOfficeFreeBusyStatus";

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is after this date
     */
    @ZAttr(id=384)
    public static final String A_zimbraPrefOutOfOfficeFromDate = "zimbraPrefOutOfOfficeFromDate";

    /**
     * out of office message
     */
    @ZAttr(id=58)
    public static final String A_zimbraPrefOutOfOfficeReply = "zimbraPrefOutOfOfficeReply";

    /**
     * whether or not out of office reply is enabled
     */
    @ZAttr(id=59)
    public static final String A_zimbraPrefOutOfOfficeReplyEnabled = "zimbraPrefOutOfOfficeReplyEnabled";

    /**
     * when user has OOO message enabled, when they login into web client,
     * whether to alert the user that the OOO message is turned on and
     * provide the ability to turn it off
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1245)
    public static final String A_zimbraPrefOutOfOfficeStatusAlertOnLogin = "zimbraPrefOutOfOfficeStatusAlertOnLogin";

    /**
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     */
    @ZAttr(id=385)
    public static final String A_zimbraPrefOutOfOfficeUntilDate = "zimbraPrefOutOfOfficeUntilDate";

    /**
     * When messages are accessed via POP3: - keep: Leave DELE&#039;ed
     * messages in Inbox. - read: Mark RETR&#039;ed messages as read, and
     * leave DELE&#039;ed messages in Inbox. - trash: Move DELE&#039;ed
     * messages to Trash, and mark them as read. - delete: Hard-delete
     * DELE&#039;ed messages. This is the straightforward POP3
     * implementation.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1165)
    public static final String A_zimbraPrefPop3DeleteOption = "zimbraPrefPop3DeleteOption";

    /**
     * download pop3 messages since
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=653)
    public static final String A_zimbraPrefPop3DownloadSince = "zimbraPrefPop3DownloadSince";

    /**
     * whether or not to include spam messages in POP3 access
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1166)
    public static final String A_zimbraPrefPop3IncludeSpam = "zimbraPrefPop3IncludeSpam";

    /**
     * quick command encoded by the client
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1211)
    public static final String A_zimbraPrefQuickCommand = "zimbraPrefQuickCommand";

    /**
     * Deprecated since: 6.0.0_BETA2. deprecated in favor of
     * zimbraPrefReadingPaneLocation and zimbraPrefConvReadingPaneLocation.
     * Orig desc: whether reading pane is shown by default
     */
    @ZAttr(id=394)
    public static final String A_zimbraPrefReadingPaneEnabled = "zimbraPrefReadingPaneEnabled";

    /**
     * where the message reading pane is displayed in list views
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=804)
    public static final String A_zimbraPrefReadingPaneLocation = "zimbraPrefReadingPaneLocation";

    /**
     * Deprecated since: 6.0.8. Deprecated per bug 46988. This feature was
     * never fully implemented.. Orig desc: address to put in reply-to header
     * of read receipt messages, if it is not set, then the compose
     * identities primary email address is used.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=823)
    public static final String A_zimbraPrefReadReceiptsToAddress = "zimbraPrefReadReceiptsToAddress";

    /**
     * what part of the original message to include during replies
     * (deprecatedSince 5.0 in identity). The value includeBody has been
     * deprecated since 6.0.6, use includeBodyAndHeaders instead.
     */
    @ZAttr(id=133)
    public static final String A_zimbraPrefReplyIncludeOriginalText = "zimbraPrefReplyIncludeOriginalText";

    /**
     * address to put in reply-to header
     */
    @ZAttr(id=60)
    public static final String A_zimbraPrefReplyToAddress = "zimbraPrefReplyToAddress";

    /**
     * personal part of email address put in reply-to header
     */
    @ZAttr(id=404)
    public static final String A_zimbraPrefReplyToDisplay = "zimbraPrefReplyToDisplay";

    /**
     * TRUE if we should set a reply-to header
     */
    @ZAttr(id=405)
    public static final String A_zimbraPrefReplyToEnabled = "zimbraPrefReplyToEnabled";

    /**
     * whether or not to save outgoing mail (deprecatedSince 5.0 in identity)
     */
    @ZAttr(id=22)
    public static final String A_zimbraPrefSaveToSent = "zimbraPrefSaveToSent";

    /**
     * whether or not search tree is expanded
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=634)
    public static final String A_zimbraPrefSearchTreeOpen = "zimbraPrefSearchTreeOpen";

    /**
     * Retention period of messages in the Sent folder. 0 means that all
     * messages will be retained. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=539)
    public static final String A_zimbraPrefSentLifetime = "zimbraPrefSentLifetime";

    /**
     * name of folder to save sent mail in (deprecatedSince 5.0 in identity)
     */
    @ZAttr(id=103)
    public static final String A_zimbraPrefSentMailFolder = "zimbraPrefSentMailFolder";

    /**
     * whether end-user wants auto-complete from shared address books.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=759)
    public static final String A_zimbraPrefSharedAddrBookAutoCompleteEnabled = "zimbraPrefSharedAddrBookAutoCompleteEnabled";

    /**
     * keyboard shortcuts
     */
    @ZAttr(id=396)
    public static final String A_zimbraPrefShortcuts = "zimbraPrefShortcuts";

    /**
     * show just the display name of email addresses in the message header
     * area and compose pane
     *
     * @since ZCS 7.0.1
     */
    @ZAttr(id=1173)
    public static final String A_zimbraPrefShortEmailAddress = "zimbraPrefShortEmailAddress";

    /**
     * show calendar week in calendar views
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1045)
    public static final String A_zimbraPrefShowCalendarWeek = "zimbraPrefShowCalendarWeek";

    /**
     * whether or not to show direction buttons in compose toolbar
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1274)
    public static final String A_zimbraPrefShowComposeDirection = "zimbraPrefShowComposeDirection";

    /**
     * show fragments in conversation and message lists
     */
    @ZAttr(id=192)
    public static final String A_zimbraPrefShowFragments = "zimbraPrefShowFragments";

    /**
     * whether to show search box or not
     */
    @ZAttr(id=222)
    public static final String A_zimbraPrefShowSearchString = "zimbraPrefShowSearchString";

    /**
     * show selection checkbox for selecting email, contact, voicemail items
     * in a list view for batch operations
     */
    @ZAttr(id=471)
    public static final String A_zimbraPrefShowSelectionCheckbox = "zimbraPrefShowSelectionCheckbox";

    /**
     * Skin to use for this account
     */
    @ZAttr(id=355)
    public static final String A_zimbraPrefSkin = "zimbraPrefSkin";

    /**
     * sort order for list view in the WEB UI
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1188)
    public static final String A_zimbraPrefSortOrder = "zimbraPrefSortOrder";

    /**
     * The name of the dictionary used for spell checking. If not set, the
     * locale is used.
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1041)
    public static final String A_zimbraPrefSpellDictionary = "zimbraPrefSpellDictionary";

    /**
     * If TRUE, the spell checker ignores words that contain only upper-case
     * letters.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1207)
    public static final String A_zimbraPrefSpellIgnoreAllCaps = "zimbraPrefSpellIgnoreAllCaps";

    /**
     * List of words to ignore when checking spelling. The word list of an
     * account includes the words specified for its cos and domain.
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1073)
    public static final String A_zimbraPrefSpellIgnoreWord = "zimbraPrefSpellIgnoreWord";

    /**
     * whether standard client should operate in accessibility Mode
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=689)
    public static final String A_zimbraPrefStandardClientAccessibilityMode = "zimbraPrefStandardClientAccessibilityMode";

    /**
     * whether or not tag tree is expanded
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=633)
    public static final String A_zimbraPrefTagTreeOpen = "zimbraPrefTagTreeOpen";

    /**
     * preferred task filtering option in UI
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1323)
    public static final String A_zimbraPrefTasksFilterBy = "zimbraPrefTasksFilterBy";

    /**
     * where the reading pane is displayed for tasks
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1151)
    public static final String A_zimbraPrefTasksReadingPaneLocation = "zimbraPrefTasksReadingPaneLocation";

    /**
     * time zone of user or COS
     */
    @ZAttr(id=235)
    public static final String A_zimbraPrefTimeZoneId = "zimbraPrefTimeZoneId";

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=541)
    public static final String A_zimbraPrefTrashLifetime = "zimbraPrefTrashLifetime";

    /**
     * Deprecated since: 5.0. no longer used in account or identity. Orig
     * desc: TRUE if we this identity should get settings from the default
     * identity
     */
    @ZAttr(id=410)
    public static final String A_zimbraPrefUseDefaultIdentitySettings = "zimbraPrefUseDefaultIdentitySettings";

    /**
     * whether or not keyboard shortcuts are enabled
     */
    @ZAttr(id=61)
    public static final String A_zimbraPrefUseKeyboardShortcuts = "zimbraPrefUseKeyboardShortcuts";

    /**
     * When composing and sending mail, whether to use RFC 2231 MIME
     * parameter value encoding. If set to FALSE, then RFC 2047 style
     * encoding is used.
     */
    @ZAttr(id=395)
    public static final String A_zimbraPrefUseRfc2231 = "zimbraPrefUseRfc2231";

    /**
     * whether list of well known time zones is displayed in calendar UI
     */
    @ZAttr(id=236)
    public static final String A_zimbraPrefUseTimeZoneListInCalendar = "zimbraPrefUseTimeZoneListInCalendar";

    /**
     * number of voice messages/call logs per page
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=526)
    public static final String A_zimbraPrefVoiceItemsPerPage = "zimbraPrefVoiceItemsPerPage";

    /**
     * whether to display a warning when users try to navigate away from ZCS
     */
    @ZAttr(id=456)
    public static final String A_zimbraPrefWarnOnExit = "zimbraPrefWarnOnExit";

    /**
     * if replying/forwarding a message in this folder, use this identity
     * (deprecatedSince 5.0 in account)
     */
    @ZAttr(id=409)
    public static final String A_zimbraPrefWhenInFolderIds = "zimbraPrefWhenInFolderIds";

    /**
     * TRUE if we should look at zimbraPrefWhenInFolderIds (deprecatedSince
     * 5.0 in account)
     */
    @ZAttr(id=408)
    public static final String A_zimbraPrefWhenInFoldersEnabled = "zimbraPrefWhenInFoldersEnabled";

    /**
     * addresses that we will look at to see if we should use an identity
     * (deprecatedSince 5.0 in account)
     */
    @ZAttr(id=407)
    public static final String A_zimbraPrefWhenSentToAddresses = "zimbraPrefWhenSentToAddresses";

    /**
     * TRUE if we should look at zimbraPrefWhenSentToAddresses
     * (deprecatedSince 5.0 in account)
     */
    @ZAttr(id=406)
    public static final String A_zimbraPrefWhenSentToEnabled = "zimbraPrefWhenSentToEnabled";

    /**
     * zimlets user wants to see in the UI
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=765)
    public static final String A_zimbraPrefZimlets = "zimbraPrefZimlets";

    /**
     * whether or not zimlet tree is expanded
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=638)
    public static final String A_zimbraPrefZimletTreeOpen = "zimbraPrefZimletTreeOpen";

    /**
     * Allowed domains for Proxy servlet
     */
    @ZAttr(id=294)
    public static final String A_zimbraProxyAllowedDomains = "zimbraProxyAllowedDomains";

    /**
     * Content types that can be cached by proxy servlet
     */
    @ZAttr(id=303)
    public static final String A_zimbraProxyCacheableContentTypes = "zimbraProxyCacheableContentTypes";

    /**
     * Name to be used in public API such as REST or SOAP proxy.
     */
    @ZAttr(id=377)
    public static final String A_zimbraPublicServiceHostname = "zimbraPublicServiceHostname";

    /**
     * Port to be used in public API such as REST or SOAP proxy.
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=699)
    public static final String A_zimbraPublicServicePort = "zimbraPublicServicePort";

    /**
     * Protocol to be used in public API such as REST or SOAP proxy.
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=698)
    public static final String A_zimbraPublicServiceProtocol = "zimbraPublicServiceProtocol";

    /**
     * Maximum allowed lifetime of public shares. A value of 0 indicates that
     * there&#039;s no limit on a public share&#039;s lifetime. . Must be in
     * valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1355)
    public static final String A_zimbraPublicShareLifetime = "zimbraPublicShareLifetime";

    /**
     * switch for turning public sharing on/off
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1351)
    public static final String A_zimbraPublicSharingEnabled = "zimbraPublicSharingEnabled";

    /**
     * Last time a quota warning was sent.
     */
    @ZAttr(id=484)
    public static final String A_zimbraQuotaLastWarnTime = "zimbraQuotaLastWarnTime";

    /**
     * Minimum duration of time between quota warnings.. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     */
    @ZAttr(id=485)
    public static final String A_zimbraQuotaWarnInterval = "zimbraQuotaWarnInterval";

    /**
     * Quota warning message template.
     */
    @ZAttr(id=486)
    public static final String A_zimbraQuotaWarnMessage = "zimbraQuotaWarnMessage";

    /**
     * Threshold for quota warning messages.
     */
    @ZAttr(id=483)
    public static final String A_zimbraQuotaWarnPercent = "zimbraQuotaWarnPercent";

    /**
     * redolog rollover destination
     */
    @ZAttr(id=76)
    public static final String A_zimbraRedoLogArchiveDir = "zimbraRedoLogArchiveDir";

    /**
     * how many seconds worth of committed redo ops to re-execute during
     * crash recovery; related to mysql parameter
     * innodb_flush_log_at_trx_commit=0
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1009)
    public static final String A_zimbraRedoLogCrashRecoveryLookbackSec = "zimbraRedoLogCrashRecoveryLookbackSec";

    /**
     * whether logs are delete on rollover or archived
     */
    @ZAttr(id=251)
    public static final String A_zimbraRedoLogDeleteOnRollover = "zimbraRedoLogDeleteOnRollover";

    /**
     * whether redo logging is enabled
     */
    @ZAttr(id=74)
    public static final String A_zimbraRedoLogEnabled = "zimbraRedoLogEnabled";

    /**
     * how frequently writes to redo log get fsynced to disk
     */
    @ZAttr(id=79)
    public static final String A_zimbraRedoLogFsyncIntervalMS = "zimbraRedoLogFsyncIntervalMS";

    /**
     * name and location of the redolog file
     */
    @ZAttr(id=75)
    public static final String A_zimbraRedoLogLogPath = "zimbraRedoLogLogPath";

    /**
     * provider class name for redo logging
     */
    @ZAttr(id=225)
    public static final String A_zimbraRedoLogProvider = "zimbraRedoLogProvider";

    /**
     * redo.log file becomes eligible for rollover over when it goes over
     * this size
     */
    @ZAttr(id=78)
    public static final String A_zimbraRedoLogRolloverFileSizeKB = "zimbraRedoLogRolloverFileSizeKB";

    /**
     * redo.log file rolls over when it goes over this size, even if it does
     * not meet the minimum file age requirement
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1021)
    public static final String A_zimbraRedoLogRolloverHardMaxFileSizeKB = "zimbraRedoLogRolloverHardMaxFileSizeKB";

    /**
     * minimum age in minutes for redo.log file before it becomes eligible
     * for rollover based on size
     *
     * @since ZCS 5.0.17
     */
    @ZAttr(id=1020)
    public static final String A_zimbraRedoLogRolloverMinFileAge = "zimbraRedoLogRolloverMinFileAge";

    /**
     * Path to remote management command to execute on this server
     */
    @ZAttr(id=336)
    public static final String A_zimbraRemoteManagementCommand = "zimbraRemoteManagementCommand";

    /**
     * Port on which remote management sshd listening on this server.
     */
    @ZAttr(id=339)
    public static final String A_zimbraRemoteManagementPort = "zimbraRemoteManagementPort";

    /**
     * Private key this server should use to access another server
     */
    @ZAttr(id=338)
    public static final String A_zimbraRemoteManagementPrivateKeyPath = "zimbraRemoteManagementPrivateKeyPath";

    /**
     * Login name of user allowed to execute remote management command
     */
    @ZAttr(id=337)
    public static final String A_zimbraRemoteManagementUser = "zimbraRemoteManagementUser";

    /**
     * Custom response headers. For example, can be used to add a P3P header
     * for user agents to understand the sites privacy policy. Note: the
     * value MUST be the entire header line (e.g. X-Foo: Bar).
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1074)
    public static final String A_zimbraResponseHeader = "zimbraResponseHeader";

    /**
     * indicate whether to turn on admin console proxy
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1321)
    public static final String A_zimbraReverseProxyAdminEnabled = "zimbraReverseProxyAdminEnabled";

    /**
     * Allowed reverse proxy IP addresses. Lookup servlet will only generate
     * authtokens if request was made from one of these IP addresses
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=697)
    public static final String A_zimbraReverseProxyAdminIPAddress = "zimbraReverseProxyAdminIPAddress";

    /**
     * the attribute that identifies the zimbra admin bind port
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=700)
    public static final String A_zimbraReverseProxyAdminPortAttribute = "zimbraReverseProxyAdminPortAttribute";

    /**
     * wait duration before nginx sending back the NO response for failed
     * imap/pop3 reverse proxy lookups. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=569)
    public static final String A_zimbraReverseProxyAuthWaitInterval = "zimbraReverseProxyAuthWaitInterval";

    /**
     * time interval that an entry cached by NGINX will remain in the cache.
     * Must be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public static final String A_zimbraReverseProxyCacheEntryTTL = "zimbraReverseProxyCacheEntryTTL";

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public static final String A_zimbraReverseProxyCacheFetchTimeout = "zimbraReverseProxyCacheFetchTimeout";

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected. Must
     * be in valid duration format: {digits}{time-unit}. digits: 0-9,
     * time-unit: [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days,
     * ms - milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public static final String A_zimbraReverseProxyCacheReconnectInterval = "zimbraReverseProxyCacheReconnectInterval";

    /**
     * CA certificate for authenticating client certificates in nginx proxy
     * (https only)
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1201)
    public static final String A_zimbraReverseProxyClientCertCA = "zimbraReverseProxyClientCertCA";

    /**
     * enable authentication via X.509 Client Certificate in nginx proxy
     * (https only)
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1200)
    public static final String A_zimbraReverseProxyClientCertMode = "zimbraReverseProxyClientCertMode";

    /**
     * Time interval after which NGINX mail proxy will disconnect while
     * establishing an upstream IMAP/POP connection. Must be in valid
     * duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=797)
    public static final String A_zimbraReverseProxyConnectTimeout = "zimbraReverseProxyConnectTimeout";

    /**
     * The default realm that will be used by NGINX mail proxy, when the
     * realm is not specified in GSSAPI Authentication
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=703)
    public static final String A_zimbraReverseProxyDefaultRealm = "zimbraReverseProxyDefaultRealm";

    /**
     * LDAP attribute that contains domain name for the domain
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=547)
    public static final String A_zimbraReverseProxyDomainNameAttribute = "zimbraReverseProxyDomainNameAttribute";

    /**
     * LDAP query to find a domain
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=545)
    public static final String A_zimbraReverseProxyDomainNameQuery = "zimbraReverseProxyDomainNameQuery";

    /**
     * search base for zimbraReverseProxyDomainNameQuery
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=546)
    public static final String A_zimbraReverseProxyDomainNameSearchBase = "zimbraReverseProxyDomainNameSearchBase";

    /**
     * the URL of customized proxy error handler. If set, when errors happen
     * in proxy, proxy will redirect to this URL with two paras - err: error
     * code; up: the addr of upstream server connecting to which the error
     * happens
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1332)
    public static final String A_zimbraReverseProxyErrorHandlerURL = "zimbraReverseProxyErrorHandlerURL";

    /**
     * Whether to enable HTTP proxy
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=628)
    public static final String A_zimbraReverseProxyHttpEnabled = "zimbraReverseProxyHttpEnabled";

    /**
     * attribute that contains http bind port
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=632)
    public static final String A_zimbraReverseProxyHttpPortAttribute = "zimbraReverseProxyHttpPortAttribute";

    /**
     * attribute that contains http ssl bind port
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1359)
    public static final String A_zimbraReverseProxyHttpSSLPortAttribute = "zimbraReverseProxyHttpSSLPortAttribute";

    /**
     * NGINX reverse proxy imap capabilities
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=719)
    public static final String A_zimbraReverseProxyImapEnabledCapability = "zimbraReverseProxyImapEnabledCapability";

    /**
     * Whether to expose version on Proxy IMAP banner
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=713)
    public static final String A_zimbraReverseProxyImapExposeVersionOnBanner = "zimbraReverseProxyImapExposeVersionOnBanner";

    /**
     * attribute that contains imap bind port
     */
    @ZAttr(id=479)
    public static final String A_zimbraReverseProxyImapPortAttribute = "zimbraReverseProxyImapPortAttribute";

    /**
     * whether IMAP SASL GSSAPI is enabled for reverse proxy
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=643)
    public static final String A_zimbraReverseProxyImapSaslGssapiEnabled = "zimbraReverseProxyImapSaslGssapiEnabled";

    /**
     * whether IMAP SASL PLAIN is enabled for reverse proxy
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=728)
    public static final String A_zimbraReverseProxyImapSaslPlainEnabled = "zimbraReverseProxyImapSaslPlainEnabled";

    /**
     * attribute that contains imap bind port for SSL
     */
    @ZAttr(id=480)
    public static final String A_zimbraReverseProxyImapSSLPortAttribute = "zimbraReverseProxyImapSSLPortAttribute";

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=641)
    public static final String A_zimbraReverseProxyImapStartTlsMode = "zimbraReverseProxyImapStartTlsMode";

    /**
     * Deprecated since: 8.0.0. deprecated in favor of local config
     * &quot;imap_max_idle_time&quot;, &quot;pop3_max_idle_time&quot;,
     * &quot;imap_authenticated_max_idle_time&quot; in bug 59685. Orig desc:
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=735)
    public static final String A_zimbraReverseProxyInactivityTimeout = "zimbraReverseProxyInactivityTimeout";

    /**
     * Sets the upper limit on logins from a remote IP via POP or IMAP to
     * this proxy server after which login is rejected with an appropriate
     * protocol specific bye response. This counter is cumulative for all
     * users that appear to the proxy to be logging in from the same IP
     * address. If multiple users appear to the proxy to be logging in from
     * the same IP address (usual with NATing), then each of the different
     * users login will contribute to increasing the hit counter for that IP
     * address, and when the counter eventually exceeds the limit, then the
     * connections from that IP address will be throttled. Therefore, all
     * users from the same IP will contribute to (and be affected by) this
     * counter. Logins using all protocols (POP3/POP3S/IMAP/IMAPS) will
     * affect this counter (the counter is aggregate for all protocols, *not*
     * separate). If this value is set to 0, then no limiting will take place
     * for any IP.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=622)
    public static final String A_zimbraReverseProxyIPLoginLimit = "zimbraReverseProxyIPLoginLimit";

    /**
     * Sets the time-to-live for the hit counter for IP based login
     * throttling. If time is set to 3600 and limit is set to 1000, then it
     * means that NGINX should not allow more than 1000 users to log in via
     * the proxy from the same IP, within the time interval of an hour. The
     * semantics for such a configuration would then be: allow maximum 1000
     * users per hour from any given IP address.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=623)
    public static final String A_zimbraReverseProxyIPLoginLimitTime = "zimbraReverseProxyIPLoginLimitTime";

    /**
     * The error message with which a connection attempt from an IP address
     * will be throttled, if the connection count exceeds the configured
     * limit
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=727)
    public static final String A_zimbraReverseProxyIpThrottleMsg = "zimbraReverseProxyIpThrottleMsg";

    /**
     * Log level for NGINX Proxy error log
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=723)
    public static final String A_zimbraReverseProxyLogLevel = "zimbraReverseProxyLogLevel";

    /**
     * whether this server is a reverse proxy lookup target
     */
    @ZAttr(id=504)
    public static final String A_zimbraReverseProxyLookupTarget = "zimbraReverseProxyLookupTarget";

    /**
     * Whether to enable IMAP/POP proxy
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=629)
    public static final String A_zimbraReverseProxyMailEnabled = "zimbraReverseProxyMailEnabled";

    /**
     * LDAP attribute that contains mailhost for the user
     */
    @ZAttr(id=474)
    public static final String A_zimbraReverseProxyMailHostAttribute = "zimbraReverseProxyMailHostAttribute";

    /**
     * LDAP query to find a user
     */
    @ZAttr(id=472)
    public static final String A_zimbraReverseProxyMailHostQuery = "zimbraReverseProxyMailHostQuery";

    /**
     * search base for zimbraReverseProxyMailHostQuery
     */
    @ZAttr(id=473)
    public static final String A_zimbraReverseProxyMailHostSearchBase = "zimbraReverseProxyMailHostSearchBase";

    /**
     * whether to run proxy in HTTP, HTTPS, both, mixed, or redirect mode.
     * See also related attributes zimbraMailProxyPort and
     * zimbraMailSSLProxyPort
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=685)
    public static final String A_zimbraReverseProxyMailMode = "zimbraReverseProxyMailMode";

    /**
     * whether NGINX mail proxy will pass upstream server errors back to the
     * downstream email clients
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=736)
    public static final String A_zimbraReverseProxyPassErrors = "zimbraReverseProxyPassErrors";

    /**
     * NGINX reverse proxy pop3 capabilities
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=721)
    public static final String A_zimbraReverseProxyPop3EnabledCapability = "zimbraReverseProxyPop3EnabledCapability";

    /**
     * Whether to expose version on Proxy POP3 banner
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=712)
    public static final String A_zimbraReverseProxyPop3ExposeVersionOnBanner = "zimbraReverseProxyPop3ExposeVersionOnBanner";

    /**
     * attribute that contains pop3 bind port
     */
    @ZAttr(id=477)
    public static final String A_zimbraReverseProxyPop3PortAttribute = "zimbraReverseProxyPop3PortAttribute";

    /**
     * whether POP3 SASL GSSAPI is enabled for reverse proxy
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=644)
    public static final String A_zimbraReverseProxyPop3SaslGssapiEnabled = "zimbraReverseProxyPop3SaslGssapiEnabled";

    /**
     * whether POP3 SASL PLAIN is enabled for reverse proxy
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=729)
    public static final String A_zimbraReverseProxyPop3SaslPlainEnabled = "zimbraReverseProxyPop3SaslPlainEnabled";

    /**
     * attribute that contains pop3 bind port for SSL
     */
    @ZAttr(id=478)
    public static final String A_zimbraReverseProxyPop3SSLPortAttribute = "zimbraReverseProxyPop3SSLPortAttribute";

    /**
     * on - on the plain POP/IMAP port, starttls is allowed off - no starttls
     * is offered on plain port only - you have to use starttls before clear
     * text login
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=642)
    public static final String A_zimbraReverseProxyPop3StartTlsMode = "zimbraReverseProxyPop3StartTlsMode";

    /**
     * LDAP query to find server object
     */
    @ZAttr(id=475)
    public static final String A_zimbraReverseProxyPortQuery = "zimbraReverseProxyPortQuery";

    /**
     * search base for zimbraReverseProxyPortQuery
     */
    @ZAttr(id=476)
    public static final String A_zimbraReverseProxyPortSearchBase = "zimbraReverseProxyPortSearchBase";

    /**
     * Time interval after which NGINX will fail over to the next route
     * lookup handler, if a handler does not respond to the route lookup
     * request within this time. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public static final String A_zimbraReverseProxyRouteLookupTimeout = "zimbraReverseProxyRouteLookupTimeout";

    /**
     * Time interval (ms) given to mail route lookup handler to cache a
     * failed response to route a previous lookup request (after this time
     * elapses, Proxy retries this host). Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=778)
    public static final String A_zimbraReverseProxyRouteLookupTimeoutCache = "zimbraReverseProxyRouteLookupTimeoutCache";

    /**
     * whether nginx should send ID command for imap
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=588)
    public static final String A_zimbraReverseProxySendImapId = "zimbraReverseProxySendImapId";

    /**
     * whether nginx should send XOIP command for pop3
     *
     * @since ZCS 5.0.1
     */
    @ZAttr(id=587)
    public static final String A_zimbraReverseProxySendPop3Xoip = "zimbraReverseProxySendPop3Xoip";

    /**
     * permitted ciphers for reverse proxy. Ciphers are in the formats
     * supported by OpenSSL e.g.
     * ALL:!ADH:!EXPORT56:RC4+RSA:+HIGH:+MEDIUM:+LOW:+SSLv2:+EXP; if not set,
     * default ciphers permitted by nginx will apply
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=640)
    public static final String A_zimbraReverseProxySSLCiphers = "zimbraReverseProxySSLCiphers";

    /**
     * If set as TRUE, proxy will use SSL to connect to the upstream mail
     * servers for web and mail proxy. Note admin console proxy always use
     * https no matter how this attr is set.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1360)
    public static final String A_zimbraReverseProxySSLToUpstreamEnabled = "zimbraReverseProxySSLToUpstreamEnabled";

    /**
     * The read timeout for long polling support by proxy, e.g. ActiveSync
     * for mobile devices. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 7.1.4
     */
    @ZAttr(id=1337)
    public static final String A_zimbraReverseProxyUpstreamPollingTimeout = "zimbraReverseProxyUpstreamPollingTimeout";

    /**
     * The read timeout for the response of upstream server, which determines
     * how long nginx will wait to get the response to a request. . Must be
     * in valid duration format: {digits}{time-unit}. digits: 0-9, time-unit:
     * [hmsd]|ms. h - hours, m - minutes, s - seconds, d - days, ms -
     * milliseconds. If time unit is not specified, the default is
     * s(seconds).
     *
     * @since ZCS 7.1.4
     */
    @ZAttr(id=1335)
    public static final String A_zimbraReverseProxyUpstreamReadTimeout = "zimbraReverseProxyUpstreamReadTimeout";

    /**
     * The send timeout of transfering a request to the upstream server. If
     * after this time the upstream server doesn&#039;t take new data, proxy
     * will close the connection. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 7.1.4
     */
    @ZAttr(id=1336)
    public static final String A_zimbraReverseProxyUpstreamSendTimeout = "zimbraReverseProxyUpstreamSendTimeout";

    /**
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup servlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public static final String A_zimbraReverseProxyUseExternalRoute = "zimbraReverseProxyUseExternalRoute";

    /**
     * Use external route configured on domain if account cannot be found.
     * Also see zimbraReverseProxyUseExternalRoute.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1132)
    public static final String A_zimbraReverseProxyUseExternalRouteIfAccountNotExist = "zimbraReverseProxyUseExternalRouteIfAccountNotExist";

    /**
     * Limit how many times a user can login via the proxy. Setting limit to
     * 100 and time to 3600 means: allow maximum 100 logins per hour for any
     * user. As with the ip counterparts, the user hit counter and timeout
     * are cumulative for all protocols. Also, for a given users login, both
     * counters are checked in succession, with the IP counter being checked
     * first. A login may be rejected (throttled) because the IP is
     * over-usage, or because the login name itself is over-usage. A value of
     * 0 indicates that no throttling will take place for any user.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=624)
    public static final String A_zimbraReverseProxyUserLoginLimit = "zimbraReverseProxyUserLoginLimit";

    /**
     * Sets the time-to-live for the hit counter for per user login
     * throttling.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=625)
    public static final String A_zimbraReverseProxyUserLoginLimitTime = "zimbraReverseProxyUserLoginLimitTime";

    /**
     * LDAP attribute that contains user name for the principal
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=572)
    public static final String A_zimbraReverseProxyUserNameAttribute = "zimbraReverseProxyUserNameAttribute";

    /**
     * The error message with which a login attempt by a user will be
     * throttled, if the attempt count exceeds the configured limit
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=726)
    public static final String A_zimbraReverseProxyUserThrottleMsg = "zimbraReverseProxyUserThrottleMsg";

    /**
     * Maximum number of connections that an NGINX Proxy worker process is
     * allowed to handle
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=725)
    public static final String A_zimbraReverseProxyWorkerConnections = "zimbraReverseProxyWorkerConnections";

    /**
     * Number of worker processes of NGINX Proxy
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=724)
    public static final String A_zimbraReverseProxyWorkerProcesses = "zimbraReverseProxyWorkerProcesses";

    /**
     * whether TLS is required for IMAP/POP GSSAPI auth
     *
     * @since ZCS 5.0.20
     */
    @ZAttr(id=1068)
    public static final String A_zimbraSaslGssapiRequiresTls = "zimbraSaslGssapiRequiresTls";

    /**
     * Maximum number of scheduled tasks that can run simultaneously.
     */
    @ZAttr(id=522)
    public static final String A_zimbraScheduledTaskNumThreads = "zimbraScheduledTaskNumThreads";

    /**
     * Object classes to add when creating a zimbra server object.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=756)
    public static final String A_zimbraServerExtraObjectClass = "zimbraServerExtraObjectClass";

    /**
     * ZimbraID of the server that this component is running on
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=742)
    public static final String A_zimbraServerId = "zimbraServerId";

    /**
     * Deprecated since: 5.0. deprecated in favor of the serverInherited
     * flag. Orig desc: zimbraServer attrs that get inherited from global
     * config
     */
    @ZAttr(id=62)
    public static final String A_zimbraServerInheritedAttr = "zimbraServerInheritedAttr";

    /**
     * services that are enabled on this server
     */
    @ZAttr(id=220)
    public static final String A_zimbraServiceEnabled = "zimbraServiceEnabled";

    /**
     * public hostname of the host
     */
    @ZAttr(id=65)
    public static final String A_zimbraServiceHostname = "zimbraServiceHostname";

    /**
     * services that are installed on this server
     */
    @ZAttr(id=221)
    public static final String A_zimbraServiceInstalled = "zimbraServiceInstalled";

    /**
     * All items an account has shared
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1219)
    public static final String A_zimbraSharedItem = "zimbraSharedItem";

    /**
     * Deprecated since: 8.0.0. Manual publishing of shares by admin is no
     * longer required since now automated publishing of sharing info updates
     * to LDAP is supported. Orig desc: items an account or group has shared
     */
    @ZAttr(id=357)
    public static final String A_zimbraShareInfo = "zimbraShareInfo";

    /**
     * Maximum allowed lifetime of shares to internal users or groups. A
     * value of 0 indicates that there&#039;s no limit on an internal
     * share&#039;s lifetime. . Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1348)
    public static final String A_zimbraShareLifetime = "zimbraShareLifetime";

    /**
     * Account name for authenticating to share notification MTA.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1343)
    public static final String A_zimbraShareNotificationMtaAuthAccount = "zimbraShareNotificationMtaAuthAccount";

    /**
     * Password for authenticating to share notification MTA.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1344)
    public static final String A_zimbraShareNotificationMtaAuthPassword = "zimbraShareNotificationMtaAuthPassword";

    /**
     * Whether to use credential to authenticate to share notification MTA.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1346)
    public static final String A_zimbraShareNotificationMtaAuthRequired = "zimbraShareNotificationMtaAuthRequired";

    /**
     * Connection mode when connecting to share notification MTA.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1345)
    public static final String A_zimbraShareNotificationMtaConnectionType = "zimbraShareNotificationMtaConnectionType";

    /**
     * Whether share notification MTA is enabled.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1361)
    public static final String A_zimbraShareNotificationMtaEnabled = "zimbraShareNotificationMtaEnabled";

    /**
     * SMTP hostname for share notification MTA used for sending email
     * notifications.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1341)
    public static final String A_zimbraShareNotificationMtaHostname = "zimbraShareNotificationMtaHostname";

    /**
     * SMTP port for share notification MTA used for sending email
     * notifications.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1342)
    public static final String A_zimbraShareNotificationMtaPort = "zimbraShareNotificationMtaPort";

    /**
     * Interval between successive executions of the task that publishes
     * shared item updates to LDAP. Must be in valid duration format:
     * {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h - hours, m -
     * minutes, s - seconds, d - days, ms - milliseconds. If time unit is not
     * specified, the default is s(seconds).
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1220)
    public static final String A_zimbraSharingUpdatePublishInterval = "zimbraSharingUpdatePublishInterval";

    /**
     * Unique ID for an signature
     */
    @ZAttr(id=490)
    public static final String A_zimbraSignatureId = "zimbraSignatureId";

    /**
     * maximum number of signatures allowed on an account
     */
    @ZAttr(id=493)
    public static final String A_zimbraSignatureMaxNumEntries = "zimbraSignatureMaxNumEntries";

    /**
     * minimum number of signatures allowed on an account, this is only used
     * in the client
     */
    @ZAttr(id=523)
    public static final String A_zimbraSignatureMinNumEntries = "zimbraSignatureMinNumEntries";

    /**
     * name of the signature
     */
    @ZAttr(id=491)
    public static final String A_zimbraSignatureName = "zimbraSignatureName";

    /**
     * background color for chameleon skin for the domain
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=648)
    public static final String A_zimbraSkinBackgroundColor = "zimbraSkinBackgroundColor";

    /**
     * favicon for chameleon skin for the domain
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=800)
    public static final String A_zimbraSkinFavicon = "zimbraSkinFavicon";

    /**
     * foreground color for chameleon skin for the domain
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=647)
    public static final String A_zimbraSkinForegroundColor = "zimbraSkinForegroundColor";

    /**
     * logo app banner for chameleon skin for the domain
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=671)
    public static final String A_zimbraSkinLogoAppBanner = "zimbraSkinLogoAppBanner";

    /**
     * logo login banner for chameleon skin for the domain
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=670)
    public static final String A_zimbraSkinLogoLoginBanner = "zimbraSkinLogoLoginBanner";

    /**
     * Logo URL for chameleon skin for the domain
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=649)
    public static final String A_zimbraSkinLogoURL = "zimbraSkinLogoURL";

    /**
     * secondary color for chameleon skin for the domain
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=668)
    public static final String A_zimbraSkinSecondaryColor = "zimbraSkinSecondaryColor";

    /**
     * selection color for chameleon skin for the domain
     *
     * @since ZCS 5.0.7
     */
    @ZAttr(id=669)
    public static final String A_zimbraSkinSelectionColor = "zimbraSkinSelectionColor";

    /**
     * LDAP attribute(s) for public key lookup for S/MIME via external LDAP.
     * Multiple attributes can be separated by comma. All SMIME attributes
     * are in the format of {config-name}:{value}. A &#039;SMIME config&#039;
     * is a set of SMIME attribute values with the same {config-name}.
     * Multiple SMIME configs can be configured on a domain or on
     * globalconfig. Note: SMIME attributes on domains do not inherited
     * values from globalconfig, they are not domain-inherited attributes.
     * During SMIME public key lookup, if there are any SMIME config on the
     * domain of the account, they are used. SMIME configs on globalconfig
     * will be used only when there is no SMIME config on the domain. SMIME
     * attributes cannot be modified directly with zmprov md/mcf commands.
     * Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1182)
    public static final String A_zimbraSMIMELdapAttribute = "zimbraSMIMELdapAttribute";

    /**
     * LDAP bind DN for public key lookup for S/MIME via external LDAP. Can
     * be empty for anonymous bind. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1178)
    public static final String A_zimbraSMIMELdapBindDn = "zimbraSMIMELdapBindDn";

    /**
     * LDAP bind password for public key lookup for S/MIME via external LDAP.
     * Can be empty for anonymous bind. All SMIME attributes are in the
     * format of {config-name}:{value}. A &#039;SMIME config&#039; is a set
     * of SMIME attribute values with the same {config-name}. Multiple SMIME
     * configs can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1179)
    public static final String A_zimbraSMIMELdapBindPassword = "zimbraSMIMELdapBindPassword";

    /**
     * Whether or not to discover search base DNs if
     * zimbraSMIMELdapSearchBase is not set. Allowed values are TRUE or
     * FALSE. If zimbraSMIMELdapSearchBase is set for a config, this
     * attribute is ignored for the config. If not set, default for the
     * config is FALSE. In that case, if zimbraSMIMELdapSearchBase is not
     * set, the search will default to the rootDSE. If multiple DNs are
     * discovered, the ldap search will use them one by one until a hit is
     * returned. All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @since ZCS 7.1.1
     */
    @ZAttr(id=1209)
    public static final String A_zimbraSMIMELdapDiscoverSearchBaseEnabled = "zimbraSMIMELdapDiscoverSearchBaseEnabled";

    /**
     * LDAP search filter for public key lookup for S/MIME via external LDAP.
     * Can contain the following conversion variables for expansion: %n -
     * search key with @ (or without, if no @ was specified) %u - with @
     * removed e.g. (mail=%n) All SMIME attributes are in the format of
     * {config-name}:{value}. A &#039;SMIME config&#039; is a set of SMIME
     * attribute values with the same {config-name}. Multiple SMIME configs
     * can be configured on a domain or on globalconfig. Note: SMIME
     * attributes on domains do not inherited values from globalconfig, they
     * are not domain-inherited attributes. During SMIME public key lookup,
     * if there are any SMIME config on the domain of the account, they are
     * used. SMIME configs on globalconfig will be used only when there is no
     * SMIME config on the domain. SMIME attributes cannot be modified
     * directly with zmprov md/mcf commands. Use zmprov
     * gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1181)
    public static final String A_zimbraSMIMELdapFilter = "zimbraSMIMELdapFilter";

    /**
     * LDAP search base for public key lookup for S/MIME via external LDAP.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1180)
    public static final String A_zimbraSMIMELdapSearchBase = "zimbraSMIMELdapSearchBase";

    /**
     * Whether to use startTLS for public key lookup for S/MIME via external
     * LDAP. All SMIME attributes are in the format of {config-name}:{value}.
     * A &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1177)
    public static final String A_zimbraSMIMELdapStartTlsEnabled = "zimbraSMIMELdapStartTlsEnabled";

    /**
     * LDAP URL(s) for public key lookup for S/MIME via external LDAP.
     * Multiple URLs for error fallback purpose can be separated by space.
     * All SMIME attributes are in the format of {config-name}:{value}. A
     * &#039;SMIME config&#039; is a set of SMIME attribute values with the
     * same {config-name}. Multiple SMIME configs can be configured on a
     * domain or on globalconfig. Note: SMIME attributes on domains do not
     * inherited values from globalconfig, they are not domain-inherited
     * attributes. During SMIME public key lookup, if there are any SMIME
     * config on the domain of the account, they are used. SMIME configs on
     * globalconfig will be used only when there is no SMIME config on the
     * domain. SMIME attributes cannot be modified directly with zmprov
     * md/mcf commands. Use zmprov gcsc/gdsc/mcsc/mdsc/rcsc/rdsc command
     * instead.
     *
     * @since ZCS 7.1.0
     */
    @ZAttr(id=1176)
    public static final String A_zimbraSMIMELdapURL = "zimbraSMIMELdapURL";

    /**
     * Whether to enable smtp debug trace
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=793)
    public static final String A_zimbraSmtpEnableTrace = "zimbraSmtpEnableTrace";

    /**
     * the SMTP server to connect to when sending mail
     */
    @ZAttr(id=97)
    public static final String A_zimbraSmtpHostname = "zimbraSmtpHostname";

    /**
     * the SMTP server port to connect to when sending mail
     */
    @ZAttr(id=98)
    public static final String A_zimbraSmtpPort = "zimbraSmtpPort";

    /**
     * If TRUE, the address for MAIL FROM in the SMTP session will always be
     * set to the email address of the account. If FALSE, the address will be
     * the value of the Sender or From header in the outgoing message, in
     * that order.
     *
     * @since ZCS 6.0.5
     */
    @ZAttr(id=1077)
    public static final String A_zimbraSmtpRestrictEnvelopeFrom = "zimbraSmtpRestrictEnvelopeFrom";

    /**
     * If true, an X-Authenticated-User header will be added to messages sent
     * via SendMsgRequest.
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=747)
    public static final String A_zimbraSmtpSendAddAuthenticatedUser = "zimbraSmtpSendAddAuthenticatedUser";

    /**
     * Whether X-Mailer will be added to messages sent by Zimbra
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=636)
    public static final String A_zimbraSmtpSendAddMailer = "zimbraSmtpSendAddMailer";

    /**
     * Whether X-Originating-IP will be added to messages sent via
     * SendMsgRequest.
     */
    @ZAttr(id=435)
    public static final String A_zimbraSmtpSendAddOriginatingIP = "zimbraSmtpSendAddOriginatingIP";

    /**
     * Value of the mail.smtp.sendpartial property
     */
    @ZAttr(id=249)
    public static final String A_zimbraSmtpSendPartial = "zimbraSmtpSendPartial";

    /**
     * timeout value in seconds
     */
    @ZAttr(id=99)
    public static final String A_zimbraSmtpTimeout = "zimbraSmtpTimeout";

    /**
     * If TRUE, enables support for GetVersionInfo for account SOAP requests.
     * If FALSE, GetVersionInfoRequest returns a SOAP fault.
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=708)
    public static final String A_zimbraSoapExposeVersion = "zimbraSoapExposeVersion";

    /**
     * Maximum size in bytes for incoming SOAP requests. 0 means no limit.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=557)
    public static final String A_zimbraSoapRequestMaxSize = "zimbraSoapRequestMaxSize";

    /**
     * If TRUE, spam messages will be affected by user mail filters instead
     * of being automatically filed into the Junk folder. This attribute is
     * deprecated and will be removed in a future release. See bug 23886 for
     * details.
     *
     * @since ZCS 5.0.2
     */
    @ZAttr(id=604)
    public static final String A_zimbraSpamApplyUserFilters = "zimbraSpamApplyUserFilters";

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraServiceEnabled.
     * Orig desc: Whether to enable spam checking
     */
    @ZAttr(id=201)
    public static final String A_zimbraSpamCheckEnabled = "zimbraSpamCheckEnabled";

    /**
     * mail header name for flagging spam
     */
    @ZAttr(id=210)
    public static final String A_zimbraSpamHeader = "zimbraSpamHeader";

    /**
     * regular expression for matching the spam header
     */
    @ZAttr(id=211)
    public static final String A_zimbraSpamHeaderValue = "zimbraSpamHeaderValue";

    /**
     * When user classifies a message as not spam forward message via SMTP to
     * this account
     */
    @ZAttr(id=245)
    public static final String A_zimbraSpamIsNotSpamAccount = "zimbraSpamIsNotSpamAccount";

    /**
     * When user classifies a message as spam forward message via SMTP to
     * this account
     */
    @ZAttr(id=244)
    public static final String A_zimbraSpamIsSpamAccount = "zimbraSpamIsSpamAccount";

    /**
     * Spaminess percentage beyond which a message is dropped
     */
    @ZAttr(id=202)
    public static final String A_zimbraSpamKillPercent = "zimbraSpamKillPercent";

    /**
     * value for envelope from (MAIL FROM) in spam report
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1049)
    public static final String A_zimbraSpamReportEnvelopeFrom = "zimbraSpamReportEnvelopeFrom";

    /**
     * mail header name for sender in spam report
     */
    @ZAttr(id=465)
    public static final String A_zimbraSpamReportSenderHeader = "zimbraSpamReportSenderHeader";

    /**
     * spam report type value for ham
     */
    @ZAttr(id=468)
    public static final String A_zimbraSpamReportTypeHam = "zimbraSpamReportTypeHam";

    /**
     * mail header name for report type in spam report
     */
    @ZAttr(id=466)
    public static final String A_zimbraSpamReportTypeHeader = "zimbraSpamReportTypeHeader";

    /**
     * spam report type value for spam
     */
    @ZAttr(id=467)
    public static final String A_zimbraSpamReportTypeSpam = "zimbraSpamReportTypeSpam";

    /**
     * Subject prefix for spam messages
     */
    @ZAttr(id=203)
    public static final String A_zimbraSpamSubjectTag = "zimbraSpamSubjectTag";

    /**
     * Spaminess percentage beyond which a message is marked as spam
     */
    @ZAttr(id=204)
    public static final String A_zimbraSpamTagPercent = "zimbraSpamTagPercent";

    /**
     * Aliases of Trash folder. In case some IMAP clients use different
     * folder names other than Trash, the spam filter still special-cases
     * those folders as if they are Trash.
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1167)
    public static final String A_zimbraSpamTrashAlias = "zimbraSpamTrashAlias";

    /**
     * Mail header name for flagging a message as not spam. If set, this
     * takes precedence over zimbraSpamHeader.
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1257)
    public static final String A_zimbraSpamWhitelistHeader = "zimbraSpamWhitelistHeader";

    /**
     * regular expression for matching the value of zimbraSpamWhitelistHeader
     * for flagging a message as not spam
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1258)
    public static final String A_zimbraSpamWhitelistHeaderValue = "zimbraSpamWhitelistHeaderValue";

    /**
     * The list of available dictionaries that can be used for spell
     * checking.
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1042)
    public static final String A_zimbraSpellAvailableDictionary = "zimbraSpellAvailableDictionary";

    /**
     * URL of the server running the spell checking service. Multi-valued
     * attribute that allows multiple spell check servers to be specified. If
     * the request to the first server fails, a request to the second server
     * is sent and so on.
     */
    @ZAttr(id=267)
    public static final String A_zimbraSpellCheckURL = "zimbraSpellCheckURL";

    /**
     * whether spnego SSO is enabled
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1118)
    public static final String A_zimbraSpnegoAuthEnabled = "zimbraSpnegoAuthEnabled";

    /**
     * spnego auth error URL
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1124)
    public static final String A_zimbraSpnegoAuthErrorURL = "zimbraSpnegoAuthErrorURL";

    /**
     * spnego auth principal
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1122)
    public static final String A_zimbraSpnegoAuthPrincipal = "zimbraSpnegoAuthPrincipal";

    /**
     * spnego auth realm
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1119)
    public static final String A_zimbraSpnegoAuthRealm = "zimbraSpnegoAuthRealm";

    /**
     * spnego auth target name
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1123)
    public static final String A_zimbraSpnegoAuthTargetName = "zimbraSpnegoAuthTargetName";

    /**
     * Public key of this server, used by other hosts to authorize this
     * server to login.
     */
    @ZAttr(id=262)
    public static final String A_zimbraSshPublicKey = "zimbraSshPublicKey";

    /**
     * CA Cert used to sign all self signed certs
     */
    @ZAttr(id=277)
    public static final String A_zimbraSslCaCert = "zimbraSslCaCert";

    /**
     * CA Key used to sign all self signed certs
     */
    @ZAttr(id=278)
    public static final String A_zimbraSslCaKey = "zimbraSslCaKey";

    /**
     * SSL certificate
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=563)
    public static final String A_zimbraSSLCertificate = "zimbraSSLCertificate";

    /**
     * excluded cipher suites
     *
     * @since ZCS 5.0.5
     */
    @ZAttr(id=639)
    public static final String A_zimbraSSLExcludeCipherSuites = "zimbraSSLExcludeCipherSuites";

    /**
     * SSL private key
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=564)
    public static final String A_zimbraSSLPrivateKey = "zimbraSSLPrivateKey";

    /**
     * description of the custom tab in the Preferences page in HTML client
     * in the format {tab-name},{associated-URL}
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1267)
    public static final String A_zimbraStandardClientCustomPrefTab = "zimbraStandardClientCustomPrefTab";

    /**
     * whether extra custom tabs in the Preferences page in HTML client are
     * enabled
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1266)
    public static final String A_zimbraStandardClientCustomPrefTabsEnabled = "zimbraStandardClientCustomPrefTabsEnabled";

    /**
     * Prefixes of thread names. Each value is a column in threads.csv that
     * tracks the number of threads whose name starts with the given prefix.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=792)
    public static final String A_zimbraStatThreadNamePrefix = "zimbraStatThreadNamePrefix";

    /**
     * The maximum batch size for each ZimbraSync transaction. Default value
     * of 0 means to follow client requested size. If set to any positive
     * integer, the value will be the maximum number of items to sync even if
     * client requests more. This setting affects all sync categories
     * including email, contacts, calendar and tasks.
     */
    @ZAttr(id=437)
    public static final String A_zimbraSyncWindowSize = "zimbraSyncWindowSize";

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance will be performed if the
     * number of rows grows by this factor
     */
    @ZAttr(id=171)
    public static final String A_zimbraTableMaintenanceGrowthFactor = "zimbraTableMaintenanceGrowthFactor";

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: maximum number of rows required for database
     * table maintenance
     */
    @ZAttr(id=169)
    public static final String A_zimbraTableMaintenanceMaxRows = "zimbraTableMaintenanceMaxRows";

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: minimum number of rows required for database
     * table maintenance
     */
    @ZAttr(id=168)
    public static final String A_zimbraTableMaintenanceMinRows = "zimbraTableMaintenanceMinRows";

    /**
     * Deprecated since: 4.5.7. We now maintain all tables unconditionally.
     * See bug 19145. Orig desc: table maintenance operation that will be
     * performed. Valid options: &quot;ANALYZE&quot;, &quot;OPTIMIZE&quot;
     */
    @ZAttr(id=170)
    public static final String A_zimbraTableMaintenanceOperation = "zimbraTableMaintenanceOperation";

    /**
     * The registered name of the Zimbra Analyzer Extension for this account
     * to use
     */
    @ZAttr(id=393)
    public static final String A_zimbraTextAnalyzer = "zimbraTextAnalyzer";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc: Start
     * date for daylight time
     */
    @ZAttr(id=232)
    public static final String A_zimbraTimeZoneDaylightDtStart = "zimbraTimeZoneDaylightDtStart";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc: Offset
     * in daylight time
     */
    @ZAttr(id=233)
    public static final String A_zimbraTimeZoneDaylightOffset = "zimbraTimeZoneDaylightOffset";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc:
     * iCalendar recurrence rule for onset of daylight time
     */
    @ZAttr(id=234)
    public static final String A_zimbraTimeZoneDaylightRRule = "zimbraTimeZoneDaylightRRule";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc: Start
     * date for standard time
     */
    @ZAttr(id=229)
    public static final String A_zimbraTimeZoneStandardDtStart = "zimbraTimeZoneStandardDtStart";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc: Offset
     * in standard time
     */
    @ZAttr(id=230)
    public static final String A_zimbraTimeZoneStandardOffset = "zimbraTimeZoneStandardOffset";

    /**
     * Deprecated since: 5.0. Deprecated as of bug 12416. Orig desc:
     * iCalendar recurrence rule for onset of standard time
     */
    @ZAttr(id=231)
    public static final String A_zimbraTimeZoneStandardRRule = "zimbraTimeZoneStandardRRule";

    /**
     * whether end-user services on SOAP and LMTP interfaces are enabled
     */
    @ZAttr(id=146)
    public static final String A_zimbraUserServicesEnabled = "zimbraUserServicesEnabled";

    /**
     * account version information
     */
    @ZAttr(id=399)
    public static final String A_zimbraVersion = "zimbraVersion";

    /**
     * an email address to send mail to if Zimbra version check detects a new
     * version. Must be in valid duration format: {digits}{time-unit}.
     * digits: 0-9, time-unit: [hmsd]|ms. h - hours, m - minutes, s -
     * seconds, d - days, ms - milliseconds. If time unit is not specified,
     * the default is s(seconds).
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1059)
    public static final String A_zimbraVersionCheckInterval = "zimbraVersionCheckInterval";

    /**
     * time Zimbra version was last checked
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1056)
    public static final String A_zimbraVersionCheckLastAttempt = "zimbraVersionCheckLastAttempt";

    /**
     * last response of last Zimbra version check. This will be a short XML
     * that will contain information about available updates.
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1058)
    public static final String A_zimbraVersionCheckLastResponse = "zimbraVersionCheckLastResponse";

    /**
     * time Zimbra version was last checked successfully
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1057)
    public static final String A_zimbraVersionCheckLastSuccess = "zimbraVersionCheckLastSuccess";

    /**
     * template used to construct the body of an Zimbra version check
     * notification message
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1066)
    public static final String A_zimbraVersionCheckNotificationBody = "zimbraVersionCheckNotificationBody";

    /**
     * email address to send mail to for the Zimbra version check
     * notification message
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1063)
    public static final String A_zimbraVersionCheckNotificationEmail = "zimbraVersionCheckNotificationEmail";

    /**
     * from address for the Zimbra version check notification message
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1064)
    public static final String A_zimbraVersionCheckNotificationEmailFrom = "zimbraVersionCheckNotificationEmailFrom";

    /**
     * template used to construct the subject of an Zimbra version check
     * notification message
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1065)
    public static final String A_zimbraVersionCheckNotificationSubject = "zimbraVersionCheckNotificationSubject";

    /**
     * whether to send a notification message if Zimbra version check detects
     * a new version
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1062)
    public static final String A_zimbraVersionCheckSendNotifications = "zimbraVersionCheckSendNotifications";

    /**
     * zimbraId of the server that should perform the Zimbra version checks
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1060)
    public static final String A_zimbraVersionCheckServer = "zimbraVersionCheckServer";

    /**
     * URL of the Zimbra version check script
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1061)
    public static final String A_zimbraVersionCheckURL = "zimbraVersionCheckURL";

    /**
     * An alias for this domain, used to determine default login domain based
     * on URL client is visiting
     */
    @ZAttr(id=352)
    public static final String A_zimbraVirtualHostname = "zimbraVirtualHostname";

    /**
     * An virtual IP address for this domain, used to determine domain based
     * on an IP address
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=562)
    public static final String A_zimbraVirtualIPAddress = "zimbraVirtualIPAddress";

    /**
     * Whether to block archive files that are password protected or
     * encrypted
     */
    @ZAttr(id=205)
    public static final String A_zimbraVirusBlockEncryptedArchive = "zimbraVirusBlockEncryptedArchive";

    /**
     * Deprecated since: 4.5. Deprecated in favor of zimbraServiceEnabled.
     * Orig desc: Whether to enable virus checking
     */
    @ZAttr(id=206)
    public static final String A_zimbraVirusCheckEnabled = "zimbraVirusCheckEnabled";

    /**
     * how often the virus definitions are updated. Must be in valid duration
     * format: {digits}{time-unit}. digits: 0-9, time-unit: [hmsd]|ms. h -
     * hours, m - minutes, s - seconds, d - days, ms - milliseconds. If time
     * unit is not specified, the default is s(seconds).
     */
    @ZAttr(id=191)
    public static final String A_zimbraVirusDefinitionsUpdateFrequency = "zimbraVirusDefinitionsUpdateFrequency";

    /**
     * Whether to email admin on virus detection
     */
    @ZAttr(id=207)
    public static final String A_zimbraVirusWarnAdmin = "zimbraVirusWarnAdmin";

    /**
     * Whether to email recipient on virus detection
     */
    @ZAttr(id=208)
    public static final String A_zimbraVirusWarnRecipient = "zimbraVirusWarnRecipient";

    /**
     * link for admin users in the web client
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=701)
    public static final String A_zimbraWebClientAdminReference = "zimbraWebClientAdminReference";

    /**
     * login URL for web client to send the user to upon failed login, auth
     * expired, or no/invalid auth
     */
    @ZAttr(id=506)
    public static final String A_zimbraWebClientLoginURL = "zimbraWebClientLoginURL";

    /**
     * regex for allowed client IP addresses for honoring
     * zimbraWebClientLoginURL. If not set, all IP addresses are allowed. If
     * multiple values are set, an IP address is allowed as long as it
     * matches any one of the values.
     *
     * @since ZCS 7.1.5
     */
    @ZAttr(id=1352)
    public static final String A_zimbraWebClientLoginURLAllowedIP = "zimbraWebClientLoginURLAllowedIP";

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLoginURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1141)
    public static final String A_zimbraWebClientLoginURLAllowedUA = "zimbraWebClientLoginURLAllowedUA";

    /**
     * logout URL for web client to send the user to upon explicit logging
     * out
     */
    @ZAttr(id=507)
    public static final String A_zimbraWebClientLogoutURL = "zimbraWebClientLogoutURL";

    /**
     * regex for allowed client IP addresses for honoring
     * zimbraWebClientLogoutURL. If not set, all IP addresses are allowed. If
     * multiple values are set, an IP address is allowed as long as it
     * matches any one of the values.
     *
     * @since ZCS 7.1.5
     */
    @ZAttr(id=1353)
    public static final String A_zimbraWebClientLogoutURLAllowedIP = "zimbraWebClientLogoutURLAllowedIP";

    /**
     * regex to be matched for allowed user agents for honoring
     * zimbraWebClientLogoutURL. If not set, all UAs are allowed. If multiple
     * values are set, an UA is allowed as long as it matches any one of the
     * values. e.g. &quot;.*Windows NT.*Firefox/3.*&quot; will match firefox
     * 3 or later browsers on Windows. &quot;.*MSIE.*Windows NT.*&quot; will
     * match IE browsers on Windows.
     *
     * @since ZCS 7.0.0
     */
    @ZAttr(id=1142)
    public static final String A_zimbraWebClientLogoutURLAllowedUA = "zimbraWebClientLogoutURLAllowedUA";

    /**
     * max input buffer length for web client
     *
     * @since ZCS 8.0.0
     */
    @ZAttr(id=1339)
    public static final String A_zimbraWebClientMaxInputBufferLength = "zimbraWebClientMaxInputBufferLength";

    /**
     * whether or not to show link to offline version in the web UI top bar
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1047)
    public static final String A_zimbraWebClientShowOfflineLink = "zimbraWebClientShowOfflineLink";

    /**
     * XMPP Category of the component
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=737)
    public static final String A_zimbraXMPPComponentCategory = "zimbraXMPPComponentCategory";

    /**
     * class name of the XMPP component
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=763)
    public static final String A_zimbraXMPPComponentClassName = "zimbraXMPPComponentClassName";

    /**
     * XMPP Type of the component
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=739)
    public static final String A_zimbraXMPPComponentFeatures = "zimbraXMPPComponentFeatures";

    /**
     * Name of the XMPP Component
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=740)
    public static final String A_zimbraXMPPComponentName = "zimbraXMPPComponentName";

    /**
     * XMPP Type of the component
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=738)
    public static final String A_zimbraXMPPComponentType = "zimbraXMPPComponentType";

    /**
     * Enable XMPP support for IM
     */
    @ZAttr(id=397)
    public static final String A_zimbraXMPPEnabled = "zimbraXMPPEnabled";

    /**
     * Shared Secret for XMPP Server Dialback Protocol
     *
     * @since ZCS 5.0.9
     */
    @ZAttr(id=695)
    public static final String A_zimbraXMPPServerDialbackKey = "zimbraXMPPServerDialbackKey";

    /**
     * Yahoo ID
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=658)
    public static final String A_zimbraYahooId = "zimbraYahooId";

    /**
     * List of Zimlets available to this COS Values can be prefixed with ! or
     * + or - !: mandatory + (or no prefix): enabled by default -: disabled
     * by default
     */
    @ZAttr(id=291)
    public static final String A_zimbraZimletAvailableZimlets = "zimbraZimletAvailableZimlets";

    /**
     * The content object section in the Zimlet description
     */
    @ZAttr(id=288)
    public static final String A_zimbraZimletContentObject = "zimbraZimletContentObject";

    /**
     * whether zimlets that send sensitive data are disabled in
     * &quot;mixed&quot; zimbraMailMode
     *
     * @since ZCS 7.1.3
     */
    @ZAttr(id=1269)
    public static final String A_zimbraZimletDataSensitiveInMixedModeDisabled = "zimbraZimletDataSensitiveInMixedModeDisabled";

    /**
     * Zimlet description
     */
    @ZAttr(id=283)
    public static final String A_zimbraZimletDescription = "zimbraZimletDescription";

    /**
     * List of Zimlets available to this domain. Zimlets available to
     * accounts in the domain is the union of account/cos attribute
     * zimbraZimletAvailableZimlets and this attribute. See
     * zimbraZimletAvailableZimlets for value format.
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=710)
    public static final String A_zimbraZimletDomainAvailableZimlets = "zimbraZimletDomainAvailableZimlets";

    /**
     * whether this Zimlet is enabled
     */
    @ZAttr(id=301)
    public static final String A_zimbraZimletEnabled = "zimbraZimletEnabled";

    /**
     * The handler class for server side Zimlet extension
     */
    @ZAttr(id=286)
    public static final String A_zimbraZimletHandlerClass = "zimbraZimletHandlerClass";

    /**
     * The global config for the Zimlet
     */
    @ZAttr(id=287)
    public static final String A_zimbraZimletHandlerConfig = "zimbraZimletHandlerConfig";

    /**
     * Whether server side keyword indexing enabled
     */
    @ZAttr(id=284)
    public static final String A_zimbraZimletIndexingEnabled = "zimbraZimletIndexingEnabled";

    /**
     * Whether this zimlet is an extension
     */
    @ZAttr(id=304)
    public static final String A_zimbraZimletIsExtension = "zimbraZimletIsExtension";

    /**
     * Server side object keyword used for indexing and search for this
     * Zimlet
     */
    @ZAttr(id=281)
    public static final String A_zimbraZimletKeyword = "zimbraZimletKeyword";

    /**
     * The panel item section in the Zimlet description
     */
    @ZAttr(id=289)
    public static final String A_zimbraZimletPanelItem = "zimbraZimletPanelItem";

    /**
     * Object match priority
     */
    @ZAttr(id=302)
    public static final String A_zimbraZimletPriority = "zimbraZimletPriority";

    /**
     * URL of extra scripts used by the Zimlet
     */
    @ZAttr(id=290)
    public static final String A_zimbraZimletScript = "zimbraZimletScript";

    /**
     * Regex of content object
     */
    @ZAttr(id=292)
    public static final String A_zimbraZimletServerIndexRegex = "zimbraZimletServerIndexRegex";

    /**
     * Whether store is matched for this type
     */
    @ZAttr(id=285)
    public static final String A_zimbraZimletStoreMatched = "zimbraZimletStoreMatched";

    /**
     * Zimlet target apps
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=606)
    public static final String A_zimbraZimletTarget = "zimbraZimletTarget";

    /**
     * User properties for Zimlets
     */
    @ZAttr(id=296)
    public static final String A_zimbraZimletUserProperties = "zimbraZimletUserProperties";

    /**
     * Version of the Zimlet
     */
    @ZAttr(id=282)
    public static final String A_zimbraZimletVersion = "zimbraZimletVersion";

    ///// END-AUTO-GEN-REPLACE
}

