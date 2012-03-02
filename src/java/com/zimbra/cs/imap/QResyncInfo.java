/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.cs.imap;

public class QResyncInfo {
    int uvv;
    int modseq;
    String knownUIDs;
    String seqMilestones;
    String uidMilestones;

    public void setUvv(int uvv) {
        this.uvv = uvv;
    }

    public void setModseq(int modseq) {
        this.modseq = modseq;
    }

    public void setKnownUIDs(String knownUIDs) {
        this.knownUIDs = knownUIDs;
    }

    public void setSeqMilestones(String seqMilestones) {
        this.seqMilestones = seqMilestones;
    }

    public void setUidMilestones(String uidMilestones) {
        this.uidMilestones = uidMilestones;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((knownUIDs == null) ? 0 : knownUIDs.hashCode());
        result = prime * result + modseq;
        result = prime * result + ((seqMilestones == null) ? 0 : seqMilestones.hashCode());
        result = prime * result + ((uidMilestones == null) ? 0 : uidMilestones.hashCode());
        result = prime * result + uvv;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        QResyncInfo other = (QResyncInfo) obj;
        if (knownUIDs == null) {
            if (other.knownUIDs != null) {
                return false;
            }
        } else if (!knownUIDs.equals(other.knownUIDs)) {
            return false;
        }
        if (modseq != other.modseq) {
            return false;
        }
        if (seqMilestones == null) {
            if (other.seqMilestones != null) {
                return false;
            }
        } else if (!seqMilestones.equals(other.seqMilestones)) {
            return false;
        }
        if (uidMilestones == null) {
            if (other.uidMilestones != null) {
                return false;
            }
        } else if (!uidMilestones.equals(other.uidMilestones)) {
            return false;
        }
        if (uvv != other.uvv) {
            return false;
        }
        return true;
    }
}
