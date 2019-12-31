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

import javax.servlet.http.HttpServletRequest;

public class HttpSessionThreadLocal {

    public static final ThreadLocal<HttpSessionImpl> sessionThreadLocal = new ThreadLocal<HttpSessionImpl>();

    private static ServletBridgeHttpSessionStore sessionStore;

    public static ServletBridgeHttpSessionStore getSessionStore() {
        return sessionStore;
    }

    public static void setSessionStore(ServletBridgeHttpSessionStore store) {
        sessionStore = store;
    }

    public static void set(HttpSessionImpl session) {
        sessionThreadLocal.set(session);
    }

    public static void unset() {
        sessionThreadLocal.remove();
    }

    public static HttpSessionImpl get() {
        HttpSessionImpl session = sessionThreadLocal.get();
        if (session != null) {
            session.touch();
        }
        return session;
    }

    public static HttpSessionImpl getOrCreate(HttpServletRequest request) {
        if (HttpSessionThreadLocal.get() == null) {
            if (sessionStore == null) {
                sessionStore = new DefaultServletBridgeHttpSessionStore();
            }

            HttpSessionImpl newSession = sessionStore.createSession(request);
            newSession.setMaxInactiveInterval(ServletBridgeWebapp.get().getWebappConfig().getSessionTimeout());
            sessionThreadLocal.set(newSession);
        }
        return get();
    }

}
