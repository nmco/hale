/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2010.
 */

package eu.esdihumboldt.hale.models;

import java.net.URI;

import org.geotools.feature.FeatureType;
import org.semanticweb.knowledgeweb.heterogeneity.alignment.AlignmentDocument;
import org.semanticweb.knowledgeweb.heterogeneity.alignment.AlignmentType;

/**
 * The {@link AlignmentService} provides access to the currently loaded 
 * alignment. The model it uses is directly derived from OML (Ontology
 * Mapping Language)
 * 
 * @author Thorsten Reitz
 * @version {$Id}
 */
public interface AlignmentService {
	
	/**
	 * @return the entire {@link AlignmentDocument} as currently represented in the Alignment Model.
	 */
	public AlignmentDocument getAlignment();
	
	/**
	 * @param alignment the {@link AlignmentType} to update or add to the Alignment Model.
	 * @return true if an existing alignment was updated, false if a new one was added.
	 */
	public boolean addOrUpdateAlignment(AlignmentType alignment);
	
	/**
	 * This method is used to return all Alignments that have the given type 
	 * as their source or target as a {@link AlignmentDocument}.
	 * @param type the {@link FeatureType} for which to return the Alignments.
	 * @return
	 */
	public AlignmentDocument getAlignmentForType(FeatureType type);
	
	/**
	 * Adds the alignments defined in an OML file to the currently loaded ones if the alignments
	 * match the currently loaded schemas.
	 * @param file the {@link URI} to the file from which to load alignments.
	 * @return true if the loading was successful.
	 */
	public boolean loadAlignment(URI file);
	
	/**
	 * Invoke this operation if you want to clear out all alignments stored. 
	 * This method is required when one wants to start working on a new alignment.
	 * @return true if the cleaning was successful.
	 */
	public boolean cleanModel();

}
