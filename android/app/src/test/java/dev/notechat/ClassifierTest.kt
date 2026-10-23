package dev.notechat

import org.junit.Assert.*
import org.junit.Test

class ClassifierTest {
    private val classes=listOf(Category("ideas","Ideas","Proposals",listOf("We could open a bookstore","What if the roof was a garden")),Category("todos","To-do","Actions",listOf("Download books for the flight","Book the dentist")),Category("questions","Questions","Questions",listOf("Why does this happen?","How does it work?")))
    @Test fun kotlinAndSqlUseCodeCategoryWithoutCatchingOrdinaryInstructions() {
        val classifier=TextClassifier(null)
        classifier.configure(classes+Category("junk","Junk / code","Code"),emptyList())
        listOf("val x = listOf(1,2,3).map { it * 2 }", "val x: Int = 2", "fun greet(name: String) = name",
            "suspend fun load() { return 1 }", "SELECT * FROM notes;", "select title, body from notes where id = 1",
            "INSERT INTO notes VALUES (1)", "UPDATE notes SET title = 'Hi'").forEach {
            assertEquals(it,"junk",classifier.classify(Message(text=it)).category)
        }
        listOf("Select a birthday gift from the shop", "Fun things to try", "Val will call tomorrow", "Update my set of flashcards").forEach {
            assertNotEquals(it,"Code or placeholder text.",classifier.classify(Message(text=it)).reason)
        }
        classifier.configure(classes+Category("junk","Junk / code","Code"),listOf(Message(text="SELECT * FROM notes;",category="ideas",approved=true)))
        assertEquals("ideas",classifier.classify(Message(text="SELECT * FROM notes;")).category)
    }
    @Test fun longNotesUseBoundedEncoderInputButFullTextForApprovedMatches() {
        var largest=0
        val encoder=object:SentenceEncoder {
            override val identity="fixture"
            override fun encode(text:String):FloatArray { largest=maxOf(largest,text.length); return FloatArray(384) }
        }
        val prefix="A long observation. ".repeat(4000)
        val idea=Message(text=prefix+"First ending",category="ideas",approved=true)
        val task=Message(text=prefix+"Second ending",category="todos",approved=true)
        val classifier=TextClassifier(encoder)
        classifier.configure(classes,listOf(idea,task))
        assertEquals("ideas",classifier.classify(idea).category)
        assertEquals("todos",classifier.classify(task).category)
        assertNotEquals("Matches a message you approved.",classifier.classify(Message(text=prefix+"Unapproved ending")).reason)
        assertTrue(largest<=CLASSIFICATION_CHARS)
    }
    @Test fun exactApprovedBoundaryBeatsStarterCuesAndRejectionSkipsIt() {
        val classifier=TextClassifier(null)
        classifier.configure(classes,listOf(Message(text="Why this game",category="todos",approved=true),Message(text="What color is this? Website",category="ideas",approved=true)))
        assertEquals("todos",classifier.classify(Message(text="Why this game")).category)
        assertEquals("ideas",classifier.classify(Message(text="What color is this? Website")).category)
        assertNotEquals("todos",classifier.classify(Message(text="Why this game",rejected=setOf("todos"))).category)
        assertNull(classifier.classify(Message(text="Anything",rejected=classes.map { it.id }.toSet())).category)
    }
    @Test fun approvalIsRequiredEvenWhenTheClassifierReceivedPendingMessages() {
        val classifier=TextClassifier(null)
        classifier.configure(classes,listOf(Message(text="Why this game",category="todos",approved=false)))
        assertEquals("questions",classifier.classify(Message(text="Why this game")).category)
    }
    @Test fun evaluatedTrainingPersistsHoldoutAssignmentsAndFittedWeights() {
        val classifier=TextClassifier(null)
        val labels=classes.flatMap { c -> (0..7).map { Message(text="${c.examples[0]} example $it",category=c.id,approved=true) } }
        classifier.configure(classes,labels)
        val first=classifier.train()
        assertEquals(3,first.heldOut)
        val split=classifier.splits()
        val persisted=classifier.learned()
        classifier.configure(classes,labels+classes.map { Message(text="${it.description} a new example",category=it.id,approved=true) },persisted,split)
        classifier.train()
        assertTrue(split.all { (key,value)->classifier.splits()[key]==value })
        assertEquals(24,split.size)
    }
    @Test fun wordPieceHandlesAccentCjkPunctuationAndUnknownWords() {
        val tokenizer=WordPiece(mapOf("cafe" to 5,"hello" to 6,"##s" to 7,"!" to 8,"中" to 9))
        assertArrayEquals(longArrayOf(5,6,7,8,9,100),tokenizer.tokens("Café hellos!中 unknown"))
    }
    @Test fun personalHeadCanOverrideBookmarkAndCodeStarterRules() {
        val classifier=TextClassifier(null)
        val choices=classes+listOf(Category("junk","Junk","Code",listOf("const count = 42;")),Category("links","Links","Bookmarks",listOf("https://example.org/books")))
        val labels=choices.flatMap { c -> (0..7).map { Message(text="${c.examples[0]} example $it",category=c.id,approved=true) } }
        classifier.configure(choices,labels); classifier.train()
        val fitted=classifier.learned()!!
        val controlled=fitted.copy(weights=fitted.weights.map { row -> row.map { 0f } },bias=listOf(10f,0f,0f,0f,0f))
        classifier.configure(choices,emptyList(),controlled)
        assertEquals("ideas",classifier.classify(Message(text="const prototype = 42; https://example.org")).category)
    }
}
