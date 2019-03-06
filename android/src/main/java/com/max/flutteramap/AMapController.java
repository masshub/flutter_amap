package com.max.flutteramap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.platform.PlatformView;

/**
 * Created by Maker on 2019/3/6.
 * Describe:
 */
public class AMapController implements PlatformView, Application.ActivityLifecycleCallbacks,
        MethodChannel.MethodCallHandler, AMap.OnMapLoadedListener, AMap.OnCameraChangeListener,
        AMap.OnMarkerClickListener {

    public static final String CHANNEL = "com.flutter.amap";
    public static final String AMAP_ON_MAP_LOAD = "amap_on_map_load";
    public static final String AMAP_ON_CAMERA_CHANGE = "amap_on_camera_change";
    public static final String AMAP_CHANGE_CAMERA = "amap_change_camera";
    public static final String AMAP_ADD_MARKER = "amap_add_marker";
    public static final String AMAP_UPDATE_MARKER = "amap_update_marker";

    private final Context context;
    private final AtomicInteger atomicInteger;
    private final PluginRegistry.Registrar registrar;
    private final MethodChannel methodChannel;

    private MapView mapView;
    private AMap aMap;

    private boolean disposed = false;
    private final int registrarActivityHashCode;
    private final Map<String, Marker> markers;

    public AMapController(Context context, AtomicInteger atomicInteger,
                          PluginRegistry.Registrar registrar, int id) {
        this.context = context;
        this.atomicInteger = atomicInteger;
        this.registrar = registrar;
        this.registrarActivityHashCode = registrar.activity().hashCode();

        registrar.activity().getApplication().registerActivityLifecycleCallbacks(this);
        methodChannel = new MethodChannel(registrar.messenger(), CHANNEL + id);
        methodChannel.setMethodCallHandler(this);
        mapView = new MapView(context);
        mapView.onCreate(null);
        aMap = mapView.getMap();
        this.markers = new HashMap<String, Marker>();

        initListener();
    }

    private void initListener() {
        aMap.setOnMapLoadedListener(this);
        aMap.setOnCameraChangeListener(this);
        //覆盖物
        aMap.setOnMarkerClickListener(this);


    }


    @Override
    public View getView() {
        return mapView;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        mapView.onDestroy();
        registrar.activity().getApplication().unregisterActivityLifecycleCallbacks(this);

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onCreate(bundle);

    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onResume();

    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onPause();

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onSaveInstanceState(bundle);

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode) {
            return;
        }
        mapView.onDestroy();

    }

    /**
     * Flutter调Android
     *
     * @param methodCall
     * @param result
     */
    @Override
    public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {

        Marker marker = null;
        MarkerOptions markerOptions = null;

        switch (methodCall.method) {
            case AMAP_CHANGE_CAMERA:
                List<Object> arguments = (List<Object>) methodCall.arguments;
                CameraPosition cameraPosition = AMapConvert.toCameraPosition(arguments.get(0));
                boolean isAnimate = (boolean) arguments.get(1);
                changeCamera(cameraPosition, isAnimate);
                result.success(null);
                break;
            case AMAP_ADD_MARKER:
                markerOptions = AMapConvert.toMarkerOptions(methodCall.argument("options"));
                marker = aMap.addMarker(markerOptions);

                markers.put(marker.getId(), marker);

                // 将marker唯一标识传递回去
                result.success(marker.getId());
                break;
            case AMAP_UPDATE_MARKER:
                final String markerId = methodCall.argument("marker");

                marker = markers.get(markerId);
                markerOptions = AMapConvert.toMarkerOptions(methodCall.argument("options"));
                marker.setMarkerOptions(markerOptions);

                // 将marker唯一标识传递回去
                result.success(null);
                break;
            default:
                result.notImplemented();

        }


    }


    public void changeCamera(CameraPosition cameraPosition, boolean isAnimate) {
        if (cameraPosition != null) {
            if (isAnimate) {
                aMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } else {
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if(methodChannel != null) {
            final Map<String, Object> arguments = new HashMap<>(2);
            arguments.put("position", AMapConvert.toJson(cameraPosition));
            arguments.put("isFinish", false);
            methodChannel.invokeMethod(AMAP_ON_CAMERA_CHANGE, arguments);
        }


    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        final Map<String, Object> arguments = new HashMap<>(2);
        arguments.put("position", AMapConvert.toJson(cameraPosition));
        arguments.put("isFinish", true);
        methodChannel.invokeMethod(AMAP_ON_CAMERA_CHANGE, arguments);

    }

    @Override
    public void onMapLoaded() {
        if(methodChannel != null) {
            final Map<String, Object> arguments = new HashMap<>(2);
            methodChannel.invokeMethod(AMAP_ON_MAP_LOAD, arguments);
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return true;
    }
}
