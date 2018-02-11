package pettr.lily.pettr

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


/**
 * Created by Lily Hoskin on 11/02/2018.
 */
interface PettrAPI {
    @GET("cat")
    fun getCats(@Query("location") location: String): Call<List<Cat>>
}