package edu.usc.sql;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by mian on 10/15/15.
 */
public class Drawable implements Cloneable {
    private String name;
    private String type; // bitmap, nine-patch, shape,
    private Set<Item> itemSet;

    public Drawable(String name, String type, Set<Item> itemSet) {
        this.name = name;
        this.type = type;
        this.itemSet = itemSet;
    }

    public Set<Item> getItemSet() {
        return itemSet;
    }

    public void setItemSet(Set<Item> itemSet) {
        this.itemSet = itemSet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
