package com.ultreon.texteditor

import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.beans.PropertyVetoException
import javax.swing.*
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import kotlin.system.exitProcess

// This is example is from Kjell Dirdal.
// Referenced from http://www.javaworld.com/javaworld/jw-05-2001/jw-0525-mdi.html
class KjellDirdalNotepad : JFrame() {
    private val desktop = MDIDesktopPane()
    private val menuBar = JMenuBar()
    private val fileMenu = JMenu("File")
    private val newMenu = JMenuItem("New")
    private val scrollPane = JScrollPane()

    init {
        menuBar.add(fileMenu)
        menuBar.add(WindowMenu(desktop))
        fileMenu.add(newMenu)
        jMenuBar = menuBar
        title = "MDI Test"
        scrollPane.viewport.add(desktop)
        contentPane.layout = BorderLayout()
        contentPane.add(scrollPane, BorderLayout.CENTER)

        UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName());

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                exitProcess(0)
            }
        })
        newMenu.addActionListener { desktop.add(TextFrame()) }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val notepad = KjellDirdalNotepad()
            notepad.setSize(600, 400)
            notepad.isVisible = true
        }
    }
}

internal class TextFrame : JInternalFrame() {
    private val textArea = JTextArea()
    private val scrollPane = JScrollPane()

    init {
        setSize(200, 300)
        setTitle("Edit Text")
        isMaximizable = true
        isIconifiable = true
        isClosable = true
        isResizable = true
        scrollPane.viewport.add(textArea)
        contentPane.layout = BorderLayout()
        contentPane.add(scrollPane, BorderLayout.CENTER)
    }
}

/**
 * An extension of WDesktopPane that supports often used MDI functionality. This
 * class also handles setting scroll bars for when windows move too far to the
 * left or bottom, providing the MDIDesktopPane is in a ScrollPane.
 */
internal class MDIDesktopPane : JDesktopPane() {
    private val manager: MDIDesktopManager = MDIDesktopManager(this)

    init {
        desktopManager = manager
        dragMode = OUTLINE_DRAG_MODE
    }

    override fun setBounds(x: Int, y: Int, w: Int, h: Int) {
        super.setBounds(x, y, w, h)
        checkDesktopSize()
    }

    fun add(frame: JInternalFrame): Component {
        val array = allFrames
        val p: Point
        var w: Int
        var h: Int
        val retval = super.add(frame)
        checkDesktopSize()
        if (array.isNotEmpty()) {
            p = array[0].location
            p.x = p.x + FRAME_OFFSET
            p.y = p.y + FRAME_OFFSET
        } else {
            p = Point(0, 0)
        }
        frame.setLocation(p.x, p.y)
        if (frame.isResizable) {
            w = width - width / 3
            h = height - height / 3
            if (w < frame.minimumSize.getWidth()) w = frame.minimumSize.getWidth().toInt()
            if (h < frame.minimumSize.getHeight()) h = frame.minimumSize.getHeight().toInt()
            frame.setSize(w, h)
        }
        moveToFront(frame)
        frame.isVisible = true
        try {
            frame.isSelected = true
        } catch (e: PropertyVetoException) {
            frame.toBack()
        }
        return retval
    }

    override fun remove(c: Component) {
        super.remove(c)
        checkDesktopSize()
    }

    /**
     * Cascade all internal frames
     */
    fun cascadeFrames() {
        var x = 0
        var y = 0
        val allFrames = allFrames
        manager.setNormalSize()
        val frameHeight = bounds.height - 5 - allFrames.size * FRAME_OFFSET
        val frameWidth = bounds.width - 5 - allFrames.size * FRAME_OFFSET
        for (i in allFrames.indices.reversed()) {
            allFrames[i].setSize(frameWidth, frameHeight)
            allFrames[i].setLocation(x, y)
            x += FRAME_OFFSET
            y += FRAME_OFFSET
        }
    }

    /**
     * Tile all internal frames
     */
    fun tileFrames() {
        val allFrames: Array<out JInternalFrame> = allFrames
        manager.setNormalSize()
        val frameHeight = bounds.height / allFrames.size
        var y = 0
        for (i in allFrames.indices) {
            allFrames[i].setSize(bounds.width, frameHeight)
            allFrames[i].setLocation(0, y)
            y += frameHeight
        }
    }

    /**
     * Sets all component size properties ( maximum, minimum, preferred) to the
     * given dimension.
     */
    private fun setAllSize(d: Dimension?) {
        minimumSize = d
        maximumSize = d
        preferredSize = d
    }

    /**
     * Sets all component size properties ( maximum, minimum, preferred) to the
     * given width and height.
     */
    fun setAllSize(width: Int, height: Int) {
        setAllSize(Dimension(width, height))
    }

