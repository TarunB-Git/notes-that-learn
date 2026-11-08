package dev.notechat

import java.util.UUID

/** Presentation belongs to the notebook; switching it never rewrites its notes. */
data class TableColumn(val id: String, val title: String)
data class NotebookLayout(val mode: String = "default", val columns: List<TableColumn> = listOf(TableColumn("note", "Note"), TableColumn("details", "Details"))) {
    fun checked(): NotebookLayout {
        require(mode in listOf("default", "notes", "list", "table")) { "Choose Notes, List or Table" }
        require(columns.size in 1..12 && columns.first().id == "note" && columns.map { it.id }.distinct().size == columns.size)
        require(columns.all { it.id.matches(Regex("[A-Za-z0-9-]{1,64}")) && it.title.isNotBlank() && it.title.length <= 60 })
        return this
    }
    fun withTitles(titles: List<String>): NotebookLayout = copy(columns = titles.mapIndexed { i, title ->
        TableColumn(columns.getOrNull(i)?.id ?: UUID.randomUUID().toString(), title.trim())
    }).checked()
}
fun ChatState.notebook(route: String): NotebookLayout = if(route.startsWith("chat:"))
    chats.firstOrNull { it.id == route.removePrefix("chat:") }?.layout ?: NotebookLayout()
else categories.firstOrNull { it.id == route }?.layout ?: NotebookLayout()
fun ChatState.isChecklist(message: Message): Boolean {
    val layout = notebook(message.chatId?.let { "chat:$it" } ?: message.category.orEmpty())
    return layout.mode == "list" || (layout.mode == "default" && message.chatId == null && message.category == "todos")
}
fun ChatEngine.setNotebook(route: String, layout: NotebookLayout) {
    layout.checked()
    if(route.startsWith("chat:")) {
        val id=route.removePrefix("chat:")
        require(state.chats.any { it.id==id })
        state=state.copy(chats=state.chats.map { if(it.id==id)it.copy(layout=layout) else it })
    } else {
        require(state.categories.any { it.id==route })
        state=state.copy(categories=state.categories.map { if(it.id==route)it.copy(layout=layout) else it })
    }
}
fun Message.searchText() = (listOf(text) + tableCells.values).filter(String::isNotBlank).joinToString("\n")

/** Results target the original attachment, never a newer recording or deleted note. */
fun ChatEngine.applyTranscript(id:String,attachment:String,text:String):Boolean {
    val current=state.messages.firstOrNull { it.id==id && it.deleted==null && it.attachment?.file==attachment } ?: return false
    if(current.transcribed)return false
    require(text.isNotBlank())
    val combined=if(current.text.isBlank())text else current.text+"\n\n"+text
    require(combined.length<=MAX_MESSAGE_CHARS)
    edit(id,combined,keepCategory=true)
    update(id) { it.copy(transcribed=true) }
    return true
}
