#import "LibyuvResizer.h"

@implementation LibyuvResizer

RCT_EXPORT_MODULE(LibyuvResizer)

RCT_EXPORT_METHOD(resize:(NSString *)filePath
                  targetWidth:(double)targetWidth
                  targetHeight:(double)targetHeight
                  quality:(double)quality
                  rotation:(double)rotation
                  mode:(NSString *)mode
                  outputPath:(NSString *)outputPath
                  filterMode:(NSString *)filterMode
                  keepMeta:(BOOL)keepMeta
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
    (void)keepMeta;
    reject(@"E_NOT_IMPLEMENTED", @"resize is not yet implemented on iOS", nil);
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeLibyuvResizerSpecJSI>(params);
}

@end
