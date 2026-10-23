package dev.notechat

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout

/** Confirms newly created backup passwords; restores need only the original password. */
class BackupPasswordForm(context: Context, confirm: Boolean) : LinearLayout(context) {
    private fun field(label: String) = EditText(context).apply {
        hint=label
        inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val horizontal=(20*resources.displayMetrics.density).toInt()
        val vertical=(12*resources.displayMetrics.density).toInt()
        setPadding(horizontal,vertical,horizontal,vertical)
    }
    private val password=field("Backup password · at least 8 characters")
    private val repeated=if(confirm)field("Repeat backup password") else null
    init { orientation=VERTICAL; addView(password); repeated?.let(::addView) }

    fun readPassword(): CharArray? {
        password.error=null; repeated?.error=null
        if(password.text.length<8) { password.error="Use at least eight characters"; return null }
        if(repeated!=null && password.text.toString()!=repeated.text.toString()) {
            repeated.error="Passwords do not match"; repeated.requestFocus(); return null
        }
        return password.text.toString().toCharArray()
    }
    fun clear() { password.text.clear(); repeated?.text?.clear() }
}
