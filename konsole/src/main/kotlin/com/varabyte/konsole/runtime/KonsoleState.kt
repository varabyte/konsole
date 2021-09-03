package com.varabyte.konsole.runtime

import com.varabyte.konsole.runtime.internal.KonsoleCommand
import com.varabyte.konsole.runtime.internal.ansi.commands.*

/**
 * Keep track of all state related commands which should be reapplied to the current block if the ansi terminal resets
 * itself.
 *
 * Unfortunately, when you need to reset a single value (say, foreground color), the ANSI standard doesn't provide a
 * scalpel - instead, it provides a nuke (clear EVERYTHING). Since Konsole embraces a hierarchical, nested API, e.g.
 *
 * ```
 * white(BG) {
 *   red {
 *     underline {
 *       text("Red underlined text on white")
 *     }
 *     text("Red text on white")
 *   }
 * }
 * ```
 *
 * In order to support resetting just a subset of text styles, we need to maintain a copy of the state ourselves. In
 * order to, say, remove a foreground color setting, what we're really doing is nuking everything and building the whole
 * state back up again.
 */
class KonsoleState internal constructor(internal val parent: KonsoleState? = null) {
    /** A collection of relevent ANSI styles.
     *
     * @param parentStyles If provided, it means this style should fall back to its parent's value when unset.
     */
    internal class Styles(val parentStyles: Styles? = null) {
        var fgColor: KonsoleCommand? = parentStyles?.fgColor
            set(value) { field = value ?: parentStyles?.fgColor }
        var bgColor: KonsoleCommand? = parentStyles?.bgColor
            set(value) { field = value ?: parentStyles?.bgColor }
        var underlined: KonsoleCommand? = parentStyles?.underlined
            set(value) { field = value ?: parentStyles?.underlined}
        var bolded: KonsoleCommand? = parentStyles?.bolded
            set(value) { field = value ?: parentStyles?.bolded}
        var struckThrough: KonsoleCommand? = parentStyles?.struckThrough
            set(value) { field = value ?: parentStyles?.struckThrough}
        var inverted: KonsoleCommand? = parentStyles?.inverted
            set(value) { field = value ?: parentStyles?.inverted}
    }

    /** Styles which are actively applied, and any text rendered right now would use them. */
    internal val applied: Styles = parent?.applied ?: Styles()
    /**
     * The current style based on commands received so far in the current state scope.
     *
     * They are worth being deferred in case they change before new text is ultimately received.
     */
    internal val deferred: Styles = Styles(parent?.deferred)

    fun applyTo(block: KonsoleBlock) {
        if (deferred.fgColor?.text !== applied.fgColor?.text) {
            applied.fgColor = deferred.fgColor
            block.appendCommand(applied.fgColor ?: FG_CLEAR_COMMAND)
        }
        if (deferred.bgColor?.text !== applied.bgColor?.text) {
            applied.bgColor = deferred.bgColor
            block.appendCommand(applied.bgColor ?: BG_CLEAR_COMMAND)
        }
        if (deferred.underlined?.text !== applied.underlined?.text) {
            applied.underlined = deferred.underlined
            block.appendCommand(applied.underlined ?: CLEAR_UNDERLINE_COMMAND)
        }
        if (deferred.bolded?.text !== applied.bolded?.text) {
            applied.bolded = deferred.bolded
            block.appendCommand(applied.bolded ?: CLEAR_BOLD_COMMAND)
        }
        if (deferred.struckThrough?.text !== applied.struckThrough?.text) {
            applied.struckThrough = deferred.struckThrough
            block.appendCommand(applied.struckThrough ?: CLEAR_STRIKETHROUGH_COMMAND)
        }
        if (deferred.inverted?.text !== applied.inverted?.text) {
            applied.inverted = deferred.inverted
            block.appendCommand(applied.inverted ?: CLEAR_INVERT_COMMAND)
        }
    }
}