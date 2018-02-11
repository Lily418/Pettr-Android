package pettr.lily.pettr

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

import pettr.lily.pettr.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class MainActivity : Activity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var cats : List<Cat> = emptyList()
    private var groundOverlays : MutableList<GroundOverlay> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = fragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment.getMapAsync(this)

        val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.2:3000")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        val service = retrofit.create<PettrAPI>(PettrAPI::class.java)
        service.getCats("[51.548066, -0.070590]").enqueue(object : Callback<List<Cat>> {
            override fun onFailure(call: Call<List<Cat>>?, t: Throwable?) {
                print(call.toString())
            }

            override fun onResponse(call: Call<List<Cat>>?, response: Response<List<Cat>>?) {
                response?.body()?.let {
                    cats = it
                    refreshMapPins()
                }
            }

        })
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
            val zoom = googleMap.cameraPosition.zoom
            val maxZoom = googleMap.maxZoomLevel
            val zoomScale = maxZoom / zoom
            Log.d("CameraZoomCalc", (googleMap.cameraPosition.zoom).toString())
            groundOverlays.forEach {
                val scaledSize = 20f * Math.pow(zoomScale.toDouble(), 6.25).toFloat()
                it.setDimensions(Math.min(scaledSize, 400000f))
            }
        }
        refreshMapPins()
    }
}
