/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                  01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to this website:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to : http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2010.
 *
 * Component    : HALE
 * Created on   : Jun 3, 2009 -- 4:50:10 PM
 */
package eu.esdihumboldt.hale.models.schema;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.cs3d.util.logging.ALogger;
import de.cs3d.util.logging.ALoggerFactory;
import de.cs3d.util.logging.ATransaction;

import eu.esdihumboldt.hale.models.SchemaService;
import eu.esdihumboldt.hale.schemaprovider.ProgressIndicator;
import eu.esdihumboldt.hale.schemaprovider.Schema;
import eu.esdihumboldt.hale.schemaprovider.SchemaProvider;
import eu.esdihumboldt.hale.schemaprovider.model.AttributeDefinition;
import eu.esdihumboldt.hale.schemaprovider.model.Definition;
import eu.esdihumboldt.hale.schemaprovider.model.SchemaElement;
import eu.esdihumboldt.hale.schemaprovider.provider.ApacheSchemaProvider;
import eu.esdihumboldt.hale.schemaprovider.provider.ShapeSchemaProvider;

/**
 * Implementation of {@link SchemaService}. It uses a {@link SchemaProvider}
 * for actually loading the Schema
 * 
 * @author Simon Templer, Fraunhofer IGD
 * @version $Id$
 */
public class SchemaProviderService 
	extends AbstractSchemaService {
	
	private static ALogger log = ALoggerFactory.getLogger(SchemaProviderService.class); 
	
	/**
	 * Source schema
	 */
	private Schema sourceSchema = Schema.EMPTY_SCHEMA;

	/** 
	 * Target schema 
	 */
	private Schema targetSchema = Schema.EMPTY_SCHEMA;
	
	private final Set<SchemaProvider> providers = new HashSet<SchemaProvider>();
	
	/**
	 * Creates the schema service
	 */
	public SchemaProviderService() {
		super();
		
		providers.add(new ApacheSchemaProvider());
		providers.add(new ShapeSchemaProvider());
	}

	/**
	 * @see SchemaService#cleanSourceSchema()
	 */
	public boolean cleanSourceSchema() {
		sourceSchema = Schema.EMPTY_SCHEMA;
		notifySchemaChanged(SchemaType.SOURCE);
		
		return true;
	}

	/**
	 * @see SchemaService#cleanTargetSchema()
	 */
	public boolean cleanTargetSchema() {
		targetSchema = Schema.EMPTY_SCHEMA;
		notifySchemaChanged(SchemaType.TARGET);
		
		return true;
	}

	/**
	 * @see SchemaService#getSourceSchema()
	 */
	public Collection<SchemaElement> getSourceSchema() {
		return sourceSchema.getElements().values();
	}

	/**
	 * @see SchemaService#getTargetSchema()
	 */
	public Collection<SchemaElement> getTargetSchema() {
		return targetSchema.getElements().values();
	}

	/**
	 * @see SchemaService#loadSchema(URI, String, SchemaType, ProgressIndicator)
	 */
	@Override
	public boolean loadSchema(URI location, String schemaFormat, SchemaType type, ProgressIndicator progress) throws IOException {
		ATransaction logTrans = log.begin("Loading " + type + " schema from " + location.toString());
		try {
			SchemaProvider provider = getSchemaProvider((schemaFormat != null)?(schemaFormat):(determineSchemaFormat(location)));
			Schema schema = provider.loadSchema(location, progress);
			
			if (type.equals(SchemaType.SOURCE)) {
				sourceSchema = schema;
			} 
			else {
				targetSchema = schema;
			}
			
			notifySchemaChanged(type);
			return true;
		} finally {
			logTrans.end();
		}
	}

	private SchemaProvider getSchemaProvider(String format) {
		for (SchemaProvider provider : providers) {
			if (provider.supportsSchemaFormat(format)) {
				return provider;
			}
		}
		
		throw new IllegalArgumentException("No schema provider for the given format: " + format);
	}

	private String determineSchemaFormat(URI location) {
		String loc = location.toString();
		
		// special cases
		
		// WFS describe feature type
		if (loc.toLowerCase().contains("request=describefeaturetype")) {
			return "xsd";
		}
		
		int index = loc.lastIndexOf('.');
		if (index < 0) {
			throw new IllegalArgumentException("Unable to automatically determine schema format");
		}
		else {
			return loc.substring(index + 1);
		}
	}

	/**
	 * @see SchemaService#getSourceNameSpace()
	 */
	public String getSourceNameSpace() {
		return sourceSchema.getNamespace();
	}

	/**
	 * @see SchemaService#getSourceURL()
	 */
	public URL getSourceURL() {
		return sourceSchema.getLocation();
	}

	/**
	 * @see SchemaService#getTargetNameSpace()
	 */
	public String getTargetNameSpace() {
		return targetSchema.getNamespace();
	}

	/**
	 * @see SchemaService#getTargetURL()
	 */
	public URL getTargetURL() {
		return targetSchema.getLocation();
	}

	/**
	 * @see SchemaService#getElementByName(String)
	 */
	public SchemaElement getElementByName(String name) {
		SchemaElement result = null;
		// handles cases where a full name was given.
		if (!getSourceNameSpace().equals("") && name.contains(getSourceNameSpace())) {
			for (SchemaElement element : getSourceSchema()) {
				if (element.getElementName().getLocalPart().equals(name)) {
					result = element;
					break;
				}
			}
		}
		else if (!getTargetNameSpace().equals("") && name.contains(getTargetNameSpace())) {
			for (SchemaElement element : getTargetSchema()) {
				if (element.getElementName().getLocalPart().equals(name)) {
					result = element;
					break;
				}
			}
		}
		// handle case where only the local part was given.
		else {
			Collection<SchemaElement> allElements = new HashSet<SchemaElement>();
			allElements.addAll(getSourceSchema());
			allElements.addAll(getTargetSchema());
			for (SchemaElement element : allElements) {
				if (element.getElementName().getLocalPart().equals(name)) {
					result = element;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * @see SchemaService#getSchema(SchemaService.SchemaType)
	 */
	public Collection<SchemaElement> getSchema(SchemaType schemaType) {
		if (SchemaType.SOURCE.equals(schemaType)) {
			return getSourceSchema();
		}
		else {
			return getTargetSchema();
		}
	}

	/**
	 * @see SchemaService#getDefinition(String)
	 */
	@Override
	public Definition getDefinition(String identifier) {
		//XXX improve implementation?
		Definition result = getDefinition(identifier, sourceSchema);
		if (result == null) {
			result = getDefinition(identifier, targetSchema);
		}
		
		return result;
	}

	/**
	 * @see SchemaService#getDefinition(String, SchemaType)
	 */
	@Override
	public Definition getDefinition(String identifier, SchemaType schema) {
		return getDefinition(identifier, (schema == SchemaType.SOURCE)?(sourceSchema):(targetSchema));
	}

	private Definition getDefinition(String identifier, Schema schema) {
		//XXX improve implementation?
		Definition result = schema.getElements().get(identifier);
		
		if (result == null) {
			// not found as type, may be attribute
			int index = identifier.lastIndexOf('/');
			if (index > 0) {
				String subIdentifier = identifier.substring(0, index);
				String attributeName = identifier.substring(index + 1);
				
				SchemaElement type = schema.getElements().get(subIdentifier);
				
				if (type != null) {
					// try to find attribute
					for (AttributeDefinition attribute : type.getType().getAttributes()) {
						if (attribute.getName().equals(attributeName)) {
							return attribute;
						}
					}
				}
			}
		}
		
		return result;
	}
	
}
