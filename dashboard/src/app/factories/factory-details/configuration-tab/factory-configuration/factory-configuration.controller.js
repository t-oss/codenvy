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

/**
 * Controller for a factory configuration.
 * @author Oleksii Orel
 */
export class FactoryConfigurationCtrl {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($route, $scope, $timeout, $filter, codenvyAPI, cheNotification, $log) {
    this.$timeout = $timeout;
    this.$filter = $filter;
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;
    this.$log = $log;

    this.factoryId = $route.current.params.id;

    $scope.$watch(() => {
      return this.factory ? this.factory.originFactory : null;
    }, (originFactory) => {
      this.$timeout(() => {
        this.updateFactoryContent(originFactory);
      }, 1000);
    });
  }

  //Update the factory content from origin factory.
  updateFactoryContent(originFactory) {
    if (!originFactory) {
      return;
    }

    let copyOriginFactory = angular.copy(originFactory);

    if (copyOriginFactory.links) {
      // remove links for display (links are automatically generated so no need to display them)
      delete copyOriginFactory.links;
    }
    this.originFactoryContent = copyOriginFactory;
    this.factoryContent = this.$filter('json')(this.originFactoryContent, 2);
  }

  //Update the factory information by factory Id.
  updateFactory(factoryId) {
    let promise = this.codenvyAPI.getFactory().fetchFactory(factoryId);

    promise.then((factory) => {
      this.factory = factory;
      this.cheNotification.showInfo('Factory information successfully updated.');
    }, (error) => {
      this.cheNotification.showError(error.data.message ? error.data.message : 'Update factory failed.');
      this.$log.error(error);
    });
  }

  //Set factory content by factory Id.
  setFactoryContent(factoryId, factoryContent) {
    let promise = this.codenvyAPI.getFactory().setFactoryContent(factoryId, factoryContent);

    promise.then((factory) => {
      this.factory = factory;
      this.cheNotification.showInfo('Factory information successfully updated.');
    }, (error) => {
      this.factoryContent = this.$filter('json')(this.originFactoryContent, 2);
      this.cheNotification.showError(error.data.message ? error.data.message : 'Update factory failed.');
      this.$log.error(error);
    });
  }

}
