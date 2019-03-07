package com.max.flutteramap;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
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
        AMap.OnMarkerClickListener, AMap.OnMyLocationChangeListener, AMapLocationListener {

    public static final String CHANNEL = "com.flutter.amap";
    public static final String AMAP_ON_MAP_LOAD = "amap_on_map_load";
    public static final String AMAP_ON_CAMERA_CHANGE = "amap_on_camera_change";
    public static final String AMAP_CHANGE_CAMERA = "amap_change_camera";
    public static final String AMAP_ADD_MARKER = "amap_add_marker";
    public static final String AMAP_UPDATE_MARKER = "amap_update_marker";
    public static final String AMAP_UPDATE_LOCATION = "amap_update_location";
    public static final String AMAP_KEY = "amap_key";


    private final Context context;
    private final AtomicInteger atomicInteger;
    private final PluginRegistry.Registrar registrar;
    private final MethodChannel methodChannel;

    private MapView mapView;
    private AMap aMap;
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;


    private boolean disposed = false;
    private boolean isLocation;
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
        // 定位
        aMapLocationClient = new AMapLocationClient(context);
        aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        aMapLocationClientOption.setOnceLocationLatest(true);
        aMapLocationClientOption.setInterval(3000);
        aMapLocationClientOption.setNeedAddress(true);
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.startLocation();


        mapView.onCreate(null);
        aMap = mapView.getMap();
        aMap.setMyLocationEnabled(true);


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
        aMapLocationClient.setLocationListener(this);


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
        if (disposed || activity.hashCode() != registrarActivityHashCode || aMapLocationClient == null) {
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
        if (disposed || activity.hashCode() != registrarActivityHashCode || aMapLocationClient == null) {
            return;
        }
        aMapLocationClient.stopLocation();


    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

        if (disposed || activity.hashCode() != registrarActivityHashCode || aMapLocationClient == null) {
            return;
        }
        mapView.onSaveInstanceState(bundle);

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (disposed || activity.hashCode() != registrarActivityHashCode || aMapLocationClient == null) {
            return;
        }
        mapView.onDestroy();
        aMapLocationClient.onDestroy();

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
            case AMAP_UPDATE_LOCATION:
                break;

            case "startup":
                //启动
                result.success(this.startup((Map) methodCall.arguments));
                break;
            case "shutdown":
                //关闭
                result.success(this.shutdown());
                break;
            case "getLocation":
                boolean needsAddress = (boolean) methodCall.arguments;
                this.getLocation(needsAddress, result);
                break;
            case "startLocation":
                //启动定位,如果还没有启动，那么返回false
                result.success(this.startLocation(this));
                break;
            case "updateOption":
                result.success(this.updateOption((Map) methodCall.arguments));
                break;
            case "stopLocation":
                //停止定位
                result.success(this.stopLocation());
                break;
            default:
                result.notImplemented();

        }


    }


    private boolean getLocation(boolean needsAddress, final MethodChannel.Result result) {
        synchronized (this) {

            if (aMapLocationClient == null) return false;

            if (needsAddress != aMapLocationClientOption.isNeedAddress()) {
                aMapLocationClientOption.setNeedAddress(needsAddress);
                aMapLocationClient.setLocationOption(aMapLocationClientOption);
            }

            aMapLocationClientOption.setOnceLocation(true);

            final AMapLocationListener listener = new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation aMapLocation) {
                    //恢复原来的值
                    aMapLocationClientOption.setOnceLocation(onceLocation);
                    result.success(resultToMap(aMapLocation));
                    stopLocation();
                }
            };

            startLocation(listener);

            return true;
        }
    }


    private static final String TAG = "AmapLocationPugin";

    private Map resultToMap(AMapLocation a) {

        Map map = new HashMap();

        if (a != null) {

            if (a.getErrorCode() != 0) {
                //错误信息
                map.put("description", a.getErrorInfo());
                map.put("success", false);
            } else {
                map.put("success", true);
                map.put("accuracy", a.getAccuracy());
                map.put("altitude", a.getAltitude());
                map.put("speed", a.getSpeed());
                map.put("timestamp", (double) a.getTime() / 1000);
                map.put("latitude", a.getLatitude());
                map.put("longitude", a.getLongitude());
                map.put("locationType", a.getLocationType());
                map.put("provider", a.getProvider());

                map.put("formattedAddress", a.getAddress());
                map.put("country", a.getCountry());
                map.put("province", a.getProvince());
                map.put("city", a.getCity());
                map.put("district", a.getDistrict());
                map.put("citycode", a.getCityCode());
                map.put("adcode", a.getAdCode());
                map.put("street", a.getStreet());
                map.put("number", a.getStreetNum());
                map.put("POIName", a.getPoiName());
                map.put("AOIName", a.getAoiName());
            }

            map.put("code", a.getErrorCode());
            Log.d(TAG, "定位获取结果:" + a.getLatitude() + " code：" + a.getErrorCode() + " 省:" + a.getProvince());
        }

        return map;
    }

    private boolean stopLocation() {
        synchronized (this) {
            if (aMapLocationClient == null) {
                return false;
            }
            aMapLocationClient.stopLocation();
            isLocation = false;
            return true;
        }

    }

    private boolean shutdown() {
        synchronized (this) {
            if (aMapLocationClient != null) {
                aMapLocationClient.stopLocation();
                aMapLocationClient = null;
                aMapLocationClientOption = null;
                return true;
            }
            return false;
        }


    }

    private boolean startLocation(AMapLocationListener listener) {
        synchronized (this) {
            if (aMapLocationClient == null) {
                return false;
            }

            if (listener == this) {
                //持续定位

            } else {
                //单次定位

            }

            aMapLocationClient.setLocationListener(listener);
            aMapLocationClient.startLocation();
            isLocation = true;
            return true;
        }

    }

    private boolean startup(Map arguments) {
        synchronized (this) {

            if (aMapLocationClient == null) {
                //初始化client
                aMapLocationClient = new AMapLocationClient(context);
                //设置定位参数
                AMapLocationClientOption option = new AMapLocationClientOption();
                parseOptions(option, arguments);
                aMapLocationClient.setLocationOption(option);

                //将option保存一下
                this.aMapLocationClientOption = option;

                return true;
            }

            return false;
        }
    }

    private boolean updateOption(Map arguments) {
        synchronized (this) {
            if (aMapLocationClient == null) return false;

            parseOptions(aMapLocationClientOption, arguments);
            aMapLocationClient.setLocationOption(aMapLocationClientOption);

            return true;
        }
    }

    /**
     * this.locationMode : AMapLocationMode.Hight_Accuracy,
     * this.gpsFirst:false,
     * this.httpTimeOut:10000,             //30有点长，特殊情况才需要这么长，改成10
     * this.interval:2000,
     * this.needsAddress : true,
     * this.onceLocation : false,
     * this.onceLocationLatest : false,
     * this.locationProtocal : AMapLocationProtocol.HTTP,
     * this.sensorEnable : false,
     * this.wifiScan : true,
     * this.locationCacheEnable : true,
     * <p>
     * this.allowsBackgroundLocationUpdates : false,
     * this.desiredAccuracy : CLLocationAccuracy.kCLLocationAccuracyBest,
     * this.locatingWithReGeocode : false,
     * this.locationTimeout : 10000,     //30有点长，特殊情况才需要这么长，改成10
     * this.pausesLocationUpdatesAutomatically : false,
     * this.reGeocodeTimeout : 5000,
     * <p>
     * <p>
     * this.geoLanguage : GeoLanguage.DEFAULT,
     *
     * @param arguments
     * @return
     */
    private boolean onceLocation;

    private void parseOptions(AMapLocationClientOption option, Map arguments) {
        //  AMapLocationClientOption option = new AMapLocationClientOption();
        onceLocation = (Boolean) arguments.get("onceLocation");
        option.setLocationMode(AMapLocationClientOption.AMapLocationMode.valueOf((String) arguments.get("locationMode")));//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        option.setGpsFirst((Boolean) arguments.get("gpsFirst"));//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        option.setHttpTimeOut((Integer) arguments.get("httpTimeOut"));//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        option.setInterval((Integer) arguments.get("interval"));//可选，设置定位间隔。默认为2秒
        option.setNeedAddress((Boolean) arguments.get("needsAddress"));//可选，设置是否返回逆地理地址信息。默认是true
        option.setOnceLocation(onceLocation);//可选，设置是否单次定位。默认是false
        option.setOnceLocationLatest((Boolean) arguments.get("onceLocationLatest"));//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.valueOf((String) arguments.get("locationProtocal")));//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        option.setSensorEnable((Boolean) arguments.get("sensorEnable"));//可选，设置是否使用传感器。默认是false
        option.setWifiScan((Boolean) arguments.get("wifiScan")); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        option.setLocationCacheEnable((Boolean) arguments.get("locationCacheEnable")); //可选，设置是否使用缓存定位，默认为true
        option.setGeoLanguage(AMapLocationClientOption.GeoLanguage.valueOf((String) arguments.get("geoLanguage")));//可选，设置逆地理信息的语言，默认值为默认语言（根据所在地区选择语言）

    }


    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {

        synchronized (this) {
            if (methodChannel == null) return;
            Map<String, Object> data = new HashMap<>();
            methodChannel.invokeMethod("updateLocation", resultToMap(aMapLocation));
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
        return true;
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

//    @Override
//    public void onLocationChanged(AMapLocation aMapLocation) {
//        if (aMapLocation != null) {
//            if (aMapLocation.getErrorCode() == 0) {
//                //可在其中解析amapLocation获取相应内容。
//                //amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
//                //amapLocation.getLatitude();//获取纬度
//                //amapLocation.getLongitude();//获取经度
//                //amapLocation.getAccuracy();//获取精度信息
//                //amapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
//                //amapLocation.getCountry();//国家信息
//                //amapLocation.getProvince();//省信息
//                //amapLocation.getCity();//城市信息
//                //amapLocation.getDistrict();//城区信息
//                //amapLocation.getStreet();//街道信息
//                //amapLocation.getStreetNum();//街道门牌号信息
//                //amapLocation.getCityCode();//城市编码
//                //amapLocation.getAdCode();//地区编码
//                //amapLocation.getAoiName();//获取当前定位点的AOI信息
//                //amapLocation.getBuildingId();//获取当前室内定位的建筑物Id
//                //amapLocation.getFloor();//获取当前室内定位的楼层
//                //amapLocation.getGpsAccuracyStatus();//获取GPS的当前状态
//                ////获取定位时间
//                //SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                //Date date = new Date(amapLocation.getTime());
//                //df.format(date);
//
//            } else {
//                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
//                Log.e("AmapError", "location Error, ErrCode:"
//                        + aMapLocation.getErrorCode() + ", errInfo:"
//                        + aMapLocation.getErrorInfo());
//            }
//        }
//
//
//    }
}
