package br.com.hosb.glpi.data.remote

import br.com.hosb.glpi.domain.model.CreateFollowupPayload
import br.com.hosb.glpi.domain.model.CreateTicketPayload
import br.com.hosb.glpi.domain.model.GlpiTicketDto
import br.com.hosb.glpi.domain.model.InitSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Mapeamento oficial dos endpoints da API REST do GLPI (https://glpi.hosb.com.br/apirest.php/).
 */
interface GlpiApiService {

    @GET("initSession")
    suspend fun initSessionWithBasicAuth(
        @Header("Authorization") basicAuthHeader: String,
        @Header("App-Token") appToken: String
    ): Response<InitSessionResponse>

    @GET("initSession")
    suspend fun initSessionWithUserToken(
        @Header("Authorization") userTokenHeader: String,
        @Header("App-Token") appToken: String
    ): Response<InitSessionResponse>

    @GET("killSession")
    suspend fun killSession(): Response<Unit>

    @GET("Ticket")
    suspend fun getTickets(
        @Query("range") range: String = "0-49",
        @Query("sort") sortField: Int = 19, // Data de última atualização
        @Query("order") order: String = "DESC",
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<List<GlpiTicketDto>>

    @GET("Ticket/{id}")
    suspend fun getTicketById(
        @Path("id") id: Int,
        @Query("expand_dropdowns") expandDropdowns: Boolean = true
    ): Response<GlpiTicketDto>

    @POST("Ticket")
    suspend fun createTicket(
        @Body payload: CreateTicketPayload
    ): Response<Map<String, Any>>

    @POST("Ticket/{id}/ITILFollowup")
    suspend fun addFollowup(
        @Path("id") ticketId: Int,
        @Body payload: CreateFollowupPayload
    ): Response<Map<String, Any>>
}
