package com.max.flutteramap;

import android.graphics.Point;

import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MarkerOptions;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.view.FlutterMain;

/**
 * Created by Maker on 2019/3/6.
 * Describe:
 */
public class AMapConvert {
    private static BitmapDescriptor toBitmapDescriptor(Object object) {
        final List<?> data = toList(object);
        switch (toString(data.get(0))) {
            case "defaultMarker":
                if (data.size() == 1) {
                    return BitmapDescriptorFactory.defaultMarker();
                } else {
                    BitmapDescriptorFactory.defaultMarker(toFloat(data.get(1)));
                }

            case "fromAsset":
                if (data.size() == 2) {
                    return BitmapDescriptorFactory.fromAsset(
                            FlutterMain.getLookupKeyForAsset(toString(data.get(1)))
                    );
                } else {
                    return BitmapDescriptorFactory.fromAsset(
                            FlutterMain.getLookupKeyForAsset(toString(data.get(1)), toString(data.get(2)))
                    );

                }
            default:
                throw new IllegalArgumentException("Cannot interpret " + object + " as BitmapDescriptor");


        }

    }

    private static List<?> toList(Object object) {
        return (List<?>) object;
    }

    private static Map<?, ?> toMap(Object object) {
        return (Map<?, ?>) object;
    }

    private static String toString(Object object) {
        return (String) object;
    }

    private static boolean toBoolean(Object object) {
        return (boolean) object;
    }

    private static float toFloat(Object object) {
        return ((Number) object).floatValue();
    }

    private static double toDouble(Object object) {
        return ((Number) object).doubleValue();
    }

    private static int toInt(Object object) {
        return ((Number) object).intValue();
    }

    private static long toLong(Object object) {
        return ((Number) object).longValue();
    }

    private static short toShort(Object object) {
        return ((Number) object).shortValue();
    }

    private static int toPixels(Object object, float density) {
        return (int) toFractionalPixels(object, density);
    }

    private static float toFractionalPixels(Object object, float density) {
        return toFloat(object) * density;
    }

    private static Object toJson(LatLng latLng) {
        return Arrays.asList(latLng.latitude, latLng.longitude);
    }

    private static Point toPoint(Object object, float density) {
        final List<?> data = toList(object);
        return new Point(toPixels(data.get(0), density), toPixels(data.get(1), density));
    }

    private static LatLng toLatLng(Object object) {
        if (object == null) {
            return null;
        }
        final List<?> data = toList(object);
        return new LatLng(toDouble(data.get(0)), toDouble(data.get(1)));
    }

    private static LatLngBounds toLatLngBounds(Object object) {
        if (object == null) {
            return null;
        }
        final List<?> data = toList(object);
        return new LatLngBounds(toLatLng(data.get(0)), toLatLng(data.get(1)));
    }

    static Object toJson(CameraPosition position) {
        if (position == null) {
            return null;
        }
        final Map<String, Object> data = new HashMap<>();
        data.put("bearing", position.bearing);
        data.put("target", toJson(position.target));
        data.put("tilt", position.tilt);
        data.put("zoom", position.zoom);
        return data;
    }

    static CameraPosition toCameraPosition(Object object) {
        final Map<?, ?> data = toMap(object);
        final CameraPosition.Builder builder = CameraPosition.builder();
        builder.bearing(toFloat(data.get("bearing")));
        builder.target(toLatLng(data.get("target")));
        builder.tilt(toFloat(data.get("tilt")));
        builder.zoom(toFloat(data.get("zoom")));
        return builder.build();
    }

    static CameraUpdate toCameraUpdate(Object o, float density) {
        final List<?> data = toList(o);
        switch (toString(data.get(0))) {
            case "newCameraPosition":
                return CameraUpdateFactory.newCameraPosition(toCameraPosition(data.get(1)));
            case "newLatLng":
                return CameraUpdateFactory.newLatLng(toLatLng(data.get(1)));
            case "newLatLngBounds":
                return CameraUpdateFactory.newLatLngBounds(
                        toLatLngBounds(data.get(1)), toPixels(data.get(2), density));
            case "newLatLngZoom":
                return CameraUpdateFactory.newLatLngZoom(toLatLng(data.get(1)), toFloat(data.get(2)));
            case "scrollBy":
                return CameraUpdateFactory.scrollBy( //
                        toFractionalPixels(data.get(1), density), //
                        toFractionalPixels(data.get(2), density));
            case "zoomBy":
                if (data.size() == 2) {
                    return CameraUpdateFactory.zoomBy(toFloat(data.get(1)));
                } else {
                    return CameraUpdateFactory.zoomBy(toFloat(data.get(1)), toPoint(data.get(2), density));
                }
            case "zoomIn":
                return CameraUpdateFactory.zoomIn();
            case "zoomOut":
                return CameraUpdateFactory.zoomOut();
            case "zoomTo":
                return CameraUpdateFactory.zoomTo(toFloat(data.get(1)));
            default:
                throw new IllegalArgumentException("Cannot interpret " + o + " as CameraUpdate");
        }
    }

    /**
     * 根据flutter传递的数据生成markeroptions
     *
     * @param o
     * @return
     */
    static MarkerOptions toMarkerOptions(Object o) {
        MarkerOptions options = new MarkerOptions();
        final Map<?, ?> data = toMap(o);
        final Object alpha = data.get("alpha");
        if (alpha != null) {
            options.alpha(toFloat(alpha));
        }
        final Object anchorU = data.get("anchorU");
        final Object anchorV = data.get("anchorV");
        if (anchorU != null && anchorV != null) {
            options.anchor(toFloat(anchorU), toFloat(anchorV));
        }
        final Object draggable = data.get("draggable");
        if (draggable != null) {
            options.draggable(toBoolean(draggable));
        }
        final Object flat = data.get("flat");
        if (flat != null) {
            options.setFlat(toBoolean(flat));
        }
        final Object icon = data.get("icon");
        if (icon != null) {
            options.icon(toBitmapDescriptor(icon));
        }
        final Object position = data.get("position");
        if (position != null) {
            options.position(toLatLng(position));
        }
        final Object rotation = data.get("rotation");
        if (rotation != null) {
            options.rotateAngle(toFloat(rotation));
        }
        final Object visible = data.get("visible");
        if (visible != null) {
            options.visible(toBoolean(visible));
        }
        final Object zIndex = data.get("zIndex");
        if (zIndex != null) {
            options.zIndex(toFloat(zIndex));
        }
        return options;
    }


}
