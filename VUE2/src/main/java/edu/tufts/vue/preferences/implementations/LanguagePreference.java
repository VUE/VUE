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

package edu.tufts.vue.preferences.implementations;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Locale;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;

import tufts.vue.LWComponent;
import tufts.vue.VUE;
import tufts.vue.VueResources;

/**
 * @author Brian Goodmon
 */

public class LanguagePreference extends edu.tufts.vue.preferences.generics.GenericListPreference {
	protected static final org.apache.log4j.Logger
						Log = org.apache.log4j.Logger.getLogger(LanguagePreference.class);
	protected String	category;
	protected String	key;
	protected String	name;
	protected String	description;
	protected String	defaultValue;
	protected boolean	ignoreListSelectionEvent = false;

	public static LanguagePreference create(String category, String key, String name, String desc, String defaultValue, boolean showInUI) {
		return new LanguagePreference(category, key, name, desc, defaultValue, showInUI);
	}

	public static LanguagePreference create(String category, String key, String name, String desc, String defaultValue) {
		return new LanguagePreference(category, key, name, desc, defaultValue, true);
	}

	public static LanguagePreference create(String category, String key, String name, String desc) {
		return new LanguagePreference(category, key, name, desc, null, true);
	}

	private LanguagePreference(String category, String key, String name, String desc, String defaultValue, boolean showInUI) {
		super();

		this.category = category;
		this.key = key;
		this.name = name;
		this.description = desc;
		this.defaultValue = (defaultValue != null ? defaultValue : fixCaps(Locale.getDefault().getDisplayName()));

		if (showInUI) {
			edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
		}

		loadList = new Runnable() {
			public void run() {
				String					filename = null;
				try {
					TreeSet<StringPair>	foundLocales = new TreeSet<StringPair>();
					ClassLoader			loader = ClassLoader.getSystemClassLoader();
					URL					url = loader.getResource("tufts/vue/");

					if (url != null) {
						filename = URLDecoder.decode(url.getFile(), "UTF-8");
						filename = filename.substring(filename.indexOf("/"), filename.lastIndexOf("!"));

						File			file = new File(filename);
						JarFile			jar = new JarFile(file);
						Enumeration<JarEntry>
										jarEntries = jar.entries();

						while (jarEntries.hasMoreElements()) {
							JarEntry	jarEntry = jarEntries.nextElement();
							String		name = jarEntry.getName();

							if (name.indexOf("VueResources_") != -1 && name.indexOf("__") == -1) {
								int		languageIndex = name.indexOf("_") + 1;
								String	langCountry = name.substring(languageIndex);
								String	language = langCountry.substring(0, 2);
								int		countryIndex = langCountry.lastIndexOf("_") + 1;
								String	country = (countryIndex == 0 ? null : langCountry.substring(countryIndex, countryIndex + 2));
								Locale	foundLocale = (country == null ? new Locale(language) : new Locale(language, country));
								String	foundLanguage = foundLocale.getLanguage(),
										localizedDisplayName = fixCaps(foundLocale.getDisplayName(new Locale(foundLanguage)));

								if (localizedDisplayName.length() > 0) {
									// TreeSet is used to collect found locales because it doesn't add duplicates, and
									// it automatically orders its members.
									String	foundCountry = foundLocale.getCountry();

									foundLocales.add(new StringPair(localizedDisplayName, foundLanguage + (foundCountry.length() < 2 ? "" : ("_" + foundCountry))));
									}
								}
							}
						}

						list.setListData(foundLocales.toArray());
	
						// Find the list item previously chosen as the preference (or the default) and select it.
						String			prefValue = getValue();
						ListModel		model = list.getModel();
						int				modelSize = model.getSize();

						for (int index = 0; index < modelSize; index++) {
							if (prefValue.equals(((StringPair)model.getElementAt(index)).getValue())) {
								ignoreListSelectionEvent = true;
								list.setSelectedIndex(index);
								break;
						}
					}
				} catch (Exception ex) {
					Log.error("loadList exception loading " + filename + ": " + ex);
				}
			}
		};
	}

	protected void finalize() throws Throwable {
		try {
			category = null;
			key = null;
			name = null;
			description = null;
			defaultValue = null;
		}
		finally {
			super.finalize();
		}
	}

	public void setLocalizedStringsAndRegister() {
		// An instance of this class is created by VueResources to get the VUE-specific preferred language.
		// At the time of its creation the title and description can't be set because they are localized and
		// can't be fetched until VueResources is initialized.  After VueResources initializes it will call
		// this method to set the title and description, and to register the class with PreferencesManager.
		if (name == null) {
			name = VueResources.getString("preferences.language.title");
		}

		if (description == null) {
			description = VueResources.getString("preferences.language.description");
		}

		edu.tufts.vue.preferences.PreferencesManager.registerPreference(this);
	}

	public String getCategoryKey() {
		return category;
	}

	public String getTitle() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getPrefName() {
		return category + "." + key;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getCode() {
		return Preferences.userNodeForPackage(getPrefRoot()).get(getPrefName() + ".code",
			Preferences.systemNodeForPackage(getPrefRoot()).get(getPrefName() + ".code",
			Locale.getDefault().toString()));
	}

	public void setCode(String string) {
		Preferences	p = Preferences.userNodeForPackage(getPrefRoot());

		p.put(getPrefName() + ".code", string);
	}

	public String getLanguage() {
		return getCode().substring(0, 2);
	}

	public String getCountry() {
		String	code = getCode();

		return (code.length() < 5 ? "" : getCode().substring(3, 5));
	}

	public String fixCaps(String name) {
		//Fix the improperly capitalized names returned by Locale.getDisplayName(Locale).
		int		nameLength = name.length();

		return (nameLength < 1 ? "" : name.substring(0, 1).toUpperCase() +
			(nameLength < 2 ? "" : name.substring(1)));
	}

	/** interface ListSelectionListener */
	public void valueChanged(ListSelectionEvent event) {
		if (!event.getValueIsAdjusting()) {

			StringPair selected = (StringPair)list.getSelectedValue();

			if (ignoreListSelectionEvent) {
				// If the ListSelectionListener event was caused by loadList it should be ignored and
				// NOT set the value/code in the preference (because it might be selecting the default
				// value, which shouldn't be written to the backing store).
				ignoreListSelectionEvent = false;
			} else {
				if (selected != null) {
					setValue(selected.getValue());
					setCode(selected.getCode());
				}
			}
		}
	}

	public class StringPair implements Comparable<StringPair> {
		String	value = null,
				code = null;

		public StringPair(String value, String code) {
			this.value = value;
			this.code = code;
		}

		public String getValue() {
			return value;
		}

		public String getCode() {
			return code;
		}

		public String toString() {
			return value;
		}

		/** interface Comparable<StringPair>
		 * Implemented so that a StringPair can be added to a TreeSet.
		 */
		public int compareTo(StringPair pair) {
			return value.compareTo(pair.getValue());
		}
	}
}
