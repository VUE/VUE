package tufts.vue;

import java.awt.*;
import javax.swing.*;

class VueTheme extends javax.swing.plaf.metal.DefaultMetalTheme
{
    /*
    static int getDefaultFontStyle(int key) {
        System.out.println("getDefaultFontStyle " + key);
        return Font.PLAIN;
    }
    */

    static String getDefaultFontName(int key) {
        throw new RuntimeException("hello?");
    }
    static int getDefaultFontSize(int key) {
        throw new RuntimeException("hello?");
    }
    static int getDefaultFontStyle(int key) {
        throw new RuntimeException("hello?");
    }
    static String getDefaultPropertyName(int key) {
        throw new RuntimeException("hello?");
    }
    
    
}
