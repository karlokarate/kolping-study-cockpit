package de.kolping.cockpit.repository

import de.kolping.cockpit.api.GraphQLClient
import de.kolping.cockpit.api.MoodleClient
import de.kolping.cockpit.models.*

/**
 * Central repository for study data
 * Coordinates between GraphQL and Moodle clients
 */
class StudyRepository(
    private val graphQLClient: GraphQLClient,
    private val moodleClient: MoodleClient
) : AutoCloseable {
    
    /**
     * Check if authenticated with both services
     */
    val isFullyAuthenticated: Boolean
        get() = graphQLClient.isAuthenticated && moodleClient.isAuthenticated
    
    /**
     * Get student personal data
     */
    suspend fun getStudentData(): Result<Student> {
        return graphQLClient.getMyStudentData()
            .mapCatching { it.myStudentData }
    }
    
    /**
     * Get grade overview with modules
     */
    suspend fun getGradeOverview(): Result<GradeOverview> {
        return graphQLClient.getMyGradeOverview()
            .mapCatching { it.myStudentGradeOverview }
    }
    
    /**
     * Get Moodle dashboard
     */
    suspend fun getMoodleDashboard(): Result<MoodleDashboard> {
        return moodleClient.getDashboard()
    }
    
    /**
     * Get list of Moodle courses
     */
    suspend fun getMoodleCourses(): Result<List<MoodleCourse>> {
        return moodleClient.getCourses()
    }
    
    /**
     * Get assignments
     */
    suspend fun getAssignments(): Result<List<MoodleAssignment>> {
        return moodleClient.getAssignments()
    }
    
    /**
     * Get upcoming deadlines
     */
    suspend fun getUpcomingDeadlines(): Result<List<MoodleEvent>> {
        return moodleClient.getUpcomingDeadlines()
    }
    
    /**
     * Test connectivity to both services
     */
    suspend fun testConnectivity(): ConnectivityStatus {
        val graphqlTest = graphQLClient.testConnection()
        val moodleTest = moodleClient.testSession()
        
        return ConnectivityStatus(
            graphqlConnected = graphqlTest.isSuccess,
            graphqlError = graphqlTest.exceptionOrNull()?.message,
            moodleConnected = moodleTest.getOrNull() ?: false,
            moodleError = moodleTest.exceptionOrNull()?.message
        )
    }
    
    /**
     * Close all HTTP clients to prevent resource leaks
     */
    override fun close() {
        graphQLClient.close()
        moodleClient.close()
    }
    
    data class ConnectivityStatus(
        val graphqlConnected: Boolean,
        val graphqlError: String? = null,
        val moodleConnected: Boolean,
        val moodleError: String? = null
    ) {
        val isFullyConnected: Boolean
            get() = graphqlConnected && moodleConnected
    }
}
