/*
 *  [2015] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
'use strict';

/* global FormData */

/**
 * This class is handling the factory retrieval
 * @author Florent Benoit
 */
export class CodenvyFactory {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($resource, $q, codenvyUser, lodash) {
    // keep resource
    this.$resource = $resource;
    this.$q = $q;

    this.codenvyUser = codenvyUser;
    this.lodash = lodash;

    this.factories = [];
    this.factoriesById = new Map();
    this.parametersFactories = new Map();
    this.factoryContentsByWorkspaceId = new Map();

    // remote calls
    this.remoteFactoryFindAPI = this.$resource('/api/factory/find');
    this.remoteFactoryAPI = this.$resource('/api/factory/:factoryId', {factoryId: '@id'}, {
      put: {method: 'PUT', url: '/api/factory/:factoryId'},
      getFactoryContentFromWorkspace: {method: 'GET', url: '/api/factory/workspace/:workspaceId'},
      getParametersFactory: {method: 'POST', url: '/api/factory/resolver/'},
      createFactoryByContent: {
        method: 'POST',
        url: '/api/factory',
        isArray: false,
        headers: {'Content-Type': undefined},
        transformRequest: angular.identity
      }
    });
  }

  /**
   * Gets the factory service path.
   * @returns {string}
   */
  getFactoryServicePath() {
    return 'factory';
  }

  /**
   * Ask for loading the factory content in asynchronous way
   * If there are no changes, it's not updated
   * @param workspace
   * @returns {*} the promise
   */
  fetchFactoryContentFromWorkspace(workspace) {
    let deferred = this.$q.defer();

    let factoryContent = this.factoryContentsByWorkspaceId.get(workspace.id);
    if (factoryContent) {
      deferred.resolve(factoryContent);
    }

    let promise = this.remoteFactoryAPI.getFactoryContentFromWorkspace({
      workspaceId: workspace.id
    }).$promise;

    promise.then((factoryContent) => {
      //update factoryContents map
      this.factoryContentsByWorkspaceId.set(workspace.id, factoryContent);
      deferred.resolve(factoryContent);
    }, (error) => {
      if (error.status === 304) {
        let findFactoryContent = this.factoryContentsByWorkspaceId.get(workspace.id);
        deferred.resolve(findFactoryContent);
      } else {
        deferred.reject(error);
      }
    });

    return deferred.promise;
  }

  /**
   * Get factory from project
   * @param workspace
   * @return the factory content
   * @returns factoryContent
   */
  getFactoryContentFromWorkspace(workspace) {
    return this.factoryContentsByWorkspaceId.get(workspace.workspaceId);
  }

  /**
   * Create factory by content
   * @param factoryContent  the factory content
   * @returns {*} the promise
   */
  createFactoryByContent(factoryContent) {

    let formDataObject = new FormData();
    formDataObject.append('factory', factoryContent);

    return this.remoteFactoryAPI.createFactoryByContent({}, formDataObject).$promise;
  }

  /**
   * Gets the factories of the current user
   * @returns {Array}
   */
  getFactories() {
    return this.factories;
  }

  /**
   * Gets the factories of the current user
   * @returns {Map}
   */
  getFactoriesMap() {
    return this.factoriesById;
  }

