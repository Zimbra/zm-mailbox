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
package com.zimbra.doc.soap.changelog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.zimbra.doc.soap.apidesc.SoapApiAttribute;
import com.zimbra.doc.soap.apidesc.SoapApiCommand;
import com.zimbra.doc.soap.apidesc.SoapApiElement;
import com.zimbra.doc.soap.apidesc.SoapApiElementArtifact;
import com.zimbra.doc.soap.apidesc.SoapApiElementChoiceInfo;
import com.zimbra.doc.soap.apidesc.SoapApiNamedElement;
import com.zimbra.doc.soap.apidesc.SoapApiSimpleElement;
import com.zimbra.doc.soap.apidesc.SoapApiType;
import com.zimbra.doc.soap.apidesc.SoapApiWrapperElement;

/**
 * Encodes information about changes to a command between a baseline revision and the current revision
 * @author gren
 */
public class CommandChanges {
    private final SoapApiCommand baselineCmd;
    private final SoapApiCommand currentCmd;
    private final Map<String,SoapApiType> baselineTypes = Maps.newHashMap();
    private final Map<String,SoapApiType> currentTypes = Maps.newHashMap();

    private final List<NamedAttr> newAttrs = Lists.newArrayList();
    private final List<NamedAttr> deletedAttrs = Lists.newArrayList();
    private final List<AttributeChanges> modifiedAttrs = Lists.newArrayList();
    private final List<NamedElem> newElems = Lists.newArrayList();
    private final List<NamedElem> deletedElems = Lists.newArrayList();
    private final List<ElementChanges> modifiedElements = Lists.newArrayList();

    public boolean hasChanges() {
        return (newAttrs.size() > 0) || (deletedAttrs.size() > 0) || (modifiedAttrs.size() > 0) ||
                (newElems.size() > 0) || (deletedElems.size() > 0) || (modifiedElements.size() > 0);
    }

    public class Ancestry {
        private final Set<String> classNames = Sets.newHashSet();
        private final String xpath;
        public Ancestry(String xpath, String className) {
            this.xpath = xpath;
            if (className != null) {
                classNames.add(className);
            }
        }

        public Ancestry(Ancestry parentAncestry, String name, String className) {
            this(String.format("%s/%s", parentAncestry.getXpath(), name), className);
            classNames.addAll(parentAncestry.getClassNames());
        }

        public String getXpath() { return xpath; }
        public Set<String> getClassNames() { return classNames; }
        public boolean hasAncestor(String className) {
            return classNames.contains(className);
        }
    }

    public class NamedAttr {
        private SoapApiAttribute info;
        private String xpath;
        public NamedAttr(String parentXpath, SoapApiAttribute attr) {
            info = attr;
            xpath = parentXpath + "@" + attr.getName();
        }

        public SoapApiAttribute getInfo() { return info; }
        public String getXpath() { return xpath; }
    }

    public class NamedElem {
        private SoapApiNamedElement info;
        private String xpath;
        public NamedElem(String parentXpath, SoapApiNamedElement elem) {
            info = elem;
            xpath = parentXpath + "/" + elem.getName();
        }

        public SoapApiNamedElement getInfo() { return info; }
        public String getXpath() { return xpath; }
    }

    public CommandChanges(SoapApiCommand baselineCmd, SoapApiCommand currentCmd,
            List<SoapApiType> baselineTypesList, List<SoapApiType> currentTypesList) {
        this.baselineCmd = baselineCmd;
        this.currentCmd = currentCmd;
        for (SoapApiType type :baselineTypesList) {
            baselineTypes.put(type.getClassName(), type);
        }
        for (SoapApiType type :currentTypesList) {
            currentTypes.put(type.getClassName(), type);
        }
        findChanges();
    }

