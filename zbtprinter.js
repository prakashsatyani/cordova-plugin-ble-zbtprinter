	var exec = require("cordova/exec");

 	function ZebraBluetoothPrinter() {
    }

    ZebraBluetoothPrinter.prototype.esegui = function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ZebraBluetoothPrinter', 'print', ["ciccio"]);
    };
    var bluetoothPrinter = new ZebraBluetoothPrinter();
    module.exports = bluetoothPrinter;

