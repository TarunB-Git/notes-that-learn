package dev.notechat

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import android.view.View

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w320dp-h720dp-port-mdpi")
class NotebookLayoutTest {
    private val categories=listOf(Category("work","Work","Work notes"),Category("todos","To-do","Tasks"))
    @Test fun conversionPreservesEveryRowCellAndBackupState() {
        val row=Message(id="one",text="Book venue",category="work",tableCells=mapOf("details" to "Seats 40\nProjector needed"))
        val engine=ChatEngine(ChatState(categories,listOf(row)))
        for(mode in listOf("table","list","default","table"))engine.setNotebook("work",NotebookLayout(mode))
        assertEquals(row,engine.state.messages.single())
        assertEquals(engine.state,StateJson.decode(StateJson.encode(engine.state)))
        BackupArchive.validate(engine.state)
        val legacy=org.json.JSONObject(StateJson.encode(ChatState(categories)).toString(Charsets.UTF_8))
        legacy.getJSONArray("categories").getJSONObject(0).remove("layout")
        assertEquals("default",StateJson.decode(legacy.toString().toByteArray()).categories.first().layout.mode)
    }
    @Test fun customListsKeepCompletedItemsWhileTodoArchiveStillWorks() {
        val chat=Chat("private","Shopping",layout=NotebookLayout("list"))
        val engine=ChatEngine(ChatState(categories.map { if(it.id=="work")it.copy(layout=NotebookLayout("list")) else it },
            listOf(Message(id="a",text="A",category="work"),Message(id="b",text="B",chatId="private"),Message(id="c",text="C",category="todos")),chats=listOf(chat)))
        listOf("a","b","c").forEach { engine.complete(it,true,1000L) }
        assertTrue(engine.state.messages.take(2).all { it.done && it.visible(ARCHIVE_DELAY_MS+2000) })
        assertTrue(engine.state.messages.last().completedArchived(ARCHIVE_DELAY_MS+2000))
        assertFalse(engine.state.messages[1].trainable())
        val rendered=MarkdownWorkspace.render(engine.state,null,2000L,"private")
        assertTrue(rendered.contains("- [x] B"))
        assertTrue(MarkdownWorkspace.parse(rendered,engine.state).entries.single().done)
    }
    @Test fun markdownCellsRemainEditableAndRecoverWithTheirLayout() {
        val message=Message(id="one",text="Venue",category="work",tableCells=mapOf("details" to "Seats 40\nProjector"))
        var state=ChatState(categories.map { if(it.id=="work")it.copy(layout=NotebookLayout("table")) else it },listOf(message))
        val original=MarkdownWorkspace.render(state,"work")
        assertEquals(message.tableCells,MarkdownWorkspace.parse(original,state).entries.single().tableCells)
        state=state.copy(markdown=MarkdownState(baseline=mapOf("Work.md" to original)))
        val edited=original.replace("Seats 40","Seats 50")
        val merged=MarkdownWorkspace.merge(state,mapOf("Work.md" to edited))
        assertTrue(merged.conflicts.isEmpty())
        assertEquals("Seats 50\nProjector",merged.state.messages.single().tableCells["details"])
        assertEquals("Venue",merged.state.messages.single().text)
        val recovered=FolderRecovery.reconstruct(mapOf("Work.md" to edited),categories)
        assertEquals("table",recovered.categories.first().layout.mode)
        assertEquals("Seats 50\nProjector",recovered.messages.single().tableCells["details"])
    }
    @Test fun smallTablesFitThePhoneAndWiderTablesScroll() {
        val context=RuntimeEnvironment.getApplication()
        val three=NotebookLayout("table").withTitles(listOf("Item","Amount","Details"))
        fun view(layout:NotebookLayout):NotebookTable {
            val table=NotebookTable(context,layout,listOf(Message(text="Rice")),UiColors(false),emptySet(),{ _,_-> },{})
            table.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY))
            table.layout(0,0,320,600)
            return table
        }
        assertTrue(view(three).getChildAt(0).measuredWidth<=320)
        assertTrue(view(three.withTitles(listOf("Item","Amount","Details","Owner","Date"))).getChildAt(0).measuredWidth>320)
    }
    @Test fun largeCellsHaveBoundedPreviewsWithoutLosingTheirStoredText() {
        val context=RuntimeEnvironment.getApplication()
        val message=Message(text="Long note\n".repeat(20_000))
        var opened:Message?=null
        val table=NotebookTable(context,NotebookLayout("table"),listOf(message),UiColors(false),emptySet(),{ row,_->opened=row },{})
        table.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY)); table.layout(0,0,320,600)
        fun texts(view:View):List<android.widget.TextView> = if(view is android.widget.TextView)listOf(view) else if(view is android.view.ViewGroup)(0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else emptyList()
        val preview=texts(table).first { it.text.startsWith("Long note") }
        assertTrue(preview.text.length<=TEXT_PREVIEW_CHARS); assertEquals(6,preview.maxLines)
        preview.performClick(); assertEquals(message.text,opened!!.text)
    }

}
