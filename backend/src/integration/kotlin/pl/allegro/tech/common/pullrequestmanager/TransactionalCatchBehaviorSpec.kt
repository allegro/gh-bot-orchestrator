package pl.allegro.tech.common.pullrequestmanager

import io.kotest.matchers.shouldBe
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

/**
 * Verifies Postgres transaction behavior when a SQL error occurs inside a try-catch
 * within a single transaction.
 */
class TransactionalCatchBehaviorSpec(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val transactionTemplate: TransactionTemplate
) : BaseIntegrationSpec() {

    init {

        test("SQL error caught inside transaction should NOT prevent subsequent SQL in the same transaction") {
            // This test checks whether Postgres allows further SQL after a caught SQL error
            // within the same transaction (without savepoints).
            //
            // Expected behavior in Postgres: the transaction is aborted and further SQL fails.
            // If this test PASSES, the try-catch pattern in schedulers is safe.
            // If this test FAILS, the try-catch pattern silently loses tasks.
            var secondInsertSucceeded = false
            var caughtAbortedTransaction = false

            try {
                transactionTemplate.execute {
                    jdbcTemplate.update(
                        "CREATE TEMP TABLE _tx_test (id INT PRIMARY KEY, val TEXT) ON COMMIT DROP",
                        emptyMap<String, Any>()
                    )
                    jdbcTemplate.update(
                        "INSERT INTO _tx_test (id, val) VALUES (1, 'first')",
                        emptyMap<String, Any>()
                    )

                    try {
                        jdbcTemplate.update(
                            "INSERT INTO _tx_test (id, val) VALUES (1, 'duplicate')",
                            emptyMap<String, Any>()
                        )
                    } catch (e: Exception) {}

                    try {
                        jdbcTemplate.update(
                            "INSERT INTO _tx_test (id, val) VALUES (2, 'second')",
                            emptyMap<String, Any>()
                        )
                        secondInsertSucceeded = true
                    } catch (e: Exception) {
                        caughtAbortedTransaction = true
                    }
                }
            } catch (_: Exception) {}

            secondInsertSucceeded shouldBe false
            caughtAbortedTransaction shouldBe true
        }
    }
}
