var additionalController = angular.module('HomeApp').controller('AdditionalController', function ($scope, dataService, $state, $timeout) {
	
	$scope.showChallengePage = false;
	$scope.isUploadingExcel = false;
	$scope.errorMessage = null;
	$scope.progress = {
			percentage: 0,
			status: "In Progress",
			message: "File upload started"
	}
	
	$scope.isErrorMessagePresent = function () {
		if($scope.errorMessage == null) {
			return false;
		} else {
			return true;
		}
	}
	
	$scope.uploadFile = function () {
		if(!$scope.myFile) {
			return;
		}
		$scope.progress = {
				percentage: 0,
				status: "In Progress",
				message: "File upload started"
		}
		$scope.errorMessage = null;
        var file = $scope.myFile;
        $scope.isUploadingExcel = true;
        dataService.uploadFile(file).then(function success(response) {
        	$timeout($scope.checkProgress, 50);
        }, function error(response) {
            $scope.serverResponse = 'An error has occurred';
        })
    };
    
    $scope.checkProgress = function () {
    	dataService.getUploadProgress().then(function success(response) {
    		$scope.progress = response.data.content;
    		console.log("progress-->"+$scope.progress.percentage)
    		if($scope.progress.status == "In Progress") {
    			$timeout($scope.checkProgress, 50);
    		} else {
    			if($scope.progress.status == "Failed") {
    				$scope.isUploadingExcel = false;
    				$scope.errorMessage = $scope.progress.message;
    			} else {
    				$timeout($scope.showChallengePage, 500);
    			}
    		}
    	}, function error(response) {
    		console.log("error response -->"+ JSON.stringify(response));
        })
    }
    
    $scope.showChallengePage = function () {
    	$scope.isUploadingExcel = false;
		$state.go('additional.challenge', {excelName: "additional"}, {location:false, reload: true});
    }
	
});