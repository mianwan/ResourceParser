package edu.usc.sql;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by mian on 10/22/15.
 */
public class ManifestMain {
    private static final String MANIFEST = "manifest";

//    public static void main(String[] args) throws DocumentException {
//        String appDir = "/Users/mian/Documents/Projects/com.snapchat.android";
//        retrieveThemeMap(appDir);
//    }

    public Map<String, String> retrieveThemeMap(String appDir) throws DocumentException {
        Map<String, String> themeMap = new TreeMap<String, String>();

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
            themeMap.put("Application", "@android:style/Theme.Holo");
        } else {
            themeMap.put("Application", appTheme);
        }

        List<Element> list = app.elements("activity");
        for (Element e : list) {
            String actTheme = e.attributeValue("theme");
            if (actTheme != null) {
                String act = e.attributeValue("name");
                themeMap.put(act, actTheme);
            }
        }

        return themeMap;
    }
}
