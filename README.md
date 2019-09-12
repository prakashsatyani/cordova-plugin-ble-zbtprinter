# cordova-plugin-ble-zbtprinter
This plugin defines a global 'cordova.plugins.zbtprinter' object, which provides an API for printing base64 images and labels on a Zebra printer, converting base64 string to equivalent ZPL code, get printer status and discovering Zebra printers with Bluetooth.


## Usage
Images can be printed on a Zebra printer in base64 format:

```js
var base64StringArray = [base64String1, base64String2];

cordova.plugins.zbtprinter.printImage(base64StringArray, MACAddress,
    function(success) { 
        alert("Print ok"); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

You can send data in ZPL Zebra Programing Language:

```js
var printText = "^XA^FO20,20^A0N,25,25^FDThis is a ZPL test.^FS^XZ";

cordova.plugins.zbtprinter.print(MACAddress, printText,
    function(success) { 
        alert("Print ok"); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

Discover nearby bluetooth Zebra printers:

```js
cordova.plugins.zbtprinter.discoverPrinters(
    function(MACAddress) { 
        alert("discovered a new printer: " + MACAddress); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

You can get a status response from a connected Zebra printer using:
```js
cordova.plugins.zbtprinter.getStatus(address,
    function(success){
        alert("Zbtprinter status: " + success);
    }, function(fail) {
        alert("Zbtprinter error: " + fail);
    }
);
```

Retrieve the currently connected printer name:

```js
cordova.plugins.zbtprinter.getPrinterName(MACAddress,
    function(printerName) { 
        alert("Printer name: " + printerName); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

Get ZPL equivalent code from the base64 Image string :

```js

var base64Image     	= base64String;
var addHeaderFooter 	= false;    	//Want to add header/footer ZPL code or not
var blacknessPercentage = 50;		//Blackness Percentage

cordova.plugins.zbtprinter.getZPLfromImage(base64Image, addHeaderFooter, blacknessPercentage,
    function(zplCode) {
        alert("ZPL Code : " + zplCode);
    }, function(error) {
	alert(error);
    }
);
```

## Installation
cordova plugin add https://github.com/prakashsatyani/cordova-plugin-ble-zbtprinter

## ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)

