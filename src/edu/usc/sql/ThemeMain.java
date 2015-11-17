package edu.usc.sql;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Created by mian on 10/21/15.
 */
public class ThemeMain {

    private static String RESOURCE = "resources";
    private static String PREFIX = "android:";

    public static void main(String[] args) throws DocumentException, CloneNotSupportedException {
        // write your code here
        // Start analyzing SDK resources
        if (args.length != 3) {
            System.err.println("Usage: jarfile framework-res-folder app-folder outputPath");
            return;
        }
        String rootDir = args[0];   //"/Users/mian/Documents/Projects/framework-res";
        String appDir = args[1];    //"/Users/mian/Documents/Projects/com.alarm.alarmmobile.android.cpi";
        String output = args[2];    //"/Users/mian/Desktop/UsedColor.txt";

        Set<String> colorSet = getUIColors(rootDir, appDir);

        try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output)));
                for (String color : colorSet) {
                    bw.write(color);
                    bw.write("\n");
                }
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        System.out.println("Total Color Number: " + colorSet.size());

    }

    public static Set<String> getUIColors(String rootDir, String appDir) throws DocumentException, CloneNotSupportedException {
        Map<String, Style> styleMap = loadSDKStyle(rootDir);

        // Get SDK Color Definitions
        ColorMain cm = new ColorMain();
        cm.parseColorDefinition(rootDir);
        Map<String, ARGB> colorMap = cm.getColorMap();
        Map<String, HashSet<ARGB>> colorSetMap = cm.getColorSetMap();

        // Get SDK Drawable Definition
        DrawableMain dm = new DrawableMain();
        Map<String, Drawable> drawableMap = dm.parseDrawableDefinition(rootDir);

        // Copy SDK Style Map
        Map<String, Style> sdkStyleMap = new TreeMap<String, Style>();

        for (Map.Entry<String, Style> entry : styleMap.entrySet()) {
            Style copyStyle = (Style) entry.getValue().clone();
            sdkStyleMap.put(entry.getKey(), copyStyle);
        }
        // App resources
        Map<String, Style> appStyleMap = loadAppStyle(appDir, sdkStyleMap);

        // Get App Color Definitions
        ColorMain appCm = new ColorMain();
        appCm.parseColorDefinition(appDir, colorMap, colorSetMap);
        Map<String, ARGB> appColorMap = appCm.getColorMap();
        Map<String, HashSet<ARGB>> appColorSetMap = appCm.getColorSetMap();

        // Get App Drawable Definition
        DrawableMain appDm = new DrawableMain();
        Map<String, Drawable> appDrawableMap = appDm.parseDrawableDefinition(appDir);

        Set<ARGB> uiColors = new HashSet<ARGB>();


        // Retrieve style, drawable and color from layouts
        LayoutMain lm = new LayoutMain();
        Set<String> itemSet = new HashSet<String>();
        uiColors.addAll(lm.getLayoutInfo(appDir, itemSet));

        // Handle all the styles, drawables, and colors
        uiColors.addAll(processAppItems(itemSet, appStyleMap, appDrawableMap, appColorMap, appColorSetMap,
                sdkStyleMap, drawableMap, colorMap, colorSetMap));

        // Retrieve themes for app
        ManifestMain mm = new ManifestMain();
        Map<String, String> themeMap = mm.retrieveThemeMap(appDir);
        Set<String> keySet = new HashSet<String>(themeMap.values());
        for (String themeKey : keySet) {
//            String themeKey = "@style/SnapchatTheme";
            Style themeNow;
            if (themeKey.contains("android:")) {
                themeNow = styleMap.get(themeKey.split("/")[1]);
                uiColors.addAll(processSdkTheme(themeNow, sdkStyleMap, drawableMap, colorMap, colorSetMap));

            } else {
                themeNow = appStyleMap.get(themeKey.split("/")[1]);
                uiColors.addAll(processAppTheme(themeNow, appStyleMap, appDrawableMap, appColorMap, appColorSetMap,
                        sdkStyleMap, drawableMap, colorMap, colorSetMap));
            }
        }

        Set<String> colorStringSet = new HashSet<String>();
        for (ARGB argb : uiColors) {
            colorStringSet.add(argb.toString());
        }
        return colorStringSet;
    }

    public static Map<String, Style> loadSDKStyle(String rootDir) throws DocumentException, CloneNotSupportedException {
        Map<String, Style> styleMap = new TreeMap<String, Style>();
        styleMap.putAll(getAllStyles(rootDir));
        System.out.println(styleMap.size());
        handleInheritance(styleMap);
        Set<Style> allThemes = getAllThemes(styleMap);
//        replaceSelfReferenceValue(allThemes);

        return styleMap;
    }

    public static Map<String, Style> loadAppStyle(String appDir, Map<String, Style> sdkStyleMap) throws DocumentException, CloneNotSupportedException {
        Map<String, Style> appStyleMap = new TreeMap<String, Style>();
        appStyleMap.putAll(getAllStyles(appDir));
        handleInheritance(appStyleMap);
        Set<Style> appThemes = getAllThemes(appStyleMap, sdkStyleMap);
//        replaceSelfReferenceValue(appThemes);

        return appStyleMap;
    }

    public static void addColor2Set(String key, Map<String, ARGB> colorMap, Map<String, HashSet<ARGB>> colorSetMap, Set<ARGB> colorSet) {
        if (colorMap.containsKey(key)) {
            colorSet.add(colorMap.get(key));
        } else {
            colorSet.addAll(colorSetMap.get(key));
        }
    }

    public static Map<String, Style> getAllStyles(String rootDir) throws DocumentException {

        SAXReader reader = new SAXReader();
        File xml = new File(rootDir +  File.separator + "res" + File.separator + "values" + File.separator + "styles.xml");
        Map<String, Style> styleMap = new TreeMap<String, Style>();
        if (xml.exists()) {
            Document doc = reader.read(xml);
            Element root = doc.getRootElement();
            if (!root.getName().equals(RESOURCE)) {
                System.err.println("This is not a resource xml file");
                return null;
            }

            Iterator<Element> it = root.elementIterator();


            // Start to store items for each style
            while (it.hasNext()) {
                Element entry = it.next();
                String styleName = entry.attributeValue("name");
                String parent = entry.attributeValue("parent");

                Map<String, Item> itemMap = new HashMap<String, Item>();

                for (Element e : entry.elements()) {
                    String name = e.attributeValue("name");
                    String tempValue = e.getText();
                    String category = "";
                    String value;
                    if (tempValue.contains("/")) {
                        String[] strArray = tempValue.split("/");
                        category = strArray[0];
                        value = strArray[1];
                    } else {
                        // Already defined before
                        if (tempValue.contains("?")) {
                            String key = tempValue.substring(1);
                            // if parsing a theme, then we can replace with known referred value
                            //                        System.out.println(styleName + "->" + tempValue);
                            if (itemMap.containsKey(key)) {
                                // reuse related category & value
                                Item targetItem = itemMap.get(key);
                                category = targetItem.getCategory();
                                value = targetItem.getValue();
                                // if style has "?", then it depends on a style attribute in the current theme
                                // keep it, then expand it later
                            } else {
                                value = tempValue;
                            }
                        } else {
                            value = tempValue;
                        }
                    }
                    Item item = new Item(name, category, value);
                    itemMap.put(name, item);
                }
                Style style = new Style(styleName, parent, itemMap);
                styleMap.put(styleName, style);
            }
        }
        return styleMap;
    }

    public static void handleInheritance(Map<String, Style> styleMap) {
        // Find root styles and assign children
        Set<String> rootStyles = new HashSet<String>();
        for(Map.Entry<String, Style> entry : styleMap.entrySet()) {
            Style v = entry.getValue();
            String parent = v.getParent();
            if (parent != null && parent.startsWith("@style")) {
                String pKey = parent.split("/")[1];
                Style parentStyle = styleMap.get(pKey);
                parentStyle.addChild(v.getName());

            } else {
                rootStyles.add(v.getName());
            }

        }
        // BFS traverse all the styles
        Queue<String> queue = new LinkedList<String>();

        for(String r : rootStyles) {
            queue.add(r);
            while (!queue.isEmpty()) {
                String current = queue.remove();
                Style currentStyle = styleMap.get(current);
                queue.addAll(currentStyle.getChildren());
                String parent = currentStyle.getParent();
                if (parent != null && parent.startsWith("@style")) {
                    String pKey = parent.split("/")[1];
                    Style parentStyle = styleMap.get(pKey);
                    Map<String, Item> mergedItems = new HashMap<String, Item>();
                    mergedItems.putAll(parentStyle.getItemMap());
                    mergedItems.putAll(currentStyle.getItemMap());
                    currentStyle.setItemMap(mergedItems);
                }
            }
        }
    }

    public static Set<Style> getAllThemes(Map<String, Style> styleMap) {
        Set<Style> themeSet = new HashSet<Style>();
        for (String s : styleMap.keySet()) {
            Style theme = styleMap.get(s);
            if (theme.isTheme()) {
                themeSet.add(theme);
            }
        }
        return themeSet;
    }

    public static Set<Style> getAllThemes(Map<String, Style> appStyleMap, Map<String, Style> sdkStyleMap) {
        for (String s : appStyleMap.keySet()) {
            Style style = appStyleMap.get(s);
            String parent = style.getParent();
            if (parent != null && parent.startsWith("@android:style")) {
                Style fatherStyle = sdkStyleMap.get(parent.split("/")[1]);
                style.setIsTheme(fatherStyle.isTheme());
            }
        }
        return getAllThemes(appStyleMap);
    }

    public static void replaceSelfReferenceValue(Set<Style> themeSet) {
        for (Style theme : themeSet) {
            Map<String, Item> themeItems = theme.getItemMap();

            for (Item item : themeItems.values()) {
                String value = item.getValue();
                /****** Explain ? in item's value ******/
                if (value.startsWith("?") && !value.contains("android:")) {
                    String variable = value.substring(1);
                    if (themeItems.containsKey(variable)) {
                        System.out.println(item.getValue());
                        Item newValue = themeItems.get(variable);
                        item.setCategory(newValue.getCategory());
                        item.setValue(newValue.getValue());
                    }
                }
            }
        }
    }

    public static Set<ARGB> processSdkTheme(Style themeNow, Map<String, Style> sdkStyleMap, Map<String, Drawable> drawableMap,
                                            Map<String, ARGB> colorMap, Map<String, HashSet<ARGB>> colorSetMap) {
        Set<String> targets = new HashSet<String>();
        targets.add("@color");
        targets.add("@drawable");
        targets.add("@style");

        Set<ARGB> uiColors = new HashSet<ARGB>();
        Queue<Style> themeQueue = new LinkedList<Style>();
        themeQueue.add(themeNow);
        Set<String> visitedTheme = new HashSet<String>();

        while (!themeQueue.isEmpty()) {

            Style currentTheme = themeQueue.remove();
            if (visitedTheme.contains(currentTheme.getName())) {
                continue;
            }
            visitedTheme.add(currentTheme.getName());

            Queue<Item> itemQueue = new LinkedList<Item>();
            for (Item item : currentTheme.getItemMap().values()) {
                if (targets.contains(item.getCategory())) {
                    itemQueue.add(item);
                }
                if (item.getValue().startsWith("#")) {
                    ARGB argb = new ARGB(item.getValue());
                    uiColors.add(argb);
                }
                // ? is ignored here, since it doesn't create new color
            }

            Set<Item> visited = new HashSet<Item>();
            while (!itemQueue.isEmpty()) {
                Item item = itemQueue.remove();
                visited.add(item);

                if (item.getCategory().equals("@drawable")) {
                    String key = item.getValue();
                    if (drawableMap.containsKey(key)) {
                        Drawable drawable = drawableMap.get(key);
                        for (Item di : drawable.getItemSet()) {
                            if (targets.contains(di.getCategory())) {
                                if (!visited.contains(di)) {
                                    itemQueue.add(di);
                                }
                            }
                            if (di.getValue().startsWith("#")) {
                                ARGB argb = new ARGB(di.getValue());
                                uiColors.add(argb);
                            }
                            // ignore "?" in drawable item
                        }
                    }
                } else if (item.getCategory().equals("@style")) {
                    String key = item.getValue();
                    Style style = sdkStyleMap.get(key);
                    if (style.isTheme()) {
                        themeQueue.add(style);
                        continue;
                    }
                    for (Item si : style.getItemMap().values()) {
                        if (targets.contains(si.getCategory())) {
                            if (!visited.contains(si)) {
                                itemQueue.add(si);
                            }
                        }
                        if (si.getValue().startsWith("#")) {
                            ARGB argb = new ARGB(si.getValue());
                            uiColors.add(argb);
                        }
                    }
                } else if (item.getCategory().equals("@color")) {
                    String key = item.getValue();
                    addColor2Set(key, colorMap, colorSetMap, uiColors);
                }

            }
        }
        return uiColors;

    }

    public static Set<ARGB> processAppTheme(Style themeNow, Map<String, Style> appStyleMap, Map<String, Drawable> appDrawableMap,
                                            Map<String, ARGB> appColorMap, Map<String, HashSet<ARGB>> appColorSetMap,
                                            Map<String, Style> sdkStyleMap, Map<String, Drawable> drawableMap,
                                            Map<String, ARGB> colorMap, Map<String, HashSet<ARGB>> colorSetMap) {
        Set<String> targets = new HashSet<String>();
        targets.add("@color");
        targets.add("@android:color");
        targets.add("@drawable");
        targets.add("@android:drawable");
        targets.add("@style");
        targets.add("@android:style");

        Set<ARGB> uiColors = new HashSet<ARGB>();
//        Set<String> overridingItems = new HashSet<String>();
        Queue<Style> themeQueue = new LinkedList<Style>();
        themeQueue.add(themeNow);
        Set<String> visitedTheme = new HashSet<String>();

        while (!themeQueue.isEmpty()) {
            Style currentTheme = themeQueue.remove();
            if (visitedTheme.contains(currentTheme.getName())) {
                continue;
            }
            visitedTheme.add(currentTheme.getName());

            Queue<Item> itemQueue = new LinkedList<Item>();

            for (Item item : currentTheme.getItemMap().values()) {
                if (targets.contains(item.getCategory())) {
                    itemQueue.add(item);
                }
                if (item.getValue().startsWith("#")) {
                    ARGB argb = new ARGB(item.getValue());
                    uiColors.add(argb);
                }
            }

            Set<Item> visited = new HashSet<Item>();
            while (!itemQueue.isEmpty()) {
                Item item = itemQueue.remove();
                visited.add(item);


//            String itemName = item.getName();
//            if (itemName.startsWith("android:")) {
//                itemName = itemName.substring(8);
//                overridingItems.add(itemName);
//            }

                if (item.getCategory().equals("@drawable")) {
                    String key = item.getValue();
                    if (appDrawableMap.containsKey(key)) {
                        Drawable drawable = appDrawableMap.get(key);
                        for (Item di : drawable.getItemSet()) {
                            if (targets.contains(di.getCategory())) {
                                if (!visited.contains(di)) {
                                    itemQueue.add(di);
                                }
                            }
                            if (di.getValue().startsWith("#")) {
                                ARGB argb = new ARGB(di.getValue());
                                uiColors.add(argb);
                            }
                            // ignore "?" in drawable item
                        }
                    }
                } else if (item.getCategory().equals("@android:drawable")) {
                    String key = item.getValue();
                    if (drawableMap.containsKey(key)) {
                        System.out.println("!!!!@android:drawable" + key);
                        Drawable drawable = drawableMap.get(key);
                        for (Item di : drawable.getItemSet()) {
                            if (targets.contains(di.getCategory())) {
                                String cat = di.getCategory();
                                if (!cat.contains("android")) {
                                    di.setCategory("@android:" + cat);
                                }
                                if (!visited.contains(di)) {
                                    itemQueue.add(di);
                                }
                            }
                            if (di.getValue().startsWith("#")) {
                                ARGB argb = new ARGB(di.getValue());
                                uiColors.add(argb);
                            }
                            // ignore "?" in drawable item
                        }
                    }

                } else if (item.getCategory().equals("@style")) {
                    String key = item.getValue();
                    Style style = appStyleMap.get(key);
                    if (style.isTheme()) {
                        themeQueue.add(style);
                        continue;
                    }
                    for (Item si : style.getItemMap().values()) {
                        if (targets.contains(si.getCategory())) {
                            if (!visited.contains(si)) {
                                itemQueue.add(si);
                            }
                        }
                        if (si.getValue().startsWith("#")) {
                            ARGB argb = new ARGB(si.getValue());
                            uiColors.add(argb);
                        }
                    }
                } else if (item.getCategory().equals("@android:style")) {
                    String key = item.getValue();
                    Style style = sdkStyleMap.get(key);
                    if (style.isTheme()) {
                        uiColors.addAll(processSdkTheme(style, sdkStyleMap, drawableMap, colorMap, colorSetMap));
                        continue;
                    }
                    for (Item si : style.getItemMap().values()) {
                        if (targets.contains(si.getCategory())) {
                            String cat = si.getCategory();
                            if (!cat.contains("android")) {
                                si.setCategory("@android:" + cat);
                            }
                            if (!visited.contains(si)) {
                                itemQueue.add(si);
                            }
                        }
                        if (si.getValue().startsWith("#")) {
                            ARGB argb = new ARGB(si.getValue());
                            uiColors.add(argb);
                        }
                    }
                } else if (item.getCategory().equals("@color")) {
                    String key = item.getValue();
                    addColor2Set(key, appColorMap, appColorSetMap, uiColors);
                } else if (item.getCategory().equals("@android:color")) {
                    String key = item.getValue();
                    addColor2Set(key, colorMap, colorSetMap, uiColors);
                }
            }

        }
        String parent = themeNow.getParent();
        if (parent != null && parent.startsWith("@android:style")) {
            Style parentStyle = sdkStyleMap.get(parent.split("/")[1]);
            uiColors.addAll(processSdkTheme(parentStyle, sdkStyleMap, drawableMap, colorMap, colorSetMap));
        }

        return uiColors;
    }

    public static Set<ARGB> processAppItems(Set<String> itemSet, Map<String, Style> appStyleMap, Map<String, Drawable> appDrawableMap,
                                            Map<String, ARGB> appColorMap, Map<String, HashSet<ARGB>> appColorSetMap,
                                            Map<String, Style> sdkStyleMap, Map<String, Drawable> drawableMap,
                                            Map<String, ARGB> colorMap, Map<String, HashSet<ARGB>> colorSetMap) {
        Set<String> targets = new HashSet<String>();
        targets.add("@color");
        targets.add("@android:color");
        targets.add("@drawable");
        targets.add("@android:drawable");
        targets.add("@style");
        targets.add("@android:style");

        Queue<Item> itemQueue = new LinkedList<Item>();
        Set<ARGB> uiColors = new HashSet<ARGB>();


        for (String s : itemSet) {
            String category = s.split("/")[0];
            String value = s.split("/")[1];
            Item newItem = new Item("", category, value);
            itemQueue.add(newItem);
        }

        Set<Item> visited = new HashSet<Item>();
        while (!itemQueue.isEmpty()) {
            Item item = itemQueue.remove();
            visited.add(item);

            if (item.getCategory().equals("@drawable")) {
                String key = item.getValue();
                if (appDrawableMap.containsKey(key)) {
                    Drawable drawable = appDrawableMap.get(key);
                    for (Item di : drawable.getItemSet()) {
                        if (targets.contains(di.getCategory())) {
                            if (!visited.contains(di)) {
                                itemQueue.add(di);
                            }
                        }
                        if (di.getValue().startsWith("#")) {
                            ARGB argb = new ARGB(di.getValue());
                            uiColors.add(argb);
                        }
                        // ignore "?" in drawable item
                    }
                }
            } else if (item.getCategory().equals("@android:drawable")) {
                String key = item.getValue();
                if (drawableMap.containsKey(key)) {
                    System.out.println("!!!!@android:drawable/" + key);
                    Drawable drawable = drawableMap.get(key);
                    for (Item di : drawable.getItemSet()) {
                        if (targets.contains(di.getCategory())) {
                            String cat = di.getCategory();
                            if (!cat.contains("android")) {
                                di.setCategory("@android:" + cat);
                            }
                            if (!visited.contains(di)) {
                                itemQueue.add(di);
                            }
                        }
                        if (di.getValue().startsWith("#")) {
                            ARGB argb = new ARGB(di.getValue());
                            uiColors.add(argb);
                        }
                        // ignore "?" in drawable item
                    }
                }

            } else if (item.getCategory().equals("@style")) {
                String key = item.getValue();
                Style style = appStyleMap.get(key);
                if (style.isTheme()) {
                    uiColors.addAll(processAppTheme(style, appStyleMap, appDrawableMap, appColorMap, appColorSetMap,
                            sdkStyleMap, drawableMap, colorMap, colorSetMap));
                    continue;
                }
                for (Item si : style.getItemMap().values()) {
                    if (targets.contains(si.getCategory())) {
                        if (!visited.contains(si)) {
                            itemQueue.add(si);
                        }
                    }
                    if (si.getValue().startsWith("#")) {
                        ARGB argb = new ARGB(si.getValue());
                        uiColors.add(argb);
                    }
                }
            } else if (item.getCategory().equals("@android:style")) {
                String key = item.getValue();
                Style style = sdkStyleMap.get(key);
                if (style.isTheme()) {
                    uiColors.addAll(processSdkTheme(style, sdkStyleMap, drawableMap, colorMap, colorSetMap));
                    continue;
                }
                for (Item si : style.getItemMap().values()) {
                    if (targets.contains(si.getCategory())) {
                        String cat = si.getCategory();
                        if (!cat.contains("android")) {
                            si.setCategory("@android:" + cat);
                        }
                        if (!visited.contains(si)) {
                            itemQueue.add(si);
                        }
                    }
                    if (si.getValue().startsWith("#")) {
                        ARGB argb = new ARGB(si.getValue());
                        uiColors.add(argb);
                    }
                }
            } else if (item.getCategory().equals("@color")) {
                String key = item.getValue();
                addColor2Set(key, appColorMap, appColorSetMap, uiColors);
            } else if (item.getCategory().equals("@android:color")) {
                String key = item.getValue();
                addColor2Set(key, colorMap, colorSetMap, uiColors);
            }
        }
        return uiColors;
    }
}
