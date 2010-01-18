/*
 * Copyright 2008 Oracle Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.osgi.jmx.codec;

import javax.management.openmbean.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.jmx.compendium.UserManagerMBean;
import org.osgi.service.useradmin.Authorization;

/**
 * @author Hal Hildebrand Date: Dec 5, 2008 Time: 7:40:14 AM
 * 
 */
public class OSGiAuthorization {
    private static final Log log = LogFactory.getLog(OSGiAuthorization.class);
    protected String name;
    protected String[] roles;

    public OSGiAuthorization(CompositeData data) {
        if (data != null) {
            this.name = (String) data.get(UserManagerMBean.USER_NAME);
            this.roles = (String[]) data.get(UserManagerMBean.ROLE_NAMES);
        }
    }

    public OSGiAuthorization(Authorization authorization) {
        this(authorization.getName(), authorization.getRoles());
    }

    public OSGiAuthorization(String name, String[] roles) {
        this.name = name;
        this.roles = roles;
    }

    private static CompositeType createAuthorizationType() {
        String description = "An authorization object defines which roles has a user got";
        String[] itemNames = UserManagerMBean.AUTHORIZATION;
        String[] itemDescriptions = new String[2];
        itemDescriptions[0] = "The user name for this authorization object";
        itemDescriptions[1] = "The names of the roles encapsulated by this auth object";
        OpenType[] itemTypes = new OpenType[2];
        itemTypes[0] = SimpleType.STRING;
        itemTypes[1] = Util.STRING_ARRAY_TYPE;
        try {
            return new CompositeType("Authorization", description, itemNames,
                                     itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            log.error("cannot create authorization open data type", e);
            return null;
        }
    }

    public CompositeData asCompositeData() throws OpenDataException {
        Object[] itemValues = new Object[2];
        String[] itemNames = UserManagerMBean.AUTHORIZATION;
        itemValues[0] = name;
        itemValues[1] = roles;
        return new CompositeDataSupport(AUTHORIZATION, itemNames, itemValues);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the roles
     */
    public String[] getRoles() {
        return roles;
    }

    public final static CompositeType AUTHORIZATION = createAuthorizationType();
}
