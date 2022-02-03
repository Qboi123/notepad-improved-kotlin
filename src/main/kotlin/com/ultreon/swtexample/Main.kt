package com.ultreon.swtexample

import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.ImageLoader
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell
import java.util.*

class Main() {
    companion object {
        lateinit var instance: Main

        @JvmStatic
        fun main(vararg args: String) {
            val main = Main()
            main.postInit()
        }
    }

    // Components
    private var display: Display
    private var shell: Shell
    private var menuBar: Menu

    init {
        preInit()
        display = Display()

        initImages()
        initIcons()

        shell = Shell(display)
        shell.text = "Title"
//        shell.layout = /*Layout*/
//        shell.size = /*Point(x, y)*/

        menuBar = Menu(shell, SWT.BAR)
        initMenuBar(menuBar)
    }

    private fun preInit() {
        instance = this
    }

    private fun initImages() {

    }

    private fun initIcons() {

    }

    private fun initMenuBar(menu: Menu) {

    }

    private fun postInit() {
        shell.pack()
        shell.size = Point(600, 450)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        display.dispose()
    }

    fun loadMultiResIcon(path: String): Array<Image> {
        return Arrays.stream(ImageLoader().load(this::class.java.getResourceAsStream("/$path"))).map {
            Image(display, it)
        }.toList().toTypedArray()
    }

    fun loadImage(path: String): Image {
        var path1 = path
        if (!path1.endsWith(".png")) {
            path1 += ".png"
        }
        return Image(display, ImageLoader().load(this::class.java.getResourceAsStream("/$path1"))[0])
    }
}