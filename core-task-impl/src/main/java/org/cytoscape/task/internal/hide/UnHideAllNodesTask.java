package org.cytoscape.task.internal.hide;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
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


import java.util.List;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.undo.UndoSupport;


public class UnHideAllNodesTask extends AbstractNetworkViewTask {
	private final UndoSupport undoSupport;
	private final CyEventHelper eventHelper;
	private final VisualMappingManager vmMgr;

	public UnHideAllNodesTask(final UndoSupport undoSupport,
							  final CyEventHelper eventHelper,
							  final VisualMappingManager vmMgr,
							  final CyNetworkView v) {
		super(v);
		this.undoSupport = undoSupport;
		this.eventHelper = eventHelper;
		this.vmMgr = vmMgr;
	}

	public void run(TaskMonitor e) {
		e.setProgress(0.0);
		
		final CyNetwork network = view.getModel();
		final List<CyNode> nodeList = network.getNodeList();
		undoSupport.postEdit(new HideEdit("Show All Nodes", view, nodeList, true, eventHelper, vmMgr));
		e.setProgress(0.2);
		
		HideUtils.setVisibleNodes(nodeList, true, view);
		e.setProgress(0.7);
		
		vmMgr.getVisualStyle(view).apply(view);
		view.updateView();
		e.setProgress(1.0);
	} 
}
