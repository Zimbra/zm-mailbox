package com.zimbra.cs.contacts;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.soap.mail.type.RelatedContactsTarget;

/**
 * Class encapsulating parameters of a "related contacts" search
 */
public class RelatedContactsParams {

    private String accountId;
    private List<AffinityTarget> targets;
    private Long dateCutoff;
    private int limit;
    private int minOccurCount = 1;
    private boolean includeIncomingMsgAffinity;
    private AffinityType requestedAffinityType = AffinityType.all;

    public RelatedContactsParams(String accountId) {
        this(accountId, null);
    }

    public RelatedContactsParams(String accountId, AffinityTarget target) {
        this.targets = Lists.newArrayList();
        this.accountId = accountId;
        if (target != null) {
            targets.add(target);
        }
    }

    public RelatedContactsParams addTarget(AffinityTarget target) {
        targets.add(target);
        return this;
    }

    public RelatedContactsParams setDateCutoff(long dateCutoff) {
        this.dateCutoff = dateCutoff;
        return this;
    }

    public RelatedContactsParams setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    public RelatedContactsParams setRequestedAffinityType(AffinityType affinityType) {
        this.requestedAffinityType = affinityType;
        return this;
    }

    public RelatedContactsParams setMinOccurCount(int minOccurCount) {
        this.minOccurCount = minOccurCount;
        return this;
    }

    public RelatedContactsParams setIncludeIncomingMsgAffinity(boolean include) {
        this.includeIncomingMsgAffinity = include;
        return this;
    }

    public List<AffinityTarget> getTargets() {
        return targets;
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean hasDateCutoff() {
        return dateCutoff != null;
    }

    public long getDateCutoff() {
        return dateCutoff;
    }

    public int getLimit() {
        return limit;
    }

    public AffinityType getRequestedAffinityType() {
        return requestedAffinityType;
    }

    public int getMinOccur() {
        return minOccurCount;
    }

    public boolean getIncludeIncomingMsgAffinity() {
        return includeIncomingMsgAffinity;
    }

    public static class AffinityTarget {

        private AffinityType affinityType;
        private String contactEmail;

        public AffinityTarget(AffinityType affinityType, String contactEmail) {
            this.affinityType = affinityType;
            this.contactEmail = contactEmail;
        }

        public String getContactEmail() {
            return contactEmail;
        }

        public AffinityType getAffinityType() {
            return affinityType;
        }

        public static AffinityTarget fromSOAPTarget(RelatedContactsTarget target) throws ServiceException {
            String targetEmail = target.getTargetEmail();
            String affinityStr = target.getAffinity();
            AffinityType affinity;
            if (Strings.isNullOrEmpty(affinityStr)) {
                affinity = AffinityType.all;
            } else {
                affinity = AffinityType.of(affinityStr);
            }
            return new AffinityTarget(affinity, targetEmail);
        }
        @Override
        public String toString() {
            if (affinityType == AffinityType.all) {
                return String.format("[%s]", contactEmail);
            } else {
                return String.format("[%s:%s]", affinityType.name(), contactEmail);
            }
        }
    }

    public static enum ScopeMethod {
        adaptive, widest;

        public static ScopeMethod of(String str) throws ServiceException {
            for (ScopeMethod policy: ScopeMethod.values()) {
                if (policy.name().equalsIgnoreCase(str)) {
                    return policy;
                }
            }
            throw ServiceException.INVALID_REQUEST(String.format("%s is not a valid affinity scope method",  str), null);
        }
    }

    public static enum AffinityType {
        all, to, cc, bcc;

        public static AffinityType of(String typeStr) throws ServiceException {
            for (AffinityType type: AffinityType.values()) {
                if (type.name().equalsIgnoreCase(typeStr)) {
                    return type;
                }
            }
            throw ServiceException.INVALID_REQUEST(String.format("%s is not a valid affinity type",  typeStr), null);
        }
    }

    @Override
    public String toString() {
        ToStringHelper helper = Objects.toStringHelper(this)
        .add("acctId", accountId)
        .add("targets", Joiner.on(",").join(targets))
        .add("affinityType", requestedAffinityType)
        .add("dateCutoff", dateCutoff)
        .add("limit", limit)
        .add("minOccur", minOccurCount);
        return helper.toString();
    }
}
