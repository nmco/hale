/*
 * Copyright (c) 2016 Data Harmonisation Panel
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
 *     Data Harmonisation Panel <http://www.dhpanel.eu>
 */

package eu.esdihumboldt.hale.common.align.model.impl.mdexpl;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.pegdown.Extensions;
import org.pegdown.PegDownProcessor;

import com.google.common.collect.ListMultimap;

import de.fhg.igd.slf4jplus.ALogger;
import de.fhg.igd.slf4jplus.ALoggerFactory;
import eu.esdihumboldt.hale.common.align.extension.function.AbstractFunction;
import eu.esdihumboldt.hale.common.align.extension.function.AbstractParameter;
import eu.esdihumboldt.hale.common.align.extension.function.FunctionUtil;
import eu.esdihumboldt.hale.common.align.model.Cell;
import eu.esdihumboldt.hale.common.align.model.Entity;
import eu.esdihumboldt.hale.common.align.model.impl.AbstractCellExplanation;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;
import groovy.text.TemplateEngine;

/**
 * Markdown based cell explanation.
 * 
 * @author Simon Templer
 */
public abstract class MarkdownCellExplanation extends AbstractCellExplanation {

	private static final ALogger log = ALoggerFactory.getLogger(MarkdownCellExplanation.class);

	private final PegDownProcessor pegdown = new PegDownProcessor(Extensions.AUTOLINKS | //
			Extensions.HARDWRAPS | //
			Extensions.SMARTYPANTS | //
			Extensions.TABLES);

	private final TemplateEngine engine = new GStringTemplateEngine();

	private final Map<Locale, Optional<Template>> templateCache = new HashMap<>();

	/**
	 * Get the explanation template for a given locale.
	 * 
	 * @param clazz the class to retrieve the template for
	 * @param locale the locale
	 * @return the loaded template as string, if available
	 */
	public Optional<Template> getTemplate(Class<?> clazz, Locale locale) {
		return templateCache.computeIfAbsent(locale, cl -> {
			return findResource(clazz, clazz.getSimpleName(), "md", cl).flatMap(url -> {
				try (Reader reader = new InputStreamReader(url.openStream(),
						StandardCharsets.UTF_8)) {
					return Optional.ofNullable(engine.createTemplate(reader));
				} catch (Exception e) {
					log.error("Could not read cell explanation template", e);
					return Optional.empty();
				}
			});
		});
	}

	private Optional<URL> findResource(Class<?> clazz, String baseName, String suffix,
			Locale locale) {
		ResourceBundle.Control control = ResourceBundle.Control
				.getControl(ResourceBundle.Control.FORMAT_DEFAULT);

		List<Locale> candidateLocales = control.getCandidateLocales(baseName, locale);

		for (Locale specificLocale : candidateLocales) {
			String bundleName = control.toBundleName(baseName, specificLocale);
			String resourceName = control.toResourceName(bundleName, suffix);

			URL url = clazz.getResource(resourceName);
			if (url != null) {
				return Optional.of(url);
			}
		}

		return Optional.empty();
	}

	@Override
	protected String getExplanation(Cell cell, boolean html, Locale locale) {
		Optional<Template> maybeTemplate = getTemplate(getClass(), locale);
		if (maybeTemplate.isPresent()) {
			Template template = maybeTemplate.get();

			// process template
			String explanation = template.make(createBinding(cell, html)).toString();

			if (html) {
				explanation = pegdown.markdownToHtml(explanation);
			}

			return explanation;
		}
		else {
			return null;
		}
	}

	private Map<String, Object> createBinding(Cell cell, boolean html) {
		Map<String, Object> binding = new HashMap<>();

		AbstractFunction<? extends AbstractParameter> function = FunctionUtil
				.getFunction(cell.getTransformationIdentifier());

		// parameters
		binding.put("_params", new ParameterBinding(cell, function));

		// entities
		addEntityBindings(binding, function.getSource(), cell.getSource(), "_source", html);
		addEntityBindings(binding, function.getTarget(), cell.getTarget(), "_target", html);

		// customization
		customizeBinding(binding);

		return binding;
	}

	/**
	 * Customize the binding provided to the template.
	 * 
	 * @param binding the binding
	 */
	protected void customizeBinding(Map<String, Object> binding) {
		// override me
	}

	private void addEntityBindings(Map<String, Object> binding,
			Set<? extends AbstractParameter> definitions,
			ListMultimap<String, ? extends Entity> entities, String defaultName, boolean html) {
		if (!definitions.isEmpty()) {
			if (definitions.size() == 1) {
				// single entity
				AbstractParameter def = definitions.iterator().next();

				// _defaultName always maps to single entity
				addEntityBindingValue(defaultName, def, entities.get(def.getName()), html, binding);

				// in addition also the name if it is present
				String name = def.getName();
				if (name != null) {
					addEntityBindingValue(name, def, entities.get(name), html, binding);
				}
			}
			else {
				for (AbstractParameter def : definitions) {
					// add each entity based on its name, the default name is
					// used for the null entity
					String name = def.getName();
					if (name != null) {
						addEntityBindingValue(name, def, entities.get(name), html, binding);
					}
					else {
						// null entity -> default name
						addEntityBindingValue(defaultName, def, entities.get(name), html, binding);
					}
				}
			}
		}
	}

	private void addEntityBindingValue(String bindingName, AbstractParameter definition,
			List<? extends Entity> entities, boolean html, Map<String, Object> binding) {
		final Object entityBinding;
		if (definition.getMaxOccurrence() == 1) {
			// single entity
			if (entities.isEmpty()) {
				// not present
				entityBinding = null;
			}
			else {
				entityBinding = formatEntity(entities.get(0), html, true);
			}
		}
		else {
			// entity list
			entityBinding = entities.stream().map(entity -> {
				return formatEntity(entity, html, true);
			}).collect(Collectors.toList());
		}

		binding.put(bindingName, entityBinding);
	}

}