package edu.usc.sql.transform;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.util.*;

/**
 * Created by mian on 10/29/15.
 */
public class StyleTransform {
    private final String RESOURCE = "resources";
    private final String StylePrefix = "";
    private final String ColorPrefix = "nyx_";

    public static void main(String args[]) throws DocumentException, IOException {

        String sdkDir = "/home/mianwan/Desktop/4.3/framework-res";
        String appDir = "/home/mianwan/Desktop/backup/com.financial.calculator";
        String outputPath = "/Users/mian/Documents/Projects/newApp";

        String cts = "/home/mianwan/Desktop/backup/transform.txt";

        StyleTransform st = new StyleTransform();
        st.transformAll(appDir, sdkDir, cts, appDir);

    }

    public void transformAll(String appDir, String sdkDir, String ctsPath, String outputPath) throws IOException, DocumentException {
        Map<String, String> switchMap = new HashMap<String, String>();

        try {
            FileReader fr = new FileReader(ctsPath);
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

        Set<String> sdkStyleSet = new HashSet<String>();

        ColorTransform ct = new ColorTransform();
        ct.getTransform(appDir, switchMap, outputPath);
        ct.getTransform(sdkDir, switchMap, outputPath);

        DrawableTransform dt = new DrawableTransform();
        dt.getTransform(appDir, switchMap, outputPath);

        LayoutTransform lt = new LayoutTransform();
        sdkStyleSet.addAll(lt.getTransform(appDir, switchMap, outputPath));

        ManifestTransform mt = new ManifestTransform();
        sdkStyleSet.addAll(mt.getTransform(appDir, outputPath));

        getAppTransform(appDir, switchMap, outputPath);

//        sdkStyleSet.addAll(getAppTransform(appDir, switchMap, outputPath));
//        getSdkTransform(sdkDir, sdkStyleSet, outputPath);
    }

    public void getSdkTransform(String sdkDir, Set<String> sdkStyleSet, String outputPath) throws DocumentException, IOException {
//        DrawableTransform dt = new DrawableTransform();
//        Set<String> drawableSet = dt.getTransform(sdkDir, colorMap, outputPath);


        SAXReader reader = new SAXReader();
        File xml = new File(sdkDir +  File.separator + "res" + File.separator + "values" + File.separator + "styles.xml");

        //
        if (xml.exists()) {
            Document doc = reader.read(xml);
            Element root = doc.getRootElement();
            if (!root.getName().equals(RESOURCE)) {
                System.err.println("This is not a resource xml file");
                return;
            }


            Queue<String> queue = new LinkedList<String>();
            queue.addAll(sdkStyleSet);

            Document newDoc = DocumentHelper.createDocument();
            Element newRoot = newDoc.addElement(RESOURCE);

            Set<String> visited = new HashSet<String>();
            Set<String> standalone = new HashSet<String>();
            while (!queue.isEmpty()) {
                String current = queue.remove();
                String key = current.split("/")[1];
                if (visited.contains(key)) {
                    continue;
                }

                visited.add(key);

                for (Element e : root.elements()) {
                    String styleName = e.attributeValue("name");
                    if (styleName.equals(key)) {
                        System.out.println(e.getName());


                        // add parent style name
                        String parent = e.attributeValue("parent");
                        Element styleElement = DocumentHelper.createElement(e.getName());
                        styleElement.addAttribute("name", StylePrefix + styleName);
                        if (parent != null) {
                            queue.add(parent);
                            styleElement.addAttribute("parent", "@style/" + StylePrefix + parent.split("/")[1]);

                            styleElement.appendContent(e);

                            for (Element it : styleElement.elements()) {
                                String text = it.getText();
                                it.addAttribute("name",  "android:" + it.attributeValue("name"));
                                //|| text.startsWith("@drawable")
                                String namevalue=it.attributeValue("name");

                                if(!namevalue.matches(".*[C|c]olor.*"))
                                {
                                    System.out.println("match Color: "+namevalue);
                                    styleElement.remove(it);

                                    continue;
                                }
                                if (text.startsWith("@style") || text.startsWith("@color")) {
                                    if (text.startsWith("@style")) {
                                        text = "@style/" + StylePrefix + text.split("/")[1];
                                        it.setText(text);
                                    } else if (text.startsWith("@drawable")) {
                                        //if (!drawableSet.contains(text.split("/")[1])) {
                                            //styleElement.remove(it);
                                        //}
                                        styleElement.remove(it);
                                    }
                                    else if (text.startsWith("@color")) {
                                        text = text.replace("/", "/" + ColorPrefix);
                                        it.setText(text);
                                    }
                                } else if (text.startsWith("?")) {
                                    text = text.replace("?", "?android:");
                                    it.setText(text);
                                } else if (text.startsWith("@") && !text.equals("@null")){
                                    text = text.replace("@", "@android:");
                                    it.setText(text);
                                }
                            }

                        } else {
                            standalone.add(styleName);
                            styleElement.addAttribute("parent", "@android:style/" + styleName);

                            styleElement.appendContent(e);

                            for (Element it : styleElement.elements()) {
                                String text = it.getText();
                                String namevalue=it.attributeValue("name");

                                if(!namevalue.matches(".*[C|c]olor.*"))
                                {
                                    System.out.println("match Color: "+namevalue);
                                    styleElement.remove(it);

                                    continue;
                                }
                                //|| text.startsWith("@drawable")

                                if (text.startsWith("@style") || text.startsWith("@color") ) {
                                    it.addAttribute("name",  "android:" + it.attributeValue("name"));
                                    if (text.startsWith("@style")) {
                                        text = "@style/" + StylePrefix + text.split("/")[1];
                                        it.setText(text);
                                    } else if (text.startsWith("@drawable")) {
//                                        if (!drawableSet.contains(text.split("/")[1])) {
//                                            styleElement.remove(it);
//                                        }
                                        styleElement.remove(it);
                                    }
                                    else if (text.startsWith("@color")) {
                                        text = text.replace("/", "/" + ColorPrefix);
                                        it.setText(text);
                                    }
                                } else {
                                    styleElement.remove(it);
                                }
                            }
                        }



                        newRoot.add(styleElement);

                        // add styles in items
                        for (Element child : e.elements()) {
                            String value = child.getText();
                            if (value.startsWith("@style")) {
                                queue.add(value);
                            }
                        }

                        break;
                    }
                }

            }

            String alteredColorXml = outputPath + File.separator + "res" + File.separator + "values" + File.separator + "sdk" + xml.getName();
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "UTF-8"), format);
            writer.write(newDoc);
            writer.close();
        }
    }

    public Set<String> getAppTransform(String appDir, Map<String, String> colorMap, String outputPath) throws IOException, DocumentException {
//        DrawableTransform dt = new DrawableTransform();
//        Set<String> drawableSet = dt.getTransform(appDir, colorMap, outputPath);

        Set<String> sdkStyles = new HashSet<String>();

        List<String> styles = get_all_styles(appDir);
        for (String styleFilePath : styles) {
            SAXReader reader = new SAXReader();
            File xml = new File(styleFilePath);

            //
            if (xml.exists()) {
                Document doc = reader.read(xml);
                Element root = doc.getRootElement();
                if (!root.getName().equals(RESOURCE)) {
                    System.err.println("This is not a resource xml file");
                    return null;
                }


                //            Set<String> visited = new HashSet<String>();
                //            Set<String> standalone = new HashSet<String>();


                for (Element e : root.elements()) {
                    //                String styleName = e.attributeValue("name");

                    // add parent style name
                    String parent = e.attributeValue("parent");

                    if (parent != null && parent.startsWith("@android:style")) {
                        String newStyle = parent.split("/")[1];
                        if (newStyle.contains(".Light")) {
                            newStyle = newStyle.substring(0, newStyle.indexOf(".Light"));
                            e.addAttribute("parent", "@android:style/" + newStyle);
                        }
                        // e.addAttribute("parent", "@style/" + StylePrefix + parent.split("/")[1]);
                        sdkStyles.add(parent);
                    }

                    for (Element it : e.elements()) {
                        String text = it.getText();
                        if (text.startsWith("@android:style")) {
                            //                        text = text.split("/")[0].replace("android:","") + "/" + StylePrefix + text.split("/")[1];
                            String newStyle = text.split("/")[1];
                            newStyle = newStyle.substring(0, newStyle.indexOf(".Light"));
                            it.setText(newStyle);
                            sdkStyles.add(text);

                        } else if (text.startsWith("@android:color")) {
                            text = text.split("/")[0].replace("android:", "") + "/" + ColorPrefix + text.split("/")[1];
                            it.setText(text);
                        } else if (text.startsWith("#")) {
                            String alpha = text.substring(1, 3).toLowerCase();
                            String key = "#" + text.substring(3).toLowerCase();
                            if (colorMap.containsKey(key)) {
                                String transformed = colorMap.get(key);
                                transformed = "#" + alpha + transformed.substring(1);

                                it.setText(transformed);
                            }
                        }
                    }

                }

//                String alteredColorXml = outputPath + File.separator + "res" + File.separator + "values" + File.separator + xml.getName();
                String alteredColorXml = styleFilePath;
                OutputFormat format = OutputFormat.createPrettyPrint();
                XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "UTF-8"), format);
                writer.write(doc);
                writer.close();
            }
        }
        return sdkStyles;
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

    private static List<String> get_all_styles(String app_dir) {
        List<String> all_styles = new ArrayList<String>();

        try {
            Process proc = Runtime.getRuntime().exec("find " + app_dir + " -name styles.xml");
            BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()), 16);
            String line;
            while ((line = br.readLine()) != null) {
                all_styles.add(line.trim());
            }
            proc.waitFor();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return all_styles;
    }


}
