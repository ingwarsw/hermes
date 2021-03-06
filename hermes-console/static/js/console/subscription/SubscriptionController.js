var subscriptions = angular.module('hermes.subscription', [
    'ui.bootstrap',
    'hermes.modals',
    'hermes.subscription.repository',
    'hermes.subscription.health',
    'hermes.subscription.metrics',
    'hermes.topic.metrics'
]);

subscriptions.controller('SubscriptionController', ['SubscriptionRepository', 'SubscriptionHealth', 'SubscriptionMetrics', 'TopicRepository', 'TopicMetrics', '$scope', '$location', '$stateParams', '$uibModal', '$q', 'ConfirmationModal', 'toaster', 'PasswordService', 'MessagePreviewModal',
    function (subscriptionRepository, subscriptionHealth, subscriptionMetrics, topicRepository, topicMetrics, $scope, $location, $stateParams, $modal, $q, confirmationModal, toaster, passwordService, messagePreviewModal) {
        var groupName = $scope.groupName = $stateParams.groupName;
        var topicName = $scope.topicName = $stateParams.topicName;
        var subscriptionName = $scope.subscriptionName = $stateParams.subscriptionName;

        $scope.subscription = subscriptionRepository.get(topicName, subscriptionName);
        $scope.retransmissionLoading = false;

        $scope.metricsUrls = subscriptionMetrics.metricsUrls(groupName, topicName, subscriptionName);

        topicRepository.get(topicName).then(function(topic) {
            initRetransmissionCalendar(topic.topic.retentionTime.duration);
        });

        $q.all([
            subscriptionMetrics.metrics(topicName, subscriptionName),
            topicMetrics.metrics(topicName)
        ])
        .then(function(data) {
            $scope.metrics = data[0];
            $scope.health = subscriptionHealth.healthStatus($scope.subscription, data[1], data[0]);
        });

        subscriptionRepository.lastUndelivered(topicName, subscriptionName).$promise
                .then(function (lastUndelivered) {
                    $scope.lastUndelivered = lastUndelivered;
                })
                .catch(function () {
                    $scope.lastUndelivered = null;
                });

        subscriptionRepository.undelivered(topicName, subscriptionName).$promise
            .then(function (undelivered) {
                $scope.undelivered = undelivered;
            })
            .catch(function () {
                $scope.undelivered = [];
            });


        $scope.edit = function () {
            $modal.open({
                templateUrl: 'partials/modal/editSubscription.html',
                controller: 'SubscriptionEditController',
                size: 'lg',
                resolve: {
                    subscription: function () {
                        return $scope.subscription;
                    },
                    topicName: function () {
                        return topicName;
                    },
                    operation: function () {
                        return 'EDIT';
                    }
                }
            });
        };

        $scope.remove = function () {
            confirmationModal.open({
                action: 'Remove',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function (result) {
                passwordService.set(result.password);
                subscriptionRepository.remove(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            toaster.pop('success', 'Success', 'Subscription has been removed');
                            $location.path('/groups/' + groupName + '/topics/' + topicName).replace();
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
            });
        };

        $scope.suspend = function () {
            confirmationModal.open({
                action: 'Suspend',
                actionSubject: 'Subscription '+ $scope.subscription.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function (result) {
                passwordService.set(result.password);
                subscriptionRepository.suspend(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            $scope.subscription.state = 'SUSPENDED';
                            toaster.pop('success', 'Success', 'Subscription has been suspended');
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
            });
        };

        $scope.activate = function () {
            confirmationModal.open({
                action: 'Activate',
                actionSubject: 'Subcription ' + $scope.subscription.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function (result) {
                passwordService.set(result.password);
                subscriptionRepository.activate(topicName, $scope.subscription.name).$promise
                        .then(function () {
                            $scope.subscription.state = 'ACTIVE';
                            toaster.pop('success', 'Success', 'Subscription has been activated');
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                        })
                        .finally(function () {
                            passwordService.reset();
                        });
            });
        };

        $scope.previewMessage = function(cluster, partition, offset, messageId) {
            messagePreviewModal.previewMessage(topicName, cluster, partition, offset, messageId);
        };

        $scope.showTrace = function() {
            subscriptionRepository.eventTrace(topicName, subscriptionName, $scope.eventId).$promise
                .then(function (trace) {
                    $scope.eventTrace = trace;
                })
                .catch(function () {
                    $scope.eventTrace = [];
                });
        };

        $scope.retransmit = function() {
            var retransmitFromDate = $('#retransmitFromDate').val();

            if (retransmitFromDate === '') {
                toaster.pop('info', 'Info', 'Select date & time from which retransmission should be started');
                return;
            }

            confirmationModal.open({
                action: 'Retransmit',
                actionSubject: 'Subscription ' + $scope.subscription.name,
                passwordLabel: 'Group password',
                passwordHint: 'Password for group ' + groupName
            }).result.then(function (result) {
                    passwordService.set(result.password);
                    $scope.retransmissionLoading = true;
                    subscriptionRepository.retransmit(topicName, $scope.subscription.name, retransmitFromDate).$promise
                        .then(function () {
                            toaster.pop('success', 'Success', 'Retransmission started');
                            $scope.retransmissionLoading = false;
                        })
                        .catch(function (response) {
                            toaster.pop('error', 'Error ' + response.status, response.data.message);
                            $scope.retransmissionLoading = false;
                        });
                });
        };
    }]);

subscriptions.controller('SubscriptionEditController', ['SubscriptionRepository', '$scope', '$modalInstance', 'subscription', 'topicName', 'PasswordService', 'toaster', 'operation',
    function (subscriptionRepository, $scope, $modal, subscription, topicName, passwordService, toaster, operation) {
        $scope.topicName = topicName;
        $scope.subscription = subscription;
        $scope.operation = operation;

        var subscriptionBeforeChanges = _.cloneDeep(subscription);

        $scope.save = function () {
            var promise;
            passwordService.set($scope.groupPassword);

            if (operation === 'ADD') {
                promise = subscriptionRepository.add(topicName, $scope.subscription).$promise;
            }
            else {
                var subscriptionToSave = _.cloneDeep(subscription);
                if(subscription.endpoint === subscriptionBeforeChanges.endpoint) {
                    delete subscriptionToSave.endpoint;
                }
                promise = subscriptionRepository.save(topicName, subscriptionToSave).$promise;
            }

            promise
                    .then(function () {
                        toaster.pop('success', 'Success', 'Subscription has been saved');
                        $modal.close();
                    })
                    .catch(function (response) {
                        toaster.pop('error', 'Error ' + response.status, response.data.message);
                    })
                    .finally(function () {
                        passwordService.reset();
                    });
        };
    }]);

subscriptions.factory('SubscriptionFactory', [function () {
        return {
            create: function () {
                return {
                    name: '',
                    endpoint: '',
                    description: '',
                    supportTeam: '',
                    contact: '',
                    contentType: 'JSON',
                    subscriptionPolicy: {
                        messageTtl: 3600,
                        messageBackoff: 100,
                        rate: 1000
                    }
                };
            }
        };

    }]);

function initRetransmissionCalendar(daysBack) {
    var startDate = new Date();
    startDate.setDate(startDate.getDate() - daysBack);

    $('#retransmitCalendarButton').datetimepicker({
        format: "dd-MM-yyyy hh:ii",
        linkField: "retransmitFromDate",
        linkFormat: "yyyy-mm-ddThh:ii",
        showMeridian: true,
        autoclose: true,
        startDate: startDate,
        endDate: new Date(),
    });
}
