package sample;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.List;

public class JPad extends JFrame {

    private static final String DEFAULT_TITLE = "Untitled - JPad";
    private static final String SAVE_CHANGES = "Save changes?";
    private static final String TITLE_AYS = "Are you sure?";
    private static final String DEFAULT_STATUS = "JPad 1.0";
    private final JFileChooser fc;
    private final Clipboard cb;
    private final UndoManager um;
    private final DropTarget dt;
    private JLabel lbl;
    private JTextArea ta;
    private boolean fDirty;
    private File currentFile;

    public JPad(String[] args) {
        cb = Toolkit.getDefaultToolkit().getSystemClipboard();

        fc = new JFileChooser(".");
        FileFilter ff = new FileNameExtensionFilter("TXT documents (*.txt)", "txt");
        fc.setFileFilter(ff);

        JMenuBar mb = new JMenuBar();
        JMenu mFile = new JMenu("File");
        JMenuItem miNew = new JMenuItem("New");
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
        ActionListener al;
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (fDirty) {
                    switch (JOptionPane.showConfirmDialog(JPad.this, SAVE_CHANGES, TITLE_AYS, JOptionPane.YES_NO_OPTION)) {
                        case JOptionPane.YES_OPTION: if (doSave()) doNew(); break;
                        case JOptionPane.NO_OPTION: doNew();
                    }
                } else {
                    doNew();
                }
            }
        };
        miNew.addActionListener(al);
        mFile.add(miNew);

        JMenuItem miOpen = new JMenuItem("Open");
        miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doOpen();
            }
        };
        miOpen.addActionListener(al);
        mFile.add(miOpen);

        JMenuItem miSave = new JMenuItem("Save");
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSave();
            }
        };
        miSave.addActionListener(al);
        mFile.add(miSave);

        JMenuItem miSaveAs = new JMenuItem("Save As...");
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doSaveAs();
            }
        };
        miSaveAs.addActionListener(al);
        mFile.add(miSaveAs);

        mFile.addSeparator();
        JMenuItem miExit = new JMenuItem("Exit");
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doExit();
            }
        };
        miExit.addActionListener(al);
        mFile.add(miExit);

        MenuListener ml;
        ml = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                lbl.setText("New doc|Open existing doc|Save changes|Exit");
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                lbl.setText(DEFAULT_STATUS);
            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        };
        mFile.addMenuListener(ml);

        mb.add(mFile);
        JMenu mEdit = new JMenu("Edit");
        final JMenuItem miUndo = new JMenuItem("Undo");
        miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK));
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                um.undo();
            }
        };
        miUndo.addActionListener(al);
        mEdit.add(miUndo);
        mEdit.addSeparator();

        final JMenuItem miCut = new JMenuItem("Cut");
        miCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK));
        miCut.setEnabled(false);

        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ta.cut();
            }
        };
        miCut.addActionListener(al);
        mEdit.add(miCut);

        final JMenuItem miCopy = new JMenuItem("Copy");
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK));
        miCopy.setEnabled(false);

        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ta.copy();
            }
        };
        miCopy.addActionListener(al);
        mEdit.add(miCopy);

        final JMenuItem miPaste = new JMenuItem("Paste");
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK));
        miPaste.setEnabled(false);

        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ta.paste();
            }
        };
        miPaste.addActionListener(al);
        mEdit.add(miPaste);

        ml = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                miUndo.setEnabled(um.canUndo());
                miPaste.setEnabled(cb.isDataFlavorAvailable(DataFlavor.stringFlavor));
                lbl.setText("Undo last change|Cut or copy selected text|Paste");
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                lbl.setText(DEFAULT_STATUS);
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        };
        mEdit.addMenuListener(ml);
        mb.add(mEdit);

        JMenu mFormat = new JMenu("Format");
        final JCheckBoxMenuItem cbmiWordWrap = new JCheckBoxMenuItem("Word Wrap");
        al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ta.setLineWrap(cbmiWordWrap.isSelected());
            }
        };
        cbmiWordWrap.addActionListener(al);
        mFormat.add(cbmiWordWrap);
        ml = new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                lbl.setText("Enable/disable word wrap");
            }

            @Override
            public void menuDeselected(MenuEvent e) {
                lbl.setText(DEFAULT_STATUS);
            }

            @Override
            public void menuCanceled(MenuEvent e) {

            }
        };
        mFormat.addMenuListener(ml);
        mb.add(mFormat);

        setJMenuBar(mb);

        getContentPane().add(new JScrollPane(ta = new JTextArea()));
        DocumentListener dl;
        dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fDirty = true;
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fDirty = true;
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        ta.getDocument().addDocumentListener(dl);

        um = new UndoManager();
        UndoableEditListener uel = new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent uee) {
                um.addEdit(uee.getEdit());
            }
        };
        ta.getDocument().addUndoableEditListener(uel);


        CaretListener cl;
        cl = new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                if (ta.getSelectedText() != null) {
                    miCopy.setEnabled(true);
                    miCut.setEnabled(true);
                } else {
                    miCopy.setEnabled(false);
                    miCut.setEnabled(false);
                }
            }
        };
        ta.addCaretListener(cl);

        DropTargetAdapter dta = new DropTargetAdapter() {

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                Transferable tr = dtde.getTransferable();
                DataFlavor[] flavors = tr.getTransferDataFlavors();
                for (DataFlavor flavor : flavors) {
                    if (flavor.isFlavorJavaFileListType()) {
                        dtde.acceptDrag(DnDConstants.ACTION_COPY);
                        return;
                    }
                }
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable tr = dtde.getTransferable();
                    DataFlavor[] flavors = tr.getTransferDataFlavors();
                    for (DataFlavor flavor : flavors) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        List<File> files = (List<File>) tr.getTransferData(flavor);
                        File fileToOpen = files.get(0);
                        if (fDirty) {
                            switch (JOptionPane.showConfirmDialog(JPad.this, SAVE_CHANGES, TITLE_AYS, JOptionPane.YES_NO_OPTION)) {
                                case JOptionPane.YES_OPTION: if (doSave()) doOpen(fileToOpen); break;
                                case JOptionPane.NO_OPTION: doOpen(fileToOpen);
                            }
                        } else {
                            doOpen(fileToOpen);
                        }
                        dtde.dropComplete(true);
                        um.discardAllEdits();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JPad.this, "Drop error: " + e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        dt = new DropTarget(ta, dta);

        getContentPane().add(lbl = new JLabel(DEFAULT_STATUS), BorderLayout.SOUTH);
        setSize(400, 400);
        setTitle(DEFAULT_TITLE);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doExit();
            }
        });
        setVisible(true);

        if (args.length != 0) {
            doOpen(new File(args[0]));
        }
    }

    private void doOpen() {
        doOpen(null);
    }

    private void doOpen(File file) {
        if (file == null) {
            if (fc.showOpenDialog(JPad.this) == JFileChooser.APPROVE_OPTION)
                file = fc.getSelectedFile();
        }

        if (file == null) return;

        fc.setCurrentDirectory(file.getParentFile());
        if (file.toString().indexOf(".") == -1)
            file = new File(file + ".txt");

        try {
            ta.setText(read(file));
            currentFile = file;
            ta.setCaretPosition(0);
            ta.requestFocus();
            setTitle(file.toString() + " - JPad");
            fDirty = false;
            um.discardAllEdits();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(JPad.this, "I/O error: " + e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doExit() {
        if (fDirty) {
            switch (JOptionPane.showConfirmDialog(JPad.this, SAVE_CHANGES, TITLE_AYS, JOptionPane.YES_NO_OPTION)) {
                case JOptionPane.YES_OPTION: if (doSave()) dispose(); break;
                case JOptionPane.NO_OPTION: dispose();
            }
        } else {
            dispose();
        }
    }

    private void doNew() {
        currentFile = null;
        ta.setText("");
        setTitle(DEFAULT_TITLE);
        fDirty = false;
        um.discardAllEdits();
    }

    private boolean doSave() {
        if (currentFile == null) {
            return doSaveAs();
        }

        try {
            write(currentFile, ta.getText());
            fDirty = false;
            um.discardAllEdits();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(JPad.this, "I/O error: " + e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    private boolean doSaveAs() {

        File file;
        if (fc.showSaveDialog(JPad.this) == JFileChooser.APPROVE_OPTION) {
            file = fc.getSelectedFile();
        } else
            return false;

        fc.setCurrentDirectory(file.getParentFile());
        if (file.getName().indexOf(".") == -1)
            file = new File(file + ".txt");
        try {
            write(file, ta.getText());
            currentFile = file;
            setTitle(file.toString() + " - JPad");
            fDirty = false;
            um.discardAllEdits();
            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(JPad.this, "I/O error: " + e.getMessage(), "Alert", JOptionPane.ERROR_MESSAGE);
        }

        return false;
    }

    private String read(File f) throws IOException {
        FileReader fr = null;
        try {
            fr = new FileReader(f);
            char[] buffer = new char[(int) f.length()];
            fr.read(buffer);
            return new String(buffer);
        } finally {
            if (fr != null) fr.close();
        }
    }

    private void write(File f, String text) throws IOException {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(f));
            pw.print(text);
            pw.flush();
        } finally {
            if (pw != null) pw.close();
        }
    }

    public static void main(final String[] args) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                new JPad(args);
            }
        };

        EventQueue.invokeLater(r);
    }
}
