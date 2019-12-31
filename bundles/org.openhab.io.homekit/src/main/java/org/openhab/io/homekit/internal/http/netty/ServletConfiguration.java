/*
 * Copyright 2013 by Maxim Kalina
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.openhab.io.homekit.internal.http.netty;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.openhab.io.homekit.api.AccessoryServer;
import org.openhab.io.homekit.internal.http.HomekitServletConfig;

public class ServletConfiguration extends HttpComponentConfigurationAdapter<HttpServlet, HomekitServletConfig> {

    public ServletConfiguration(AccessoryServer server, Class<? extends HttpServlet> servletClazz,
            String... urlPatterns) {
        super(server, servletClazz, urlPatterns);
    }

    public ServletConfiguration(AccessoryServer server, Class<? extends HttpServlet> componentClazz) {
        super(server, componentClazz);
    }

    public ServletConfiguration(AccessoryServer server, HttpServlet component, String... urlPatterns) {
        super(server, component, urlPatterns);
    }

    public ServletConfiguration(AccessoryServer server, HttpServlet servlet) {
        super(server, servlet);
    }

    @Override
    protected void doInit() throws ServletException {
        this.component.init(this.config);
    }

    @Override
    protected void doDestroy() throws ServletException {
        this.component.destroy();
    }

    @Override
    protected HomekitServletConfig newConfigInstance(Class<? extends HttpServlet> componentClazz) {
        return new HomekitServletConfig(server, this.component.getClass().getName());
    }

    public ServletConfiguration addInitParameter(String name, String value) {
        super.addConfigInitParameter(name, value);
        return this;
    }
}
