package com.aegis.ai.tokenizer

class BPETokenizer {
    companion object {
        fun createMinimal(): BPETokenizer = BPETokenizer()
    }
    
    fun vocabSize(): Int = 32000
    
    fun encode(text: String): IntArray = text.split(" ").map { it.hashCode() }.toIntArray()
    
    fun decode(tokens: IntArray): String = tokens.joinToString(" ") { it.toString() }
}
