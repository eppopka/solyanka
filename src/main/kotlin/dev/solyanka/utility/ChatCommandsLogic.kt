/* SPDX-License-Identifier: GPL-3.0-only */
package dev.solyanka.utility

import kotlin.random.Random

data class ChatCommand(val name: String = "", val response: String = "")

data class ChatCommandsSettings(
  val prefix: String = "!",
  val commands: List<ChatCommand> = emptyList(),
  val publicChat: Boolean = true,
  val party: Boolean = true,
  val guild: Boolean = true,
  val officer: Boolean = true,
  val privateChat: Boolean = true,
  val coop: Boolean = true,
  val matchAnywhere: Boolean = false,
) {
  fun validate() {
    require(
      prefix.length in 1..4 &&
        prefix.none { it.isWhitespace() || it.isISOControl() || it == '/' || it == '§' },
    ) {
      "Prefix must contain 1–4 non-space characters (not /)"
    }
    require(commands.size <= 128)
    commands.forEach {
      require(
        it.name.length in 1..32 &&
          it.name.none { c -> c.isWhitespace() || c.isISOControl() || c == '/' || c == '§' },
      ) {
        "Command names must have 1–32 non-space characters (not /)"
      }
      require(it.response.length <= 160 && it.response.none { c -> c.isISOControl() || c == '§' }) {
        "Response must have at most 160 characters without control codes"
      }
    }
    require(commands.map { it.name.lowercase() }.distinct().size == commands.size) {
      "Command names must be unique"
    }
  }
}

data class IncomingCommandChat(val channel: String, val name: String, val text: String)

data class MatchedChatCommand(val command: ChatCommand? = null, val suggestion: String? = null)

/** Suppress only echoes of our own command-like replies, not manually typed commands. */
class ChatReplyEchoes {
  private data class Reply(val channel: String, val text: String, val time: Long)

  private val replies = mutableListOf<Reply>()

  // shortcut: a lost echo can hide identical self-input for 30s; upgrade if message IDs become
  // available.
  private fun prune(now: Long) {
    replies.removeAll { now - it.time > 30_000 || now < it.time }
  }

  fun remember(channel: String, text: String, now: Long) {
    prune(now)
    if (replies.size >= 256) replies.removeAt(0)
    replies.add(Reply(channel, text, now))
  }

  fun consume(channel: String, text: String, now: Long): Boolean {
    prune(now)
    val index = replies.indexOfFirst { it.channel == channel && it.text == text }
    if (index < 0) return false
    replies.removeAt(index)
    return true
  }
}

object ChatCommandsLogic {
  private val line =
    Regex(
      "^(?:(Party|Guild|Officer|Co-op) > |(From) )?(?:\\[[^]\\r\\n]{1,32}] )*([A-Za-z0-9_]{1,16})(?: \\[[^]\\r\\n]{1,32}])?: (.{1,256})$",
    )
  private val random = Regex("\\{randint\\((-?\\d{1,7}),\\s*(-?\\d{1,7})\\)}")

  fun parse(raw: String): IncomingCommandChat? {
    val m = line.matchEntire(ChatLogic.plain(raw)) ?: return null
    val channel =
      when (m.groupValues[1]) {
        "Party" -> "party"
        "Guild" -> "guild"
        "Officer" -> "officer"
        "Co-op" -> "coop"
        else -> if (m.groupValues[2].isNotEmpty()) "private" else "public"
      }
    return IncomingCommandChat(channel, m.groupValues[3], m.groupValues[4])
  }

  fun allowed(s: ChatCommandsSettings, channel: String) =
    when (channel) {
      "party" -> s.party
      "guild" -> s.guild
      "officer" -> s.officer
      "coop" -> s.coop
      "private" -> s.privateChat
      "public" -> s.publicChat
      else -> false
    }

  fun match(s: ChatCommandsSettings, text: String, allowSuggestion: Boolean): MatchedChatCommand? {
    if (s.prefix.isEmpty()) return null
    val positions =
      if (s.matchAnywhere)
        Regex("(?<!\\S)" + Regex.escape(s.prefix) + "(\\S+)").findAll(text).map { it.range.first }
      else sequenceOf(0)
    for (start in positions) {
      if (!text.startsWith(s.prefix, start)) continue
      val tail = text.substring(start + s.prefix.length).trim()
      if (tail.isEmpty()) continue
      val name = tail.takeWhile { !it.isWhitespace() }
      val argument = tail.substring(name.length).trim()
      if (allowSuggestion && name.equals("suggest", true) && argument.isNotEmpty())
        return MatchedChatCommand(suggestion = argument)
      if (!s.matchAnywhere && argument.isNotEmpty()) continue
      val command = s.commands.firstOrNull { it.name.equals(name, true) } ?: continue
      return MatchedChatCommand(command = command)
    }
    return null
  }

  fun response(
    s: ChatCommandsSettings,
    chat: IncomingCommandChat,
    randomGenerator: Random = Random.Default,
  ): String? {
    if (!allowed(s, chat.channel)) return null
    val command = match(s, chat.text, false)?.command ?: return null
    return formatResponse(command, chat.name, randomGenerator)
  }

  fun formatResponse(
    command: ChatCommand,
    name: String,
    randomGenerator: Random = Random.Default,
  ): String? {
    val formatted =
      random
        .replace(command.response.replace("{name}", name)) { result ->
          val low = result.groupValues[1].toLong()
          val high = result.groupValues[2].toLong()
          if (low > high || low < -1_000_000 || high > 1_000_000) result.value
          else randomGenerator.nextLong(low, high + 1).toString()
        }
        .trim()
    return formatted.takeIf { it.isNotEmpty() && it.length <= 200 && !it.startsWith('/') }
  }
}