    private void findChanges() {
        SoapApiType baseReq = baselineTypes.get(baselineCmd.getRequest().getJaxb());
        SoapApiType baseResp = baselineTypes.get(baselineCmd.getResponse().getJaxb());
        SoapApiType currReq = currentTypes.get(currentCmd.getRequest().getJaxb());
        SoapApiType currResp = currentTypes.get(currentCmd.getResponse().getJaxb());
        Ancestry reqAncestry = new Ancestry(baselineCmd.getRequest().getName(), baselineCmd.getRequest().getJaxb());
        Ancestry respAncestry = new Ancestry(baselineCmd.getResponse().getName(), baselineCmd.getResponse().getJaxb());
        checkElemValue(reqAncestry, baselineCmd.getRequest(), currentCmd.getRequest());
        checkElemValue(respAncestry, baselineCmd.getResponse(), currentCmd.getResponse());
        findChanges(reqAncestry, baseReq, currReq);
        findChanges(respAncestry, baseResp, currResp);
    }

    private void findChanges(Ancestry ancestry, SoapApiType baseType, SoapApiType currType) {
        findAttributeChanges(ancestry.getXpath(), baseType, currType);
        findSubElementChanges(ancestry, baseType, currType);
    }

    private void findAttributeChanges(String xpath, SoapApiType baseType, SoapApiType currType) {
        Map<String,SoapApiAttribute> baselineAttrs = Maps.newTreeMap();
        Map<String,SoapApiAttribute> currentAttrs = Maps.newTreeMap();
        if (baseType.getAttributes() != null) {
            for (SoapApiAttribute attr : baseType.getAttributes()) {
                baselineAttrs.put(attr.getName(), attr);
            }
        }

        if (currType.getAttributes() != null) {
            for (SoapApiAttribute attr : currType.getAttributes()) {
                currentAttrs.put(attr.getName(), attr);
            }
        }

        for (Entry<String, SoapApiAttribute> entry : currentAttrs.entrySet()) {
            SoapApiAttribute baselineAttr = baselineAttrs.get(entry.getKey());
            if (baselineAttr != null) {
                AttributeChanges attrInfo = new AttributeChanges(xpath, baselineAttr, entry.getValue(),
                        baselineTypes, currentTypes);
                if (!attrInfo.isSame()) {
                    modifiedAttrs.add(attrInfo) ;
                }
            } else {
                newAttrs.add(new NamedAttr(xpath, entry.getValue()));
            }
        }
        for (Entry<String, SoapApiAttribute> entry : baselineAttrs.entrySet()) {
            if (!currentAttrs.containsKey(entry.getKey())) {
                deletedAttrs.add(new NamedAttr(xpath, entry.getValue()));
            }
        }
    }

    private void findSubElementChanges(Ancestry ancestry, SoapApiType baseType, SoapApiType currType) {
        Map<String,SoapApiNamedElement> baseElems = makeSubElemMap(baseType);
        Map<String,SoapApiNamedElement> currElems = makeSubElemMap(currType);
        findSubElementChanges(ancestry, baseElems, currElems);
    }
    
    private void findSubElementChanges(Ancestry ancestry,
            Map<String,SoapApiNamedElement> baseElems, Map<String,SoapApiNamedElement> currElems) {

        for (Entry<String, SoapApiNamedElement> entry : currElems.entrySet()) {
            SoapApiNamedElement baseElem = baseElems.get(entry.getKey());
            if (baseElem != null) {
                String className = null;
                boolean insideSelf = false;
                if (baseElem instanceof SoapApiElement) {
                    SoapApiElement be = (SoapApiElement) baseElem;
                    className = be.getJaxb();
                    insideSelf = ancestry.hasAncestor(className);
                }
                if (!insideSelf) {
                    Ancestry subAncestry = new Ancestry(ancestry, baseElem.getName(), className);
                    findElementChanges(subAncestry, baseElem, entry.getValue());
                }
            } else {
                newElems.add(new NamedElem(ancestry.getXpath(), entry.getValue()));
            }
        }
        for (Entry<String, SoapApiNamedElement> entry : baseElems.entrySet()) {
            if (!currElems.containsKey(entry.getKey())) {
                deletedElems.add(new NamedElem(ancestry.getXpath(), entry.getValue()));
            }
        }
    }

