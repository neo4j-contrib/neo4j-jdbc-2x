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
package org.neo4j.jdbc.ext;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.jdbc.Driver;
import org.neo4j.jdbc.Neo4jConnection;

/**
 * IntelliJ specific Neo4j connection. Contains workarounds to get it to work with IntelliJ.
 */
public class IntelliJConnection
        extends Neo4jConnection
        implements Connection
{
    public IntelliJConnection( Driver driver, String url, Properties properties ) throws SQLException
    {
        super( driver, url, properties );
    }

    @Override
    public ResultSet executeQuery( String query, Map<String, Object> parameters ) throws SQLException
    {
        {
            Pattern pattern = Pattern.compile( "select \"Default\".\"Default\".\"(\\w*)\".\\* from \"Default\"" +
                    ".\"Default\".\"(\\w*)\"" );
            Matcher matcher = pattern.matcher( query );
            if ( matcher.matches() )
            {
                String table = matcher.group( 1 );
                HashMap<String, Object> map = new HashMap<>( parameters );
                map.put("typeName",table);
                String ewp = getDriver().getQueries().getData( table, returnProperties( table,
                        "instance" ) );
                return executeQuery( ewp , map);
            }
        }

        return super.executeQuery( query, parameters );
    }
}
