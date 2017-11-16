const { NativeModules, NativeEventEmitter, Platform } = require('react-native');
const { GPSState } = NativeModules;

const gpsStateEmitter = new NativeEventEmitter(GPSState);
let subscription = null;
let listener = null;
const isDroid = Platform.OS == 'android';
const isIOS = Platform.OS == 'ios';

subscription = gpsStateEmitter.addListener('OnStatusChange', (response) => {
  if (listener) {
    let status = null;
    if (isIOS) {
      status = response;
    } else {
      status = response.status;
    }

    if (status) listener.apply(this, [status]);
  }
});

GPSState.addListener = (callback) => {
  if (typeof callback === 'function') {
    listener = callback;
    GPSState._startListen();
  }
};

GPSState.removeListener = (callback) => {
  if (subscription) {
    GPSState._stopListen();
    subscription.remove();
    subscription = null;
  }
};

GPSState.getStatus = () => GPSState._getStatus();

GPSState.openSettings = () => {
  GPSState._openSettings();
};

GPSState.requestAuthorization = (authType) => {
  if (isIOS) {
    let type = parseInt(authType);
    const min = GPSState.STATUS_NOT_DETERMINED;
    const max = GPSState.STATUS_AUTHORIZED_WHENINUSE;
    const inRange = type >= min && type <= max;

    if (isNaN(type) || !inRange) {
      type = GPSState.AUTHORIZED_WHENINUSE;
    }
    GPSState._requestAuthorization(type);
  } else {
    GPSState._requestAuthorization();
  }
};

module.exports = GPSState;
