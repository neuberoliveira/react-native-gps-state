'use strict'

const {NativeModules, NativeEventEmitter} = require('react-native');
const {GPSState} = NativeModules;

const gpsStateEmitter = new NativeEventEmitter(GPSState);
var subscription = null;

GPSState.addListener = (callback)=>{
	if(typeof callback == 'function'){
		GPSState.startListen();
		subscription = gpsStateEmitter.addListener('OnStatusChange', ()=>{
			alert('HAR HAR HAR!!!!');
		});
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
