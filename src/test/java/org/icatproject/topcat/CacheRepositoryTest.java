package org.icatproject.topcat;

import java.util.*;
import java.lang.reflect.*;

import static org.junit.Assert.*;
import org.junit.*;


import javax.ejb.EJB;


import org.icatproject.topcat.repository.CacheRepository;

public class CacheRepositoryTest {

	@EJB
	private CacheRepository cacheRepository;

	private static String sessionId;


	@Test
	public void testPutAndGet() throws Exception {
		//cacheRepository.put("test:1", "Hello World!");
		//assertEquals( "Hello World!", (String) cacheRepository.get("test:1"));
	}

}