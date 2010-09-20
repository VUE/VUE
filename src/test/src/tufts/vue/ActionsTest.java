package test.src.tufts.vue;

import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;


import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import tufts.vue.*;
import tufts.vue.Actions.*;
import tufts.vue.VueAction.*;
import tufts.Util;
import tufts.vue.LWComponent.ChildKind;
import tufts.vue.LWComponent.Flag;
import tufts.vue.LWComponent.HideCause;
import tufts.vue.NodeTool.NodeModeTool;
import tufts.vue.gui.DeleteSlideDialog;
import tufts.vue.gui.DockWindow;
import tufts.vue.gui.FullScreen;
import tufts.vue.gui.GUI;
import tufts.vue.gui.VueFileChooser;
import tufts.vue.gui.renderer.SearchResultTableModel;
import edu.tufts.vue.metadata.MetadataList;
import edu.tufts.vue.preferences.ui.PreferencesDialog;

import junit.framework.*;

public class ActionsTest extends TestCase {

	private Action lwcAction; 
	
	protected void setUp() {
		lwcAction = Actions.AddWormholeToNewMapAction;
	}

	protected void tearDown() {
		lwcAction = null;
	}
	
	public void testAct() {

	}


}



