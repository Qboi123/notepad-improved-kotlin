package com.ultreon.texteditor

import org.apache.commons.lang3.StringUtils
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Device
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.FontData
import org.eclipse.swt.widgets.Display
import java.util.*
import kotlin.collections.HashMap

/** [SWT] font related utility methods.  */
object SWTFontUtils {
    /** Cache: mapping from SWT devices to their monospaced fonts.  */
    private val MONOSPACED_FONTS: MutableMap<Device, Font> = HashMap<Device, Font>()// Get current display.

    // Forward request.
    /**
     * Returns the monospaced font for the current display. The font will
     * automatically be disposed once the display is disposed.
     *
     *
     * This method is thread safe.
     *
     * @return The monospaced font for the current display.
     * @throws IllegalStateException If the method is not invoked from a SWT
     * UI thread.
     */
    val monospacedFont: Font
        get() {
            synchronized(MONOSPACED_FONTS) {

                // Get current display.
                val display = Display.getCurrent()
                if (display == null) {
                    val msg = "Must be invoked for a SWT UI thread."
                    throw IllegalStateException(msg)
                }

                // Forward request.
                return getMonospacedFont(display)
            }
        }

    /**
     * Creates a monospaced font for the given display. The font will
     * automatically be disposed once the display is disposed.
     *
     *
     * This method is thread safe.
     *
     * @param display The display for which to create a monospaced font.
     * @return A monospaced font for the given display.
     */
    fun getMonospacedFont(display: Display): Font {
        synchronized(MONOSPACED_FONTS) {

            // Based on class 'org.eclipse.jface.resource.FontRegistry' and
            // resources 'org.eclipse.jface.resource/jfacefonts*.properties'
            // from 'org.eclipse.jface' plug-in (version 3.9.1).

            // Use cache if possible.
            val cachedFont = MONOSPACED_FONTS[display]
            if (cachedFont != null) return cachedFont

            // Get operating system and windowing system names.
            var os = System.getProperty("os.name")
            var version = System.getProperty("os.version")
            println(version)
            println(os)
            var ws: String = SWT.getPlatform()
            os = StringUtils.deleteWhitespace(os).lowercase(Locale.US)
            ws = StringUtils.deleteWhitespace(ws).lowercase(Locale.US)

            // Get names to check, in order from specific to generic.
            val names = arrayOf(os + "_" + ws, os, "")

            println("names = ${names.toList()}")

            // Get font data texts for platform.
            var fontDataTxts: Array<String>? = null
            for (name: String in names) {
                println("os = ${os}")
                println("name = ${name}")
                if (name == "aix") {
                    fontDataTxts = arrayOf("adobe-courier|normal|12")
                    break
                } else if (name == "hp-ux") {
                    fontDataTxts = arrayOf("adobe-courier|normal|14")
                    break
                } else if (name == "linux_gtk") {
                    fontDataTxts = arrayOf("Monospace|normal|10")
                    break
                } else if (name == "linux") {
                    fontDataTxts = arrayOf("adobe-courier|normal|12")
                    break
                } else if (name == "macosx") {
                    println("Mac OS X")
                    fontDataTxts = arrayOf(
                        "Monaco|normal|11",
                        "Courier|normal|12",
                        "Courier New|normal|12"
                    )
                    break
                } else if (name == "sunos" || name == "solaris") {
                    println("SunOS / Solaris")
                    fontDataTxts = arrayOf("adobe-courier|normal|12")
                    break
                } else if (name == "windows98") {
                    println("Windows 98")
                    fontDataTxts = arrayOf(
                        "Courier New|normal|10",
                        "Courier|normal|10",
                        "Lucida Console|normal|9"
                    )
                    break
                } else if (name == "windowsnt") {
                    println("Windows NT")
                    fontDataTxts = arrayOf(
                        "Courier New|normal|10",
                        "Courier|normal|10",
                        "Lucida Console|normal|9"
                    )
                    break
                } else if (name == "windows2000") {
                    println("Windows 2000")
                    fontDataTxts = arrayOf(
                        "Courier New|normal|10",
                        "Courier|normal|10",
                        "Lucida Console|normal|9"
                    )
                    break
                } else if (name == "windowsxp") {
                    println("Windows XP")
                    fontDataTxts = arrayOf(
                        "Courier New|normal|10",
                        "Courier|normal|10",
                        "Lucida Console|normal|9"
                    )
                    break
                } else if (name == "windowsvista") {
                    println("Windows Vista")
                    fontDataTxts = arrayOf(
                        "Consolas|normal|10",
                        "Courier New|normal|10"
                    )
                    break
                } else if (name == "windows7") {
                    println("Windows 7")
                    fontDataTxts = arrayOf(
                        "Consolas|normal|10",
                        "Courier New|normal|10"
                    )
                    break
                } else if (name == "windows8") {
                    println("Windows 8")
                    fontDataTxts = arrayOf(
                        "Consolas|normal|10",
                        "Courier New|normal|10"
                    )
                    break
                } else if (name == "windows10") {
                    println("Windows 10")
                    fontDataTxts = arrayOf(
                        "Consolas|normal|10",
                        "Courier New|normal|10"
                    )
                    break
                } else if (name == "windows11") {
                    println("Windows 11 !!!")
                    fontDataTxts = arrayOf(
                        "Lucida Console|normal|10",
                        "Consolas|normal|10",
                        "Courier New|normal|10"
                    )
                    break
                } else if (name == "") {
                    fontDataTxts = arrayOf(
                        "Courier New|normal|10",
                        "Courier|normal|10",
                        "b&h-lucidabright|normal|9"
                    )
                    break
                }
            }
            if (fontDataTxts == null) {
                // Can't happen, but silences a warning.
                throw AssertionError()
            }

            // Convert texts to font data.
            val fontDatas: Array<FontData?> = arrayOfNulls<FontData>(fontDataTxts.size)
            for (i in fontDatas.indices) {
                // Find splitters.
                val txt = fontDataTxts[i]
                val bar2 = txt.lastIndexOf('|')
//                Assert.assertTrue(bar2 != -1)
                val bar1 = txt.lastIndexOf('|', bar2 - 1)
//                Assert.assertTrue(bar1 != -1)

                // Get font name.
                val name = txt.substring(0, bar1)
//                Assert.assertTrue(name.length > 0)

                // Get font style.
                val styles = txt.substring(bar1 + 1, bar2).split(",").toTypedArray()
                var style = 0
                for (s: String in styles) {
                    if ((s == "normal")) {
                        style = style or SWT.NORMAL
                    } else if ((s == "bold")) {
                        style = style or SWT.BOLD
                    } else if ((s == "italic")) {
                        style = style or SWT.ITALIC
                    } else {
                        throw RuntimeException("Invalid style: $s")
                    }
                }

                // Get font height.
                val height = txt.substring(bar2 + 1).toInt()

                // Create and add font date.
                fontDatas[i] = FontData(name, height, style)
            }

            // Create font.
            val font = Font(display, fontDatas)

            // Register dispose callback, to dispose of the font once the
            // display is disposed.
            display.disposeExec {
                synchronized(MONOSPACED_FONTS) {
                    MONOSPACED_FONTS.remove(display)
                    font.dispose()
                }
            }

            // Add to cache.
            MONOSPACED_FONTS[display] = font

            // Return the new font.
            return font
        }
    }
}