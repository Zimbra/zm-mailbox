package com.zimbra.cs.contacts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;

/**
 * AffinityScope defines the context within contacts are considered "related".
 */
public abstract class AffinityScope implements Comparable<AffinityScope> {

    private static Map<Integer, AffinityScope> KNOWN_SCOPES = new HashMap<>();

    public static AffinityScope OUTGOING_EXACT_MATCH            = new OutgoingAffinityScope(0, MatchType.ALL, false);
    public static AffinityScope OUTGOING_EXACT_MATCH_ANY_FIELD  = new OutgoingAffinityScope(1, MatchType.ALL, true);
    public static AffinityScope OUTGOING_BROAD_MATCH            = new OutgoingAffinityScope(2, MatchType.ANY, false);
    public static AffinityScope OUTGOING_BROAD_MATCH_ANY_FIELD  = new OutgoingAffinityScope(3, MatchType.ANY, true);
    public static AffinityScope INCOMING_FROM_TARGET            = new IncomingAffinityScope(4, true, true);
    public static AffinityScope INCOMING_FROM_ANY_SENDER        = new IncomingAffinityScope(5, false, true);

    static {
        addScope(OUTGOING_EXACT_MATCH);
        addScope(OUTGOING_EXACT_MATCH_ANY_FIELD);
        addScope(OUTGOING_BROAD_MATCH);
        addScope(OUTGOING_BROAD_MATCH_ANY_FIELD);
        addScope(INCOMING_FROM_TARGET);
        addScope(INCOMING_FROM_ANY_SENDER);
    }

    private RelatedVia relatedVia;
    private boolean ignoreTargetAffinityField;
    private int scopeLevel;
    protected MatchType matchType;
    protected boolean senderMustBeTarget;

    private static void addScope(AffinityScope scope) {
        KNOWN_SCOPES.put(scope.getLevel(), scope);
    }

    public static List<AffinityScope> getAffinityScopesForQuery(RelatedContactsParams params) {
        int numTargets = params.getTargets().size();
        List<AffinityScope> scopes = new ArrayList<>();
        for (AffinityScope scope: KNOWN_SCOPES.values()) {
            if (scope.getRelatedVia() == RelatedVia.INCOMING_MSGS) {
                //don't include incoming msg scope if AFFINITY events are not logged
                //or if there is only one target with senderMustBeTarget=true
                if (!params.getIncludeIncomingMsgAffinity() || (scope.getSenderMustBeTarget() && numTargets == 1)) {
                    continue;
                }
            }
            scopes.add(scope);
        }
        Collections.sort(scopes);
        return scopes;
    }

    private AffinityScope(int scopeLevel, RelatedVia relatedVia, boolean ignoreTargetAffinityField) {
        this.scopeLevel = scopeLevel;
        this.relatedVia = relatedVia;
        this.ignoreTargetAffinityField = ignoreTargetAffinityField;
    }

    public static enum RelatedVia {
        INCOMING_MSGS, OUTGOING_MSGS;
    }

    public static enum MatchType {
        ALL, ANY;
    }

    public int getLevel() {
        return scopeLevel;
    }

    public RelatedVia getRelatedVia() {
        return relatedVia;
    }

    public boolean getIgnoreTargetAffinityField() {
        return ignoreTargetAffinityField;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public boolean getSenderMustBeTarget() {
        return senderMustBeTarget;
    }

    @Override
    public int compareTo(AffinityScope other) {
        if (this == other) {
            return 0;
        }
        return scopeLevel - other.getLevel();
    }

    public static class OutgoingAffinityScope extends AffinityScope {

        public OutgoingAffinityScope(int id, MatchType matchType, boolean ignoreTargetAffinityField) {
            super(id, RelatedVia.OUTGOING_MSGS, ignoreTargetAffinityField);
            this.matchType = matchType;
        }
    }

    public static class IncomingAffinityScope extends AffinityScope {

        public IncomingAffinityScope(int id, boolean senderMustBeTarget, boolean ignoreTargetAffinityField) {
            super(id, RelatedVia.INCOMING_MSGS, ignoreTargetAffinityField);
            this.senderMustBeTarget = senderMustBeTarget;
        }
    }

    public static AffinityScope of(int id) throws ServiceException {
        AffinityScope scope = KNOWN_SCOPES.get(id);
        if (scope == null) {
            throw ServiceException.FAILURE(String.format("%d is not a known affinity scope ID",  id), null);
        }
        return scope;
    }
}