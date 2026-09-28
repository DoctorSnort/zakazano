package kz.chaykin.zakazano.data.sync

/** Чем кончилась неудачная выгрузка — от этого зависит, будить ли человека. */
enum class SyncFailureKind {
    /** Без человека не починится: Google просит заново подтвердить доступ. */
    NEEDS_USER,

    /** Может пройти само: мигнула сеть, Google ответил ошибкой, таймаут. */
    TRANSIENT,
}

/**
 * Показывать ли уведомление о неудачной фоновой выгрузке.
 *
 * Если нужен человек — сразу: ждать бессмысленно, само не пройдёт. Временные ошибки —
 * только с третьей неудачи подряд (`attempt` у WorkManager считается с нуля). Иначе
 * каждый мигнувший в метро интернет превращался бы в тревогу на экране блокировки,
 * хотя через полчаса повтор всё и так выгрузит.
 */
fun shouldNotifyAboutFailure(attempt: Int, kind: SyncFailureKind): Boolean = when (kind) {
    SyncFailureKind.NEEDS_USER -> true
    SyncFailureKind.TRANSIENT -> attempt >= TRANSIENT_ATTEMPTS_BEFORE_NOTICE
}

private const val TRANSIENT_ATTEMPTS_BEFORE_NOTICE = 2
