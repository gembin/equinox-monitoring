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
package com.oracle.osgi.jmx.compendium;

import java.io.IOException;
import java.util.ArrayList;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.jmx.codec.*;
import org.osgi.jmx.compendium.UserManagerMBean;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

import com.oracle.osgi.jmx.Monitor;

/**
 * @author Hal Hildebrand Date: Dec 2, 2008 Time: 2:43:32 PM
 * 
 */
public class UserManager extends Monitor implements UserManagerMBean {

    protected UserAdmin admin;

    public UserManager(UserAdmin admin) {
        this.admin = admin;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#addCredential(java.lang.String,
     * byte[], java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void addCredential(String key, byte[] value, String username)
                                                                        throws IOException {
        User user;
        try {
            user = (User) admin.getRole(username);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a User: " + username);
        }
        user.getCredentials().put(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#addCredential(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void addCredential(String key, String value, String username)
                                                                        throws IOException {
        User user;
        try {
            user = (User) admin.getRole(username);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a User: " + username);
        }
        user.getCredentials().put(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#addMember(java.lang.String,
     * java.lang.String)
     */
    public boolean addMember(String groupname, String rolename)
                                                               throws IOException {
        Role group = admin.getRole(groupname);
        Role role = admin.getRole(rolename);
        return group.getType() == Role.GROUP && ((Group) group).addMember(role);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#addProperty(java.lang.String,
     * byte[], java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void addProperty(String key, byte[] value, String rolename)
                                                                      throws IOException {
        admin.getRole(rolename).getProperties().put(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#addProperty(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void addProperty(String key, String value, String rolename)
                                                                      throws IOException {
        admin.getRole(rolename).getProperties().put(key, value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#addRequiredMember(java.lang.
     * String, java.lang.String)
     */
    public boolean addRequiredMember(String groupname, String rolename)
                                                                       throws IOException {
        Role group = admin.getRole(groupname);
        Role role = admin.getRole(rolename);
        return group.getType() == Role.GROUP
               && ((Group) group).addRequiredMember(role);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#createGroup(java.lang.String)
     */
    public void createGroup(String name) throws IOException {
        admin.createRole(name, Role.GROUP);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#createUser(java.lang.String)
     */
    public void createUser(String name) throws IOException {
        admin.createRole(name, Role.USER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getAuthorization(java.lang.String
     * )
     */
    public CompositeData getAuthorization(String u) throws IOException {
        User user;
        try {
            user = (User) admin.getRole(u);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a user: " + u);
        }
        try {
            return new OSGiAuthorization(admin.getAuthorization(user)).asCompositeData();
        } catch (OpenDataException e) {
            throw new IOException("Unable to create open data type: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getCredentials(java.lang.String)
     */
    public TabularData getCredentials(String username) throws IOException {
        User user;
        try {
            user = (User) admin.getRole(username);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a user: " + username);
        }
        return OSGiProperties.tableFrom(user.getCredentials());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getGroup(java.lang.String)
     */
    public CompositeData getGroup(String groupname) throws IOException {
        Group group;
        try {
            group = (Group) admin.getRole(groupname);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a group: " + groupname);
        }
        try {
            return new OSGiGroup(group).asCompositeData();
        } catch (OpenDataException e) {
            throw new IOException("Cannot encode open data for group: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getGroups()
     */
    public String[] getGroups() throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(
                                            "Cannot use null filter, apparently: "
                                                    + e);
        }
        ArrayList<String> groups = new ArrayList<String>();
        for (Role role : roles) {
            if (role.getType() == Role.GROUP) {
                groups.add(role.getName());
            }
        }
        return groups.toArray(new String[groups.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getGroups(java.lang.String)
     */
    public String[] getGroups(String filter) throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(
                                            "Cannot use null filter, apparently: "
                                                    + e);
        }
        ArrayList<String> groups = new ArrayList<String>();
        for (Role role : roles) {
            if (role.getType() == Role.GROUP) {
                groups.add(role.getName());
            }
        }
        return groups.toArray(new String[groups.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getImpliedRoles(java.lang.String
     * )
     */
    public String[] getImpliedRoles(String username) throws IOException {
        Role role = admin.getRole(username);
        if (role.getType() == Role.USER && role instanceof User) {
            return admin.getAuthorization((User) role).getRoles();
        } else {
            return new String[0];
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getMembers(java.lang.String)
     */
    public String[] getMembers(String groupname) throws IOException {
        Group group;
        try {
            group = (Group) admin.getRole(groupname);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a group: " + groupname);
        }
        Role[] members = group.getMembers();
        if (members == null) {
            return new String[0];
        }
        String[] names = new String[members.length];
        for (int i = 0; i < members.length; i++) {
            names[i] = members[i].getName();
        }
        return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getProperties(java.lang.String)
     */
    public TabularData getProperties(String rolename) throws IOException {
        Role role = admin.getRole(rolename);
        if (role == null) {
            return null;
        }
        return OSGiProperties.tableFrom(role.getProperties());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#getRequiredMembers(java.lang
     * .String)
     */
    public String[] getRequiredMembers(String groupname) throws IOException {
        Group group;
        try {
            group = (Group) admin.getRole(groupname);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a group: " + groupname);
        }
        Role[] members = group.getRequiredMembers();
        if (members == null) {
            return new String[0];
        }
        String[] names = new String[members.length];
        for (int i = 0; i < members.length; i++) {
            names[i] = members[i].getName();
        }
        return names;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getRole(java.lang.String)
     */
    public CompositeData getRole(String name) throws IOException {
        Role role = admin.getRole(name);
        try {
            return role == null ? null : new OSGiRole(role).asCompositeData();
        } catch (OpenDataException e) {
            throw new IOException("Unable to create open data: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getRoles()
     */
    public String[] getRoles() throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(
                                            "Cannot use null filter, apparently: "
                                                    + e);
        }
        String[] result = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            result[i] = roles[i].getName();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getRoles(java.lang.String)
     */
    public String[] getRoles(String filter) throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Invalid filter: " + e);
        }
        String[] result = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            result[i] = roles[i].getName();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getUser(java.lang.String)
     */
    public CompositeData getUser(String username) throws IOException {
        User user;
        try {
            user = (User) admin.getRole(username);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a user: " + username);
        }
        try {
            return user == null ? null : new OSGiUser(user).asCompositeData();
        } catch (OpenDataException e) {
            throw new IOException("Unable to create open data: " + e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getUser(java.lang.String,
     * java.lang.String)
     */
    public String getUser(String key, String value) throws IOException {
        User user = admin.getUser(key, value);
        return user == null ? null : user.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getUsers()
     */
    public String[] getUsers() throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(null);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(
                                            "Cannot use null filter, apparently: "
                                                    + e);
        }
        ArrayList<String> groups = new ArrayList<String>();
        for (Role role : roles) {
            if (role.getType() == Role.USER) {
                groups.add(role.getName());
            }
        }
        return groups.toArray(new String[groups.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.compendium.UserManagerMBean#getUsers(java.lang.String)
     */
    public String[] getUsers(String filter) throws IOException {
        Role[] roles;
        try {
            roles = admin.getRoles(filter);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(
                                            "Cannot use null filter, apparently: "
                                                    + e);
        }
        ArrayList<String> groups = new ArrayList<String>();
        for (Role role : roles) {
            if (role.getType() == Role.USER) {
                groups.add(role.getName());
            }
        }
        return groups.toArray(new String[groups.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#removeCredential(java.lang.String
     * , java.lang.String)
     */
    public void removeCredential(String key, String username)
                                                             throws IOException {
        User user;
        try {
            user = (User) admin.getRole(username);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a user: " + username);
        }
        if (user == null) {
            return;
        }
        user.getCredentials().remove(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#removeMember(java.lang.String,
     * java.lang.String)
     */
    public boolean removeMember(String groupname, String rolename)
                                                                  throws IOException {
        Group group;
        try {
            group = (Group) admin.getRole(groupname);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Not a group: " + groupname);
        }
        if (group == null) {
            return false;
        }
        Role role = admin.getRole(rolename);
        if (role == null) {
            return false;
        }
        return group.removeMember(role);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#removeProperty(java.lang.String,
     * java.lang.String)
     */
    public void removeProperty(String key, String rolename) throws IOException {
        Role role = admin.getRole(rolename);
        if (role == null) {
            return;
        }
        role.getProperties().remove(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.jmx.compendium.UserManagerMBean#removeRole(java.lang.String)
     */
    public boolean removeRole(String name) throws IOException {
        return admin.removeRole(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.core.Monitor#addListener()
     */
    @Override
    protected void addListener() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.jmx.core.Monitor#removeListener()
     */
    @Override
    protected void removeListener() {
        // TODO Auto-generated method stub

    }

}
