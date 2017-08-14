'use strict'

const {NativeModules, NativeEventEmitter} = require('react-native');
const {GPSState} = NativeModules;

const gpsStateEmitter = new NativeEventEmitter(GPSState);
var subscription = null;
var listener = null;

subscription = gpsStateEmitter.addListener('OnStatusChange', (status)=>{
	if(listener){
		listener.apply(this, [status]);
	}
});

GPSState.addListener = (callback)=>{
	if(typeof callback == 'function'){
		listener = callback;
		GPSState.startListen();
	}
}

GPSState.removeListener = (callback)=>{
	if(subscription){
		GPSState.stopListen();
		subscription.remove();
		subscription = null;
	}
}
module.exports = GPSState;
