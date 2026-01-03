import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlLibrary extends Library {
    private final Connection conn;

    public SqlLibrary(String url) throws SQLException {
        conn = DriverManager.getConnection(url);
        initSchema();
    }

    private void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS books(id TEXT PRIMARY KEY, title TEXT, author TEXT, available INTEGER)");
            st.execute("CREATE TABLE IF NOT EXISTS members(id TEXT PRIMARY KEY, name TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS loans(book_id TEXT PRIMARY KEY, member_id TEXT, borrow_date TEXT)");
        }
    }

    public void addBook(Book b) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO books(id,title,author,available) VALUES(?,?,?,?)")) {
            ps.setString(1, b.getId());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setInt(4, b.isAvailable() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void registerMember(Member m) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO members(id,name) VALUES(?,?)")) {
            ps.setString(1, m.getId());
            ps.setString(2, m.getName());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public boolean borrowBook(String bookId, String memberId) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement q = conn.prepareStatement("SELECT available FROM books WHERE id=?")) {
                q.setString(1, bookId);
                try (ResultSet rs = q.executeQuery()) {
                    if (!rs.next() || rs.getInt("available") == 0) { conn.rollback(); conn.setAutoCommit(true); return false; }
                }
            }
            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO loans(book_id,member_id,borrow_date) VALUES(?,?,?)")) {
                ins.setString(1, bookId);
                ins.setString(2, memberId);
                ins.setString(3, LocalDate.now().toString());
                ins.executeUpdate();
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE books SET available=0 WHERE id=?")) {
                up.setString(1, bookId); up.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { }
            e.printStackTrace();
            return false;
        }
    }

    public boolean returnBook(String bookId) {
        try (PreparedStatement q = conn.prepareStatement("SELECT book_id FROM loans WHERE book_id=?")) {
            q.setString(1, bookId);
            try (ResultSet rs = q.executeQuery()) {
                if (!rs.next()) return false;
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM loans WHERE book_id=?")) {
                del.setString(1, bookId); del.executeUpdate();
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE books SET available=1 WHERE id=?")) {
                up.setString(1, bookId); up.executeUpdate();
            }
            conn.commit(); conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { }
            e.printStackTrace();
            return false;
        }
    }

    public List<Book> listAllBooks() {
        List<Book> out = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id,title,author,available FROM books")) {
            while (rs.next()) {
                Book b = new Book(rs.getString("id"), rs.getString("title"), rs.getString("author"));
                b.setAvailable(rs.getInt("available") == 1);
                out.add(b);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    public List<Loan> listLoans() {
        List<Loan> out = new ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT l.book_id, l.member_id, l.borrow_date, b.title, b.author, m.name FROM loans l JOIN books b ON l.book_id=b.id JOIN members m ON l.member_id=m.id")) {
            while (rs.next()) {
                Book b = new Book(rs.getString("book_id"), rs.getString("title"), rs.getString("author"));
                b.setAvailable(false);
                Member m = new Member(rs.getString("member_id"), rs.getString("name"));
                LocalDate borrow = LocalDate.parse(rs.getString("borrow_date"));
                out.add(new Loan(b, m, borrow, null));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return out;
    }

    @Override
    public Optional<Book> findBook(String id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id,title,author,available FROM books WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Book b = new Book(rs.getString("id"), rs.getString("title"), rs.getString("author"));
                    b.setAvailable(rs.getInt("available") == 1);
                    return Optional.of(b);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    public boolean memberExists(String id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM members WHERE id=?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
}
