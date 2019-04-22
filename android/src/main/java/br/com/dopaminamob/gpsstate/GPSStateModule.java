package br.com.dopaminamob.gpsstate;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

import java.util.HashMap;
import java.util.Map;

public class GPSStateModule extends ReactContextBaseJavaModule /*implements ActivityEventListener, ActivityCompat.OnRequestPermissionsResultCallback , ActivityEventListener, LocationListener, GpsStatus.Listener*/ {
    private static final int STATUS_NOT_DETERMINED = 0;
    private static final int STATUS_RESTRICTED = 1; //Location is disabled
    private static final int STATUS_DENIED = 2; //Permission for app to use location is denied
    private static final int STATUS_AUTHORIZED = 3; //Permission for app to use location is granted
    private static final int STATUS_AUTHORIZED_ALWAYS = 3; //Same as STATUS_AUTHORIZED
    private static final int STATUS_AUTHORIZED_WHENINUSE = 4; //Permission for app to use location when in use is granted

    private static final int REQUEST_CODE_AUTHORIZATION = 2308;
    private static final String EVENT_STATUS_CHANGE = "OnStatusChange";

    private boolean isListen = false;
    private int targetSdkVersion = -1;
    private int deviceSdkVersion = Build.VERSION.SDK_INT;
    private int currentStatus = STATUS_NOT_DETERMINED;

    private BroadcastReceiver mGpsSwitchStateReceiver = null;
    private LocationManager locationManager;

    public GPSStateModule(ReactApplicationContext reactContext) {
        super(reactContext);
        locationManager = (LocationManager) reactContext.getSystemService(reactContext.LOCATION_SERVICE);

        try {
            final PackageInfo info = reactContext.getPackageManager().getPackageInfo(reactContext.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "GPSState";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("NOT_DETERMINED", STATUS_NOT_DETERMINED);
        constants.put("RESTRICTED", STATUS_RESTRICTED);
        constants.put("DENIED", STATUS_DENIED);
        constants.put("AUTHORIZED", STATUS_AUTHORIZED);
        constants.put("AUTHORIZED_ALWAYS", STATUS_AUTHORIZED_ALWAYS);
        constants.put("AUTHORIZED_WHENINUSE", STATUS_AUTHORIZED_WHENINUSE);
        return constants;
    }

    @ReactMethod
    void startListen() {
        stopListen();
        try {
            mGpsSwitchStateReceiver = new GPSProvideChangeReceiver();
            getReactApplicationContext().registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
            isListen = true;
        } catch (Exception ex) {
        }
    }

    @ReactMethod
    void stopListen() {
        isListen = false;
        try {
            //locationManager.removeGpsStatusListener(this);
            if (mGpsSwitchStateReceiver != null) {
                getReactApplicationContext().unregisterReceiver(mGpsSwitchStateReceiver);
                mGpsSwitchStateReceiver = null;
            }
        } catch (Exception ex) {
        }
    }

    @ReactMethod
    void getStatus(Promise promise) {
        promise.resolve(getGpsState());
    }

    @ReactMethod
    void openSettings(boolean openInDetails) {
        Intent callGPSSettingIntent = new Intent();
        String packageName = getReactApplicationContext().getPackageName();
        String intentAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS;

        if (openInDetails && _NativeIsDeviceMOrAbove()) {
            intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

            Uri uri = Uri.fromParts("package", packageName, null);
            callGPSSettingIntent.setData(uri);
        }

        callGPSSettingIntent.setAction(intentAction);
        getCurrentActivity().startActivityForResult(callGPSSettingIntent, 0);
    }

    @ReactMethod
    public void requestAuthorization() {
        if (_NativeIsDeviceMOrAbove()) {
            String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
            Activity activity = getCurrentActivity();
            int requestCode = REQUEST_CODE_AUTHORIZATION;

            if (activity instanceof ReactActivity) {
                ((ReactActivity) activity).requestPermissions(permissions, requestCode, listener);

            } else if (activity instanceof PermissionAwareActivity) {
                ((PermissionAwareActivity) activity).requestPermissions(permissions, requestCode, listener);

            } else {
                ActivityCompat.requestPermissions(activity, permissions, requestCode);
            }
        }
    }

    @ReactMethod
    void isMarshmallowOrAbove(Promise promise) {
        promise.resolve(_NativeIsDeviceMOrAbove());
    }

    boolean _NativeIsDeviceMOrAbove() {
        return deviceSdkVersion >= Build.VERSION_CODES.M;
    }

    boolean _NativeIsTargetMOrAbove() {
        return targetSdkVersion >= Build.VERSION_CODES.M;
    }

    private PermissionListener listener = new PermissionListener() {
        public boolean onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
            if (requestCode == REQUEST_CODE_AUTHORIZATION) {
                sendEvent(getGpsState());
            }

            return true;
        }
    };

    int getGpsState() {
        int status;
        boolean enabled = isGpsEnabled();

        if (_NativeIsDeviceMOrAbove()) {
            boolean isGranted = isPermissionGranted();

            if (enabled) {
                if (isGranted) {
                    status = STATUS_AUTHORIZED;
                } else {
                    status = STATUS_DENIED;
                }
            } else {
                status = STATUS_RESTRICTED;
            }
        } else {
            status = (enabled ? STATUS_AUTHORIZED : STATUS_RESTRICTED);
        }

        currentStatus = status;
        return status;
    }

    int getPermission() {
        return ActivityCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
    }

    boolean isPermissionGranted() {
        int permission = getPermission();
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    void sendEvent(int status) {
        ReactContext reactContext = getReactApplicationContext();
        WritableMap params = Arguments.createMap();
        params.putInt("status", status);

        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_STATUS_CHANGE, params);
    }

    private final class GPSProvideChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.matches("android.location.PROVIDERS_CHANGED")) {
                sendEvent(getGpsState());
            }
        }
    }
}
