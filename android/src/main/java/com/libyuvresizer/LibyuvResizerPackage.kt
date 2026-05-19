package com.libyuvresizer

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class LibyuvResizerPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == LibyuvResizerModule.NAME) {
      LibyuvResizerModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider() = ReactModuleInfoProvider {
    mapOf(LibyuvResizerModule.NAME to buildReactModuleInfo())
  }

  private fun buildReactModuleInfo(): ReactModuleInfo {
    val name = LibyuvResizerModule.NAME
    val isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
    // RN <= 0.73 constructor: (name, className, canOverride, needsEagerInit, hasConstants, isCxxModule, isTurboModule)
    val ctor7 = runCatching {
      ReactModuleInfo::class.java.getConstructor(
        String::class.java, String::class.java,
        Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType
      )
    }.getOrNull()
    if (ctor7 != null) {
      return ctor7.newInstance(name, name, false, false, false, false, isTurboModule) as ReactModuleInfo
    }
    // RN >= 0.74 constructor: (name, className, canOverride, needsEagerInit, isCxxModule, isTurboModule)
    val ctor6 = ReactModuleInfo::class.java.getConstructor(
      String::class.java, String::class.java,
      Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
      Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType
    )
    return ctor6.newInstance(name, name, false, false, false, isTurboModule) as ReactModuleInfo
  }
}
