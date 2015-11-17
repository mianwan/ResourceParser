package edu.usc.sql;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.*;

public class ColorMain {

    private Map<String, ARGB> colorMap = new HashMap<String, ARGB>();
    private Map<String, HashSet<ARGB>> colorSetMap = new HashMap<String, HashSet<ARGB>>();
    private final String RESOURCE = "resources";
    private final String COLOR = "color";
    private final String SELECTOR = "selector";
    private final String ITEM = "item";

//    public static void main(String[] args) throws FileNotFoundException {
//        // write your code here
//        String rootDir = "/Users/mian/Documents/Projects/framework-res";
//        String output = "/Users/mian/Desktop/color.txt";
//        parseDrawableDefinition(rootDir);
//    }

    public Map<String, ARGB> getColorMap() {
        return colorMap;
    }

    public void setColorMap(Map<String, ARGB> colorMap) {
        this.colorMap = colorMap;
    }

    public Map<String, HashSet<ARGB>> getColorSetMap() {
        return colorSetMap;
    }

    public void setColorSetMap(Map<String, HashSet<ARGB>> colorSetMap) {
        this.colorSetMap = colorSetMap;
    }

    public void parseColorDefinition(String rootDir) { //, String outputPath
        SAXReader reader = new SAXReader();
        File xml = new File(rootDir +"/res/values/colors.xml");
        Map<String, String> cacheMap = new HashMap<String, String>();
        try {
            // In case apps don't have this file
            if (xml.exists()) {
                Document doc = reader.read(xml);
                Element root = doc.getRootElement();
                if (!root.getName().equals(RESOURCE)) {
                    System.err.println("This is not a resource xml file");
                    return;
                }

                Iterator<Element> it = root.elementIterator();

                // --------------Phase 1: define color directly using #AARRGGBB----------
                while (it.hasNext()) {
                    Element entry = it.next();
                    if (entry.getName().equals(COLOR)) {
                        String colorName = entry.attributeValue("name");
                        String value = entry.getText();
                        ARGB color = null;
                        //                    System.out.println(colorName);
                        // this is a simple definition
                        if (value.startsWith("#")) {
                            color = new ARGB(value);
                            colorMap.put(colorName, color);
                            // cache this referred color
                        } else if (value.startsWith("@color")) {
                            String referee = value.split("/")[1];
                            cacheMap.put(colorName, referee);
                        }
                    }

                }

                int beforeSize = 0;
                int afterSize = 0;
                // -----------------Phase 2: Parse the referred color in this color.xml-------------
                do {
                    Set<String> foundColorSet = new HashSet<String>();
                    for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                        String srcColor = entry.getKey();
                        String targetColor = entry.getValue();
                        ARGB foundColor = colorMap.get(targetColor);
                        if (foundColor != null) {
                            colorMap.put(srcColor, foundColor);
                            foundColorSet.add(srcColor);
                        }
                    }
                    beforeSize = cacheMap.size();
                    // Remove color already founded
                    if (foundColorSet.size() > 0) {
                        for (String key : foundColorSet) {
                            cacheMap.remove(key);
                        }
                    }
                    afterSize = cacheMap.size();
                    //                System.out.println(beforeSize - afterSize);
                } while (beforeSize - afterSize > 0);
            }

            // -------------------- Phase 3: Parse color defined in XML ----------------------------
            File folder = new File(rootDir + "/res/color/");
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                for (File f : files) {
                    String fileName = f.getName();
                    if (!fileName.contains(".xml"))
                        continue;
                    String srcColor = fileName.substring(0, fileName.length() - ".xml".length());
//                    System.out.println("src:" + srcColor);
                    SAXReader subReader = new SAXReader();
                    Document subDoc = subReader.read(f);
                    Element subRoot = subDoc.getRootElement();

                    Iterator<Element> subIt = subRoot.elementIterator();
                    HashSet<ARGB> colorSet = new HashSet<ARGB>();
                    while (subIt.hasNext()) {
                        Element subEle = subIt.next();
                        if (subEle.getName().equals(ITEM)) {
                            String refereeColor = subEle.attributeValue("color");
//                            System.out.println("to: " + refereeColor);
                            if (refereeColor.contains("@color")) {
                                String subKey = refereeColor.split("/")[1];
                                ARGB subColor = colorMap.get(subKey);
                                if (subColor != null) {
                                    colorSet.add(subColor);
                                } else {
                                    throw new RuntimeException("Cannot find color!");
                                }
                            } else {
                                ARGB color = new ARGB(refereeColor);
                                colorSet.add(color);
                            }
                        }
                    }
//                    System.out.println(srcColor + "\t" + colorSet);
                    colorSetMap.put(srcColor, colorSet);
                }

                // -----------Phase 4: Parse the referred color in /color folder(a.k.a <selector>)------------
                if (cacheMap.size() > 0) {
                    for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                        String srcColor = entry.getKey();
                        String targetColor = entry.getValue();

                        HashSet<ARGB> colorSet = colorSetMap.get(targetColor);
                        if (colorSet == null) {
                            throw new RuntimeException("Referring an undefined color.");
                        } else {
//                            System.out.println(srcColor + "->" + colorSet);
                            colorSetMap.put(srcColor, colorSet);
                        }// refer to previous defined color
                    }
                }
                System.out.println(colorMap.size());
                System.out.println(colorSetMap.size());
            }

