package com.example.transaction.ml

/**
 * A Kotlin implementation of the WordPiece tokenization algorithm for BERT-based models.
 */
class WordPieceTokenizer(private val vocabulary: Map<String, Int>) {
    private val unknownToken = "[UNK]"
    private val maxCharsPerWord = 100

    /**
     * Tokenizes a single word into WordPieces.
     */
    fun tokenize(text: String): List<String> {
        val outputTokens = mutableListOf<String>()

        if (text.length > maxCharsPerWord) {
            outputTokens.add(unknownToken)
            return outputTokens
        }

        var isBad = false
        var start = 0
        val subTokens = mutableListOf<String>()

        while (start < text.length) {
            var end = text.length
            var curSubStr: String? = null
            
            while (start < end) {
                var substr = text.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }
                
                if (vocabulary.containsKey(substr)) {
                    curSubStr = substr
                    break
                }
                end--
            }

            if (curSubStr == null) {
                isBad = true
                break
            }

            subTokens.add(curSubStr)
            start = end
        }

        if (isBad) {
            outputTokens.add(unknownToken)
        } else {
            outputTokens.addAll(subTokens)
        }

        return outputTokens
    }
    
    /**
     * Full BERT tokenization: Basic whitespace split + WordPiece.
     */
    fun tokenizeFull(text: String): List<Int> {
        val words = text.split(Regex("\\s+"))
        val tokens = mutableListOf<String>()
        tokens.add("[CLS]")
        
        for (word in words) {
            if (word.isEmpty()) continue
            tokens.addAll(tokenize(word.lowercase()))
        }
        
        tokens.add("[SEP]")
        
        return tokens.map { vocabulary[it] ?: vocabulary[unknownToken]!! }
    }
}
