package tufts.vue;

import javax.swing.JDialog;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.lang.reflect.*;
import java.beans.*;
import java.util.prefs.*;

/*
#define makeBool(x) private boolean do##x; \
        public boolean is##x() { return do##x; } \
        public void set##x(boolean t) { this.do##x = t; }
makeBool(RunSlowly);
*/
    

public class VuePreferences
{
    // map preferences
    private boolean doRolloverZoom;
    private boolean doShowIcons;
    private boolean doFixedLinkWidth;

    final static String PropertyKey = "vuePreferenceProperty";

    static class Property
        implements ActionListener
    {
        String mKey;
        Object mDefault;
        String mUnits;
        
        Property(String key, Object pDefault, String units) {
            mKey = key;
            mDefault = pDefault;
            mUnits = units;
        }
        Property(String key, Object pDefault) {
            this(key, pDefault, "");
        }

        public JComponent makeEditor() {
            JComponent editor = null;
            if (mDefault instanceof Boolean) {
                JCheckBox checkBox = new JCheckBox(mKey);
                checkBox.addActionListener(this);
                editor = checkBox;
            } else {
                editor = new JPanel();
                editor.setBackground(Color.lightGray);
                editor.add(new JLabel(mKey));
                JTextField text;
                if (mDefault instanceof Number)
                    text = new JTextField(4);
                else {
                    text = new JTextField();
                    text.setText((String)mDefault);
                }
                text.addActionListener(this);
                editor.add(text);
                if (mUnits.length() > 0)
                    editor.add(new JLabel(mUnits));
                    
            }
            editor.putClientProperty(PropertyKey, this);
            return editor;
        }

        public String toString() {
            return "Property[" + mKey + "]";
        }

        public void actionPerformed(ActionEvent e) {
            Property p = null;
            if (e.getSource() instanceof JComponent)
                p = (Property) ((JComponent)e.getSource()).getClientProperty(PropertyKey);
            if (p != null)
                System.out.println(p);
        }
        
    }

    // need to add default values and descriptions.
    // can get type from default value!
    static Property[] MapProperties = new Property[] {
        new Property("RolloverZoom", Boolean.FALSE),
        new Property("RolloverZoomDelay", new Integer(200), "milliseconds"),
        new Property("ShowIcons", Boolean.TRUE),
        new Property("FixedWidthLinks", Boolean.FALSE),
        new Property("GroupsForceBorder", Boolean.FALSE),
        new Property("SizeOfChildNodes", new Float(.75f)),
        new Property("SizeOfChildImages", new Float(.25f)),
    };
    
    static Property[] VueProperties = new Property[] {
        new Property("DisplayResourceBrowser", Boolean.TRUE),
        new Property("RightViewerTracksSelection", Boolean.TRUE),
        new Property("DisplayScrollbars", Boolean.TRUE),
    };

    static Property[] PresentationProperties = new Property[] {
        new Property("Fade Page Transitions", Boolean.TRUE),
    };

    
    public boolean isRolloverZoom() {
        return doRolloverZoom;
    }

    public void setRolloverZoom(boolean t) {
        doRolloverZoom = t;
    }

    public boolean doShowIcons() {
        return doShowIcons;
    }

    static class EditPane extends JPanel {

        EditPane(Preferences prefs) {
            setOpaque(false);
            try {
                String[] keys = prefs.keys();
                for (int i = 0; i < keys.length; i++) {
                    Property p = new Property(keys[i], prefs.get(keys[i], ""));
                    JComponent editor = p.makeEditor();
                    //editor.setOpaque(false);
                    add(editor);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        
        EditPane(Property[] properties) {
            //setBackground(SystemColor.window);
            setOpaque(false); // needed for mac default white-line BG color: textures don't align!
            //setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            for (int i = 0; i < properties.length; i++) {
                Property p = properties[i];
                JComponent editor = p.makeEditor();
                editor.setOpaque(false);
                add(editor);
            }
        }
        
        
    }

    static class PreferencePane extends JTabbedPane {
        public PreferencePane() {
            addTab("General", new EditPane(VueProperties));
            addTab("Map", new EditPane(MapProperties));
            addTab("Fedora Prefs", new EditPane(Preferences.userRoot().node("tufts/oki/dr/fedora")));
        }
    }

    public static void main(String[] args) {
        //VUE.initUI();
        tufts.Util.displayComponent(new PreferencePane(), 300,400);

        try {
            PropertyDescriptor p = new PropertyDescriptor("rolloverZoom", VuePreferences.class);
            System.out.println(p);
            System.out.println(p.getPropertyType());
            System.out.println(p.getPropertyEditorClass());
            Method setter = p.getWriteMethod();

            VuePreferences prefs = new VuePreferences();
            System.out.println(setter);
            System.out.println(prefs.isRolloverZoom());
            setter.invoke(prefs, new Object[] { Boolean.TRUE });
            System.out.println(prefs.isRolloverZoom());
            
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        /*
        PreferenceDialog p = new PreferenceDialog();
        tufts.Util.centerOnScreen(p);
        p.pack();
        p.show();
        */
    }
    
}