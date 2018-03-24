package pettr.lily.pettr

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


/**
 * Created by Lily Hoskin on 11/02/2018.
 */
interface PettrAPI {
    @GET("cat")
    fun getCats(@Query("location") location: String): Call<List<Cat>>

    @Multipart
    @PUT("cat")
    fun putCat(@Query("location") location: String, @Part cat: MultipartBody.Part): Call<Any?>
}