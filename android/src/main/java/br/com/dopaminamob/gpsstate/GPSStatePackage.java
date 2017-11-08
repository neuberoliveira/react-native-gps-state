package br.com.dopaminamob.gpsstate;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by neuber on 14/08/17.
 */
public class GPSStatePackage implements ReactPackage {
	@Override
	public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
		return Collections.emptyList();
	}
	
	@Override
	public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
		List<NativeModule> modules = new ArrayList<>();
		modules.add(new GPSStateModule(reactContext));
		
		return modules;
	}
	
	public List<Class<? extends JavaScriptModule>> createJSModules() {
		return Collections.emptyList();
	}
}