    private void checkElemValue(Ancestry ancestry, SoapApiNamedElement baseElem, SoapApiNamedElement currElem) {
        ElementChanges info = new ElementChanges(ancestry.getXpath(), baseElem, currElem, baselineTypes, currentTypes);
        if (!info.isSame()) {
            modifiedElements.add(info) ;
        }
    }

    private void findElementChanges(Ancestry ancestry, SoapApiNamedElement baseElem, SoapApiNamedElement currElem) {
        checkElemValue(ancestry, baseElem, currElem);
        if (baseElem instanceof SoapApiWrapperElement) {
            elementChangesWhereBaseIsWrapper(ancestry, (SoapApiWrapperElement) baseElem, currElem);
        } else if (baseElem instanceof SoapApiSimpleElement) {
            SoapApiSimpleElement simpleBase = (SoapApiSimpleElement) baseElem;
            if (currElem instanceof SoapApiWrapperElement) {
                String baseJaxbClass = simpleBase.getJaxb();
                SoapApiWrapperElement currWrapper = (SoapApiWrapperElement) currElem;
                Map<String,SoapApiNamedElement> currSubElems = makeSubElemMap(currWrapper.getSubElements());
                if (baseJaxbClass == null) {
                    // "base" doesn't have attrs or sub-elements, so all elems under "curr" wrapper are new
                    for (Entry<String, SoapApiNamedElement> entry : currSubElems.entrySet()) {
                        newElems.add(new NamedElem(ancestry.getXpath(), entry.getValue()));
                    }
                } else {
                    SoapApiType baseType = baselineTypes.get(baseJaxbClass);
                    // "curr" is a wrapper, so any attributes in "base" have been deleted
                    if (baseType.getAttributes() != null) {
                        for (SoapApiAttribute attr : baseType.getAttributes()) {
                            deletedAttrs.add(new NamedAttr(ancestry.getXpath(), attr));
                        }
                    }
                    Map<String,SoapApiNamedElement> baseSubElems = makeSubElemMap(baseType.getSubElements());
                    findSubElementChanges(ancestry, baseSubElems, currSubElems);
                }
            } else if (currElem instanceof SoapApiSimpleElement) {
                elementChangesBothNonWrapper(ancestry, simpleBase, (SoapApiSimpleElement) currElem);
            }
        }
    }

    private void elementChangesBothNonWrapper(
            Ancestry ancestry, SoapApiSimpleElement baseElem, SoapApiSimpleElement currElem) {
        String baseJaxbClass = baseElem.getJaxb();
        SoapApiType baseType = baselineTypes.get(baseJaxbClass);
        SoapApiSimpleElement simpleCurr = (SoapApiSimpleElement) currElem;
        String currJaxbClass = simpleCurr.getJaxb();
        if (currJaxbClass == null) {
            // "curr" doesn't have attributes or sub-elements
            if (baseType != null) {
                if (baseType.getAttributes() != null) {
                    for (SoapApiAttribute attr : baseType.getAttributes()) {
                        deletedAttrs.add(new NamedAttr(ancestry.getXpath(), attr));
                    }
                }
                Map<String,SoapApiNamedElement> baseSubElems = makeSubElemMap(baseType.getSubElements());
                for (SoapApiNamedElement baseSubElem : baseSubElems.values()) {
                    deletedElems.add(new NamedElem(ancestry.getXpath(), baseSubElem));
                }
            }
        } else {
            SoapApiType currType = currentTypes.get(currJaxbClass);
            if (baseType != null) {
                findChanges(ancestry, baseType, currType);
            } else if (currType != null) {
                if (currType.getAttributes() != null) {
                    for (SoapApiAttribute attr : currType.getAttributes()) {
                        newAttrs.add(new NamedAttr(ancestry.getXpath(), attr));
                    }
                }
                Map<String,SoapApiNamedElement> currSubElems = makeSubElemMap(currType.getSubElements());
                for (SoapApiNamedElement currSubElem : currSubElems.values()) {
                    newElems.add(new NamedElem(ancestry.getXpath(), currSubElem));
                }
            }
        }
    }

