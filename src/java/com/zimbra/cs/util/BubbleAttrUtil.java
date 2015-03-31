package com.zimbra.cs.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
/***
 * 
 * @author sankumar
 * Utility class created to bubble up the existing server attributes in zimbra-attrs.xml to cluster
 * After the conversion replace zimbra-attrs with output file and submit the changelist
 */
public class BubbleAttrUtil {
    private static String A_NAME = "name";
    private static String A_FLAGS = "flags";
    private static String A_OPTIONA_IN = "optionalIn";
    private static String O_ALWAYS_ON_CLUSTER = "alwaysOnCluster";
    private static String F_SERVER_PREFER_ALWAYSON = "serverPreferAlwaysOn";

    public static void main(String[] args) {
        List<String> bubbleAttrList = loadBubbleAttributeList();
        String zimbraAttrPathName = "conf/attrs/zimbra-attrs.xml";
        File masterZimbraAttrFile = new File(zimbraAttrPathName);
        SAXReader reader = new SAXReader();
        FileWriter fw = null;
        Document doc;
        try {
            doc = reader.read(masterZimbraAttrFile);
            Element root = doc.getRootElement();
            String name;
            String flags;
            String optionalIn;
            for (Iterator<Element> iter = root.elementIterator(); iter.hasNext();) {
                Element eattr = (Element) iter.next();
                name = eattr.attributeValue(A_NAME);
                if (bubbleAttrList.contains(name)) {
                    System.out.println("Processing element : " + name);
                    optionalIn = eattr.attributeValue(A_OPTIONA_IN);
                    if (!optionalIn.contains(O_ALWAYS_ON_CLUSTER)) {
                        optionalIn = optionalIn + "," + O_ALWAYS_ON_CLUSTER;
                        eattr.addAttribute(A_OPTIONA_IN, optionalIn);
                    } else {
                        System.out.println("Element already has " + O_ALWAYS_ON_CLUSTER + " value in optionalIn. Skipping");
                    }
                    flags = eattr.attributeValue(A_FLAGS);
                    if (null == flags) {
                        System.out.println("No flags attribute on element : " + name);
                        eattr.addAttribute(A_FLAGS, F_SERVER_PREFER_ALWAYSON);
                        flags = F_SERVER_PREFER_ALWAYSON;
                    }
                    if (!flags.contains(F_SERVER_PREFER_ALWAYSON)) {
                        flags = flags + "," + F_SERVER_PREFER_ALWAYSON;
                        eattr.addAttribute(A_FLAGS, flags);
                    } else {
                        System.out.println("Element already has "+ F_SERVER_PREFER_ALWAYSON + " flag. Skipping");
                    }
                }
            }

            String outputDirName = "target/alwaysonattr";
            File outputDir = new File(outputDirName);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
                System.out.println("Created dir:" + outputDir.getAbsolutePath());
            }
            String zimbraAttrOutputPathName = outputDirName + "/zimbra-attrs.xml";
            File output = new File(zimbraAttrOutputPathName);
            fw = new FileWriter(output);
            fw.write(doc.asXML());
            System.out.println("Output written to file : " + zimbraAttrOutputPathName);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fw) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static List<String> loadBubbleAttributeList() {
        String eligibleAttributeFileName = "src/java/com/zimbra/cs/util/BubbleServerAttributes.txt";
        File attrFile = new File(eligibleAttributeFileName);
        List<String> attrList = new ArrayList<String>();
        try (BufferedReader br = new BufferedReader(new FileReader(attrFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                attrList.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return attrList;
    }

}
