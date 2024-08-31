var exec = require('cordova/exec');

var HtmlPrinter = {
    printHTML: function(htmlContent, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'HtmlPrinter', 'printHTML', [htmlContent]);
    }
};

module.exports = HtmlPrinter;
