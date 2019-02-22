//@flow
'use strict'

type ListenerFunc = (status:number)=>void
type GPSStateType = {
	NOT_DETERMINED:number,
    RESTRICTED:number,
    DENIED:number,
    AUTHORIZED:number,
    AUTHORIZED_ALWAYS:number,
	AUTHORIZED_WHENINUSE:number,
	openAppDetails:()=>void,
	openLocationSettings:()=>void,
	isMarshmallowOrAbove:()=>boolean,
	isAuthorized:()=>boolean,
	isDenied:()=>boolean,
	isNotDetermined:()=>boolean,
	addListener:(callback:ListenerFunc)=>void,
	removeListener:()=>void,
	getStatus:()=>Promise<number>,
	requestAuthorization:(authType:number)=>void,
	
}
const {NativeModules, NativeEventEmitter, Platform} = require('react-native')
const GPSStateNative = NativeModules.GPSState

const isDroid = Platform.OS=='android'
const isIOS = Platform.OS=='ios'
const gpsStateEmitter = new NativeEventEmitter(GPSStateNative)

var _subscription
var _listener:ListenerFunc|null = null
var _isListening:boolean = true
var _currentStatus:number = GPSStateNative.NOT_DETERMINED
var _isMarshmallowOrAbove:boolean = false

GPSStateNative.getStatus().then((status:number)=>_currentStatus = status)
if(isDroid){
	GPSStateNative.isMarshmallowOrAbove().then((isM:boolean)=>_isMarshmallowOrAbove = isM)
}

_subscription = gpsStateEmitter.addListener('OnStatusChange', (response)=>{
	var status:number
	if(isIOS){
		status = response
	}else{
		status = response.status
	}
	
	_currentStatus = status
	if(_listener && status && _isListening){
		_listener(status)
	}
})


const GPSState:GPSStateType = {
    NOT_DETERMINED: GPSStateNative.NOT_DETERMINED,
    RESTRICTED: GPSStateNative.RESTRICTED,
    DENIED: GPSStateNative.DENIED,
    AUTHORIZED: GPSStateNative.AUTHORIZED,
    AUTHORIZED_ALWAYS: GPSStateNative.AUTHORIZED_ALWAYS,
	AUTHORIZED_WHENINUSE: GPSStateNative.AUTHORIZED_WHENINUSE,
	
	openAppDetails:()=>{
		GPSStateNative.openSettings(true)
	},
	openLocationSettings:()=>{
		GPSStateNative.openSettings(false)
	},
	isMarshmallowOrAbove:()=>{
		return _isMarshmallowOrAbove
	},
	
	isAuthorized:()=>(isPermissionEquals(GPSStateNative.AUTHORIZED_WHENINUSE) || isPermissionEquals(GPSStateNative.AUTHORIZED_ALWAYS)),
	
	isDenied:()=> isPermissionEquals(GPSStateNative.DENIED),
	
	isRestricted:()=> isPermissionEquals(GPSStateNative.RESTRICTED),
	
	isNotDetermined:()=> isPermissionEquals(GPSStateNative.NOT_DETERMINED),
	
	addListener:(callback:ListenerFunc)=>{
		if(typeof callback == 'function'){
			_isListening = true
			_listener = callback
			GPSStateNative.startListen()
		}
	},
	
	removeListener:()=>{
		_isListening = false
		_listener = null
		GPSStateNative.stopListen()
	},
	
	getStatus:()=>GPSStateNative.getStatus(),
	
	requestAuthorization: (authType)=>{
		if(isIOS){
			var type = parseInt(authType)
			var min = GPSStateNative.STATUS_NOT_DETERMINED
			var max = GPSStateNative.STATUS_AUTHORIZED_WHENINUSE
			var inRange = (type>=min && type <= max)
	
			if(isNaN(type) || !inRange){
				type = GPSStateNative.AUTHORIZED_WHENINUSE
			}
			GPSStateNative.requestAuthorization(type)
		}else{
			GPSStateNative.requestAuthorization()
		}
	},
}

function isPermissionEquals(expectedPerm){
	return _currentStatus == expectedPerm;
}
module.exports = GPSState
