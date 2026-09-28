package kz.chaykin.zakazano.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kz.chaykin.zakazano.ZakazanoApp
import java.util.concurrent.TimeUnit

/**
 * Ежесуточная выгрузка копии на Диск. Здесь нельзя ничего спрашивать у пользователя:
 * если Google решит переспросить разрешение, работа честно останавливается и пишет
 * об этом в настройках, а не крутится впустую.
 */
class DriveSyncWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ZakazanoApp).container
        val settingsStore = container.settingsStore

        /** Причина пишется в настройки всегда, а на экран телефона — только если стоит будить. */
        suspend fun failed(reason: String, kind: SyncFailureKind): Result {
            settingsStore.setSyncFailed(reason)
            if (shouldNotifyAboutFailure(runAttemptCount, kind)) {
                container.backupFailureNotifier.show(reason)
            }
            return if (kind == SyncFailureKind.NEEDS_USER) Result.failure() else Result.retry()
        }

        return when (val access = container.driveAuth.request()) {
            // Уведомление об успехе снимает сам DriveSync.upload — и здесь, и при ручной выгрузке.
            is DriveAccess.Granted -> runCatching { container.driveSync.upload(access.token) }
                .fold(
                    onSuccess = { at ->
                        settingsStore.setSyncSucceeded(at)
                        Result.success()
                    },
                    onFailure = { error ->
                        failed(
                            reason = error.readableMessage(),
                            kind = if (error is DriveAuthExpired) SyncFailureKind.NEEDS_USER else SyncFailureKind.TRANSIENT,
                        )
                    },
                )

            is DriveAccess.NeedsConsent -> failed(
                reason = "Google просит подтвердить доступ — зайдите в настройки",
                kind = SyncFailureKind.NEEDS_USER,
            )

            is DriveAccess.Failed -> failed(access.message, SyncFailureKind.TRANSIENT)
        }
    }

    private fun Throwable.readableMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: this::class.simpleName.orEmpty()

    companion object {
        private const val WORK_NAME = "drive-daily-backup"

        fun setDailyEnabled(context: Context, enabled: Boolean) {
            val manager = WorkManager.getInstance(context.applicationContext)
            if (!enabled) {
                manager.cancelUniqueWork(WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<DriveSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
