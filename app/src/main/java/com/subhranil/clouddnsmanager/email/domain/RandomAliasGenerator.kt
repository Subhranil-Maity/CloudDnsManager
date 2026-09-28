package com.subhranil.clouddnsmanager.email.domain

import java.security.SecureRandom
import java.util.Random

/**
 * Generates readable, hard-to-guess alias local parts like `quiet-river-4821`:
 * adjective + noun + 4 digits, all lowercase ASCII, so the result is always a valid local part.
 *
 * About 190 x 190 x 10 000 ≈ 360 million combinations. [random] is a [SecureRandom] by default
 * so aliases can't be predicted from earlier ones; tests pass a seeded [Random].
 */
class RandomAliasGenerator(private val random: Random = SecureRandom()) {

    fun generate(): String {
        val adjective = ADJECTIVES[random.nextInt(ADJECTIVES.size)]
        val noun = NOUNS[random.nextInt(NOUNS.size)]
        val digits = random.nextInt(10_000).toString().padStart(4, '0')
        return "$adjective-$noun-$digits"
    }

    companion object {
        val ADJECTIVES: List<String> = listOf(
            "able", "amber", "ancient", "autumn", "azure", "bold", "brave", "breezy", "bright", "brisk",
            "broad", "bronze", "calm", "candid", "careful", "cheerful", "chilly", "clean", "clear", "clever",
            "cloudy", "coastal", "cool", "copper", "cosmic", "cozy", "crimson", "crisp", "curious", "daring",
            "dawn", "deep", "dusty", "eager", "early", "earthy", "easy", "electric", "elegant", "emerald",
            "epic", "even", "fair", "fancy", "fast", "fearless", "fierce", "fine", "firm", "floral",
            "fluffy", "flying", "foggy", "frosty", "gentle", "giant", "gifted", "glad", "golden", "graceful",
            "grand", "green", "happy", "hardy", "hazy", "hidden", "hollow", "honest", "humble", "icy",
            "idle", "indigo", "ivory", "jade", "jolly", "keen", "kind", "large", "lazy", "light",
            "little", "lively", "lone", "lucky", "lunar", "magic", "major", "mellow", "merry", "mighty",
            "mild", "minty", "misty", "modern", "modest", "morning", "mossy", "muted", "narrow", "neat",
            "nimble", "noble", "north", "odd", "olive", "open", "orange", "pale", "patient", "peaceful",
            "pink", "plain", "plucky", "polar", "polished", "proud", "purple", "quick", "quiet", "rapid",
            "rare", "ready", "red", "regal", "rising", "robust", "rocky", "rosy", "round", "royal",
            "rustic", "rusty", "sandy", "sharp", "shiny", "silent", "silver", "simple", "sleek", "sleepy",
            "slow", "small", "smooth", "snowy", "soft", "solar", "solid", "sonic", "spare", "spicy",
            "spring", "steady", "still", "stormy", "strong", "sturdy", "summer", "sunny", "super", "sweet",
            "swift", "tall", "tame", "tidy", "tiny", "tranquil", "true", "twilight", "urban", "vast",
            "velvet", "violet", "vivid", "warm", "wandering", "west", "wild", "windy", "winter", "wise",
            "witty", "wooden", "young", "zany", "zesty",
        )

        val NOUNS: List<String> = listOf(
            "acorn", "anchor", "apple", "arrow", "aspen", "badger", "bamboo", "banjo", "basil", "bay",
            "beacon", "bear", "beach", "birch", "bison", "bloom", "boat", "breeze", "brook", "butter",
            "cabin", "cactus", "canyon", "cedar", "cherry", "cliff", "cloud", "clover", "comet", "coral",
            "cotton", "creek", "crow", "dawn", "delta", "desert", "dolphin", "dove", "dragon", "dune",
            "eagle", "echo", "ember", "falcon", "fern", "field", "finch", "fire", "fjord", "flame",
            "flower", "forest", "fox", "frost", "garden", "gecko", "glacier", "glade", "grove", "harbor",
            "hawk", "hazel", "heron", "hill", "honey", "horizon", "island", "ivy", "jungle", "kettle",
            "kite", "koala", "lagoon", "lake", "lantern", "leaf", "lemon", "lily", "lion", "lotus",
            "lynx", "maple", "marble", "meadow", "mesa", "meteor", "mint", "moon", "moose", "moss",
            "mountain", "nebula", "nest", "oak", "oasis", "ocean", "orbit", "orchid", "otter", "owl",
            "panda", "path", "peach", "pearl", "pebble", "pepper", "pine", "planet", "plum", "pond",
            "poppy", "prairie", "puffin", "quartz", "rabbit", "rain", "raven", "reef", "ridge", "river",
            "robin", "rock", "rose", "sage", "salmon", "shadow", "shell", "shore", "sky", "sparrow",
            "spruce", "star", "stone", "storm", "stream", "summit", "sun", "swan", "thistle", "thunder",
            "tide", "tiger", "trail", "tree", "tulip", "tundra", "valley", "violet", "wave", "willow",
            "wind", "wolf", "wren", "yak", "zebra", "badge", "bell", "bridge", "candle", "canoe",
            "castle", "compass", "crystal", "feather", "harp", "jewel", "kayak", "ladder", "lark", "magnet",
            "mirror", "needle", "paper", "pillow", "pocket", "quill", "ribbon", "rocket", "saddle", "sail",
            "scroll", "signal", "spark", "spoon", "tower", "violin", "wagon", "whistle", "window", "yarn",
        )
    }
}