//            try {
//                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputPath)));
//                for (Map.Entry<String, ARGB> entry: colorMap.entrySet()) {
//                    bw.write(entry.getKey() + "\t" + entry.getValue());
//                    bw.write("\n");
//                }
//                bw.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }


        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

    public void parseColorDefinition(String rootDir, Map<String, ARGB> knownColorMap, Map<String, HashSet<ARGB>> knownColorSetMap) {
        SAXReader reader = new SAXReader();
        File xml = new File(rootDir + File.separator + "res" + File.separator + "values" + File.separator + "colors.xml");
        Map<String, String> cacheMap = new HashMap<String, String>();
        try {
            // In case apps don't have this file
            if (xml.exists()) {
                Document doc = reader.read(xml);
                Element root = doc.getRootElement();
                if (!root.getName().equals(RESOURCE)) {
                    System.err.println("This is not a resource xml file");
                    return;
                }

                Iterator<Element> it = root.elementIterator();

                // --------------Phase 1: define color directly using #AARRGGBB----------
                while (it.hasNext()) {
                    Element entry = it.next();
                    if (entry.getName().equals(COLOR)) {
                        String colorName = entry.attributeValue("name");
                        String value = entry.getText();
                        ARGB color = null;
                        //                    System.out.println(colorName);
                        // this is a simple definition
                        if (value.startsWith("#")) {
                            color = new ARGB(value);
                            colorMap.put(colorName, color);
                            // cache this referred color
                        } else if (value.startsWith("@color")) {
                            String referee = value.split("/")[1];
                            cacheMap.put(colorName, referee);
                        } else if (value.startsWith("@android:color")) {
                            String key = value.split("/")[1];
                            if (knownColorMap.containsKey(key)) {
                                colorMap.put(colorName, knownColorMap.get(key));
                            } else {
                                colorSetMap.put(colorName, knownColorSetMap.get(key));
                            }
                        }
                    }

                }

                int beforeSize = 0;
                int afterSize = 0;
                // -----------------Phase 2: Parse the referred color in this color.xml-------------
                do {
                    Set<String> foundColorSet = new HashSet<String>();
                    for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                        String srcColor = entry.getKey();
                        String targetColor = entry.getValue();
                        ARGB foundColor = colorMap.get(targetColor);
                        if (foundColor != null) {
                            colorMap.put(srcColor, foundColor);
                            foundColorSet.add(srcColor);
                        }
                    }
                    beforeSize = cacheMap.size();
                    // Remove color already founded
                    if (foundColorSet.size() > 0) {
                        for (String key : foundColorSet) {
                            cacheMap.remove(key);
                        }
                    }
                    afterSize = cacheMap.size();
                    //                System.out.println(beforeSize - afterSize);
                } while (beforeSize - afterSize > 0);
            }

            // -------------------- Phase 3: Parse color defined in XML ----------------------------
            File folder = new File(rootDir + "/res/color/");
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                for (File f : files) {
                    String fileName = f.getName();
                    if (!fileName.contains(".xml"))
                        continue;
                    String srcColor = fileName.substring(0, fileName.length() - ".xml".length());
//                    System.out.println("src:" + srcColor);
                    SAXReader subReader = new SAXReader();
                    Document subDoc = subReader.read(f);
                    Element subRoot = subDoc.getRootElement();

                    Iterator<Element> subIt = subRoot.elementIterator();
                    HashSet<ARGB> colorSet = new HashSet<ARGB>();
                    while (subIt.hasNext()) {
                        Element subEle = subIt.next();
                        if (subEle.getName().equals(ITEM)) {
                            String refereeColor = subEle.attributeValue("color");
//                            System.out.println("to: " + refereeColor);
                            if (refereeColor.contains("@color")) {
                                String subKey = refereeColor.split("/")[1];
                                ARGB subColor = colorMap.get(subKey);
                                if (subColor != null) {
                                    colorSet.add(subColor);
                                } else {
                                    throw new RuntimeException("Cannot find color!");
                                }
                            } else {
                                ARGB color = new ARGB(refereeColor);
                                colorSet.add(color);
                            }
                        }
                    }
//                    System.out.println(srcColor + "\t" + colorSet);
                    colorSetMap.put(srcColor, colorSet);
                }

                // -----------Phase 4: Parse the referred color in /color folder(a.k.a <selector>)------------
                if (cacheMap.size() > 0) {
                    for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                        String srcColor = entry.getKey();
                        String targetColor = entry.getValue();

                        HashSet<ARGB> colorSet = colorSetMap.get(targetColor);
                        if (colorSet == null) {
                            throw new RuntimeException("Referring an undefined color.");
                        } else {
                            System.out.println(srcColor + "->" + colorSet);
                            colorSetMap.put(srcColor, colorSet);
                        }// refer to previous defined color
                    }
                }
                System.out.println(colorMap.size());
                System.out.println(colorSetMap.size());
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}
