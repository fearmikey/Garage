package com.fearmikey.garage.data.repository

import com.fearmikey.garage.data.remote.RecallApi
import com.fearmikey.garage.data.remote.dto.RecallDto
import com.fearmikey.garage.data.remote.dto.RecallResponse
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class RecallRepositoryTest {

    private fun repoThatThrows(e: Exception) = RecallRepository(
        object : RecallApi {
            override suspend fun getRecalls(make: String, model: String, modelYear: Int): RecallResponse = throw e
        },
    )

    private fun httpError(code: Int) = HttpException(
        Response.error<RecallResponse>(code, """{"Count":0,"Message":"Results returned successfully","results":[]}""".toResponseBody()),
    )

    @Test
    fun `successful response maps recalls`() = runBlocking {
        val repo = RecallRepository(
            object : RecallApi {
                override suspend fun getRecalls(make: String, model: String, modelYear: Int) =
                    RecallResponse(count = 1, results = listOf(RecallDto(campaignNumber = "20V314000")))
            },
        )
        val result = repo.getRecalls(2018, "Honda", "Accord") as RecallLookupResult.Success
        assertEquals(listOf("20V314000"), result.recalls.map { it.campaignNumber })
    }

    @Test
    fun `HTTP 400 is NHTSA's no-results answer and maps to an empty success`() = runBlocking {
        val result = repoThatThrows(httpError(400)).getRecalls(2021, "KTM", "890 Duke")
        assertEquals(RecallLookupResult.Success(emptyList()), result)
    }

    @Test
    fun `other HTTP errors are reported with a readable message`() = runBlocking {
        val result = repoThatThrows(httpError(503)).getRecalls(2021, "KTM", "890 Duke")
        assertTrue(result is RecallLookupResult.Error)
        assertTrue((result as RecallLookupResult.Error).message.contains("HTTP 503"))
    }

    @Test
    fun `network failures are reported as connectivity errors`() = runBlocking {
        val result = repoThatThrows(IOException("timeout")).getRecalls(2021, "KTM", "890 Duke")
        assertTrue((result as RecallLookupResult.Error).message.contains("internet connection"))
    }
}
