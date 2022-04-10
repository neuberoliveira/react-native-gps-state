import { NativeModules, Platform, NativeEventEmitter } from 'react-native';
import MessageQueue from 'react-native/Libraries/BatchedBridge/MessageQueue';

import type {
  PermissionType,
  ListenerCallback,
  PermissionState,
} from './types';

const LINKING_ERROR =
  `The package 'react-native-gps-state' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const GPSStateNative = NativeModules.GPSState
  ? NativeModules.GPSState
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

MessageQueue.spy((info: any) => {
  if (info.module !== 'WebSocketModule') {
    console.log(
      info.module,
      info.method,
      info.type === 0 ? 'native -> js' : 'js -> native'
    );
    console.log(info);
    console.log(
      '----------------------------------------------------------------------------------------------------------------------------------------------'
    );
  }
});
console.log('MessageQueue.spy attached');

const statuses: PermissionState = {
  NOT_DETERMINED: GPSStateNative.NOT_DETERMINED,
  RESTRICTED: GPSStateNative.RESTRICTED,
  DENIED: GPSStateNative.DENIED,
  AUTHORIZED: GPSStateNative.AUTHORIZED,
  AUTHORIZED_ALWAYS: GPSStateNative.AUTHORIZED_ALWAYS,
  AUTHORIZED_WHENINUSE: GPSStateNative.AUTHORIZED_WHENINUSE,
};

const isDroid = Platform.OS === 'android';
const isIOS = Platform.OS === 'ios';
const gpsStateEmitter = new NativeEventEmitter(NativeModules.GPSState);

let _listener: ListenerCallback | undefined;
let _isListening: boolean = true;
let _currentStatus: PermissionType = GPSStateNative.NOT_DETERMINED;
let _isMarshmallowOrAbove: boolean = false;

const isPermissionEquals = (expectedPerm: PermissionType): boolean =>
  _currentStatus === expectedPerm;

if (isDroid) {
  GPSStateNative.isMarshmallowOrAbove().then((isM: boolean) => {
    _isMarshmallowOrAbove = isM;
  });
}

GPSStateNative.getStatus().then(
  (status: PermissionType) => (_currentStatus = status)
);

console.log('_isListening', _isListening);
console.log('_listener......', _listener);

function onStatusHandler(response: any) {
  console.log('jsmodule -> OnStatusChange -> received....', response);
}
gpsStateEmitter.addListener('OnStatusChange', onStatusHandler);

const _addListener = (callback: ListenerCallback): (() => void) => {
  console.log('jsmodule -> _addListener');
  if (typeof callback === 'function') {
    console.log('jsmodule -> is a callback');
    _isListening = true;
    _listener = callback;
    GPSStateNative.startListen();
  }

  return _removeListener;
};

const _removeListener = (): void => {
  _isListening = false;
  _listener = undefined;
  // GPSStateNative.stopListen();
  // _subscription.remove();
};

const _getStatus = (): Promise<PermissionType> => GPSStateNative.getStatus();

const _requestAuthorization = (authType: PermissionType): void => {
  if (isIOS) {
    let type = authType;
    const min = GPSStateNative.STATUS_NOT_DETERMINED;
    const max = GPSStateNative.STATUS_AUTHORIZED_WHENINUSE;
    const inRange = type >= min && type <= max;

    if (isNaN(type) || !inRange) {
      type = GPSStateNative.AUTHORIZED_WHENINUSE;
    }
    GPSStateNative.requestAuthorization(type);
  } else {
    GPSStateNative.requestAuthorization();
  }
};

const GPSState = {
  ...statuses,

  isMarshmallowOrAbove: _isMarshmallowOrAbove,

  openAppDetails: () => GPSStateNative.openSettings(),

  openLocationSettings: () => GPSStateNative.openLocationSettings(),

  isAuthorized: () =>
    isPermissionEquals(GPSStateNative.AUTHORIZED_WHENINUSE) ||
    isPermissionEquals(GPSStateNative.AUTHORIZED_ALWAYS),

  isDenied: () => isPermissionEquals(GPSStateNative.DENIED),

  isRestricted: () => isPermissionEquals(GPSStateNative.RESTRICTED),

  isNotDetermined: () => isPermissionEquals(GPSStateNative.NOT_DETERMINED),

  addListener: _addListener,

  removeListener: _removeListener,

  getStatus: _getStatus,

  requestAuthorization: _requestAuthorization,

  sendDebugEvent: () => GPSStateNative.debugEmitter(),
};

export const useGPSState = () => {
  return {
    startListen: () => {
      gpsStateEmitter.addListener('OnStatusChange', (status) => {
        console.log('FINALLY', status);
      });
    },
  };
};

export default GPSState;
