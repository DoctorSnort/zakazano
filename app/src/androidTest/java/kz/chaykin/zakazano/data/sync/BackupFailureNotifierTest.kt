package kz.chaykin.zakazano.data.sync

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Уведомление о неудачной выгрузке: одно, заменяется, снимается. */
@RunWith(AndroidJUnit4::class)
class BackupFailureNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifier = BackupFailureNotifier(context)
    private val manager = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notifier.createChannel()
        notifier.cancel()
    }

    @After
    fun tearDown() = notifier.cancel()

    private fun ours() = manager.activeNotifications.filter { it.packageName == context.packageName }

    /**
     * Система публикует уведомления асинхронно и притормаживает частые notify() от одного
     * приложения, поэтому сразу после вызова список активных может ещё не обновиться.
     */
    private fun awaitOurs(
        count: Int,
        ready: (List<StatusBarNotification>) -> Boolean = { true },
    ): List<StatusBarNotification> {
        val deadline = System.currentTimeMillis() + 3_000
        var shown = ours()
        while ((shown.size != count || !ready(shown)) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100)
            shown = ours()
        }
        return shown
    }

    @Test
    fun новое_уведомление_заменяет_старое_а_не_копится() {
        notifier.show("Нет связи с Google")
        notifier.show("Google Диск ответил ошибкой 503")

        val latest = "Google Диск ответил ошибкой 503"
        val shown = awaitOurs(1) { list -> list.single().notification.extras.text() == latest }
        assertEquals(1, shown.size)
        val extras = shown.single().notification.extras
        assertEquals("Копия не уехала на Google Диск", extras.getString(NotificationCompat.EXTRA_TITLE))
        assertEquals(latest, extras.text())
        assertEquals("backup", shown.single().notification.channelId)
    }

    @Test
    fun удачная_выгрузка_снимает_уведомление() {
        notifier.show("Нет связи с Google")
        assertEquals(1, awaitOurs(1).size)

        notifier.cancel()

        assertTrue(awaitOurs(0).isEmpty())
    }

    private fun android.os.Bundle.text(): String? =
        getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
}
