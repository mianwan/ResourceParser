package edu.usc.sql;

/**
 * Created by mian on 10/6/15.
 * Shared by Style and Drawable
 */
public class Item {
    protected String name;
    protected String category;
    protected String value;


    public Item(String name, String category, String value) {
        this.name = name;
        this.category = category;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return name + " = " + category + "/" + value;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + category.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }
        Item item = (Item) obj;
        return (this.name.equals(item.name) && this.category.equals(item.category) && this.value.equals(item.value));
    }
}
