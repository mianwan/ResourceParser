package edu.usc.sql;

/**
 * Created by mian on 10/1/15.
 */
public class ARGB {
    private int a;
    private int r;
    private int g;
    private int b;

    // Constructors
    public ARGB () {
        a = 0;
        r = 0;
        g = 0;
        b = 0;
    }

    // argb's format: #AARRGGBB
    public ARGB (String color) {
        try {
            long argb = Long.parseLong(color.substring(1), 16);
            a = (int) (argb >> 24) & 0xff;
            r = (int) (argb >> 16) & 0xff;
            g = (int) (argb >> 8) & 0xff;
            b = (int) (argb) & 0xff;
        } catch (Exception e) {

        }
    }

    @Override
    public int hashCode() {

        int result = 17;
        result = 31 * result + a;
        result = 31 * result + r;
        result = 31 * result + g;
        result = 31 * result + b;
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof ARGB)) {
            return false;
        }
        ARGB argb = (ARGB) obj;
        return (this.a == argb.a && this.r == argb.r && this.g == argb.g && this.b == argb.b);
    }

    @Override
    public String toString() {

        String alpha = Integer.toHexString(this.a);
        String red = Integer.toHexString(this.r);
        String green = Integer.toHexString(this.g);
        String blue = Integer.toHexString(this.b);

        if (alpha.length() == 1)
            alpha = "0" + alpha;
        if(red.length() == 1)
            red = "0" + red;
        if(green.length() == 1)
            green = "0" + green;
        if(blue.length() == 1)
            blue = "0"+ blue;
        return "#"+ alpha + red + green + blue;
    }
}
