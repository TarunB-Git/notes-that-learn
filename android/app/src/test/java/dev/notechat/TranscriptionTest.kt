package dev.notechat

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class TranscriptionTest {
    @Test fun encryptedResultsAreBoundToTheRightKeyAndRecording() {
        val context=RuntimeEnvironment.getApplication(); val key=ByteArray(32) { 7 }
        TranscriptionStore.write(context,"one","recording",key,"Private dentist appointment")
        assertEquals("Private dentist appointment",TranscriptionStore.read(context,"one","recording",key))
        assertTrue(File(context.filesDir,"transcripts").listFiles()!!.filter { it.extension=="enc" }.none { it.readBytes().toString(Charsets.ISO_8859_1).contains("Private dentist") })
        assertNull(TranscriptionStore.read(context,"one","different-recording",key))
        assertThrows(Exception::class.java) { TranscriptionStore.read(context,"one","recording",ByteArray(32) { 8 }) }
        TranscriptionStore.remove(context,"one","recording")
        assertNull(TranscriptionStore.read(context,"one","recording",key))
    }
    @Test fun savedTranscriptsMergeOnlyOnceWithoutImplicitApprovals() {
        val message=Message(id="voice",text="Edited caption",category="thoughts",approved=true,attachment=Attachment("audio","Voice","audio/mp4",100))
        val engine=ChatEngine(ChatState(listOf(Category("thoughts","Thoughts","Notes")),listOf(message)))
        assertTrue(engine.applyTranscript("voice","audio","Call the dentist"))
        assertEquals("Edited caption\n\nCall the dentist",engine.state.messages.single().text)
        assertTrue(engine.state.messages.single().transcribed)
        assertFalse(engine.state.messages.single().approved)
        assertFalse(engine.applyTranscript("voice","audio","Duplicate result"))
        assertEquals("Edited caption\n\nCall the dentist",engine.state.messages.single().text)
        assertTrue(StateJson.decode(StateJson.encode(engine.state)).messages.single().transcribed)
    }
    @Test fun delayedResultsIgnoreDeletedNotesAndReplacementAttachments() {
        val message=Message(id="voice",attachment=Attachment("new-audio","Voice","audio/mp4",100),chatId="private")
        val engine=ChatEngine(ChatState(emptyList(),listOf(message),chats=listOf(Chat("private","Private"))))
        assertFalse(engine.applyTranscript("voice","old-audio","Old recording"))
        engine.delete(setOf("voice"))
        assertFalse(engine.applyTranscript("voice","new-audio","Deleted recording"))
        engine.restore("voice")
        assertTrue(engine.applyTranscript("voice","new-audio","Private recording"))
        assertEquals("private",engine.state.messages.single().chatId)
        assertFalse(engine.state.messages.single().trainable())
    }
}
