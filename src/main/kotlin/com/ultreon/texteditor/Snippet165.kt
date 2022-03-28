package com.ultreon.texteditor

import kotlin.jvm.JvmStatic
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.custom.CTabFolder
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.custom.CTabItem
import org.eclipse.swt.custom.CTabFolder2Adapter
import org.eclipse.swt.custom.CTabFolderEvent
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Text

/*
 * Create a CTabFolder with min and max buttons, as well as close button and 
 * image only on selected tab.
 *
 * For a list of all SWT example snippets see
 * http://dev.eclipse.org/viewcvs/index.cgi/%7Echeckout%7E/platform-swt-home/dev.html#snippets
 */
object Snippet165 {
    @JvmStatic
    fun main(args: Array<String>) {
        val display = Display()
        val image = Image(display, 16, 16)
        val gc = GC(image)
        gc.background = display.getSystemColor(SWT.COLOR_BLUE)
        gc.fillRectangle(0, 0, 16, 16)
        gc.background = display.getSystemColor(SWT.COLOR_YELLOW)
        gc.fillRectangle(3, 3, 10, 10)
        gc.dispose()
        val shell = Shell(display)
        shell.layout = GridLayout()
        val folder = CTabFolder(shell, SWT.BORDER)
        folder.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
        folder.simple = false
        folder.unselectedImageVisible = false
        folder.unselectedCloseVisible = false
        for (i in 0..7) {
            val item = CTabItem(folder, SWT.CLOSE)
            item.text = "Item $i"
            item.image = image
            val text = Text(
                folder, SWT.MULTI or SWT.V_SCROLL
                        or SWT.H_SCROLL
            )
            text.text = """
                Text for item $i
                
                one, two, three
                
                abcdefghijklmnop
                """.trimIndent()
            item.control = text
        }
        folder.minimizeVisible = true
        folder.maximizeVisible = true
        folder.addCTabFolder2Listener(object : CTabFolder2Adapter() {
            override fun minimize(event: CTabFolderEvent) {
                folder.minimized = true
                shell.layout(true)
            }

            override fun maximize(event: CTabFolderEvent) {
                folder.maximized = true
                folder.layoutData = GridData(
                    SWT.FILL, SWT.FILL, true,
                    true
                )
                shell.layout(true)
            }

            override fun restore(event: CTabFolderEvent) {
                folder.minimized = false
                folder.maximized = false
                folder.layoutData = GridData(
                    SWT.FILL, SWT.FILL, true,
                    false
                )
                shell.layout(true)
            }
        })
        shell.setSize(300, 300)
        shell.open()
        while (!shell.isDisposed) {
            if (!display.readAndDispatch()) display.sleep()
        }
        image.dispose()
        display.dispose()
    }
}