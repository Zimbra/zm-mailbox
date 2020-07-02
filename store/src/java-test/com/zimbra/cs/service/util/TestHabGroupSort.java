package com.zimbra.cs.service.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.soap.account.type.HABGroup;
import com.zimbra.soap.type.ZmBoolean;

import junit.framework.Assert;

/**
 * @author zimbra
 *
 */
public class TestHabGroupSort {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testSort1() {
        HABGroup grp = new HABGroup();
        grp.setName("MyOrg");
        grp.setId("1");
        grp.setRootGroup(ZmBoolean.ONE);

        List<HABGroup> childList1 = new ArrayList<HABGroup>();
        grp.setChildGroups(childList1);

        HABGroup grp1 = new HABGroup();
        grp1.setName("Prod");
        grp1.setId("2");
        grp1.setSeniorityIndex(100);
        childList1.add(grp1);

        HABGroup grp2 = new HABGroup();
        grp2.setName("Sales");
        grp2.setId("3");
        childList1.add(grp2);

        HABGroup grp3 = new HABGroup();
        grp3.setName("Market");
        grp3.setId("4");
        childList1.add(grp3);

        HABGroup grp4 = new HABGroup();
        grp4.setName("QA");
        grp4.setSeniorityIndex(55);
        grp4.setId("5");
        childList1.add(grp4);

        HABGroup grp5 = new HABGroup();
        grp5.setName("Engg");
        grp5.setSeniorityIndex(45);
        grp5.setId("6");
        childList1.add(grp5);

        HABGroup grp7 = new HABGroup();
        grp7.setName("NQA");
        grp7.setId("7");
        childList1.add(grp7);

        HABGroup grp8 = new HABGroup();
        grp8.setName("Dev");
        grp8.setId("8");
        childList1.add(grp8);

        Collections.sort(childList1, new SortBySeniorityIndexThenName());
        System.out.println(childList1);

        Assert.assertEquals("Prod", childList1.get(0).getName());
        Assert.assertEquals("Engg", childList1.get(2).getName());
        Assert.assertEquals("Sales", childList1.get(6).getName());
    }

    @Test
    public void testSort2() {
        HABGroup grp = new HABGroup();
        grp.setName("MyOrg");
        grp.setId("1");
        grp.setRootGroup(ZmBoolean.ONE);

        List<HABGroup> childList1 = new ArrayList<HABGroup>();
        grp.setChildGroups(childList1);

        HABGroup grp1 = new HABGroup();
        grp1.setName("Prod");
        grp1.setId("2");
        grp1.setSeniorityIndex(100);
        childList1.add(grp1);

        HABGroup grp2 = new HABGroup();
        grp2.setName("Sales");
        grp2.setId("3");
        childList1.add(grp2);

        HABGroup grp3 = new HABGroup();
        grp3.setName("Market");
        grp3.setId("4");
        childList1.add(grp3);

        HABGroup grp4 = new HABGroup();
        grp4.setName("QA");
        grp4.setSeniorityIndex(55);
        grp4.setId("5");
        childList1.add(grp4);

        HABGroup grp5 = new HABGroup();
        grp5.setName("Engg");
        grp5.setSeniorityIndex(45);
        grp5.setId("6");
        childList1.add(grp5);

        HABGroup grp7 = new HABGroup();
        grp7.setId("7");
        childList1.add(grp7);

        HABGroup grp8 = new HABGroup();
        grp8.setName("Dev");
        grp8.setId("8");
        childList1.add(grp8);

        Collections.sort(childList1, new SortBySeniorityIndexThenName());

        Assert.assertEquals("Prod", childList1.get(0).getName());
        Assert.assertEquals("Engg", childList1.get(2).getName());
        Assert.assertEquals("Sales", childList1.get(5).getName());
    }

}
