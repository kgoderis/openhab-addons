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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class ServletContextImpl extends ConfigAdapter implements ServletContext {

    private static final Logger log = LoggerFactory.getLogger(ServletContextImpl.class);

    private static ServletContextImpl instance;

    private Map<String, Object> attributes;

    private String servletContextName;

    public static ServletContextImpl get() {
        if (instance == null) {
            instance = new ServletContextImpl();
        }

        return instance;
    }

    private ServletContextImpl() {
        super("Netty Servlet Bridge");
    }

    @Override
    public Object getAttribute(String name) {
        return attributes != null ? attributes.get(name) : null;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Utils.enumerationFromKeys(attributes);
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public int getMajorVersion() {
        return 2;
    }

    @Override
    public int getMinorVersion() {
        return 4;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        return ServletContextImpl.class.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        return ServletContextImpl.class.getResourceAsStream(path);
    }

    @Override
    public String getServerInfo() {
        return super.getOwnerName();
    }

    @Override
    public void log(String msg) {
        log.info(msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        log.error(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public void removeAttribute(String name) {
        if (this.attributes != null) {
            this.attributes.remove(name);
        }
    }

    @Override
    public void setAttribute(String name, Object object) {
        if (this.attributes == null) {
            this.attributes = new HashMap<String, Object>();
        }

        this.attributes.put(name, object);
    }

    @Override
    public String getServletContextName() {
        return this.servletContextName;
    }

    void setServletContextName(String servletContextName) {
        this.servletContextName = servletContextName;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        throw new IllegalStateException("Deprecated as of Java Servlet API 2.1, with no direct replacement!");
    }

    @Override
    public Enumeration<String> getServletNames() {
        throw new IllegalStateException(
                "Method 'getServletNames' deprecated as of Java Servlet API 2.0, with no replacement.");
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        throw new IllegalStateException(
                "Method 'getServlets' deprecated as of Java Servlet API 2.0, with no replacement.");
    }

    @Override
    public ServletContext getContext(String uripath) {
        return this;
    }

    @Override
    public String getMimeType(String file) {
        return Utils.getMimeType(file);

    }

    @Override
    public Set<String> getResourcePaths(String path) {
        throw new IllegalStateException("Method 'getResourcePaths' not yet implemented!");
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        Collection<ServletConfiguration> colls = ServletBridgeWebapp.get().getWebappConfig().getServletConfigurations();
        HttpServlet servlet = null;
        for (ServletConfiguration configuration : colls) {
            if (configuration.getConfig().getServletName().equals(name)) {
                servlet = configuration.getHttpComponent();
            }
        }

        return new RequestDispatcherImpl(name, null, servlet);
    }

    @Override
    public String getRealPath(String path) {
        if ("/".equals(path)) {
            try {
                File file = File.createTempFile("netty-servlet-bridge", "");
                file.mkdirs();
                return file.getAbsolutePath();
            } catch (IOException e) {
                throw new IllegalStateException("Method 'getRealPath' not yet implemented!");
            }
        } else {
            throw new IllegalStateException("Method 'getRealPath' not yet implemented!");
        }
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        Collection<ServletConfiguration> colls = ServletBridgeWebapp.get().getWebappConfig().getServletConfigurations();
        HttpServlet servlet = null;
        String servletName = null;
        for (ServletConfiguration configuration : colls) {
            if (configuration.matchesUrlPattern(path)) {
                servlet = configuration.getHttpComponent();
                servletName = configuration.getHttpComponent().getServletName();
            }
        }

        return new RequestDispatcherImpl(servletName, path, servlet);
    }

    @Override
    public int getEffectiveMajorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Dynamic addServlet(String servletName, String className) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, String className) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
        // TODO Auto-generated method stub

    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(String className) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        // TODO Auto-generated method stub

    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        // TODO Auto-generated method stub

    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void declareRoles(String... roleNames) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getVirtualServerName() {
        // TODO Auto-generated method stub
        return null;
    }

}
