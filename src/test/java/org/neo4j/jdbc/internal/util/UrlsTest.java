package org.neo4j.jdbc.internal.util;

import java.sql.SQLException;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class UrlsTest
{
    @Test
    public void testURLProperties() throws SQLException
    {
        final Properties properties = new Properties();
        Uris.parseUrlProperties( "jdbc:neo4j://asd?debug=false", properties );
        Assert.assertThat( properties.getProperty( "debug" ), CoreMatchers.equalTo( "false" ) );
    }
}
