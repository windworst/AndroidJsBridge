function createJsBridge() {
    var functions = {}
    var callbacks = {}
    function sendToNative(data) {
        if(null != window.webkit) {
            window.webkit.messageHandlers.AppModel.jsCallNative(data);   //iOS
        } else if(null != window.native_webview) {
            window.native_webview.jsCallNative(JSON.stringify(data));    //Android
        } else {
            console.log(data)
        }
    }

    var callbackCount = 0
    function makeJsCallBackString(callbackFunc) {
        if(null == callbackFunc) return null
        ++callbackCount
        var callbackString = 'JS_CB_' + callbackCount
        callbacks[callbackString] = callbackFunc
        return callbackString
    }

    function makeNativeCallback(nativeCallback) {
        if(null == nativeCallback) return null
        return function(paramString) {
            sendToNative({nativeCallback: nativeCallback, paramString: paramString})
        }
    }

    return {
        callNative: function (funcName, paramString, callbackFunc) {
            var data = {funcName: funcName, paramString: (paramString ? paramString : "")}
            if(null != callbackFunc) {
                data.jsCallback = makeJsCallBackString(callbackFunc)
            }
            sendToNative(data)
        },
        nativeCallJs: function(paramString) {
            var params = JSON.parse(paramString)
            if(null != params.jsCallback) {
                var func = callbacks[params.jsCallback]
                if(null != func) {
                    delete callbacks[params.jsCallback]
                    func(params.paramString)
                }
            } else if(null != params.funcName) {
                var func = functions[params.funcName]
                if(null != func) {
                    func(params.paramString, makeNativeCallback(params.nativeCallback))
                }
            }
        },
        register: function(name, func) {
            functions[name] = func
            return this
        },
        unregister: function(name) {
            delete functions[name]
            return this
        },
        unregisterAll: function() {
            functions = {}
            return this
        }
    }
}
