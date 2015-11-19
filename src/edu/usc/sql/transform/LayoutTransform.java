package edu.usc.sql.transform;

import edu.usc.sql.ARGB;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

/**
 * Created by mian on 10/29/15.
 */
public class LayoutTransform {
    private final String StylePrefix = "";
    private final String ColorPrefix = "nyx_";
    public static void main(String args[]) {
        LayoutTransform lt = new LayoutTransform();
        String appDir = "/Users/mian/Desktop/Projects/TransformPool/abrc.mf.td";
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

        try {
            lt.getTransform(appDir, switchMap, outputPath);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getTransform(String appDir, Map<String, String> colorMap, String outputPath) throws DocumentException, IOException {
        File folder = new File(appDir + "/res/layout/");
        Set<String> sdkStyles = new HashSet<String>();

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            for (File f : files) {
                String fileName = f.getName();
                if (!fileName.contains(".xml"))
                    continue;
                SAXReader reader = new SAXReader();
                Document doc = reader.read(f);
                Element root = doc.getRootElement();


                // BFS traverse all the elements
                Queue<Element> queue = new LinkedList<Element>();
                queue.add(root);

                while (!queue.isEmpty()) {
                    Element e = queue.remove();
                    queue.addAll(e.elements());

                    List<Attribute> list = e.attributes();

                    for (Attribute attr : list) {
                        String oldvalue = attr.getValue();

                        if (oldvalue.startsWith("@android:style")){
                            sdkStyles.add(oldvalue);
                            String name = oldvalue.split("/")[1];
                            name = StylePrefix + name;
                            String value = "@style/" + name;
                            System.out.println(oldvalue);

                            System.out.println(value);
                            //attr.setValue(value);
                        } else if (oldvalue.startsWith("@android:color")) {
                            String name = oldvalue.split("/")[1];
                            name = ColorPrefix + name;
                            String value = "@color/" + name;
                            attr.setValue(value);
                        } else if (oldvalue.contains("#")) {
                            String alpha = oldvalue.substring(1, 3).toLowerCase();
                            String key = "#" + oldvalue.substring(3).toLowerCase();
                            if (colorMap.containsKey(key)) {
                                String transformed = colorMap.get(key);
                                transformed = "#" + alpha + transformed.substring(1);

                                attr.setValue(transformed);
                            }
                        }
                    }

                    /**************
                     * Temp rule for ListView
                     */
                    if (e.getName().equals("TextView") && e.attributeValue("textColor") == null) {
                        e.addAttribute("android:textColor", "#ffffffff");
                    }
                    if (e.getName().equals("EditText") && e.attributeValue("textColor") == null) {
                        e.addAttribute("android:textColor", "#ffffffff");
                    }
                    if (e.getName().equals("LinearLayout") && e.attributeValue("background") == null) {
                        if (e.attributeValue("style") != null) {
                            if (e.attributeValue("style") .toLowerCase().contains("actionbar")) {
                                e.addAttribute("android:background", "#ffffffff");
                            } else {
                                e.addAttribute("android:background", "#ff000000");
                            }

                        } else {
                            e.addAttribute("android:background", "#ff000000");
                        }
                    }
                }

                String alterXmlFolder = outputPath + File.separator +"res" + File.separator + "layout";
                File layoutFolder = new File(alterXmlFolder);
                if (!layoutFolder.exists()) {
                    layoutFolder.mkdirs();
                }
                String alteredColorXml = alterXmlFolder + File.separator + fileName;
                XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
                writer.write(doc);
                writer.close();
            }
        }
        return sdkStyles;
    }


}
