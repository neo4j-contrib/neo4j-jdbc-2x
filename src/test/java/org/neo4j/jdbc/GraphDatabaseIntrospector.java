package org.neo4j.jdbc;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

class GraphDatabaseIntrospector {

    public static boolean isEmbedded(GraphDatabaseService db) {
        return getEmbeddedClass().isAssignableFrom(db.getClass());
    }

    private static Class<?> getEmbeddedClass() {
        try {
            return Class.forName("org.neo4j.kernel.impl.factory.GraphDatabaseFacade");
        } catch (ClassNotFoundException e) {
            return EmbeddedGraphDatabase.class;
        }
    }
}
