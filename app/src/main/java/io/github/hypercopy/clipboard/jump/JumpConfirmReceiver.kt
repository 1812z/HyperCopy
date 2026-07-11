package io.github.hypercopy.clipboard.jump

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.hypercopy.Config

class JumpConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Config.ACTION_CONFIRM_JUMP) return
        val id = intent.getLongExtra(Config.EXTRA_PENDING_JUMP_ID, 0L)
        if (id == 0L) return
        val userId = intent.getIntExtra(Config.EXTRA_PENDING_JUMP_USER_ID, -1)
        PendingJumpCoordinator.confirm(context.applicationContext, id, userId.takeIf { it >= 0 })
    }
}
