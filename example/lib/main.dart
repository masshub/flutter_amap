import 'package:flutter/material.dart';
import 'package:flutter_amap/flutter_amap.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'AMap Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'AMap Demo'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);

  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  AMapController mapController;

  Marker currentMarker;

  bool _isAnimation = true;

  AMapLocation aMapLocation;




  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: Text(widget.title),
        ),
        body: AMap(
          onMapCreated: onMapCreated,
        )
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("移动到北京"),
//                onPressed: () {
//                  if (mapController != null) {
//                    mapController.changeCamera(
//                        CameraPosition(
//                            target: LatLng(39.893927, 116.405972), zoom: 10),
//                        _isAnimation);
//                  }
//                },
//              ),
//              _isAnimationButton(),
//            ],
//          ),
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("添加Marker"),
//                onPressed: () {
//                  if (mapController != null) {
//                    MarkerOptions options = MarkerOptions.defaultOptions;
//
//                    mapController.addMarker(options).then((marker) {
//                      setState(() {
//                        currentMarker = marker;
//                      });
//                    });
//                  }
//                },
//              ),
//              FlatButton(
//                child: Text("修改位置"),
//                onPressed: () {
//                  if (mapController != null) {
//                    if (currentMarker != null) {
//                      //fixme 如果默认值不一样如何更新
//                      MarkerOptions markerOptions = MarkerOptions(
//                          position: LatLng(
//                              currentMarker.options.position.latitude,
//                              currentMarker.options.position.longitude +
//                                  0.001));
//                      mapController.updateMarker(currentMarker, markerOptions);
//                    } else {
//                      print("currentMarker is null");
//                    }
//                  }
//                },
//              ),
//            ],
//          ),
//          Row(
//            children: <Widget>[
//              FlatButton(
//                child: Text("添加Polyline"),
//                onPressed: () {
//                  if (mapController != null) {
//                    MarkerOptions options = MarkerOptions.defaultOptions;
//                    mapController.addMarker(options);
//                  }
//                },
//              ),
//            ],
//          ),
//        ]

        );
  }

  Widget _isAnimationButton() {
    return FlatButton(
      child: Text('${_isAnimation ? '关闭' : '开启'} 动画'),
      onPressed: () {
        setState(() {
          _isAnimation = !_isAnimation;
        });
      },
    );
  }

  void onMapCreated(AMapController controller) async{
    print("onMapCreated");
    mapController = controller;
    aMapLocation = await AMapLocationClient.getLocation(true);
    AMapLocationClient.startLocation();
    mapController.changeCamera(CameraPosition(target: LatLng(aMapLocation.latitude, aMapLocation.longitude)), true);




    // 注册监听
    mapController.onMapLoaded.add(onMapLoaded);
    mapController.onCameraChanged.add(onCameraChanged);
    mapController.onLocationChanged.add(onLocationChanged);
    mapController.onLocationUpdated.listen((Location location) {
      print("onLocationUpdated");
    });


  }

  void onLocationChanged(Location location) {
    print("onLocationChanged " + location.toString());

  }

  void onMapLoaded(argument) {
    print("onMapLoaded");
  }

  void onCameraChanged(CameraPosition cameraPostion) {
    print("onCameraChanged " + onCameraChanged.toString());
  }
}
