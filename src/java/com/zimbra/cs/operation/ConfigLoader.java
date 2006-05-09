package com.zimbra.cs.operation;

import java.io.File;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public class ConfigLoader {
	private static final String E_SCHEDULER = "scheduler";
	
	private static final String E_CONFIG = "config";
	private static final String A_MAX_LOAD = "maxload";
	private static final String A_MAX_OPS = "maxops";
	
	private static final String E_OP = "op";
	private static final String A_NAME = "name";
	private static final String A_LOAD = "load";
	private static final String A_SCALE = "scale";
	
	private static String defaultConfigFile() {
		String zmHome = System.getProperty("zimbra.home");
		if (zmHome == null) {
			zmHome = File.separator + "opt" + File.separator + "zimbra";
		}
		return zmHome + File.separator + "conf" + File.separator + "opconfig.xml";
	}
	
	public static void loadConfig() throws ServiceException  {
		String configFile = defaultConfigFile();
			
		try {
			File cf = new File(configFile);
			if (cf.exists() && cf.canRead()) {
				SAXReader reader = new SAXReader();
				Document document = reader.read(cf);
				Element root = document.getRootElement();
				
				if (!root.getName().equals(E_SCHEDULER))
					throw new DocumentException("config file " + configFile + " root tag is not " + E_SCHEDULER);
				
				
				Element eConfig = root.element(E_CONFIG);
				if (eConfig != null) {
					int targetLoad = 50;
					int maxOps = 20;
					
					targetLoad = getAttrAsInt(eConfig, A_MAX_LOAD, 50);
					maxOps = getAttrAsInt(eConfig, A_MAX_OPS, 25);
					
					Scheduler.setSchedulerParams(targetLoad, maxOps);
				}
				
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
						
//						updateOp(name, load, maxLoad, scale);
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
	
//	private static void updateOp(String name, int load, int maxLoad, int scale) throws ServiceException {
//		ZimbraLog.system.info("Setting config for Operation: "+name+"("+load+","+maxLoad+","+scale+")");
//		if (name.indexOf('.') < 0)
//			name = "com.zimbra.cs.operation."+name;
//		
//		Class c = null;
//		try {
//			c = Class.forName(name);
//			
//			Class[] paramTypes = new Class[] { Integer.TYPE, Integer.TYPE, Integer.TYPE };
//			Method init = null;
//			try {
//				init = c.getDeclaredMethod("init", paramTypes);
//			} catch (NoSuchMethodException e) { 
//				ZimbraLog.system.info("Operation Config File specified settings for "+name+" but no init() method found in class");
//			}
//			if (init != null) {
//				Object[] arguments = new Object[] { load, maxLoad, scale };
//				try {
//					init.invoke(null, arguments);
//				} catch (IllegalAccessException e) {
//					throw ServiceException.FAILURE("IllegalAccesException for class "+name, e);
//				} catch (InvocationTargetException e) {
//					throw ServiceException.FAILURE("TargetException for class "+name, e);
//				}
//			}
//		} catch (ClassNotFoundException e) {
//			ZimbraLog.system.info("Could not find class for '"+name+"' trying to set Operation config.  Ignoring");
//		}
//	}
}
