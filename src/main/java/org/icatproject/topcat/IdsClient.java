package org.icatproject.topcat;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.icatproject.topcat.httpclient.*;
import org.icatproject.topcat.exceptions.*;
import org.icatproject.topcat.Properties;

import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcat.repository.CacheRepository;

public class IdsClient extends StorageClient{

    private Logger logger = LoggerFactory.getLogger(IdsClient.class);

    private HttpClient httpClient;

    private int timeout;
    
    private long investigationSizeCacheLifetime;
    
    private boolean neverCacheZeroSizedInvestigations;
    
    // Allow ids.timeout property to have optional "s" or "m" post-qualifier (for seconds or minutes - default is (still) milliseconds)
    private static final Pattern IDS_TIMEOUT_PATTERN = Pattern.compile("(\\d+)([sm]?)");
   
    public IdsClient(String url){
        this.httpClient = new HttpClient(url + "/ids");
        Properties properties = Properties.getInstance();
        this.timeout = parseTimeout(properties.getProperty("ids.timeout", "-1"));
        this.investigationSizeCacheLifetime = Long.valueOf(properties.getProperty("investigationSizeCacheLifetimeSeconds", "0"));
        this.neverCacheZeroSizedInvestigations = Boolean.valueOf(properties.getProperty("neverCacheZeroSizedInvestigations", "false"));
    }

