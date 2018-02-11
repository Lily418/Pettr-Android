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

    val latlng : LatLng
        get() = LatLng(location!!.coordinates[0], location!!.coordinates[1])

    val bitmap : Bitmap
        get() {
            val byteArray = Base64.decode(photo, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val scale = 250.0 / bitmap.width
            return Bitmap.createScaledBitmap(bitmap, 250, (bitmap.height.toDouble() * scale).toInt(), true)
        }

    data class PettrPoint(val coordinates : List<Double>)
}