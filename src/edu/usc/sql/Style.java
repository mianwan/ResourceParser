package edu.usc.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mian on 10/6/15.
 */
public class Style implements Cloneable {
    private String name;
    private String parent;
    private Set<String> children;
    private Map<String, Item> itemMap;
    private boolean isTheme;

    public Style(String name, String parent, Map<String, Item> itemMap) {
        this.name = name;
        this.parent = parent;
        this.itemMap = itemMap;
        if (name.startsWith("Theme")) {
            isTheme = true;
        } else {
            isTheme = false;
        }
        children = new HashSet<String>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Item> getItemMap() {
        return itemMap;
    }

    public void setItemMap(Map<String, Item> itemMap) {
        this.itemMap = itemMap;
    }

    public boolean isTheme() {
        return isTheme;
    }

    public void setIsTheme(boolean isTheme) {
        this.isTheme = isTheme;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void addChild(String child) {
        children.add(child);
    }

    public Set<String> getChildren() {
        return children;
    }

    public void setChildren(Set<String> children) {
        this.children = children;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Style style = (Style) super.clone();
        style.setName(this.getName());
        style.setParent(this.getParent());
        style.setChildren(this.getChildren());
        Map<String, Item> newItems = new HashMap<String, Item>();
        for(String key : this.getItemMap().keySet()) {
            Item current = this.getItemMap().get(key);
            Item nItem = new Item(current.getName(), current.getCategory(), current.getValue());
            newItems.put(key, nItem);
        }
        style.setItemMap(newItems);
        style.setIsTheme(this.isTheme());
        return style;
    }
}
