package edu.usc.sql.transform;

import edu.usc.sql.Drawable;
import edu.usc.sql.Item;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

/**
 * Created by mian on 10/27/15.
 */
public class DrawableTransform {
    private final String RESOURCE = "resources";
    public static void main(String args[]) throws IOException, DocumentException {
        DrawableTransform dt = new DrawableTransform();
        String sdkDir = "/Users/mian/Documents/Projects/framework-res";
        String appDir = "/Users/mian/Documents/Projects/TransformPool/abrc.mf.td";
        String outputPath = "/Users/mian/Documents/Projects/newApp";

        String cts = "/Users/mian/Documents/Projects/TransformPool/transform.txt";
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
        dt.getTransform(appDir, switchMap, outputPath);

    }

    public Set<String> getTransform(String rootDir, Map<String, String> colorMap, String outputPath) throws DocumentException, IOException {
        Set<String> drawableSet = new HashSet<String>();

        // Analyze res/values/drawables.xml
        File xml = new File(rootDir + File.separator + "res" + File.separator + "values" + File.separator + "drawables.xml");
        if (xml.exists()) {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(xml);
            Element root = doc.getRootElement();
            if (!root.getName().equals(RESOURCE)) {
                System.err.println("This is not a resource xml file");
                return null;
            }

            Iterator<Element> it = root.elementIterator();

            while (it.hasNext()) {
                Element e = it.next();
                Set<Item> itemSet = new HashSet<Item>();
                if (e.attributeValue("type").equals("drawable")) {
                    String drawableName = e.attributeValue("name"); //getQualifiedName()
                    String value = e.getText();
                    drawableSet.add(drawableName);

                    if (value.startsWith("#")) {
                        String alpha = value.substring(1, 3).toLowerCase();
                        String key = "#" + value.substring(3).toLowerCase();
                        if (colorMap.containsKey(key)) {
                            String transformed = colorMap.get(key);
                            transformed = "#" + alpha + transformed.substring(1);

                            e.setText(transformed);
                        }
                    }
                }
            }

            String alterXmlFolder = outputPath + File.separator +"res" + File.separator + "values";
            File folder = new File(alterXmlFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String alteredColorXml = alterXmlFolder + File.separator + "drawable.xml";
            XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
            writer.write(doc);
            writer.close();

        }

        File folder = new File(rootDir + File.separator + "res" + File.separator + "drawable");
        // Analyze res/drawable folder
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            for (File f : files) {
                String fileName = f.getName();
                if (!fileName.contains(".xml"))
                    continue;
                SAXReader reader4Folder = new SAXReader();
                Document doc4Folder = reader4Folder.read(f);
                Element root4Folder = doc4Folder.getRootElement();

                drawableSet.add(fileName.substring(0, fileName.length() - 4));

                // BFS traverse all the elements
                Queue<Element> queue = new LinkedList<Element>();
                queue.add(root4Folder);


                while (!queue.isEmpty()) {
                    Element e = queue.remove();
                    queue.addAll(e.elements());

                    List<Attribute> list = e.attributes();

                    for (Attribute attr : list) {
                        String attrName = attr.getName(); //getQualifiedName()
                        String value = attr.getValue();
                        if (value.startsWith("#")) {
                            String alpha = value.substring(1, 3).toLowerCase();
                            String key = "#" + value.substring(3).toLowerCase();
                            if (colorMap.containsKey(key)) {
                                String transformed = colorMap.get(key);
                                transformed = "#" + alpha + transformed.substring(1);

                                e.addAttribute(attrName, transformed);
                            }
                        }
                    }
                }


                String alterXmlFolder = outputPath + File.separator +"res" + File.separator + "drawable";
                File outFolder = new File(alterXmlFolder);
                if (!outFolder.exists()) {
                    outFolder.mkdirs();
                }
                String alteredColorXml = alterXmlFolder + File.separator + fileName;
                XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
                writer.write(doc4Folder);
                writer.close();

//                Iterator<Element> it = root.elementIterator();

            }
        }
        return drawableSet;
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
