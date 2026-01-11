package de.kolping.cockpit.api

import de.kolping.cockpit.models.GradeOverview
import de.kolping.cockpit.models.Student
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * GraphQL client for Kolping Study API
 * Ported from Python graphql_client.py
 * 
 * Handles Bearer token authentication and provides typed query methods.
 * REQUIRES authentication - token from cms.kolping-hochschule.de login.
 */
class GraphQLClient(
    private val bearerToken: String? = null,
    private val endpoint: String = "https://app-kolping-prod-gateway.azurewebsites.net/graphql"
) {
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        
        defaultRequest {
            contentType(ContentType.Application.Json)
            header("Accept", "*/*")
            header("Origin", "https://cms.kolping-hochschule.de")
            header("Referer", "https://cms.kolping-hochschule.de/")
            
            bearerToken?.let {
                header("Authorization", "Bearer $it")
            }
        }
    }
    
    val isAuthenticated: Boolean
        get() = bearerToken != null
    
    companion object {
        // GraphQL queries verified from captured API responses
        private const val MY_STUDENT_DATA_QUERY = """
            query gtMyStudentData {
                myStudentData {
                    studentId
                    geschlechtTnid
                    titel
                    akademischerGradTnid
                    vorname
                    nachname
                    geburtsdatum
                    geburtsort
                    geburtslandTnid
                    staatsangehoerigkeitTnid
                    createdAt
                    wohnlandTnid
                    telefonnummer
                    emailPrivat
                    strasse
                    hausnummer
                    plz
                    wohnort
                    benutzername
                    emailKh
                    notizen
                    bemerkung
                    akademischerGrad
                    geburtsland
                    staatsangehoerigkeit
                    wohnland
                }
            }
        """
        
        private const val MY_GRADE_OVERVIEW_QUERY = """
            query getMyStudentGradeOverview {
                myStudentGradeOverview {
                    modules {
                        modulId
                        semester
                        modulbezeichnung
                        eCTS
                        pruefungsId
                        pruefungsform
                        grade
                        points
                        note
                        color
                        examStatus
                        eCTSString
                    }
                    grade
                    eCTS
                    student {
                        studentId
                        geschlechtTnid
                        titel
                        akademischerGradTnid
                        vorname
                        nachname
                        geburtsdatum
                        geburtsort
                        geburtslandTnid
                        staatsangehoerigkeitTnid
                        createdAt
                        wohnlandTnid
                        telefonnummer
                        emailPrivat
                        strasse
                        hausnummer
                        plz
                        wohnort
                        benutzername
                        emailKh
                        notizen
                        bemerkung
                    }
                    currentSemester
                }
            }
        """
    }
    
    @Serializable
    private data class GraphQLRequest(
        val query: String,
        val variables: Map<String, String>? = null,
        val operationName: String? = null
    )
    
    @Serializable
    private data class GraphQLError(
        val message: String,
        val locations: List<Location>? = null,
        val path: List<String>? = null
    ) {
        @Serializable
        data class Location(val line: Int, val column: Int)
    }
    
    @Serializable
    private data class GraphQLResponse<T>(
        val data: T? = null,
        val errors: List<GraphQLError>? = null
    )
    
    /**
     * Execute a raw GraphQL query
     */
    private suspend inline fun <reified T> execute(
        query: String,
        variables: Map<String, String>? = null,
        operationName: String? = null
    ): Result<T> {
        return try {
            val response = client.post(endpoint) {
                setBody(GraphQLRequest(query, variables, operationName))
            }.body<GraphQLResponse<T>>()
            
            when {
                response.errors != null -> {
                    val errorMessage = response.errors.joinToString("\n") { it.message }
                    Result.failure(Exception("GraphQL errors: $errorMessage"))
                }
                response.data != null -> Result.success(response.data)
                else -> Result.failure(Exception("No data returned from GraphQL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current student's personal data
     */
    suspend fun getMyStudentData(): Result<StudentDataResponse> {
        return execute(MY_STUDENT_DATA_QUERY)
    }
    
    /**
     * Get current student's complete grade overview
     */
    suspend fun getMyGradeOverview(): Result<GradeOverviewResponse> {
        return execute(MY_GRADE_OVERVIEW_QUERY)
    }
    
    /**
     * Test connection to GraphQL endpoint
     */
    suspend fun testConnection(): Result<Boolean> {
        return try {
            val result = execute<Map<String, String>>(
                query = "{ __typename }"
            )
            if (result.isSuccess) {
                Result.success(true)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Connection test failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        client.close()
    }
    
    @Serializable
    data class StudentDataResponse(
        val myStudentData: Student
    )
    
    @Serializable
    data class GradeOverviewResponse(
        val myStudentGradeOverview: GradeOverview
    )
}
