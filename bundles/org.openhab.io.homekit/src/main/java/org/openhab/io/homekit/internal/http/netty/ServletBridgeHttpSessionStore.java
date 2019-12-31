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

import javax.servlet.ServletRequest;

public interface ServletBridgeHttpSessionStore {

    HttpSessionImpl findSession(String sessionId);

    void destroySession(String sessionId);

    void destroyInactiveSessions();

    HttpSessionImpl findSession(ServletRequest request);

    HttpSessionImpl createSession(ServletRequest request);

    HttpSessionImpl findSession(String hostString, int port);

}
