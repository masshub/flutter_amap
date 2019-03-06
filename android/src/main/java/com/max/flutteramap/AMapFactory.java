package com.max.flutteramap;

import android.content.Context;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.StandardMessageCodec;
import io.flutter.plugin.platform.PlatformView;
import io.flutter.plugin.platform.PlatformViewFactory;

public class AMapFactory extends PlatformViewFactory {

  private final AtomicInteger mActivityState;
  private final PluginRegistry.Registrar mPluginRegistrar;

  public AMapFactory(AtomicInteger state, PluginRegistry.Registrar registrar) {
    super(StandardMessageCodec.INSTANCE);
    mActivityState = state;
    mPluginRegistrar = registrar;
  }

  @Override
  public PlatformView create(Context context, int id, Object args) {
    Map<String, Object> params = (Map<String, Object>) args;
    // params 可以传递部分初始化需要的参数
    AMapController aMapController = new AMapController(context, mActivityState, mPluginRegistrar,id);
    return aMapController;
  }
}
