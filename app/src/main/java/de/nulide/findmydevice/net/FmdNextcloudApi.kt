package de.nulide.findmydevice.net

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Retrofit interface for the Nextcloud `fmd` app API.
 * Base path is set by NextcloudRetrofitApiBuilder to `/index.php/apps/fmd/`.
 * All calls go through the Nextcloud Files app network stack (SSO).
 */
interface FmdNextcloudApi {

    @GET("api/v1/devices")
    fun listDevices(): Call<JsonObject>

    @POST("api/v1/devices")
    fun registerDevice(@Body body: JsonObject): Call<JsonObject>

    @GET("api/v1/devices/{deviceId}")
    fun getDevice(@Path("deviceId") deviceId: String): Call<JsonObject>

    @DELETE("api/v1/devices/{deviceId}")
    fun deleteDevice(@Path("deviceId") deviceId: String): Call<JsonObject>

    @PUT("api/v1/devices/{deviceId}/push")
    fun updatePush(
        @Path("deviceId") deviceId: String,
        @Body body: JsonObject,
    ): Call<JsonObject>

    @POST("api/v1/devices/{deviceId}/data/{type}")
    fun postData(
        @Path("deviceId") deviceId: String,
        @Path("type") type: String,
        @Body body: JsonObject,
    ): Call<JsonObject>

    @GET("api/v1/devices/{deviceId}/data/{type}")
    fun getData(
        @Path("deviceId") deviceId: String,
        @Path("type") type: String,
    ): Call<JsonObject>

    @GET("api/v1/devices/{deviceId}/commands/pending")
    fun pendingCommands(@Path("deviceId") deviceId: String): Call<JsonObject>

    @POST("api/v1/devices/{deviceId}/commands/{commandId}/ack")
    fun ackCommand(
        @Path("deviceId") deviceId: String,
        @Path("commandId") commandId: Long,
    ): Call<JsonObject>
}
