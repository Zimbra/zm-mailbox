/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"sessions", "errors", "signalledAccounts",
    "bufferedCommits"})
public class WaitSetInfo {

    //  This class provides a single JAXB object for handling all of
    //  WaitSetBase / AllAccountsWaitSet / SomeAccountsWaitSet
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String waitSetId;

    @XmlAttribute(name=AdminConstants.A_OWNER, required=true)
    private final String owner;

    @XmlElement(name="session", required=false)
    private List<SessionForWaitSet> sessions = Lists.newArrayList();

    @XmlAttribute(name=AdminConstants.A_DEFTYPES, required=true)
    private final String defaultInterests;

    @XmlAttribute(name=AdminConstants.A_LAST_ACCESSED_DATE, required=true)
    private final long lastAccessDate;

    @XmlElementWrapper(name=AdminConstants.E_ERRORS, required=false)
    @XmlElement(name=MailConstants.E_ERROR, required=false)
    private List<IdAndType> errors = Lists.newArrayList();

    @XmlElement(name=AdminConstants.A_READY, required=false)
    private AccountsAttrib signalledAccounts;

    @XmlAttribute(name=AdminConstants.A_CB_SEQ_NO, required=false)
    private String cbSeqNo;

    @XmlAttribute(name=AdminConstants.A_CURRENT_SEQ_NO, required=false)
    private String currentSeqNo;

    @XmlAttribute(name=AdminConstants.A_NEXT_SEQ_NO, required=false)
    private String nextSeqNo;

    @XmlElementWrapper(name="buffered", required=false)
    @XmlElement(name="commit", required=false)
    private List<BufferedCommitInfo> bufferedCommits = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private WaitSetInfo() {
        this((String) null, (String) null, (String) null, -1L);
    }

    public WaitSetInfo(String waitSetId, String owner,
                        String defaultInterests, long lastAccessDate) {
        this.waitSetId = waitSetId;
        this.owner = owner;
        this.defaultInterests = defaultInterests;
        this.lastAccessDate = lastAccessDate;
    }

    public void setSessions(Iterable <SessionForWaitSet> sessions) {
        this.sessions.clear();
        if (sessions != null) {
            Iterables.addAll(this.sessions,sessions);
        }
    }

    public WaitSetInfo addSession(SessionForWaitSet session) {
        this.sessions.add(session);
        return this;
    }

    public void setErrors(Iterable <IdAndType> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    public WaitSetInfo addError(IdAndType error) {
        this.errors.add(error);
        return this;
    }

    public void setSignalledAccounts(AccountsAttrib signalledAccounts) {
        this.signalledAccounts = signalledAccounts;
    }

    public void setCbSeqNo(String cbSeqNo) { this.cbSeqNo = cbSeqNo; }
    public void setCurrentSeqNo(String currentSeqNo) {
        this.currentSeqNo = currentSeqNo;
    }

    public void setNextSeqNo(String nextSeqNo) { this.nextSeqNo = nextSeqNo; }
    public void setBufferedCommits(
                    Iterable <BufferedCommitInfo> bufferedCommits) {
        this.bufferedCommits.clear();
        if (bufferedCommits != null) {
            Iterables.addAll(this.bufferedCommits,bufferedCommits);
        }
    }

    public WaitSetInfo addBufferedCommit(BufferedCommitInfo bufferedCommit) {
        this.bufferedCommits.add(bufferedCommit);
        return this;
    }

    public String getWaitSetId() { return waitSetId; }
    public String getOwner() { return owner; }
    public List<SessionForWaitSet> getSessions() {
        return Collections.unmodifiableList(sessions);
    }
    public String getDefaultInterests() { return defaultInterests; }
    public long getLastAccessDate() { return lastAccessDate; }
    public List<IdAndType> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    public AccountsAttrib getSignalledAccounts() { return signalledAccounts; }
    public String getCbSeqNo() { return cbSeqNo; }
    public String getCurrentSeqNo() { return currentSeqNo; }
    public String getNextSeqNo() { return nextSeqNo; }
    public List<BufferedCommitInfo> getBufferedCommits() {
        return Collections.unmodifiableList(bufferedCommits);
    }
}
