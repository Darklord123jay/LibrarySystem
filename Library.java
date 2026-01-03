import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Library {
    private final Map<String, Book> books = new HashMap<>();
    private final Map<String, Member> members = new HashMap<>();
    private final Map<String, Loan> loans = new HashMap<>();

    public void addBook(Book book) {
        books.put(book.getId(), book);
    }

    public void registerMember(Member member) {
        members.put(member.getId(), member);
    }

    public boolean borrowBook(String bookId, String memberId) {
        Book book = books.get(bookId);
        Member member = members.get(memberId);
        if (book == null || member == null) return false;
        if (!book.isAvailable()) return false;
        Loan loan = new Loan(book, member);
        book.setAvailable(false);
        loans.put(bookId, loan);
        return true;
    }

    public boolean returnBook(String bookId) {
        Loan loan = loans.get(bookId);
        if (loan == null) return false;
        loan.setReturnDate(LocalDate.now());
        Book book = loan.getBook();
        book.setAvailable(true);
        loans.remove(bookId);
        return true;
    }

    public List<Book> listAvailableBooks() {
        List<Book> out = new ArrayList<>();
        for (Book b : books.values()) if (b.isAvailable()) out.add(b);
        return out;
    }

    public List<Book> listAllBooks() {
        return new ArrayList<>(books.values());
    }

    public List<Loan> listLoans() {
        return new ArrayList<>(loans.values());
    }

    public Optional<Book> findBook(String id) {
        return Optional.ofNullable(books.get(id));
    }

    public boolean memberExists(String id) {
        return members.containsKey(id);
    }
}
