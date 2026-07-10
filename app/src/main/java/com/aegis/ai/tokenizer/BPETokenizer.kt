package com.aegis.ai.tokenizer

class BPETokenizer(
    private val vocabSize: Int = 32000
) {
    companion object {
        fun createMinimal(): BPETokenizer = BPETokenizer(vocabSize = 100)
    }
    
    val eosTokenId: Int = vocabSize - 1
    val padTokenId: Int = vocabSize - 2
    val bosTokenId: Int = vocabSize - 3
    
    private val vocab = mutableMapOf<String, Int>()
    private val reverseVocab = mutableMapOf<Int, String>()
    
    init {
        // Initialize with basic tokens
        val commonWords = listOf(
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could", "should",
            "may", "might", "must", "can", "to", "of", "in", "for", "on", "with", "at",
            "by", "from", "as", "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "here", "there", "when",
            "where", "why", "how", "all", "each", "few", "more", "most", "other", "some",
            "such", "no", "nor", "not", "only", "own", "same", "so", "than", "too", "very",
            "just", "and", "but", "if", "or", "because", "as", "until", "while", "of",
            "at", "by", "for", "with", "about", "against", "between", "into", "through",
            "during", "before", "after", "above", "below", "to", "from", "up", "down", "in",
            "out", "on", "off", "over", "under", "again", "further", "then", "once"
        )
        
        commonWords.forEachIndexed { index, word ->
            vocab[word] = index
            reverseVocab[index] = word
        }
        
        // Add special tokens
        vocab["<eos>"] = eosTokenId
        reverseVocab[eosTokenId] = "<eos>"
        vocab["<pad>"] = padTokenId
        reverseVocab[padTokenId] = "<pad>"
        vocab["<bos>"] = bosTokenId
        reverseVocab[bosTokenId] = "<bos>"
    }
    
    fun encode(text: String): IntArray {
        if (text.isEmpty()) return intArrayOf()
        
        // Simple word-level tokenization
        val words = text.lowercase().split(Regex("\\s+"))
        val tokens = mutableListOf<Int>()
        
        for (word in words) {
            val tokenId = vocab[word]
            if (tokenId != null) {
                tokens.add(tokenId)
            } else {
                // Hash unknown words to a token ID within vocab range
                val hashToken = (word.hashCode() % (vocabSize - 10)).let { if (it < 0) it + vocabSize - 10 else it }
                tokens.add(hashToken)
            }
        }
        
        return tokens.toIntArray()
    }
    
    fun decode(tokens: IntArray): String {
        return tokens.map { token ->
            reverseVocab[token] ?: "?"
        }.joinToString(" ")
    }
}
