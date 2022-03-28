package com.ultreon.texteditor

import org.apache.commons.lang3.SystemUtils
import org.eclipse.jface.action.StatusLineManager
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.window.ApplicationWindow
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.ShellAdapter
import org.eclipse.swt.events.ShellEvent
import org.eclipse.swt.graphics.*
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.*
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*


open class Main(file: File?) {
    companion object {
        private val windows: MutableList<Main> = mutableListOf()
        private val display = Display()
        lateinit var instance: Main

        @JvmStatic
        fun main(vararg args: String) {
            var file: File? = null

            if (args.size == 1) {
                val filePath = args[0]
                file = File(filePath)
            }

            newWindow(file)

            while (windows.isNotEmpty()) {
                if (!display.readAndDispatch()) {
                    display.sleep()
                }
            }
            display.dispose()
        }

        fun newWindow(file: File?) {
            val main = Main(file)
            main.postInit()
            windows.add(main)
        }
    }

    private var statusbar: Boolean = true
        set(value) {
            field = value
            value.also { state ->
                status.visible = state

                textEditor.layoutData = FormData().also {
                    it.left = FormAttachment(0)
                    it.right = FormAttachment(100)
                    if (state) {
                        it.bottom = FormAttachment(100, -17)
                    } else {
                        it.bottom = FormAttachment(100)
                    }
                    it.top = FormAttachment(0)
                }
                textEditor.update()
                shell.requestLayout()
            }
        }
    private var wordWrap: Boolean = false
        set(value) {
            field = value
            textEditor.wordWrap = value
        }

    // Components
    private var shell: Shell
    private var appWin: ApplicationWindow
    private var slm: StatusLineManager
    private var status: Label
    private var menuBar: Menu
    private var textEditor: StyledText

    // File management
    private var openDialog: FileDialog
    private var saveDialog: FileDialog
    private var fontDialog: FontDialog

    private var isChanged: Boolean = false
    set(value) {
        field = value
        if (value) status.text = "Unsaved changes"
        else status.text = "All changes saved"
    }
    private var saveLocation: File? = null

    private lateinit var newMenuItem: MenuItem
    private lateinit var newWindowMenuItem: MenuItem
    private lateinit var openMenuItem: MenuItem
    private lateinit var saveMenuItem: MenuItem
    private lateinit var saveAsMenuItem: MenuItem
    private lateinit var exitMenuItem: MenuItem

    private lateinit var newIcon: Image
    private lateinit var newWindowIcon: Image
    private lateinit var openIcon: Image
    private lateinit var saveIcon: Image
    private lateinit var saveAsIcon: Image
    private lateinit var exitIcon: Image

    // Editing
    private lateinit var cutMenuItem: MenuItem
    private lateinit var copyMenuItem: MenuItem
    private lateinit var pasteMenuItem: MenuItem
    private lateinit var deleteMenuItem: MenuItem
    private lateinit var fontMenuItem: MenuItem
    private lateinit var selectAllMenuItem: MenuItem

    private lateinit var cutIcon: Image
    private lateinit var copyIcon: Image
    private lateinit var pasteIcon: Image
    private lateinit var deleteIcon: Image
    private lateinit var fontIcon: Image
    private lateinit var selectIcon: Image
    private lateinit var webIcon: Image

    // View
    private lateinit var wordWrapMenuItem: MenuItem
    private lateinit var statusbarMenuItem: MenuItem
    private lateinit var zoomInMenuItem: MenuItem

    init {
        preInit()

        initImages()
        initIcons()

//        val layout = FillLayout()

        val layout = FormLayout()
        shell = Shell(display)
        shell.text = "Title"
        shell.layout = layout
//        shell.layout = layout

        textEditor = StyledText(shell, SWT.MULTI)
        textEditor.addModifyListener { isChanged = true }
        textEditor.font = SWTFontUtils.getMonospacedFont(display)

        val textData = FormData()
        textData.left = FormAttachment(0)
        textData.right = FormAttachment(100)
        textData.bottom = FormAttachment(100, -17)
        textData.top = FormAttachment(0)
        textEditor.layoutData = textData

        val label = Label(shell, SWT.BORDER)
        status = label
        isChanged = isChanged
        val labelData = FormData()
        labelData.left = FormAttachment(0)
        labelData.right = FormAttachment(100)
        labelData.bottom = FormAttachment(100)
        label.layoutData = labelData
        slm = StatusLineManager()

        appWin = object : ApplicationWindow(shell) {
            init {
                addStatusLine()
            }

            override fun createStatusLineManager(): StatusLineManager {
                return slm
            }
        }

        appWin.setStatus("test")

        file?.let { open(it) }

        // Dialogs
        openDialog = FileDialog(shell, SWT.OPEN)
        openDialog.filterNames = arrayOf("Text files", "All Files (*.*)")
        openDialog.filterExtensions = arrayOf("*.txt", "*.*") // Windows
        openDialog.fileName = "*.txt"

        saveDialog = FileDialog(shell, SWT.SAVE)
        saveDialog.filterNames = arrayOf("Text files", "All Files (*.*)")
        saveDialog.filterExtensions = arrayOf("*.txt", "*.*") // Windows
        saveDialog.fileName = "*.txt"

        fontDialog = FontDialog(shell)
        fontDialog.fontList = textEditor.font.fontData
        fontDialog.effectsVisible = false

        menuBar = Menu(shell, SWT.BAR)
        initMenuBar(menuBar)
        shell.menuBar = menuBar
    }

