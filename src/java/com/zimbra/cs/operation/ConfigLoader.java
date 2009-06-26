/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.io.File;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Load scheduler configuration data from "schedconfig.xml".  This file is loaded once
 * at system startup and the settings are used to control the prioritization/throttling
 * subsystem.
 * 
 * 
 *  The schedconfig.xml file should be formatted like this:
 *  
 *  <scheduler>
 *     <config schedulers="NUM_SCHEDULERS" maxops="MAXOPS" maxload="MAXLOAD">
 *         <maxload pri="PRIORITY" load="MAXLOAD"/>
 *          ...(1 for each priority level)...
 *      </config>
 *      
 *      NUM_SCHEDULERS is the number of Scheduler objects.  If it turns out that contention in 
 *                  the Scheduler is a systemwide bottleneck, then increase the # schedulers here
 *                  (Ops are routed to a specific scheduler based on the ID of the target mailbox)
 *                  Note that the maxops and maxload values should be lowered if the # schedulers
 *                  is increased.
 *                  
 *      MAXOPS is the maximum # simultaneous ops allowed to be running at one time.  This value
 *                allows some coarse-grained concurrency control.
 *                
 *      MAXLOAD: Tells the system the cutoff load level (where system load is the sum of the 
 *                 load of all the running ops) above which the system will no longer schedule 
 *                 operations.
 *                
 *      <op name="CLASSNAME" load="LOAD" scale="SCALE" maxload="MAXLOAD"/>
 *      ......(many ops).....
 *      
 *      CLASSNAME: the java classname, either a simple classname (e.g. GetFolderOperation)
 *               or a fully qualified classname (e.g. com.zimbra.cs.operation.GetFolderOperation)
 *               The server will check for both.
 *               
 *       LOAD: the load value of this operation
 *       
 *       SCALE: some operations take multiple targets.  Those operations calculate their load
 *               value by scaling it up proportionally to the # operations passed.  The effective
 *               load is calculated by the formula:  Math.min((LOAD * (numTargets/SCALE)), MAXLOAD)
 *                
 *       MAXLOAD: if the effective load of this operation is scaled up, this maximum is a cutoff
 *  
 *  </scheduler>
 *
 */
public class ConfigLoader {
    private static final String E_SCHEDULER = "scheduler";

    private static final String E_CONFIG = "config";
    private static final String A_MAX_LOAD = "maxload";
    private static final String A_LOAD = "load";
    private static final String A_MAX_OPS = "maxops";
    private static final String E_OP = "op";
    private static final String A_NAME = "name";
    private static final String A_SCALE = "scale";

    private static String defaultConfigFile() {
        String zmHome = System.getProperty("zimbra.home");
        if (zmHome == null) {
            zmHome = File.separator + "opt" + File.separator + "zimbra";
        }
        return zmHome + File.separator + "conf" + File.separator + "schedconfig.xml";
    }

    public static void loadConfig() throws ServiceException  {
        String configFile = defaultConfigFile();

        try {
            File cf = new File(configFile);
            if (cf.exists() && cf.canRead()) {
                SAXReader reader = com.zimbra.common.soap.Element.getSAXReader();
                Document document = reader.read(cf);
                Element root = document.getRootElement();

                if (!root.getName().equals(E_SCHEDULER))
                    throw new DocumentException("config file " + configFile + " root tag is not " + E_SCHEDULER);

//                Element eConfig = root.element(E_CONFIG);
//                if (eConfig != null) {
//                    int maxLoad = getAttrAsInt(eConfig, A_MAX_LOAD, 10000); 
//                    int maxOps = getAttrAsInt(eConfig, A_MAX_OPS, 25);
//
//                    Scheduler.setSchedulerParams(maxLoad, new int[] {10000,10000,10000,10000,10000});
//                }

                for (Iterator iter = root.elementIterator(E_OP); iter.hasNext();) {
                    Element e = (Element) iter.next();

                    String name = e.attributeValue(A_NAME);
                    if (name == null || name.length() ==0)  {
                        ZimbraLog.system.warn("Operation ConfigLoader - cannot read name attribute for element " + e.toString());
                    } else {
                        int load = getAttrAsInt(e, A_LOAD, -1);
                        int maxLoad =  getAttrAsInt(e, A_MAX_LOAD, -1);
                        int scale =  getAttrAsInt(e, A_SCALE , -1);

                        Operation.Config newConfig = new Operation.Config();
                        newConfig.mLoad = load;
                        newConfig.mScale = scale;
                        newConfig.mMaxLoad = maxLoad;

//                      updateOp(name, load, maxLoad, scale);
                        if (Operation.mConfigMap.containsKey(name))
                            Operation.mConfigMap.remove(name);
                        Operation.mConfigMap.put(name, newConfig);
                    }
                }
            } else {
                ZimbraLog.system.warn("Operation ConfigLoader: local config file `" + cf + "' is not readable");
            }
        } catch (DocumentException e) {
            throw ServiceException.FAILURE("Caught document exception loading Operation Config file: "+configFile, e);
        }	
    }

    private static int getAttrAsInt(Element e, String attName, int defaultValue) {
        String s = e.attributeValue(attName, null);
        if (s == null)
            return defaultValue;

        return Integer.parseInt(s);
    }
}
