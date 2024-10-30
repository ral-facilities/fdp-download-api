package org.icatproject.topcat;

import java.util.HashMap;
import java.util.List;

import org.icatproject.topcat.httpclient.*;
import org.icatproject.topcat.exceptions.*;

import java.io.*;
import jakarta.json.*;

import org.icatproject.topcat.repository.CacheRepository;

public class FtsClient extends StorageClient {

    private HttpClient httpClient;
   
    public FtsClient(String url){
        this.httpClient = new HttpClient(url);
    }

    public String prepareData(String sessionId, List<Long> investigationIds, List<Long> datasetIds,
            List<Long> datafileIds) throws TopcatException {
        try {
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            objectBuilder.add("investigation_ids", Json.createArrayBuilder(investigationIds));
            objectBuilder.add("dataset_ids", Json.createArrayBuilder(datasetIds));
            objectBuilder.add("datafile_ids", Json.createArrayBuilder(datafileIds));
            String data = objectBuilder.build().toString();

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + sessionId);
            Response out = httpClient.post("restore", headers, data);

            if(out.getCode() == 404){
                throw new NotFoundException("Could not prepareData got a 404 response");
            } else if(out.getCode() >= 400){
                throw new BadRequestException("Could not prepareData got " + out.getCode() + " response: " + out.toString());
            }

            return out.toString();
        } catch(TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public boolean isPrepared(String preparedId) throws TopcatException, IOException {
        try {
            Response response = httpClient.get("job/" + preparedId + "/complete", new HashMap<String, String>());
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

    public boolean isTwoLevel() {
        return true;
    }

    public Long getSize(String sessionId, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds) {
        return -1L;
    }

    public Long getSize(CacheRepository cacheRepository, String sessionId, String entityType, Long entityId) {
        return -1L;
    }
}
