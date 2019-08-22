package com.crrl.beatplayer.alertdialog

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.crrl.beatplayer.alertdialog.dialogs.AlertItemAction
import com.crrl.beatplayer.alertdialog.stylers.AlertItemStyle
import com.crrl.beatplayer.alertdialog.stylers.AlertType
import com.crrl.beatplayer.alertdialog.stylers.InputStyle
import com.crrl.beatplayer.alertdialog.stylers.ItemStyle
import com.crrl.beatplayer.alertdialog.views.BottomSheetAlert
import com.crrl.beatplayer.alertdialog.views.DialogAlert
import com.crrl.beatplayer.alertdialog.views.InputDialog

class AlertDialog(
    private var title: String,
    private var message: String,
    private var style: ItemStyle,
    private var type: AlertType,
    private val inputText: String = ""
) {

    private var theme: AlertType? = AlertType.DIALOG
    private var actions: ArrayList<AlertItemAction> = ArrayList()
    private var alert: DialogFragment? = null

    /**
     * Add Item to AlertDialog
     * If you are using InputDialog, you can only add 2 actions
     * that will appear at the dialog bottom
     * @param item: AlertItemAction
     */
    fun addItem(item: AlertItemAction) {
        actions.add(item)
    }

    /**
     * Receives an Activity (AppCompatActivity), It's is necessary to getContext and show AlertDialog
     * @param activity: AppCompatActivity
     */
    fun show(activity: AppCompatActivity) {
        when (type) {
            AlertType.BOTTOM_SHEET -> {
                Thread {
                    if (alert == null) alert =
                        BottomSheetAlert(title, message, actions, style as AlertItemStyle)
                    alert?.show(activity.supportFragmentManager, alert?.tag)
                }.start()

            }
            AlertType.DIALOG -> {
                Thread {
                    if (alert == null) alert =
                        DialogAlert(title, message, actions, style as AlertItemStyle)
                    alert?.show(activity.supportFragmentManager, alert?.tag)
                }.start()
            }

            AlertType.INPUT -> {
                Thread {
                    if (alert == null) alert =
                        InputDialog(title, message, actions, style as InputStyle, inputText)
                    alert?.show(activity.supportFragmentManager, alert?.tag)
                }.start()
            }
        }
    }

    /**
     * Set type for alert. Choose between "AlertType.DIALOG" and "AlertType.BOTTOM_SHEET"
     * @param type: AlertType
     */
    fun setType(type: AlertType) {
        this.theme = type
    }

    /**
     * Update all style in the application
     * @param style: AlertType
     */
    fun setStyle(style: AlertItemStyle) {
        this.style = style
    }

    /**
     * Changes the style directly
     * @return style: AlertItemStyle
     */
    fun getStyle(): ItemStyle {
        return this.style
    }
}