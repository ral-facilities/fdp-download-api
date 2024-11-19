package org.icatproject.topcat;

import java.util.HashMap;
import java.util.Map;

import org.icatproject.topcat.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacilityMap {
	
    private static FacilityMap instance = null;

    public synchronized static FacilityMap getInstance() throws InternalException {
       if(instance == null) {
          instance = new FacilityMap();
       }
       return instance;
    }
    
	private Logger logger = LoggerFactory.getLogger(FacilityMap.class);
	
	private Properties properties;
	private Map<String,String> facilityIcatUrl;
	private Map<String,String> facilityTransferUrl;
	
	public FacilityMap() throws InternalException{
		// The "normal" case: use the Topcat Properties instance (that reads run.properties)
		this(Properties.getInstance());
	}

	public FacilityMap(Properties injectedProperties) throws InternalException{
		
		// This allows us to inject a mock Properties instance for testing
		
		facilityIcatUrl = new HashMap<String,String>();
		facilityTransferUrl = new HashMap<String,String>();
		
		properties = injectedProperties;
		
		logger.info("FacilityMap: facility.list = '" + properties.getProperty("facility.list","") + "'");
		
		String[] facilities = properties.getProperty("facility.list","").split("([ ]*,[ ]*|[ ]+)");
		
		// Complain/log if property is not set
		if( facilities.length == 0 || (facilities.length == 1 && facilities[0].length() == 0)){
			logger.error( "FacilityMap: property facility.list is not defined.");
			throw new InternalException("Property facility.list is not defined.");
		}
		
		for( String facility : facilities ){
			logger.info("FacilityMap: looking for properties for facility '" + facility + "'...");
			String icatUrl = properties.getProperty("facility." + facility + ".icatUrl","");
			// Complain/log if property is not set
			if( icatUrl.length() == 0 ){
				String error = "FacilityMap: property facility." + facility + ".icatUrl is not defined.";
				logger.error( error );
				throw new InternalException( error );
			}
			logger.info("FacilityMap: icatUrl for facility '" + facility + "' is '" + icatUrl + "'");
			facilityIcatUrl.put( facility,  icatUrl );
			String transferUrl = properties.getProperty("facility." + facility + ".transferUrl","");
			// Complain/log if property is not set
			if( transferUrl.length() == 0 ){
				String error = "FacilityMap: property facility." + facility + ".transferUrl is not defined.";
				logger.error( error );
				throw new InternalException( error );
			}
			logger.info("FacilityMap: transferUrl for facility '" + facility + "' is '" + transferUrl + "'");
			facilityTransferUrl.put( facility,  transferUrl );
		}
	}
	
	public String getIcatUrl( String facility ) throws InternalException{
		String url = facilityIcatUrl.get( facility );
		if( url == null ){
			String error = "FacilityMap.getIcatUrl: unknown facility: " + facility;
			logger.error( error );
			throw new InternalException( error );
		}
		return url;
	}

	public String getTransferUrl( String facility ) throws InternalException{
		String url = facilityTransferUrl.get( facility );
		if( url == null ){
			String error = "FacilityMap.getTransferUrl: unknown facility: " + facility;
			logger.error( error );
			throw new InternalException( error );
		}
		return url;
	}
	
	public String getDownloadUrl( String facility, String downloadType ) throws InternalException{
		String url = "";
		// First, look for the property directly
		url = properties.getProperty( "facility." + facility + ".downloadType." + downloadType, "" );
		if( url.length() == 0 ){
			// No such property, so fall back to the facility transferUrl
			logger.info("FacilityMap.getDownloadUrl: no specific property for facility '" 
					+ facility + "' and download type '" + downloadType + "'; returning transferUrl instead" );
			url = this.getTransferUrl(facility);
		}
		return url;
	}
}
