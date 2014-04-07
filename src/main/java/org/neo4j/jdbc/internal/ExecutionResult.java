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

import java.io.Closeable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.neo4j.jdbc.internal.util.Closer;

/**
 * Cypher execution result.
 */
public class ExecutionResult implements Iterable<Object[]>, Closeable
{
    public static final ExecutionResult EMPTY_RESULT = new ExecutionResult( Collections.<String>emptyList(),
            Collections.<Object[]>emptyList().iterator() );
    private List<String> columns;
    private Iterator<Object[]> result;

    public ExecutionResult( List<String> columns, Iterator<Object[]> result )
    {
        this.columns = columns;
        this.result = result;
    }

    public List<String> columns()
    {
        return columns;
    }

    @Override
    public Iterator<Object[]> iterator()
    {
        return result;
    }

    @Override
    public String toString()
    {
        String result = "Columns:" + columns;
        result += "\n" + this.result;
        return result;
    }

    public Iterator<Object[]> getResult()
    {
        return result;
    }

    @Override
    public void close() {
        Closer.close(result);
    }
}
