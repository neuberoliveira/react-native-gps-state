package br.com.dopaminamob.gpsstate;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by neuber on 14/08/17.
 */
 
public class GPSStateModule extends ReactContextBaseJavaModule implements ActivityCompat.OnRequestPermissionsResultCallback /*, ActivityEventListener, LocationListener, GpsStatus.Listener*/ {
	private static final int STATUS_NOT_DETERMINED = 0;
	private static final int STATUS_RESTRICTED = 1;
	private static final int STATUS_DENIED = 2;
	private static final int STATUS_AUTHORIZED = 3;
	private static final int STATUS_AUTHORIZED_ALWAYS = 3;
	private static final int STATUS_AUTHORIZED_WHENINUSE = 4;
	
	private static final int REQUEST_CODE_AUTHORIZATION = 1;
	
	private static final String EVENT_STATUS_CHANGE = "OnStatusChange";

	private boolean isListen = false;
	private int targetSdkVersion = -1;
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

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
		if(requestCode==REQUEST_CODE_AUTHORIZATION){
			int status = STATUS_NOT_DETERMINED;

			if(grantResults.length>0) {
				int result = grantResults[0];
				status = (result == PackageManager.PERMISSION_GRANTED) ? STATUS_AUTHORIZED : STATUS_DENIED;
			}
			sendEvent(status);
		}
	}



	@ReactMethod
	public void _startListen() {
		_stopListen();
		try {
			mGpsSwitchStateReceiver = new GPSProvideChangeReceiver();
			getReactApplicationContext().registerReceiver(mGpsSwitchStateReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
			isListen = true;
		}catch(Exception ex){}
	}
	
	@ReactMethod
	public void _stopListen() {
		isListen = false;
		try {
			//locationManager.removeGpsStatusListener(this);
			if (mGpsSwitchStateReceiver != null) {
				getReactApplicationContext().unregisterReceiver(mGpsSwitchStateReceiver);
				mGpsSwitchStateReceiver = null;
			}
		}catch(Exception ex){}
	}
	
	@ReactMethod
	public void _getStatus(Promise promise) {
		promise.resolve( getGpsState() );
	}
	
	@ReactMethod
	public void _openSettings(boolean openDetails){
		Intent callGPSSettingIntent = new Intent();
		String packageName = getReactApplicationContext().getPackageName();
		String intentAction = Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
		if(openDetails && isMarshmallowOrAbove()){
			intentAction = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS;

			Uri uri = Uri.fromParts("package", packageName, null);
			callGPSSettingIntent.setData(uri);

			waitForPermissionBecomeGranted();
		}

		callGPSSettingIntent.setAction(intentAction);
		getCurrentActivity().startActivityForResult(callGPSSettingIntent, 0);
	}
	
	@ReactMethod
	public void _requestAuthorization(){
		ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_AUTHORIZATION);
	}
	
	
	
	int getGpsState(){
		int status;
		boolean enabled = isGpsEnabled();

		if(isMarshmallowOrAbove()) {
            boolean isGranted = isPermissionGranted();

			if(enabled) {
				if(isGranted){
					status = STATUS_AUTHORIZED;
				}else{
					status = STATUS_DENIED;
				}
			}else{
				status = STATUS_RESTRICTED;
			}
		}else{
			status = (enabled ? STATUS_AUTHORIZED : STATUS_RESTRICTED);
		}

		currentStatus = status;
		return status;
	}

	int getPermission(){
	    return ActivityCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
    }

    boolean isPermissionGranted(){
        int permission = getPermission();
	    return permission == PackageManager.PERMISSION_GRANTED;
    }

    boolean isGpsEnabled(){
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
	
	
	void sendEvent(int status){
		ReactContext reactContext = getReactApplicationContext();
		WritableMap params = Arguments.createMap();
		params.putInt("status", status);
		
		reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_STATUS_CHANGE, params);
	}

	boolean isMarshmallowOrAbove(){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || targetSdkVersion >= Build.VERSION_CODES.M;
	}

	boolean isPermissionEquals(int expectedPerm){
	    return currentStatus == expectedPerm;
    }

    boolean isAuthorized(){
        return isPermissionEquals(STATUS_AUTHORIZED);
    }

    boolean isDenied(){
        return isPermissionEquals(STATUS_DENIED);
    }

    boolean isUnknow(){
        return isPermissionEquals(STATUS_NOT_DETERMINED);
    }

    void waitForPermissionBecomeGranted(){
        final Ticker ticker = new Ticker();
        ticker.setInterval(3000);
        ticker.setMaxTicks(30);
        ticker.startTick(new TickerCallBack() {
            @Override
            public void tick() {
                if(isPermissionGranted()){
                    ticker.stopTick();
                    sendEvent(getGpsState());
                }
            }
        });
    }




	private final class GPSProvideChangeReceiver extends BroadcastReceiver {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.matches("android.location.PROVIDERS_CHANGED")) {
				int status = getGpsState();
				sendEvent(status);
			}
		}
	}
}

