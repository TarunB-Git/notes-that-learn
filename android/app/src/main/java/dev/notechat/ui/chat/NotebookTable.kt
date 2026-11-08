package dev.notechat

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** One horizontal viewport keeps column headers aligned with virtualized rows. */
class NotebookTable(context: Context, layout: NotebookLayout, messages: List<Message>, colors: UiColors,
                    selected: Set<String>, edit: (Message, String) -> Unit, select: (Message) -> Unit,
                    heading: ((TableColumn) -> Unit)? = null, addColumn: (() -> Unit)? = null) : HorizontalScrollView(context) {
    private class RowHolder(val box:LinearLayout):RecyclerView.ViewHolder(box) { init { box.layoutParams=RecyclerView.LayoutParams(-1,-2) } }
    val recycler=RecyclerView(context).apply { itemAnimator=null }
    init {
        isFillViewport=true
        val density=resources.displayMetrics.density
        fun dp(n:Int)=(n*density).toInt()
        val available=(resources.displayMetrics.widthPixels-dp(24)).coerceAtLeast(dp(260))
        val toolsWidth=if(addColumn==null)0 else dp(48)
        val columnWidth=((available-toolsWidth)/layout.columns.size).coerceAtLeast(dp(96))
        val content=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),0,dp(12),0) }
        fun cell(text:String,heading:Boolean=false)=TextView(context).apply {
            this.text=text.take(TEXT_PREVIEW_CHARS); maxLines=if(heading)3 else 6; ellipsize=android.text.TextUtils.TruncateAt.END; textSize=if(heading)13f else 16f; setTextColor(if(heading)colors.muted else colors.ink)
            setPadding(dp(12),dp(12),dp(12),dp(12)); minHeight=dp(52); gravity=Gravity.TOP
            if(heading)setTypeface(null,Typeface.BOLD)
            background=android.graphics.drawable.GradientDrawable().apply { setColor(if(heading)colors.inset else colors.surface); setStroke(dp(1).coerceAtLeast(1),colors.divider) }
        }
        content.addView(LinearLayout(context).apply {
            layout.columns.forEach { column -> addView(cell(column.title,true).apply {
                if(heading!=null) { contentDescription="Rename column ${column.title}"; setOnClickListener { heading(column) } }
            },LinearLayout.LayoutParams(columnWidth,-1)) }
            if(addColumn!=null)addView(cell("+",true).apply {
                contentDescription="Add column"; gravity=Gravity.CENTER; textSize=22f; setOnClickListener { addColumn() }
            },LinearLayout.LayoutParams(toolsWidth,-1))
        })
        recycler.layoutManager=LinearLayoutManager(context)
        recycler.adapter=object:RecyclerView.Adapter<RowHolder>() {
            override fun getItemCount()=messages.size
            override fun onCreateViewHolder(parent:ViewGroup,type:Int)=RowHolder(LinearLayout(context).apply { orientation=LinearLayout.VERTICAL })
            override fun onBindViewHolder(holder:RowHolder,position:Int) {
                val message=messages[position]; holder.box.removeAllViews(); holder.box.tag=message.id
                val row=LinearLayout(context).apply { minimumHeight=dp(52) }
                layout.columns.forEach { column ->
                    val value=if(column.id=="note")message.text.ifBlank { message.attachment?.name.orEmpty() } else message.tableCells[column.id].orEmpty()
                    row.addView(cell(value).apply {
                        contentDescription="${column.title}: ${value.take(200).ifBlank { "Empty cell" }}"
                        if(message.id in selected)setBackgroundColor(colors.selected)
                        setOnClickListener { if(selected.isNotEmpty())select(message) else edit(message,column.id) }
                        setOnLongClickListener { select(message); true }
                    },LinearLayout.LayoutParams(columnWidth,-1))
                }
                holder.box.addView(row,LinearLayout.LayoutParams(-1,-2))
            }
        }
        content.addView(recycler,LinearLayout.LayoutParams(-1,0,1f))
        addView(content,LayoutParams(columnWidth*layout.columns.size+toolsWidth+dp(24),-1))
    }
}
