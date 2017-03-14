

(function() {
    'use strict';

    var app = angular.module('topcat');

    app.service('tcIcat', function($sessionStorage, $rootScope, $q, helpers, tcIcatEntity, tcIcatQueryBuilder, tcCache){

    	this.create = function(facility){
    		return new Icat(facility);
    	};

        /**
         * @interface Icat
         **/
    	function Icat(facility){
            var that = this;
            var cache;

            this.cache = function(){
              if(!cache) cache = tcCache.create('icat:' + facility.config().name);
              return cache;
            };

            /**
             * Returns the Facility that this particular Icat belongs to.
             * 
             * @method
             * @name  Icat#facility
             * @return {Facility}
             */
    		this.facility = function(){
    			return facility;
    		};

            /**
             * Returns the current version of this Icat.
             *
             * @method
             * @name Icat#version
             * @return {Promise<string>}
             */
    		this.version = function(){
    			var out = $q.defer();
    			this.get('version').then(function(data){
    				out.resolve(data.version);
    			}, function(){ out.reject(); });
    			return out.promise;
    		};

            /**
             * Returns and object containing the data stored within this session.
             *
             * @method
             * @name Icat#session
             * @return {object}
             */
    		this.session = function(){
    			var facilityName = facility.config().name;
    			if($sessionStorage.sessions && $sessionStorage.sessions[facilityName]){
    				return $sessionStorage.sessions[facilityName];
    			}
    			return {};
    		};


            /**
             * Refreshes the session's time to live.
             *
             * @method
             * @name Icat#refreshSession
             * @return {Promise}
             */
    		this.refreshSession = function(){
    			return this.put('session/' + this.session().sessionId);
    		};

            /**
             * Logs a user into this Icat with a username and password.
             *
             * @method
             * @name Icat#login
             * @param plugin {string} The type of authentication mechanism
             * @param username {string}
             * @param password {string}
             * @return {Promise}
             */
            /**
             * Logs a user into this Icat with token; used for single sign in systems like CAS.
             *
             * @method
             * @name Icat#login
             * @param plugin {string} The type of authentication mechanism
             * @param token {string}
             * @return {Promise}
             */
    		this.login = function(plugin, arg2, arg3){
                
    			var params;

                if(plugin == 'cas'){
                    var service = arg2;
                    var ticket = arg3;

                    params = {
                        json: JSON.stringify({
                            plugin: plugin,
                            credentials: [
                                {service: service},
                                {ticket: ticket}
                            ]
                        })
                    };
                } else {
                    var username = arg2;
                    var password = arg3;
                    if(username === undefined) username = "anon";

                    params = {
        				json: JSON.stringify({
                            plugin: plugin,
                            credentials: [
                                {username: username},
                                {password: password}
                            ]
                        })
                    };
    			};

    			return this.post('session', params).then(function(response){
    				if(!$sessionStorage.sessions) $sessionStorage.sessions = {};
    				var facilityName = facility.config().name;
                    var sessionId = response.sessionId;

                    $sessionStorage.sessions[facilityName] = { sessionId: sessionId }

                    return that.get('session/' + response.sessionId).then(function(response){
                        var username = response.userName;

                        $sessionStorage.sessions[facilityName].username = username;
                        $sessionStorage.sessions[facilityName].plugin = plugin;

                        var promises = [];

                        var name = facility.config().name;
                        promises.push(that.query([
                            "SELECT facility FROM Facility facility WHERE facility.name = ?", name
                        ]).then(function(results){
                            var facility = results[0];
                            if(facility){
                                $sessionStorage.sessions[facilityName].facilityId = facility.id;
                            } else {
                                console.error("Could not find facility by name '" + name + "'");
                            }
                        }));

                        var idsUploadDatasetType = facility.config().idsUploadDatasetType;
                        if(idsUploadDatasetType){
                            promises.push(that.query([
                                "SELECT datasetType FROM DatasetType datasetType, datasetType.facility as facility", 
                                "WHERE facility.name = ?", name,
                                "AND datasetType.name = ?", idsUploadDatasetType
                            ]).then(function(results){
                                var datasetType = results[0];
                                if(datasetType){
                                    $sessionStorage.sessions[facilityName].idsUploadDatasetTypeId = datasetType.id;
                                } else {
                                    console.error("Could not find IDS upload dataset type with name '" + idsUploadDatasetType + "'");
                                }
                            }));
                        }

                        var idsUploadDatafileFormat = facility.config().idsUploadDatafileFormat;
                        if(idsUploadDatafileFormat){
                            promises.push(that.query([
                                "SELECT datasetType FROM DatafileFormat datasetType, datasetType.facility as facility", 
                                "WHERE facility.name = ?", name,
                                "AND datasetType.name = ?", idsUploadDatafileFormat
                            ]).then(function(results){
                                var datasetType = results[0];
                                if(datasetType){
                                    $sessionStorage.sessions[facilityName].idsUploadDatafileFormatId = datasetType.id;
                                } else {
                                    console.error("Could not find IDS upload datafile format with name '" + idsUploadDatafileFormat + "'");
                                }
                            }));
                        }

                        promises.push(facility.admin().isValidSession(sessionId).then(function(isAdmin){
                            $sessionStorage.sessions[facilityName].isAdmin = isAdmin;
                            if(isAdmin) document.cookie = "isAdmin=true";
                        }));

                        promises.push(that.query(["select user from User user where user.name = ?", username]).then(function(users){
                            if(users[0]){
                                $sessionStorage.sessions[facilityName].fullName = users[0].fullName;
                            } else {
                                $sessionStorage.sessions[facilityName].fullName = username;
                            }
                        }));

                        return $q.all(promises).then(function(){
                          $rootScope.$broadcast('session:change');
                        });
                    });

    			});
    		};

            this.verifyPassword = function(password){
                var params = {
                    json: JSON.stringify({
                        plugin: this.session().plugin,
                        credentials: [
                            {username: this.session().username.replace(/^[^\/]*\//, '')},
                            {password: password}
                        ]
                    })
                };
                return this.post('session', params).then(function(response){
                    return true;
                }, function(){
                    return false;
                });
            };

            this.logout = helpers.overload({
            	'boolean': function(isSoft){
            		var promises = [];
            		if(!isSoft && this.session().sessionId){
            			promises.push(this.delete('session/' + this.session().sessionId, {
    		            	server: facility.config().icatUrl
    		            }));
            		}

                    if(this.session().plugin == 'cas'){
                        var authenticationTypesIndex = {};
                        _.each(facility.config().authenticationTypes, function(authenticationType){
                            authenticationTypesIndex[authenticationType.plugin] = authenticationType;
                        });

                        var authenticationType = authenticationTypesIndex['cas'];

                        var casIframe = $('<iframe>').attr({
                            src: authenticationType.casUrl + '/logout'
                        }).css({
                            position: 'relative',
                            left: '-1000000px',
                            height: '1px',
                            width: '1px'
                        });

                        $(document.body).append(casIframe);

                        var defered = $q.defer();
                        promises.push(defered.promise);

                        $(casIframe).on('load', function(){
                            defered.resolve();
                            $(casIframe).remove();
                        });

                    }

            		delete $sessionStorage.sessions[facility.config().name];
    				$sessionStorage.$apply();

                    if(tc.adminFacilities().length == 0){
                        document.cookie = 'isAdmin=; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
                    }


            		return $q.all(promises).then(function(){
            			$rootScope.$broadcast('session:change');
            		}, function(){
            			$rootScope.$broadcast('session:change');
            		});
            	},
            	'': function(){
            		return this.logout(false);
            	}
            });


            this.query = helpers.overload({
            	'array, object': function(query, options){    	
    	        	var defered = $q.defer();
                    var query = helpers.buildQuery(query);
                    var key = "query:" + query;

    	        	this.cache().getPromise(key, 10 * 60 * 60, function(){
                        return that.get('entityManager', {
                            sessionId: that.session().sessionId,
                            query: query,
                            server: facility.config().icatUrl
                        }, options);
                    }).then(function(results){
                    	defered.resolve(_.map(results, function(result){
                    		var type = _.keys(result)[0];
                    		if(helpers.typeOf(result) != 'object' || !type) return result;
            				var out = result[type];
            				out.entityType = helpers.uncapitalize(type);
            				out = tcIcatEntity.create(out, facility);
            				return out;
                		}));
                    }, function(response){
                    	defered.reject(response);
                    });

                    return defered.promise;
            	},
            	'promise, array': function(timeout, query){
    	        	return this.query(query, {timeout: timeout});
    	        },
    	        'promise, string': function(timeout, query){
    	        	return this.query([query], {timeout: timeout});
    	        },
            	'array': function(query){
    	        	return this.query(query, {});
    	        },
    	        'string': function(query){
    	        	return this.query([query], {});
    	        }
            });

            this.write = helpers.overload({
                'array, object': function(entities, options){
                    return this.post('entityManager', {
                        sessionId: this.session().sessionId,
                        entities: JSON.stringify(entities)
                    }, options);
                },
                'promise, array': function(timeout, entities){
                    return this.write(entities, {timeout: timeout});
                },
                'array': function(entities){
                    return this.write(entities, {});
                }
            });

          	this.queryBuilder = function(entityType){
        		return tcIcatQueryBuilder.create(this, entityType);
        	};

            this.getSize = helpers.overload({
              'string, number, object': function(entityType, entityId, options){
                var key = 'getSize:' + entityType + ":" + entityId;
                return this.cache().getPromise(key, function(){
                  var params = {
                    icatUrl: facility.config().icatUrl,
                    sessionId: that.session().sessionId,
                    entityType: entityType,
                    entityId: entityId
                  };
                  options.lowPriority = true;
                  return facility.tc().get('user/getSize', params, options).then(function(size){
                    return parseInt('' + size);
                  });
                });
              },
              'string, number, promise': function(entityType, entityId, timeout){
                return this.getSize(entityType, entityId, {timeout: timeout});
              },
              'string, number': function(entityType, entityId){
                return this.getSize(entityType, entityId, {});
              }
            });

          	helpers.generateRestMethods(this, facility.config().icatUrl + '/icat/');

            helpers.mixinPluginMethods('icat', this);
        }

	});

})();
