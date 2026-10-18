package dev.notechat

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class LongNoteTest {
    private val categories=listOf(Category("thoughts","Thoughts","Observations"))
    @Test fun millionCharacterNoteSurvivesTranscriptMarkdownAndEncryptedBackup() {
        val text="a".repeat(MAX_MESSAGE_CHARS-5)+"\nEND!"
        val imported=ChatImport.parse("13/10/2026, 10:00 - Me: $text").entries.single()
        assertEquals(text,imported.text)
        val note=Message(text=imported.text,category="thoughts")
        val state=ChatState(categories,listOf(note))
        assertEquals(text,MarkdownWorkspace.parse(MarkdownWorkspace.render(state,"thoughts"),state).entries.single().text)
        val dir=Files.createTempDirectory("long-note-backup").toFile()
        try {
            val file=java.io.File(dir,"notes.zip")
            BackupArchive.write(file,state,"backup-password".toCharArray()) { error("No attachment") }
            val restored=BackupArchive.read(file,"backup-password".toCharArray(),dir)
            try { assertEquals(state.messages,restored.state.messages) } finally { restored.close() }
        } finally { dir.deleteRecursively() }
    }
    @Test fun editsSplitsAndMergesAcceptLongTextAndRejectOversizedMergesBeforeJoining() {
        val original=Message(text="An observation")
        val engine=ChatEngine(ChatState(categories,listOf(original)))
        val text="An observation. ".repeat(6000)
        engine.edit(original.id,text)
        val parts=engine.split(original.id,listOf(text.substring(0,40000),text.substring(40000)))
        assertEquals(text,parts.joinToString("") { it.text })
        assertEquals(parts.joinToString("\n\n") { it.text },engine.merge(parts.map { it.id }.toSet()).text)
        engine.state=engine.state.copy(messages=listOf(Message(id="one",text="a".repeat(MAX_MESSAGE_CHARS)),Message(id="two",text="b")))
        assertThrows(IllegalArgumentException::class.java) { engine.merge(setOf("one","two")) }
    }
    @Test fun pagingAndImportChunksKeepEmojiAtBoundariesIntact() {
        val text="a".repeat(MAX_MESSAGE_CHARS-1)+"📝"+"b".repeat(20)
        val chunks=ChatImport.parse(text).entries
        assertTrue(chunks.all { it.text.length<=MAX_MESSAGE_CHARS })
        assertEquals(text,chunks.joinToString("") { it.text })
        val pages=(0 until TextPages.count(text)).map { TextPages.page(text,it) }
        assertEquals(text,pages.joinToString(""))
        assertTrue(pages.all { it.length<=TEXT_PREVIEW_CHARS && !it.first().isLowSurrogate() && !it.last().isHighSurrogate() })
        val edge="a".repeat(TextPages.PAGE_CHARS-1)+"📝"+"b".repeat(TextPages.PAGE_CHARS)
        assertEquals(edge,(0 until TextPages.count(edge)).joinToString("") { TextPages.page(edge,it) })
        assertEquals(500,WidgetTasks.project(ChatState(categories,listOf(Message(text=text,category="todos")))).single().text.length)
    }
}
