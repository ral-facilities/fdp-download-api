'use strict';

angular.
    module('angularApp').factory('ICATQueryBuilder', ICATQueryBuilder);

ICATQueryBuilder.$inject = ['$log'];

function ICATQueryBuilder($log) {
        //private methods
        /**
         * taken from angualr source to build query string
         * @param  {[type]} obj      [description]
         * @param  {[type]} iterator [description]
         * @param  {[type]} context  [description]
         * @return {[type]}          [description]
         */
        /*function forEachSorted(obj, iterator, context) {
            var keys = sortedKeys(obj);
            for (var i = 0; i < keys.length; i++) {
                iterator.call(context, obj[keys[i]], keys[i]);
            }
            return keys;
        }*/

        /**
         * sort keys
         * @param  {[type]} obj [description]
         * @return {[type]}     [description]
         */
        /*function sortedKeys(obj) {
            var keys = [];
            for (var key in obj) {
                if (obj.hasOwnProperty(key)) {
                    keys.push(key);
                }
            }
            return keys.sort();
        }*/

        /**
         * squel expects false for DESC order
         * @param  {[type]} order [description]
         * @return {[type]}       [description]
         */
        function sortOrder(order) {
            if (typeof order !== 'undefined' || order !== null) {
                if (order.toUpperCase() === 'DESC') {
                    return false;
                }
            }

            return true;
        }

        /**
         * [validateRequiredArguments description]
         * @param  {[type]} mySessionId [description]
         * @param  {[type]} facility    [description]
         * @param  {[type]} queryParams [description]
         * @return {[type]}             [description]
         */
        function validateRequiredArguments(mySessionId, facility, queryParams) {
            //session argument is required and must be a string
            if (!mySessionId && ! angular.isString(mySessionId)) {
                throw new Error('Invalid arguments. Session string is expected');
            }

            //facility argument is required and must be an object
            if (! facility && ! angular.isObject(facility)) {
                throw new Error('Invalid arguments. facility object is expected');
            }

            //facility key facilityId and icatUrl are required
            if (! angular.isDefined(facility.facilityId) || ! angular.isDefined(facility.icatUrl)) {
                throw new Error('Invalid arguments. facility object must have the keys facilityId and icatUrl');
            }

            //queryParams is optional
            if (angular.isDefined(queryParams)) {
                //queryParams must be an object
                if (! angular.isObject(queryParams)) {
                    throw new Error('Invalid arguments. queryParams must be an object');
                }
            }
        }


        function urlEncodeParameters(params) {
            var p = _.object(_.map(params, function(v, k) {
                return [k, encodeURIComponent(v)];
            }));

            return p;
        }

        function buildParams(query, countQuery, searchExpr, queryParams, entityAlias) {
            var params = {};
            //var filterCountQuery = countQuery.clone();

            if (angular.isDefined(queryParams)) {
                if (!_.isEmpty(queryParams.search) && _.isArray(queryParams.search)) {
                    query.where(
                        searchExpr
                    );

                    /*filterCountQuery.where(
                        searchExpr
                    );

                    params.filterCountQuery = filterCountQuery;*/
                }

                //set limit
                if (angular.isDefined(queryParams.start) && angular.isDefined(queryParams.numRows)) {
                    query.limit(queryParams.start, queryParams.numRows);
                }

                //set sort
                if (angular.isDefined(queryParams.sortField) && angular.isDefined(queryParams.order)) {
                    query.order(entityAlias + '.' + queryParams.sortField, sortOrder(queryParams.order));
                }
            }

            return params;
        }

        function getSearchExpr(search, entityAlias) {
            var searchExpr = squel.expr();

            if (! _.isEmpty(search) && _.isArray(search)) {
                _.each(search, function(value) {
                    if (typeof value.search !== 'undefined') {
                        searchExpr.and('UPPER(' + entityAlias + '.' + value.field + ') LIKE ?', '%' + value.search.toUpperCase() + '%');
                    }
                });
            }

            return searchExpr;
        }


        // Public API here
        return {
            /*buildUrl: function(url, params) {
                if (!params) {
                    return url;
                }
                var parts = [];
                forEachSorted(params, function(value, key) {
                    if (value === null || value === undefined) {
                        return;
                    }
                    if (angular.isObject(value)) {
                        value = angular.toJson(value);
                    }
                    parts.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
                });
                return url + ((url.indexOf('?') === -1) ? '?' : '&') + parts.join('&');
            },*/

            getFacilityCycles: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(fc)')
                    .from('FacilityCycle', 'fc')
                    .from('fc.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('fc')
                    .from('FacilityCycle', 'fc')
                    .from('fc.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'fc');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'fc');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    entity: 'FacilityCycle',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getFacilityCyclesByInstrumentId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);
                //SELECT fc FROM FacilityCycle fc, fc.facility f, f.investigations inv, inv.investigationInstruments invins, invins.instrument ins
                //WHERE (f.id = 1 AND ins.id = 11 AND (inv.startDate BETWEEN fc.startDate AND fc.endDate)) ORDER BY fc.name ASC LIMIT 0, 50
                $log.debug('queryParams', queryParams);
                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(fc)')
                    .from('FacilityCycle', 'fc')
                    .from('fc.facility', 'f')
                    .from('f.investigations', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                            .and_begin() //jshint ignore:line
                                .and('inv.startDate BETWEEN fc.startDate AND fc.endDate')
                            .end()
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('fc')
                    .from('FacilityCycle', 'fc')
                    .from('fc.facility', 'f')
                    .from('f.investigations', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                            .and_begin() //jshint ignore:line
                                .and('inv.startDate BETWEEN fc.startDate AND fc.endDate')
                            .end()
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'fc');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'fc');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    entity: 'FacilityCycle',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatasetsByFacilityCycleId: function() {

            },

            getDatafilesByFacilityCycleId: function() {

            },


            getInvestigations: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(inv)')
                    .from('Investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('inv')
                    .from('Investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'inv');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'inv');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    entity: 'Investigation',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            /** get instruments **/
            getInstruments: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(ins)')
                    .from('Instrument', 'ins')
                    .from('ins.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('ins')
                    .from('Instrument', 'ins')
                    .from('ins.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'ins');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'ins');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Instrument',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getInvestigationsByFacilityCycleId: function(mySessionId, facility, queryParams){
                validateRequiredArguments(mySessionId, facility, queryParams);
                //SELECT fc FROM FacilityCycle fc, fc.facility f, f.investigations inv, inv.investigationInstruments invins, invins.instrument ins
                //WHERE (f.id = 1 AND ins.id = 11 AND (inv.startDate BETWEEN fc.startDate AND fc.endDate)) ORDER BY fc.name ASC LIMIT 0, 50
                $log.debug('queryParams', queryParams);
                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('inv')
                    .from('Investigation', 'inv')
                    .from('inv.facility', 'f')
                    .from('f.facilityCycles', 'fc')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                            .and_begin() //jshint ignore:line
                                .and('inv.startDate BETWEEN fc.startDate AND fc.endDate')
                            .end()
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('inv')
                    .from('Investigation', 'inv')
                    .from('inv.facility', 'f')
                    .from('f.facilityCycles', 'fc')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                            .and_begin() //jshint ignore:line
                                .and('inv.startDate BETWEEN fc.startDate AND fc.endDate')
                            .end()
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'inv');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'inv');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    entity: 'FacilityCycle',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);

            },

            getProposalsByInstrumentId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(DISTINCT inv.name)')
                    .from('Investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .distinct()
                    .field('inv.name')
                    .from('Investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'inv');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'inv');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Proposal',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getInvestigationsByProposalId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(DISTINCT inv)')
                    .from('Investigation', 'inv')
                    //.from('inv.investigationInstruments', 'invins')
                    //.from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.name = ?', queryParams.proposalId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .distinct()
                    .field('inv')
                    .from('Investigation', 'inv')
                    //.from('inv.investigationInstruments', 'invins')
                    //.from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.name = ?', queryParams.proposalId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'inv');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'inv');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Investigation',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },


            getInvestigationsByInstrumentId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(inv)')
                    .from('Investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('inv')
                    .from('Investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'inv');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'inv');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Investigation',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getInvestigationsByInstrumentIdByCycleId: function() {

            },

            getDatasets: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(ds)')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('ds')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'ds');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'ds');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Dataset',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatasetsByInstrumentId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(ds)')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('ds')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'ds');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'ds');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Dataset',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatasetsByInvestigationId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(ds)')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.id = ?', queryParams.investigationId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('ds')
                    .from('Dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.id = ?', queryParams.investigationId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'ds');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'ds');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Dataset',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatafiles: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(df)')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('df')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'df');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'df');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Datafile',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatafilesByInstrumentId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(df)')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('df')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.investigationInstruments', 'invins')
                    .from('invins.instrument', 'ins')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ins.id = ?', queryParams.instrumentId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'df');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'df');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Datafile',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatafilesByInvestigationId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(df)')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.id = ?', queryParams.investigationId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('df')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('inv.id = ?', queryParams.investigationId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'df');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'df');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Datafile',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },

            getDatafilesByDatasetId: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                console.log('getDatafilesByDatasetId queryParams', queryParams);

                var countQuery = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('COUNT(df)')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ds.id = ?', queryParams.datasetId)
                    );

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('df')
                    .from('Datafile', 'df')
                    .from('df.dataset', 'ds')
                    .from('ds.investigation', 'inv')
                    .from('inv.facility', 'f')
                    .where(
                        squel.expr()
                            .and('f.id = ?', facility.facilityId)
                            .and('ds.id = ?', queryParams.datasetId)
                    );

                var searchExpr = getSearchExpr(queryParams.search, 'df');

                var params = buildParams(query, countQuery, searchExpr, queryParams, 'df');

                _.extend(params, {
                    sessionId: mySessionId,
                    query: query.toString(),
                    countQuery: countQuery.toString(),
                    //filterCountQuery: filterCountQuery.toString(),
                    entity: 'Datafile',
                    server: facility.icatUrl
                });

                return urlEncodeParameters(params);
            },


            getEntityById: function(mySessionId, facility, queryParams) {
                validateRequiredArguments(mySessionId, facility, queryParams);

                var query = squel.ICATSelect({ autoQuoteAliasNames: false })
                    .field('e')
                    .from(queryParams.entityType, 'e')
                    .where(
                        squel.expr()
                            .and('e.id = ?', queryParams.entityId)
                    );

                /*if (typeof queryParams.include !== 'undefined') {
                    _.each(queryParams.include, function(value) {
                        query.include(value);
                    });
                }*/

                var params = {
                    sessionId: mySessionId,
                    query: query.toString(),
                    entity: queryParams.entityType,
                    server: facility.icatUrl
                };

                return urlEncodeParameters(params);
            }
        };
    }