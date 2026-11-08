package dev.notechat

import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w393dp-h830dp-port-mdpi")
class NavigationMotionTest {
    @Test fun forwardAndBackSlideOppositeWaysAndInterruptedNavigationClearsOldContent() {
        val context=RuntimeEnvironment.getApplication()
        val root=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(0,24,0,16) }
        fun layout() { root.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(830,View.MeasureSpec.EXACTLY)); root.layout(0,0,393,830) }
        val old=TextView(context).apply { text="First screen" }
        root.addView(old,LinearLayout.LayoutParams(-1,-1)); layout()
        val slide=ScreenSlide(root)
        slide.capture()
        val next=TextView(context).apply { text="Settings" }
        root.addView(next,LinearLayout.LayoutParams(-1,-1)); slide.start(false)
        assertTrue(next.translationX<0) // Enters from the left, moving right.
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(120))
        assertTrue(next.translationX<=0)
        slide.finish()
        assertEquals(0f,next.translationX); assertNull(old.parent)
        slide.capture()
        root.addView(old,LinearLayout.LayoutParams(-1,-1)); slide.start(true)
        assertTrue(old.translationX>0) // Back enters from the right, moving left.
        slide.finish()
        assertEquals(0f,old.translationX); assertNull(next.parent)
    }
    @Test fun horizontalBoardPreviewKeepsItsGestureAwayFromTheOuterPager() {
        val context=RuntimeEnvironment.getApplication()
        var disallowed=false
        val parent=object:FrameLayout(context) {
            override fun requestDisallowInterceptTouchEvent(value:Boolean) { disallowed=value; super.requestDisallowInterceptTouchEvent(value) }
        }
        val guard=PreviewGuard(context)
        val preview=RecyclerView(context).apply {
            layoutManager=LinearLayoutManager(context,RecyclerView.HORIZONTAL,false)
        }
        guard.addView(preview,FrameLayout.LayoutParams(300,100)); parent.addView(guard,FrameLayout.LayoutParams(393,830))
        parent.measure(View.MeasureSpec.makeMeasureSpec(393,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(830,View.MeasureSpec.EXACTLY)); parent.layout(0,0,393,830)
        fun touch(action:Int,x:Float,y:Float) { MotionEvent.obtain(0,10,action,x,y,0).let { guard.dispatchTouchEvent(it); it.recycle() } }
        touch(MotionEvent.ACTION_DOWN,50f,50f); assertTrue(disallowed)
        touch(MotionEvent.ACTION_MOVE,10f,50f); assertTrue(disallowed)
        touch(MotionEvent.ACTION_CANCEL,10f,50f); assertFalse(disallowed)
        touch(MotionEvent.ACTION_DOWN,50f,300f); assertFalse(disallowed)
    }
}