    private fun checkDesktopSize() {
        if (parent != null && isVisible) manager.resizeDesktop()
    }

    companion object {
        private const val FRAME_OFFSET = 20
    }
}

/**
 * Private class used to replace the standard DesktopManager for JDesktopPane.
 * Used to provide scrollbar functionality.
 */
internal class MDIDesktopManager(private val desktop: MDIDesktopPane) : DefaultDesktopManager() {
    override fun endResizingFrame(f: JComponent) {
        super.endResizingFrame(f)
        resizeDesktop()
    }

    override fun endDraggingFrame(f: JComponent) {
        super.endDraggingFrame(f)
        resizeDesktop()
    }

    fun setNormalSize() {
        val scrollPane = scrollPane
        val x = 0
        val y = 0
        val scrollInsets = scrollPaneInsets
        if (scrollPane != null) {
            val d = scrollPane.visibleRect.size
            if (scrollPane.border != null) {
                d.setSize(
                    d.getWidth() - scrollInsets.left - scrollInsets.right, d.getHeight()
                            - scrollInsets.top - scrollInsets.bottom
                )
            }
            d.setSize(d.getWidth() - 20, d.getHeight() - 20)
            desktop.setAllSize(x, y)
            scrollPane.invalidate()
            scrollPane.validate()
        }
    }

    private val scrollPaneInsets: Insets
        get() {
            val scrollPane = scrollPane
            return if (scrollPane == null) Insets(0, 0, 0, 0) else scrollPane.border.getBorderInsets(scrollPane)
        }
    private val scrollPane: JScrollPane?
        get() {
            if (desktop.parent is JViewport) {
                val viewPort = desktop.parent as JViewport
                if (viewPort.parent is JScrollPane) return viewPort.parent as JScrollPane
            }
            return null
        }

    fun resizeDesktop() {
        var x = 0
        var y = 0
        val scrollPane = scrollPane
        val scrollInsets = scrollPaneInsets
        if (scrollPane != null) {
            val allFrames = desktop.allFrames
            for (i in allFrames.indices) {
                if (allFrames[i].x + allFrames[i].width > x) {
                    x = allFrames[i].x + allFrames[i].width
                }
                if (allFrames[i].y + allFrames[i].height > y) {
                    y = allFrames[i].y + allFrames[i].height
                }
            }
            val d = scrollPane.visibleRect.size
            if (scrollPane.border != null) {
                d.setSize(
                    d.getWidth() - scrollInsets.left - scrollInsets.right, d.getHeight()
                            - scrollInsets.top - scrollInsets.bottom
                )
            }
            if (x <= d.getWidth()) x = d.getWidth().toInt() - 20
            if (y <= d.getHeight()) y = d.getHeight().toInt() - 20
            desktop.setAllSize(x, y)
            scrollPane.invalidate()
            scrollPane.validate()
        }
    }
}

/**
 * Menu component that handles the functionality expected of a standard
 * "Windows" menu for MDI applications.
 */
internal class WindowMenu(private val desktop: MDIDesktopPane) : JMenu() {
    private val cascade = JMenuItem("Cascade")
    private val tile = JMenuItem("Tile")

    init {
        text = "Window"
        cascade.addActionListener { desktop.cascadeFrames() }
        tile.addActionListener { desktop.tileFrames() }
        addMenuListener(object : MenuListener {
            override fun menuCanceled(e: MenuEvent) {}
            override fun menuDeselected(e: MenuEvent) {
                removeAll()
            }

            override fun menuSelected(e: MenuEvent) {
                buildChildMenus()
            }
        })
    }

    /* Sets up the children menus depending on the current desktop state */
    private fun buildChildMenus() {
        var menu: ChildMenuItem
        val array = desktop.allFrames
        add(cascade)
        add(tile)
        if (array.isNotEmpty()) addSeparator()
        cascade.isEnabled = array.isNotEmpty()
        tile.isEnabled = array.isNotEmpty()
        var i = 0
        while (i < array.size) {
            menu = ChildMenuItem(array[i])
            menu.state = i == 0
            menu.addActionListener { ae ->
                val frame = (ae.source as ChildMenuItem).frame
                frame.moveToFront()
                try {
                    frame.isSelected = true
                } catch (e: PropertyVetoException) {
                    e.printStackTrace()
                }
            }
            menu.icon = array[i].frameIcon
            add(menu)
            i++
        }
    }

    /*
   * This JCheckBoxMenuItem descendant is used to track the child frame that
   * corresponds to a give menu.
   */
    internal inner class ChildMenuItem(val frame: JInternalFrame) : JCheckBoxMenuItem(frame.title)
}