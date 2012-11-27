package org.cytoscape.app.internal.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.cytoscape.app.internal.event.AppsChangedEvent;
import org.cytoscape.app.internal.event.AppsChangedListener;
import org.cytoscape.app.internal.manager.App;
import org.cytoscape.app.internal.manager.App.AppStatus;
import org.cytoscape.app.internal.manager.AppManager;
import org.cytoscape.app.internal.manager.AppParser;
import org.cytoscape.app.internal.net.DownloadStatus;
import org.cytoscape.app.internal.net.ResultsFilterer;
import org.cytoscape.app.internal.net.Update;
import org.cytoscape.app.internal.net.WebApp;
import org.cytoscape.app.internal.net.WebQuerier;
import org.cytoscape.app.internal.net.WebQuerier.AppTag;
import org.cytoscape.app.internal.ui.downloadsites.DownloadSite;
import org.cytoscape.app.internal.ui.downloadsites.DownloadSitesManager;
import org.cytoscape.app.internal.ui.downloadsites.DownloadSitesManager.DownloadSitesChangedEvent;
import org.cytoscape.app.internal.ui.downloadsites.DownloadSitesManager.DownloadSitesChangedListener;
import org.cytoscape.app.internal.util.DebugHelper;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;

/**
 * This class represents the panel in the App Manager dialog's tab used for installing new apps.
 * Its UI setup code is generated by the Netbeans 7 GUI builder.
 */
public class InstallAppsPanel extends javax.swing.JPanel {
	
	/** Long serial version identifier required by the Serializable class */
	private static final long serialVersionUID = -1208176142084829272L;
	
    private javax.swing.JButton closeButton;
    private javax.swing.JPanel descriptionPanel;
    private javax.swing.JScrollPane descriptionScrollPane;
    private javax.swing.JSplitPane descriptionSplitPane;
    private javax.swing.JTextPane descriptionTextPane;
    private javax.swing.JComboBox downloadSiteComboBox;
    private javax.swing.JLabel downloadSiteLabel;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JButton installButton;
    private javax.swing.JButton installFromFileButton;
    private javax.swing.JButton manageSitesButton;
    private javax.swing.JScrollPane resultsScrollPane;
    private javax.swing.JTree resultsTree;
    private javax.swing.JLabel searchAppsLabel;
    private javax.swing.JScrollPane tagsScrollPane;
    private javax.swing.JSplitPane tagsSplitPane;
    private javax.swing.JTree tagsTree;
    private javax.swing.JButton viewOnAppStoreButton;
	
	private JFileChooser fileChooser;
	
	private AppManager appManager;
	private DownloadSitesManager downloadSitesManager;
	private FileUtil fileUtil;
	private TaskManager taskManager;
	private Container parent;
	
	private WebApp selectedApp;
	private WebQuerier.AppTag currentSelectedAppTag;
	
	private Set<WebApp> resultsTreeApps;
	
