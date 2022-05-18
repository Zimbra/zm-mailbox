/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.doc.soap;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.doc.soap.apidesc.SoapApiCommand;
import com.zimbra.doc.soap.apidesc.SoapApiDescription;
import com.zimbra.doc.soap.changelog.AttributeChanges;
import com.zimbra.doc.soap.changelog.CommandChanges;
import com.zimbra.doc.soap.changelog.CommandChanges.NamedAttr;
import com.zimbra.doc.soap.changelog.CommandChanges.NamedElem;
import com.zimbra.doc.soap.changelog.ElementChanges;
import com.zimbra.doc.soap.changelog.SoapApiChangeLog;
import com.zimbra.soap.type.ZmBoolean;

public class ChangelogTest {

    private static final Logger LOG = LogManager.getLogger(ChangelogTest.class);

    static {
        Configurator.reconfigure();
        Configurator.setRootLevel(Level.INFO);
        Configurator.setLevel(LOG.getName(), Level.INFO);
    }

    @BeforeClass
    public static void init() throws Exception {
    }

    @XmlRootElement(name="aRequest")
    public class aRequest {
        @XmlAttribute(name="attribute1", required=false)
        private String attribute1;
        
        @XmlAttribute(name="attribute2", required=false)
        private String attribute2;
        
        @XmlElement(name="element", required=false)
        private aElem element;
        
        public aRequest() {
        }

        public String getAttribute1() { return attribute1; }
        public void setAttribute1(String attribute1) { this.attribute1 = attribute1; }
        public String getAttribute2() { return attribute2; }
        public void setAttribute2(String attribute2) { this.attribute2 = attribute2; }
        public aElem getElement() { return element; }
        public void setElement(aElem element) { this.element = element; }
    }

    @XmlRootElement(name="aResponse")
    public class aResponse {
        @XmlElement(name="an-elem", required=false)
        private ZmBoolean anElem;

        public ZmBoolean getAnElem() { return anElem; }
        public void setAnElem(ZmBoolean anElem) { this.anElem = anElem; }
    }

    @XmlRootElement(name="bRequest")
    public class bRequest {
    }

    @XmlRootElement(name="bResponse")
    public class bResponse {
    }

    @XmlRootElement(name="cRequest")
    public class cRequest {
    }

    @XmlRootElement(name="cResponse")
    public class cResponse {
    }

    public class aElem {
        @XmlAttribute(name="long-attribute", required=false)
        private Long longAttribute;
        @XmlAttribute(name="int-attribute", required=true)
        private int intAttribute;
        @XmlValue
        int value;
        public Long getLongAttribute() { return longAttribute; }
        public void setLongAttribute(Long longAttribute) { this.longAttribute = longAttribute; }
        public int getIntAttribute() { return intAttribute; }
        public void setIntAttribute(int intAttribute) { this.intAttribute = intAttribute; }
    }

    @Test
    public void makeChangelogTest()
    throws Exception {
        Map<String, ApiClassDocumentation> javadocInfo = Maps.newTreeMap();
        List<Class<?>> classes = Lists.newArrayList();
        classes.add(aRequest.class);
        classes.add(aResponse.class);
        classes.add(bRequest.class);
        classes.add(bResponse.class);
        classes.add(cRequest.class);
        classes.add(cResponse.class);
        Root soapApiDataModelRoot = WsdlDocGenerator.processJaxbClasses(javadocInfo, classes);
        SoapApiDescription jsonDescCurrent = new SoapApiDescription("7.99.99", "20000131-2359");
        jsonDescCurrent.build(soapApiDataModelRoot);
        // File json = new File("/tmp/test1.json");
        // jsonDescCurrent.serializeToJson(json);
        InputStream is = getClass().getResourceAsStream("baseline1.json");
        SoapApiDescription jsonDescBaseline = SoapApiDescription.deserializeFromJson(is);
        SoapApiChangeLog clog = new SoapApiChangeLog();
        clog.setBaselineDesc(jsonDescBaseline);
        clog.setCurrentDesc(jsonDescCurrent);
        clog.makeChangeLogDataModel();
        List<SoapApiCommand> newCmds = clog.getNewCommands();
        LOG.info("    New Command:" + newCmds.get(0).getName());
        List<SoapApiCommand> delCmds = clog.getDeletedCommands();
        LOG.info("    Deleted Command:" + delCmds.get(0).getName());
        List<CommandChanges> modCmds = clog.getModifiedCommands();
        CommandChanges modCmd = modCmds.get(0);
        LOG.info("    Modified Command:" + modCmd.getName());
        List<NamedAttr> delAttrs = modCmd.getDeletedAttrs();
        for (NamedAttr attr : delAttrs) {
            LOG.info("    Deleted Attribute:" + attr.getXpath());
        }
        List<NamedAttr> addAttrs = modCmd.getNewAttrs();
        for (NamedAttr attr : addAttrs) {
            LOG.info("    Added Attribute:" + attr.getXpath());
        }
        List<AttributeChanges> modAttrs = modCmd.getModifiedAttrs();
        for (AttributeChanges modAttr : modAttrs) {
            LOG.info("    Modified Attribute " + modAttr.getXpath() +":\nbase=" + modAttr.getBaselineRepresentation() +
                    "\ncurr=" + modAttr.getCurrentRepresentation());
        }
        List<NamedElem> delEs = modCmd.getDeletedElems();
        for (NamedElem el : delEs) {
            LOG.info("    Deleted Element :" + el.getXpath());
        }
        List<NamedElem> newEs = modCmd.getNewElems();
        for (NamedElem el : newEs) {
            LOG.info("    New Element :" + el.getXpath());
        }
        List<ElementChanges> modEs = modCmd.getModifiedElements();
        for (ElementChanges el : modEs) {
            LOG.info("    Modified Element " + el.getXpath() +":\nbase=" + el.getBaselineRepresentation() +
                    "\ncurr=" + el.getCurrentRepresentation());
        }
        Assert.assertEquals("Number of new commands", 1, newCmds.size());
        Assert.assertEquals("Number of deleted commands", 1, delCmds.size());
        Assert.assertEquals("Number of modified commands", 1, modCmds.size());
        Assert.assertEquals("Number of deleted attributes", 1, delAttrs.size());
        Assert.assertEquals("Number of new attributes", 1, addAttrs.size());
        Assert.assertEquals("Number of modified attributes", 1, modAttrs.size());
        Assert.assertEquals("Number of deleted elements", 1, delEs.size());
        Assert.assertEquals("Number of new elements", 1, newEs.size());
        Assert.assertEquals("Number of modified elements", 1, modEs.size());
    }
}
