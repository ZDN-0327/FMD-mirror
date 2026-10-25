package de.nulide.findmydevice;

interface IFmdPluginCallback {
    void onResponse(String response);
    void accessGranted();
}