    public String prepareData(String sessionId, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds) throws TopcatException {
        try {
            StringBuffer investigationIdsBuffer = new StringBuffer();
            StringBuffer datasetIdsBuffer = new StringBuffer();
            StringBuffer datafileIdsBuffer = new StringBuffer();
            
            if(investigationIds != null){
                for(Long investigationId : investigationIds){
                    if(investigationIdsBuffer.length() != 0){
                        investigationIdsBuffer.append(",");
                    }
                    investigationIdsBuffer.append(investigationId);
                }
            }

            if(datasetIds != null){
                for(Long datasetId : datasetIds){
                    if(datasetIdsBuffer.length() != 0){
                        datasetIdsBuffer.append(",");
                    }
                    datasetIdsBuffer.append(datasetId);
                }
            }

            if(datafileIds != null){
                for(Long datafileId : datafileIds){
                    if(datafileIdsBuffer.length() != 0){
                        datafileIdsBuffer.append(",");
                    }
                    datafileIdsBuffer.append(datafileId);
                }
            }

            StringBuffer data = new StringBuffer();
            data.append("sessionId=" + sessionId);
            data.append("&zip=true");
            if(investigationIdsBuffer.length() > 0){
                data.append("&investigationIds=" + investigationIdsBuffer);
            }
            if(datasetIdsBuffer.length() > 0){
                data.append("&datasetIds=" + datasetIdsBuffer);
            }
            if(datafileIdsBuffer.length() > 0){
                data.append("&datafileIds=" + datafileIdsBuffer);
            }

            Response out = httpClient.post("prepareData", new HashMap<String, String>(), data.toString(), timeout);
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
            Response response = httpClient.get("isPrepared?zip=true&preparedId=" + preparedId, new HashMap<String, String>(), timeout);
            if(response.getCode() == 404){
                throw new NotFoundException("Could not run isPrepared got a 404 response");
            } else if(response.getCode() >= 400){
                throw new BadRequestException(Utils.parseJsonObject(response.toString()).getString("message"));
            }
            return response.toString().equals("true");
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
            Response response = httpClient.get("isTwoLevel", new HashMap<String, String>());

            if(response.getCode() == 404){
                throw new NotFoundException("Could not run isTwoLevel got a 404 response");
            } else if(response.getCode() >= 400){
                throw new BadRequestException(Utils.parseJsonObject(response.toString()).getString("message"));
            }

            return response.toString().equals("true");
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
    }

    public Long getSize(String sessionId, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds) throws TopcatException {
        try {
            String prefix = "getSize?sessionId=" + sessionId + "&";
            Long size = 0L;
            
            for( String chunkedUrl : chunkOffsets(prefix,investigationIds,datasetIds,datafileIds)) {            	
                Response out = httpClient.get(chunkedUrl, new HashMap<String, String>(), timeout);
                if(out.getCode() == 404){
                    throw new NotFoundException("Could not getSize got a 404 response");
                } else if(out.getCode() >= 400){
                    throw new BadRequestException("Could not getSize got " + out.getCode() + " response: " + out.toString());
                }

                try {
                    size += Long.parseLong(out.toString());
                } catch (Exception e){
                    logger.info("getSize: can't extract number from response '" + out.toString() + "'; got exception: '" + e.getMessage() 
                        + "'; replacing with 0");
                }
            }
            return size;
        } catch(TopcatException e){
            throw e;
        } catch (Exception e){
            throw new BadRequestException(e.getMessage());
        }
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

        List<Long> investigationIds = new ArrayList<Long>();
        List<Long> datasetIds = new ArrayList<Long>();
        List<Long> datafileIds = new ArrayList<Long>();
            
        if(entityType.equals("investigation")){
            investigationIds.add(entityId);
        } else if(entityType.equals("dataset")){
            datasetIds.add(entityId);
        } else if(entityType.equals("datafile")){
            datafileIds.add(entityId);
        } else {
            throw new BadRequestException("Unknown or supported entity \"" + entityType + "\" for getSize");
        }

        size = this.getSize(sessionId,investigationIds,datasetIds,datafileIds);
        
        // Never cache zero-sized investigations if so configured (issue#394)
        if( ! (this.neverCacheZeroSizedInvestigations && size == 0 && "investigation".equals(entityType))) {
        	cacheRepository.put(key,size);
        }

        return size;
    }

    private List<String> chunkOffsets(String offsetPrefix, List<Long> investigationIds, List<Long> datasetIds, List<Long> datafileIds){
        List<String> out = new ArrayList<String>();
        List<Long> currentInvestigationIds = new ArrayList<Long>();
        List<Long> currentDatasetIds = new ArrayList<Long>();
        List<Long> currentDatafileIds = new ArrayList<Long>();

        Long newInvestigationId;
        Long newDatasetId;
        Long newDatafileId;

        while(true){
            newInvestigationId = null;
            newDatasetId = null;
            newDatafileId = null;


            if(investigationIds.size() > 0){
                newInvestigationId = investigationIds.get(0);
            } else if(datasetIds.size() > 0){
                newDatasetId = datasetIds.get(0);
            } else if(datafileIds.size() > 0){
                newDatafileId = datafileIds.get(0);
            } else {
                break;
            }

            String offset = generateDataSelectionOffset(offsetPrefix, currentInvestigationIds, newInvestigationId, currentDatasetIds, newDatasetId, currentDatafileIds, newDatafileId);

            if(offset.length() > 1024){
            	out.add(offset);
                currentInvestigationIds = new ArrayList<Long>();
                currentDatasetIds = new ArrayList<Long>();
                currentDatafileIds = new ArrayList<Long>();

            } else if(newInvestigationId != null){
                currentInvestigationIds.add(newInvestigationId);
            } else if(newDatasetId != null){
                currentDatasetIds.add(newDatasetId);
            } else if(newDatafileId != null){
                currentDatafileIds.add(newDatafileId);
            }

            if(newInvestigationId != null){
                investigationIds.remove(0);
                newInvestigationId = null;
            } else if(newDatasetId != null){
                datasetIds.remove(0);
                newDatasetId = null;
            } else if(newDatafileId != null){
                datafileIds.remove(0);
                newDatafileId = null;
            }

        }

        if(currentInvestigationIds.size() > 0 || currentDatasetIds.size() > 0 || currentDatafileIds.size() > 0){
            out.add(generateDataSelectionOffset(offsetPrefix, currentInvestigationIds, null, currentDatasetIds, null, currentDatafileIds, null));
        }

        return out;
    }

    private String generateDataSelectionOffset(
        String offsetPrefix,
        List<Long> investigationIds, Long newInvestigationId,
        List<Long> datasetIds, Long newDatasetId,
        List<Long> datafileIds, Long newDatafileId){

        StringBuffer investigationIdsBuffer = new StringBuffer();
        StringBuffer datasetIdsBuffer = new StringBuffer();
        StringBuffer datafileIdsBuffer = new StringBuffer();

        if(newInvestigationId != null){
            investigationIdsBuffer.append(newInvestigationId);
        }
        if(investigationIds != null){
            for(Long investigationId : investigationIds){
                if(investigationIdsBuffer.length() != 0){
                    investigationIdsBuffer.append(",");
                }
                investigationIdsBuffer.append(investigationId);
            }
        }

        if(newDatasetId != null){
            datasetIdsBuffer.append(newDatasetId);
        }
        if(datasetIds != null){
            for(Long datasetId : datasetIds){
                if(datasetIdsBuffer.length() != 0){
                    datasetIdsBuffer.append(",");
                }
                datasetIdsBuffer.append(datasetId);
            }
        }

        if(newDatafileId != null){
            datafileIdsBuffer.append(newDatafileId);
        }
        if(datafileIds != null){
            for(Long datafileId : datafileIds){
                if(datafileIdsBuffer.length() != 0){
                    datafileIdsBuffer.append(",");
                }
                datafileIdsBuffer.append(datafileId);
            }
        }

        StringBuffer idsBuffer = new StringBuffer();
        if(investigationIdsBuffer.length() > 0){
            idsBuffer.append("investigationIds=" + investigationIdsBuffer);
        }
        if(datasetIdsBuffer.length() > 0){
            if(idsBuffer.length() > 0){
                idsBuffer.append("&");
            }
            idsBuffer.append("datasetIds=" + datasetIdsBuffer);
        }
        if(datafileIdsBuffer.length() > 0){
            if(idsBuffer.length() > 0){
                idsBuffer.append("&");
            }
            idsBuffer.append("datafileIds=" + datafileIdsBuffer);
        }

        return offsetPrefix + idsBuffer;
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
