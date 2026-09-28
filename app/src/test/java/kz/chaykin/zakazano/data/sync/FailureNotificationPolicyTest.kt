package kz.chaykin.zakazano.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FailureNotificationPolicyTest {

    @Test
    fun когда_нужен_человек_будим_сразу() {
        assertTrue(shouldNotifyAboutFailure(attempt = 0, kind = SyncFailureKind.NEEDS_USER))
        assertTrue(shouldNotifyAboutFailure(attempt = 5, kind = SyncFailureKind.NEEDS_USER))
    }

    @Test
    fun мигнувшую_сеть_первые_две_неудачи_терпим_молча() {
        assertFalse(shouldNotifyAboutFailure(attempt = 0, kind = SyncFailureKind.TRANSIENT))
        assertFalse(shouldNotifyAboutFailure(attempt = 1, kind = SyncFailureKind.TRANSIENT))
    }

    @Test
    fun с_третьей_неудачи_подряд_уже_говорим() {
        // runAttemptCount у WorkManager считается с нуля: 2 — это третья попытка.
        assertTrue(shouldNotifyAboutFailure(attempt = 2, kind = SyncFailureKind.TRANSIENT))
        assertTrue(shouldNotifyAboutFailure(attempt = 7, kind = SyncFailureKind.TRANSIENT))
    }
}
