package de.kolping.cockpit.models

import kotlinx.serialization.Serializable

/**
 * Student personal data
 * Ported from Python graphql_client.py
 */
@Serializable
data class Student(
    val studentId: String,
    val geschlechtTnid: String? = null,
    val titel: String? = null,
    val akademischerGradTnid: String? = null,
    val vorname: String,
    val nachname: String,
    val geburtsdatum: String? = null,
    val geburtsort: String? = null,
    val geburtslandTnid: String? = null,
    val staatsangehoerigkeitTnid: String? = null,
    val createdAt: String? = null,
    val wohnlandTnid: String? = null,
    val telefonnummer: String? = null,
    val emailPrivat: String? = null,
    val strasse: String? = null,
    val hausnummer: String? = null,
    val plz: String? = null,
    val wohnort: String? = null,
    val benutzername: String? = null,
    val emailKh: String? = null,
    val notizen: String? = null,
    val bemerkung: String? = null,
    val akademischerGrad: String? = null,
    val geburtsland: String? = null,
    val staatsangehoerigkeit: String? = null,
    val wohnland: String? = null
)

/**
 * Module with grade information
 * Ported from Python graphql_client.py
 */
@Serializable
data class Module(
    val modulId: String,
    val semester: Int,
    val modulbezeichnung: String,
    val eCTS: Double,
    val pruefungsId: String? = null,
    val pruefungsform: String? = null,
    val grade: String? = null,
    val points: Double? = null,
    val note: String? = null,
    val color: String? = null,
    val examStatus: String? = null,
    val eCTSString: String? = null
)

/**
 * Complete grade overview
 * Ported from Python graphql_client.py
 */
@Serializable
data class GradeOverview(
    val modules: List<Module>,
    val grade: String? = null,
    val eCTS: Double,
    val currentSemester: String,
    val student: Student? = null
)
