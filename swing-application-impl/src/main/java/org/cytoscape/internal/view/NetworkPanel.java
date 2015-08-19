package org.cytoscape.internal.view;

/*
 * #%L
 * Cytoscape Swing Application Impl (swing-application-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetSelectedNetworksEvent;
import org.cytoscape.application.events.SetSelectedNetworksListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.internal.task.TaskFactoryTunableAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RemovedEdgesEvent;
import org.cytoscape.model.events.RemovedEdgesListener;
import org.cytoscape.model.events.RemovedNodesEvent;
import org.cytoscape.model.events.RemovedNodesListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.task.DynamicTaskFactoryProvisioner;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.edit.EditNetworkTitleTaskFactory;
import org.cytoscape.util.swing.JTreeTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.work.ServiceProperties;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class NetworkPanel extends JPanel implements CytoPanelComponent2,
		TreeSelectionListener, SetSelectedNetworksListener,
		NetworkAddedListener, NetworkViewAddedListener, NetworkAboutToBeDestroyedListener,
		NetworkViewAboutToBeDestroyedListener, RowsSetListener,
    RemovedEdgesListener, RemovedNodesListener {

	private static final Logger logger = LoggerFactory.getLogger(NetworkPanel.class);

	private static final String TITLE = "Network";
	private static final String ID = "org.cytoscape.Network";
	
	private static final int TABLE_ROW_HEIGHT = 16;
	private static final Dimension PANEL_SIZE = new Dimension(400, 700);

	private final JTreeTable treeTable;
	private final NetworkTreeNode root;
	private JPanel navigatorPanel;
	private JSplitPane split;

	private final NetworkTreeTableModel treeTableModel;
	private final CyApplicationManager appMgr;
	final CyNetworkManager netMgr;
	private final CyNetworkViewManager netViewMgr;
	private final DialogTaskManager taskMgr;
	private final DynamicTaskFactoryProvisioner factoryProvisioner;

	private final JPopupMenu popup;
	private JMenuItem editRootNetworTitle;
	
	private final Map<TaskFactory, JMenuItem> popupMap;
	private final Map<TaskFactory, CyAction> popupActions;
	private HashMap<JMenuItem, Double> actionGravityMap;
	private final Map<Object, TaskFactory> provisionerMap;
	
	private final Map<CyTable, CyNetwork> nameTables;
	private final Map<CyTable, CyNetwork> nodeEdgeTables;
	private final Map<Long, NetworkTreeNode> treeNodeMap;
	private final Map<CyNetwork, NetworkTreeNode> network2nodeMap;

	private boolean ignoreTreeSelectionEvents;

	private final EditNetworkTitleTaskFactory rootNetworkTitleEditor;
	private final JPopupMenu rootPopupMenu;	
	private CyRootNetwork selectedRoot;
	private Set<CyRootNetwork> selectedRootSet;

	public NetworkPanel(final CyApplicationManager appMgr,
						final CyNetworkManager netMgr,
						final CyNetworkViewManager netViewMgr,
						final BirdsEyeViewHandler bird,
						final DialogTaskManager taskMgr,
						final DynamicTaskFactoryProvisioner factoryProvisioner,
						final EditNetworkTitleTaskFactory networkTitleEditor) {
		this.treeNodeMap = new HashMap<>();
		this.provisionerMap = new HashMap<>();
		this.appMgr = appMgr;
		this.netMgr = netMgr;
		this.netViewMgr = netViewMgr;
		this.taskMgr = taskMgr;
		this.factoryProvisioner = factoryProvisioner;
		
		root = new NetworkTreeNode("Network Root", null);
		treeTableModel = new NetworkTreeTableModel(this, root);
		treeTable = new JTreeTable(treeTableModel);
		
		initialize();

		this.actionGravityMap = new HashMap<>();
		
		// create and populate the popup window
		popup = new JPopupMenu();
		popupMap = new WeakHashMap<>();
		popupActions = new WeakHashMap<>();
		nameTables = new WeakHashMap<>();
		nodeEdgeTables = new WeakHashMap<>();
		this.network2nodeMap = new WeakHashMap<>();

		
		setNavigator(bird.getBirdsEyeView());

		/*
		 * Remove CTR-A for enabling select all function in the main window.
		 */
		for (KeyStroke listener : treeTable.getRegisteredKeyStrokes()) {
			if (listener.toString().equals("ctrl pressed A")) {
				final InputMap map = treeTable.getInputMap();
				map.remove(listener);
				treeTable.setInputMap(WHEN_FOCUSED, map);
				treeTable.setInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, map);
			}
		}
		

		this.rootNetworkTitleEditor = networkTitleEditor;
		rootPopupMenu = new JPopupMenu();
		editRootNetworTitle = new JMenuItem("Rename Network Collection...");
		editRootNetworTitle.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				taskMgr.execute(rootNetworkTitleEditor.createTaskIterator(selectedRoot));
			}
		});
		rootPopupMenu.add(editRootNetworTitle);
		
		JMenuItem selectAllSubNetsMenuItem = new JMenuItem("Select All Networks");
		selectAllSubNetsMenuItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				selectAllSubnetwork();
			}
		});
		rootPopupMenu.add(selectAllSubNetsMenuItem);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return TITLE;
	}
	
	@Override
	public String getIdentifier() {
		return ID;
	}

	@Override
	public Icon getIcon() {
		return null;
	}
	
	protected void initialize() {
		setLayout(new BorderLayout());
		setPreferredSize(PANEL_SIZE);
		setSize(PANEL_SIZE);

		treeTable.getTree().addTreeSelectionListener(this);
		treeTable.getTree().setRootVisible(false);

		ToolTipManager.sharedInstance().registerComponent(treeTable);
		
		treeTable.getTree().setCellRenderer(new TreeCellRenderer());

		treeTable.getColumn("Network").setPreferredWidth(250);
		treeTable.getColumn("Nodes").setPreferredWidth(45);
		treeTable.getColumn("Edges").setPreferredWidth(45);

		treeTable.setBackground(UIManager.getColor("Table.background"));
		treeTable.setRowHeight(TABLE_ROW_HEIGHT);
		treeTable.setCellSelectionEnabled(false);
		treeTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		treeTable.getTree().setSelectionModel(new DefaultTreeSelectionModel());

		navigatorPanel = new JPanel();
		navigatorPanel.setLayout(new BorderLayout());
		navigatorPanel.setPreferredSize(new Dimension(280, 280));
		navigatorPanel.setSize(new Dimension(280, 280));
		navigatorPanel.setBackground(UIManager.getColor("Table.background"));

		JScrollPane scroll = new JScrollPane(treeTable);

		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, navigatorPanel);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setResizeWeight(1);
		split.setDividerLocation(400);

		add(split);

		// this mouse listener listens for the right-click event and will show
		// the pop-up window when that occurs
		treeTable.addMouseListener(new PopupListener());
	}

	public Map<Long, Integer> getNetworkListOrder() {
		Map<Long, Integer> order = new HashMap<Long, Integer>();
		
		// Save the network orders
		final JTree tree = treeTable.getTree();
		
		for (final Entry<CyNetwork, NetworkTreeNode> entry : network2nodeMap.entrySet()) {
			final CyNetwork net = entry.getKey();
			final NetworkTreeNode node = entry.getValue();
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				final int row = tree.getRowForPath(tp);
				order.put(net.getSUID(), row);
			}
		}
				
		return order;
	}
	
	public void setNetworkListOrder(final Map<Long, Integer> order) {
		if (order == null || order.size() < 2)
			return; // No need to sort 1 network
		
		// Restore the network collections order
		final Set<CyNetwork> networks = netMgr.getNetworkSet();
		final List<CyNetwork> sortedNetworks = new ArrayList<CyNetwork>(networks);
		
		Collections.sort(sortedNetworks, new Comparator<CyNetwork>() {
			@Override
			public int compare(final CyNetwork n1, final CyNetwork n2) {
				try {
					Integer o1 = order.get(n1.getSUID());
					Integer o2 = order.get(n2.getSUID());
					if (o1 == null) o1 = -1;
					if (o2 == null) o2 = -1;
					
					return o1.compareTo(o2);
				} catch (final Exception e) {
					logger.error("Cannot sort networks", e);
				}
				
				return 0;
			}
		});
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				nameTables.clear();
				nodeEdgeTables.clear();
				network2nodeMap.clear();
				treeNodeMap.clear();
				
				ignoreTreeSelectionEvents = true;
				root.removeAllChildren();
				treeTable.getTree().updateUI();
				treeTable.repaint();
				ignoreTreeSelectionEvents = false;
				
				for (final CyNetwork n : sortedNetworks)
					addNetwork(n);
					
				updateNetworkTreeSelection();
			}
		});
	}
	
	public void addTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		addFactory(factory, props);
	}

	public void removeTaskFactory(TaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(factory);
	}

	public void addNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkCollectionTaskFactory(NetworkCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewCollectionTaskFactory(NetworkViewCollectionTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkTaskFactory(NetworkTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void addNetworkViewTaskFactory(final NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		TaskFactory provisioner = factoryProvisioner.createFor(factory);
		provisionerMap.put(factory, provisioner);
		addFactory(provisioner, props);
	}

	public void removeNetworkViewTaskFactory(NetworkViewTaskFactory factory, @SuppressWarnings("rawtypes") Map props) {
		removeFactory(provisionerMap.remove(factory));
	}

	public void setNavigator(final Component comp) {
		this.navigatorPanel.removeAll();
		this.navigatorPanel.add(comp, BorderLayout.CENTER);
	}

	/**
	 * This is used by Session writer.
	 * @return
	 */
	public JTreeTable getTreeTable() {
		return treeTable;
	}

	public JPanel getNavigatorPanel() {
		return navigatorPanel;
	}

	// // Event handlers // //
	
	@Override
	public void handleEvent(final NetworkAboutToBeDestroyedEvent nde) {
		final CyNetwork net = nde.getNetwork();
		logger.debug("Network about to be destroyed: " + net);
		removeNetwork(net);
	}

	@Override
	public void handleEvent(final NetworkAddedEvent e) {
		final CyNetwork net = e.getNetwork();
		logger.debug("Network added: " + net);
		addNetwork(net);
	}

	@Override
	public void handleEvent(final RowsSetEvent e) {
		final Collection<RowSetRecord> payload = e.getPayloadCollection();
		if (payload.size() == 0)
			return;

		final RowSetRecord record = e.getPayloadCollection().iterator().next();
		if (record == null)
			return;
		
		final CyTable table = e.getSource();
		final CyNetwork network = nameTables.get(table);
		
		// Case 1: Network name/title updated
		if (network != null && record.getColumn().equals(CyNetwork.NAME)) {
			final CyRow row = payload.iterator().next().getRow();
			final String newTitle = row.get(CyNetwork.NAME, String.class);
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			final String oldTitle = treeTableModel.getValueAt(node, 0).toString();

			if (newTitle.equals(oldTitle) == false) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						treeTableModel.setValueAt(newTitle, node, 0);
						treeTable.repaint();
					}
				});
			}
			return;
		}

		final CyNetwork updateSelected = nodeEdgeTables.get(table);

		// Case 2: Selection updated.
		if (updateSelected != null && record.getColumn().equals(CyNetwork.SELECTED)) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					treeTable.repaint();
				}
			});
		}
	}

	@Override
	public void handleEvent(final RemovedNodesEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				treeTable.repaint();
			}
		});
	}

	@Override
	public void handleEvent(final RemovedEdgesEvent e) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				treeTable.repaint();
			}
		});
	}

	@Override
	public void handleEvent(final SetSelectedNetworksEvent e) {
		updateNetworkTreeSelection();
	}

	@Override
	public void handleEvent(final NetworkViewAboutToBeDestroyedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view about to be destroyed: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null) {
					final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(netView.getModel());
					
					if (views.isEmpty())
						treeTable.repaint();
				}
			}
		});
	}

	@Override
	public void handleEvent(final NetworkViewAddedEvent nde) {
		final CyNetworkView netView = nde.getNetworkView();
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logger.debug("Network view added to NetworkPanel: " + netView);
				final NetworkTreeNode node = treeNodeMap.get(netView.getModel().getSUID());
				
				if (node != null)
					treeTable.repaint();
			}
		});
	}
	
	/**
	 * This method highlights a network in the NetworkPanel.
	 */
	@Override
	public void valueChanged(final TreeSelectionEvent e) {
		if (ignoreTreeSelectionEvents)
			return;

		final JTree tree = treeTable.getTree();

		// Sets the "current" network based on last node in the tree selected
		final NetworkTreeNode node = (NetworkTreeNode) tree.getLastSelectedPathComponent();
		
		if (node == null || node.getUserObject() == null)
			return;

		CyNetwork cn = node.getNetwork();
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
	
		/*
		if (cn instanceof CyRootNetwork) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			CyRootNetwork root = (CyRootNetwork) cn; 
					//((NetworkTreeNode) node.getFirstChild()).getNetwork()).getRootNetwork();

			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			// Determine the current network
			if (!selectedNetworks.isEmpty())
				cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		} else { */
			// Regular multiple networks selection...
			try {
				// Create a list of all selected networks
				for (int i = tree.getMinSelectionRow(); i <= tree.getMaxSelectionRow(); i++) {
					NetworkTreeNode tn = (NetworkTreeNode) tree.getPathForRow(i).getLastPathComponent();
					
					if (tn != null && tn.getUserObject() != null && tree.isRowSelected(i))
						selectedNetworks.add(tn.getNetwork());
				}
			} catch (Exception ex) {
				logger.error("Error creating the list of selected networks", ex);
			}
	//	}
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}
	
	// // Private Methods // //
	
	private void addNetwork(final CyNetwork network) {
		// first see if it is not in the tree already
		if (this.network2nodeMap.get(network) == null) {
			NetworkTreeNode parentTreeNode = null;
			CyRootNetwork parentNetwork = null;
			
			// In current version, ALL networks are created as Subnetworks.
			// So, this should be always true.
			if (network instanceof CySubNetwork) {
				parentNetwork = ((CySubNetwork) network).getRootNetwork();
				parentTreeNode = treeNodeMap.get(parentNetwork.getSUID());
			}

			if (parentTreeNode == null){
				final String rootNetName = parentNetwork.getRow(parentNetwork).get(CyNetwork.NAME, String.class);
				parentTreeNode = new NetworkTreeNode(rootNetName, parentNetwork);
				nameTables.put(parentNetwork.getDefaultNetworkTable(), parentNetwork);
				network2nodeMap.put(parentNetwork, parentTreeNode);
			}

			// Actual tree node for this network
			String netName = network.getRow(network).get(CyNetwork.NAME, String.class);

			if (netName == null) {
				logger.error("Network name is null--SUID=" + network.getSUID());
				netName = "? (SUID: " + network.getSUID() + ")";
			}

			final NetworkTreeNode dmtn = new NetworkTreeNode(netName, network);
			network2nodeMap.put(network, dmtn);
			parentTreeNode.add(dmtn);

			if (treeNodeMap.values().contains(parentTreeNode) == false)
				root.add(parentTreeNode);

			// Register top-level node to map
			if (parentNetwork != null)
				treeNodeMap.put(parentNetwork.getSUID(), parentTreeNode);

			treeNodeMap.put(network.getSUID(), dmtn);
			
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					ignoreTreeSelectionEvents = true;
					// apparently this doesn't fire valueChanged
					treeTable.getTree().collapsePath(new TreePath(new TreeNode[] { root }));
					
					treeTable.getTree().updateUI();
					final TreePath path = new TreePath(dmtn.getPath());
					
					treeTable.getTree().expandPath(path);
					treeTable.getTree().scrollPathToVisible(path);
					treeTable.doLayout();
					
					ignoreTreeSelectionEvents = false;
				}
			});
		}
		
		nameTables.put(network.getDefaultNetworkTable(), network);
		nodeEdgeTables.put(network.getDefaultNodeTable(), network);
		nodeEdgeTables.put(network.getDefaultEdgeTable(), network);
	}
	
	/**
	 * Remove a network from the panel.
	 */
	private void removeNetwork(final CyNetwork network) {
		nameTables.values().removeAll(Collections.singletonList(network));
		nodeEdgeTables.values().removeAll(Collections.singletonList(network));
		treeNodeMap.remove(network.getSUID());
		final NetworkTreeNode node = network2nodeMap.remove(network);
		
		if (node == null)
			return;
		
		final List<NetworkTreeNode> removedChildren = new ArrayList<NetworkTreeNode>();
		final Enumeration<?> children = node.children();
		
		if (children.hasMoreElements()) {
			while (children.hasMoreElements())
				removedChildren.add((NetworkTreeNode) children.nextElement());
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				
				for (final NetworkTreeNode child : removedChildren) {
					child.removeFromParent();
					root.add(child);
				}
				
				final NetworkTreeNode parentNode = (NetworkTreeNode) node.getParent();
				node.removeFromParent();

				if (parentNode.isLeaf()) {
					// Remove from root node
					final CyNetwork parentNet = parentNode.getNetwork();
					nameTables.values().removeAll(Collections.singletonList(parentNet));
					nodeEdgeTables.values().removeAll(Collections.singletonList(parentNet));
					network2nodeMap.remove(parentNet);
					treeNodeMap.remove(parentNet.getSUID());
					
					parentNode.removeFromParent();
				}
				
				treeTable.getTree().updateUI();
				treeTable.repaint();
				
				ignoreTreeSelectionEvents = false;
			}
		});
	}

	/**
	 * Update selected row.
	 */
	private final void updateNetworkTreeSelection() {
		final List<TreePath> paths = new ArrayList<TreePath>();
		final List<CyNetwork> selectedNetworks = appMgr.getSelectedNetworks();
		
		for (final CyNetwork network : selectedNetworks) {
			final NetworkTreeNode node = this.network2nodeMap.get(network);
			
			if (node != null) {
				final TreePath tp = new TreePath(node.getPath());
				paths.add(tp);
			}
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ignoreTreeSelectionEvents = true;
				treeTable.getTree().getSelectionModel().setSelectionPaths(paths.toArray(new TreePath[paths.size()]));
				ignoreTreeSelectionEvents = false;
		
				int maxRow = 0;
		
				for (final TreePath tp : paths) {
					final int row = treeTable.getTree().getRowForPath(tp);
					maxRow = Math.max(maxRow, row);
				}
				
				final int row = maxRow;
		
				treeTable.getTree().scrollRowToVisible(row);
				treeTable.repaint();
			}
		});
	}

	private void selectAllSubnetwork(){
		if (selectedRootSet == null)
			return;
		
		final List<CyNetwork> selectedNetworks = new LinkedList<CyNetwork>();
		CyNetwork cn = null;
		NetworkTreeNode node = null;
		
		for (final CyRootNetwork root : selectedRootSet) {
			// This is a "network set" node...
			// When selecting root node, all of the subnetworks are selected.
			node = this.network2nodeMap.get(root);
			
			// Creates a list of all selected networks
			List<CySubNetwork> subNetworks = root.getSubNetworkList();
			
			for (CySubNetwork sn : subNetworks) {
				if (netMgr.networkExists(sn.getSUID()))
					selectedNetworks.add(sn);
			}
			
			cn = root;
		}
		
		// Determine the current network
		if (!selectedNetworks.isEmpty())
			cn = ((NetworkTreeNode) node.getFirstChild()).getNetwork();
		
		final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
		
		for (final CyNetwork n : selectedNetworks) {
			final Collection<CyNetworkView> views = netViewMgr.getNetworkViews(n);
			
			if (!views.isEmpty())
				selectedViews.addAll(views);
		}
		
		// No need to set the same network again. It should prevent infinite loops.
		// Also check if the network still exists (it could have been removed by another thread).
		if (cn == null || netMgr.networkExists(cn.getSUID())) {
			if (cn == null || !cn.equals(appMgr.getCurrentNetwork()))
				appMgr.setCurrentNetwork(cn);
		
			CyNetworkView cv = null;
			
			// Try to get the first view of the current network
			final Collection<CyNetworkView> cnViews = cn != null ? netViewMgr.getNetworkViews(cn) : null;
			cv = (cnViews == null || cnViews.isEmpty()) ? null : cnViews.iterator().next();
			
			if (cv == null || !cv.equals(appMgr.getCurrentNetworkView()))
				appMgr.setCurrentNetworkView(cv);
			
			appMgr.setSelectedNetworks(selectedNetworks);
			appMgr.setSelectedNetworkViews(selectedViews);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addFactory(TaskFactory factory, Map props) {
		CyAction action;
		if ( props.containsKey("enableFor") )
			action = new TaskFactoryTunableAction(taskMgr, factory, props, appMgr, netViewMgr);
		else
			action = new TaskFactoryTunableAction(taskMgr, factory, props);

		final JMenuItem item = new JMenuItem(action);

		Double gravity = 10.0;
		if (props.containsKey(ServiceProperties.MENU_GRAVITY)){
			gravity = Double.valueOf(props.get(ServiceProperties.MENU_GRAVITY).toString());
		}
		
		this.actionGravityMap.put(item, gravity);
		
		popupMap.put(factory, item);
		popupActions.put(factory, action);
		int menuIndex = getMenuIndexByGravity(item);
		popup.insert(item, menuIndex);
		popup.addPopupMenuListener(action);
	}

	private int getMenuIndexByGravity(JMenuItem item) {
		Double gravity = this.actionGravityMap.get(item);		
		Double gravityX;
		for (int i=0; i < popup.getComponentCount(); i++ ){
			gravityX = this.actionGravityMap.get(popup.getComponent(i));
			if (gravity < gravityX){
				return i;
			}
		}
		
		return popup.getComponentCount();
	}
	
	private void removeFactory(TaskFactory factory) {
		JMenuItem item = popupMap.remove(factory);
		if (item != null)
			popup.remove(item);
		CyAction action = popupActions.remove(factory);
		if (action != null)
			popup.removePopupMenuListener(action);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends CyNetwork> Set<T> getSelectionNetworks(Class<T> type) {
		final Set<T> nets = new LinkedHashSet<T>();
		final JTree tree = treeTable.getTree();
		final TreePath[] selectionPaths = tree.getSelectionPaths();
		
		if (selectionPaths != null) {
			for (final TreePath tp : selectionPaths) {
				final CyNetwork n = ((NetworkTreeNode) tp.getLastPathComponent()).getNetwork();
				
				if (n != null && type.isAssignableFrom(n.getClass()))
					nets.add((T) n);
			}
		}
		
		return nets;
	}
	
	// // Private Classes // //
	
	/**
	 * This class listens to mouse events from the TreeTable, if the mouse event
	 * is one that is canonically associated with a popup menu (ie, a right
	 * click) it will pop up the menu with option for destroying view, creating
	 * view, and destroying network (this is platform specific apparently)
	 */
	private final class PopupListener extends MouseAdapter {

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		// On Windows, popup is triggered by mouse release, not press 
		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		/**
		 * if the mouse press is of the correct type, this function will maybe
		 * display the popup
		 */
		private final void maybeShowPopup(final MouseEvent e) {
			// Ignore if not valid trigger.
			if (!e.isPopupTrigger())
				return;

			// get the row where the mouse-click originated
			final int row = treeTable.rowAtPoint(e.getPoint());
			if (row == -1)
				return;

			final JTree tree = treeTable.getTree();
			final TreePath treePath = tree.getPathForRow(row);
			Long networkID = null;
			
			try {
				final CyNetwork clickedNet = ((NetworkTreeNode) treePath.getLastPathComponent()).getNetwork();
				
				if (clickedNet instanceof CyRootNetwork) {
					networkID = null;
					selectedRoot = (CyRootNetwork) clickedNet;
				} else {
					networkID = clickedNet.getSUID();
					selectedRoot = null;
				}
			} catch (NullPointerException npe) {
				// The tree root does not represent a network, ignore it.
				return;
			}

			if (networkID != null) {
				final CyNetwork network = netMgr.getNetwork(networkID);
				
				if (network != null) {
					// if the network is not selected, select it
					final List<CyNetwork> selectedList = appMgr.getSelectedNetworks();
					
					if (selectedList == null || !selectedList.contains(network)) {
						appMgr.setCurrentNetwork(network);
						appMgr.setSelectedNetworks(Arrays.asList(new CyNetwork[]{ network }));
//						final Collection<CyNetworkView> netViews = netViewMgr.getNetworkViews(network);
//						appMgr.setSelectedNetworkViews(new ArrayList<CyNetworkView>(netViews));
					}
					
					// Make sure all network views of selected networks are selected as well,
					// so tasks/actions are executed on all of them
					final List<CyNetworkView> selectedViews = new ArrayList<CyNetworkView>();
					
					for (final CyNetwork n : appMgr.getSelectedNetworks()) {
						if (netMgr.networkExists(n.getSUID()))
							selectedViews.addAll(netViewMgr.getNetworkViews(n));
					}
					
					appMgr.setSelectedNetworkViews(selectedViews);
					
					// Always repaint, because the rendering of the tree selection
					// may be out of sync with the AppManager one
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							treeTable.repaint();
						}
					});
					
					// enable/disable any actions based on state of system
					for (CyAction action : popupActions.values())
						action.updateEnableState();

					// then popup menu
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			} else if (selectedRoot != null) {
				// if the right-clicked root-network is not selected, select it (other selected items will be unselected)
				selectedRootSet = getSelectionNetworks(CyRootNetwork.class);
				
				if (!selectedRootSet.contains(selectedRoot)) {
					final NetworkTreeNode node = network2nodeMap.get(selectedRoot);
					
					if (node != null) {
						appMgr.setCurrentNetwork(null);
						appMgr.setSelectedNetworks(null);
						appMgr.setSelectedNetworkViews(null);
						
						selectedRootSet = Collections.singleton(selectedRoot);
						final TreePath tp = new TreePath(new NetworkTreeNode[]{ root, node });
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								ignoreTreeSelectionEvents = true;
								tree.getSelectionModel().setSelectionPaths(new TreePath[]{ tp });
								treeTable.repaint();
								ignoreTreeSelectionEvents = false;
							}
						});
					}
				}
				
				editRootNetworTitle.setEnabled(selectedRootSet.size() == 1);
				rootPopupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	class TreeCellRenderer extends DefaultTreeCellRenderer {

		private final static long serialVersionUID = 1213748836751014L;
		
		private final String NETWORK_ICON = "/images/network_16.png";
		private final String NETWORK_LEAF_ICON = "/images/blank_icon_16.png";

		private final Dimension CELL_SIZE = new Dimension(1200, 40);

		TreeCellRenderer() {
			final Image iconImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource(NETWORK_ICON));
			final ImageIcon icon = new ImageIcon(iconImage);

			// If we don't provide a leaf Icon, a default one will be used.
			final Image iconImageLeaf = Toolkit.getDefaultToolkit().getImage(getClass().getResource(NETWORK_LEAF_ICON));
			final ImageIcon iconLeaf = new ImageIcon(iconImageLeaf);
			
			setClosedIcon(icon);
			setOpenIcon(icon);
			setLeafIcon(iconLeaf);
		}

		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			setPreferredSize(CELL_SIZE);
			setSize(CELL_SIZE);
			setBackground(sel ? UIManager.getColor("Table.focusCellBackground") : UIManager.getColor("Table.background"));
			setForeground(sel ? UIManager.getColor("Table.focusCellForeground") : UIManager.getColor("Table.foreground"));
			
			final NetworkTreeNode treeNode = value instanceof NetworkTreeNode ? (NetworkTreeNode) value : null;

			if (treeNode != null && treeNode.getNetwork() instanceof CySubNetwork) {
				if (!sel)
					setForeground(netViewMgr.viewExists(treeNode.getNetwork()) ?
							UIManager.getColor("Table.foreground") : UIManager.getColor("TextField.inactiveForeground"));
				
				try {
					setToolTipText(treeNode.getNetwork().getRow(treeNode.getNetwork()).get(CyNetwork.NAME, String.class));
				} catch (NullPointerException e) {
					// It's possible that the network got deleted but we haven't been notified yet.
				}
			}
			
			return this;
		}
	}
}
