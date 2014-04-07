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

package org.neo4j.jdbc.internal;

import java.util.LinkedHashMap;
import java.util.Map;

import org.neo4j.jdbc.Version;

/**
 * @author mh
 * @since 15.06.12
 */
public interface QueryExecutor
{
    ExecutionResult executeQuery( String query, Map<String, Object> parameters, boolean autoCommit ) throws Exception;

    void stop() throws Exception;

    Version getVersion();

    void commit() throws Exception;

    void rollback() throws Exception;

    public class Metadata
    {
        String label;
        Map<String, Object> props;
        Map<String, Metadata> rels;  // key == -[:%s {%s}]-> or -[:%s]-> or <-[:%s {%s}]-

        public String toString()
        {
            return String.format( "(:%s {%s})", label, props );
        }

        public Map<String, Object> toMap()
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.putAll( props );
            for ( Map.Entry<String, Metadata> entry : rels.entrySet() )
            {
                result.put( entry.getKey(), entry.getValue().toString() );
            }
            return result;
        }
    }
}
