package org.cytoscape.task.internal.loaddatatable;

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



import java.net.URI;

import org.cytoscape.io.read.CyTableReader;
import org.cytoscape.io.read.CyTableReaderManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;


abstract class AbstractLoadTableTask extends AbstractTask {

	private final CyServiceRegistrar serviceRegistrar;
	
	public AbstractLoadTableTask(final CyServiceRegistrar serviceRegistrar) {
		this.serviceRegistrar = serviceRegistrar;
	}

	void loadTable(final String name, final URI uri, boolean combine, final TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setStatusMessage("Finding Table Data Reader...");

		final CyTableReaderManager tableReaderMgr = serviceRegistrar.getService(CyTableReaderManager.class);
		final CyTableReader reader = tableReaderMgr.getReader(uri, uri.toString());

		if (reader == null)
			throw new NullPointerException("Failed to find reader for specified file.");
		
		if (combine) {
			taskMonitor.setStatusMessage("Importing Data Table...");
			insertTasksAfterCurrentTask(new CombineReaderAndMappingTask(reader, serviceRegistrar));
		} else {
			taskMonitor.setStatusMessage("Loading Data Table...");
			insertTasksAfterCurrentTask(
					new ReaderTableTask(reader, serviceRegistrar),
					new AddImportedTableTask(reader, serviceRegistrar)
			);
		}
	}
}

