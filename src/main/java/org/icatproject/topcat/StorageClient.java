package org.icatproject.topcat;

import java.util.List;

import org.icatproject.topcat.httpclient.*;
import org.icatproject.topcat.exceptions.*;

import java.io.*;

import org.icatproject.topcat.repository.CacheRepository;

public abstract class StorageClient {

    protected HttpClient httpClient;

    public abstract String prepareData(String sessionId, List<Long> investigationIds, List<Long> datasetIds,
            List<Long> datafileIds) throws TopcatException;

    public abstract boolean isPrepared(String preparedId) throws TopcatException, IOException;

    public abstract boolean isTwoLevel() throws TopcatException;

    public abstract Long getSize(String sessionId, List<Long> investigationIds, List<Long> datasetIds,
            List<Long> datafileIds) throws TopcatException;

    public abstract Long getSize(CacheRepository cacheRepository, String sessionId, String entityType, Long entityId)
            throws TopcatException;
}
