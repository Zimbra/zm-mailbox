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
import com.zimbra.soap.type.IdAndType;

// soap-waitset.txt implies a lot of these attributes are directly unser QueryWaitSetResponse.  They aren't.
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"sessions", "errors", "signalledAccounts",
    "bufferedCommits"})
public class WaitSetInfo {

    //  This class provides a single JAXB object for handling all of
    //  WaitSetBase / AllAccountsWaitSet / SomeAccountsWaitSet

    /**
     * @zm-api-field-tag waitset-id
     * @zm-api-field-description WaitSet ID
     */
    @XmlAttribute(name=AdminConstants.A_ID /* id */, required=true)
    private final String waitSetId;

    /**
     * @zm-api-field-tag waitset-owner-acct-id
     * @zm-api-field-description WaitSet owner account ID
     */
    @XmlAttribute(name=AdminConstants.A_OWNER /* owner */, required=true)
    private final String owner;

    /**
     * @zm-api-field-tag default-interests
     * @zm-api-field-description Default interest types: comma-separated list.  Currently:
     * <table>
     * <tr> <td> <b>c</b> </td> <td> contacts </td> </tr>
     * <tr> <td> <b>m</b> </td> <td> msgs (and subclasses) </td> </tr>
     * <tr> <td> <b>a</b> </td> <td> appointments </td> </tr>
     * <tr> <td> <b>t</b> </td> <td> tasks </td> </tr>
     * <tr> <td> <b>d</b> </td> <td> documents </td> </tr>
     * <tr> <td> <b>all</b> </td> <td> all types (equiv to "c,m,a,t,d") * </td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.A_DEFTYPES /* defTypes */, required=true)
    private final String defaultInterests;

    /**
     * @zm-api-field-tag last-access-date
     * @zm-api-field-description Last access date
     */
    @XmlAttribute(name=AdminConstants.A_LAST_ACCESSED_DATE /* ld */, required=true)
    private final long lastAccessDate;

    /**
     * @zm-api-field-description Error information
     */
    @XmlElementWrapper(name=AdminConstants.E_ERRORS /* errors */, required=false)
    @XmlElement(name=MailConstants.E_ERROR /* error */, required=false)
    private List<IdAndType> errors = Lists.newArrayList();

    /**
     * @zm-api-field-tag ready-comm-sep-acct-ids
     * @zm-api-field-description Comma separated list of account IDs
     */
    @XmlElement(name=AdminConstants.A_READY /* ready */, required=false)
    private AccountsAttrib signalledAccounts;

    /**
     * @zm-api-field-tag cb-seq-no
     * @zm-api-field-description CB sequence number
     */
    @XmlAttribute(name=AdminConstants.A_CB_SEQ_NO /* cbSeqNo */, required=false)
    private String cbSeqNo;

    /**
     * @zm-api-field-tag curr-seq-no
     * @zm-api-field-description Current sequence number
     */
    @XmlAttribute(name=AdminConstants.A_CURRENT_SEQ_NO /* currentSeqNo */, required=false)
    private String currentSeqNo;

    /**
     * @zm-api-field-tag next-seq-no
     * @zm-api-field-description Next sequence number
     */
    @XmlAttribute(name=AdminConstants.A_NEXT_SEQ_NO /* nextSeqNo */, required=false)
    private String nextSeqNo;

    /**
     * @zm-api-field-description Buffered commit information
     */
    @XmlElementWrapper(name="buffered", required=false)
    @XmlElement(name="commit", required=false)
    private List<BufferedCommitInfo> bufferedCommits = Lists.newArrayList();

    /**
     * @zm-api-field-description Session information
     */
    @XmlElement(name="session", required=false)
    private List<SessionForWaitSet> sessions = Lists.newArrayList();

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
