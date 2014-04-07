/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.jdbc.integration;


import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ServiceLoader;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.jdbc.Driver;

import static org.junit.Assert.*;

public class DriverIT extends Neo4jJdbcIntegrationTest
{
    Driver driver;

    public DriverIT( Mode mode ) throws SQLException
    {
        super( mode );
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        driver = new Driver();
    }

    @Test
    public void testAcceptsURL() throws SQLException
    {
        assertTrue( driver.acceptsURL( jdbcUrl() ) );
        assertTrue( !driver.acceptsURL( "jdbc:derby://localhost:7474/" ) );
    }

    @Test
    public void testConnect() throws SQLException
    {
        final Properties properties = new Properties();
        properties.put( "db", gdb );
        assertNotNull( driver.connect( "jdbc:neo4j:instance:db", properties ) );
        assertNull( driver.connect( "jdbc:derby://localhost:7474/", properties ) );
    }

    @Test
    public void testDriverRegistration()
    {
        try
        {
            java.sql.Driver driver = DriverManager.getDriver( jdbcUrl() );
            assertNotNull( driver );
            assertEquals( this.driver.getClass(), driver.getClass() );
        }
        catch ( SQLException e )
        {
            fail( e.getLocalizedMessage() );
        }

    }

    @Test
    public void testDriverService()
    {
        ServiceLoader<java.sql.Driver> serviceLoader = ServiceLoader.load( java.sql.Driver.class );
        for ( java.sql.Driver driver : serviceLoader )
        {
            if ( Driver.class.isInstance( driver ) )
            {
                return;
            }
        }
        fail( Driver.class.getName() + " not registered as a Service" );
    }
}
