package de.nulide.findmydevice;

import de.nulide.findmydevice.IFmdPluginCallback;

interface IFmdPlugin {
    void executeCommand(String cmd);
    void requestPluginAccess();
    void registerCallBack(IFmdPluginCallback callback);
}