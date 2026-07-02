package io.github.hypercopy.clipboard

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import io.github.hypercopy.AppIconCache
import io.github.hypercopy.R
import org.json.JSONArray
import org.json.JSONObject

object MiuiSuperIslandNotification {
    private const val ACTION_JUMP_PREFIX = "miui.focus.action_jump_"
    private const val PIC_APP_ICON = "miui.focus.pic_app_icon"
    private const val PIC_ARROW_RIGHT = "miui.focus.pic_arrow_right"

    fun apply(
        context: Context,
        notification: Notification,
        title: String,
        content: String,
        packageName: String,
        jumpActions: List<PendingJumpCoordinator.JumpAction>,
    ) {
        val extras = Bundle()
        extras.putBundle("miui.focus.actions", actionBundle(context, jumpActions))
        extras.putBundle("miui.focus.pics", pictureBundle(context, packageName))
        notification.extras.putAll(extras)
        notification.extras.putString("miui.focus.param", islandParams(title, content, jumpActions))
    }

    private fun actionBundle(context: Context, jumpActions: List<PendingJumpCoordinator.JumpAction>): Bundle {
        val actions = Bundle()
        jumpActions.forEachIndexed { index, jumpAction ->
            val action = Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_menu_view),
                jumpAction.title,
                jumpAction.pendingIntent,
            ).build()
            actions.putParcelable(actionKey(index), action)
        }
        return actions
    }

    private fun pictureBundle(context: Context, packageName: String): Bundle {
        val pics = Bundle()
        pics.putParcelable(PIC_APP_ICON, appIcon(context, packageName))
        pics.putParcelable(PIC_ARROW_RIGHT, Icon.createWithResource(context, R.drawable.ic_arrow_right))
        return pics
    }

    private fun appIcon(context: Context, packageName: String): Icon {
        AppIconCache.loadNow(context, packageName)?.let { return Icon.createWithBitmap(it) }
        val moduleIcon = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_menu_upload
        return Icon.createWithResource(context, moduleIcon)
    }

    private fun islandParams(title: String, content: String, jumpActions: List<PendingJumpCoordinator.JumpAction>): String {
        val actionArray = JSONArray().put(actionInfo(0, jumpActions.firstOrNull()?.title ?: contextSafeJumpText, "#E0E0E0"))
        val textButtonArray = JSONArray().apply {
            jumpActions.take(2).forEachIndexed { index, jumpAction ->
                put(actionInfo(index, jumpAction.title, null))
            }
        }
        val actionParams = JSONObject()
            .put("protocol", 3)
            .put("business", "code")
            .put("updatable", true)
            .put("ticker", "Code")
            .put("enableFloat", true)
            .put("isShowNotification", true)
            .put("islandFirstFloat", true)
            .put("tickerPic", PIC_APP_ICON)
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
                                            .put("pic", PIC_APP_ICON),
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
                                    .put("pic", PIC_ARROW_RIGHT),
                            ),
                    )
                    .put(
                        "smallIslandArea",
                        JSONObject().put(
                            "picInfo",
                            JSONObject()
                                .put("type", 1)
                                .put("pic", PIC_APP_ICON)
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
                            .put("src", PIC_APP_ICON)
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
        if (jumpActions.size > 1) {
            actionParams.put("textButton", textButtonArray)
        } else {
            actionParams.put("actions", actionArray)
        }
        return JSONObject()
            .put("isShowNotification", true)
            .put("param_v2", actionParams)
            .toString()
    }

    private fun actionInfo(index: Int, title: String, bgColor: String?): JSONObject {
        val info = JSONObject()
            .put("type", 2)
            .put("action", actionKey(index))
            .put("actionTitle", title)
            .put("actionIntentType", 1)
        if (bgColor != null) info.put("actionBgColor", bgColor)
        return info
    }

    private fun actionKey(index: Int): String = "$ACTION_JUMP_PREFIX$index"

    private const val contextSafeJumpText = "跳转"
}
