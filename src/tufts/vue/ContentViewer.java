/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.awt.Color;
import java.awt.LayoutManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.Unmarshaller;
import org.xml.sax.InputSource;

import tufts.Util;
import tufts.vue.gui.GUI;

public abstract class ContentViewer extends JPanel {
	public static final long	serialVersionUID = 1;
	private static final org.apache.log4j.Logger Log = org.apache.log4j.Logger.getLogger(ContentViewer.class);

	protected static int				vCount = 0;
	protected static final JLabel		StatusLabel = new JLabel(VueResources.getString("addLibrary.loading.label"), JLabel.CENTER);
	protected static final JComponent	Status;

	protected BrowseDataSource			browserDS = null;

	public ContentViewer() {
		super();
	}


	public ContentViewer(LayoutManager layout) {
		super(layout);
	}


	public ContentViewer(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}


	public ContentViewer(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}


	public void finalize() {
		browserDS = null;
	}


	static {
		GUI.apply(GUI.StatusFace, StatusLabel);
		StatusLabel.setAlignmentX(0.5f);

		JProgressBar bar = new JProgressBar();
		bar.setIndeterminate(true);

		if (false && Util.isMacLeopard()) {
			bar.putClientProperty("JProgressBar.style", "circular");
			bar.setBorder(BorderFactory.createLineBorder(Color.darkGray));
			//bar.putClientProperty("JComponent.sizeVariant", "small"); // no effect
			//bar.setString("Loading...");// no effect on mac
			//bar.setStringPainted(true); // no effect on mac
		} else {
			if (DEBUG.BOXES) bar.setBorder(BorderFactory.createLineBorder(Color.green));
			bar.setBackground(Color.red);
			bar.setEnabled(false); // don't make so garish (mostly for mac)
		}

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		if (DEBUG.BOXES) StatusLabel.setBorder(BorderFactory.createLineBorder(Color.blue, 1));

		panel.add(StatusLabel);
		panel.add(Box.createVerticalStrut(5));
		panel.add(bar);
		panel.setBorder(GUI.WidgetInsetBorder3);
		Status = panel;
	}


	protected abstract void displayInBrowsePane(JComponent viewer, boolean priority);


	protected abstract void repaintList();


	// Overridden in DataSourceViewer -- not needed in DataSetViewer
	public void setActiveDataSource(edu.tufts.vue.dsm.DataSource ds) {}


	// Overridden in DataSetViewer -- not needed in DataSourceViewer
	public void setActiveDataSource(final tufts.vue.DataSource ds) {}


	tufts.vue.DataSource getBrowsedDataSource() {
		return browserDS;
	}


	protected JComponent produceViewer(final tufts.vue.BrowseDataSource ds) {
		return produceViewer(ds, false);
	}


	protected JComponent produceViewer(final tufts.vue.BrowseDataSource ds, final boolean caching) {

		/**
		There is a weird problem in the latest Java 1.6 code on
		all the Sun JVMs running in the browser where this test fails even though it should not.
		*/
		if (!SwingUtilities.isEventDispatchThread())		
		{
			if (VUE.isApplet())
				System.out.println("not threadsafe except for AWT");
		//	else
				throw new Error("not threadsafe except for AWT");
		}

		if (DEBUG.DR) Log.debug("produceViewer: " + ds);

		final JComponent viewer = ds.getResourceViewer();

		if (viewer != null)
			return viewer;

		StatusLabel.setText(statusName(ds));

		if (ds.isLoading()) {
			// could up priority any time we come back through
			//ds.getLoadThread().setPriority(Thread.MAX_PRIORITY);
			//ds.getLoadThread().setPriority(Thread.NORM_PRIORITY);
			return Status;
		}

		String s = ds.getClass().getSimpleName() + "[" + ds.getDisplayName();
		if (ds.getAddressName() != null)
			s += "; " + ds.getAddressName();
		final String name = s + "]";

		final Thread buildViewerThread =
			new Thread(String.format("VBLD-%02d %s", vCount++, name)) {
				{
					setDaemon(true);
					if (caching)
						setPriority(Thread.currentThread().getPriority() - 1);
				}

				@Override
				public void run() {

					if (DEBUG.DR)Log.debug("kicked off");

					final JComponent newViewer = buildViewer(ds);

					if (isInterrupted()) {
						if (DEBUG.DR && newViewer != null)
							Log.debug("produced; but not needed: aborting");
						return;
					}

					Log.info("produced " + GUI.name(newViewer));

					GUI.invokeAfterAWT(new AWTAcceptViewerTask(ds, this, newViewer, name));
				}
			};

		ds.setLoadThread(buildViewerThread);

		buildViewerThread.start();

		return Status;
	}


