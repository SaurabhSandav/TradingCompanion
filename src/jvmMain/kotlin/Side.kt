internal enum class Side(val strValue: String) {
    Long("long"),
    Short("short");

    companion object {

        fun fromString(strValue: String) = when (strValue.lowercase()) {
            "long" -> Long
            "short" -> Short
            else -> error("Invalid side")
        }
    }
}
