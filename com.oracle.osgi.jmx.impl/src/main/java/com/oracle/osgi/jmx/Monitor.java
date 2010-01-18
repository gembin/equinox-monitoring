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
package com.oracle.osgi.jmx;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

/**
 * @author Hal Hildebrand Date: Nov 23, 2008 Time: 6:26:44 PM
 * 
 */

abstract public class Monitor extends NotificationBroadcasterSupport implements
        MBeanRegistration {

    public void postDeregister() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean registrationDone) {
        addListener();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        removeListener();
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name)
                                                                      throws Exception {
        objectName = name;
        this.server = server;
        return name;
    }

    abstract protected void addListener();

    abstract protected void removeListener();

    protected ObjectName objectName;

    protected volatile long sequenceNumber = 0;

    protected MBeanServer server;
}