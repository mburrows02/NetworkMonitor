/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.httpcopy.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @since 4.3
 */
public final class NetUtils {

    public static void formatAddress(
            final StringBuilder buffer,
            final SocketAddress socketAddress) {
        Args.notNull(buffer, "Buffer");
        Args.notNull(socketAddress, "Socket address");
        if (socketAddress instanceof InetSocketAddress) {
            final InetSocketAddress socketaddr = ((InetSocketAddress) socketAddress);
            final InetAddress inetaddr = socketaddr.getAddress();
            buffer.append(inetaddr != null ? inetaddr.getHostAddress() : inetaddr)
                .append(':').append(socketaddr.getPort());
        } else {
            buffer.append(socketAddress);
        }
    }

}