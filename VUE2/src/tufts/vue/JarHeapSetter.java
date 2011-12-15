/* JarHeapSetter
* Code derived from Image to ZX Spec
* Copyright (C) 2010 Silent Software (Benjamin Brown)
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or 
* without modification, are permitted provided that the following 
* conditions are met:
* - Redistributions of source code must retain the above 
* copyright notice, this list of conditions and the following disclaimer.
* 
* - Redistributions in binary form must reproduce the above copyright 
* notice, this list of conditions and the following disclaimer in the 
* documentation and/or other materials provided with the distribution.
* 
* - Neither the name of Silent Software, Silent Development, Benjamin
* Brown nor the names of its contributors may be used to endorse or promote 
* products derived from this software without specific prior written 
* permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
* "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
* TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
* PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
* HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
* TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package tufts.vue;

import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.xml.sax.InputSource;

import tufts.Util;
import tufts.vue.VUE;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.action.AboutAction;
import tufts.vue.action.ExitAction;
import tufts.vue.action.OpenAction;
import tufts.vue.action.SaveAction;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.FullScreen;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFrame;
import tufts.vue.gui.VueMenuBar;
import tufts.vue.ui.InspectorPane;
import edu.tufts.vue.compare.ui.MergeMapsControlPanel;
import edu.tufts.vue.dsm.impl.VueDataSourceManager;
import edu.tufts.vue.preferences.implementations.MetadataSchemaPreference;
import edu.tufts.vue.preferences.implementations.ShowAgainDialog;
import edu.tufts.vue.preferences.implementations.WindowPropertiesPreference;

// HO 08/11/2011 BEGIN ***************
public class JarHeapSetter 
	implements VueConstants{
	
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(JarHeapSetter.class);
  /**
    * Bare minimum heap memory for starting app and allowing
    * for reasonable sized images (note deliberate 1 MB
    * smaller than 512 due to rounding/non accurate free 
    * heap calculation). The solution allows JVMs with
    * enough heap already to just start without spawning 
    * a new process. 
    */
	// HO 08/11/2011 BEGIN **********
	// I couldn't get it to run with 511 but 127 seems to work...
    //private final static int MIN_HEAP = 511; 
	private final static int MIN_HEAP = 127; 
	// HO 08/11/2011 END **********
	// HO 08/11/2011 BEGIN **********
	// VUE forums suggest that about half the available heap
	// memory is good to set as maximum in order to scale
	// up to handle loads of images... may want to double
	// this, see if it improves performance
    private final static int RECOMMENDED_HEAP = 511;
    // HO 08/11/2011 END **********

    public static void main(String[] args) throws Exception {

    // Do we have enough memory already (some VMs and later Java 6 
    // revisions have bigger default heaps based on total machine memory)?
    float heapSizeMegs = (Runtime.getRuntime().maxMemory()/1024)/1024;

    // Yes so start
    if (heapSizeMegs > MIN_HEAP) {
    	
    	VUE.main(args);

    // No so set a large heap. Tut - I did use -server mode here originally
    // which does something similar for heap (i.e. can choose a machine specific
    // maximum) but this has some problems with Java 6 R19 for some reason 
    // on my single core machine but worked on Java 6 R13 :( so for now I'll 
    // use a naughty non standard -XX:+AggressiveHeap option.
    // NOTE I DO NOT RECOMMEND YOU DO THIS FOR PRODUCTION CODE use -Xmx1024m 
    // instead (or whatever memory you need) as another constant and use this
    // in place of "-XX:AggressiveHeap" below. E.g. 
    // "private final static int RECOMMENDED_HEAP = 1024;"
    // and
    // ...new ProcessBuilder("java","-Xmx"+RECOMMENDED_HEAP+"m"... 
    } else {
    	// HO 08/11/2011 BEGIN **********
    	// Instead, going to use the settings recommended in the discussion
    	// of scaling heap memory for loads of images in VUE forums...
    	// For reference, here is the string copied from info.plist with the same
    	// values untouched
    	// <string>-Xms128m -Xmx512m -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:SurvivorRatio=6 -XX:TargetSurvivorRatio=80 -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled</string>
    	// String pathToJar = ImageToZxSpecRunner.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
    	//ProcessBuilder pb = new ProcessBuilder("java","-XX:+AggressiveHeap", "-classpath", pathToJar, "tufts.vue.VUE");
    	// HO 08/11/2011 END **********
    	   	    		    	
    	// HO 08/11/2011 BEGIN **********
    	String classpath = System.getProperty("java.class.path");
    	ProcessBuilder pb = new ProcessBuilder("java","-Xmx"+RECOMMENDED_HEAP+"m -Xms"+MIN_HEAP+"m -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:NewSize=100m -XX:MaxNewSize=100m -XX:SurvivorRatio=6 -XX:TargetSurvivorRatio=80 -XX:+CMSClassUnloadingEnabled -XX:+CMSPermGenSweepingEnabled", "-classpath", classpath, "tufts.vue.VUE");
    	// if the minimum heap size is too big, it doesn't even go
    	// in here, so we need this statement for trace
    	Log.info("Doing the heapsetting thing!");
    	// HO 08/11/2011 END **********
    	pb.start();
    }
  }

}

// HO 08/11/2011 END *****************