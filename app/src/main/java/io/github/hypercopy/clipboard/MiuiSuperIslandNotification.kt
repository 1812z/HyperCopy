package io.github.hypercopy.clipboard

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import io.github.hypercopy.R
import org.json.JSONArray
import org.json.JSONObject

object MiuiSuperIslandNotification {
    private const val ACTION_JUMP = "miui.focus.action_jump"
    private const val PIC_ICON = "miui.focus.pic_icon"

    fun apply(
        context: Context,
        notification: Notification,
        title: String,
        content: String,
        jumpPendingIntent: PendingIntent,
    ) {
        val extras = Bundle()
        extras.putBundle("miui.focus.actions", actionBundle(context, jumpPendingIntent))
        extras.putBundle("miui.focus.pics", pictureBundle(context))
        notification.extras.putAll(extras)
        notification.extras.putString("miui.focus.param", islandParams(title, content))
    }

    private fun actionBundle(context: Context, jumpPendingIntent: PendingIntent): Bundle {
        val actions = Bundle()
        val action = Notification.Action.Builder(
            Icon.createWithResource(context, android.R.drawable.ic_menu_view),
            context.getString(R.string.action_jump),
            jumpPendingIntent,
        ).build()
        actions.putParcelable(ACTION_JUMP, action)
        return actions
    }

    private fun pictureBundle(context: Context): Bundle {
        val pics = Bundle()
        pics.putParcelable(PIC_ICON, Icon.createWithResource(context, android.R.drawable.ic_menu_upload))
        return pics
    }

    private fun islandParams(title: String, content: String): String {
        return JSONObject()
            .put("isShowNotification", true)
            .put(
                "param_v2",
                JSONObject()
                    .put("protocol", 3)
                    .put("business", "code")
                    .put("updatable", true)
                    .put("ticker", "Code")
                    .put("enableFloat", true)
                    .put("isShowNotification", true)
                    .put("islandFirstFloat", true)
                    .put("tickerPic", PIC_ICON)
                    .put(
                        "param_island",
                        JSONObject()
                            .put("islandProperty", 1)
                            .put("islandPriority", 2)
                            .put("islandOrder", false)
                            .put("dismissIsland", false)
                            .put("maxSize", false)
                            .put("needCloseAnimation", true)
                            .put(
                                "bigIslandArea",
                                JSONObject()
                                    .put(
                                        "imageTextInfoLeft",
                                        JSONObject()
                                            .put("type", 1)
                                            .put(
                                                "picInfo",
                                                JSONObject()
                                                    .put("type", 1)
                                                    .put("pic", PIC_ICON),
                                            )
                                            .put(
                                                "miui.focus.paramtextInfo",
                                                JSONObject()
                                                    .put("frontTitle", title)
                                                    .put("title", "跳转")
                                                    .put("content", content)
                                                    .put("useHighLight", false),
                                            ),
                                    )
                                    .put(
                                        "picInfo",
                                        JSONObject()
                                            .put("type", 1)
                                            .put("pic", PIC_ICON),
                                    ),
                            )
                            .put(
                                "smallIslandArea",
                                JSONObject().put(
                                    "picInfo",
                                    JSONObject()
                                        .put("type", 1)
                                        .put("pic", PIC_ICON)
                                        .put("loop", false)
                                        .put("autoplay", false)
                                        .put("number", 0),
                                ),
                            )
                            .put(
                                "shareData",
                                JSONObject().put("title", title),
                            ),
                    )
                    .put(
                        "iconTextInfo",
                        JSONObject()
                            .put(
                                "animIconInfo",
                                JSONObject()
                                    .put("type", 0)
                                    .put("src", PIC_ICON)
                                    .put("loop", true)
                                    .put("autoplay", true),
                            )
                            .put("title", title)
                            .put("content", content),
                    )
                    .put(
                        "baseInfo",
                        JSONObject()
                            .put("title", title)
                            .put("content", content)
                            .put("type", 2),
                    )
                    .put(
                        "actions",
                        JSONArray().put(
                            JSONObject()
                                .put("type", 2)
                                .put("action", ACTION_JUMP)
                                .put("actionTitle", "跳转")
                                .put("actionIntentType", 1)
                                .put("actionBgColor", "#E0E0E0"),
                        ),
                    ),
            )
            .toString()
    }
}
