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
package uk.ac.stfc.topcat.gwt.client.widget;

import java.util.List;

import uk.ac.stfc.topcat.gwt.client.LoginInterface;
import uk.ac.stfc.topcat.gwt.client.UtilityService;
import uk.ac.stfc.topcat.gwt.client.UtilityServiceAsync;
import uk.ac.stfc.topcat.gwt.client.authentication.AuthenticationPlugin;
import uk.ac.stfc.topcat.gwt.client.authentication.AuthenticationPluginFactory;
import uk.ac.stfc.topcat.gwt.client.callback.EventPipeLine;
import uk.ac.stfc.topcat.gwt.client.model.AuthenticationModel;

import com.extjs.gxt.ui.client.Style.Orientation;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Window;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.form.LabelField;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.RowLayout;
import com.extjs.gxt.ui.client.widget.layout.TableLayout;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This class is a widget for login window.
 * <p>
 * 
 * @author Mr. Srikanth Nagella
 * @version 1.0, &nbsp; 30-APR-2010
 * @since iCAT Version 3.3
 */
public class LoginWidget extends Window {
    private final UtilityServiceAsync utilityService = GWT.create(UtilityService.class);
    private LoginInterface loginHandler = null;
    private LayoutContainer authTypeContainer = new LayoutContainer();
    private String facilityName;
    private ComboBox<AuthenticationModel> authTypesBox;
    private LayoutContainer authenticationWidget;
    private AuthenticationPlugin plugin;

    public LoginWidget() {
        setBlinkModal(true);
        setModal(true);

        setHeading("New Window");
        RowLayout rowLayout = new RowLayout(Orientation.VERTICAL);
        setLayout(rowLayout);

        TableLayout tl_layoutContainer = new TableLayout(2);
        tl_layoutContainer.setCellSpacing(5);
        authTypeContainer.setLayout(tl_layoutContainer);

        LabelField lblfldAuthType = new LabelField("Authentication Type");
        authTypeContainer.add(lblfldAuthType);

        authTypesBox = new ComboBox<AuthenticationModel>();
        authTypesBox.addSelectionChangedListener(new SelectionChangedListener<AuthenticationModel>() {
            @Override
            public void selectionChanged(SelectionChangedEvent<AuthenticationModel> se) {
                showPlugin(se.getSelectedItem());
            }
        });
        authTypesBox.setStore(new ListStore<AuthenticationModel>());
        authTypesBox.setDisplayField("authenticationType");
        authTypesBox.setTypeAhead(true);
        authTypesBox.setTriggerAction(TriggerAction.ALL);
        authTypeContainer.add(authTypesBox);
        authTypeContainer.setAutoHeight(true);
        add(authTypeContainer);

        authTypesBox.addListener(Events.Expand, new Listener<ComponentEvent>() {
            @Override
            public void handleEvent(ComponentEvent event) {
                EventPipeLine.getInstance().getTcEvents().fireResize();
            }
        });
        authTypesBox.addListener(Events.Collapse, new Listener<ComponentEvent>() {
            @Override
            public void handleEvent(ComponentEvent event) {
                EventPipeLine.getInstance().getTcEvents().fireResize();
            }
        });

        authenticationWidget = new LayoutContainer();
        authenticationWidget.setHeight("0px");
        authenticationWidget.setLayout(new FitLayout());
        authenticationWidget.setAutoHeight(true);
        add(authenticationWidget);
        setWidth(310);
        setLayout(new FitLayout());
        setAutoHeight(true);
    }

    public void setLoginHandler(LoginInterface loginHandler) {
        this.loginHandler = loginHandler;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
        setHeading("Login to " + facilityName);
        getAuthenticationTypes(facilityName);
    }

    public String getFacilityName() {
        return facilityName;
    }

    @Override
    public void show() {
        if (plugin != null) {
            setFocusWidget(plugin.getWidget());
        }
        super.show();
    }

    private void getAuthenticationTypes(final String facilityName) {
        authTypesBox.getStore().removeAll();
        authTypesBox.clear();
        authTypeContainer.hide();
        authenticationWidget.removeAll();
        EventPipeLine.getInstance().showRetrievingData();
        utilityService.getAuthenticationTypes(facilityName, new AsyncCallback<List<AuthenticationModel>>() {
            @Override
            public void onSuccess(List<AuthenticationModel> result) {
                EventPipeLine.getInstance().hideRetrievingData();
                authTypesBox.getStore().add(result);
                if (result.size() > 1) {
                    authTypeContainer.show();
                    authTypesBox.focus();
                } else if (result.size() == 1) {
                    showPlugin(result.get(0));
                } else {
                    hide();
                    EventPipeLine.getInstance().showErrorDialog(
                            "Error no authentication types found for " + facilityName);
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                EventPipeLine.getInstance().hideRetrievingData();
                EventPipeLine.getInstance()
                        .showErrorDialog("Error retrieving authentication types for " + facilityName);
            }
        });
    }

    /**
     * If user name exists focus on password.
     * 
     * @param model
     */
    private void showPlugin(AuthenticationModel model) {
        if (model == null) {
            // result of selecting auth type and then switching to a different
            // facility
            return;
        }

        authenticationWidget.removeAll();
        plugin = AuthenticationPluginFactory.getInstance().getPlugin(model.getAuthenticationPluginName());
        plugin.setAuthenticationModel(model);
        plugin.setLoginHandler(loginHandler);
        authenticationWidget.add(plugin.getWidget());
        authenticationWidget.layout(true);
        setFocusWidget(plugin.getWidget());
    }
}
