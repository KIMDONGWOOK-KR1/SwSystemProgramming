package moneynote.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import moneynote.model.Category;
import moneynote.model.Transaction;
import moneynote.model.TransactionType;
import moneynote.repository.CategoryRepository;
import moneynote.repository.TransactionRepository;

/**
 * 거래와 분류에 대한 응용 로직. (F1~F5)
 *
 * <p>UI는 저장소를 직접 건드리지 않고 반드시 이 계층을 거친다. 검증 규칙을 한곳에 모아
 * 등록 화면과 수정 화면이 같은 규칙을 따르게 하기 위해서다.
 */
public class LedgerService {

    private final TransactionRepository transactions;
    private final CategoryRepository categories;

    /**
     * 서비스를 만든다.
     *
     * @param transactions 거래 저장소
     * @param categories   분류 저장소
     */
    public LedgerService(TransactionRepository transactions, CategoryRepository categories) {
        this.transactions = transactions;
        this.categories = categories;
    }

    /**
     * F1: 거래 등록. (UC1, UC13)
     *
     * @param date       거래 날짜
     * @param type       수입 또는 지출
     * @param amount     금액(원), 0보다 커야 한다
     * @param categoryId 분류 id, 그 분류의 유형이 type 과 같아야 한다
     * @param payment    결제수단
     * @param memo       메모
     * @return 새 id가 붙은 거래
     * @throws IllegalArgumentException 날짜·유형이 없거나, 금액이 0 이하이거나, 분류가 없거나 유형이 다른 경우
     */
    public Transaction register(LocalDate date, TransactionType type, long amount,
                                int categoryId, String payment, String memo) {
        validate(date, type, amount, categoryId);
        return transactions.add(date, type, amount, categoryId, payment, memo);
    }

    /**
     * F2: 거래 수정. id는 유지하고 나머지 필드를 교체한다. 등록과 같은 검증을 한다. (UC2)
     *
     * @param id         수정할 거래 id
     * @param date       거래 날짜
     * @param type       수입 또는 지출
     * @param amount     금액(원), 0보다 커야 한다
     * @param categoryId 분류 id
     * @param payment    결제수단
     * @param memo       메모
     * @throws IllegalArgumentException 거래가 없거나 검증에 실패한 경우
     */
    public void update(int id, LocalDate date, TransactionType type, long amount,
                       int categoryId, String payment, String memo) {
        validate(date, type, amount, categoryId);
        Transaction t = transactions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다: " + id));
        t.setDate(date);
        t.setType(type);
        t.setAmount(amount);
        t.setCategoryId(categoryId);
        t.setPayment(payment);
        t.setMemo(memo);
    }

    /**
     * F2: 거래 삭제. 삭제 확인은 화면의 몫이다. (UC3)
     *
     * @param id 지울 거래 id
     */
    public void delete(int id) {
        transactions.delete(id);
    }

    private void validate(LocalDate date, TransactionType type, long amount, int categoryId) {
        if (date == null) {
            throw new IllegalArgumentException("날짜를 입력하세요.");
        }
        if (type == null) {
            throw new IllegalArgumentException("수입/지출을 선택하세요.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        Category category = categories.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("분류를 선택하세요."));
        if (category.type() != type) {
            throw new IllegalArgumentException(
                    "분류의 유형이 맞지 않습니다: " + category.name() + "은(는) " + category.type().label() + " 분류입니다.");
        }
    }

    /**
     * F3, F4: 조건에 맞는 거래를 날짜 내림차순으로 돌려준다. 같은 날이면 id 내림차순이다. (UC4)
     *
     * @param filter 조회 조건, 비어 있는 조건은 무시한다
     * @return 조건을 모두 만족하는 거래 목록
     */
    public List<Transaction> find(TransactionFilter filter) {
        return transactions.findAll().stream()
                .filter(t -> matches(t, filter))
                .sorted(Comparator.comparing(Transaction::date).reversed()
                        .thenComparing(Comparator.comparingInt(Transaction::id).reversed()))
                .toList();
    }

    private boolean matches(Transaction t, TransactionFilter f) {
        if (f.from() != null && t.date().isBefore(f.from())) {
            return false;
        }
        if (f.to() != null && t.date().isAfter(f.to())) {
            return false;
        }
        if (f.type() != null && t.type() != f.type()) {
            return false;
        }
        if (f.categoryId() != null && t.categoryId() != f.categoryId()) {
            return false;
        }
        if (f.keyword() != null) {
            String memo = t.memo().toLowerCase();
            if (!memo.contains(f.keyword().toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    /**
     * F5: 분류 추가. 같은 유형 안에서 이름이 중복되면 거부한다. (UC5)
     *
     * @param name 이름, 앞뒤 공백은 뗀다
     * @param type 이 분류를 쓸 거래 유형
     * @return 추가된 분류
     * @throws IllegalArgumentException 이름이 비었거나 같은 유형에 같은 이름(대소문자 무시)이 있는 경우
     */
    public Category addCategory(String name, TransactionType type) {
        String trimmed = checkName(name, type, -1);
        return categories.add(trimmed, type, false);
    }

    /**
     * F5: 분류 이름 변경. 거래는 분류 id를 참조하므로 기존 거래에도 새 이름이 보인다. (UC6)
     *
     * @param categoryId 바꿀 분류 id
     * @param newName    새 이름, 앞뒤 공백은 뗀다
     * @throws IllegalArgumentException 분류가 없거나, 이름이 비었거나, 같은 유형에 같은 이름이 있는 경우
     */
    public void renameCategory(int categoryId, String newName) {
        Category category = categories.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("분류를 선택하세요."));
        category.rename(checkName(newName, category.type(), categoryId));
    }

    /** 이름을 다듬어 돌려준다. 자기 자신(exceptId)과 같은 이름인 것은 중복으로 보지 않는다. */
    private String checkName(String name, TransactionType type, int exceptId) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("분류 이름을 입력하세요.");
        }
        boolean duplicated = categories.findByType(type).stream()
                .anyMatch(c -> c.id() != exceptId && c.name().equalsIgnoreCase(trimmed));
        if (duplicated) {
            throw new IllegalArgumentException("같은 이름의 분류가 이미 있습니다: " + trimmed);
        }
        return trimmed;
    }

    /**
     * 분류를 쓰는 거래 수. 분류 관리 화면이 삭제 가능 여부를 미리 보이는 데 쓴다.
     *
     * @param categoryId 분류 id
     * @return 이 분류를 쓰는 거래 수
     */
    public int usageCount(int categoryId) {
        return transactions.countByCategory(categoryId);
    }

    /**
     * F5: 분류 삭제. 사용 중이면 고아 데이터를 막기 위해 차단한다. (UC7)
     *
     * @param categoryId 지울 분류 id
     * @throws IllegalStateException 이 분류를 쓰는 거래가 있는 경우
     */
    public void deleteCategory(int categoryId) {
        int used = usageCount(categoryId);
        if (used > 0) {
            throw new IllegalStateException("이 분류를 사용하는 거래가 " + used + "건 있습니다. 먼저 정리하세요.");
        }
        categories.remove(categoryId);
    }

    /**
     * 분류 저장소. 화면이 분류 목록을 채우는 데 쓴다.
     *
     * @return 분류 저장소. 화면이 분류 목록을 채우는 데 쓴다.
     */
    public CategoryRepository categories() {
        return categories;
    }

    /**
     * F10: 거래와 분류를 파일에 쓴다. (UC15)
     *
     * @throws IOException 파일을 쓰지 못한 경우
     */
    public void save() throws IOException {
        transactions.save();
        categories.save();
    }
}
