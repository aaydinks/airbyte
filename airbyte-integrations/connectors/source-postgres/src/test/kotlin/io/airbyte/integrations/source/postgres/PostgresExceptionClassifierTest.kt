/*
 * Copyright (c) 2026 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.integrations.source.postgres

import io.airbyte.cdk.output.RegexExceptionClassifier
import io.airbyte.cdk.output.TransientError
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import java.net.SocketTimeoutException
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest(environments = ["test"])
class PostgresExceptionClassifierTest {

    @Inject lateinit var classifier: RegexExceptionClassifier

    @Test
    fun testSocketTimeoutConnectTimedOut() {
        val exception = SocketTimeoutException("Connect timed out")
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
        val transient = result as TransientError
        assertTrue(
            transient.displayMessage.contains("Database connection timed out during socket connect")
        )
    }

    @Test
    fun testSocketTimeoutConnectionTimedOut() {
        val exception = SocketTimeoutException("Connection timed out")
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
        val transient = result as TransientError
        assertTrue(
            transient.displayMessage.contains("Database connection timed out during socket connect")
        )
    }

    @Test
    fun testWrappedSocketTimeoutException() {
        val cause = SocketTimeoutException("Connect timed out")
        val exception =
            RuntimeException("java.net.SocketTimeoutException: Connect timed out", cause)
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
    }

    @Test
    fun testPSQLExceptionConnectionAttemptFailed() {
        val exception =
            RuntimeException("org.postgresql.util.PSQLException: The connection attempt failed.")
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
        val transient = result as TransientError
        assertTrue(transient.displayMessage.contains("Database connection attempt failed"))
    }

    @Test
    fun testExistingHikariTimeoutStillWorks() {
        val exception =
            RuntimeException(
                "java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 30000ms"
            )
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
    }

    @Test
    fun testExistingMsecTimeoutStillWorks() {
        val exception =
            RuntimeException("java.util.concurrent.TimeoutException: Timed out after 15000 msec")
        val result = classifier.classify(exception)
        assertNotNull(result)
        assertTrue(result is TransientError)
    }
}
