package dev.notechat

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

/** Both overviews stay mounted: the finger moves the pages, not a replacement screen. */
class HomePager(context: Context, pages: List<View>, initial: Int, private val settled: (Int) -> Unit) : FrameLayout(context) {
    private class Holder(val box: FrameLayout) : RecyclerView.ViewHolder(box)
    private val pages = pages.toMutableList()
    val pager = ViewPager2(context).apply { offscreenPageLimit=1; isSaveEnabled=false }
    private var reported=initial
    private val callback=object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) { if(pager.scrollState==ViewPager2.SCROLL_STATE_IDLE)report() }
        override fun onPageScrollStateChanged(state: Int) { if(state==ViewPager2.SCROLL_STATE_IDLE)report() }
    }
    init {
        require(pages.size==2 && initial in 0..1)
        pager.adapter=object : RecyclerView.Adapter<Holder>() {
            override fun getItemCount()=this@HomePager.pages.size
            override fun onCreateViewHolder(parent: ViewGroup, type: Int)=Holder(PreviewGuard(context).apply { layoutParams=RecyclerView.LayoutParams(-1,-1) })
            override fun onBindViewHolder(holder: Holder, position: Int) {
                holder.box.removeAllViews()
                this@HomePager.pages[position].let { page -> (page.parent as? ViewGroup)?.removeView(page); holder.box.addView(page,LayoutParams(-1,-1)) }
            }
        }
        pager.setCurrentItem(initial,false)
        pager.registerOnPageChangeCallback(callback)
        addView(pager,LayoutParams(-1,-1))
    }
    private fun report() { if(pager.currentItem!=reported) { reported=pager.currentItem; settled(reported) } }
    fun select(index: Int) { pager.setCurrentItem(index,android.animation.ValueAnimator.areAnimatorsEnabled()) }
    fun replace(index: Int, page: View) { pages[index]=page; pager.adapter?.notifyItemChanged(index) }
    fun dispose() { pager.unregisterOnPageChangeCallback(callback) }
}

/** A horizontal note preview owns its gesture; vertical scrolling still belongs to its board. */
internal class PreviewGuard(context: Context) : FrameLayout(context) {
    private var preview=false
    private fun hitsPreview(view: View, event: MotionEvent): Boolean {
        if(view is RecyclerView && (view.layoutManager as? LinearLayoutManager)?.orientation==RecyclerView.HORIZONTAL) {
            val bounds=Rect()
            if(view.getGlobalVisibleRect(bounds) && bounds.contains(event.rawX.toInt(),event.rawY.toInt()))return true
        }
        return view is ViewGroup && (0 until view.childCount).any { hitsPreview(view.getChildAt(it),event) }
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked==MotionEvent.ACTION_DOWN)preview=hitsPreview(this,event)
        if(preview)parent?.requestDisallowInterceptTouchEvent(true)
        val handled=super.dispatchTouchEvent(event)
        if(event.actionMasked in listOf(MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL)) { preview=false; parent?.requestDisallowInterceptTouchEvent(false) }
        return handled
    }
}
