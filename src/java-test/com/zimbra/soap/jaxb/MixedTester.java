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
package com.zimbra.soap.jaxb;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

/**
 * Test JAXB class to exercise a field annotated with {@link XmlMixed} and
 * {@link XmlElementRefs} and {@link XmlElementRef}
 *
 *  Snippet from Java 6 XmlMixed javadocs:
 *
 *    Annotate a JavaBean multi-valued property to support mixed content.
 *
 *    The usage is subject to the following constraints:
 *      # can be used with {@link XmlElementRef}, {@link XmlElementRefs} or {@link XmlAnyElement}
 *
 *    The following can be inserted into {@link XmlMixed} annotated multi-valued property
 *      # XML text information items are added as values of java.lang.String.
 *      # Children element information items are added as instances of JAXBElement or instances with a class that
 *        is annotated with {@link XmlRootElement}.
 *      # Unknown content that is not be bound to a JAXB mapped class is inserted as Element. (Assumes property
 *        annotated with {@link XmlAnyElement})
 *
 * Current usage of XmlMixed in ZimbraSoap JAXB classes:
 *    CommentInfo - List of objects - String maps to the subject text.  MailCustomMetadata maps to &lt;meta>
 *    MailQueueAction - List of objects - String maps to list of ids as text.  QueueQuery maps to &lt;query>
 *    StatsSpec - List of objects - String maps to the subject text.  StatsValueWrapper maps to &lt;values>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="mixed-tester")
public class MixedTester {
    @XmlElementRefs({
        @XmlElementRef(type=StringAttribIntValue.class) /* note: tests out case where name isn't specified here */
    })
    @XmlMixed
    private List<Object> elems = Lists.newArrayList();

    public MixedTester() { }

    public List<Object> getElems() { return elems; }
    public void setElems(List<Object> elems) { this.elems = elems; }
}