  /**
   * Ask for loading the factory in asynchronous way
   * If there are no changes, it's not updated
   * @param factoryId the factory ID
   * @returns {*} the promise
   */
  fetchFactory(factoryId) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.get({factoryId: factoryId}).$promise;
    promise.then((tmpFactory) => {
      if (!tmpFactory) {
        deferred.resolve(tmpFactory);
        return;
      }
      //set default fields
      if (!tmpFactory.name) {
        tmpFactory.name = '';
      }

      let seeLink = this.detectLinks(tmpFactory);

      let factory = {
        originFactory: tmpFactory,
        idURL: seeLink[0],
        nameURL: seeLink[1]
      };

      //update factories map
      this.factoriesById.set(factoryId, factory);
      //update factories array
      this.factories.length = 0;
      this.factoriesById.forEach((value) => {
        this.factories.push(value);
      });

      deferred.resolve(factory);
    }, (error) => {
      if (error.status === 304) {
        let findFactory = this.factoriesById.get(factoryId);
        deferred.resolve(findFactory);
      } else {
        deferred.reject(error);
      }
    });
    return deferred.promise;
  }


  /**
   * Ask for getting parameter the factory in asynchronous way
   * If there are no changes, it's not updated
   * @param githubUrl the github URL
   * @returns {*} the promise
   */
  fetchParameterFactory(parameters) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.getParametersFactory({}, parameters).$promise;
    promise.then((tmpFactory) => {
      if (!tmpFactory) {
        deferred.resolve(tmpFactory);
        return;
      }
      //set default fields
      if (!tmpFactory.name) {
        tmpFactory.name = '';
      }

      let factory = {
        originFactory: tmpFactory
      };

      //update factories map
      this.parametersFactories.set(parameters, factory);

      deferred.resolve(factory);
    }, (error) => {
      if (error.status === 304) {
        let findFactory = this.parametersFactories.get(parameters);
        deferred.resolve(findFactory);
      } else {
        deferred.reject(error);
      }
    });
    return deferred.promise;
  }

  /**
   * Detects links for factory acceptance (with id and named one)
   * @param factory factory to detect links
   * @returns links acceptance links
   */
  detectLinks(factory) {
    let links = [];

    this.lodash.find(factory.links, function (link) {
      if (link.rel === 'accept' || link.rel === 'accept-named') {
        links.push(link.href);
      }
    });

    return links;
  }


  /**
   * Get the factory from factoryMap by factoryId
   * @param factoryId the factory ID
   * @returns factory
   */
  getFactoryById(factoryId) {
    return this.factoriesById.get(factoryId);
  }

  /**
   * Set the factory
   * @param originFactory
   * @returns {*} the promise
   */
  setFactory(originFactory) {
    let deferred = this.$q.defer();
    // check originFactory
    if (!originFactory || !originFactory.id) {
      deferred.reject({data: {message: 'Read factory error.'}});
      return deferred.promise;
    }

    let promise = this.remoteFactoryAPI.put({factoryId: originFactory.id}, originFactory).$promise;
    // check if was OK or not
    promise.then((updatedFactory) => {
      let factory = this.factoriesById.get(originFactory.id);
      if (factory) {
        updatedFactory.name = updatedFactory.name ? updatedFactory.name : '';

        factory.originFactory = updatedFactory;
        let seeLink = this.detectLinks(updatedFactory);
        factory.idURL = seeLink[0];
        factory.nameURL = seeLink[1];
        //update factories map
        this.factoriesById.set(originFactory.id, factory);//set factory
        //update factories array
        this.factories.length = 0;
        this.factoriesById.forEach((value)=> {
          this.factories.push(value);
        });
      } else {
        this.fetchFactory(originFactory.id);
      }
      deferred.resolve();
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Set the factory content by factoryId
   * @param factoryId  the factory ID
   * @param factoryContent  the factory content
   * @returns {*} the promise
   */
  setFactoryContent(factoryId, factoryContent) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.put({factoryId: factoryId}, factoryContent).$promise;
    // check if was OK or not
    promise.then(() => {
      let fetchFactoryPromise = this.fetchFactory(factoryId);
      //check if was OK or not
      fetchFactoryPromise.then((factory) => {
        deferred.resolve(factory);
      }, (error) => {
        deferred.reject(error);
      });
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Performs factory deleting by the given factoryId.
   * @param factoryId the factory ID
   * @returns {*} the promise
   */
  deleteFactoryById(factoryId) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.delete({factoryId: factoryId}).$promise;
    //check if was OK or not
    promise.then(() => {
      //update factories map
      this.factoriesById.delete(factoryId);//remove factory
      //update factories array
      this.factories.length = 0;
      this.factoriesById.forEach((value)=> {
        this.factories.push(value);
      });
      deferred.resolve();
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Helper method that extract the factory ID from a factory URL
   * @param factoryURL the factory URL to analyze
   * @returns the stringified ID of a factory
   */
  getIDFromFactoryAPIURL(factoryURL) {
    let index = factoryURL.lastIndexOf('/factory/');
    if (index > 0) {
      return factoryURL.slice(index + '/factory/'.length, factoryURL.length);
    }
  }


  /**
   * Ask for loading the factories in asynchronous way
   * If there are no changes, it's not updated
   */
  fetchFactories(maxItems, skipCount) {
    let queryData = {
      'maxItems': maxItems,
      'skipCount': skipCount
    };
    let deferred = this.$q.defer();
    let user = this.codenvyUser.getUser();

    if (user) {
      queryData['creator.userId'] = user.id;
      this._fetchFactories(queryData).then(() => {
        deferred.resolve();
      }, (error) => {
        deferred.reject(error);
      });

    } else {
      this.codenvyUser.fetchUser().then((user) => {
        queryData['creator.userId'] = user.id;
        this._fetchFactories(queryData).then(() => {
          deferred.resolve();
        }, (error) => {
          deferred.reject(error);
        });
      });
    }

    return deferred.promise;
  }

  _fetchFactories(queryData) {
    let promises = [];

    let factoriesPromise = this.remoteFactoryFindAPI.query(queryData).$promise;

    promises.push(factoriesPromise);
    factoriesPromise.then((remoteFactories) => {
      //gets factory resource based on the factory ID
      remoteFactories.forEach((factory) => {
        //there is a factory ID, so we can ask the factory details
        if (factory.id) {
          let tmpFactoryPromise = this.fetchFactory(factory.id);
          promises.push(tmpFactoryPromise);
        }
      });
    });

    return this.$q.all(promises);
  }

}
