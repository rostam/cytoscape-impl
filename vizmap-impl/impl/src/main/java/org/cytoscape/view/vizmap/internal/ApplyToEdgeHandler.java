package org.cytoscape.view.vizmap.internal;

/*
 * #%L
 * Cytoscape VizMap Impl (vizmap-impl)
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

import java.util.Collection;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyRow;
import org.cytoscape.view.model.View;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualStyle;

public class ApplyToEdgeHandler extends AbstractApplyHandler<CyEdge> {


	ApplyToEdgeHandler(final VisualStyle style, final VisualLexiconManager lexManager) {
		super(style, lexManager);
	}

	@Override
	public void apply(final CyRow row, final View<CyEdge> edgeView) {
		final Collection<VisualProperty<?>> edgeVP = lexManager.getEdgeVisualProperties();
		applyValues(row, edgeView, edgeVP);
	}
}
