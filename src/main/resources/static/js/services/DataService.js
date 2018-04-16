var dataService = angular.module('DataService', [ 'AppConnection' ]);
dataService.service('dataService', function(appConnection, $http) {

	this.getExcelDetails = function (excelName) {
		return appConnection.sendGetRequest("/excel/" + excelName, {Accept:"application/json"});
	}
	
	this.getStratum = function (variables, rows, treatmentCount, tableName, excelMaxRows) {
		return appConnection.sendPostRequest("/excel/stratum", {"Content-Type":"application/json", Accept:"application/json"}, 
				{variables : variables, rows: rows, treatmentCount : treatmentCount, tableName : tableName, excelMaxRows: excelMaxRows})
	}
	
	this.getMaxRows = function (variables, tableName) {
		return appConnection.sendPostRequest("/excel/maxrows", {"Content-Type":"application/json", Accept:"application/json"}, 
				{variables : variables, tableName : tableName})
	}
	
	this.getUploadProgress = function () {
		return appConnection.sendPostRequest("/excel/progress", {"Content-Type":"application/json", Accept:"application/json"})
	}
	
	this.uploadFile = function (file) {
		var fileFormData = new FormData();
        fileFormData.append('file', file);
        return $http.post('/excel/upload', fileFormData, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        });

	}

});