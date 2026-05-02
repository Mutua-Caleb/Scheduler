package com.scheduler.calls.sync

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface SyncApi {

    @GET("health")
    suspend fun health(): HealthDto

    @POST("api/calls/sync")
    suspend fun sync(@Body request: SyncRequestDto): SyncResponseDto
}