	/**
	*
	* @return either the successfully created viewer, or an as informative as possible
	* error report panel should we encounter any exceptions.  The idea is that this
	* method is guaranteed not to return null: always something meaninful to display.
	* With one exception: if the thread this is running on has it's interrupted status
	* set, it may return null.
	*
	*/
	protected JComponent buildViewer(final tufts.vue.BrowseDataSource ds)
	{
		final String address = ds.getAddress();

		JComponent viewer = null;
		Throwable exception = null;

		try {
			viewer = ds.buildResourceViewer();
		} catch (Throwable t) {
			exception = t;
		}

		if (Thread.currentThread().isInterrupted()) {
			if (DEBUG.DR) Log.debug("built; but not needed: aborting");
			return null;
		}

		if (exception == null && viewer == null)
			exception = new Exception("no viewer available");

		if (exception != null) {
			final Throwable t = exception;

			Log.error(ds + "; getResourceViewer:", t);

			String txt = ds.getTypeName() + " unavailable:";

			if (t instanceof DataSourceException) {
				if (t.getMessage() != null)
					txt += " " + t.getMessage();
			} else
				txt += "\n\nError: " + prettyException(t);

			if (t.getCause() != null) {
				Throwable c = t.getCause();
				Log.error("FULL CAUSE:", c);
				txt += "\n\nCause: " + prettyException(c);
			}

			String a = address;
			if (a != null) {
				//if (a.length() == 0 || Character.isWhitespace(a.charAt(0)) || Character.isWhitespace(a.charAt(a.length()-1)))
				a = '[' + a + ']';
			}

			txt += "\n\nConfiguration address: " + a;

			if (DEBUG.Enabled)
				txt += "\n\nDataSource: " + ds.getClass().getName();

			txt += "\n\nThis could be a problem with the configuration for this "
				+ ds.getTypeName()
				+ ", with the local network connection, or with a remote server.";

			if (DEBUG.Enabled)
				txt += "\n\n" + Thread.currentThread();

			viewer = new ErrorText(txt);
		}

		return viewer;
	}


	public static void marshallMap(File file,SaveDataSourceViewer dataSourceViewer) {
		Marshaller marshaller = null;

		try {
			FileWriter writer = new FileWriter(file);
			marshaller = new Marshaller(writer);
			marshaller.setMapping(tufts.vue.action.ActionUtil.getDefaultMapping());
			if (DEBUG.DR) Log.debug("marshallMap: marshalling " + dataSourceViewer + " to " + file + "...");
			marshaller.setNoNamespaceSchemaLocation("none");
			marshaller.marshal(dataSourceViewer);
			if (DEBUG.DR) Log.debug("marshallMap: done marshalling.");
			writer.flush();
			writer.close();
		} catch (Throwable t) {
			t.printStackTrace();
			System.err.println("DataSourceViewer.marshallMap " + t.getMessage());
		}
	}
	

	public SaveDataSourceViewer unMarshallMap(File file)
	throws java.io.IOException,
			org.exolab.castor.xml.MarshalException,
			org.exolab.castor.xml.ValidationException,
			org.exolab.castor.mapping.MappingException
	{
		Unmarshaller unmarshaller = tufts.vue.action.ActionUtil.getDefaultUnmarshaller(file.toString());
		FileReader reader = new FileReader(file);
		SaveDataSourceViewer sviewer = (SaveDataSourceViewer) unmarshaller.unmarshal(new InputSource(reader));
		reader.close();
		return sviewer;
	}


	protected static String statusName(tufts.vue.BrowseDataSource ds) {
		String s = ds.getAddressName();

		if (s == null)
			s = ds.getDisplayName();

		if (s == null)
			s = ds.getTypeName();

		return s;
	}


	protected String prettyException(Throwable t) {
		String txt;

		if (t.getClass().getName().startsWith("java"))
			txt = t.getClass().getSimpleName();
		else
			txt = t.getClass().getName();

		if (t.getMessage() != null)
			txt += ": " + t.getMessage();

		return txt;
	}


	protected static final class ErrorText extends JTextArea {
		public static final long	serialVersionUID = 1;
		ErrorText(String txt) {
			super(txt);
			setEditable(false);
			setLineWrap(true);
			setWrapStyleWord(true);
			setBorder(GUI.WidgetInsetBorder3);
			GUI.apply(GUI.StatusFace, this);
			//setOpaque(false);
			//GUI.apply(GUI.ErrorFace, this);
		}
	}


	protected class AWTAcceptViewerTask implements Runnable {
		final tufts.vue.BrowseDataSource ds;
		final Thread serviceThread;
		final JComponent newViewer;
		final String name;

		AWTAcceptViewerTask(BrowseDataSource ds, Thread serviceThread, JComponent viewer, String name) {
			this.ds = ds;
			this.serviceThread = serviceThread;
			this.newViewer = viewer;
			this.name = name;
		}

		public void run() {
			if (serviceThread.isInterrupted()) {
				// never possible?  we're now synchronous in AWT
				Log.warn(name + "; in AWT; but viewer no longer needed: aborting result for " + serviceThread, new Throwable("FYI"));
				return;
			}

			VUE.diagPush(name);

			if (DEBUG.Enabled) Log.debug("accepting viewer & setting into VueDataSource");

			ds.setViewer(newViewer); // important to do this in AWT; it's why we have this task

			// The viewer we've just set may actually be just a text pane
			// describing an error condition: now set the actual availablity
			// of the content:

			ds.setAvailable(newViewer instanceof ErrorText == false);

			repaintList(); // so change in loaded status will be visible

			if (browserDS == ds) { // important to check this in AWT;
				if (DEBUG.Enabled) Log.debug("currently displayed data-source wants this viewer; displaying");
				displayInBrowsePane(newViewer, false); // important to do this in AWT;
			}
			else
				if (DEBUG.DR) Log.debug("display: skipping; user looking at something else");

			// this would always fallback-interrupt our own serviceThread but by now it
			// has already exited is waiting to die, as the last thing it does is add
			// this task to the AWT event queue.  There should be no code in the run
			// after the invoke.  We check for isAlive in setLoadThread just in case,
			// before we fallback-interrupt.

			// important to do both the get/set in AWT:
			if (ds.getLoadThread() == serviceThread)
				ds.setLoadThread(null); 

			VUE.diagPop(); 
		}
	}
}
