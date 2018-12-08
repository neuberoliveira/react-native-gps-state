'use strict'

const {NativeModules, NativeEventEmitter, Platform} = require('react-native');
const {GPSState as GPSStateNative} = NativeModules;

const isDroid = Platform.OS=='android';
const isIOS = Platform.OS=='ios';
const gpsStateEmitter = new NativeEventEmitter(GPSState);
const GPSState = {}

var subscription = null;
var listener = null;
var isListening = true;

subscription = gpsStateEmitter.addListener('OnStatusChange', (response)=>{
	if(listener){
		var status = null;
		if(isIOS){
			status = response;
		}else{
			status = response.status;
		}
		
		if(status && isListening){
			listener.apply(this, [status]);
		}
	}
});


GPSState.openAppDetails = ()=>{
	GPSStateNative.openSettings(true);
}

GPSState.openLocationSettings = ()=>{
	GPSStateNative.openSettings(false);
}

GPSState.isMarshmallowOrAbove = ()=>{
	if(isIOS){
		return null
	}
	return GPSStateNative.isMarshmallowOrAbove();
}

GPSState.isAuthorized = ()=>{
	return GPSStateNative.isAuthorized();
}

GPSState.isDenied = ()=>{
	return GPSStateNative.isDenied();
}

GPSState.addListener = (callback)=>{
	if(typeof callback == 'function'){
		isListening = true;
		listener = callback;
		GPSStateNative.startListen();
	}
}

GPSState.removeListener = (callback)=>{
	isListening = false
	GPSStateNative.stopListen();
}

GPSState.getStatus = ()=>{
	return GPSStateNative.getStatus();
}

GPSState.requestAuthorization = (authType)=>{
	if(isIOS){
		var type = parseInt(authType);
		var min = GPSStateNative.STATUS_NOT_DETERMINED;
		var max = GPSStateNative.STATUS_AUTHORIZED_WHENINUSE;
		var inRange = (type>=min && type <= max);

		if(isNaN(type)GPSStateNative || !inRange){
			type = GPSStateNative.AUTHORIZED_WHENINUSE;
		}
		GPSStateNative.requestAuthorization(type);
	}else{
		GPSStateNative.requestAuthorization();
	}
}

module.exports = GPSState;
