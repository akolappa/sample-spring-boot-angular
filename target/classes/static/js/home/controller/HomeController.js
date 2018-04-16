var homeController = angular.module('HomeApp').controller('HomeController', function ($scope) {
	$scope.tab='Challenge';
	$scope.tabChange = function tabChange(tabValue) {
		$scope.tab = tabValue;
	}
	
	$scope.showActive = function showActive (tabName) {
		if(tabName === $scope.tab) {
			return 'tab-active';
		}
		else { 
			return '';
		}
	}
});