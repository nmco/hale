/*
 * Copyright (c) 2013 Simon Templer
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Simon Templer - initial version
 */

package eu.esdihumboldt.hale.common.schema.groovy

import javax.xml.namespace.QName

import org.codehaus.groovy.runtime.InvokerHelper

import eu.esdihumboldt.hale.common.schema.model.Definition
import eu.esdihumboldt.hale.common.schema.model.DefinitionGroup
import eu.esdihumboldt.hale.common.schema.model.DefinitionUtil
import eu.esdihumboldt.hale.common.schema.model.GroupPropertyDefinition
import eu.esdihumboldt.hale.common.schema.model.PropertyDefinition
import eu.esdihumboldt.hale.common.schema.model.Schema
import eu.esdihumboldt.hale.common.schema.model.TypeDefinition
import eu.esdihumboldt.hale.common.schema.model.constraint.type.Binding
import eu.esdihumboldt.hale.common.schema.model.constraint.type.HasValueFlag
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultGroupPropertyDefinition
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultPropertyDefinition
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultSchema
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeDefinition
import eu.esdihumboldt.hale.common.schema.model.impl.DefaultTypeIndex
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode



/**
 * Builder to easily create schemas.
 * 
 * @author Simon Templer
 */
@CompileStatic
class SchemaBuilder {

	/**
	 * The current node.
	 */
	def current

	/**
	 * The default namespace.
	 */
	String defaultNamespace

	/**
	 * The default namespace for default property types.
	 */
	String defaultPropertyTypeNamespace = ''

	/**
	 * The created default property types.
	 */
	private Map<Class, TypeDefinition> defaultTypes = [:]

	/**
	 * Set containing all used default property type names.
	 */
	private Set<String> defaultTypeNames = new HashSet<>()

	/**
	 * Reset the builder
	 */
	void reset() {
		current = null
		defaultTypes = [:]
		defaultTypeNames.clear()
	}

	/**
	 * Build a schema, then resets the builder.
	 * 
	 * @param namespace the schema namespace and default namespace of added
	 *   types and properties
	 * @param location the schema location or <code>null</code>
	 * @return
	 */
	Schema schema(String namespace = '', URI location = null, Closure closure) {
		def root = new DefaultSchema(namespace, location);
		defaultNamespace = namespace
		def parent = current
		current = root
		closure = (Closure) closure.clone()
		closure.delegate = this
		closure.call()
		current = parent
		reset()
		return root
	}

	// def types

	// def type

	/**
	 * Called on for any missing method.
	 * 
	 * @param name the method name
	 * @param args the arguments
	 * @return something
	 */
	@CompileStatic(TypeCheckingMode.SKIP)
	@TypeChecked
	def methodMissing(String name, def args) {
		List list = InvokerHelper.asList(args)

		// determine named parameters (must be first)
		Map attributes = null
		def start = 0
		if (list && list[0] instanceof Map) {
			attributes = (Map) list[0]
			start = 1
		}

		// determine closure (must be last)
		def end = list.size()
		Closure closure = null
		if (list && list.last() instanceof Closure) {
			closure = (Closure) list.last().clone()
			closure.delegate = this
			end--
		}

		// determine other parameters
		List params = null
		if (start < end) {
			params = list.subList(start, end)
		}

		def parent = current
		def node = createNode(name, attributes, params, parent, closure != null)
		current = node

		closure?.call()

		current = parent

		// return the node created by the call
		node
	}

