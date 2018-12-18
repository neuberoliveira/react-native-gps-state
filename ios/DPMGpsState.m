//
//  DPMGpsState.m
//  DPMGpsState
//
//  Created by Neuber Oliveira on 09/08/17.
//  Copyright © 2017 Dopamina Mob. All rights reserved.
//

/*
 kCLAuthorizationStatusNotDetermined
 The user has not yet made a choice regarding whether this app can use location services.
 
 kCLAuthorizationStatusRestricted
 This app is not authorized to use location services.
 
 kCLAuthorizationStatusDenied
 The user explicitly denied the use of location services for this app or location services are currently disabled in Settings.
 
 kCLAuthorizationStatusAuthorizedAlways
 This app is authorized to start location services at any time.
 
 kCLAuthorizationStatusAuthorizedWhenInUse
 This app is authorized to start most location services while running in the foreground.*/

#import "DPMGpsState.h"

@interface DPMGpsState()
	@property(nonatomic, assign) id<CLLocationManagerDelegate> delegate;
	@property(nonatomic, strong) CLLocationManager *manager;
	@property(nonatomic, strong) NSDictionary *constants;
@end

@implementation DPMGpsState
-(id)init {
	self = [super init];
	if (self) {
		self.manager = [[CLLocationManager alloc] init];
		self.constants = [[NSDictionary alloc] initWithObjectsAndKeys:
								[NSNumber numberWithInt:kCLAuthorizationStatusNotDetermined], @"NOT_DETERMINED",
								[NSNumber numberWithInt:kCLAuthorizationStatusRestricted], @"RESTRICTED",
								[NSNumber numberWithInt:kCLAuthorizationStatusDenied], @"DENIED",
								[NSNumber numberWithInt:kCLAuthorizationStatusAuthorized], @"AUTHORIZED",
								[NSNumber numberWithInt:kCLAuthorizationStatusAuthorizedAlways], @"AUTHORIZED_ALWAYS",
								[NSNumber numberWithInt:kCLAuthorizationStatusAuthorizedWhenInUse], @"AUTHORIZED_WHENINUSE",
								nil];
	}
	return self;
}


RCT_EXPORT_MODULE(GPSState);

#pragma mark Exported Methods
RCT_EXPORT_METHOD(_startListen){
	if(!self.manager.delegate){
		self.manager.delegate = self;
	}
}

RCT_EXPORT_METHOD(_stopListen){
	self.manager.delegate = nil;
}

RCT_EXPORT_METHOD(_getStatus:(RCTResponseSenderBlock)callback){
	callback(@[ [self getLocationStatus] ]);
}

RCT_REMAP_METHOD(_getStatus, getStatusWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
	NSNumber *status = [self getLocationStatus];
	if(status >= 0){
		resolve(status);
	}else{
		NSString *errorStrCode = [NSString stringWithFormat:@"%@", status];
		NSString *errorStr = @"UNKNOW_STATUS";
		//NSError *error = [[NSError alloc] initWithDomain:errorStr code:0 userInfo:nil];
		reject(errorStrCode, errorStr, nil);
	}
}

RCT_EXPORT_METHOD(_openSettings){
	UIApplication *application = [UIApplication sharedApplication];
	NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
	
	if ([application respondsToSelector:@selector(openURL:options:completionHandler:)]) {
		[application openURL:url options:@{} completionHandler:nil];
	}else{
		[application openURL:url];
	}
}

RCT_EXPORT_METHOD(_requestAuthorization:(nonnull NSNumber*)authType){
	int type = [authType intValue];
	int authInUse = [[self.constants objectForKey:@"AUTHORIZED_WHENINUSE"] intValue];
	int authAwalys = [[self.constants objectForKey:@"AUTHORIZED_ALWAYS"] intValue];
	
	if(type==authInUse){
		[self.manager requestWhenInUseAuthorization];
	}else if(type==authAwalys){
		[self.manager requestAlwaysAuthorization];
	}
}

-(NSDictionary *)constantsToExport {
	return self.constants;
}

- (NSArray<NSString *> *)supportedEvents {
	return @[@"OnStatusChange"];
}


#pragma mark Class Helpers 
-(NSNumber*)getLocationStatus {
	return [NSNumber numberWithInt:[CLLocationManager authorizationStatus]];
}


-(void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status {
	[self sendEventWithName:@"OnStatusChange" body:[NSNumber numberWithInt:status]];
}
@end
