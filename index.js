'use strict'

const {NativeModules, NativeEventEmitter, Platform} = require('react-native');
const {GPSState} = NativeModules;

const gpsStateEmitter = new NativeEventEmitter(GPSState);
var subscription = null;
var listener = null;
const isDroid = Platform.OS=='android';
const isIOS = Platform.OS=='ios';

subscription = gpsStateEmitter.addListener('OnStatusChange', (response)=>{
	if(listener){
		var status = null;
		if(isIOS){
			status = response;
		}else{
			status = response.status;
		}

		if(status)
			listener.apply(this, [status]);
	}
});

GPSState.addListener = (callback)=>{
	if(typeof callback == 'function'){
		listener = callback;
		GPSState._startListen();
	}
}

GPSState.removeListener = (callback)=>{
	if(subscription){
		GPSState._stopListen();
		subscription.remove();
		subscription = null;
	}
}

GPSState.getStatus = ()=>{
	return GPSState._getStatus();
}

GPSState.openSettings = ()=>{
	GPSState._openSettings();
}

GPSState.requestAuthorization = (authType)=>{
	if(isIOS){
		var type = parseInt(authType);
		var min = GPSState.STATUS_NOT_DETERMINED;
		var max = GPSState.STATUS_AUTHORIZED_WHENINUSE;
		var inRange = (type>=min && type <= max);

		if(isNaN(type) || !inRange){
			type = GPSState.AUTHORIZED_WHENINUSE;
		}
		GPSState._requestAuthorization(type);
	}else{
		GPSState._requestAuthorization();
	}
}

module.exports = GPSState;
