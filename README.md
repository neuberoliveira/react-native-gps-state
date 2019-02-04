# React Native GPS State

### React Native Listener for GPS status changes
This lib will emitevent wheneaver the GPS status change, like when the permission was rejected or user disable Location service in system Settings.

### Instalation
1. Add library to project
   - `yarn add react-native-gps-state`
   - OR `npm install --save react-native-gps-state`
2. Link library to project
   - `react-native link react-native-gps-state`
   
If the link fails, do a manual setup:

#### Android

**android/settings.gradle**

```gradle
include ':react-native-gps-state'
project(':react-native-gps-state').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-gps-state/android')
```

**android/app/build.gradle**

```gradle
dependencies {
   ...
   compile project(':react-native-gps-state')
}
```

**MainApplication.java**

On top, where imports are:

```java
import br.com.dopaminamob.gpsstate.GPSStatePackage;
```

Add the `new GPSStatePackage()` class to your list of exported packages.

```java
@Override
protected List<ReactPackage> getPackages() {
    return Arrays.asList(
            new MainReactPackage(),
            new GPSStatePackage(),
    );
}
```


### Usage 

### Constants
| Platform 			| Status Code 	| Constant 				| Description 		
| :--- 				| :---:			| :--- 					| :---
| IOS/Android		| 0 	 		| NOT_DETERMINED 		| The user has not yet made a choice regarding whether this app can use location services.
| IOS/Android		| 1 	 		| RESTRICTED 			| This app is not authorized to use location services.
| IOS/Android		| 2 	 		| DENIED 				| The user explicitly denied the use of location services for this app or location services are currently disabled in Settings.
| IOS/Android		| 3 	 		| AUTHORIZED 			| This app is authorized to use location services.
| IOS				| 3 	 		| AUTHORIZED_ALWAYS 	| This app is authorized to start location services at any time.
| IOS				| 4 	 		| AUTHORIZED_WHENINUSE 	| This app is authorized to start most location services while running in the foreground


#### Methods
```javascript
//Open a system dialog requesting permission
//authType could be one of `AUTHORIZED_ALWAYS` or `AUTHORIZED_WHENINUSE`
GPSState.requestAuthorization(authType)
```

```javascript
//Open the system Settings to enable user to toggle Location on.
GPSState.openLocationSettings()
```

```javascript
//Open the system Settings in app details, so the user could manage all permissions in the `Permissions` tab
//in Android bellow M will fallback to `openLocationSettings()`
GPSState.openAppDetails()
```

```javascript
//ANDROID ONLY
//return true if system version is Marshmallow or above
GPSState.isMarshmallowOrAbove()
```

```javascript
//return true if the location permission is granted
GPSState.isAuthorized()
```

```javascript
//return true if the location permission is denied
GPSState.isDenied()
```

```javascript
//Get the current GPS state
GPSState.getStatus().then((status)=>{

})
```

#### Listeners

```javascript
import GPSState from 'react-native-gps-state'
...
componentWillMount(){
	GPSState.addListener((status)=>{
		switch(status){
			case GPSState.NOT_DETERMINED:
				alert('Please, allow the location, for us to do amazing things for you!')
			break;

			case GPSState.RESTRICTED:
				GPSState.openLocationSettings()
			break;

			case GPSState.DENIED:
				alert('It`s a shame that you do not allowed us to use location :(')
			break;

			case GPSState.AUTHORIZED_ALWAYS:
				//TODO do something amazing with you app
			break;

			case GPSState.AUTHORIZED_WHENINUSE:
				//TODO do something amazing with you app
			break;
		}
	})
	GPSState.requestAuthorization(GPSState.AUTHORIZED_WHENINUSE)
}

componentWillUnmount(){
	GPSState.removeListener()
}
```
