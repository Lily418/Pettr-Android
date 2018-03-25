package pettr.lily.pettr

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.ExifInterface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Base64
import android.util.Log
import android.view.View
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.net.ConnectException


class MainActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {
    override fun onLocationChanged(location: Location?) {
       // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
      //  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderEnabled(provider: String?) {
       // TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onProviderDisabled(provider: String?) {
      //  TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var toolbarErrorManager : ToolbarErrorManager
    private var mMap: GoogleMap? = null
    private var mCurrentPhotoPath : String? = null
    private var cats : List<Cat> = emptyList()
    private var groundOverlays : MutableList<GroundOverlay> = mutableListOf()
    private val pettrService : PettrAPI = Retrofit.Builder()
            .baseUrl("http://192.168.1.3:3000")
            .addConverterFactory(GsonConverterFactory.create())
            .build().create<PettrAPI>(PettrAPI::class.java)

    val PERMISSION_REQUEST_CAMERA = 1
    val PERMISSION_REQUEST_LOCATION = 2
    val TAKE_PHOTO_REQUEST = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbarErrorManager = ToolbarErrorManager(toolbar_error)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        pettrService.getCats("[-0.070590, 51.548066]").enqueue(object : Callback<List<Cat>> {
            override fun onFailure(call: Call<List<Cat>>?, t: Throwable?) {

                print((getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo)

                if((getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo == null) {
                    toolbarErrorManager.updateError(ToolbarErrorManager.ToolbarError(ToolbarErrorManager.ToolbarErrorTypes.NetworkError, getString(R.string.no_network_exception)))
                    return
                }

                if(t is ConnectException) {
                    toolbarErrorManager.updateError(ToolbarErrorManager.ToolbarError(ToolbarErrorManager.ToolbarErrorTypes.NetworkError, getString(R.string.connect_exception)))
                    return
                }

                toolbarErrorManager.updateError(ToolbarErrorManager.ToolbarError(ToolbarErrorManager.ToolbarErrorTypes.NetworkError, getString(R.string.other_network_exception)))
            }

            override fun onResponse(call: Call<List<Cat>>?, response: Response<List<Cat>>?) {
                if(response == null || !response.isSuccessful) {
                    toolbarErrorManager.updateError(ToolbarErrorManager.ToolbarError(ToolbarErrorManager.ToolbarErrorTypes.ServerError, getString(R.string.other_server_error)))
                    return
                }

                response.body()?.let {
                    cats = it
                    refreshMapPins()
                }
            }

        })

        floating_action_button.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA,  Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> launchCamera()
            PERMISSION_REQUEST_LOCATION -> TODO()
        }
    }

    private fun launchCamera() {
        val values = ContentValues(1)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        val fileUri = contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intent.resolveActivity(packageManager) != null) {
            mCurrentPhotoPath = fileUri.toString()
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TAKE_PHOTO_REQUEST -> processCapturedPhoto()
        }
    }

    private fun processCapturedPhoto() {
        val cursor = contentResolver.query(Uri.parse(mCurrentPhotoPath),
                Array(1) {android.provider.MediaStore.Images.ImageColumns.DATA},
                null, null, null)
        cursor.moveToFirst()
        val photoPath = cursor.getString(0)
        cursor.close()
        val file = File(photoPath)
        val exif = ExifInterface(file.absolutePath)

        var longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE).toDoubleOrNull()
        var latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE).toDoubleOrNull()
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        when(orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> print("Rotate 90")
            ExifInterface.ORIENTATION_ROTATE_180 -> print("Rotate 180")
            ExifInterface.ORIENTATION_ROTATE_270 -> print("Rotate 270")
        }

        if(longitude == null || latitude == null) {
            try {
                val location = getLocation()
                longitude = location?.longitude
                latitude = location?.latitude
            } catch(e: SecurityException) {

            }
        }

        if(longitude == null || latitude == null) {
            throw Error("Hey sorry I don't know where this cat lives")
        }


        // Optimistic adding image to map
        mMap?.addGroundOverlay(GroundOverlayOptions().position(LatLng(latitude, longitude), 250.0f, 250.0f).image(BitmapDescriptorFactory.fromPath(file.absolutePath)))
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15f))

        pettrService.putCat(encodeLocation(longitude, latitude), MultipartBody.Part.createFormData("cat", file.name, RequestBody.create(MediaType.parse("image/jpeg"), file))).enqueue(object : Callback<Any?> {
            override fun onFailure(call: Call<Any?>?, t: Throwable?) {
                print(call.toString())
            }

            override fun onResponse(call: Call<Any?>?, response: Response<Any?>?) {
                if(response?.isSuccessful == true) {
                    // Do Something
                } else {
                    print(response?.errorBody())
                }
            }

        })
    }

    private fun encodeLocation(longitude: Double, latitude: Double): String {
        return "[$longitude, $latitude]"
    }

    fun getLocation(): Location? {
        var location : Location? = null

        try {
            val locationManager = applicationContext
                    .getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // getting GPS status
            val isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)

            Log.v("isGPSEnabled", "=" + isGPSEnabled)

            // getting network status
            val isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            Log.v("isNetworkEnabled", "=" + isNetworkEnabled)

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                throw Error("No no no network in the upper field")
            } else {
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            1,
                            1f, this)
                    Log.d("Network", "Network")
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                1,
                                1f, this)
                        Log.d("GPS Enabled", "GPS Enabled")
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    }
            }

        }
        catch (e: SecurityException) {

        }
        catch (e: Exception) {
            e.printStackTrace()
        }

        return location
    }

    fun refreshMapPins() {

        // Refresh Map Pins will be called after API Response and Map is Ready but we don't know which will happen first
        // so this ensures this method only has side effects once
        if(mMap == null ||  groundOverlays.count() > 0) {
            return
        }

        cats.forEach {
            val groundOverlay = mMap?.addGroundOverlay(GroundOverlayOptions().position(it.latlng, 250.0f, 250.0f).image(BitmapDescriptorFactory.fromBitmap(it.bitmap)))
            groundOverlay?.let {
                groundOverlays.add(it)
            }

            scaleOverlays(mMap!!)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        googleMap.setOnCameraMoveListener {
            scaleOverlays(googleMap)
        }
        refreshMapPins()
    }

    fun scaleOverlays(googleMap: GoogleMap) {
        val zoom = googleMap.cameraPosition.zoom
        val maxZoom = googleMap.maxZoomLevel
        val zoomScale = maxZoom / zoom
        groundOverlays.forEach {
            val scaledSize = 20f * Math.pow(zoomScale.toDouble(), 6.25).toFloat()
            it.setDimensions(Math.min(scaledSize, 400000f))
        }
    }
}
