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

import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

public class ServletInputStreamImpl extends ServletInputStream {

    private HttpRequest request;

    private ByteBufInputStream in;

    public ServletInputStreamImpl(FullHttpRequest request) {
        this.request = request;

        this.in = new ByteBufInputStream(request.content());
    }

    public ServletInputStreamImpl(HttpRequest request) {
        this.request = request;

        this.in = new ByteBufInputStream(Unpooled.buffer(0));
    }

    @Override
    public int read() throws IOException {
        return this.in.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return this.in.read(buf);
    }

    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        return this.in.read(buf, offset, len);
    }

    @Override
    public boolean isFinished() {
        try {
            return this.in.available() == 0;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        // TODO Auto-generated method stub

    }

}
