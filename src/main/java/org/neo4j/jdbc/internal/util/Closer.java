package org.neo4j.jdbc.internal.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Closeable;

/**
 * @author mh
 * @since 01.04.14
 */
public class Closer {

    protected final static Log log = LogFactory.getLog(Closer.class);

    public static void close(Object data) {
        if (data == null) return;
        if (data instanceof AutoCloseable || data instanceof Closeable) {
            try {
                ((AutoCloseable) data).close();
            } catch (Exception e) {
                log.warn("Couldn't close object "+data.getClass(),e);
            }
        } else if (data instanceof ClosableIterator) {
            ((ClosableIterator)data).close();
        } else if (data.getClass().getName().equals("org.neo4j.helpers.collection.ClosableIterator")) {
            try {
                data.getClass().getMethod("close").invoke(data);
            } catch (Exception e) {
                log.warn("Couldn't close object "+data.getClass(),e);
            }
        }
    }
}
