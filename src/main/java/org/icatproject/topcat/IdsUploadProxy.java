package org.icatproject.topcat;

import java.util.Map;
import java.util.HashMap;

import java.io.*;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import org.icatproject.topcat.exceptions.InternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ApplicationScoped
@ServerEndpoint("/topcat/ws/user/upload")
public class IdsUploadProxy {

    private static final Logger logger = LoggerFactory.getLogger(IdsUploadProxy.class);

    private Map<Session, IdsUploadProxy.Upload> uploads = new HashMap<Session, IdsUploadProxy.Upload>();

    @OnOpen
    public void open(Session session) {
        try {
            uploads.put(session, new IdsUploadProxy.Upload(session));
            logger.info("open()");
        } catch(Exception e){
            logger.error("open(): " + e.getMessage());
        }
    }

    @OnClose
    public void close(Session session) {
        try {
            uploads.remove(session);
            logger.info("close()");
        } catch(Exception e){
            logger.error("close(): " + e.getMessage());
        }
    }

    @OnError
    public void onError(Throwable error) {
        logger.error("onError(): " + error.getMessage());
    }

    @OnMessage
    public void handleMessage(String message, Session session) {
        try {
            uploads.get(session).write(message);
        } catch(Exception e){
            logger.error("handleMessage(): " + e.getMessage());
        }
    }

    public class Upload {

        private Session session;
        private HttpURLConnection connection;
        private DataOutputStream outputStream;
        private Long contentLength;
        private Long bytesWritten = 0L;

        public Upload(Session session) throws Exception {
            this.session = session;

            Map<String, String> queryStringParams = Utils.parseQueryString(session.getQueryString());

            // Use the properties to get the idsUrl for the facilityName
            String facilityName = queryStringParams.get("facilityName");
            String idsUrl = "";
            if( facilityName != null ){
            	try {
            		idsUrl = FacilityMap.getInstance().getStorageUrl(facilityName);
            	} catch (InternalException ie){
            		logger.debug("IdsUploadProxy.Upload: error getting idsUrl for facility '" + facilityName + "': " + ie.getMessage());
            		throw ie;
            	}
            } else {
            	logger.debug("IdsUploadProxy.Upload: no facilityName supplied in request");
            	throw new InternalException("No facilityName supplied in request");
            }
            StringBuilder url = new StringBuilder();
            url.append(idsUrl + "/ids/put");
            url.append("?sessionId=" + URLEncoder.encode(queryStringParams.get("sessionId"), "UTF-8"));
            url.append("&name=" + URLEncoder.encode(queryStringParams.get("name"), "UTF-8"));
            url.append("&datafileFormatId=" + URLEncoder.encode(queryStringParams.get("datafileFormatId"), "UTF-8"));
            url.append("&datasetId=" + URLEncoder.encode(queryStringParams.get("datasetId"), "UTF-8"));

            connection = (HttpURLConnection) (new URL(url.toString())).openConnection();
            connection.setRequestMethod("PUT");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setChunkedStreamingMode(1000);
            contentLength = Long.parseLong(queryStringParams.get("contentLength"));
            connection.setRequestProperty("Content-Length", contentLength.toString());
            connection.setRequestProperty("Content-Type", "application/octet-stream");

            outputStream = new DataOutputStream(connection.getOutputStream());
        }

        public void write(String data) throws Exception {
            outputStream.writeBytes(data);
            outputStream.flush();
            bytesWritten += new Long(data.length());
            if(bytesWritten >= contentLength){
                outputStream.close();
                String responseBody = Utils.inputStreamToString(connection.getInputStream());
                session.getBasicRemote().sendText(responseBody);
            }
        }

    }

}
