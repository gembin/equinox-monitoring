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

import org.osgi.jmx.compendium.UserManagerMBean;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * @author Hal Hildebrand Date: Dec 5, 2008 Time: 8:02:42 AM
 * 
 */
public class OSGiGroup {
    protected OSGiUser user;
    protected String[] members;
    protected String[] requiredMembers;

    public OSGiGroup(Group group) {
        user = new OSGiUser(group);
        Role[] m = group.getMembers();
        if (m != null) {
            members = new String[m.length];
            int i = 0;
            for (Role role : m) {
                members[i++] = role.getName();
            }
        } else {
            members = new String[0];
        }
        Role[] rm = group.getRequiredMembers();
        if (rm != null) {
            requiredMembers = new String[rm.length];
            int i = 0;
            for (Role role : rm) {
                requiredMembers[i++] = role.getName();
            }
        } else {
            requiredMembers = new String[0];
        }
    }

    public OSGiGroup(CompositeData data) {
        user = new OSGiUser(
                            (CompositeData) data.get(UserManagerMBean.ENCODED_USER));
        members = (String[]) data.get(UserManagerMBean.GROUP_MEMBERS);
        requiredMembers = (String[]) data.get(UserManagerMBean.GROUP_REQUIRED_MEMBERS);
    }

    public CompositeData asCompositeData() throws OpenDataException {
        String[] itemNames = UserManagerMBean.GROUP;
        Object[] itemValues = new Object[3];
        itemValues[0] = user.asCompositeData();
        itemValues[1] = members;
        itemValues[2] = requiredMembers;
        return new CompositeDataSupport(GROUP, itemNames, itemValues);
    }

    private static CompositeType createGroupType() {
        String description = "Mapping of org.osgi.service.useradmin.Group for remote management purposes. Group extends User which in turn extends Role";
        String[] itemNames = UserManagerMBean.GROUP;
        /*
         * itemNames[0] = "User"; itemNames[1] = "members"; itemNames[2] =
         * "requiredMembers";
         */
        String[] itemDescriptions = new String[3];
        itemDescriptions[0] = "The user object that is extended by this group object";
        itemDescriptions[1] = "The members of this group";
        itemDescriptions[2] = "The required members for this group";
        OpenType[] itemTypes = new OpenType[3];
        itemTypes[0] = OSGiUser.USER;
        itemTypes[1] = Util.STRING_ARRAY_TYPE;
        itemTypes[2] = Util.STRING_ARRAY_TYPE;
        try {
            return new CompositeType("Group", description, itemNames,
                                     itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return the user
     */
    public OSGiUser getUser() {
        return user;
    }

    /**
     * @return the members
     */
    public String[] getMembers() {
        return members;
    }

    /**
     * @return the requiredMembers
     */
    public String[] getRequiredMembers() {
        return requiredMembers;
    }

    public final CompositeType GROUP = createGroupType();
}
