var appConnection = angular.module('AppConnection', []);
appConnection.factory('appConnection', function($http) {

	return {
		sendPostRequest : function sendPostRequest(url, headers, data) {
			return $http({
				method:'POST',
				url:url,
				headers:headers,
				data:data
			});
		},

		sendGetRequest : function sendGetRequest(url, headers) {
			return $http({
				method: 'GET',
				url: url,
				headers: headers
			});
		}
	}

});