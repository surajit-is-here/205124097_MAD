package com.example.bookswap.utils

object Options {
    val categories = listOf(
        "Engineering", "Science", "Mathematics", "Medical", "Commerce & Management",
        "Arts & Humanities", "Law", "Computer Science", "Literature", "Other"
    )
    val departments = listOf(
        "Computer Science", "Information Technology", "Electronics", "Electrical",
        "Mechanical", "Civil", "Chemical", "Biotechnology", "Mathematics", "Physics",
        "Chemistry", "Commerce", "Management", "Arts", "Law", "Medicine", "Other"
    )
    val semesters = (1..8).map { "Semester $it" }
    val years = listOf("1st year", "2nd year", "3rd year", "4th year", "Postgraduate")
    val reportReasons = listOf(
        "Inappropriate content", "Spam or fake listing", "Wrong price or misleading details",
        "Suspicious seller", "Other"
    )
    const val ANY = "Any"
}
