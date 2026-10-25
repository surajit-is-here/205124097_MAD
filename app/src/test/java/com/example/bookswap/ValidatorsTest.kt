package com.example.bookswap

import com.example.bookswap.utils.Validators
import com.example.bookswap.utils.conversationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun validEmails() {
        assertTrue(Validators.isValidEmail("student@college.edu"))
        assertTrue(Validators.isValidEmail("a.b+c@uni.ac.in"))
    }

    @Test fun invalidEmails() {
        assertFalse(Validators.isValidEmail(""))
        assertFalse(Validators.isValidEmail("no-at-sign"))
        assertFalse(Validators.isValidEmail("a@b"))
    }

    @Test fun passwordLength() {
        assertFalse(Validators.isValidPassword("12345"))
        assertTrue(Validators.isValidPassword("123456"))
    }

    @Test fun priceValidation() {
        assertTrue(Validators.isValidPrice("0"))
        assertTrue(Validators.isValidPrice("150.50"))
        assertFalse(Validators.isValidPrice(""))
        assertFalse(Validators.isValidPrice("abc"))
        assertFalse(Validators.isValidPrice("-5"))
    }

    @Test fun conversationIdIsSymmetric() {
        assertEquals(conversationId("a", "b", "book1"), conversationId("b", "a", "book1"))
    }
}
