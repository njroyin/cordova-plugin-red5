cordova-plugin-red5pro
------------------------

This is a cordova plugin interface to the Red5 Pro Mobile SDK for Android and IOS.



How to use it
===

Install like a typical Cordova Plugin using

```markdown
cordova plugin add cordova-plugin-red5
```
 

Once added you can access the Red 5 Pro SDK through the use of the **window.red5promobile** variable.
See docs for API reference.

Gotchas
-------------------------
When removing the plugin it leaves behind libraries in the jniLibs folder. You either need to 
delete this folder if re-installing this plugin or you need to remove your platform and re-add it. 


 
