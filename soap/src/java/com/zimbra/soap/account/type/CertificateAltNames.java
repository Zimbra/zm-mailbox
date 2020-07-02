/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
package com.zimbra.soap.account.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.SmimeConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class CertificateAltNames {

    /**
     * @zm-api-field-tag otherName
     * @zm-api-field-description otherName of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_OTHER_NAME, required=false)
    private List<String> otherName;

    /**
     * @zm-api-field-tag rfc822Name
     * @zm-api-field-description rfc822Name of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_RFC822_NAME, required=false)
    private List<String> rfc822Name;

    /**
     * @zm-api-field-tag dNSName
     * @zm-api-field-description dNSName of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_DNS_NAME, required=false)
    private List<String> dNSName;

    /**
     * @zm-api-field-tag x400Address
     * @zm-api-field-description x400Address of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_X400ADDRESS, required=false)
    private List<String> x400Address;

    /**
     * @zm-api-field-tag directoryName
     * @zm-api-field-description directoryName of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_DIRECTORY_NAME, required=false)
    private List<String> directoryName;

    /**
     * @zm-api-field-tag ediPartyName
     * @zm-api-field-description ediPartyName of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_EDI_PARTY_NAME, required=false)
    private List<String> ediPartyName;

    /**
     * @zm-api-field-tag uniformResourceIdentifier
     * @zm-api-field-description uniformResourceIdentifier of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_URI, required=false)
    private List<String> uniformResourceIdentifier;

    /**
     * @zm-api-field-tag iPAddress
     * @zm-api-field-description iPAddress of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_IP_ADDRESS, required=false)
    private List<String> iPAddress;

    /**
     * @zm-api-field-tag registeredID
     * @zm-api-field-description registeredID of subject or issuer.
     */
    @XmlElement(name=SmimeConstants.E_REGISTERED_ID, required=false)
    private List<String> registeredID;

    public List<String> getOtherName() {
        if (otherName != null) {
            return Collections.unmodifiableList(otherName);
        } else {
            return otherName;
        }
    }

    public void addOtherName(String otherName) {
        if (this.otherName == null) {
            this.otherName = Lists.newArrayList();
        }
        this.otherName.add(otherName);
    }

    public void setOtherName(List<String> otherName) {
        if (this.otherName != null) {
            this.otherName.clear();
        } else {
            this.otherName = Lists.newArrayList();
        }
        if (otherName != null) {
            Iterables.addAll(this.otherName, otherName);
        }
    }

    public List<String> getRfc822Name() {
        if (rfc822Name != null) {
            return Collections.unmodifiableList(rfc822Name);
        } else {
            return rfc822Name;
        }
    }

    public void addRfc822Name(String rfc822Name) {
        if (this.rfc822Name == null) {
            this.rfc822Name = Lists.newArrayList();
        }
        this.rfc822Name.add(rfc822Name);
    }

    public void setRfc822Name(List<String> rfc822Name) {
        if (this.rfc822Name != null) {
            this.rfc822Name.clear();
        } else {
            this.rfc822Name = Lists.newArrayList();
        }
        if (rfc822Name != null) {
            Iterables.addAll(this.rfc822Name, rfc822Name);
        }
    }

    public List<String> getdNSName() {
        if (dNSName != null) {
            return Collections.unmodifiableList(dNSName);
        } else {
            return dNSName;
        }
    }

    public void addDNSName(String dNSName) {
        if (this.dNSName == null) {
            this.dNSName = Lists.newArrayList();
        }
        this.dNSName.add(dNSName);
    }

    public void setdNSName(List<String> dNSName) {
        if (this.dNSName != null) {
            this.dNSName.clear();
        } else {
            this.dNSName = Lists.newArrayList();
        }
        if (dNSName != null) {
            Iterables.addAll(this.dNSName, dNSName);
        }
    }

    public List<String> getX400Address() {
        if (x400Address != null) {
            return Collections.unmodifiableList(x400Address);
        } else {
            return x400Address;
        }
    }

    public void addX400Address(String x400Address) {
        if (this.x400Address == null) {
            this.x400Address = Lists.newArrayList();
        }
        this.x400Address.add(x400Address);
    }

    public void setX400Address(List<String> x400Address) {
        if (this.x400Address != null) {
            this.x400Address.clear();
        } else {
            this.x400Address = Lists.newArrayList();
        }
        if (x400Address != null) {
            Iterables.addAll(this.x400Address, x400Address);
        }
    }

    public List<String> getDirectoryName() {
        if (directoryName != null) {
            return Collections.unmodifiableList(directoryName);
        } else {
            return directoryName;
        }
    }

    public void addDirectoryName(String directoryName) {
        if (this.directoryName == null) {
            this.directoryName = Lists.newArrayList();
        }
        this.directoryName.add(directoryName);
    }

    public void setDirectoryName(List<String> directoryName) {
        if (this.directoryName != null) {
            this.directoryName.clear();
        } else {
            this.directoryName = Lists.newArrayList();
        }
        if (directoryName != null) {
            Iterables.addAll(this.directoryName, directoryName);
        }
    }

    public List<String> getEdiPartyName() {
        if (ediPartyName != null) {
            return Collections.unmodifiableList(ediPartyName);
        } else {
            return ediPartyName;
        }
    }

    public void addEdiPartyName(String ediPartyName) {
        if (this.ediPartyName == null) {
            this.ediPartyName = Lists.newArrayList();
        }
        this.ediPartyName.add(ediPartyName);
    }

    public void setEdiPartyName(List<String> ediPartyName) {
        if (this.ediPartyName != null) {
            this.ediPartyName.clear();
        } else {
            this.ediPartyName = Lists.newArrayList();
        }
        if (ediPartyName != null) {
            Iterables.addAll(this.ediPartyName, ediPartyName);
        }
    }

    public List<String> getUniformResourceIdentifier() {
        if (uniformResourceIdentifier != null) {
            return Collections.unmodifiableList(uniformResourceIdentifier);
        } else {
            return uniformResourceIdentifier;
        }
    }

    public void addUniformResourceIdentifier(String uniformResourceIdentifier) {
        if (this.uniformResourceIdentifier == null) {
            this.uniformResourceIdentifier = Lists.newArrayList();
        }
        this.uniformResourceIdentifier.add(uniformResourceIdentifier);
    }

    public void setUniformResourceIdentifier(List<String> uniformResourceIdentifier) {
        if (this.uniformResourceIdentifier != null) {
            this.uniformResourceIdentifier.clear();
        } else {
            this.uniformResourceIdentifier = Lists.newArrayList();
        }
        if (uniformResourceIdentifier != null) {
            Iterables.addAll(this.uniformResourceIdentifier, uniformResourceIdentifier);
        }
    }

    public List<String> getiPAddress() {
        if (iPAddress != null) {
            return Collections.unmodifiableList(iPAddress);
        } else {
            return iPAddress;
        }
    }

    public void addIPAddress(String iPAddress) {
        if (this.iPAddress == null) {
            this.iPAddress = Lists.newArrayList();
        }
        this.iPAddress.add(iPAddress);
    }

    public void setiPAddress(List<String> iPAddress) {
        if (this.iPAddress != null) {
            this.iPAddress.clear();
        } else {
            this.iPAddress = Lists.newArrayList();
        }
        if (iPAddress != null) {
            Iterables.addAll(this.iPAddress, iPAddress);
        }
    }

    public List<String> getRegisteredID() {
        if (registeredID != null) {
            return Collections.unmodifiableList(registeredID);
        } else {
            return registeredID;
        }
    }

    public void addRegisteredID(String registeredID) {
        if (this.registeredID == null) {
            this.registeredID = Lists.newArrayList();
        }
        this.registeredID.add(registeredID);
    }

    public void setRegisteredID(List<String> registeredID) {
        if (this.registeredID != null) {
            this.registeredID.clear();
        } else {
            this.registeredID = Lists.newArrayList();
        }
        if (registeredID != null) {
            Iterables.addAll(this.registeredID, registeredID);
        }
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.add("otherName", otherName)
            .add("rfc822Name", rfc822Name)
            .add("dNSName", dNSName)
            .add("x400Address", x400Address)
            .add("directoryName", directoryName)
            .add("ediPartyName", ediPartyName)
            .add("uniformResourceIdentifier", uniformResourceIdentifier)
            .add("iPAddress", iPAddress)
            .add("registeredID", registeredID);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