    public InstallAppsPanel(final AppManager appManager, 
    		DownloadSitesManager downloadSitesManager, 
    		FileUtil fileUtil, TaskManager taskManager, Container parent) {
        this.appManager = appManager;
        this.downloadSitesManager = downloadSitesManager;
        this.fileUtil = fileUtil;
        this.taskManager = taskManager;
        this.parent = parent;
    	initComponents();
        
    	tagsTree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				updateResultsTree();
				updateDescriptionBox();
			}
		});
    	
		resultsTree.addTreeSelectionListener(new TreeSelectionListener() {
			
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				updateDescriptionBox();
			}
		});
		
		setupTextFieldListener();
    	setupDownloadSitesChangedListener();
    	
		queryForApps();
		
		appManager.addAppListener(new AppsChangedListener() {
			
			@Override
			public void appsChanged(AppsChangedEvent event) {
				TreePath[] selectionPaths = resultsTree.getSelectionPaths();

				updateDescriptionBox();
				
				fillResultsTree(resultsTreeApps);
				
				resultsTree.setSelectionPaths(selectionPaths);
			}
		});
    }
    
    private void setupDownloadSitesChangedListener() {
    
    	downloadSitesManager.addDownloadSitesChangedListener(new DownloadSitesChangedListener() {
			
			@Override
			public void downloadSitesChanged(
					DownloadSitesChangedEvent downloadSitesChangedEvent) {
				
				final DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel(
						new Vector<DownloadSite>(downloadSitesManager.getDownloadSites()));
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {
						downloadSiteComboBox.setModel(defaultComboBoxModel);
					}
				});
				
			}
		});
    }
    
    // Queries the currently set app store url for available apps.
    private void queryForApps() {
    	taskManager.execute(new TaskIterator(new Task() {
			
			// Obtain information for all available apps, then append tag information
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				WebQuerier webQuerier = appManager.getWebQuerier();
		    	
				taskMonitor.setTitle("Getting available apps");
				taskMonitor.setStatusMessage("Obtaining apps from: " 
						+ webQuerier.getCurrentAppStoreUrl());
				
				Set<WebApp> availableApps = webQuerier.getAllApps();
			
				// Once the information is obtained, update the tree
				
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						// populateTree(appManager.getWebQuerier().getAllApps());
						buildTagsTree();
						
						fillResultsTree(appManager.getWebQuerier().getAllApps());
					}
					
				});
			}

			@Override
			public void cancel() {
			}
			
		}));
    }

    private void initComponents() {

    	searchAppsLabel = new javax.swing.JLabel();
        installFromFileButton = new javax.swing.JButton();
        filterTextField = new javax.swing.JTextField();
        descriptionSplitPane = new javax.swing.JSplitPane();
        tagsSplitPane = new javax.swing.JSplitPane();
        tagsScrollPane = new javax.swing.JScrollPane();
        tagsTree = new javax.swing.JTree();
        resultsScrollPane = new javax.swing.JScrollPane();
        resultsTree = new javax.swing.JTree();
        descriptionPanel = new javax.swing.JPanel();
        descriptionScrollPane = new javax.swing.JScrollPane();
        descriptionTextPane = new javax.swing.JTextPane();
        viewOnAppStoreButton = new javax.swing.JButton();
        installButton = new javax.swing.JButton();
        downloadSiteLabel = new javax.swing.JLabel();
        downloadSiteComboBox = new javax.swing.JComboBox();
        closeButton = new javax.swing.JButton();
        manageSitesButton = new javax.swing.JButton();

        searchAppsLabel.setText("Search:");

        installFromFileButton.setText("Install from File...");
        installFromFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installFromFileButtonActionPerformed(evt);
            }
        });

        descriptionSplitPane.setDividerLocation(390);

        tagsSplitPane.setDividerLocation(175);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("all apps (0)");
        treeNode1.add(treeNode2);
        tagsTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        tagsTree.setFocusable(false);
        tagsTree.setRootVisible(false);
        tagsScrollPane.setViewportView(tagsTree);

        tagsSplitPane.setLeftComponent(tagsScrollPane);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        resultsTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        resultsTree.setFocusable(false);
        resultsTree.setRootVisible(false);
        resultsScrollPane.setViewportView(resultsTree);

        tagsSplitPane.setRightComponent(resultsScrollPane);

        descriptionSplitPane.setLeftComponent(tagsSplitPane);

        descriptionPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        descriptionTextPane.setContentType("text/html");
        descriptionTextPane.setEditable(false);
        //descriptionTextPane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n    <p style=\"margin-top: 0\">\n      App description is displayed here.\n    </p>\n  </body>\n</html>\n");
        descriptionTextPane.setText("");
        descriptionScrollPane.setViewportView(descriptionTextPane);

        javax.swing.GroupLayout descriptionPanelLayout = new javax.swing.GroupLayout(descriptionPanel);
        descriptionPanel.setLayout(descriptionPanelLayout);
        descriptionPanelLayout.setHorizontalGroup(
            descriptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 162, Short.MAX_VALUE)
        );
        descriptionPanelLayout.setVerticalGroup(
            descriptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(descriptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 354, Short.MAX_VALUE)
        );

        descriptionSplitPane.setRightComponent(descriptionPanel);

        viewOnAppStoreButton.setText("View on App Store");
        viewOnAppStoreButton.setEnabled(false);
        viewOnAppStoreButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                viewOnAppStoreButtonActionPerformed(evt);
            }
        });

        installButton.setText("Install");
        installButton.setEnabled(false);
        installButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                installButtonActionPerformed(evt);
            }
        });

        downloadSiteLabel.setText("Download Site:");

        downloadSiteComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { WebQuerier.DEFAULT_APP_STORE_URL }));
        downloadSiteComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                downloadSiteComboBoxItemStateChanged(evt);
            }
        });
        downloadSiteComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadSiteComboBoxActionPerformed(evt);
            }
        });
        downloadSiteComboBox.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                downloadSiteComboBoxKeyPressed(evt);
            }
        });

        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        manageSitesButton.setText("Manage Sites...");
        manageSitesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageSitesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(descriptionSplitPane)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(installFromFileButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 80, Short.MAX_VALUE)
                        .addComponent(viewOnAppStoreButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(installButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(closeButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(searchAppsLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(downloadSiteLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(downloadSiteComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(manageSitesButton)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(downloadSiteLabel)
                    .addComponent(downloadSiteComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(manageSitesButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchAppsLabel)
                    .addComponent(filterTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(descriptionSplitPane, javax.swing.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(installFromFileButton)
                    .addComponent(viewOnAppStoreButton)
                    .addComponent(installButton)
                    .addComponent(closeButton))
                .addContainerGap())
        );

        // Add a key listener to the download site combo box to listen for the enter key event
        final WebQuerier webQuerier = this.appManager.getWebQuerier();
        
        downloadSiteComboBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				final ComboBoxEditor editor = downloadSiteComboBox.getEditor();
		    	final Object selectedValue = editor.getItem();
				
				if (e.isActionKey() || e.getKeyCode() == KeyEvent.VK_ENTER) {
					
			    	if (downloadSiteComboBox.getModel() instanceof DefaultComboBoxModel
			    			&& selectedValue != null) {
			    		final DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) downloadSiteComboBox.getModel();
			    	
			    		SwingUtilities.invokeLater(new Runnable() {
			    			
			    			@Override
			    			public void run() {
				    			boolean selectedAlreadyInList = false;
				    	    	
				        		for (int i = 0; i < comboBoxModel.getSize(); i++) {
				        			Object listElement = comboBoxModel.getElementAt(i);
				        			
				        			if (listElement.equals(selectedValue)) {
				        				selectedAlreadyInList = true;
				        				
				        				break;	
				        			}
				        		}
				        		
				        		if (!selectedAlreadyInList) {
				        			comboBoxModel.insertElementAt(selectedValue, 1);
				        			
				        			editor.setItem(selectedValue);
				        		}
			    			}
			    			
			    		});
			    	}
			    	
			    	if (webQuerier.getCurrentAppStoreUrl() != selectedValue.toString()) {
			    		webQuerier.setCurrentAppStoreUrl(selectedValue.toString());
			    		
			    		queryForApps();
			    	}
				}
			}
		});
        
        // Make the JTextPane render HTML using the default UI font
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument) descriptionTextPane.getDocument()).getStyleSheet().addRule(bodyRule);
        
        // Setup the TreeCellRenderer to make the app tags use the folder icon instead of the default leaf icon, 
        // and have it use the opened folder icon when selected
    	DefaultTreeCellRenderer tagsTreeCellRenderer = new DefaultTreeCellRenderer() {

			private static final long serialVersionUID = 3311980250590351751L;
    		
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, 
					boolean expanded, boolean leaf, int row, boolean hasFocus) {
				
				Component defaultResult = super.getTreeCellRendererComponent(tree, value, selected, 
						expanded, leaf, row, hasFocus);
				
				// Make leaves use the open folder icon when selected
				if (selected && leaf) {
					setIcon(getOpenIcon());
				}
				
				return defaultResult;
			}
    	};
    	
		tagsTreeCellRenderer.setLeafIcon(tagsTreeCellRenderer.getDefaultClosedIcon());
		tagsTree.setCellRenderer(tagsTreeCellRenderer);
    }
    
    private void searchButtonActionPerformed(java.awt.event.ActionEvent evt) {                                             
	    // TODO add your handling code here:
	}

	private void installFromFileButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	// Setup a the file filter for the open file dialog
    	FileChooserFilter fileChooserFilter = new FileChooserFilter("Jar, Zip, and Karaf Kar Files (*.jar, *.zip, *.kar)",
    			new String[]{"jar", "zip", "kar"});
    	
    	Collection<FileChooserFilter> fileChooserFilters = new LinkedList<FileChooserFilter>();
    	fileChooserFilters.add(fileChooserFilter);
    	
    	// Show the dialog
    	final File[] files = fileUtil.getFiles(parent, 
    			"Choose file(s)", FileUtil.LOAD, FileUtil.LAST_DIRECTORY, "Install", true, fileChooserFilters);
    	
        if (files != null) {
        	
        	taskManager.execute(new TaskIterator(new Task() {

    			@Override
    			public void run(TaskMonitor taskMonitor) throws Exception {
    				taskMonitor.setTitle("Installing app");
    				
    				double progress = 0;
    					
    				taskMonitor.setStatusMessage("Installing app");
    				
    				for (int index = 0; index < files.length; index++) {
    	        		AppParser appParser = appManager.getAppParser();
    	        		
    	        		App parsedApp = null;
    	        		
    	        		parsedApp = appParser.parseApp(files[index]);
						
    	        		{
    	        			String parsedAppName = parsedApp.getAppName();
    	        			String parsedAppVersion = parsedApp.getVersion();
    	        			
	    	        		for (App app : appManager.getApps()) {
	
	    	        			// App with same name found, check if need to replace existing
	    	        			if (parsedAppName.equals(app.getAppName())) {
	    	        				
//	    	        				if (WebQuerier.compareVersions(parsedAppVersion, app.getVersion()) == 0) {
//	    	        					WebQuerier.c
	    	        					// TODO: Check == version, <= version.
//	    	        				}
	    	        				
	    	        			}
	    	        		}
    	        		}
    	        		
    	        		appManager.installApp(parsedApp);
    	        	}
    				
    	        	taskMonitor.setProgress(1.0);
    	        	
    	        	if (parent instanceof AppManagerDialog) {
    	        		((AppManagerDialog) parent).changeTab(1);	
    	        	}
    			}

    			@Override
    			public void cancel() {
    			}
    			
    		}));
        }
    }
    
    /**
     * Attempts to insert newlines into a given string such that each line has no 
     * more than the specified number of characters.
     */
    private String splitIntoLines(String text, int charsPerLine) {
    	return null;
    }

    private void setupTextFieldListener() {
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
        	
        	ResultsFilterer resultsFilterer = new ResultsFilterer();
        	
        	private void showFilteredApps() {
        		SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						
						tagsTree.clearSelection();
						
						fillResultsTree(resultsFilterer.findMatches(filterTextField.getText(), 
								appManager.getWebQuerier().getAllApps()));
					}
					
				});
        	}
        	
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				if (filterTextField.getText().length() != 0) {
					showFilteredApps();
				}
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				showFilteredApps();
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
			}
		});
    }
    
    private void installButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	final WebQuerier webQuerier = appManager.getWebQuerier();
        final WebApp appToDownload = selectedApp;
        
		taskManager.execute(new TaskIterator(new Task() {
			private DownloadStatus status;

			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				status = new DownloadStatus(taskMonitor);
				taskMonitor.setTitle("Installing App from App Store");
				
				double progress = 0;
					
				taskMonitor.setStatusMessage("Installing app: " + appToDownload.getFullName());
				
				// Download app
        		File appFile = webQuerier.downloadApp(appToDownload, null, new File(appManager.getDownloadedAppsPath()), status);
				
        		if (appFile != null) {
	        		// Parse app
	        		App parsedApp = appManager.getAppParser().parseApp(appFile);
	        		
	        		// Install app
					appManager.installApp(parsedApp);
        		} else {
        			// Log error: no download links were found for app
        			DebugHelper.print("Unable to find download url for: " + appToDownload.getFullName());
        		}
	        	
	        	taskMonitor.setProgress(1.0);
			}

			@Override
			public void cancel() {
				if (status != null) {
					status.cancel();
				}
			}
			
		}));
    }
    
    private void buildTagsTree() {
    	WebQuerier webQuerier = appManager.getWebQuerier();
    	
    	// Get all available apps and tags
    	Set<WebApp> availableApps = webQuerier.getAllApps();
    	Set<WebQuerier.AppTag> availableTags = webQuerier.getAllTags();
    	
    	List<WebQuerier.AppTag> sortedTags = new LinkedList<WebQuerier.AppTag>(availableTags);
    	
    	Collections.sort(sortedTags, new Comparator<WebQuerier.AppTag>() {

			@Override
			public int compare(AppTag tag, AppTag other) {
				return other.getCount() - tag.getCount();
			}
    	});
    	
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    	
    	DefaultMutableTreeNode allAppsTreeNode = new DefaultMutableTreeNode("all apps" 
    			+ " (" + availableApps.size() + ")");
    	root.add(allAppsTreeNode);
    	
    	DefaultMutableTreeNode appsByTagTreeNode = new DefaultMutableTreeNode("apps by tag");
    	
    	// Only show the "apps by tag" node if we have at least 1 app
    	if (availableApps.size() > 0) {
    		root.add(appsByTagTreeNode);
    	}
    	
    	DefaultMutableTreeNode treeNode = null;
    	for (final WebQuerier.AppTag appTag : sortedTags) {
    		if (appTag.getCount() > 0) {
    			treeNode = new DefaultMutableTreeNode(appTag);
    			appsByTagTreeNode.add(treeNode);
    		}
    	}
    	
    	tagsTree.setModel(new DefaultTreeModel(root));
    	// tagsTree.expandRow(2);
    	
    	currentSelectedAppTag = null;
    }
 
    private void updateResultsTree() {
    	
    	TreePath selectionPath = tagsTree.getSelectionPath();
    	
//    	DebugHelper.print(String.valueOf(selectedNode.getUserObject()));
    	currentSelectedAppTag = null;
    	
    	if (selectionPath != null) {
    		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
        	
	    	// Check if the "all apps" node is selected
	    	if (selectedNode.getLevel() == 1 
	    			&& String.valueOf(selectedNode.getUserObject()).startsWith("all apps")) {
	    		fillResultsTree(appManager.getWebQuerier().getAllApps());
	    		
	    	} else if (selectedNode.getUserObject() instanceof WebQuerier.AppTag) {
	    		WebQuerier.AppTag selectedTag = (WebQuerier.AppTag) selectedNode.getUserObject();
	    		
	    		fillResultsTree(appManager.getWebQuerier().getAppsByTag(selectedTag.getName()));
	    		currentSelectedAppTag = selectedTag;
	    	} else {
	    		// Clear tree
	    		resultsTree.setModel(new DefaultTreeModel(null));	    		
	    	}
    	} else {
    		// fillResultsTree(appManager.getWebQuerier().getAllApps());
//    		System.out.println("selection path null, not updating results tree");
    	}
    }
    
    private void fillResultsTree(Set<WebApp> webApps) {
    	appManager.getWebQuerier().checkWebAppInstallStatus(
    			appManager.getWebQuerier().getAllApps(), appManager);
    	
    	Set<WebApp> appsToShow = webApps;
    	List<WebApp> sortedApps = new LinkedList<WebApp>(appsToShow);
    	
    	// Sort apps by alphabetical order
    	Collections.sort(sortedApps, new Comparator<WebApp>() {
			@Override
			public int compare(WebApp webApp, WebApp other) {
				
				return (webApp.getName().compareToIgnoreCase(other.getName()));
			}
    	});
    	
    	DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
    	
    	DefaultMutableTreeNode treeNode;
    	for (WebApp webApp : sortedApps) {
    		if (webApp.getCorrespondingApp() != null
    				&& webApp.getCorrespondingApp().getStatus() == AppStatus.INSTALLED) {
    			webApp.setAppListDisplayName(webApp.getFullName() + " (Installed)");
    		} else {
    			webApp.setAppListDisplayName(webApp.getFullName());
    		}

    		treeNode = new DefaultMutableTreeNode(webApp);
    		root.add(treeNode);
    	}
    	
    	resultsTree.setModel(new DefaultTreeModel(root));
    	
    	resultsTreeApps = new HashSet<WebApp>(webApps);
  
    }
    
    private void updateDescriptionBox() {
    	
    	TreePath selectedPath = resultsTree.getSelectionPath();
    	
    	if (selectedPath != null) {
    		DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) resultsTree.getSelectionPath().getLastPathComponent();
    		WebApp selectedApp = (WebApp) selectedNode.getUserObject();
        	
    		boolean appAlreadyInstalled = (selectedApp.getCorrespondingApp() != null
    				&& selectedApp.getCorrespondingApp().getStatus() == AppStatus.INSTALLED);
    		
    		String text = "";
    		
    		// text += "<html> <head> </head> <body hspace=\"4\" vspace=\"4\">";
    		text += "<html> <body hspace=\"4\" vspace=\"2\">";
    		
    		
    		// App hyperlink to web store page
    		// text += "<p style=\"margin-top: 0\"> <a href=\"" + selectedApp.getPageUrl() + "\">" + selectedApp.getPageUrl() + "</a> </p>";
    		
    		// App name, version
    		text += "<b>" + selectedApp.getFullName() + "</b>";
    		String latestReleaseVersion = selectedApp.getReleases().get(selectedApp.getReleases().size() - 1).getReleaseVersion();
    		text += "<br />" + latestReleaseVersion;
    		
    		if (appAlreadyInstalled) {
    			if (!selectedApp.getCorrespondingApp().getVersion().equalsIgnoreCase(latestReleaseVersion)) {
    				text += " (installed: " + selectedApp.getCorrespondingApp().getVersion() + ")";
    			}
    		}
    		
    		/*
    		text += "<p>";
    		text += "<b>" + selectedApp.getFullName() + "</b>";
    		text += "<br />" + selectedApp.getReleases().get(selectedApp.getReleases().size() - 1).getReleaseVersion();
    		text += "</p>";
    		*/
    		text += "<p>";
    		
    		// App image
    		text += "<img border=\"0\" ";
    		text += "src=\"" + appManager.getWebQuerier().getDefaultAppStoreUrl() 
    			+ selectedApp.getIconUrl() + "\" alt=\"" + selectedApp.getFullName() + "\"/>";
    		
    		text += "</p>";
    		
    		
    		// App description
    		text += "<p>";
    		text += (String.valueOf(selectedApp.getDescription()).equalsIgnoreCase("null") ? "App description not found." : selectedApp.getDescription());
    		text += "</p>";
    		text += "</body> </html>";
    		descriptionTextPane.setText(text);
    		
    		this.selectedApp = selectedApp;
    		
    		if (appAlreadyInstalled) {
    			installButton.setEnabled(false);
    		} else {
    			installButton.setEnabled(true);
    		}
    		
    		viewOnAppStoreButton.setEnabled(true);
		
    	} else {
    		
    		//descriptionTextPane.setText("App description is displayed here.");
    		descriptionTextPane.setText("");
    		
    		this.selectedApp = null;
    		
    		installButton.setEnabled(false);
    		viewOnAppStoreButton.setEnabled(false);
    	}
    }

    private void resetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void viewOnAppStoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	if (selectedApp == null) {
    		return;
    	}
    	
    	if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			
			try {
				desktop.browse((new URL(selectedApp.getPageUrl())).toURI());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {
    	((javax.swing.JDialog)InstallAppsPanel.this.parent).dispose();
    }
    
    private void downloadSiteComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {
    }

    private void downloadSiteComboBoxActionPerformed(java.awt.event.ActionEvent evt) {
    	
    	final Object selected = downloadSiteComboBox.getSelectedItem();
    	
    	if (downloadSiteComboBox.getModel() instanceof DefaultComboBoxModel
    			&& selected != null) {
    		final DefaultComboBoxModel comboBoxModel = (DefaultComboBoxModel) downloadSiteComboBox.getModel();
    	
    		SwingUtilities.invokeLater(new Runnable() {
    			
    			@Override
    			public void run() {
	    			boolean selectedAlreadyInList = false;
	    	    	
	        		for (int i = 0; i < comboBoxModel.getSize(); i++) {
	        			Object listElement = comboBoxModel.getElementAt(i);
	        			
	        			if (listElement.equals(selected)) {
	        				selectedAlreadyInList = true;
	        				
	        				if (i > 0) {
	        					// comboBoxModel.removeElementAt(i);
	        					// comboBoxModel.insertElementAt(listElement, 1);
	        				}
	        				
	        				break;
	        			}
	        		}
	        		
	        		if (!selectedAlreadyInList) {
	        			comboBoxModel.insertElementAt(selected, 1);
	        		}
    			}
    			
    		});
    		
    		if (appManager.getWebQuerier().getCurrentAppStoreUrl() != selected.toString()) {
    			appManager.getWebQuerier().setCurrentAppStoreUrl(selected.toString());
	    		
	    		queryForApps();
	    	}
    	}
   
    }
    
    private void downloadSiteComboBoxKeyPressed(java.awt.event.KeyEvent evt) {
    }
    
    private void manageSitesButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (parent instanceof AppManagerDialog) {
        	((AppManagerDialog) parent).showManageDownloadSitesDialog();
        }
    }
}
