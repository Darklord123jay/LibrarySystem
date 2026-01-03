import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryUI extends JFrame {
    private Library lib;
    private final DefaultListModel<Book> bookModel = new DefaultListModel<>();
    private final JList<Book> bookList = new JList<>(bookModel);
    private final JLabel statusBar = new JLabel("Ready");
    private final List<Book> allBooks = new ArrayList<>();

    public LibraryUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        // try SQL backend (requires sqlite-jdbc on classpath). fallback to in-memory Library.
        try {
            lib = new SqlLibrary("jdbc:sqlite:library.db");
        } catch (Exception e) {
            lib = new Library();
        }
        if (lib.listAllBooks().isEmpty()) seedData();
        initComponents();
        refreshBooks();
    }

    // allow constructing UI with an existing Library backend (used for login flow)
    public LibraryUI(Library lib) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        this.lib = lib;
        if (lib.listAllBooks().isEmpty()) seedData();
        initComponents();
        refreshBooks();
    }

    private void seedData() {
        lib.addBook(new Book("B1", "1984", "George Orwell"));
        lib.addBook(new Book("B2", "Brave New World", "Aldous Huxley"));
        lib.addBook(new Book("B3", "The Hobbit", "J.R.R. Tolkien"));
        lib.registerMember(new Member("M1", "Alice"));
        lib.registerMember(new Member("M2", "Bob"));
    }

    private void initComponents() {
        setTitle("Library");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 450);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        JLabel title = new JLabel("Library");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        top.add(title, BorderLayout.WEST);

        JTextField search = new JTextField();
        search.setToolTipText("Search by title or author");
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshBooks(search.getText()); }
            public void removeUpdate(DocumentEvent e) { refreshBooks(search.getText()); }
            public void changedUpdate(DocumentEvent e) { refreshBooks(search.getText()); }
        });
        top.add(search, BorderLayout.CENTER);
        main.add(top, BorderLayout.NORTH);

        bookList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookList.setCellRenderer(new BookCellRenderer());
        JScrollPane scroll = new JScrollPane(bookList);
        scroll.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        main.add(scroll, BorderLayout.CENTER);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBorder(new EmptyBorder(6,6,6,6));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1; gbc.insets = new Insets(4,4,4,4);

        JButton btnBorrow = new JButton("Borrow");
        btnBorrow.setToolTipText("Borrow selected book");
        btnBorrow.addActionListener(e -> onBorrow());
        gbc.gridy = 0; right.add(btnBorrow, gbc);

        JButton btnReturn = new JButton("Return");
        btnReturn.setToolTipText("Return selected book");
        btnReturn.addActionListener(e -> onReturn());
        gbc.gridy = 1; right.add(btnReturn, gbc);

        JButton btnAddBook = new JButton("Add Book");
        btnAddBook.addActionListener(e -> onAddBook());
        gbc.gridy = 2; right.add(btnAddBook, gbc);

        JButton btnAddMember = new JButton("Add Member");
        btnAddMember.addActionListener(e -> onAddMember());
        gbc.gridy = 3; right.add(btnAddMember, gbc);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> refreshBooks());
        gbc.gridy = 4; right.add(btnRefresh, gbc);

        main.add(right, BorderLayout.EAST);

        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> dispose());
        file.add(exit);
        mb.add(file);
        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> JOptionPane.showMessageDialog(this, "Library App\nSimple demo UI", "About", JOptionPane.INFORMATION_MESSAGE));
        help.add(about);
        mb.add(help);
        setJMenuBar(mb);

        statusBar.setBorder(new EmptyBorder(4,4,4,4));
        add(statusBar, BorderLayout.SOUTH);

        add(main);
    }

    private class BookCellRenderer implements ListCellRenderer<Book> {
        @Override
        public Component getListCellRendererComponent(JList<? extends Book> list, Book value, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout(4,2));
            p.setBorder(new EmptyBorder(6,6,6,6));
            JLabel title = new JLabel(value.getTitle());
            title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
            JLabel meta = new JLabel(value.getAuthor() + " — " + (value.isAvailable() ? "Available" : "Checked out"));
            meta.setFont(meta.getFont().deriveFont(Font.PLAIN, 11f));
            meta.setForeground(Color.DARK_GRAY);
            p.add(title, BorderLayout.NORTH);
            p.add(meta, BorderLayout.SOUTH);
            if (isSelected) { p.setBackground(list.getSelectionBackground()); p.setForeground(list.getSelectionForeground()); }
            else { p.setBackground(list.getBackground()); p.setForeground(list.getForeground()); }
            return p;
        }
    }

    private void onBorrow() {
        Book b = bookList.getSelectedValue();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Select a book first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String mid = JOptionPane.showInputDialog(this, "Member ID:");
        if (mid == null || mid.trim().isEmpty()) return;
        boolean ok = lib.borrowBook(b.getId(), mid.trim());
        String msg = ok ? "Borrowed." : "Could not borrow (check IDs or availability).";
        JOptionPane.showMessageDialog(this, msg, "Result", JOptionPane.INFORMATION_MESSAGE);
        statusBar.setText(msg);
        refreshBooks();
    }

    private void onReturn() {
        Book b = bookList.getSelectedValue();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Select a book first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean ok = lib.returnBook(b.getId());
        String msg = ok ? "Returned." : "Could not return (no active loan).";
        JOptionPane.showMessageDialog(this, msg, "Result", JOptionPane.INFORMATION_MESSAGE);
        statusBar.setText(msg);
        refreshBooks();
    }

    private void onAddBook() {
        String id = JOptionPane.showInputDialog(this, "Book ID:");
        if (id == null || id.trim().isEmpty()) return;
        String title = JOptionPane.showInputDialog(this, "Title:");
        if (title == null) return;
        String author = JOptionPane.showInputDialog(this, "Author:");
        if (author == null) return;
        lib.addBook(new Book(id.trim(), title.trim(), author.trim()));
        statusBar.setText("Book added: " + title.trim());
        refreshBooks();
    }

    private void onAddMember() {
        String id = JOptionPane.showInputDialog(this, "Member ID:");
        if (id == null || id.trim().isEmpty()) return;
        String name = JOptionPane.showInputDialog(this, "Name:");
        if (name == null) return;
        lib.registerMember(new Member(id.trim(), name.trim()));
        statusBar.setText("Member added: " + name.trim());
    }

    private void refreshBooks() { refreshBooks(""); }

    private void refreshBooks(String filter) {
        bookModel.clear();
        allBooks.clear();
        for (Book b : lib.listAllBooks()) allBooks.add(b);
        String f = filter == null ? "" : filter.trim().toLowerCase();
        int count = 0;
        for (Book b : allBooks) {
            if (f.isEmpty() || b.getTitle().toLowerCase().contains(f) || b.getAuthor().toLowerCase().contains(f)) {
                bookModel.addElement(b);
                count++;
            }
        }
        statusBar.setText("Showing " + count + " of " + allBooks.size() + " books");
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            LibraryUI ui = new LibraryUI();
            ui.setVisible(true);
        });
    }

    public static void launch(Library lib) {
        SwingUtilities.invokeLater(() -> {
            LibraryUI ui = new LibraryUI(lib);
            ui.setVisible(true);
        });
    }
}
