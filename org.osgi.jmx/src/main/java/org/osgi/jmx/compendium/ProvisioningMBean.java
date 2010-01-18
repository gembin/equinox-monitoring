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
package org.osgi.jmx.compendium;

import java.io.IOException;

import javax.management.openmbean.TabularData;

/**
 * @author Hal Hildebrand Date: Jan 21, 2008 Time: 10:49:26 AM
 * 
 *         This MBean represents the management interface to the OSGi Initial
 *         Provisioning Service
 */
public interface ProvisioningMBean {

    /**
     * Processes the <code>ZipInputStream</code> contents of the provided zipURL
     * and extracts information to add to the Provisioning Information
     * dictionary, as well as, install/update and start bundles. This method
     * causes the <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
     * 
     * @param zipURL
     *            the String form of the URL that will be resolved into a
     *            <code>ZipInputStream</code> which will be used to add
     *            key/value pairs to the Provisioning Information dictionary and
     *            install and start bundles. If a <code>ZipEntry</code> does not
     *            have an <code>Extra</code> field that corresponds to one of
     *            the four defined MIME types (<code>MIME_STRING</code>,
     *            <code>MIME_BYTE_ARRAY</code>,<code>MIME_BUNDLE</code>, and
     *            <code>MIME_BUNDLE_URL</code>) in will be silently ignored.
     * @throws IOException
     *             if an error occurs while processing the ZipInputStream of the
     *             URL. No additions will be made to the Provisioning
     *             Information dictionary and no bundles must be started or
     *             installed.
     */
    public void addInformation(String zipURL) throws IOException;

    /**
     * Adds the key/value pairs contained in <code>info</code> to the
     * Provisioning Information dictionary. This method causes the
     * <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
     * <p>
     * 
     * @see org.osgi.jmx.codec.OSGiProperties for the details of the TabularType
     *      <p>
     *      For each entry in the Provisioning Dictionary, the following row is
     *      supplied
     *      <ul>
     *      <li>Property Key - the string key</li>
     *      <li>Property Value - the stringified version of the property value</li>
     *      <li>Property Value Type - the type of the property value</li>
     *      </ul>
     * 
     * @param info
     *            the set of Provisioning Information key/value pairs to add to
     *            the Provisioning Information dictionary. Any keys are values
     *            that are of an invalid type will be silently ignored.
     * @throws IOException
     *             if the operation fails
     */
    public void addInformation(TabularData info) throws IOException;

    /**
     * Returns a table representing the Provisioning Information Dictionary.
     * <p>
     * 
     * @see org.osgi.jmx.codec.OSGiProperties for the details of the TabularType
     *      <p>
     *      For each entry in the Provisioning Information Dictionary, the
     *      following row is supplied
     *      <ul>
     *      <li>Property Key - the string key</li>
     *      <li>Property Value - the stringified version of the property value</li>
     *      <li>Property Value Type - the type of the property value</li>
     *      </ul>
     * 
     * @throws IOException
     *             if the operation fails
     * @return The table representing the manager dictionary.
     */
    public TabularData getInformation() throws IOException;

    /**
     * Replaces the Provisioning Information dictionary with the entries of the
     * supplied table. This method causes the
     * <code>PROVISIONING_UPDATE_COUNT</code> to be incremented.
     * <p>
     * 
     * @see org.osgi.jmx.codec.OSGiProperties for the details of the TabularType
     *      <p>
     *      For each entry in the table, the following row is supplied
     *      <ul>
     *      <li>Property Key - the string key</li>
     *      <li>Property Value - the stringified version of the property value</li>
     *      <li>Property Value Type - the type of the property value</li>
     *      </ul>
     * 
     * @param info
     *            the new set of Provisioning Information key/value pairs. Any
     *            keys are values that are of an invalid type will be silently
     *            ignored.
     * @throws IOException
     *             if the operation fails
     */
    public void setInformation(TabularData info) throws IOException;

}
