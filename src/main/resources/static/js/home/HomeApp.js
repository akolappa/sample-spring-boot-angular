var homeApp = angular.module('HomeApp', [ 'ui.router' , 'rzModule', 'DataService', 'ngSanitize', 
	'angularjs-dropdown-multiselect', 'ui.bootstrap']);

homeApp.config(function($stateProvider, $urlRouterProvider) {
	$stateProvider.state({
		name : 'additional',
		templateUrl : 'static/html/additional.html'
	}).state({
		name : 'additional.challenge',
		params:{
			"excelName":'excelName'
		},
		templateUrl : 'static/html/challenge.html',
	}).state({
		name : 'challenge',
		templateUrl : 'static/html/challenge.html'
	}).state('default', {
		url : '',
		templateUrl : 'static/html/challenge.html'
	});

});

homeApp.filter('range', function() {
	return function(input, total) {
		total = parseInt(total);
		for (var i = 0; i < total; i++) {
			input.push(i);
		}
		return input;
	};
});

homeApp.directive('demoFileModel', function ($parse) {
    return {
        restrict: 'A', 

        link: function (scope, element, attrs) {
            var model = $parse(attrs.demoFileModel),
                modelSetter = model.assign; // define a setter for demoFileModel

            // Bind change event on the element
            element.bind('change', function () {
                // Call apply on scope, it checks for value changes and reflect
				// them on UI
                scope.$apply(function () {
                    // set the model value
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
});