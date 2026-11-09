package dev.notechat

import android.view.View
import android.view.animation.PathInterpolator
import android.widget.LinearLayout

/** Reuse the outgoing view tree for one transition; no screenshot of private notes is made. */
class ScreenSlide(private val root: LinearLayout) {
    private var outgoing: LinearLayout?=null
    private var incoming=emptyList<View>()
    fun capture() {
        finish()
        if(root.width==0 || root.height==0 || root.childCount==0)return
        val old=LinearLayout(root.context).apply {
            orientation=root.orientation; setBackgroundColor(UiColors.of(root.context).surface)
            setPadding(root.paddingLeft,root.paddingTop,root.paddingRight,root.paddingBottom)
        }
        while(root.childCount>0)root.getChildAt(0).let { root.removeView(it); old.addView(it) }
        old.measure(View.MeasureSpec.makeMeasureSpec(root.width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(root.height,View.MeasureSpec.EXACTLY))
        old.layout(0,0,root.width,root.height)
        root.overlay.add(old); outgoing=old
    }
    fun start(back: Boolean) {
        val old=outgoing ?: return
        val distance=root.width.toFloat()*(if(back)1 else -1)
        incoming=(0 until root.childCount).map { root.getChildAt(it) }
        val easing=PathInterpolator(.2f,0f,0f,1f)
        incoming.forEach { view -> view.translationX=-distance; view.animate().translationX(0f).setDuration(240).setInterpolator(easing).start() }
        old.animate().translationX(distance).setDuration(240).setInterpolator(easing).withEndAction { finish() }.start()
    }
    fun finish() {
        outgoing?.let { old -> old.animate().cancel(); root.overlay.remove(old); old.removeAllViews() }
        outgoing=null
        incoming.forEach { it.animate().cancel(); it.translationX=0f }; incoming=emptyList()
    }
}
