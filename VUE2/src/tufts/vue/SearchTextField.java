package tufts.vue;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import tufts.Util;
import tufts.vue.gui.GUI;
import edu.tufts.vue.metadata.VueMetadataElement;
import edu.tufts.vue.metadata.action.SearchAction;

public class SearchTextField extends JTextField {
	private static JPopupMenu popup;
	private static JPopupMenu editPopup;
	public static JCheckBoxMenuItem searcheveryWhereMenuItem;
	public static JCheckBoxMenuItem labelMenuItem;
	public static JCheckBoxMenuItem keywordMenuItem;
	public static JCheckBoxMenuItem categoriesMenuItem;
	public static JCheckBoxMenuItem categoryKeywordMenuItem;
	public static JCheckBoxMenuItem editSettingsMenuItem;
	private boolean mouse_over = false;
	SearchTextField thisTxtFld;
	JTextField fieldTxt;
	boolean isWindows = VueUtil.isWindowsPlatform();

	SearchTextField() {
		// super(VueResources.getString("search.text.default"),15);
		//super(11);
		
		thisTxtFld = this;		
		thisTxtFld.setText(VueResources.getString("search.text.default"));
		thisTxtFld.setForeground(Color.gray);
		GUI.init();
		initMenuSettings();
		if (!isWindows) {
			if (Util.isMacTiger()) {
				setColumns(15);
				thisTxtFld.setText("");
				fieldTxt = new JTextField(12);
				fieldTxt.setBorder(null);
				fieldTxt.setText(VueResources.getString("search.text.default"));
				fieldTxt.setForeground(Color.gray);
				createPopupMenu(isWindows);
				fieldTxt.addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e) {
						evaluatePopup(e);
					}

					public void mouseReleased(MouseEvent e) {
						evaluatePopup(e);
					}

					private void evaluatePopup(MouseEvent e) {
						if (e.isPopupTrigger()) {
							popup.show(e.getComponent(), e.getX(), e.getY());
						} else {
							if (fieldTxt.getText().trim().equals(
									VueResources
											.getString("search.text.default"))) {
								fieldTxt.setText("");
								fieldTxt.setForeground(Color.black);
							}
						}
					}

					public void mouseEntered(MouseEvent e) {

					}

					public void mouseExited(MouseEvent e) {

					}

				});
				// addMouseListener(new MouseAdapter() {
				// public void mouseEntered(MouseEvent e){
				// if((e.getX()< 23) ){
				// mouse_over = true;
				// repaint();
				// }else if(e.getX() < getWidth()-23){
				// mouse_over = false;
				// repaint();
				// }else{
				// mouse_over = false;
				// repaint();
				// }
				// }
				// public void mouseExited(MouseEvent e){
				// if((e.getX()< 23) ){
				// mouse_over = false;
				// repaint();
				// }else if(e.getX() < getWidth()-23){
				// mouse_over = false;
				// repaint();
				// }else{
				// mouse_over = false;
				// repaint();
				// }
				// }
				//
				// });
				fieldTxt.addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent ke) {
						if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
							if (searcheveryWhereMenuItem.isSelected()) {
								setSearchEverywhereAction();
							}/*
							 * else if(editSettingsMenuItem.isSelected()){
							 * setEditSettingsAction(); }
							 */else if (labelMenuItem.isSelected()) {
								setLabelSettingsAction();
							} else if (keywordMenuItem.isSelected()) {
								setKeywordSettingsAction();
							} else if (categoryKeywordMenuItem.isSelected()) {
								setKeywordCategorySettingsAction();
							} else if (categoriesMenuItem.isSelected()) {
								setCategorySettingsAction();
							} else {
								setSearchEverywhereAction();
							}
						}
					}
				});

				thisTxtFld.setEditable(false);
				thisTxtFld.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
				thisTxtFld.add(fieldTxt, BorderLayout.CENTER);
			} else {
				setColumns(11);
				setEditable(true);
				putClientProperty("JTextField.variant", "search");
				Insets noInsets = new Insets(0, 30, 0, 25);
				setMargin(noInsets);
				createPopupMenu(isWindows);
				addMouseListener(new MouseAdapter() {
					public void mousePressed(MouseEvent e) {
						evaluatePopup(e);
					}

					public void mouseReleased(MouseEvent e) {
						evaluatePopup(e);
					}

					private void evaluatePopup(MouseEvent e) {
						if (e.isPopupTrigger()) {
							popup.show(e.getComponent(), e.getX(), e.getY());
						} else {
							if (getText().trim().equals(
									VueResources
											.getString("search.text.default"))) {
								setText("");
								setForeground(Color.black);
							}
						}
					}
				});
				addKeyListener(new KeyAdapter() {
					public void keyReleased(KeyEvent ke) {
						if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
							if (searcheveryWhereMenuItem.isSelected()) {
								setSearchEverywhereAction();
							}/*
							 * else if(editSettingsMenuItem.isSelected()){
							 * setEditSettingsAction(); }
							 */else if (labelMenuItem.isSelected()) {
								setLabelSettingsAction();
							} else if (keywordMenuItem.isSelected()) {
								setKeywordSettingsAction();
							} else if (categoryKeywordMenuItem.isSelected()) {
								setKeywordCategorySettingsAction();
							} else if (categoriesMenuItem.isSelected()) {
								setCategorySettingsAction();
							} else {
								setSearchEverywhereAction();
							}
						}
					}
				});

			}
		} else {
			setEditable(true);
			fieldTxt = new JTextField();
			fieldTxt.setBorder(null);
			fieldTxt.setText(VueResources.getString("search.text.default"));
			fieldTxt.setForeground(Color.gray);
			fieldTxt.setPreferredSize(new Dimension(135,18));
			setPreferredSize(new Dimension(180,23));
			Insets noInsets = new Insets(0, 15, 0, 25);
			setMargin(noInsets);			
			fieldTxt.addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {
					mouse_over = false;
					setCursor(Cursor
							.getPredefinedCursor(Cursor.TEXT_CURSOR));
					repaint();
					revalidate();
				}

				public void mouseExited(MouseEvent e) {					
					mouse_over = false;
					setCursor(Cursor
							.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					repaint();
					revalidate();
				}
				public void mouseClicked(MouseEvent e) {
					if (fieldTxt.getText().trim().equals(
							VueResources.getString("search.text.default"))) {
						fieldTxt.setText("");
						fieldTxt.setForeground(Color.black);

					}
				}
				public void mouseReleased(MouseEvent e) {
//					if (fieldTxt.getText().trim().equals(
//							VueResources.getString("search.text.default"))) {
//						setText("");
//					}
					if (e.isPopupTrigger()) {
						createEditPopupMenu();
						editPopup.show(e.getComponent(), e.getX() + 5, e
								.getY());
					}
				}
			});
			addMouseListener(new MouseAdapter() {
				public void mouseEntered(MouseEvent e) {					
					if ((e.getX() < 23)) {						
						mouse_over = false;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						repaint();
						revalidate();
					} /*else if (e.getX() < getWidth() - 23) {
						System.err.println("1");
						mouse_over = false;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.TEXT_CURSOR));
						repaint();
						revalidate();
					}*/ else {						
						mouse_over = true;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						repaint();
						revalidate();
					}
				}

				public void mouseExited(MouseEvent e) {		
					
					
					if ((e.getX() < 23)) {
						mouse_over = false;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						repaint();
						revalidate();
					} else if (e.getX() < getWidth() - 23) {
						mouse_over = false;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.TEXT_CURSOR));
						repaint();
						revalidate();
					} else {
						mouse_over = false;
						setCursor(Cursor
								.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
						repaint();
						revalidate();
					}

				}
				
				public void mouseReleased(MouseEvent e) {
					
					if (e.isPopupTrigger()) {
						if ((e.getX() < 23)) {

						} else if (e.getX() < getWidth() - 23) {
							createEditPopupMenu();
							editPopup.show(e.getComponent(), e.getX() + 5, e
									.getY());
						} else {

						}
					}
					if ((e.getX() < 23)) {
						createPopupMenu(isWindows);
						popup.show(e.getComponent(), e.getX() + 5, e.getY());
					} else if (e.getX() < getWidth() - 23) {
						if (getText().trim().equals(
								VueResources.getString("search.text.default"))) {
							setText("");
							setForeground(Color.black);

						}
					} else {
						if (searcheveryWhereMenuItem.isSelected()) {
							setSearchEverywhereAction();
						}/*
						 * else if(editSettingsMenuItem.isSelected()){
						 * setEditSettingsAction(); }
						 */else if (labelMenuItem.isSelected()) {
							setLabelSettingsAction();
						} else if (keywordMenuItem.isSelected()) {
							setKeywordSettingsAction();
						} else if (categoryKeywordMenuItem.isSelected()) {
							setKeywordCategorySettingsAction();
						} else if (categoriesMenuItem.isSelected()) {
							setCategorySettingsAction();
						} else {
							setSearchEverywhereAction();
						}
					}
				}
			});
			fieldTxt.addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ke) {
					if (ke.getKeyCode() == KeyEvent.VK_ENTER) {                  
						if (searcheveryWhereMenuItem.isSelected()) {
							setSearchEverywhereAction();
						}/*
						 * else if(editSettingsMenuItem.isSelected()){
						 * setEditSettingsAction(); }
						 */else if (labelMenuItem.isSelected()) {
							setLabelSettingsAction();
						} else if (keywordMenuItem.isSelected()) {
							setKeywordSettingsAction();
						} else if (categoryKeywordMenuItem.isSelected()) {
							setKeywordCategorySettingsAction();
						} else if (categoriesMenuItem.isSelected()) {
							setCategorySettingsAction();
						} else {
							setSearchEverywhereAction();
						}
					}
				}
			});
			thisTxtFld.setEditable(true);
			thisTxtFld.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 0));
			thisTxtFld.add(fieldTxt, BorderLayout.CENTER);
		}
	}

	private void initMenuSettings() {
		searcheveryWhereMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.searcheverywhere"), true);
		labelMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.labels"));
		keywordMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.keywords"));
		categoriesMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.categories"));
		categoryKeywordMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.categories")
				+ " + " + VueResources.getString("search.popup.keywords"));
		editSettingsMenuItem = new JCheckBoxMenuItem(VueResources
				.getString("search.popup.edit.search.settings"));

	}

	/**
	 * This method is for Generating Popup menu
	 * 
	 * @param isWindow
	 */
	private void createPopupMenu(boolean isWindow) {
		if (popup == null) {
			popup = new JPopupMenu();
			ActionListener actionListener = new PopupActionListener();

			JMenuItem searchMenuItem = new JMenuItem(VueResources
					.getString("search.popup.search"));
			searchMenuItem.setEnabled(false);
			popup.add(searchMenuItem);

			searcheveryWhereMenuItem.addActionListener(actionListener);
			popup.add(searcheveryWhereMenuItem);

			labelMenuItem.addActionListener(actionListener);
			popup.add(labelMenuItem);

			keywordMenuItem.addActionListener(actionListener);
			popup.add(keywordMenuItem);

			categoriesMenuItem.addActionListener(actionListener);
			//popup.add(categoriesMenuItem);

			categoryKeywordMenuItem.addActionListener(actionListener);
			popup.add(categoryKeywordMenuItem);

			popup.addSeparator();

			editSettingsMenuItem.addActionListener(actionListener);
			popup.add(editSettingsMenuItem);

			if (!isWindow) {
				popup.addSeparator();

				JMenuItem clearMenuItem = new JMenuItem(VueResources
						.getString("search.popup.clear"));
				clearMenuItem.addActionListener(actionListener);
				popup.add(clearMenuItem);

				JMenuItem resetSettingsMenuItem = new JMenuItem(VueResources
						.getString("search.popup.reset"));
				resetSettingsMenuItem.addActionListener(actionListener);
				popup.add(resetSettingsMenuItem);

				JMenuItem selectMenuItem = new JMenuItem(VueResources
						.getString("search.popup.select.all"));
				selectMenuItem.addActionListener(actionListener);
				// selectMenuItem.setActionCommand(DefaultEditorKit.selectAllAction);
				popup.add(selectMenuItem);

				JMenuItem cutMenuItem = new JMenuItem(VueResources
						.getString("search.popup.cut"));
				cutMenuItem.addActionListener(actionListener);
				// cutMenuItem.setActionCommand(DefaultEditorKit.cutAction);
				popup.add(cutMenuItem);

				JMenuItem copyMenuItem = new JMenuItem(VueResources
						.getString("search.popup.copy"));
				copyMenuItem.addActionListener(actionListener);
				// copyMenuItem.setActionCommand(DefaultEditorKit.copyAction);
				popup.add(copyMenuItem);

				JMenuItem pasteMenuItem = new JMenuItem(VueResources
						.getString("search.popup.paste"));
				pasteMenuItem.addActionListener(actionListener);
				// pasteMenuItem.setActionCommand(DefaultEditorKit.pasteAction);
				popup.add(pasteMenuItem);

			}

		}

	}

	class PopupActionListener implements ActionListener {
		public void actionPerformed(ActionEvent actionEvent) {
			if (Util.isMacTiger() || Util.isWindowsPlatform()) {
				fieldTxt.setForeground(Color.black);
			} else {
				thisTxtFld.setForeground(Color.black);
			}

			if (VueResources.getString("search.popup.select.all").equals(
					actionEvent.getActionCommand().toString())) {
				if (Util.isMacTiger() || Util.isWindowsPlatform()) {
					fieldTxt.grabFocus();
					fieldTxt.selectAll();
				} else{
					thisTxtFld.grabFocus();
					thisTxtFld.selectAll();
				}
			} else if (VueResources.getString("search.popup.cut").equals(
					actionEvent.getActionCommand().toString())) {
				if (Util.isMacTiger() || Util.isWindowsPlatform()) {
					fieldTxt.cut();
				} else
					thisTxtFld.cut();
			} else if (VueResources.getString("search.popup.copy").equals(
					actionEvent.getActionCommand().toString())) {
				if (Util.isMacTiger() || Util.isWindowsPlatform()) {
					fieldTxt.copy();
				} else
					thisTxtFld.copy();
			} else if (VueResources.getString("search.popup.paste").equals(
					actionEvent.getActionCommand().toString())) {
				if (Util.isMacTiger() || Util.isWindowsPlatform()) {
					fieldTxt.paste();
				} else
					thisTxtFld.paste();
			} else if (VueResources.getString("search.popup.clear").equals(
					actionEvent.getActionCommand().toString())) {
				if (Util.isMacTiger() || Util.isWindowsPlatform()) {
					fieldTxt.setText("");
				} else
					thisTxtFld.setText("");
			} else if (VueResources.getString("search.popup.searcheverywhere")
					.equals(actionEvent.getActionCommand().toString())) {
				setSearchEverywhereAction();
			} else if (VueResources.getString(
					"search.popup.edit.search.settings").equals(
					actionEvent.getActionCommand().toString())) {
				setEditSettingsAction();
			} else if (VueResources.getString("search.popup.labels").equals(
					actionEvent.getActionCommand().toString())) {
				setLabelSettingsAction();
			} else if (VueResources.getString("search.popup.keywords").equals(
					actionEvent.getActionCommand().toString())) {
				setKeywordSettingsAction();
			} else if ((VueResources.getString("search.popup.categories")
					+ " + " + VueResources.getString("search.popup.keywords"))
					.equals(actionEvent.getActionCommand().toString())) {
				setKeywordCategorySettingsAction();
			} else if (VueResources.getString("search.popup.reset").equals(
					actionEvent.getActionCommand().toString())) {
				setResetSettingsAction();
			} else if (VueResources.getString("search.popup.categories")
					.equals(actionEvent.getActionCommand().toString())) {
				setCategorySettingsAction();
			}

		}
	}

	public void setCategorySettingsAction() {
		searcheveryWhereMenuItem.setSelected(false);
		keywordMenuItem.setSelected(false);
		categoriesMenuItem.setSelected(true);
		categoryKeywordMenuItem.setSelected(false);
		labelMenuItem.setSelected(false);
		//editSettingsMenuItem.setSelected(false);
		// resetSettingsMenuItem.setSelected(false);
		List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
		VueMetadataElement vme = new VueMetadataElement();
		String getTxtStr = "";
		if (Util.isMacTiger() || Util.isWindowsPlatform()) {
			getTxtStr = fieldTxt.getText().trim();
		} else {
			getTxtStr = thisTxtFld.getText().trim();
		}
		String statementObject[] = {
				VueResources.getString("metadata.vue.url") + "#none",
				getTxtStr,
				edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
		vme.setObject(statementObject);
		vme.setType(VueMetadataElement.SEARCH_STATEMENT);
		searchTerms.add(vme);
		SearchAction termsAction = new SearchAction(searchTerms);
		termsAction.setNoneIsSpecial(true);

		termsAction.setTextOnly(false);
		termsAction.setBasic(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(VUE.getMetadataSearchMainPanel()
				.getSelectedOperator());
		termsAction.setEverything(false);
		if (VUE.getMetadataSearchMainPanel() != null) {
			setTermsAction(termsAction);
		}
		JButton btn = new JButton();
		btn.setAction(termsAction);
		btn.doClick();
	}

	public void setResetSettingsAction() {
		searcheveryWhereMenuItem.setSelected(false);
		keywordMenuItem.setSelected(false);
		categoriesMenuItem.setSelected(false);
		categoryKeywordMenuItem.setSelected(false);
		labelMenuItem.setSelected(false);
		editSettingsMenuItem.setSelected(false);
		//VUE.getMetadataSearchMainGUI().setVisible(false);
		// resetSettingsMenuItem.setSelected(true);
		SearchAction.revertGlobalSearchSelectionFromMSGUI();
		VUE.getActiveViewer().repaint();
	}

	public void setKeywordCategorySettingsAction() {
		searcheveryWhereMenuItem.setSelected(false);
		keywordMenuItem.setSelected(false);
		categoriesMenuItem.setSelected(false);
		categoryKeywordMenuItem.setSelected(true);
		labelMenuItem.setSelected(false);
		//editSettingsMenuItem.setSelected(false);
		//VUE.getMetadataSearchMainGUI().setVisible(false);
		VUE.getMetadataSearchMainPanel().searchTypeCmbBox.setSelectedItem("Categories + Keywords");
		// resetSettingsMenuItem.setSelected(false);
		List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
		VueMetadataElement vme = new VueMetadataElement();
		String getTxtStr = "";
		if (Util.isMacTiger() || Util.isWindowsPlatform()) {
			getTxtStr = fieldTxt.getText().trim();
		} else {
			getTxtStr = thisTxtFld.getText().trim();
		}
		String statementObject[] = {
				VueResources.getString("metadata.vue.url") + "#none",
				getTxtStr,
				edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
		vme.setObject(statementObject);
		vme.setType(VueMetadataElement.SEARCH_STATEMENT);
		searchTerms.add(vme);
		SearchAction termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(false);
		termsAction.setTextOnly(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(VUE.getMetadataSearchMainPanel()
				.getSelectedOperator());
		termsAction.setEverything(false);
		if (VUE.getMetadataSearchMainPanel() != null) {
			setTermsAction(termsAction);
		}
		JButton btn = new JButton();
		btn.setAction(termsAction);
		btn.doClick();
	}

	public void setKeywordSettingsAction() {
		searcheveryWhereMenuItem.setSelected(false);
		keywordMenuItem.setSelected(true);
		categoriesMenuItem.setSelected(false);
		categoryKeywordMenuItem.setSelected(false);
		labelMenuItem.setSelected(false);
		//editSettingsMenuItem.setSelected(false);
		//VUE.getMetadataSearchMainGUI().setVisible(false);
		VUE.getMetadataSearchMainPanel().searchTypeCmbBox.setSelectedItem("Keywords");
		// resetSettingsMenuItem.setSelected(false);
		List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
		VueMetadataElement vme = new VueMetadataElement();
		String getTxtStr = "";
		if (Util.isMacTiger() || Util.isWindowsPlatform()) {
			getTxtStr = fieldTxt.getText().trim();
		} else {
			getTxtStr = thisTxtFld.getText().trim();
		}
		String statementObject[] = {
				VueResources.getString("metadata.vue.url") + "#none",
				getTxtStr,
				edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
		vme.setObject(statementObject);
		vme.setType(VueMetadataElement.SEARCH_STATEMENT);
		searchTerms.add(vme);
		SearchAction termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(false);
		termsAction.setTextOnly(true);
		termsAction.setMetadataOnly(true);
		termsAction.setOperator(VUE.getMetadataSearchMainPanel()
				.getSelectedOperator());
		termsAction.setEverything(false);
		if (VUE.getMetadataSearchMainPanel() != null) {
			setTermsAction(termsAction);
		}
		JButton btn = new JButton();
		btn.setAction(termsAction);
		btn.doClick();
	}

	public void setLabelSettingsAction() {
		searcheveryWhereMenuItem.setSelected(false);
		keywordMenuItem.setSelected(false);
		categoriesMenuItem.setSelected(false);
		categoryKeywordMenuItem.setSelected(false);
		labelMenuItem.setSelected(true);
		//editSettingsMenuItem.setSelected(false);
		//VUE.getMetadataSearchMainGUI().setVisible(false);
		VUE.getMetadataSearchMainPanel().searchTypeCmbBox.setSelectedItem("Labels");
		// resetSettingsMenuItem.setSelected(false);
		List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
		VueMetadataElement vme = new VueMetadataElement();
		String getTxtStr = "";
		if (Util.isMacTiger() || Util.isWindowsPlatform()) {
			getTxtStr = fieldTxt.getText().trim();
		} else {
			getTxtStr = thisTxtFld.getText().trim();
		}
		String statementObject[] = {
				VueResources.getString("metadata.vue.url") + "#none",
				getTxtStr,
				edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
		vme.setObject(statementObject);
		vme.setType(VueMetadataElement.SEARCH_STATEMENT);
		searchTerms.add(vme);
		SearchAction termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(true);
		termsAction.setTextOnly(false);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(VUE.getMetadataSearchMainPanel()
				.getSelectedOperator());
		termsAction.setEverything(false);
		if (VUE.getMetadataSearchMainPanel() != null) {
			setTermsAction(termsAction);
		}
		JButton btn = new JButton();
		btn.setAction(termsAction);
		btn.doClick();
	}

	public void setEditSettingsAction() {
//		searcheveryWhereMenuItem.setSelected(false);
//		keywordMenuItem.setSelected(false);
//		categoriesMenuItem.setSelected(false);
//		categoryKeywordMenuItem.setSelected(false);
//		labelMenuItem.setSelected(false);
		if (editSettingsMenuItem.isSelected()) {
			VUE.getMetadataSearchMainGUI().setVisible(true);
		} else {
			VUE.getMetadataSearchMainGUI().setVisible(false);
		}
		// resetSettingsMenuItem.setSelected(false);

	}

	public void setSearchEverywhereAction() {
		searcheveryWhereMenuItem.setSelected(true);
		keywordMenuItem.setSelected(false);
		categoriesMenuItem.setSelected(false);
		categoryKeywordMenuItem.setSelected(false);
		labelMenuItem.setSelected(false);
		//editSettingsMenuItem.setSelected(false);
		
		//VUE.getMetadataSearchMainGUI().setVisible(false);
		VUE.getMetadataSearchMainPanel().searchTypeCmbBox.setSelectedItem("Search everything");
		// resetSettingsMenuItem.setSelected(false);
		List<VueMetadataElement> searchTerms = new ArrayList<VueMetadataElement>();
		VueMetadataElement vme = new VueMetadataElement();
		String getTxtStr = "";
		if (Util.isMacTiger() || Util.isWindowsPlatform()) {
			getTxtStr = fieldTxt.getText().trim();
		} else {
			getTxtStr = thisTxtFld.getText().trim();
		}
		String statementObject[] = {
				VueResources.getString("metadata.vue.url") + "#none",
				getTxtStr,
				edu.tufts.vue.rdf.Query.Qualifier.CONTAINS.toString() };
		vme.setObject(statementObject);
		vme.setType(VueMetadataElement.SEARCH_STATEMENT);
		searchTerms.add(vme);
		SearchAction termsAction = new SearchAction(searchTerms);
		termsAction.setBasic(false);
		termsAction.setTextOnly(true);
		termsAction.setMetadataOnly(false);
		termsAction.setOperator(VUE.getMetadataSearchMainPanel()
				.getSelectedOperator());
		if (VUE.getMetadataSearchMainPanel() != null) {
			setTermsAction(termsAction);
		}
		JButton btn = new JButton();
		btn.setAction(termsAction);
		btn.doClick();
	}

	private void setTermsAction(SearchAction termsAction) {
		if (VUE.getMetadataSearchMainPanel().mapCmbBox != null
				&& VUE.getMetadataSearchMainPanel().mapCmbBox.getSelectedItem() != null
				&& VUE.getMetadataSearchMainPanel().mapCmbBox
						.getSelectedItem()
						.toString()
						.trim()
						.equals(
								VUE.getMetadataSearchMainPanel().ALL_MAPS_STRING)) {
			termsAction.setLocationType(SearchAction.SEARCH_ALL_OPEN_MAPS);
		} else {
			termsAction.setLocationType(SearchAction.SEARCH_SELECTED_MAP);
		}
		if (VUE.getMetadataSearchMainPanel().resultCmbBox != null
				&& VUE.getMetadataSearchMainPanel().resultCmbBox
						.getSelectedItem() != null) {
			String resultsTypeChoice = VUE.getMetadataSearchMainPanel().resultCmbBox
					.getSelectedItem().toString().trim();
			termsAction.setResultsType(resultsTypeChoice);
		} else {
			termsAction.setResultsType("Select");
		}
	}

	private void createEditPopupMenu() {
		if (editPopup == null) {
			editPopup = new JPopupMenu();
			ActionListener actionListener = new PopupActionListener();

			JMenuItem clearMenuItem = new JMenuItem(VueResources
					.getString("search.popup.clear"));
			clearMenuItem.addActionListener(actionListener);
			editPopup.add(clearMenuItem);

			JMenuItem resetSettingsMenuItem = new JMenuItem(VueResources
					.getString("search.popup.reset"));
			resetSettingsMenuItem.addActionListener(actionListener);
			editPopup.add(resetSettingsMenuItem);

			JMenuItem selectMenuItem = new JMenuItem(VueResources
					.getString("search.popup.select.all"));
			selectMenuItem.addActionListener(actionListener);
			editPopup.add(selectMenuItem);

			JMenuItem cutMenuItem = new JMenuItem(VueResources
					.getString("search.popup.cut"));
			cutMenuItem.addActionListener(actionListener);
			// cutMenuItem.setActionCommand(DefaultEditorKit.cutAction);
			editPopup.add(cutMenuItem);

			JMenuItem copyMenuItem = new JMenuItem(VueResources
					.getString("search.popup.copy"));
			copyMenuItem.addActionListener(actionListener);
			// copyMenuItem.setActionCommand(DefaultEditorKit.copyAction);
			editPopup.add(copyMenuItem);

			JMenuItem pasteMenuItem = new JMenuItem(VueResources
					.getString("search.popup.paste"));
			pasteMenuItem.addActionListener(actionListener);
			// pasteMenuItem.setActionCommand(DefaultEditorKit.pasteAction);
			editPopup.add(pasteMenuItem);

		}
	}

	protected void paintComponent(Graphics g) {
		// Paint the default look of the button.
		super.paintComponent(g);
		Image arrowImg = VueResources.getImageIcon("search.downarrowicon")
				.getImage();
		Image clearImg = VueResources.getImageIcon("search.closeicon")
				.getImage();
		Image searchImg = VueResources.getImageIcon("search.searchicon")
				.getImage();
		Image searchOVImg = VueResources.getImageIcon("search.searchicon.ov")
				.getImage();
		Image searchTigerImg = VueResources.getImageIcon(
				"search.tiger.searchicon").getImage();
		// Image searchTigerImgOv =
		// VueResources.getImageIcon("search.tiger.searchicon.ov").getImage();
		// Image clearImgOv =
		// VueResources.getImageIcon("search.closeicon.ov").getImage();
		int h = getHeight();
		int w = getWidth();
		if (!isWindows) {
			if (Util.isMacTiger()) {
				g.drawImage(searchTigerImg, 5, h / 2 - 7, searchTigerImg
						.getWidth(null), searchTigerImg.getHeight(null), this);
				g.drawImage(clearImg, w - 20, h / 2 - 8, clearImg
						.getWidth(null), clearImg.getHeight(null), this);
				// if(mouse_over){
				// g.drawImage(searchTigerImgOv,5,h/2-7,
				// searchTigerImg.getWidth(null) ,
				// searchTigerImg.getHeight(null), this);
				// g.drawImage(clearImg,w-20,h/2-8, clearImg.getWidth(null) ,
				// clearImg.getHeight(null), this);
				// }else{
				// g.drawImage(searchTigerImg,5,h/2-7,
				// searchTigerImg.getWidth(null) ,
				// searchTigerImg.getHeight(null), this);
				// g.drawImage(clearImg,w-20,h/2-8, clearImg.getWidth(null) ,
				// clearImg.getHeight(null), this);
				// }

			}
		} else {
			g.drawImage(arrowImg, 5, h / 2 - 5, arrowImg.getWidth(null),
					arrowImg.getHeight(null), this);
			if (!mouse_over) {				
				g.drawImage(searchImg, w - 20, h / 2 - 8, searchImg
						.getWidth(null), searchImg.getHeight(null), this);
			} else {
				g.drawImage(searchOVImg, w - 20, h / 2 - 8, searchOVImg
						.getWidth(null), searchOVImg.getHeight(null), this);
			}
		}

	}

	protected void setCursor(int cursorState) {
		setCursor(new Cursor(cursorState));
	}

}
