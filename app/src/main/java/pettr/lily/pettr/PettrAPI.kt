package pettr.lily.pettr

import android.util.Log
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type


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

internal class EmptyArrayConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(type: Type?, annotations: Array<out Annotation>?, retrofit: Retrofit?): Converter<ResponseBody, *>? {
        val delegate = retrofit?.nextResponseBodyConverter<ResponseBody>(this, type, annotations)

        return object : Converter<ResponseBody, Any> {
            override fun convert(body: ResponseBody?): Any {
                // No cats yet, empty array which would crash Retrofit
                //If content length is 2 we have the empty array
                if(body?.contentLength()  != null && body.contentLength() == 2L) {
                    return emptyList<Cat>()
                }

                return delegate!!.convert(body)
            }
        }

    }

   /* fun responseBody(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *> {
        val delegate = retrofit.nextResponseBodyConverter<ResponseBody>(this, type, annotations)
        retrofit.nextRequestBodyConverter<>()
        return object : Converter {
            fun convert(body: ResponseBody) {
                if (body.contentLength() == 0L) return null else return delegate.convert(body)
            }
        }
    }*/
}