/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2011.
 */

package eu.esdihumboldt.hale.io.gml.writer.internal.geometry;

import java.text.MessageFormat;
import java.util.List;
import java.util.ListIterator;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import eu.esdihumboldt.hale.io.gml.writer.internal.GmlWriterUtil;

/**
 * Represents a descent in the document, must be used to end elements 
 * started with 
 *
 * @author Simon Templer
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 */
public class Descent {
	
	private final XMLStreamWriter writer;
	
	private final DefinitionPath path;

	/**
	 * Constructor
	 * 
	 * @param writer the XMl stream writer
	 * @param path the descent path
	 */
	private Descent(XMLStreamWriter writer, DefinitionPath path) {
		super();
		this.path = path;
		this.writer = writer;
	}

	/**
	 * @return the path
	 */
	public DefinitionPath getPath() {
		return path;
	}

	/**
	 * Close the descent
	 * 
	 * @throws XMLStreamException if an error occurs closing the elements
	 */
	public void close() throws XMLStreamException {
		if (path.isEmpty()) {
			return;
		}
		
		for (int i = 0; i < path.getSteps().size(); i++) {
			writer.writeEndElement();
		}
	}
	
	/**
	 * Descend the given path
	 * 
	 * @param writer the XML stream writer 
	 * @param descendPath the path to descend
	 * @param previousDescent the previous descent, that will be closed or 
	 *   partially closed as needed, may be <code>null</code> 
	 * @param generateRequiredIDs if required IDs shall be generated for the 
	 *   path elements
	 * @return the descent that was opened, it must be closed to close the
	 *   opened elements
	 * @throws XMLStreamException if an error occurs writing the coordinates
	 */
	public static Descent descend(XMLStreamWriter writer, 
			DefinitionPath descendPath, Descent previousDescent, boolean generateRequiredIDs) throws XMLStreamException {
		if (descendPath.isEmpty()) {
			if (previousDescent != null) {
				// close previous descent
				previousDescent.close();
			}
			return new Descent(writer, descendPath);
		}

		List<PathElement> stepDown = descendPath.getSteps();
		PathElement downFrom = null;
		PathElement downAfter = null;
		
		if (previousDescent != null) {
			List<PathElement> previousSteps = previousDescent.getPath().getSteps();
			
			// find the first non-unique match in both paths 
			PathElement firstNonUniqueMatch = null;
			//XXX should we check from the beginning how far the paths match?
			for (PathElement step : previousSteps) {
				if (stepDown.contains(step)) {
					if (!step.isUnique()) {
						firstNonUniqueMatch = step;
						// found the first non-unique match
						break;
					}
				}
				else {
					// after the first miss no more valid matches can be found
					break;
				}
			}
			
			// close previous descent as needed
			ListIterator<PathElement> itPrev = previousSteps.listIterator(previousSteps.size());
			if (firstNonUniqueMatch == null) {
				boolean endedSomething = false;
				while (itPrev.hasPrevious()) {
					PathElement step = itPrev.previous();
					if (stepDown.contains(step)) {
						// step is contained in both paths
						if (step.isUnique()) {
							// step may not be closed, as the next path also wants to enter
							// from the next path all steps before and including this step must be ignored for stepping down
							downAfter = step;
	 						break;
	 					}
	 				}
					
					// close step
					writer.writeEndElement();
					endedSomething = true;
	 			}
				
				if (!endedSomething) {
					throw new IllegalStateException(MessageFormat.format(
							"Previous path ''{0}'' has only unique common elements with path ''{1}'', therefore a sequence of both is not possible", 
							previousDescent.getPath().toString(), descendPath.toString()));
				}
			}
			else {
				while (itPrev.hasPrevious()) {
					// close step
					writer.writeEndElement();
					
					PathElement step = itPrev.previous();
					if (firstNonUniqueMatch.equals(step)) {
						// step after this may not be closed, as the next path also wants to enter
						// from the next path all steps before this step must be ignored for stepping down
						downFrom = step;
						break;
					}
				}
			}
		}
		
		for (PathElement step : stepDown) {
			if (downFrom != null && downFrom.equals(step)) {
				downFrom = null;
			}
			
			if (downFrom == null && downAfter == null) {
				// start elements
				GmlWriterUtil.writeStartPathElement(writer, step, generateRequiredIDs);
			}
			
			if (downAfter != null && downAfter.equals(step)) {
				downAfter = null;
			}
		}
		
		return new Descent(writer, descendPath); 
	}
	
}
