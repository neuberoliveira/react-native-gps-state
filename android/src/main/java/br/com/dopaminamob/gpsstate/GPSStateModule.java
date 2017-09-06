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
import android.os.Build;
import android.os.Bundle;
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
public class GPSStateModule extends ReactContextBaseJavaModule implements ActivityCompat.OnRequestPermissionsResultCallback/*, ActivityEventListener, LocationListener, GpsStatus.Listener*/ {
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
	public void _openSettings(){
		Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		getCurrentActivity().startActivityForResult(callGPSSettingIntent, 0);
	}
	
	@ReactMethod
	public void _requestAuthorization(){
		ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_AUTHORIZATION);
	}
	
	
	
	int getGpsState(){
		int status = STATUS_NOT_DETERMINED;
		boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		
		//TODO check permission to inform the correct status
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || targetSdkVersion >= Build.VERSION_CODES.M) {
			int permission = ActivityCompat.checkSelfPermission(getReactApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
			boolean isGranted = permission == PackageManager.PERMISSION_GRANTED;
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
		
		return status;
	}
	
	
	void sendEvent(int status){
		ReactContext reactContext = getReactApplicationContext();
		WritableMap params = Arguments.createMap();
		params.putInt("status", status);
		
		reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(EVENT_STATUS_CHANGE, params);
	}
	
	/*
	@Override
	public void onLocationChanged(Location location) {}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Toast.makeText(getReactApplicationContext(), "onStatusChanged: ["+provider+"]"+status, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onGpsStatusChanged(int event) {
		Toast.makeText(getReactApplicationContext(), "onGpsStatusChanged: "+event, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onProviderEnabled(String provider) {
		Toast.makeText(getReactApplicationContext(), "onProviderEnabled: "+provider, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		Toast.makeText(getReactApplicationContext(), "onProviderDisabled: "+provider, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
		int rc = resultCode;
		Toast.makeText(getReactApplicationContext(), "onActivityResult: "+rc, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void onNewIntent(Intent intent) {}
	*/
	
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

