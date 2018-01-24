package com.zimbra.cs.contacts;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;

public class RelatedContactsResults {

    private RelatedContactsParams params;
    private List<RelatedContact> results;
    private long dateComputed;
    private boolean maybeHasMore = true;

    public RelatedContactsResults(RelatedContactsParams params, long dateComputed) {
        this.results = new ArrayList<>();
        this.params = params;
        this.dateComputed = dateComputed;
    }

    public void addResult(RelatedContact result) {
        this.results.add(result);
    }

    public void addResults(List<RelatedContact> results) {
        this.results.addAll(results);
    }

    public RelatedContactsParams getQueryParams() {
        return params;
    }

    public boolean hasResults() {
        return !results.isEmpty();
    }

    public List<RelatedContact> getResults() {
        return results;
    }

    public long getDate() {
        return dateComputed;
    }

    public int size() {
        return results.size();
    }

    public void setMaybeHasMore(boolean bool) {
        this.maybeHasMore = bool;
    }

    public boolean maybeHasMore() {
        return maybeHasMore;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("acctId", params.getAccountId())
                .add("targets", Joiner.on(", ").join(params.getTargets()))
                .add("affinityType", params.getRequestedAffinityType())
                .add("results", Joiner.on(", ").join(results))
                .add("date", dateComputed)
                .add("size", size())
                .toString();

    }
    public static class RelatedContact {
        private String email;
        private double score;
        private AffinityScope scope;

        public RelatedContact(String email, double score, AffinityScope scope) {
            this.email = email;
            this.score = score;
            this.scope = scope;
        }
        public String getEmail() {
            return email;
        }

        public double getScore() {
            return score;
        }

        public AffinityScope getScope() {
            return scope;
        }

        @Override
        public String toString() {
            return String.format("[%s (%s) scope=%s]", email, score, scope.getLevel());
        }
    }
}
