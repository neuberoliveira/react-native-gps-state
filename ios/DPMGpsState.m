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
	@property(nonatomic, assign) CLAuthorizationStatus *currentStatus;
	@property(nonatomic, assign) bool hasListeners;
@end

@implementation DPMGpsState
-(id)init {
	self = [super init];
	if (self) {
        self.hasListeners = NO;
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
    
    if(!self.manager.delegate){
        self.manager.delegate = self;
    }
	return self;
}

RCT_EXPORT_MODULE(GPSState);

#pragma mark Exported Methods
RCT_EXPORT_METHOD(startListen){
	
}

RCT_EXPORT_METHOD(stopListen){
	self.manager.delegate = nil;
}

RCT_EXPORT_METHOD(getStatus:(RCTResponseSenderBlock)callback){
	callback(@[ [self getLocationStatus] ]);
}

RCT_REMAP_METHOD(getStatus, getStatusWithResolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject) {
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

RCT_EXPORT_METHOD(openSettings){
	UIApplication *application = [UIApplication sharedApplication];
	NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
	
	if ([application respondsToSelector:@selector(openURL:options:completionHandler:)]) {
		[application openURL:url options:@{} completionHandler:nil];
	}else{
		[application openURL:url];
	}
}

RCT_EXPORT_METHOD(requestAuthorization:(nonnull NSNumber*)authType){
	int type = [authType intValue];
	int authInUse = [[self.constants objectForKey:@"AUTHORIZED_WHENINUSE"] intValue];
	int authAwalys = [[self.constants objectForKey:@"AUTHORIZED_ALWAYS"] intValue];
	
	if(type==authInUse){
		[self.manager requestWhenInUseAuthorization];
	}else if(type==authAwalys){
		[self.manager requestAlwaysAuthorization];
	}
}

-(dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

+(BOOL)requiresMainQueueSetup {
    return YES;
}

// Will be called when this module's first listener is added.
-(void)startObserving {
    self.hasListeners = YES;
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
    self.hasListeners = NO;
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
	self.currentStatus = status;
	if(self.hasListeners){
		[self sendEventWithName:@"OnStatusChange" body:[NSNumber numberWithInt:status]];
	}
}
@end
