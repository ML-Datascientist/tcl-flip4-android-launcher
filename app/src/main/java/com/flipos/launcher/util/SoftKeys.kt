package com.flipos.launcher.util

import android.view.KeyEvent

/**
 * Key codes that act as the on-screen LEFT soft key (the bar's "Back"/exit,
 * or Home's left-bar app).
 *
 * The TCL T440W's matrix keypad (`soc:matrix_keypad0`) delivers its soft keys
 * as F1/F2 via `/odm/usr/keylayout/soc_matrix_keypad0.kl` (root-only; the
 * F1=left/F2=right assignment was verified on-device with key probes and
 * `getevent`). SOFT_LEFT is accepted as the literal Android soft-key code
 * for devices/emulators that emit it. See AGENTS.md, "Soft-key hardware
 * mapping".
 */
val LEFT_SOFT_KEYS = intArrayOf(
    KeyEvent.KEYCODE_SOFT_LEFT,
    KeyEvent.KEYCODE_F1,
)

/**
 * Key codes that act as the on-screen RIGHT soft key (the bar's contextual
 * action: Options, Clear, Open Activities, ...). On the T440W the live code
 * is F2; BACK keeps system-back semantics on devices that emit it.
 */
val RIGHT_SOFT_KEYS = intArrayOf(
    KeyEvent.KEYCODE_SOFT_RIGHT,
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_F2,
)
