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

import static org.osgi.jmx.codec.Util.STRING_ARRAY_TYPE;
import static org.osgi.jmx.core.ServiceStateMBean.BUNDLE_IDENTIFIER;
import static org.osgi.jmx.core.ServiceStateMBean.BUNDLE_LOCATION;
import static org.osgi.jmx.core.ServiceStateMBean.OBJECT_CLASS;
import static org.osgi.jmx.core.ServiceStateMBean.SERVICE_ID;

import javax.management.openmbean.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.jmx.core.ServiceStateMBean;

/**
 * @author Hal Hildebrand Date: Nov 24, 2008 Time: 2:42:48 PM
 *         <p>
 *         This class represents the CODEC for the composite data representing a
 *         OSGi <link>ServiceEvent</link>
 *         <p>
 *         It serves as both the documentation of the type structure and as the
 *         codification of the mechanism to convert to/from the CompositeData.
 *         <p>
 *         The structure of the composite data is:
 *         <table border="1">
 *         <tr>
 *         <td>Identifier</td>
 *         <td>String</td>
 *         </tr>
 *         <tr>
 *         <td>BundleIdentifier</td>
 *         <td>long</td>
 *         </tr>
 *         <tr>
 *         <td>BundleLocation</td>
 *         <td>String</td>
 *         </tr>
 *         <tr>
 *         <td>ObjectClass</td>
 *         <td>Array of String</td>
 *         </tr>
 *         <tr>
 *         <td>EventType</td>
 *         <td>int</td>
 *         </tr>
 *         </table>
 */
public class OSGiServiceEvent {
    /**
     * Construct an OSGiServiceEvent from the CompositeData representing the
     * event
     * 
     * @param data
     *            = the CompositeData representation of the event
     */
    public OSGiServiceEvent(CompositeData data) {
        serviceId = (Long) data.get(SERVICE_ID);
        bundleId = (Long) data.get(BUNDLE_IDENTIFIER);
        location = (String) data.get(BUNDLE_LOCATION);
        interfaces = (String[]) data.get(OBJECT_CLASS);
        eventType = (Integer) data.get(ServiceStateMBean.EVENT_TYPE);
    }

    /**
     * Construct and OSGiServiceEvent
     * 
     * @param serviceId
     * @param bundleId
     * @param location
     * @param interfaces
     * @param eventType
     */
    public OSGiServiceEvent(long serviceId, long bundleId, String location,
                            String[] interfaces, int eventType) {
        this.serviceId = serviceId;
        this.bundleId = bundleId;
        this.location = location;
        this.interfaces = interfaces;
        this.eventType = eventType;
    }

    /*
     * Construct and OSGiServiceEvent from the original
     * <link>ServiceEvent</link>
     */
    public OSGiServiceEvent(ServiceEvent event) {
        this(
             (Long) event.getServiceReference().getProperty(
                                                            Constants.SERVICE_ID),
             event.getServiceReference().getBundle().getBundleId(),
             event.getServiceReference().getBundle().getLocation(),
             (String[]) event.getServiceReference().getProperty(
                                                                Constants.OBJECTCLASS),
             event.getType());
    }

    private static CompositeType createServiceEventType() {
        String description = "This eventType encapsulates OSGi service events";
        String[] itemNames = ServiceStateMBean.SERVICE_EVENT;
        OpenType[] itemTypes = new OpenType[5];
        String[] itemDescriptions = new String[5];
        itemTypes[0] = SimpleType.LONG;
        itemTypes[1] = SimpleType.LONG;
        itemTypes[2] = SimpleType.STRING;
        itemTypes[3] = STRING_ARRAY_TYPE;
        itemTypes[4] = SimpleType.INTEGER;

        itemDescriptions[0] = "The id of the bundle which registered the service";
        itemDescriptions[1] = "The id of the bundle which registered the service";
        itemDescriptions[2] = "The location of the bundle that registered the service";
        itemDescriptions[3] = "An string array containing the interfaces under which the service has been registered";
        itemDescriptions[4] = "The eventType of the event: {REGISTERED=1, MODIFIED=2 UNREGISTERING=3}";
        try {
            return new CompositeType("ServiceEvent", description, itemNames,
                                     itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            log.error("Unable to create ServiceEvent OpenData eventType", e);
            return null;
        }

    }

    /**
     * Answer the receiver encoded as CompositeData
     * 
     * @return the CompositeData encoding of the receiver.
     */
    public CompositeData asCompositeData() {
        String[] itemNames = ServiceStateMBean.SERVICE_EVENT;
        Object[] itemValues = new Object[5];
        itemValues[0] = serviceId;
        itemValues[1] = bundleId;
        itemValues[2] = location;
        itemValues[3] = interfaces;
        itemValues[4] = eventType;

        try {
            return new CompositeDataSupport(SERVICE_EVENT, itemNames,
                                            itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException(
                                            "Cannot form service event open data",
                                            e);
        }
    }

    /**
     * @return the identifier of the bundle the service belongs to
     */
    public long getBundleId() {
        return bundleId;
    }

    /**
     * @return the type of the event
     */
    public int getEventType() {
        return eventType;
    }

    /**
     * @return the interfaces the service implements
     */
    public String[] getInterfaces() {
        return interfaces;
    }

    /**
     * @return the location of the bundle the service belongs to
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the identifier of the service
     */
    public long getServiceId() {
        return serviceId;
    }

    /**
     * The CompositeType representation of the OSGiServiceEvent
     */
    public final static CompositeType SERVICE_EVENT = createServiceEventType();

    private static final Log log = LogFactory.getLog(OSGiServiceEvent.class);

    private long bundleId;

    private int eventType;

    private String[] interfaces;

    private String location;

    private long serviceId;

}
