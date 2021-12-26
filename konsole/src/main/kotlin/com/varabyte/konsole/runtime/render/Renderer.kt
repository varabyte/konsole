package com.varabyte.konsole.runtime.render

import com.varabyte.konsole.runtime.Session
import com.varabyte.konsole.runtime.internal.TerminalCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.NEWLINE_COMMAND
import com.varabyte.konsole.runtime.internal.ansi.commands.RESET_COMMAND
import com.varabyte.konsole.runtime.internal.text.MutableTextArea
import com.varabyte.konsole.runtime.text.TextArea

/**
 * A class responsible for executing some block of logic which contains render instructions, which ultimately modify an
 * internal [MutableTextArea].
 */
class Renderer(val app: Session) {
    private val _textArea = MutableTextArea()
    val textArea: TextArea = _textArea

    /** Append this command to the end of this block's text area */
    internal fun appendCommand(command: TerminalCommand) {
        _textArea.appendCommand(command)
    }

    internal fun render(block: RenderScope.() -> Unit) {
        _textArea.clear()
        RenderScope(this).apply {
            block()
            // Make sure we clear all state as we exit this block. This ensures that repaint passes don't carry
            // state leftover from its end back to the beginning.
            _textArea.appendCommand(RESET_COMMAND)
        }

        if (textArea.toRawText().lastOrNull() != '\n') {
            _textArea.appendCommand(NEWLINE_COMMAND)
        }
    }
}