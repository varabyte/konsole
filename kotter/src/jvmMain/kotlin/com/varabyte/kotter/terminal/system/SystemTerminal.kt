package com.varabyte.kotter.terminal.system

import com.varabyte.kotter.runtime.coroutines.*
import com.varabyte.kotter.runtime.terminal.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import org.jline.terminal.Terminal.Signal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.InfoCmp
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

/**
 * A [Terminal] implementation which interacts directly with the underlying system terminal.
 */
class SystemTerminal(
  private val onCtrlC: () -> Unit = { exitProcess(130) } // 130 == 128+2, where 2 == SIGINT
) : Terminal {
    private var previousCursorSetting: InfoCmp.Capability
    private val previousOut = System.out
    private val previousErr = System.err
    private var closed = false

    private val terminal = TerminalBuilder.builder()
        .system(true)
        // According to https://github.com/jline/jline3#jansi-vs-jna, both of these are equally valid libraries but
        // jansi is a bit smaller. Since we're not doing anything particularly complex here, we choose jansi.
        .jansi(true).jna(false)
        // Don't use JLine's virtual terminal - use ours! Because this is false, this builder will throw an exception
        // if the current terminal environment doesn't support standards we require. We can listen for that exception to
        // either halt the problem or start up a virtual terminal instead.
        .dumb(false)
        .build().apply {
            // Swallow keypresses - instead, we will handle them
            enterRawMode()

            // Handle Ctrl-C ourselves, because Windows otherwise swallows it
            // See also: https://github.com/jline/jline3/issues/822
            handle(Signal.INT) { onCtrlC() } 

            val disabledPrintStream = PrintStream(object : OutputStream() {
                override fun write(b: Int) = Unit
            })

            // Disable printlns, as they allow users to screw with assumptions that this library makes (e.g. inserting
            // in ANSI commands whose state we don't keep track of)
            System.setOut(disabledPrintStream)
            System.setErr(disabledPrintStream)

            // Hide the cursor; we'll handle it ourselves
            val restoreCursorCapabilities = listOf(
                InfoCmp.Capability.cursor_normal,
                InfoCmp.Capability.cursor_visible,
            )
            previousCursorSetting = restoreCursorCapabilities
                .firstOrNull { c -> getBooleanCapability(c) } ?: InfoCmp.Capability.cursor_normal
            puts(InfoCmp.Capability.cursor_invisible)
        }

    override val width: Int
        get() = terminal.width

    override val height: Int
        get() = terminal.height

    override fun write(text: String) {
        terminal.writer().print(text)
        terminal.writer().flush()
    }

    fun read(timeout: Long) = terminal.reader().read(timeout)

    private val charFlow: SharedFlow<Int> by lazy {
        flow {
            var quit = false
            val context = currentCoroutineContext()
            while (!quit && context.isActive) {
                try {
                    val c = terminal.reader().read(16)
                    if (closed) {
                        // terminal was just closed between this read and last read
                        quit = true
                    } else {
                        if (c >= 0) {
                            emit(c)
                        } else if (c == -1) {
                            quit = true
                        }
                    }
                } catch (ex: IOException) {
                    break
                }

                yield()
            }
        }.shareIn(CoroutineScope(KotterDispatchers.IO), SharingStarted.Lazily)
    }

    override fun read() = charFlow

    override fun close() {
        terminal.puts(previousCursorSetting)
        terminal.flush()
        terminal.close()

        System.setOut(previousOut)
        System.setErr(previousErr)

        closed = true
    }

    override fun clear() {
        terminal.puts(InfoCmp.Capability.clear_screen)
        terminal.writer().flush()
    }
}
