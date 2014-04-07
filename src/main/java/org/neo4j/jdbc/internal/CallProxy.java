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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import org.neo4j.jdbc.Driver;

/**
 * This implements driver debugging functionality. Method calls and results are logged to JUL.
 */
public class CallProxy
        implements InvocationHandler
{
    public static <T> T proxy( Class<T> clazz, T next )
    {
        return clazz.cast( Proxy.newProxyInstance( clazz.getClassLoader(), new Class[]{clazz},
                new CallProxy( next ) ) );
    }

    private static void log( final String str )
    {
        Logger.getLogger( Driver.class.getName() ).info( str );
    }

    private Object next;

    public CallProxy( Object next )
    {
        this.next = next;
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        if ( !method.getDeclaringClass().equals( Object.class ) )
        {
            String call = method.getDeclaringClass().getSimpleName() + "." + method.getName() + "(";
            if ( args != null )
            {
                String comma = "";
                for ( Object arg : args )
                {
                    call += comma + (arg == null ? "null" : arg.toString());
                    comma = ", ";
                }
            }
            call += ")";

            log( call );
            try
            {
                final Object result = method.invoke( next, args );
                if ( !method.getReturnType().equals( Void.TYPE ) )
                {
                    log( "->" + result + "\n" );
                }
                return result;
            }
            catch ( InvocationTargetException e )
            {
                StringWriter str = new StringWriter();
                PrintWriter print = new PrintWriter( str, true );
                e.printStackTrace( print );
                throw e.getTargetException();
            }
        }
        else
        {
            return method.invoke( next, args );
        }
    }
}
