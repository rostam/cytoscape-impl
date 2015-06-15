package org.cytoscape.tableimport.internal.reader;

/*
 * #%L
 * Cytoscape Table Import Impl (table-import-impl)
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


import static org.cytoscape.tableimport.internal.util.AttributeDataType.TYPE_BOOLEAN;
import static org.cytoscape.tableimport.internal.util.AttributeDataType.TYPE_FLOATING;
import static org.cytoscape.tableimport.internal.util.AttributeDataType.TYPE_INTEGER;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.tableimport.internal.util.AttributeDataType;

/**
 * Parse one line for network text table
 */
public class NetworkLineParser {
	
	private final NetworkTableMappingParameters nmp;
	private final List<Long> nodeList;
	private final List<Long> edgeList;
	private CyNetwork network;
	private Map<Object, CyNode> nMap;
	private final CyRootNetwork rootNetwork;
	
	public NetworkLineParser(List<Long> nodeList, List<Long> edgeList,
	                         final NetworkTableMappingParameters nmp,final Map<Object, CyNode> nMap, CyRootNetwork rootNetwork) {
		this.nmp = nmp;
		this.nodeList = nodeList;
		this.edgeList = edgeList;
		this.nMap = nMap;
		this.rootNetwork = rootNetwork;
	}

	public void parseEntry(String[] parts) {
		final CyEdge edge = addNodeAndEdge(parts);

		if (edge != null)
			addEdgeAttributes(edge, parts);
	}

	private CyEdge addNodeAndEdge(final String[] parts) {
		final CyNode source = createNode(parts, nmp.getSourceIndex());
		final CyNode target = createNode(parts, nmp.getTargetIndex());

		// Single column nodes list.  Just add nodes.
		if (source == null || target == null)
			return null;

		final String interaction;

		if ((nmp.getInteractionIndex() == -1) || (nmp.getInteractionIndex() > (parts.length - 1))
		    || (parts[nmp.getInteractionIndex()] == null)) {
			interaction = nmp.getDefaultInteraction();
		} else {
			interaction = parts[nmp.getInteractionIndex()];
		}

		//edge = Cytoscape.getCyEdge(source, target, Semantics.INTERACTION, interaction, true, true);
		CyEdge edge = network.addEdge(source, target, true);
		network.getRow(edge).set("interaction", interaction);
		String edgeName = network.getRow(source).get("name", String.class)+ " ("+interaction+") "+ network.getRow(target).get("name", String.class);
		network.getRow(edge).set("name", edgeName);

		edgeList.add(edge.getSUID());

		return edge;
	}


	private CyNode createNode(final String[] parts, final Integer nodeIndex) {

		CyNode node = null;

		if (nodeIndex.equals(-1) == false && (nodeIndex <= (parts.length - 1)) && (parts[nodeIndex] != null)) {
			//node = Cytoscape.getCyNode(parts[nodeIndex], true);

//			CyNode existingNode = nodeMap.get(parts[nodeIndex]);
//			if (existingNode != null) {
//				return existingNode;
//			}
//			node = network.addNode();
//			network.getRow(node).set("name", parts[nodeIndex]);
//
//			nodeMap.put(parts[nodeIndex], node);

			CyNode existingNode;
			if (this.nMap.get(parts[nodeIndex]) == null){
				// node does not exist yet, create it
				node = network.addNode();
				network.getRow(node).set("name", parts[nodeIndex]);
				this.nMap.put(parts[nodeIndex], this.rootNetwork.getNode(node.getSUID()));
			}
			else {// already existed in parent network
				CyNode parentNode = this.nMap.get(parts[nodeIndex]);
				CySubNetwork subnet = (CySubNetwork) network;
				subnet.addNode(parentNode);
				existingNode = subnet.getNode(parentNode.getSUID());
				return existingNode;
			}
			
			nodeList.add(node.getSUID());
		}

		return node;
	}

	private void addEdgeAttributes(final CyEdge edge, final String[] parts) {
		for (int i = 0; i < parts.length; i++) {
			if ((i != nmp.getSourceIndex()) && (i != nmp.getTargetIndex())
			    && (i != nmp.getInteractionIndex()) && parts[i] != null ) {
				if ((nmp.getImportFlag().length > i) && (nmp.getImportFlag()[i] == true)) {
					mapAttribute(edge, parts[i].trim(), i);
				}
			}
		}
	}

	private void createColumn(final CyEdge edge, final String attributeName, Class<?> theType){
		// If attribute does not exist, create it
		if (network.getRow(edge).getTable().getColumn(attributeName) == null)
			network.getRow(edge).getTable().createColumn(attributeName, theType, false);
	}

	/**
	 * Based on the attribute types, map the entry to CyAttributes.
	 */
	@SuppressWarnings("unchecked")
	private void mapAttribute(final CyEdge edge, final String entry, final int index) {

		final AttributeDataType type = nmp.getAttributeTypes()[index];

		if (entry == null || entry.length() == 0)
			return;
			
		switch (type) {
			case TYPE_BOOLEAN:
				createColumn(edge, nmp.getAttributeNames()[index], Boolean.class);
				network.getRow(edge).set(nmp.getAttributeNames()[index], new Boolean(entry));

				break;

			case TYPE_INTEGER:
				createColumn(edge, nmp.getAttributeNames()[index], Integer.class);
				network.getRow(edge).set(nmp.getAttributeNames()[index], new Integer(entry));

				break;

			case TYPE_FLOATING:
				createColumn(edge, nmp.getAttributeNames()[index], Double.class);
				network.getRow(edge).set(nmp.getAttributeNames()[index], new Double(entry));

				break;

			case TYPE_STRING:
				createColumn(edge, nmp.getAttributeNames()[index], String.class);
				network.getRow(edge).set(nmp.getAttributeNames()[index], entry.trim());

				break;

			case TYPE_BOOLEAN_LIST:
			case TYPE_INTEGER_LIST:
			case TYPE_FLOATING_LIST:
			case TYPE_STRING_LIST:
				final CyTable table = network.getRow(edge).getTable();
				
				if (table.getColumn(nmp.getAttributeNames()[index]) == null)
					table.createListColumn(nmp.getAttributeNames()[index], type.getListType(), false);

				/*
				 * In case of list, not overwrite the attribute. Get the existing list, and add it to the list.
				 */
				List<Object> curList = network.getRow(edge).get(nmp.getAttributeNames()[index], List.class);

				if (curList == null)
					curList = new ArrayList<Object>();

				curList.addAll(buildList(entry, type));

				//nmp.getAttributes().setListAttribute(key, nmp.getAttributeNames()[index], curList);
				network.getRow(edge).set(nmp.getAttributeNames()[index], curList);
				
				break;
		}
	}

	/**
	 * If an entry is a list, split the string and create new List Attribute.
	 */
	private List<Object> buildList(final String entry, final AttributeDataType type) {
		if (entry == null)
			return null;

		final List<Object> listAttr = new ArrayList<>();

		final String[] parts = (entry.replace("\"", "")).split(nmp.getListDelimiter());

		for (String listItem : parts) {
			if (type == TYPE_BOOLEAN)
				listAttr.add(new Boolean(listItem.trim()));
			else if (type == TYPE_INTEGER)
				listAttr.add(new Integer(listItem.trim()));
			else if (type == TYPE_FLOATING)
				listAttr.add(new Double(listItem.trim()));
			else // TYPE_STRING or unknown
				listAttr.add(listItem.trim());				
		}

		return listAttr;
	}

	public void setNetwork(CyNetwork network){
		this.network = network;
	}
}
