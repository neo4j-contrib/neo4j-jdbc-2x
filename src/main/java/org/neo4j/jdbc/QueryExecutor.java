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

import java.util.LinkedHashMap;
import java.util.Map;

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
        /*
        for all labels
        TODO types
        TODO collect 10 per rel-type WITH

        MATCH (n:Ping) WHERE SIZE((n)--()) < 1000 WITH n,labels(n) as start_labels,keys(n) as start_props LIMIT 100
        OPTIONAL MATCH (n)-[r]-(m)
        WITH start_labels, collect(start_props)[0..10] as start_props, type(r) as rel_type,collect(keys(r))[0..10] as
        rel_props,
        case startNode(r) when n then "OUT" else "IN" end as rel_dir, labels(m) as end_labels,collect(keys(m))[0..10]
         as end_props, count(*) as freq
        WITH start_labels,
               reduce(a=[], p in reduce(a=[],coll in start_props | a + coll) | CASE WHEN p in a THEN a ELSE a + [p] END) as start_props,
             rel_type,
               reduce(a=[], p in reduce(a=[],coll in rel_props   | a + coll) | CASE WHEN p in a then a else a + [p] end) as rel_props, rel_dir,
             end_labels,
               reduce(a=[], p in reduce(a=[],coll in end_props   | a + coll) | CASE WHEN p in a then a else a + [p] end) as end_props, freq
        RETURN start_labels, start_props,
               collect({rel_type: rel_type, rel_props : rel_props, rel_dir : rel_dir,
                        end_labels: end_labels, end_props: end_props, freq:freq}) as rel_data;


        table names: join(labels,"_")
        labels(n)_rel_type_labels(m)

        ?? treat other labels as views?
        ?? treat rels as views?

        */
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
