package org.neo4j.jdbc.helpers;

import java.util.Arrays;

import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServer;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.server.modules.DiscoveryModule;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ServerModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.web.WebServer;

/**
 * @author Michael Hunger @since 25.10.13
 */
public class TestServer
{
    public static final int PORT = 7475;

    public static WebServer startWebServer( GraphDatabaseAPI gdb, int port, boolean auth )
    {
        final ServerConfigurator config = new ServerConfigurator( gdb );
        config.configuration().setProperty( Configurator.WEBSERVER_PORT_PROPERTY_KEY, port );
        final WrappingNeoServer wrappingNeoServer = new WrappingNeoServer( gdb, config )
        {
            @Override
            protected Iterable<ServerModule> createServerModules()
            {
                return Arrays.asList(
                        new DiscoveryModule( webServer ),
                        new RESTApiModule( webServer, database, configurator.configuration() ),
                        new ThirdPartyJAXRSModule( webServer, configurator, this ) );
            }
        };
        final WebServer webServer = wrappingNeoServer.getWebServer();
        if ( auth )
        {
            webServer.addFilter( new TestAuthenticationFilter(), "/*" );
        }
        wrappingNeoServer.start();
        return webServer;
    }
}
