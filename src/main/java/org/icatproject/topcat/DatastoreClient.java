package org.icatproject.topcat;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.icatproject.topcat.httpclient.*;
import org.icatproject.topcat.domain.Download;
import org.icatproject.topcat.exceptions.*;

import java.io.*;
import jakarta.json.*;

import org.icatproject.topcat.repository.CacheRepository;

public class DatastoreClient {

    private HttpClient httpClient;

    private int timeout;
    
    private long investigationSizeCacheLifetime;
    
    private boolean neverCacheZeroSizedInvestigations;
    
    // Allow ids.timeout property to have optional "s" or "m" post-qualifier (for seconds or minutes - default is (still) milliseconds)
    private static final Pattern IDS_TIMEOUT_PATTERN = Pattern.compile("(\\d+)([sm]?)");
   
    public DatastoreClient(String url){
        this.httpClient = new HttpClient(url);
        Properties properties = Properties.getInstance();
        this.timeout = parseTimeout(properties.getProperty("transfer.timeout", "-1"));
        this.investigationSizeCacheLifetime = Long.valueOf(properties.getProperty("investigationSizeCacheLifetimeSeconds", "0"));
        this.neverCacheZeroSizedInvestigations = Boolean.valueOf(properties.getProperty("neverCacheZeroSizedInvestigations", "false"));
    }

    private String buildData(Download download) {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("investigation_ids", Json.createArrayBuilder(download.getInvestigationIds()));
        objectBuilder.add("dataset_ids", Json.createArrayBuilder(download.getDatasetIds()));
        objectBuilder.add("datafile_ids", Json.createArrayBuilder(download.getDatafileIds()));
        return objectBuilder.build().toString();
    }

    private HashMap<String, String> buildHeaders(Download download) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + download.getSessionId());
        return headers;
    }

    public String prepareData(Download download) throws TopcatException {
        try {
            String data = buildData(download);
            HashMap<String, String> headers = buildHeaders(download);
            Response out = httpClient.post("restore/" + download.getTransport(), headers, data, timeout);

            if(out.getCode() == 404){
                throw new NotFoundException("Could not prepareData got a 404 response");
            } else if(out.getCode() >= 400){
                throw new BadRequestException("Could not prepareData got " + out.getCode() + " response: " + out.toString());
            }

            JsonObject jsonObject = Utils.parseJsonObject(out.toString());
            if (jsonObject.containsKey("bucket_name")) {
                return jsonObject.getString("bucket_name");
            } else {
                JsonArray jsonArray = jsonObject.getJsonArray("job_ids");
                List<String> job_ids = jsonArray.getValuesAs(JsonString::getString);
                return String.join(",", job_ids);
            }
        } catch(TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public boolean isPrepared(Download download) throws TopcatException, IOException {
        try {
            String preparedId = download.getPreparedId();
            Response response = httpClient.get("job/" + preparedId + "/complete", new HashMap<String, String>(), timeout);
            if(response.getCode() == 404){
                throw new NotFoundException("Could not run isPrepared got a 404 response");
            } else if(response.getCode() >= 400){
                throw new BadRequestException(Utils.parseJsonObject(response.toString()).getString("message"));
            }
            JsonObject response_object = Utils.parseJsonObject(response.toString());
            return response_object.getBoolean("complete");
        } catch(IOException e){
            throw e;
        } catch (TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public boolean isTwoLevel() throws TopcatException {
        try {
            Response response = httpClient.get("config", new HashMap<String, String>());
            JsonObject jsonObject = Utils.parseJsonObject(response.toString());
            return jsonObject.getString("archive") == "TAPE";
        } catch (TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public boolean isDirect(Download download) throws TopcatException {
        try {
            Response response = httpClient.get("config", new HashMap<String, String>());
            JsonObject jsonObject = Utils.parseJsonObject(response.toString());
            JsonObject storageEndpoints = jsonObject.getJsonObject("storage_endpoints");
            return storageEndpoints.getString(download.getTransport()) == "S3";
        } catch (TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    private Long getSize(String data, HashMap<String, String> headers) throws TopcatException {
        try {
            Response response = httpClient.post("size", headers, data, timeout);
            return Long.parseLong(response.toString());
        } catch (TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public Long getSize(Download download) throws TopcatException {
        String data = buildData(download);
        HashMap<String, String> headers = buildHeaders(download);
        return getSize(data, headers);
    }

    public Long getSize(CacheRepository cacheRepository, String sessionId, String entityType, Long entityId) throws TopcatException {
        String key = "getSize:" + entityType + ":" + entityId;
        
        // Set lifetime for investigation size caching from configuration (issue#394)
        
        Long lifetime;
        if( "investigation".equals(entityType) ) {
        	lifetime = this.investigationSizeCacheLifetime;
        } else {
        	lifetime = 0L;
        }
        Long size = (Long) cacheRepository.get(key,lifetime);

        if(size != null){
            return size;
        }

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        if(entityType.equals("investigation")){
            objectBuilder.add("investigation_ids", Json.createArrayBuilder().add(entityId));
        } else if(entityType.equals("dataset")){
            objectBuilder.add("dataset_ids", Json.createArrayBuilder().add(entityId));
        } else if(entityType.equals("datafile")){
            objectBuilder.add("datafile_ids", Json.createArrayBuilder().add(entityId));
        } else {
            throw new BadRequestException("Unknown or supported entity \"" + entityType + "\" for getSize");
        }
        String data = objectBuilder.build().toString();
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + sessionId);
        size = this.getSize(data, headers);
        
        // Never cache zero-sized investigations if so configured (issue#394)
        if( ! (this.neverCacheZeroSizedInvestigations && size == 0 && "investigation".equals(entityType))) {
        	cacheRepository.put(key,size);
        }

        return size;
    }
    
    /**
     * Parse the ids.timeout property. Expected format is a digit-string followed by an optional "m" or "s"
     * (minutes, seconds). Default units and returned value are in milliseconds (for backward compatibility).
     * @param timeoutStr
     * @return
     */
    private Integer parseTimeout(String timeoutStr) {
    	Integer idsTimeout = -1;
    	Matcher m = IDS_TIMEOUT_PATTERN.matcher(timeoutStr);
    	if( m.matches() ) {
    		idsTimeout = Integer.valueOf(m.group(1));
    		String qualifier = m.group(2);
    		if( "s".equals(qualifier) ) {
    			idsTimeout = idsTimeout * 1000;
    		} else if( "m".equals(qualifier) ) {
    			idsTimeout = idsTimeout * 1000 * 60;
    		}
    	}
    	return idsTimeout;
    }
}
