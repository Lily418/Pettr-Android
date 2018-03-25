package pettr.lily.pettr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.android.gms.maps.model.LatLng

/**
 * Created by Lily Hoskin on 11/02/2018.
 */

class Cat {
    var _id : String? = null
    var location : PettrPoint? = null
    var photo : String? = null
    var _bitmap: Bitmap? = null

    // API returns GeoJSON which uses the order [longitude, latitude]
    val latlng : LatLng
        get() = LatLng(location!!.coordinates[1], location!!.coordinates[0])

    val bitmap : Bitmap
        get() {

            if(_bitmap == null && photo == null) {
                throw Error("No Bitmap data or Base64 Encoded Bitmap data for cat")
            }

            if(_bitmap == null) {
                val byteArray = Base64.decode(photo, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                _bitmap = scaleBitmap(bitmap)
            }

            return _bitmap!!
        }

    fun scaleBitmap(bitmap : Bitmap): Bitmap {
        val scale = 250.0 / bitmap.width
        return Bitmap.createScaledBitmap(bitmap, 250, (bitmap.height.toDouble() * scale).toInt(), true)
    }

    data class PettrPoint(val coordinates : List<Double>)
}