    private fun new() {
        if (userApprovesOverride()) {
            reset()
            onNew()
        }
    }

    /**
     * Exits the program.
     */
    private fun exit() {
        if (userApprovesOverride()) {
            shell.close()
        }
    }

    /**
     * Request to open a new file.
     */
    private fun open() {
        val ret: String = openDialog.open() ?: return
        if (userApprovesOverride()) {
            open(File(ret))
        }
    }

    private fun open(file: File) {
        reset()
        saveLocation = file
        onOpen(file)
        isChanged = false
    }

    /**
     * Checks for unsaved changes, and returns true if the user accepted the exit.
     *
     * @return true if the user didn't cancel it.
     */
    private fun userApprovesOverride(): Boolean {
        if (isChanged) {
            val messageBox = MessageBox(shell, SWT.ICON_WARNING or SWT.YES or SWT.NO or SWT.CANCEL)

            messageBox.text = "Warning"
            messageBox.message = "There are unsaved changes.\nDo you want to save them?"
            val buttonID = messageBox.open()
            when (buttonID) {
                SWT.YES -> {
                    save()
                    return true
                }
                SWT.NO -> {
                    return true
                }
                SWT.CANCEL -> {
                    return false
                }
            }
            println(buttonID)
        }
        return true
    }

    /**
     * Save file, request file location if there's not saved before.
     */
    private fun save() {
        if (saveLocation != null) {
            onSave()
            isChanged = false
        } else {
            saveAs()
        }
    }

    /**
     * Save as a new location.
     */
    private fun saveAs() {
        saveDialog.open()?.let {
            val file = File(it)
            saveLocation = file
            onSave()
            isChanged = false
        }
    }

    /**
     * Discards Changes.
     */
    private fun reset() {
        saveLocation = null
        onReset()
        isChanged = false
    }

    private fun onNew() {

    }

