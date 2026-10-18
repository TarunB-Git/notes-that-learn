package dev.notechat

object TextPages {
    const val PAGE_CHARS = TEXT_PREVIEW_CHARS - 2
    fun count(text: String) = ((text.length + PAGE_CHARS - 1) / PAGE_CHARS).coerceAtLeast(1)
    fun page(text: String, index: Int): String {
        require(index in 0 until count(text))
        var start=index*PAGE_CHARS
        var end=((index+1)*PAGE_CHARS).coerceAtMost(text.length)
        if(start>0 && start<text.length && text[start].isLowSurrogate() && text[start-1].isHighSurrogate())start--
        if(end<text.length && end>0 && text[end-1].isHighSurrogate() && text[end].isLowSurrogate())end--
        return text.substring(start,end)
    }
}
