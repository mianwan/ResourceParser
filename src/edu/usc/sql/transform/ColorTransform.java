package edu.usc.sql.transform;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by mian on 10/27/15.
 */
public class ColorTransform {
    private final String RESOURCE = "resources";
    private final String COLOR = "color";
    private final String ITEM = "item";
    private final String NewPrefix = "nyx_";

    public static void main(String args[]) {
        ColorTransform ct = new ColorTransform();
        String sdkDir = "/Users/mian/Documents/Projects/framework-res";
        String appDir = "/Users/mian/Documents/Projects/TransformPool/abrc.mf.td";
        String cts = "/Users/mian/Documents/Projects/TransformPool/transform.txt";
        String outputPath = "/Users/mian/Documents/Projects/newApp";
        Map<String, String> switchMap = new HashMap<String, String>();

        try {
            FileReader fr = new FileReader(cts);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                String origin = line.split(",")[0];
                String target = line.split(",")[1];
                switchMap.put(origin, target);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            ct.getTransform(appDir, switchMap, outputPath);
            ct.getTransform(sdkDir, switchMap, outputPath);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getTransform(String rootDir, Map<String, String> colorMap, String outputPath) throws DocumentException, IOException {
        String colorFile = "colors.xml";
        String path = rootDir + File.separator + "res" + File.separator + "values" +
                File.separator + colorFile;
        SAXReader reader = new SAXReader();
        File colorXml = new File(path);
        boolean isSDK = false;
        if (rootDir.contains("framework-res")) {
            isSDK = true;
        }

        // Replace color definition in colors.xml
        if (colorXml.exists()) {
            Document doc = reader.read(colorXml);
            Element root = doc.getRootElement();
            if (!root.getName().equals(RESOURCE)) {
                System.err.println("This is not a resource xml file");
                return;
            }

            Iterator<Element> it = root.elementIterator();

            while (it.hasNext()) {
                Element e = it.next();
                if (e.getName().equals(COLOR)) {
                    String colorName = e.attributeValue("name");
                    String value = e.getText();

                    // this is a simple definition
                    if (value.startsWith("#")) {
                        String alpha = value.substring(1, 3).toLowerCase();
                        String key = "#" + value.substring(3).toLowerCase();
                        if (colorMap.containsKey(key)) {
                            String transformed = colorMap.get(key);
                            transformed = "#" + alpha + transformed.substring(1);

                            e.setText(transformed);
                        }
                    }

                    if (isSDK) {
                        e.addAttribute("name", NewPrefix + colorName);
                        if (value.startsWith("@color")) {
                            e.setText("@color/" + NewPrefix + value.split("/")[1]);
                        }
                    }
                }

            }

            if (isSDK) {
                colorFile = "sdk" + colorFile;
            }

            String alterXmlFolder = outputPath + File.separator +"res" + File.separator + "values";
            File folder = new File(alterXmlFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String alteredColorXml = alterXmlFolder + File.separator + colorFile;
            XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
            writer.write(doc);
            writer.close();

        }

        // Replace color definitions in /color folder
        File folder = new File(rootDir + File.separator +"res" + File.separator + "color");
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            for (File f : files) {
                String fileName = f.getName();
                if (!fileName.contains(".xml"))
                    continue;

                SAXReader subReader = new SAXReader();
                Document subDoc = subReader.read(f);
                Element subRoot = subDoc.getRootElement();

                Iterator<Element> subIt = subRoot.elementIterator();

                while (subIt.hasNext()) {
                    Element subEle = subIt.next();
                    if (subEle.getName().equals(ITEM)) {
                        String refereeColor = subEle.attributeValue("color");
//                            System.out.println("to: " + refereeColor);
                        if (refereeColor.startsWith("#")) {
                            String alpha = refereeColor.substring(1, 3).toLowerCase();
                            String key = "#" + refereeColor.substring(3).toLowerCase();
                            if (colorMap.containsKey(key)) {
                                String transformed = colorMap.get(key);
                                transformed = "#" + alpha + transformed.substring(1);
                                subEle.addAttribute("color", transformed);
                            }
                        } else if (refereeColor.startsWith("@color/")) {
                            if (rootDir.contains("framework-res")) {
                                subEle.addAttribute("color", refereeColor.replace("/", "/" + NewPrefix));
                            }
                        }
                    }
                }

                if (isSDK) {
                    fileName = NewPrefix + fileName;
                }

                String alterXmlFolder = outputPath + File.separator +"res" + File.separator + "color";
                File outFolder = new File(alterXmlFolder);
                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }
                String alteredColorXml = alterXmlFolder + File.separator + fileName;
                XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
                writer.write(subDoc);
                writer.close();


            }
        }

    }

    public long getNewColor (long color) {
        long alpha = color & 0xff000000;
        long rgb = color & 0xffffff;
        long invert = 0xffffff - rgb;
        long newColor = alpha + invert;
        return newColor;
    }

    public String color2String(long color) {
        String str;
        if ((color & 0xff000000) == 0) {
            str = "#00" + Long.toHexString(color);
        } else if ((color & 0xff000000) < 0x10000000) {
            str = "#0" + Long.toHexString(color);
        } else {
            str = "#" + Long.toHexString(color);
        }
        return str;
    }

}
