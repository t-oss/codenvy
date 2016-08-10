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
 * Controller for a factory information.
 * @author Oleksii Orel
 */
export class FactoryInformationCtrl {

  /**
   * Default constructor that is using resource injection
   * @ngInject for Dependency injection
   */
  constructor($scope, codenvyAPI, cheNotification, $log) {
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;
    this.$log = $log;

    $scope.$watch(() => {
      return this.factory ? this.factory.originFactory : null;
    }, (newOriginFactory) => {
      this.copyOriginFactory = newOriginFactory ? angular.copy(newOriginFactory) : null;
    });
  }

  //Udpate factory content.
  updateFactory(factory) {
    let promise = this.codenvyAPI.getFactory().setFactory(factory);

    promise.then(() => {
      this.factory.originFactory = factory;
      this.cheNotification.showInfo('Factory information successfully updated.');
    }, (error) => {
      this.cheNotification.showError(error.data.message ? error.data.message : 'Update factory failed.');
      this.$log.error(error);
    });
  }
}
