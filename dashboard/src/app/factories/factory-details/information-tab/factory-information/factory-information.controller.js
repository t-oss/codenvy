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
  constructor($scope, codenvyAPI, cheNotification, $location, $mdDialog, $log, $timeout) {
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;
    this.$location = $location;
    this.$mdDialog = $mdDialog;
    this.$log = $log;
    this.$timeout = $timeout;

    this.timeoutPromise;

    let ctrl = this;

    $scope.$watch('factoryInformationCtrl.factory.originFactory', function (newOriginFactory) {
      if(!newOriginFactory){
        return;
      }
      ctrl.copyOriginFactory = angular.copy(newOriginFactory);
    });
  }

  updateFactory(isFormValid) {
    if (!isFormValid || this.copyOriginFactory.name === this.factory.originFactory.name) {
      return;
    }

    this.$timeout.cancel(this.timeoutPromise);
    this.timeoutPromise = this.$timeout(() => {
      this.doUpdateFactory(this.copyOriginFactory);
    }, 2000);
  }

  //Udpate factory content.
  doUpdateFactory(factory) {
    let promise = this.codenvyAPI.getFactory().setFactory(factory);

    promise.then(() => {
      this.factory.originFactory = factory;
      this.cheNotification.showInfo('Factory information successfully updated.');
    }, (error) => {
      this.cheNotification.showError(error.data.message ? error.data.message : 'Update factory failed.');
      this.$log.log(error);
    });
  }

  //Perform factory deletion.
  deleteFactory(event) {
    let confirm = this.$mdDialog.confirm()
      .title('Would you like to delete the factory ' + (this.factory.originFactory.name ? '"' + this.factory.originFactory.name + '"' : this.factory.originFactory.id + '?'))
      .content('Please confirm for the factory removal.')
      .ariaLabel('Remove factory')
      .ok('Delete it!')
      .cancel('Cancel')
      .clickOutsideToClose(true)
      .targetEvent(event);
    this.$mdDialog.show(confirm).then(() => {
      // remove it !
      let promise = this.codenvyAPI.getFactory().deleteFactoryById(this.factory.originFactory.id);
      promise.then(() => {
        this.$location.path('/factories');
      }, (error) => {
        this.cheNotification.showError(error.data.message ? error.data.message : 'Delete failed.');
        this.$log.log(error);
      });
    });
  }
}
