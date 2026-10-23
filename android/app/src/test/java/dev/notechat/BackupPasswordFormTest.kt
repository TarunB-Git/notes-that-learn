package dev.notechat

import android.widget.EditText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class BackupPasswordFormTest {
    private fun form(confirm: Boolean): BackupPasswordForm {
        val context=Robolectric.buildActivity(android.app.Activity::class.java).get()
        return BackupPasswordForm(context,confirm)
    }
    @Test fun creationRejectsTyposAndAcceptsMatchingPasswordsWithoutChangingThem() {
        val form=form(true)
        assertEquals(2,form.childCount)
        val first=form.getChildAt(0) as EditText
        val repeated=form.getChildAt(1) as EditText
        first.setText("correct-password"); repeated.setText("typo-password")
        assertNull(form.readPassword()); assertNotNull(repeated.error)
        assertEquals("correct-password",first.text.toString())
        repeated.setText("correct-password")
        val secret=form.readPassword()!!
        assertEquals("correct-password",String(secret)); secret.fill('\u0000')
        form.clear()
        assertTrue(first.text.isEmpty()); assertTrue(repeated.text.isEmpty())
    }
    @Test fun restoresUseOneFieldAndShortPasswordsAreRejected() {
        val form=form(false)
        assertEquals(1,form.childCount)
        val password=form.getChildAt(0) as EditText
        password.setText("short"); assertNull(form.readPassword()); assertNotNull(password.error)
        password.setText("existing-password")
        val secret=form.readPassword()!!
        assertEquals("existing-password",String(secret)); secret.fill('\u0000')
        form.clear(); assertTrue(password.text.isEmpty())
    }
}
