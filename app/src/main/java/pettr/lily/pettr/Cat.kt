package pettr.lily.pettr

import com.google.android.gms.maps.model.LatLng

/**
 * Created by Lily Hoskin on 11/02/2018.
 */

class Cat {
    var _id : String? = null
    var location : PettrPoint? = null

    val latlng : LatLng
        get() = LatLng(location!!.coordinates[0], location!!.coordinates[1])

    data class PettrPoint(val coordinates : List<Double>)
}