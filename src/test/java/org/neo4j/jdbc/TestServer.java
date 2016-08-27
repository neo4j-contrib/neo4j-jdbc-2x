/**
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.jdbc;

import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;
import org.neo4j.server.web.WebServer;

import java.io.IOException;

/**
 * @author Michael Hunger @since 25.10.13
 */
public class TestServer
{
    public static final int PORT = 7475;

    public static CommunityNeoServer startWebServer( int port, boolean auth )
    {
        try
        {
            CommunityServerBuilder builder = CommunityServerBuilder.server();
            CommunityNeoServer server = builder.onPort( port ).build();
            server.init();
            final WebServer webServer = server.getWebServer();
            if ( auth )
            {
                webServer.addFilter( new TestAuthenticationFilter(), "/*" );
            }
            server.start();
            return server;
        } catch(IOException ioe) {
            throw new RuntimeException( "Error starting server on port "+port+" with auth "+auth,ioe );
        }
    }
}
