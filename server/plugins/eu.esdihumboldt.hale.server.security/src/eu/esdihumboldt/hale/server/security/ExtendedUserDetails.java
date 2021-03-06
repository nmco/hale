/*
 * Copyright (c) 2013 Data Harmonisation Panel
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

package eu.esdihumboldt.hale.server.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * User details extended with additional information.
 * 
 * @author Simon Templer
 */
public interface ExtendedUserDetails extends UserDetails {

	/**
	 * If the user was newly created, e.g. by logging in with OpenID for the
	 * first time.
	 * 
	 * @return if the user is a new user
	 */
	public boolean isNewUser();

}
