package edu.usc.sql.transform;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by mian on 10/29/15.
 */
public class ManifestTransform {
    private final String MANIFEST = "manifest";
    private final String StylePrefix = "";

    public static void main(String args[]) {
        ManifestTransform mt = new ManifestTransform();

        String appDir = "/Users/mian/Documents/Projects/TransformPool/abrc.mf.td";
        String outputPath = "/Users/mian/Documents/Projects/newApp";
        try {
            mt.getTransform(appDir, outputPath);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getTransform(String appDir, String outputPath) throws DocumentException, IOException {
        Set<String> sdkStyles = new HashSet<String>();

        SAXReader reader = new SAXReader();
        File manifest = new File(appDir + File.separator + "AndroidManifest.xml");
        Document doc = reader.read(manifest);
        Element root = doc.getRootElement();
        if (!root.getName().equals(MANIFEST)) {
            System.err.println("This is not a manifest xml file");
            return null;
        }
        Element app = root.element("application");
        String appTheme = app.attributeValue("theme");
        if (appTheme == null) {
//            app.addAttribute("theme", "@style/" + StylePrefix + "Theme.Holo");
//            sdkStyles.add("@android:style/Theme.Holo");
        } else {
            if (appTheme.startsWith("@android:style")) {
                sdkStyles.add(appTheme);
                String newStyle = appTheme.split("/")[1];
                if (newStyle.contains(".Light")) {
                    newStyle = newStyle.substring(0, newStyle.indexOf(".Light"));
                    app.addAttribute("theme", "@android:style/" + newStyle);
                }
//                app.addAttribute("theme", "@style/" + StylePrefix + appTheme.split("/")[1]);
            }
        }

        List<Element> list = app.elements("activity");
        for (Element e : list) {
            String actTheme = e.attributeValue("theme");
            if (actTheme != null) {
                if (actTheme.startsWith("@android:style")) {
                    sdkStyles.add(actTheme);
//                    e.addAttribute("theme", "@style/" + StylePrefix + actTheme.split("/")[1]);
                }
            }
        }


        String alteredColorXml = outputPath + File.separator + "AndroidManifest.xml";
        XMLWriter writer = new XMLWriter(new OutputStreamWriter(new FileOutputStream(alteredColorXml), "utf-8"));
        writer.write(doc);
        writer.close();
        return sdkStyles;
    }
}
