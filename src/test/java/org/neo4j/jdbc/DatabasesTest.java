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

import java.io.File;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.io.fs.FileUtils;
import org.neo4j.jdbc.embedded.EmbeddedDatabases;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.test.TestGraphDatabaseFactory;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 15.06.12
 */
public class DatabasesTest
{

    private EmbeddedDatabases databases;

    @Before
    public void setUp() throws Exception
    {
        databases = new EmbeddedDatabases();
    }

    @Test
    public void testLocateMem() throws Exception
    {
        GraphDatabaseService db = databases.createDatabase( ":mem", null );
        assertTrue( db instanceof GraphDatabaseFacade );
        assertTrue( ( (GraphDatabaseFacade) db ).getStoreDir().contains("impermanent"));
    }

    @Test
    public void testLocateNamedMem() throws Exception
    {
        final GraphDatabaseService db = databases.createDatabase( ":mem:a", null );
        assertTrue( db instanceof GraphDatabaseFacade );
        final GraphDatabaseService db2 = databases.createDatabase( ":mem:a", null );
        assertSame( db2, db );
    }

    @Test
    public void testLocateNamedInstance() throws Exception
    {
        final GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        final Properties props = new Properties();
        props.put( "a", db );
        final GraphDatabaseService db2 = databases.createDatabase( ":instance:a", props );
        assertSame( db2, db );
    }

    @Test
    public void testLocateFileDb() throws Exception
    {
        FileUtils.deleteRecursively( new File( "target/test-db" ) );
        final GraphDatabaseService db = databases.createDatabase( ":file:target/test-db", null );
        assertTrue( db instanceof GraphDatabaseFacade );
        assertTrue( ((GraphDatabaseFacade) db).getStoreDir().contains("test-db") );
        final GraphDatabaseService db2 = databases.createDatabase( ":file:target/test-db", null );
        assertSame( db2, db );
    }
}
