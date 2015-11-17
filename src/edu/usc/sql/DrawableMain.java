package edu.usc.sql;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.*;

/**
 * Created by mian on 10/15/15.
 */
public class DrawableMain {
    private final String RESOURCE = "resources";
    private final String COLOR = "color";
    private final String SELECTOR = "selector";
    private final String ITEM = "item";

    public static void main(String[] args) throws DocumentException {
        DrawableMain dm = new DrawableMain();
        String rootDir = "/Users/mian/Documents/Projects/framework-res";
//        String rootDir = "/Users/mian/Desktop";
        dm.parseDrawableDefinition(rootDir);

    }

    public Map<String, Drawable> parseDrawableDefinition(String rootDir) throws DocumentException {
        File folder = new File(rootDir + File.separator + "res" + File.separator + "drawable");
        Map<String, Drawable> drawableMap = new HashMap<String, Drawable>();
        // Analyze res/values/drawables.xml
        File xml = new File(rootDir + File.separator + "res" + File.separator + "values" + File.separator + "drawables.xml");
        if (xml.exists()) {
            SAXReader reader = new SAXReader();
            Document doc = reader.read(xml);
            Element root = doc.getRootElement();
            if (!root.getName().equals(RESOURCE)) {
                System.err.println("This is not a resource xml file");
                return drawableMap;
            }

            Iterator<Element> it = root.elementIterator();

            while (it.hasNext()) {
                Element e = it.next();
                Set<Item> itemSet = new HashSet<Item>();
                if (e.attributeValue("type").equals("drawable")) {
                    String drawableName = e.attributeValue("name"); //getQualifiedName()
                    String tempValue = e.getText();
                    String category = "";
                    String value;
                    if (tempValue.contains("/")) {
                        String[] strArray = tempValue.split("/");
                        category = strArray[0];
                        value = strArray[1];
                    } else {
                        value = tempValue;
                    }
                    Item item = new Item("", category, value);
                    itemSet.add(item);
                    Drawable drawable = new Drawable(drawableName, "xml", itemSet);
                    drawableMap.put(drawableName, drawable);
                }
            }
        }


        // Analyze res/drawable folder
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
                Set<Item> itemSet = new HashSet<Item>();
                while (!queue.isEmpty()) {
                    Element e = queue.remove();
                    queue.addAll(e.elements());

                    List<Attribute> list = e.attributes();

                    for (Attribute attr : list) {
                        String attrName = attr.getName(); //getQualifiedName()
                        String tempValue = attr.getValue();
                        String category = "";
                        String value;
                        if (tempValue.contains("/")) {
                            String[] strArray = tempValue.split("/");
                            category = strArray[0];
                            value = strArray[1];
                        } else {
                            value = tempValue;
                        }
                        Item item = new Item(attrName, category, value);
                        itemSet.add(item);
//                        System.out.println(item);
                    }
                }

                String drawableName = fileName.substring(0, fileName.lastIndexOf(".xml"));
                Drawable drawable = new Drawable(drawableName, root.getName(), itemSet);
                drawableMap.put(drawableName, drawable);

//                Iterator<Element> it = root.elementIterator();

            }
        }

        return drawableMap;
    }
}