    private fun onOpen(file: File) {
        val bytes: ByteArray
        try {
            bytes = Files.readAllBytes(file.toPath())
        } catch (e: IOException) {
            errorMessage(e.javaClass.name, e.message?: "null")
            return
        }
        textEditor.text = try {
            String(bytes)
        } catch (e: IOException) {
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun errorMessage(title: String, message: String) {
        MessageDialog.openError(shell, title, message)
    }

    /**
     * Save event handler, here the file(s) will be saved.
     */
    private fun onSave() {
        Files.writeString(saveLocation!!.toPath(), textEditor.text, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
    }

    private fun onReset() {
        textEditor.text = ""
    }

    private fun preInit() {
        instance = this
    }

    private fun initImages() {

    }

    private fun initIcons() {
        newIcon = loadImage("Icons/Gui/New")
        newWindowIcon = loadImage("Icons/Gui/NewWindow")
        openIcon = loadImage("Icons/Gui/Open")
        saveIcon = loadImage("Icons/Gui/Save")
        saveAsIcon = loadImage("Icons/Gui/SaveAs")
        exitIcon = loadImage("Icons/Gui/Exit")

        cutIcon = loadImage("Icons/Gui/Cut")
        copyIcon = loadImage("Icons/Gui/Copy")
        pasteIcon = loadImage("Icons/Gui/Paste")
        deleteIcon = loadImage("Icons/Gui/Delete")
        fontIcon = loadImage("Icons/Gui/Font")
        selectIcon = loadImage("Icons/Gui/Select")
        webIcon = loadImage("Icons/Gui/Web")
    }

    private fun initMenuBar(menu: Menu) {
        val fileMenuItem = MenuItem(menu, SWT.CASCADE)
        fileMenuItem.text = "&File"
        val fileMenu = Menu(shell, SWT.DROP_DOWN)
        fileMenuItem.menu = fileMenu

        newMenuItem = MenuItem(fileMenu, SWT.PUSH)
        newMenuItem.text = "&New\tCtrl+N"
        newMenuItem.image = newIcon
        newMenuItem.accelerator = (SWT.CTRL + 'N'.code)
        newMenuItem.addSelectionListener(SelectionCommand(::new))

        newWindowMenuItem = MenuItem(fileMenu, SWT.PUSH)
        newWindowMenuItem.text = "New &Window\tCtrl+Shift+N"
        newWindowMenuItem.image = newWindowIcon
        newWindowMenuItem.accelerator = (SWT.CTRL + SWT.SHIFT + 'N'.code)
        newWindowMenuItem.addSelectionListener(SelectionCommand(::newWindow))

        openMenuItem = MenuItem(fileMenu, SWT.PUSH)
        openMenuItem.text = "&Open...\tCtrl+O"
        openMenuItem.image = openIcon
        openMenuItem.accelerator = (SWT.CTRL + 'O'.code)
        openMenuItem.addSelectionListener(SelectionCommand(::open))

        saveMenuItem = MenuItem(fileMenu, SWT.PUSH)
        saveMenuItem.text = "&Save\tCtrl+S"
        saveMenuItem.image = saveIcon
        saveMenuItem.accelerator = (SWT.CTRL + 'S'.code)
        saveMenuItem.addSelectionListener(SelectionCommand(::save))

        saveAsMenuItem = MenuItem(fileMenu, SWT.PUSH)
        saveAsMenuItem.text = "Save &As...\tCtrl+Shift+S"
        saveAsMenuItem.image = saveAsIcon
        saveAsMenuItem.accelerator = (SWT.CTRL + SWT.SHIFT + 'S'.code)
        saveAsMenuItem.addSelectionListener(SelectionCommand(::saveAs))

        MenuItem(fileMenu, SWT.SEPARATOR)

        exitMenuItem = MenuItem(fileMenu, SWT.PUSH)
        exitMenuItem.text = "E&xit${if (SystemUtils.IS_OS_WINDOWS) "\tAlt+F4" else ""}"
        exitMenuItem.image = exitIcon
        exitMenuItem.addSelectionListener(SelectionCommand(::exit))
        
        val editMenuItem = MenuItem(menu, SWT.CASCADE)
        editMenuItem.text = "&Edit"
        val editMenu = Menu(shell, SWT.DROP_DOWN)
        editMenuItem.menu = editMenu

        cutMenuItem = MenuItem(editMenu, SWT.PUSH)
        cutMenuItem.text = "Cu&t\tCtrl+X"
        cutMenuItem.image = cutIcon
        cutMenuItem.accelerator = (SWT.CTRL + 'X'.code)
        cutMenuItem.addSelectionListener(SelectionCommand(textEditor::cut))

        copyMenuItem = MenuItem(editMenu, SWT.PUSH)
        copyMenuItem.text = "&Copy\tCtrl+C"
        copyMenuItem.image = copyIcon
        copyMenuItem.addSelectionListener(SelectionCommand(textEditor::copy))

        pasteMenuItem = MenuItem(editMenu, SWT.PUSH)
        pasteMenuItem.text = "&Paste\tCtrl+V"
        pasteMenuItem.image = pasteIcon
        pasteMenuItem.addSelectionListener(SelectionCommand(textEditor::paste))

        deleteMenuItem = MenuItem(editMenu, SWT.PUSH)
        deleteMenuItem.text = "&Delete\tDel"
        deleteMenuItem.image = deleteIcon
        deleteMenuItem.addSelectionListener(SelectionCommand(::deleteSelection))

        MenuItem(editMenu, SWT.SEPARATOR)

        selectAllMenuItem = MenuItem(editMenu, SWT.PUSH)
        selectAllMenuItem.text = "Select &All\tCtrl+A"
        selectAllMenuItem.image = selectIcon
        selectAllMenuItem.accelerator = (SWT.CTRL + 'a'.code)
        selectAllMenuItem.addSelectionListener(SelectionCommand(textEditor::selectAll))

        MenuItem(editMenu, SWT.SEPARATOR)

        fontMenuItem = MenuItem(editMenu, SWT.PUSH)
        fontMenuItem.text = "&Font"
        fontMenuItem.image = fontIcon
        fontMenuItem.addSelectionListener(SelectionCommand(::editFont))

        MenuItem(editMenu, SWT.SEPARATOR)

        val searchMenuItem = MenuItem(editMenu, SWT.CASCADE)
        searchMenuItem.text = "&Search With"
        val searchMenu = Menu(shell, SWT.DROP_DOWN)
        searchMenuItem.menu = searchMenu

        val googleMenuItem = MenuItem(searchMenu, SWT.PUSH)
        googleMenuItem.text = "&Google"
        googleMenuItem.image = webIcon
        googleMenuItem.addSelectionListener(SelectionCommand {
            val query = URLEncoder.encode(textEditor.selectionText, Charsets.UTF_8)
            Desktop.getDesktop().browse(URI("https://google.com/search?q=$query"))
        })

        val youtubeMenuItem = MenuItem(searchMenu, SWT.PUSH)
        youtubeMenuItem.text = "&YouTube"
        youtubeMenuItem.image = webIcon
        youtubeMenuItem.addSelectionListener(SelectionCommand {
            val query = URLEncoder.encode(textEditor.selectionText, Charsets.UTF_8)
            Desktop.getDesktop().browse(URI("https://youtube.com/search?q=$query"))
        })

        val duckduckgoMenuItem = MenuItem(searchMenu, SWT.PUSH)
        duckduckgoMenuItem.text = "&DuckDuckGo"
        duckduckgoMenuItem.image = webIcon
        duckduckgoMenuItem.addSelectionListener(SelectionCommand {
            val query = URLEncoder.encode(textEditor.selectionText, Charsets.UTF_8)
            Desktop.getDesktop().browse(URI("https://duckduckgo.com/?q=$query&t=h_&ia=web"))
        })

        val bingMenuItem = MenuItem(searchMenu, SWT.PUSH)
        bingMenuItem.text = "&Bing"
        bingMenuItem.image = webIcon
        bingMenuItem.addSelectionListener(SelectionCommand {
            val query = URLEncoder.encode(textEditor.selectionText, Charsets.UTF_8)
            Desktop.getDesktop().browse(URI("https://bing.com/search?q=$query"))
        })

        val viewMenuItem = MenuItem(menu, SWT.CASCADE)
        viewMenuItem.text = "&View"
        val viewMenu = Menu(shell, SWT.DROP_DOWN)
        viewMenuItem.menu = viewMenu

        wordWrapMenuItem = MenuItem(viewMenu, SWT.CHECK)
        wordWrapMenuItem.text = "&Word Wrap"
        wordWrapMenuItem.addSelectionListener(SelectionCommand { wordWrap = !wordWrap })

        statusbarMenuItem = MenuItem(viewMenu, SWT.CHECK)
        statusbarMenuItem.text = "&Statusbar"
        statusbarMenuItem.selection = this.statusbar
        statusbarMenuItem.addSelectionListener(SelectionCommand { statusbar = statusbarMenuItem.selection })

//        val viewZoomMenuItem = MenuItem(viewMenu, SWT.CASCADE)
//        viewZoomMenuItem.text = "&Zooming"
//        val viewZoomMenu = Menu(shell, SWT.DROP_DOWN)
//        viewZoomMenuItem.menu = viewZoomMenu
//
//        zoomInMenuItem = MenuItem(viewZoomMenu, SWT.CHECK)
//        zoomInMenuItem.text = "Zoom &In\tCtrl+Plus"
//        zoomInMenuItem.image = zoomInIcon
//        zoomInMenuItem.addSelectionListener(SelectionCommand { this.textEditor })
    }

    private fun editFont() {
        val open: FontData = fontDialog.open() ?: return
        textEditor.font = Font(display, open)
    }

    private fun newWindow() {
        newWindow(null)
    }

    private fun deleteSelection() {
        var content: String = textEditor.text
        val selection: Point = textEditor.selection

        // Get the first non-selected part, add "Ball" and get the second non-selected part
        content = content.substring(0, selection.x) + content.substring(selection.y, content.length)

        textEditor.text = content
    }

    private fun postInit() {
        shell.pack()
        shell.size = Point(600, 450)
        shell.open()

        val main = this

        shell.addShellListener(object : ShellAdapter() {
            override fun shellClosed(e: ShellEvent) {
                windows.remove(main)
            }
        })
    }

    fun loadMultiResIcon(path: String): Array<Image> {
        return Arrays.stream(ImageLoader().load(this::class.java.getResourceAsStream("/$path"))).map {
            Image(display, it)
        }.toList().toTypedArray()
    }

    private fun loadImage(path: String): Image {
        var path1 = path
        if (!path1.endsWith(".png")) {
            path1 += ".png"
        }
        return Image(display, ImageLoader().load(this::class.java.getResourceAsStream("/$path1"))[0])
    }
}