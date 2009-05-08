/*
 * Copyright 2003-2009 Tufts University  Licensed under the
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

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;

import tufts.vue.VueResources;

/**
 * @author Brian Goodmon
 */

public class LanguagePreference extends edu.tufts.vue.preferences.generics.GenericListPreference {
	protected String	category;
	protected String	key;
	protected String	name;
	protected String	description;
	protected String	defaultValue;
	protected boolean	ignoreListSelectionEvent = true;

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
				Locale[]			locales = Locale.getAvailableLocales();
				TreeSet<StringPair>	foundLocales = new TreeSet<StringPair>();
				int					localeCount = locales.length;

				for (int index = 0; index < localeCount; index++) {
					try {
						Locale			locale = locales[index],
										foundLocale;
						ResourceBundle	bundle = ResourceBundle.getBundle("tufts.vue.VueResources", new Locale(locale.getLanguage(), locale.getCountry()));

						foundLocale = bundle.getLocale();

						String	foundLanguage = foundLocale.getLanguage(),
								localizedDisplayName = fixCaps(foundLocale.getDisplayName(new Locale(foundLanguage)));

						if (localizedDisplayName.length() > 0) {
							// TreeSet is used to collect found locales because it doesn't add duplicates, and
							// it automatically orders its members.
							foundLocales.add(new StringPair(localizedDisplayName, foundLanguage + "_" + foundLocale.getCountry()));
						}
					}
					catch (Exception ex) {
					}
				}

				list.setListData(foundLocales.toArray());

				// Find the list item previously chosen as the preference (or the default) and select it.
				String		prefValue = getValue();
				ListModel	model = list.getModel();
				int			modelSize = model.getSize();

				ignoreListSelectionEvent = true;

				for (int index = 0; index < modelSize; index++) {
					if (prefValue.equals(((StringPair)model.getElementAt(index)).getValue())) {
						list.setSelectedIndex(index);
						break;
					}
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
		Preferences	p = Preferences.userNodeForPackage(getPrefRoot());

		return p.get(getPrefName() + ".code", Locale.getDefault().toString());	// will be lang_COUNTRY e.g. en_US
	}

 	public void setCode(String string) {
 		Preferences	p = Preferences.userNodeForPackage(getPrefRoot());

 		p.put(getPrefName() + ".code", string);
	}

	public String getLanguage() {
		return getCode().substring(0, 2);
	}

	public String getCountry() {
		return getCode().substring(3, 5);
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
