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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import javax.management.openmbean.*;

import org.osgi.jmx.compendium.UserManagerMBean;
import org.osgi.service.useradmin.User;

/**
 * @author Hal Hildebrand Date: Dec 5, 2008 Time: 8:02:35 AM
 * 
 */
public class OSGiUser {
    protected OSGiRole role;
    protected Hashtable<String, Object> credentials;

    @SuppressWarnings("unchecked")
    public OSGiUser(User user) {
        role = new OSGiRole(user);
        credentials = new Hashtable<String, Object>();
        Dictionary<String, Object> c = user.getCredentials();
        for (Enumeration keys = c.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            credentials.put(key, c.get(key));
        }
    }

    public OSGiUser(CompositeData data) {
        role = new OSGiRole(
                            (CompositeData) data.get(UserManagerMBean.ENCODED_ROLE));
        credentials = OSGiProperties.propertiesFrom((TabularData) data.get(UserManagerMBean.ENCODED_CREDENTIALS));
    }

    public CompositeData asCompositeData() throws OpenDataException {
        String[] itemNames = UserManagerMBean.USER;
        Object[] itemValues = new Object[2];
        itemValues[0] = role.asCompositeData();
        itemValues[1] = OSGiProperties.tableFrom(credentials);
        return new CompositeDataSupport(USER, itemNames, itemValues);
    }

    private static CompositeType createUserType() {
        String description = "Mapping of org.osgi.service.useradmin.User for remote management purposes. User extends Role";
        String[] itemNames = UserManagerMBean.USER;
        String[] itemDescriptions = new String[2];
        itemDescriptions[0] = "The role object that is extended by this user object";
        itemDescriptions[1] = "The credentials for this user";
        OpenType[] itemTypes = new OpenType[2];
        itemTypes[0] = OSGiRole.ROLE;
        itemTypes[1] = OSGiProperties.PROPERTY_TABLE;
        try {

            return new CompositeType("User", description, itemNames,
                                     itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return the role
     */
    public OSGiRole getRole() {
        return role;
    }

    /**
     * @return the credentials
     */
    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public static final CompositeType USER = createUserType();

}
