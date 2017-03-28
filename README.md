# cordova-plugin-zbtprinter
This plugin defines a global 'cordova.plugins.zbtprinter' object, which provides an API for printing base64 images on a Zebra printer 
and discovering Zebra printer devices via Bluetooth.


## Usage
You can print images on the zebra printer in base64 format:

```js
cordova.plugins.zbtprinter.printImage(base64StringArray, MACAddress,
    function(success) { 
        alert("Print ok"); 
    }, function(fail) { 
        alert(fail); 
    }
);
```

And discover nearby Blueooth Zebra printers:

```js
cordova.plugins.zbtprinter.discoverPrinters(
    function(MACAddress) { 
        alert("discovered a new printer: " + MACAddress); 
    }, function(fail) { 
        alert(fail); 
    }
)
```

## Install
###Cordova

cordova plugin add https://github.com/aximobile/cordova-plugin-zbtprinter

##ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)

This plugin is based on mmilidoni's zebra printer plugin and can be found here: https://github.com/mmilidoni/zbtprinter.git
