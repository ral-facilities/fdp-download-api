
(function(){
    'use strict';

    var app = angular.module('topcat');

    app.controller('DownloadsController', function($state, $scope, $translate, $timeout, $uibModalInstance, $q, $interval, tc, uiGridConstants, helpers){
        var that = this;
        var pagingConfig = tc.config().paging;
        var timeout = $q.defer();
        var filter = function(){ return true; };
        var sorter = function(){ return true; };
        $scope.$on('$destroy', function(){ timeout.resolve(); });
        this.isScroll = pagingConfig.pagingType == 'scroll';
        this.gridOptions = _.merge({data: [], appScopeProvider: this}, tc.config().myDownloads.gridOptions);
        helpers.setupTopcatGridOptions(this.gridOptions, 'download');
        this.gridOptions.useExternalPagination =  false;
        this.gridOptions.useExternalSorting =  false;
        this.gridOptions.useExternalFiltering =  false;
        this.gridOptions.columnDefs.push({
            name : 'actions',
            title: 'DOWNLOAD.COLUMN.ACTIONS',
            enableFiltering: false,
            enable: false,
            enableColumnMenu: false,
            enableSorting: false,
            enableHiding: false,
            cellTemplate : [
                '<div class="ui-grid-cell-contents">',
                    '<span ng-if="row.entity.transport == \'https\'">',
                        '<a ng-if="row.entity.status == \'COMPLETE\'" href="{{row.entity.transportUrl + \'/ids/getData?preparedId=\' + row.entity.preparedId + \'&outname=\' + row.entity.fileName}}" translate="DOWNLOAD.ACTIONS.LINK.HTTP_DOWNLOAD.TEXT" class="btn btn-primary btn-xs" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.HTTP_DOWNLOAD.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"></a>',
                        '<span ng-if="row.entity.status != \'COMPLETE\'" class="inline-block" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.NON_ACTIVE_DOWNLOAD.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"><button translate="DOWNLOAD.ACTIONS.LINK.NON_ACTIVE_DOWNLOAD.TEXT" class="btn btn-primary btn-xs disabled"></button></span>',
                    '</span> ',
                    '<a ng-if="row.entity.transport == \'globus\'" href="#globus-help" target="_blank" translate="DOWNLOAD.ACTIONS.LINK.GLOBUS_DOWNLOAD.TEXT" class="btn btn-primary btn-xs" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.GLOBUS_DOWNLOAD.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"></a> ',
                    '<span ng-if="row.entity.transport == \'smartclient\'">',
                        '<a ng-if="row.entity.status == \'COMPLETE\'" href="#smartclient-complete" target="_blank" translate="DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_COMPLETE.TEXT" class="btn btn-primary btn-xs" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_COMPLETE.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"></a>',
                        '<span ng-if="row.entity.status != \'COMPLETE\'">',
                            '<a ng-if="!row.entity.isServer" href="#smartclient-start-server" target="_blank" translate="DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_START_SERVER.TEXT" class="btn btn-primary btn-xs" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_START_SERVER.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"></a>',
                            '<span ng-if="row.entity.isServer" class="inline-block" uib-tooltip="{{\'DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_NOT_COMPLETE.TOOLTIP.TEXT\' | translate}}" tooltip-placement="left" tooltip-append-to-body="true"><button translate="DOWNLOAD.ACTIONS.LINK.SMARTCLIENT_NOT_COMPLETE.TEXT" class="btn btn-primary btn-xs disabled"></button></span>',
                        '</span> ',
                    '</span>',
                    '<span class="remove-download">', 
                        '<a ng-click="grid.appScope.remove(row.entity)" translate="DOWNLOAD.ACTIONS.LINK.REMOVE.TEXT" class="btn btn-primary btn-xs" uib-tooltip="' + $translate.instant('DOWNLOAD.ACTIONS.LINK.REMOVE.TOOLTIP.TEXT') + '" tooltip-placement="left" tooltip-append-to-body="true"></a>',
                    '</span>',
                '</div>'
            ].join('')
        });
        var data = [];


        var refreshPromise = $interval(refresh, 1000 * 60);
        $scope.$on('$destroy', function(){ $interval.cancel(refreshPromise); });
        refresh();


        function refresh(){
            timeout.resolve();
            timeout = $q.defer();
            var promises = [];
            data = [];
            _.each(tc.userFacilities(), function(facility){
                var smartclient = facility.smartclient();
                var smartclientPing = smartclient.isEnabled() ? smartclient.ping(timeout.promise) : $q.reject();
                promises.push(facility.user().downloads(timeout.promise, "where download.isDeleted = false").then(function(results){
                    _.each(results, function(download){
                        if(download.transport == 'smartclient' && download.status != 'COMPLETE'){
                            smartclientPing.then(function(isServer){
                                download.isServer = isServer;
                            });
                        }
                    });
                    data = _.flatten([data, results]);
                }));
            });

            $q.all(promises).then(function(){
                that.gridOptions.data = _.select(data, filter);
                that.gridOptions.data.sort(sorter);
            });
        };
    
        this.remove = function(download){
            var _data = [];
            _.each(data, function(currentDownload){
                if(currentDownload.id != download.id) _data.push(currentDownload);
            });
            data = _data;
            that.gridOptions.data = _.select(data, filter);
            that.gridOptions.data.sort(sorter);

            download.delete().then(function(){
                if(data.length == 0){
                    $uibModalInstance.dismiss('cancel');
                }
            });
        };

        this.cancel = function() {
            $uibModalInstance.dismiss('cancel');
        };

        this.gridOptions.onRegisterApi = function(gridApi) {
            //sort change callback
            gridApi.core.on.sortChanged($scope, function(grid, sortColumns){
                $timeout(function(){
                    sorter = helpers.generateEntitySorter(sortColumns);
                    refresh();
                });
            });

            //filter change callback
            gridApi.core.on.filterChanged($scope, function(){
                var _timeout = $timeout(function(){
                    filter = helpers.generateEntityFilter(that.gridOptions);
                    refresh();
                });
            });
        };

    });

})();
