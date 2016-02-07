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

/**
 * This class contains all the Cypher queries that the driver needs to issue.
 */
public class DriverQueries
{
    public String getTables()
    {
        return "MATCH (r:MetaDataRoot)-[:TYPE]->(type) RETURN type.type";
    }

    public String getColumns()
    {
        return "MATCH (r:MetaDataRoot)-[:TYPE]->(type)-[:HAS_PROPERTY]->(property) RETURN type.type, property.name, " +
                "property.type";
    }

    public String getColumns( String typeName )
    {
        return "MATCH (r:MetaDataRoot)-[:TYPE]->(type {type:{typeName}})-[:HAS_PROPERTY]->(property) RETURN type.type, property.name, property.type";
    }

    public String getData( String typeName, Iterable<String> returnProperties )
    {

        StringBuilder builder = new StringBuilder();
        for ( String property : returnProperties )
        {
            if (builder.length() > 0 ) builder.append( ", " );
            builder.append( property );
        }
        return "MATCH (r:MetaDataRoot)-[:TYPE]->(type {type:{typeName}})<-[:IS_A]->(instance) RETURN " + builder;
    }
}
