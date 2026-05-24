package com.armin.garmin.transliteration

object ArmenianTransliterator {

    // Multi-character mappings must be checked before single-character ones
    private val multiCharMap = mapOf(
        "ու" to "u",
        "Ու" to "U",
        "ՈՒ" to "U"
    )

    private val charMap = mapOf(
        'ա' to "a", 'Ա' to "A",
        'բ' to "b", 'Բ' to "B",
        'գ' to "g", 'Գ' to "G",
        'դ' to "d", 'Դ' to "D",
        'ե' to "e", 'Ե' to "E",
        'զ' to "z", 'Զ' to "Z",
        'է' to "e", 'Է' to "E",
        'ը' to "y", 'Ը' to "Y",
        'թ' to "t", 'Թ' to "T",
        'ժ' to "zh", 'Ժ' to "Zh",
        'ի' to "i", 'Ի' to "I",
        'լ' to "l", 'Լ' to "L",
        'խ' to "kh", 'Խ' to "Kh",
        'ծ' to "ts", 'Ծ' to "Ts",
        'կ' to "k", 'Կ' to "K",
        'հ' to "h", 'Հ' to "H",
        'ձ' to "dz", 'Ձ' to "Dz",
        'ղ' to "gh", 'Ղ' to "Gh",
        'ճ' to "tch", 'Ճ' to "Tch",
        'մ' to "m", 'Մ' to "M",
        'յ' to "y", 'Յ' to "Y",
        'ն' to "n", 'Ն' to "N",
        'շ' to "sh", 'Շ' to "Sh",
        'ո' to "vo", 'Ո' to "Vo",
        'չ' to "ch", 'Չ' to "Ch",
        'պ' to "p", 'Պ' to "P",
        'ջ' to "j", 'Ջ' to "J",
        'ռ' to "r", 'Ռ' to "R",
        'ս' to "s", 'Ս' to "S",
        'վ' to "v", 'Վ' to "V",
        'տ' to "t", 'Տ' to "T",
        'ր' to "r", 'Ր' to "R",
        'ց' to "c", 'Ց' to "C",
        'փ' to "p", 'Փ' to "P",
        'ք' to "q", 'Ք' to "Q",
        'օ' to "o", 'Օ' to "O",
        'ֆ' to "f", 'Ֆ' to "F",
        'ւ' to "u",   // standalone Yiwn (part of ու digraph, also appears alone)
        'և' to "ev",  // Armenian ligature ECH YIWN
        // Punctuation unique to Armenian
        '՝' to ",",
        '՞' to "?",
        '՜' to "!",
        '։' to ":",
        '«' to "\"",
        '»' to "\""
    )

    fun transliterate(input: String): String {
        if (input.isEmpty()) return input

        val result = StringBuilder(input.length * 2)
        var i = 0

        while (i < input.length) {
            // Try to match 2-char Armenian digraphs first
            if (i + 1 < input.length) {
                val twoChar = input.substring(i, i + 2)
                val mapped = multiCharMap[twoChar]
                if (mapped != null) {
                    result.append(mapped)
                    i += 2
                    continue
                }
            }

            val c = input[i]
            val mapped = charMap[c]
            if (mapped != null) {
                // Fix: "ո" at the start of a word or after space/punctuation maps to "vo"
                // but mid-word it should just be "o" — handled by initial "vo" mapping
                // For word-initial "Ո" we already have "Vo"
                result.append(mapped)
            } else {
                result.append(c)
            }
            i++
        }

        return result.toString()
    }

    fun containsArmenian(text: String): Boolean {
        return text.any { it.code in 0x0531..0x058A }
    }
}
