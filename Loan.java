import java.time.LocalDate;

public class Loan {
    private final Book book;
    private final Member member;
    private final LocalDate borrowDate;
    private LocalDate returnDate;

    public Loan(Book book, Member member) {
        this(book, member, LocalDate.now(), null);
    }

    // constructor used when loading persisted loans
    public Loan(Book book, Member member, LocalDate borrowDate, LocalDate returnDate) {
        this.book = book;
        this.member = member;
        this.borrowDate = borrowDate;
        this.returnDate = returnDate;
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getBorrowDate() {
        return borrowDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    @Override
    public String toString() {
        return book.getId() + " borrowed by " + member.getName() + " on " + borrowDate + (returnDate == null ? "" : ", returned " + returnDate);
    }
}