	/**
	 * Create a new node.
	 * 
	 * @param name the node name
	 * @param attributes the named parameters, may be <code>null</code>
	 * @param params other parameters, may be <code>null</code>
	 * @param parent the parent node, may be <code>null</code>
	 * @param subClosure states if there is a sub-closure for this node
	 * @return the created node
	 */
	def createNode(String name, Map attributes, List params, def parent, boolean subClosure) {
		def node
		if (parent instanceof DefaultTypeIndex) {
			// create a type as child
			TypeDefinition type = createType(name, attributes, params)
			((DefaultTypeIndex) parent).addType(type)
			node = type
		}
		else if (parent instanceof Definition) {
			DefinitionGroup parentGroup = DefinitionUtil.getDefinitionGroup(parent)

			// create property or group as child
			if (name == '_') {
				// create a group
				GroupPropertyDefinition group = createGroup(attributes, params, parentGroup)
				node = group
			}
			else {
				// create a property
				PropertyDefinition property = createProperty(name, attributes, params,
						parentGroup, subClosure)
				node = property
			}
		}
		else {
			//TODO
		}

		node
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	QName createName(String name, Map attributes) {
		String ns
		if (attributes && attributes.namespace != null) {
			// use specified namespace
			// empty namespace allowed (to override default namespace)
			ns = attributes.namespace
		}
		else {
			ns = defaultNamespace
		}

		if (ns) {
			new QName(ns, name)
		}
		else {
			new QName(name)
		}
	}

	TypeDefinition createType(String name, Map attributes, List params) {
		QName typeName = createName(name, attributes)
		DefaultTypeDefinition type = new DefaultTypeDefinition(typeName)

		type
	}

	GroupPropertyDefinition createGroup(Map attributes, List params,
			DefinitionGroup parent) {
		QName name = new QName(defaultPropertyTypeNamespace,
				newDefaultPropertyTypeName('group'))
		DefaultGroupPropertyDefinition group = new DefaultGroupPropertyDefinition(
				name, parent, false)

		group
	}

	PropertyDefinition createProperty(String name, Map attributes, List params,
			DefinitionGroup parent, boolean subClosure) {

		// create property type
		TypeDefinition propertyType

		if (subClosure) {
			// the sub-closure defines an anonymous property type

			// a class specifying the value type may be given as parameter
			Class type = null
			if (params) {
				if (params[0] instanceof Class) {
					type = params[0]
				}
				else {
					//TODO error?
				}
			}
			propertyType = createDefaultNestingPropertyType(type, name)
		}
		else {
			/*
			 * The first parameter must be either
			 * - a class specifying the type of a simple default property
			 * - a TypeDefinition instance
			 * - a QName or string specifying the name of a type that may not be yet defined
			 */
			def type = String // default if nothing given
			if (params) {
				type = params[0]
			}

			if (type instanceof Class) {
				propertyType = getOrCreateDefaultPropertyType((Class) type)
			}
			else if (type instanceof TypeDefinition) {
				propertyType = (TypeDefinition) type
			}
			else if (type instanceof QName) {
				//TODO
			}
			else {
				String typeRef = type.toString()
				//TODO
			}
		}

		// create property
		QName propertyName = createName(name, attributes)
		DefaultPropertyDefinition property = new DefaultPropertyDefinition(propertyName, parent,
				propertyType);
	}

	/**
	 * Get the existing or create a new default property type for the given
	 * class.
	 * 	
	 * @param type the binding for the default property type
	 * @return the default property type definition
	 */
	protected TypeDefinition getOrCreateDefaultPropertyType(Class type) {
		TypeDefinition typeDef = defaultTypes.get(type)
		if (!typeDef) {
			typeDef = createDefaultPropertyType(type)
			defaultTypes.put(type, typeDef)
		}

		typeDef
	}

	/**
	 * Create a new default property type for the given class.
	 *
	 * @param type the binding for the default property type
	 * @return the default property type definition
	 */
	protected TypeDefinition createDefaultPropertyType(Class type) {
		QName name = new QName(defaultPropertyTypeNamespace,
				newDefaultPropertyTypeName(type.name.toLowerCase()))
		DefaultTypeDefinition typeDef = new DefaultTypeDefinition(name)

		// set binding & hasValue
		typeDef.setConstraint(Binding.get(type))
		typeDef.setConstraint(HasValueFlag.ENABLED)

		//TODO any others?

		typeDef
	}

	/**
	 * Create a new default property type for a nested property.
	 *
	 * @param type the binding for the property value or <code>null</code>
	 * @return the type definition
	 */
	protected TypeDefinition createDefaultNestingPropertyType(Class type, String propertyName) {
		QName name = new QName(defaultPropertyTypeNamespace,
				newDefaultPropertyTypeName(propertyName))
		DefaultTypeDefinition typeDef = new DefaultTypeDefinition(name)

		// set binding & hasValue
		if (type) {
			typeDef.setConstraint(Binding.get(type))
			typeDef.setConstraint(HasValueFlag.ENABLED)
		}

		//TODO any others?

		typeDef
	}

	/**
	 * Determine an unused local name for a default property type.
	 * 
	 * @param preferred the preferred name
	 * @return the local name to use
	 */
	protected String newDefaultPropertyTypeName(String preferred) {
		String name = preferred
		int count = 1
		while (defaultTypeNames.contains(name)) {
			count++
			name = preferred + count
		}

		name
	}

}
