var challengeController = angular.module('HomeApp').controller('ChallengeController', function ($scope, dataService, $stateParams) {

	$scope.variables = [{
		"name":"Gender",
		"type":"Categorical",
		"values":["Male", "Female"]
		}, {
		"name": "DOB",
		"type": "Time",
		"range":[1984, 2005]
		}, {
		"name": "kids",
		"type": "Numeric",
		"range": [0, 10]
		}
	]
	
	
	
	$scope.excelMaxRows = 0
	
	$scope.sampleSize = { 
		options: {floor: 0, ceil: 500},
		value: 250
	}

	$scope.sratumData = null;
	
	$scope.treatmentCount = 1;
	if($stateParams.excelName) {
		$scope.excelName = $stateParams.excelName
	} else {
		$scope.excelName = "default"
	}
	
	$scope.init = function () {
		dataService.getExcelDetails($scope.excelName).then(function success(response) {
			$scope.excelMaxRows = response.data.content["maxRows"];
			$scope.sampleSize.options.ceil = $scope.excelMaxRows
			$scope.sampleSize.value = (0 + $scope.excelMaxRows)/2
			$scope.variables = response.data.content["variables"]
			$scope.convertVariableRange();
		}, function error(response) {
			console.log("error response -->"+ JSON.stringify(response));
		});	
	}
	
	$scope.changeOccured = function (changedVariable) {
		_.each($scope.variables, function(variable) {
			if(changedVariable.name == variable.name) {
				if($scope.isChangePresent(changedVariable.selectedValuesShow, variable.oldSelectedValuesShow)) {
					variable.oldSelectedValuesShow = [];
					_.each(changedVariable.selectedValuesShow, function (change) {
						variable.oldSelectedValuesShow.push(change);
					})
					$scope.updateMaxRows();
				}
			}
		});
	}
	
	$scope.updateMaxRows = function () {
		selectedVariables = [];
		_.each($scope.variables, function (variable) {
			if(variable.selected) {
				selectedVariables.push(variable)
			}
			
			if(variable["type"] == 'Categorical') {
				variable["selectedValues"] = [];
				_.each(variable.selectedValuesShow, function (valuesShow) {
					variable["selectedValues"].push(valuesShow.id);
				}) ;
			}
		});
		
		dataService.getMaxRows(selectedVariables, $scope.excelName).then(function success(response) {
			$scope.excelMaxRows = response.data.content;
			$scope.sampleSize.options.ceil = $scope.excelMaxRows;
			$scope.sampleSize.value = (0 + $scope.excelMaxRows)/2
		}, function error(response){
			console.log("error response -->"+ JSON.stringify(response));
		});
	}
	
	$scope.isChangePresent = function (var1, var2) {
		if(!var1 || !var2) {
			return false;
		}
		if(var1.length != var2.length) {
			return true;
		} else {
			for(i =0; i < var2.length; i++) {
				if(var1[i].id != var2[i].id) {
					return true;
				}
			}
		}
		return false;
	}
	
	$scope.convertVariableRange = function () {
		_.each($scope.variables, function (variable) {
			variable["selected"] = false
			
			if(variable["type"] == 'Time' || variable["type"] == "Numeric") {
				variable["options"] = {floor: variable["range"][0], ceil: variable["range"][1]}
				variable["value"] = (variable["range"][0] + variable["range"][1])/2
			} else {
				variable["valuesShow"] = [];
				variable["selectedValuesShow"] = [];
				variable["selectedValues"] = [];
				variable["oldSelectedValuesShow"] = [];
				
				_.each(variable.values, function (value) {
					variable["valuesShow"].push({id: value, label: value});
					variable["selectedValues"].push(value);
					variable["selectedValuesShow"].push({id:value});
					variable["oldSelectedValuesShow"].push({id:value});
				})
				
				variable.events = {
						onSelectionChanged: function () {
							$scope.changeOccured(variable);
						}
				}
			}
		})
	}
	
	$scope.selectDeselect = function (variable) {
		clickedVariable = _.find($scope.variables, function (scopeVariable) {
			return scopeVariable.name == variable.name
		});
		clickedVariable["selected"] = !clickedVariable["selected"];
	}
	
	$scope.showSelected = function (variable) {
		if(variable["selected"]) {
			return "variable-selected"
		} else {
			return ""
		}
	}
	
	$scope.getData = function () {
		selectedVariables = [];
		_.each($scope.variables, function (variable) {
			if(variable.selected) {
				selectedVariables.push(variable)
			}
			
			if(variable["type"] == 'Categorical') {
				variable["selectedValues"] = [];
				_.each(variable.selectedValuesShow, function (valuesShow) {
					variable["selectedValues"].push(valuesShow.id);
				}) ;
			}
		});
		
		if(selectedVariables.length == 0) {
			alert("please select any variable");
			return;
		}
		
		dataService.getStratum(selectedVariables, $scope.sampleSize.value, $scope.treatmentCount, $scope.excelName, $scope.excelMaxRows).then(function success(response) {
			$scope.stratumData = response.data.content;
		}, function error(response) {
			console.log("error response -->"+ JSON.stringify(response));
		})
	}
	
	$scope.findTotalTreatmentCount = function (index) {
		var sum = 0;
		_.each($scope.stratumData, function (rowData) {
			sum = sum + rowData[index].count;
		});
		return sum;
	}
	
	$scope.findGroupCount = function (data) {
		var sum = 0;
		_.each(data, function(treatmentData) {
			sum = sum + treatmentData.count;
		});
		return sum
	}
	
	$scope.totalSum = function () {
		var sum = 0;
		_.each($scope.stratumData, function (rowData) {
			_.each(rowData, function (treatmentData) {
				sum = sum + treatmentData.count;
			})
		});
		return sum;
	}
	
	$scope.showStratumInformation = function () {
		if($scope.stratumData) {
			return true;
		} else {
			return false;
		}
	}
	
	$scope.init();
	
});