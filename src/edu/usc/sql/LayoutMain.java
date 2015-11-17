package edu.usc.sql;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * Created by mian on 10/22/15.
 */
public class LayoutMain {

    public static void main(String[] args) throws DocumentException {
        String appDir = "/Users/mian/Documents/Projects/com.snapchat.android";
        Set<String> itemSet = new HashSet<String>();
        LayoutMain lm = new LayoutMain();
        lm.getLayoutInfo(appDir, itemSet);
    }

    public Set<ARGB> getLayoutInfo(String appDir, Set<String> itemSet) throws DocumentException {
        Set<ARGB> straightColors = new HashSet<ARGB>();

        File folder = new File(appDir + "/res/layout/");
        Set<String> targets = new HashSet<String>();
        targets.add("@color");
        targets.add("@android:color");
        targets.add("@drawable");
        targets.add("@android:drawable");
        targets.add("@style");
        targets.add("@android:style");

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
                        String value = attr.getValue();
                        if (value.startsWith("@style") || value.startsWith("@android:style") || value.startsWith("@drawable")
                                || value.startsWith("@android:drawable") || value.startsWith("@color") || value.startsWith("@android:color")) {
                            itemSet.add(value);
                        } else if (value.contains("#")) {
                            ARGB argb = new ARGB(value);
                            straightColors.add(argb);
                        }
                    }
                }
            }
        }
        return straightColors;

    }
}