    private void elementChangesWhereBaseIsWrapper(
            Ancestry ancestry, SoapApiWrapperElement baseWrapper, SoapApiNamedElement currElem) {
        Map<String,SoapApiNamedElement> baseSubElems = makeSubElemMap(baseWrapper.getSubElements());
        if (currElem instanceof SoapApiWrapperElement) {
            // Both "base" and "curr" are wrappers.
            SoapApiWrapperElement currWrapper = (SoapApiWrapperElement) currElem;
            Map<String,SoapApiNamedElement> currSubElems = makeSubElemMap(currWrapper.getSubElements());
            findSubElementChanges(ancestry, baseSubElems, currSubElems);
        } else if (currElem instanceof SoapApiSimpleElement) {
            // "base" was a wrapper.
            SoapApiSimpleElement simpleCurr = (SoapApiSimpleElement) currElem;
            String currJaxbClass = simpleCurr.getJaxb();
            if (currJaxbClass == null) {
                // New class doesn't have children, so, have deleted elements which used to come under wrapper
                for (Entry<String, SoapApiNamedElement> entry : baseSubElems.entrySet()) {
                    deletedElems.add(new NamedElem(ancestry.getXpath(), entry.getValue()));
                }
            } else {
                SoapApiType currType = currentTypes.get(currJaxbClass);
                // "base" is a wrapper, so any attributes in "curr" are new
                if (currType.getAttributes() != null) {
                    for (SoapApiAttribute attr : currType.getAttributes()) {
                        newAttrs.add(new NamedAttr(ancestry.getXpath(), attr));
                    }
                }
                Map<String,SoapApiNamedElement> currSubElems = makeSubElemMap(currType.getSubElements());
                findSubElementChanges(ancestry, baseSubElems, currSubElems);
            }
        }
    }

    private Map<String,SoapApiNamedElement> makeSubElemMap(SoapApiType apiType) {
        return makeSubElemMap(apiType.getSubElements());
    }

    /**
     * Convenience method.  Creating the map is slightly complicated by SoapApiElementChoiceInfo pseudo nodes
     * which we can ignore for the purposes of the changelog, but we need to promote their children
     */
    private Map<String,SoapApiNamedElement> makeSubElemMap(Collection<SoapApiElementArtifact> subElems) {
        Map<String,SoapApiNamedElement> elems = Maps.newTreeMap();
        if (subElems != null) {
            for (SoapApiElementArtifact elemArtifact : subElems) {
                if (elemArtifact instanceof SoapApiElementChoiceInfo) {
                    SoapApiElementChoiceInfo choiceNode = (SoapApiElementChoiceInfo) elemArtifact;
                    for (SoapApiElementArtifact subE : choiceNode.getSubElements()) {
                        if (subE instanceof SoapApiNamedElement) {
                            SoapApiNamedElement namedE = (SoapApiNamedElement) subE;
                            elems.put(namedE.getName(), namedE);
                        }
                    }
                } else if (elemArtifact instanceof SoapApiNamedElement) {
                        SoapApiNamedElement namedE = (SoapApiNamedElement) elemArtifact;
                        elems.put(namedE.getName(), namedE);
                }
            }
        }
        return elems;
    }

    public String getName() { return baselineCmd.getName(); }
    public String getNamespace() { return baselineCmd.getNamespace(); }
    public String getDocApiLinkFragment() { return baselineCmd.getDocApiLinkFragment(); }
    public List<NamedAttr> getNewAttrs() { return newAttrs; }
    public List<NamedAttr> getDeletedAttrs() { return deletedAttrs; }
    public List<NamedElem> getNewElems() { return newElems; }
    public List<NamedElem> getDeletedElems() { return deletedElems; }
    public List<AttributeChanges> getModifiedAttrs() { return modifiedAttrs; }
    public List<ElementChanges> getModifiedElements() { return modifiedElements; }
}
