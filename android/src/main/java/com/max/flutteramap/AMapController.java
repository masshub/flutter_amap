package com.max.flutteramap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;

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
        AMap.OnMarkerClickListener, AMap.OnMyLocationChangeListener, AMap.InfoWindowAdapter {

    public static final String CHANNEL = "com.flutter.amap";
    public static final String AMAP_ON_MAP_LOAD = "amap_on_map_load";
    public static final String AMAP_ON_CAMERA_CHANGE = "amap_on_camera_change";
    public static final String AMAP_CHANGE_CAMERA = "amap_change_camera";
    public static final String AMAP_ADD_MARKER = "amap_add_marker";
    public static final String AMAP_UPDATE_MARKER = "amap_update_marker";
    public static final String AMAP_UPDATE_LOCATION = "amap_update_location";
    public static final String AMAP_KEY = "amap_key";
    public static final String AMAP_INFOWINDOW = "amap_infowindow";
    public static final String AMAP_APPOINTMENT = "amap_appointment";
    public static final String AMAP_MARKER_CLICK = "amap_marker_click";


    private final Context context;
    private final AtomicInteger atomicInteger;
    private final PluginRegistry.Registrar registrar;
    private final MethodChannel methodChannel;

    private MapView mapView;
    private AMap aMap;
    private MyLocationStyle myLocationStyle;
    private UiSettings uiSettings;
    private View infoWindow;



    private boolean disposed = false;
    private final int registrarActivityHashCode;
    private final Map<String, Marker> markers;
    private Map<String,String> infoWindowClick;
    private TextView mName;
    private TextView mDistance;
    private String mId;

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
        uiSettings = aMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomControlsEnabled(false);
        aMap.setMyLocationEnabled(true);
        myLocationStyle = new MyLocationStyle();

        myLocationStyle.showMyLocation(false);
        aMap.getMapScreenMarkers().clear();
        aMap.setMyLocationStyle(myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW_NO_CENTER));



        this.markers = new HashMap<String, Marker>();

        initListener();
    }

    private void initListener() {
        aMap.setOnMapLoadedListener(this);
        aMap.setOnCameraChangeListener(this);
        //覆盖物
        aMap.setOnMarkerClickListener(this);
        // 定位
        aMap.setOnMyLocationChangeListener(this);
        // infowindow
        aMap.setInfoWindowAdapter(this);

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
        String call = methodCall.method;

        switch (call) {
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
            case AMAP_KEY:
                result.success(true);
                break;
            case AMAP_APPOINTMENT:
                // 点击预约，返回需要数据，通知flutter进行相应处理

                break;
            case AMAP_INFOWINDOW:
                // 初始化InfoWindow信息
                String name = methodCall.argument("name");
                String distance = methodCall.argument("distance");
                mId = methodCall.argument("id");
                mName.setText(name);
                mDistance.setText(distance + "KM");
                Log.d("infowindow信息",name +","+ distance + "," + mId);
                Toast.makeText(context,name +","+ distance + "," + mId,Toast.LENGTH_SHORT).show();
                marker.showInfoWindow();
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
        if (methodChannel != null) {
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
        if (methodChannel != null) {
            final Map<String, Object> arguments = new HashMap<>(2);
            methodChannel.invokeMethod(AMAP_ON_MAP_LOAD, arguments);
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (methodChannel != null) {
            Map<String,Object> map = new HashMap<>();
            map.put("onMarkerClick","yse");
            methodChannel.invokeMethod(AMAP_MARKER_CLICK,map);
        }
        Toast.makeText(context,"点击了marker",Toast.LENGTH_SHORT).show();
//        marker.showInfoWindow();
        return false;
    }

    @Override
    public void onMyLocationChange(Location location) {
        aMap.setMyLocationEnabled(true);
        aMap.setOnMyLocationChangeListener(this);
        if (methodChannel != null) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("latitude", location.getLatitude());
            map.put("longitude", location.getLongitude());
            map.put("accuracy", location.getAccuracy());
            map.put("altitude", location.getAltitude());
            map.put("speed", location.getSpeed());
            map.put("timestamp", (double) location.getTime() / 1000);

            methodChannel.invokeMethod(AMAP_UPDATE_LOCATION, map);
        }

    }

    @Override
    public View getInfoWindow(final Marker marker) {
        infoWindow = LayoutInflater.from(context).inflate(R.layout.infowindow,null);
        mName = infoWindow.findViewById(R.id.tv_name);
        mDistance = infoWindow.findViewById(R.id.tv_distance);
        final TextView appointment = infoWindow.findViewById(R.id.tv_appointment);
        ImageView close = infoWindow.findViewById(R.id.iv_close);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marker.setInfoWindowEnable(false);
                marker.setInfoWindowEnable(true);
            }
        });

        appointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                marker.setInfoWindowEnable(false);
                marker.setInfoWindowEnable(true);
                Toast.makeText(context,"预约成功",Toast.LENGTH_SHORT).show();

                if (methodChannel != null) {
                    Map<String,Object> arguments = new HashMap<>();
                    arguments.put("appointment",mId);
                    methodChannel.invokeMethod(AMAP_APPOINTMENT,arguments);

                }

            }
        });



        return infoWindow;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }


}
