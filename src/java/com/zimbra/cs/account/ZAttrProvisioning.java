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
package com.zimbra.cs.account;

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

    /* build: 6.0.2_BETA1_1111 pshao 20100601-1142 */

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
     * RFC2798: preferred name to be used when displaying entries
     */
    @ZAttr(id=-1)
    public static final String A_displayName = "displayName";

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
     * RFC2256/2307: password of user. Stored encoded as SSHA (salted-SHA1)
     */
    @ZAttr(id=-1)
    public static final String A_userPassword = "userPassword";

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
     * account status
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
     * lifetime (nnnnn[hmsd]) of newly created admin auth tokens
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
     * SSL port for admin UI
     */
    @ZAttr(id=155)
    public static final String A_zimbraAdminPort = "zimbraAdminPort";

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
     * whether or not to index attachemts
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
     * explict mapping to an external LDAP dn for a given account
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
     * mechanism to use for authentication. Valid values are zimbra, ldap,
     * ad, kerberos5, custom:{handler-name} [arg1 arg2 ...]
     */
    @ZAttr(id=42)
    public static final String A_zimbraAuthMech = "zimbraAuthMech";

    /**
     * auth token secret key
     */
    @ZAttr(id=100)
    public static final String A_zimbraAuthTokenKey = "zimbraAuthTokenKey";

    /**
     * lifetime (nnnnn[hmsd]) of newly created auth tokens
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
     * CalDAV shared folder cache duration
     *
     * @since ZCS 5.0.14
     */
    @ZAttr(id=817)
    public static final String A_zimbraCalendarCalDavSharedFolderCacheDuration = "zimbraCalendarCalDavSharedFolderCacheDuration";

    /**
     * see description of zimbraCalendarCalDavSyncStart
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
     * CalDAV.
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
     * whether calendar reasources can be double booked
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
     * Names of additonal components that have been installed
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
     * get friendly name for the email address. Use 0 to disable the refresh.
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
     * SQL statements that take longer than this duration to execute will be
     * logged to the sqltrace category in mailbox.log.
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
     * The time interval between automated data imports for a Caldav data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling.
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=788)
    public static final String A_zimbraDataSourceCaldavPollingInterval = "zimbraDataSourceCaldavPollingInterval";

    /**
     * The time interval between automated data imports for a remote calendar
     * data source. If unset or 0, the data source will not be scheduled for
     * automated polling.
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
     * automated polling.
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
     * automated polling.
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
     * automated polling.
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
     * Shortest allowed duration for zimbraDataSourcePollingInterval.
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
     * account/cos, use 0, which means no automated polling.
     */
    @ZAttr(id=455)
    public static final String A_zimbraDataSourcePollingInterval = "zimbraDataSourcePollingInterval";

    /**
     * The time interval between automated data imports for a Pop3 data
     * source. If unset or 0, the data source will not be scheduled for
     * automated polling.
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
     * automated polling.
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
     * for automated polling.
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
     * zimbraId of domain alias target
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=775)
    public static final String A_zimbraDomainAliasTargetId = "zimbraDomainAliasTargetId";

    /**
     * maximum number of accounts allowed to be assigned to specified COSes
     * in a domain
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
     * zimbraAccountStatus values: active - active lockout - no login until
     * lockout duration is over locked - no login maintenance - no login, no
     * delivery(try again, no bouncing) pending - no login, no
     * delivery(bouncing mails), Account behavior is like closed, except that
     * when the status is being set to pending, account addresses are not
     * removed from distribution lists. The use case is for hosted. New
     * account creation based on invites that are not completed until user
     * accepts TOS on account creation confirmation page. closed - no login,
     * no delivery(bouncing mails) all addresses (account main email and all
     * aliases) of the account are removed from all distribution lists.
     * zimbraDomainStatus values: all values for zimbraAccountStatus (except
     * for lockout, see mapping below) suspended - maintenance + no
     * creating/deleting/modifying accounts/DLs under the domain. shutdown -
     * suspended + cannot modify domain attrs + cannot delete the domain
     * Indicating server is doing major and lengthy maintenance work on the
     * domain, e.g. renaming the domain and moving LDAP enteries.
     * Modification and deletion of the domain can only be done internally by
     * the server when it is safe to release the domain, they cannot be done
     * in admin console or zmprov. How zimbraDomainStatus affects account
     * behavior : ------------------------------------- zimbraDomainStatus
     * account behavior ------------------------------------- active
     * zimbraAccountStatus locked zimbraAccountStatus if it is maintenance or
     * pending or closed, else locked maintenance zimbraAccountStatus if it
     * is pending or closed, else maintenance suspended zimbraAccountStatus
     * if it is pending or closed, else maintenance shutdown
     * zimbraAccountStatus if it is pending or closed, else maintenance
     * closed closed
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
     * advanced search button enabled
     */
    @ZAttr(id=138)
    public static final String A_zimbraFeatureAdvancedSearchEnabled = "zimbraFeatureAdvancedSearchEnabled";

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
     * whether import export folder feature is enabled
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=750)
    public static final String A_zimbraFeatureImportExportFolderEnabled = "zimbraFeatureImportExportFolderEnabled";

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
     * enable end-user to manage zimlets
     *
     * @since ZCS 6.0.2
     */
    @ZAttr(id=1051)
    public static final String A_zimbraFeatureManageZimlets = "zimbraFeatureManageZimlets";

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
     * Maximum size in bytes for attachments
     */
    @ZAttr(id=227)
    public static final String A_zimbraFileUploadMaxSize = "zimbraFileUploadMaxSize";

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
     * The duration of f/b block pushed to Exchange server.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=621)
    public static final String A_zimbraFreebusyExchangeCachedInterval = "zimbraFreebusyExchangeCachedInterval";

    /**
     * The value of duration is used to indicate the start date (in the past
     * relative to today) of the f/b interval pushed to Exchange server.
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=620)
    public static final String A_zimbraFreebusyExchangeCachedIntervalStart = "zimbraFreebusyExchangeCachedIntervalStart";

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
     * such as Exchange.
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
     * LDAP search base for interal GAL queries (special values:
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
     * for GAL. This apples to both Zimbra and external LDAP servers. A value
     * of 0 means paging is not enabled.
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
     * for GAL sync. This apples to both Zimbra and external LDAP servers. A
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
     * data will be moved to secondary storage (nnnnn[hmsd]).
     */
    @ZAttr(id=8)
    public static final String A_zimbraHsmAge = "zimbraHsmAge";

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
     * whether POP3 SASL GSSAPI is enabled for a given server
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=555)
    public static final String A_zimbraImapSaslGssapiEnabled = "zimbraImapSaslGssapiEnabled";

    /**
     * whether IMAP server is enabled for a given server
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
     * true if this server is the monitor host
     */
    @ZAttr(id=132)
    public static final String A_zimbraIsMonitorHost = "zimbraIsMonitorHost";

    /**
     * Indicates the account is a resource used by the system such as spam
     * accounts or Notebook accounts.
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
     * how often (nnnnn[hmsd]) the zimbraLastLogonTimestamp is updated. if
     * set to 0, updating zimbraLastLogonTimestamp is completely disabled
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
     * lifetime (nnnnn[hmsd]) of raw log rows in consolidated logger tables
     */
    @ZAttr(id=263)
    public static final String A_zimbraLogRawLifetime = "zimbraLogRawLifetime";

    /**
     * lifetime (nnnnn[hmsd]) of summarized log rows in consolidated logger
     * tables
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
     * RFC822 email address of this recipient for accepting mail
     */
    @ZAttr(id=20)
    public static final String A_zimbraMailAlias = "zimbraMailAlias";

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
     * the server hosting the accounts mailbox
     */
    @ZAttr(id=4)
    public static final String A_zimbraMailHost = "zimbraMailHost";

    /**
     * servers that an account can be initially provisioned on
     */
    @ZAttr(id=125)
    public static final String A_zimbraMailHostPool = "zimbraMailHostPool";

    /**
     * idle timeout (nnnnn[hmsd])
     */
    @ZAttr(id=147)
    public static final String A_zimbraMailIdleSessionTimeout = "zimbraMailIdleSessionTimeout";

    /**
     * The id of the last purged mailbox.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=543)
    public static final String A_zimbraMailLastPurgedMailboxId = "zimbraMailLastPurgedMailboxId";

    /**
     * lifetime (nnnnn[hmsd]) of a mail message regardless of location
     */
    @ZAttr(id=106)
    public static final String A_zimbraMailMessageLifetime = "zimbraMailMessageLifetime";

    /**
     * minimum allowed value for zimbraPrefMailPollingInterval (nnnnn[hmsd])
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
     * HTTP port for end-user UI
     */
    @ZAttr(id=154)
    public static final String A_zimbraMailPort = "zimbraMailPort";

    /**
     * HTTP proxy port
     *
     * @since ZCS 5.0.3
     */
    @ZAttr(id=626)
    public static final String A_zimbraMailProxyPort = "zimbraMailProxyPort";

    /**
     * Sleep time between subsequent mailbox purges. 0 means that mailbox
     * purging is disabled.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=542)
    public static final String A_zimbraMailPurgeSleepInterval = "zimbraMailPurgeSleepInterval";

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
     * shorter duration is used.
     */
    @ZAttr(id=105)
    public static final String A_zimbraMailSpamLifetime = "zimbraMailSpamLifetime";

    /**
     * SSL port for end-user UI
     */
    @ZAttr(id=166)
    public static final String A_zimbraMailSSLPort = "zimbraMailSSLPort";

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
     * where to deliver parameter for use in postfix transport_maps
     */
    @ZAttr(id=247)
    public static final String A_zimbraMailTransport = "zimbraMailTransport";

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This admin-modifiable attribute works in
     * conjunction with zimbraPrefTrashLifetime, which is user-modifiable.
     * The shorter duration is used.
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
     * Deprecated since: 3.2.0. greatly simplify dl/group model. Orig desc:
     * for group membership, included with person object
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
     * Size of cache for delivery time dedupe based on Message-Id header. Set
     * to 0 to disable this type of deduping.
     */
    @ZAttr(id=334)
    public static final String A_zimbraMessageIdDedupeCacheSize = "zimbraMessageIdDedupeCacheSize";

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
     * whether to allow simple password; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=839)
    public static final String A_zimbraMobilePolicyAllowSimpleDevicePassword = "zimbraMobilePolicyAllowSimpleDevicePassword";

    /**
     * whether to require alpha-numeric password as device pin; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=840)
    public static final String A_zimbraMobilePolicyAlphanumericDevicePasswordRequired = "zimbraMobilePolicyAlphanumericDevicePasswordRequired";

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
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=842)
    public static final String A_zimbraMobilePolicyDevicePasswordExpiration = "zimbraMobilePolicyDevicePasswordExpiration";

    /**
     * number of previously used password stored in history; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE or
     * zimbraMobilePolicyDevicePasswordExpiration=0
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=843)
    public static final String A_zimbraMobilePolicyDevicePasswordHistory = "zimbraMobilePolicyDevicePasswordHistory";

    /**
     * number of consecutive incorrect pin input before device is wiped;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=845)
    public static final String A_zimbraMobilePolicyMaxDevicePasswordFailedAttempts = "zimbraMobilePolicyMaxDevicePasswordFailedAttempts";

    /**
     * max idle time in minutes before device is locked; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=844)
    public static final String A_zimbraMobilePolicyMaxInactivityTimeDeviceLock = "zimbraMobilePolicyMaxInactivityTimeDeviceLock";

    /**
     * least number of complex characters must be included in device pin;
     * ignored if zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=841)
    public static final String A_zimbraMobilePolicyMinDevicePasswordComplexCharacters = "zimbraMobilePolicyMinDevicePasswordComplexCharacters";

    /**
     * min length for device pin; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=838)
    public static final String A_zimbraMobilePolicyMinDevicePasswordLength = "zimbraMobilePolicyMinDevicePasswordLength";

    /**
     * support device pin recovery; ignored if
     * zimbraFeatureMobilePolicyEnabled=FALSE or
     * zimbraMobileDevicePasswordEnabled=FALSE
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
     * Value for postconf relayhost
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
     * Account for storing templates and providing space for public wiki
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
     * admin resets it
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
     * occurred
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
     * whether IMAP is enabled for a server
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
     * whether to end auto-complete on comma
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1091)
    public static final String A_zimbraPrefAutoCompleteQuickCompletionOnComma = "zimbraPrefAutoCompleteQuickCompletionOnComma";

    /**
     * time to wait before auto saving a draft(nnnnn[hmsd])
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
     * automatically add appointments when invited
     *
     * @since ZCS 6.0.0_BETA1
     */
    @ZAttr(id=848)
    public static final String A_zimbraPrefCalendarAutoAddInvites = "zimbraPrefCalendarAutoAddInvites";

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
     * Deprecated since: 6.0.0_BETA1. was added for Yahoo calendar, no longer
     * used. Orig desc: The email the reminder goes to.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=575)
    public static final String A_zimbraPrefCalendarReminderEmail = "zimbraPrefCalendarReminderEmail";

    /**
     * Flash title when on appointment remimnder notification
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
     * used. Orig desc: To send email or to not send email is the question.
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
     * whether to pop-up reminder for past due appointments in the UI
     *
     * @since ZCS 6.0.0_BETA2
     */
    @ZAttr(id=1022)
    public static final String A_zimbraPrefCalendarShowPastDueReminders = "zimbraPrefCalendarShowPastDueReminders";

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
     * Disables autocomplete matching against the members email address.
     *
     * @since ZCS 6.0.7
     */
    @ZAttr(id=1090)
    public static final String A_zimbraPrefContactsDisableAutocompleteOnContactGroupMembers = "zimbraPrefContactsDisableAutocompleteOnContactGroupMembers";

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
     * Deprecated since: 4.5. Deprecated in favor of
     * zimbraPrefForwardReplyFormat. Orig desc: whether or not to use same
     * format (text or html) of message we are replying to
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
     * all messages will be retained.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=538)
    public static final String A_zimbraPrefInboxReadLifetime = "zimbraPrefInboxReadLifetime";

    /**
     * Retention period of unread messages in the Inbox folder. 0 means that
     * all messages will be retained.
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=537)
    public static final String A_zimbraPrefInboxUnreadLifetime = "zimbraPrefInboxUnreadLifetime";

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
     * The shorter duration is used.
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
     * interval at which the web client polls the server for new messages
     * (nnnnn[hmsd])
     */
    @ZAttr(id=111)
    public static final String A_zimbraPrefMailPollingInterval = "zimbraPrefMailPollingInterval";

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
     * interval
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
     * out of office notifications (if enabled) are sent only if current date
     * is before this date
     */
    @ZAttr(id=385)
    public static final String A_zimbraPrefOutOfOfficeUntilDate = "zimbraPrefOutOfOfficeUntilDate";

    /**
     * download pop3 messages since
     *
     * @since ZCS 5.0.6
     */
    @ZAttr(id=653)
    public static final String A_zimbraPrefPop3DownloadSince = "zimbraPrefPop3DownloadSince";

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
     * address to put in reply-to header of read receipt messages, if it is
     * not set, then the compose identitys primary email address is used.
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
     * messages will be retained.
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
     * show calendar week in calendar views
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1045)
    public static final String A_zimbraPrefShowCalendarWeek = "zimbraPrefShowCalendarWeek";

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
     * show selection checkbox for selecting email, contact, voicemial items
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
     * The name of the dictionary used for spell checking. If not set, the
     * locale is used.
     *
     * @since ZCS 6.0.0_GA
     */
    @ZAttr(id=1041)
    public static final String A_zimbraPrefSpellDictionary = "zimbraPrefSpellDictionary";

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
     * time zone of user or COS
     */
    @ZAttr(id=235)
    public static final String A_zimbraPrefTimeZoneId = "zimbraPrefTimeZoneId";

    /**
     * Retention period of messages in the Trash folder. 0 means that all
     * messages will be retained. This user-modifiable attribute works in
     * conjunction with zimbraMailTrashLifetime, which is admin-modifiable.
     * The shorter duration is used.
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
     * Last time a quota warning was sent.
     */
    @ZAttr(id=484)
    public static final String A_zimbraQuotaLastWarnTime = "zimbraQuotaLastWarnTime";

    /**
     * Minimum duration of time between quota warnings.
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
     * imap/pop3 reverse proxy lookups
     *
     * @since ZCS 5.0.0
     */
    @ZAttr(id=569)
    public static final String A_zimbraReverseProxyAuthWaitInterval = "zimbraReverseProxyAuthWaitInterval";

    /**
     * time interval that an entry cached by NGINX will remain in the cache
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=732)
    public static final String A_zimbraReverseProxyCacheEntryTTL = "zimbraReverseProxyCacheEntryTTL";

    /**
     * time interval that NGINX proxy will wait for a cache result, before
     * considering the result as a cache miss
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=731)
    public static final String A_zimbraReverseProxyCacheFetchTimeout = "zimbraReverseProxyCacheFetchTimeout";

    /**
     * time interval that NGINX proxy will wait before attempting to
     * re-establish a connection to a memcache server that disconnected
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=730)
    public static final String A_zimbraReverseProxyCacheReconnectInterval = "zimbraReverseProxyCacheReconnectInterval";

    /**
     * Time interval after which NGINX mail proxy will disconnect while
     * establishing an upstream IMAP/POP connection
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
     * Time interval after which NGINX mail proxy will disconnect an inactive
     * IMAP/POP connection
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
     * request within this time
     *
     * @since ZCS 5.0.10
     */
    @ZAttr(id=745)
    public static final String A_zimbraReverseProxyRouteLookupTimeout = "zimbraReverseProxyRouteLookupTimeout";

    /**
     * Time interval (ms) given to mail route lookup handler to cache a
     * failed response to route a previous lookup request (after this time
     * elapses, Proxy retries this host)
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
     * There is a deployment scenario for migrations where all of the
     * customers users are pointed at the zimbra POP IMAP reverse proxy. We
     * then want their connections proxied back to the legacy system for for
     * not-yet-non-migrated users. If this attribute is TRUE, reverse proxy
     * lookup sevlet should check to see if zimbraExternal* is set on the
     * domain. If so it is used. If not, lookup proceeds as usual.
     *
     * @since ZCS 5.0.12
     */
    @ZAttr(id=779)
    public static final String A_zimbraReverseProxyUseExternalRoute = "zimbraReverseProxyUseExternalRoute";

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
     * items an account or group has shared
     */
    @ZAttr(id=357)
    public static final String A_zimbraShareInfo = "zimbraShareInfo";

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
     * Spaminess percentage beyound which a message is marked as spam
     */
    @ZAttr(id=204)
    public static final String A_zimbraSpamTagPercent = "zimbraSpamTagPercent";

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
     * space separated list of excluded cipher suites
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
     * Start date for daylight time
     */
    @ZAttr(id=232)
    public static final String A_zimbraTimeZoneDaylightDtStart = "zimbraTimeZoneDaylightDtStart";

    /**
     * Offset in daylight time
     */
    @ZAttr(id=233)
    public static final String A_zimbraTimeZoneDaylightOffset = "zimbraTimeZoneDaylightOffset";

    /**
     * iCalendar recurrence rule for onset of daylight time
     */
    @ZAttr(id=234)
    public static final String A_zimbraTimeZoneDaylightRRule = "zimbraTimeZoneDaylightRRule";

    /**
     * Start date for standard time
     */
    @ZAttr(id=229)
    public static final String A_zimbraTimeZoneStandardDtStart = "zimbraTimeZoneStandardDtStart";

    /**
     * Offset in standard time
     */
    @ZAttr(id=230)
    public static final String A_zimbraTimeZoneStandardOffset = "zimbraTimeZoneStandardOffset";

    /**
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
     * version
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
     * how often (nnnnn[hmsd]) the virus definitions are updated
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
     * logout URL for web client to send the user to upon explicit loggin out
     */
    @ZAttr(id=507)
    public static final String A_zimbraWebClientLogoutURL = "zimbraWebClientLogoutURL";

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

