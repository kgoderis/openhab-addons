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

import java.net.InetSocketAddress;
import java.util.Collection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

public class HttpSessionInterceptor implements ServletBridgeInterceptor {

    private boolean sessionRequestedByCookie = false;
    private boolean sessionRequestedByAddress = false;

    public HttpSessionInterceptor(ServletBridgeHttpSessionStore sessionStore) {
        HttpSessionThreadLocal.setSessionStore(sessionStore);
    }

    @Override
    public void onRequestReceived(ChannelHandlerContext ctx, HttpRequest request) {

        HttpSessionThreadLocal.unset();

        Collection<Cookie> cookies = Utils.getCookies(HttpSessionImpl.SESSION_ID_KEY, request);
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String jsessionId = cookie.value();
                HttpSessionImpl s = HttpSessionThreadLocal.getSessionStore().findSession(jsessionId);
                if (s != null) {
                    HttpSessionThreadLocal.set(s);
                    sessionRequestedByCookie = true;
                    break;
                }
            }
        }

        if (!sessionRequestedByCookie) {
            HttpSessionImpl s = HttpSessionThreadLocal.getSessionStore().findSession(
                    ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString(),
                    ((InetSocketAddress) ctx.channel().remoteAddress()).getPort());
            if (s != null) {
                HttpSessionThreadLocal.set(s);
                sessionRequestedByAddress = true;
            }
        }

    }

    @Override
    public void onRequestSuccessed(ChannelHandlerContext ctx, HttpRequest request, HttpResponse response) {

        HttpSessionImpl s = HttpSessionThreadLocal.get();
        if (s != null && !this.sessionRequestedByCookie) {
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                    ServerCookieEncoder.STRICT.encode(HttpSessionImpl.SESSION_ID_KEY, s.getId()));
        }

    }

    @Override
    public void onRequestFailed(ChannelHandlerContext ctx, Throwable e, HttpResponse response) {
        this.sessionRequestedByCookie = false;
        HttpSessionThreadLocal.unset();
    }

}
