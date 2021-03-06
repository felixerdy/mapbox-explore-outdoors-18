package felix.atlas

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.mapbox.mapboxsdk.Mapbox

import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.github.florent37.kotlin.pleaseanimate.please
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.style.sources.Source
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.nio.charset.Charset
import java.time.LocalTime
import kotlin.math.round
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity(), LocationEngineListener, PermissionsListener {

    private lateinit var mapView: MapView
    private lateinit var map: MapboxMap
    private lateinit var permissionsManager: PermissionsManager
    private var locationPlugin: LocationLayerPlugin? = null
    private var locationEngine: LocationEngine? = null
    private lateinit var originLocation: Location

    private var originCoord : LatLng? = null

    private lateinit var originPosition : Point
    private lateinit var destinationPosition : Point
    private var currentRoute : DirectionsRoute? = null
    private var navigationMapRoute : NavigationMapRoute? = null

    private var movingCriteria : String = DirectionsCriteria.PROFILE_CYCLING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { mapboxMap ->
            this.map = mapboxMap
            enableLocationPlugin()

            val geoJson : String = loadGeoJsonFromAsset("features.geojson")!!
            val featureCollection : FeatureCollection  = FeatureCollection.fromJson(geoJson)
            val source : Source = GeoJsonSource("places", featureCollection)

            mapboxMap.addSource(source)

            val bbqIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_bbq)
            mapboxMap.addImage("bbq", drawableToBitmap(bbqIcon!!))

            val cafeIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_cafe)
            mapboxMap.addImage("cafe", drawableToBitmap(cafeIcon!!))

            val mountainIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_mountain)
            mapboxMap.addImage("hiking", drawableToBitmap(mountainIcon!!))
            mapboxMap.addImage("climbing", drawableToBitmap(mountainIcon))

            val swimIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_swim)
            mapboxMap.addImage("swim", drawableToBitmap(swimIcon!!))

            val snackIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_fast_food)
            mapboxMap.addImage("snack", drawableToBitmap(snackIcon!!))

            val museumIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_museum)
            mapboxMap.addImage("museum", drawableToBitmap(museumIcon!!))

            val attractionIcon : Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_attraktion)
            mapboxMap.addImage("view", drawableToBitmap(attractionIcon!!))

            val myLayer = SymbolLayer("places-layer", "places")
                    .withProperties(PropertyFactory.iconImage("{type}"))
                    .withProperties(PropertyFactory.iconAllowOverlap(true))
                    .withProperties(PropertyFactory.iconOffset(arrayOf(0f, -12f)))
                    .withProperties(PropertyFactory.iconSize(1.5f))

            mapboxMap.addLayer(myLayer)

            mapboxMap.addOnMapClickListener { point ->

                val screenPoint : PointF = mapboxMap.projection.toScreenLocation(point)
                val features : List<Feature> = mapboxMap.queryRenderedFeatures(screenPoint, "places-layer")
                if (!features.isEmpty()) {
                    val selectedFeature: Feature = features[0]
                    val title: String = selectedFeature.getStringProperty("name")
                    val type: String = selectedFeature.getStringProperty("type")

                    var icon = ContextCompat.getDrawable(this, R.drawable.ic_bbq)
                    when (type) {
                        "bbq" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_bbq)
                        "cafe" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_cafe)
                        "hiking" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_mountain)
                        "climbing" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_mountain)
                        "swim" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_swim)
                        "snack" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_fast_food)
                        "museum" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_museum)
                        "view" -> icon = ContextCompat.getDrawable(this, R.drawable.ic_attraktion)
                    }

                    findViewById<CardView>(R.id.cardView).visibility = View.VISIBLE;
                    findViewById<TextView>(R.id.textView).text = title
                    findViewById<ImageView>(R.id.imageView).setImageDrawable(icon)

                    please {
                        animate(findViewById<CardView>(R.id.cardView)) toBe {
                            bottomOfItsParent(marginDp = 48f)
                            visible()
                        }

                        animate(findViewById<SeekBar>(R.id.seekBar)) toBe {
                            rightOfItsParent(marginDp = -8f)
                            visible()
                        }
                    }.start()

                    findViewById<TextView>(R.id.timeTextView).visibility = View.INVISIBLE

                    if(::originLocation.isInitialized) {
                        destinationPosition = Point.fromJson(selectedFeature.geometry()!!.toJson())

                        originCoord = LatLng(originLocation.latitude, originLocation.longitude)
                        originPosition = Point.fromLngLat(originCoord!!.longitude, originCoord!!.latitude)
                        getRoute(originPosition!!, destinationPosition!!)
                    }

                } else {
                    please {
                        animate(findViewById<CardView>(R.id.cardView)) toBe {
                            bottomOfItsParent(marginDp = -150f)
                            invisible()
                        }
                        animate(findViewById<SeekBar>(R.id.seekBar)) toBe {
                            rightOfItsParent(marginDp = -300f)
                            invisible()
                        }
                    }.start()
                    if (navigationMapRoute != null) {
                        navigationMapRoute!!.removeRoute()
                    }
                }
            }
        }

        var seekbar = findViewById<SeekBar>(R.id.seekBar)
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(p0: SeekBar?) {

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                when(progress) {
                    0 -> {
                        movingCriteria = DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
                        seekbar.thumb = applicationContext.getDrawable(R.drawable.ic_car)
                    }
                    1 -> {
                        movingCriteria = DirectionsCriteria.PROFILE_CYCLING
                        seekbar.thumb = applicationContext.getDrawable(R.drawable.ic_bicycle)
                    }
                    2 -> {
                        movingCriteria = DirectionsCriteria.PROFILE_WALKING
                        seekbar.thumb = applicationContext.getDrawable(R.drawable.ic_walk)
                    }
                }
                if(::originPosition.isInitialized && ::destinationPosition.isInitialized) {
                    getRoute(originPosition, destinationPosition)
                }

            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (locationEngine != null) {
            locationEngine?.requestLocationUpdates()
        }
        if (locationPlugin != null) {
            locationPlugin?.onStart()
        }
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (locationEngine != null) {
            locationEngine?.removeLocationUpdates()
        }
        if (locationPlugin != null) {
            locationPlugin?.onStop()
        }
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        if (locationEngine != null) {
            locationEngine?.deactivate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView.onSaveInstanceState(outState)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun enableLocationPlugin() {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine()

            locationPlugin = LocationLayerPlugin(mapView, map, locationEngine)
            locationPlugin?.renderMode = RenderMode.COMPASS

        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager.requestLocationPermissions(this)
        }

    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationEngine() {
        val locationEngineProvider = LocationEngineProvider(this)
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable()
        locationEngine?.priority = LocationEnginePriority.HIGH_ACCURACY
        locationEngine?.activate()

        val lastLocation = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    private fun setCameraPosition(location: Location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude), 13.0))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onExplanationNeeded(permissionsToExplain: List<String>) {

    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationPlugin()
        } else {
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onConnected() {
        locationEngine?.requestLocationUpdates()
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            originLocation = location
            setCameraPosition(location)
            locationEngine?.removeLocationEngineListener(this)
        }
    }

    private fun getRoute(origin : Point, destination : Point) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .destination(destination)
                .profile(movingCriteria)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    @SuppressLint("SetTextI18n")
                    override fun onResponse(call: retrofit2.Call<DirectionsResponse>?, response: Response<DirectionsResponse>?) {
                        Timber.d("Response code: %s", response?.code())
                        if (response?.body() == null) {
                            Timber.e("No routes found, make sure you set the right user and access token.")
                            return
                        } else if (response.body()!!.routes().size < 1) {
                            Timber.e("No routes found")
                            return
                        }

                        currentRoute = response.body()!!.routes().get(0)

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute!!.removeRoute()
                        } else {
                            navigationMapRoute = NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute)
                        }
                        navigationMapRoute?.addRoute(currentRoute)

                        val distance = round(currentRoute?.distance()!!.div(10)).div(100)

                        findViewById<TextView>(R.id.distanceTextView).text = "${distance}km"

                        val travelTimeSeconds = currentRoute?.duration()!!.toLong()
                        val timeOfDay : LocalTime = LocalTime.ofSecondOfDay(travelTimeSeconds)
                        findViewById<TextView>(R.id.timeTextView).text = "${timeOfDay.hour}h ${timeOfDay.minute}min"

                        findViewById<TextView>(R.id.timeTextView).visibility = View.VISIBLE
                        findViewById<ImageButton>(R.id.imageButton).visibility = View.VISIBLE

                        findViewById<ImageButton>(R.id.imageButton).setOnClickListener { _ ->
                            val options : NavigationLauncherOptions = NavigationLauncherOptions.builder()
                                    .directionsRoute(currentRoute)
                                    .shouldSimulateRoute(true)
                                    .build()

                            // Call this method with Context from within an Activity
                            NavigationLauncher.startNavigation(this@MainActivity, options)
                        }
                    }

                    override fun onFailure(call: retrofit2.Call<DirectionsResponse>?, t: Throwable?) {
                        Timber.e("Error: %s", t?.message)
                    }
                })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadGeoJsonFromAsset(filename: String): String? {
        try {
            // Load GeoJSON file
            val `is` = assets.open(filename)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            return String(buffer, Charset.forName("UTF-8"))

        } catch (exception: Exception) {
            Timber.e("Exception Loading GeoJSON: %s", exception.toString())
            exception.printStackTrace()
            return null
        }
    }

    private fun  drawableToBitmap (drawable : Drawable ) : Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap  = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
}
