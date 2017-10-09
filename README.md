# cordova-plugin-bluetooth-zbtprinter
This plugin defines a global 'cordova.plugins.zbtprinter' object, which provides an API for printing base64 images on a Zebra printer 
and discovering Zebra printers with Bluetooth.


## Usage
Images can be printed on a Zebra printer in base64 format:

```js
cordova.plugins.zbtprinter.printImage(base64StringArray, MACAddress,
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
)
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

## Installation
cordova plugin add https://github.com/aximobile/cordova-plugin-zbtprinter

## ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)

This plugin is based on mmilidoni's zebra printer plugin and can be found here: https://github.com/mmilidoni/zbtprinter.git
