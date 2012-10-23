/**
 *
 * Copyright (c) 2009-2012
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the distribution.
 * Neither the name of the STFC nor the names of its contributors may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */
package uk.ac.stfc.topcat.ejb.session;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import uk.ac.stfc.topcat.core.exception.AuthenticationException;
import uk.ac.stfc.topcat.core.exception.ICATMethodNotFoundException;
import uk.ac.stfc.topcat.core.gwt.module.TDatafile;
import uk.ac.stfc.topcat.core.gwt.module.TDatafileParameter;
import uk.ac.stfc.topcat.core.gwt.module.TDataset;
import uk.ac.stfc.topcat.core.gwt.module.TDatasetParameter;
import uk.ac.stfc.topcat.core.gwt.module.TFacility;
import uk.ac.stfc.topcat.core.gwt.module.TFacilityCycle;
import uk.ac.stfc.topcat.core.gwt.module.TInvestigation;
import uk.ac.stfc.topcat.core.gwt.module.TopcatException;
import uk.ac.stfc.topcat.ejb.entity.IcatAuthentication;
import uk.ac.stfc.topcat.ejb.entity.TopcatUserDownload;
import uk.ac.stfc.topcat.ejb.manager.UtilityManager;

/**
 * This is Utiltiy bean implementation which has methods that utility functions
 * such as getting facility names, instrument names etc.
 * <p>
 * 
 * @author Mr. Srikanth Nagella
 * @version 1.0, &nbsp; 30-APR-2010
 * @since iCAT Version 3.3
 */
@Stateless
public class UtilityBean implements UtilityLocal {
    @PersistenceContext(unitName = "TopCATEJBPU")
    protected EntityManager manager;
    private UtilityManager utilManager;

    public UtilityBean() {
        utilManager = new UtilityManager();
    }

    @Override
    public ArrayList<TFacility> getFacilities() {
        return utilManager.getAllFacilities(manager);
    }

    @Override
    public ArrayList<String> getFacilityNames() {
        return utilManager.getAllFacilityNames(manager);
    }

    @Override
    public ArrayList<String> getInstrumentNames(String sessionId, String serverName) {
        return utilManager.getInstrumentNames(manager, sessionId, serverName);
    }

    @Override
    public ArrayList<String> getInvestigationTypes(String sessionId, String serverName) {
        return utilManager.getInvestigationTypes(manager, sessionId, serverName);
    }

    @Override
    public ArrayList<TFacilityCycle> getFacilityCyclesWithInstrument(String sessionId, String serverName,
            String instrument) throws ICATMethodNotFoundException {
        return utilManager.getFacilityCyclesWithInstrument(manager, sessionId, serverName, instrument);
    }

    @Override
    public ArrayList<TInvestigation> getMyInvestigationsInServer(String sessionId, String serverName) {
        return utilManager.getMyInvestigationsInServer(manager, sessionId, serverName);
    }

    @Override
    public ArrayList<TInvestigation> getMyInvestigationsInServerAndInstrument(String sessionId, String serverName,
            String instrumentName) throws TopcatException {
        return utilManager.getMyInvestigationsInServerAndInstrument(manager, sessionId, serverName, instrumentName);
    }

    @Override
    public ArrayList<TInvestigation> getAllInvestigationsInServerAndInstrument(String sessionId, String serverName,
            String instrumentName) throws TopcatException {
        return utilManager.getAllInvestigationsInServerAndInstrument(manager, sessionId, serverName, instrumentName);
    }

    @Override
    public ArrayList<TInvestigation> getMyInvestigationsInServerInstrumentAndCycle(String sessionId, String serverName,
            String instrumentName, TFacilityCycle cycle) throws TopcatException {
        return utilManager.getMyInvestigationsInServerInstrumentAndCycle(manager, sessionId, serverName,
                instrumentName, cycle);
    }

    @Override
    public ArrayList<TInvestigation> getAllInvestigationsInServerInstrumentAndCycle(String sessionId,
            String serverName, String instrumentName, TFacilityCycle cycle) throws TopcatException {
        return utilManager.getAllInvestigationsInServerInstrumentAndCycle(manager, sessionId, serverName,
                instrumentName, cycle);
    }

    @Override
    public TInvestigation getInvestigationDetails(String sessionId, String serverName, String investigationId)
            throws AuthenticationException {
        return utilManager.getInvestigationDetails(manager, sessionId, serverName, investigationId);
    }

    @Override
    public ArrayList<TDataset> getDatasetsInServer(String sessionId, String serverName, String investigationId)
            throws AuthenticationException {
        return utilManager.getDatasetsInServer(manager, sessionId, serverName, investigationId);
    }

    @Override
    public String getDatasetName(String sessionId, String serverName, String datasetId) {
        return utilManager.getDatasetName(manager, sessionId, serverName, datasetId);
    }

    @Override
    public ArrayList<TDatafile> getDatafilesInServer(String sessionId, String serverName, String datasetId) {
        return utilManager.getDatafilesInServer(manager, sessionId, serverName, datasetId);
    }

    @Override
    public ArrayList<TDatasetParameter> getDatasetInfoInServer(String sessionId, String serverName, String datasetId) {
        return utilManager.getDatasetInfo(manager, sessionId, serverName, datasetId);
    }

    @Override
    public ArrayList<TDatafileParameter> getDatafileInfoInServer(String sessionId, String serverName, String datafileId) {
        return utilManager.getDatafileInfo(manager, sessionId, serverName, datafileId);
    }

    @Override
    public ArrayList<String> getParameterNames(String sessionId, String facilityName) throws TopcatException {
        return utilManager.getParameterNames(manager, sessionId, facilityName);
    }

    @Override
    public ArrayList<String> getParameterUnits(String sessionId, String facilityName, String name)
            throws TopcatException {
        return utilManager.getParameterUnits(manager, sessionId, facilityName, name);
    }

    @Override
    public ArrayList<String> getParameterTypes(String sessionId, String facilityName, String name, String units)
            throws TopcatException {
        return utilManager.getParameterTypes(manager, sessionId, facilityName, name, units);
    }

    @Override
    public String getDatafilesDownloadURL(String sessionId, String serverName, ArrayList<Long> datafileIds) {
        return utilManager.getDatafilesDownloadURL(manager, sessionId, serverName, datafileIds);
    }

    @Override
    public String getDatasetDownloadURL(String sessionId, String serverName, Long datasetId) {
        return utilManager.getDatasetDownloadURL(manager, sessionId, serverName, datasetId);
    }

    @Override
    public List<TopcatUserDownload> getMyDownloadList(String sessionId, String serverName) {
        return utilManager.getMyDownloadList(manager, sessionId, serverName);
    }

    @Override
    public void addMyDownload(String sessionId, String facilityName, Date submitTime, String downloadName,
            String status, Date expiryTime, String url) {
        utilManager.addMyDownload(manager, sessionId, facilityName, submitTime, downloadName, status, expiryTime, url);
    }

    @Override
    public void updateDownloadStatus(String sessionId, String facilityName, String url, String updatedUrl, String status) {
        utilManager.updateDownloadStatus(manager, sessionId, facilityName, url, updatedUrl, status);
    }

    @Override
    public List<String> getAuthenticationTypes(String facilityName) {
        return utilManager.getAuthenticationTypes(manager, facilityName);

    }

}
