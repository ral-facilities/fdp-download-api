package org.icatproject.topcat;

import java.util.*;
import java.lang.reflect.*;

import static org.junit.Assert.*;
import org.junit.*;

public class DatastoreClientTest {	
	@Test
	public void testParseTimeout() throws Exception {
		DatastoreClient datastoreClient = new DatastoreClient("https://localhost:8181");
		Method parseTimeout = datastoreClient.getClass().getDeclaredMethod("parseTimeout", String.class);
		parseTimeout.setAccessible(true);
		
		assertEquals(parseTimeout.invoke(datastoreClient, "1000"), 1000);
		assertEquals(parseTimeout.invoke(datastoreClient, "10s"), 10000);
		assertEquals(parseTimeout.invoke(datastoreClient, "10m"), 600000);
		assertEquals(parseTimeout.invoke(datastoreClient, "-1"), -1);
		assertEquals(parseTimeout.invoke(datastoreClient, "rubbish"), -1);
	}
}