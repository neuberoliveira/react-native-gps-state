//
//  DPMGpsState.h
//  DPMGpsState
//
//  Created by Neuber Oliveira on 09/08/17.
//  Copyright © 2017 Dopamina Mob. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <React/RCTLog.h>

@interface DPMGpsState : RCTEventEmitter <RCTBridgeModule, CLLocationManagerDelegate>

@end
