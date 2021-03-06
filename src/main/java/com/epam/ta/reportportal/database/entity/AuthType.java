/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/commons-dao
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.ta.reportportal.database.entity;

import java.util.Arrays;

/**
 * Authentication mechanics enum for external system
 *
 * @author Andrei_Ramanchuk
 */
public enum AuthType {

	//@formatter:off
	OAUTH(false),
	NTLM(true),
	APIKEY(true),
	BASIC(true);
	//@formatter:on

	final boolean requiresPassword;

	AuthType(boolean requiresPassword) {
		this.requiresPassword = requiresPassword;
	}

	public boolean requiresPassword() {
		return requiresPassword;
	}

	public static AuthType findByName(String name) {
		return Arrays.stream(AuthType.values()).filter(type -> type.name()
                .equalsIgnoreCase(name)).findAny().orElse(null);
	}

	public static boolean isPresent(String name) {
		return null != findByName(name);
	}
}