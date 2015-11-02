(function() {
    'use strict';

    angular.
        module('angularApp').service('IdsService', IdsService);

    IdsService.$inject = ['$http'];

    /*jshint -W098 */
    function IdsService($http) {
        /**
         * Get ICAT version
         * @param  {[type]} facility [description]
         * @return {[type]}          [description]
         */
        this.getSize = function(mySessionId, facility, options) {
            var url = facility.idsUrl + '/ids/getSize';

            var params = {
                params : {
                    sessionId : mySessionId,
                    server : facility.idsUrl
                },
                info : {
                    'facilityKeyName' : facility.facilityName,
                    'facilityTitle' : facility.title
                },
                cache: true,
                timeout: options.canceler
            };

            delete options.canceler;

            params = _.merge(params, {
                params : options
            });

            return $http.get(url, params);
        };


        this.isTwoLevel = function(facility, options) {
            var url = facility.idsUrl + '/ids/isTwoLevel';
            var params = {
                    params : {
                    },
                    info : {
                        'facilityKeyName' : facility.facilityName,
                        'facilityTitle' : facility.title
                    }
                };

            params = _.merge(params, {
                params : options
            });

            return $http.get(url, params);
        };


        this.getStatus = function(mySessionId, facility, options) {
            var url = facility.idsUrl + '/ids/getStatus';
            var params = {
                    params : {
                        sessionId : mySessionId,
                        server : facility.idsUrl
                    },
                    info : {
                        'facilityKeyName' : facility.facilityName,
                        'facilityTitle' : facility.title
                    },
                    cache: true
                };

            params = _.merge(params, {
                params : options
            });

            return $http.get(url, params);
        };
    }
})();
