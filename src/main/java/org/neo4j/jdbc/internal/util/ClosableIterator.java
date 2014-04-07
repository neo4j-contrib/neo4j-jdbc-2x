package org.neo4j.jdbc.internal.util;

import java.io.Closeable;
import java.util.Iterator;

/**
 * @author mh
 * @since 01.04.14
 */
public interface ClosableIterator<T> extends Iterator<T>, Closeable {
    void close();
}
