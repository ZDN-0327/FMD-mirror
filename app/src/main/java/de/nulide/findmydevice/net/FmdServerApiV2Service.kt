package de.nulide.findmydevice.net

import de.nulide.findmydevice.net.model.DataRequestResponse
import de.nulide.findmydevice.net.model.LoginRequest
import de.nulide.findmydevice.net.model.LoginResponse
import de.nulide.findmydevice.net.model.MessagesResponse
import de.nulide.findmydevice.net.model.PasswordRequest
import de.nulide.findmydevice.net.model.PasswordResponse
import de.nulide.findmydevice.net.model.PushUrlRequestResponse
import de.nulide.findmydevice.net.model.RegisterRequest
import de.nulide.findmydevice.net.model.RegisterResponse
import de.nulide.findmydevice.net.model.SaltResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FmdServerApiV2Service {

    /* ----- Account management ----- */

    @GET("account/{name}/salt")
    fun getSalt(@Path("name") username: String): Call<SaltResponse>

    @POST("account/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("account/register")
    fun register(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("account/logout")
    fun logout(): Call<Unit>

    @DELETE("account")
    fun deleteAccount(): Call<Unit>

    /* ----- Account settings ----- */

    @GET("account/push_url")
    fun getPushUrl(): Call<PushUrlRequestResponse>

    @POST("account/push_url")
    fun postPushUrl(@Body request: PushUrlRequestResponse): Call<Unit>

    @POST("account/password")
    fun postPassword(@Body request: PasswordRequest): Call<PasswordResponse>

    /* ----- Data ----- */

    @GET("data/{type}")
    fun getData(@Path("type") dataType: String): Call<DataRequestResponse>

    @POST("data/{type}")
    fun postData(
        @Path("type") dataType: String,
        @Body request: DataRequestResponse,
    ): Call<Unit>

    @DELETE("data/{type}/{id}")
    fun deleteData(
        @Path("type") dataType: String,
        @Path("id") id: String,
    ): Call<Unit>

    /* ----- Server messages ----- */

    @GET("messages")
    fun getMessages(): Call<MessagesResponse>

    @DELETE("messages/{id}")
    fun deleteSingleMessage(
        @Path("id") uuid: String,
    ): Call<Unit>
}
