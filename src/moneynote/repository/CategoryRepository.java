package moneynote.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moneynote.model.Category;
import moneynote.model.TransactionType;

/** 분류 저장소. 최초 실행 시 기본 분류를 시드한다. (F5) */
public class CategoryRepository {

    private static final String[] HEADER = {"id", "name", "type", "seeded"};

    private static final String[] DEFAULT_EXPENSE = {
        "식비", "교통", "주거", "통신", "문화", "의료", "쇼핑", "기타"
    };
    private static final String[] DEFAULT_INCOME = {"급여", "용돈", "기타수입"};

    private final Path file;
    private final List<Category> items = new ArrayList<>();
    private int nextId = 1;

    /**
     * 저장소를 만든다. 파일은 {@link #load()} 를 부를 때 읽는다.
     *
     * @param file 저장할 CSV 파일 경로
     */
    public CategoryRepository(Path file) {
        this.file = file;
    }

    /**
     * 등록 순서대로 모든 분류 (읽기 전용).
     *
     * @return 등록 순서대로 모든 분류 (읽기 전용)
     */
    public List<Category> findAll() {
        return List.copyOf(items);
    }

    /**
     * 한 유형의 분류 목록.
     *
     * @param type 수입 또는 지출
     * @return 그 유형의 분류
     */
    public List<Category> findByType(TransactionType type) {
        return items.stream().filter(c -> c.type() == type).toList();
    }

    /**
     * id로 분류를 찾는다.
     *
     * @param id 분류 id
     * @return 분류, 없으면 빈 값
     */
    public Optional<Category> findById(int id) {
        return items.stream().filter(c -> c.id() == id).findFirst();
    }

    /**
     * 화면 표시용 이름. 삭제된 분류를 참조하는 거래가 있어도 안전하게 동작한다.
     *
     * @param categoryId 분류 id
     * @return 분류 이름, 없으면 "(삭제된 분류)"
     */
    public String nameOf(int categoryId) {
        return findById(categoryId).map(Category::name).orElse("(삭제된 분류)");
    }

    /**
     * 새 id를 붙여 분류를 추가한다. 이름 검증은 서비스 계층이 한다.
     *
     * @param name   이름
     * @param type   이 분류를 쓸 거래 유형
     * @param seeded 기본 분류이면 true
     * @return 추가된 분류
     */
    public Category add(String name, TransactionType type, boolean seeded) {
        Category category = new Category(nextId++, name, type, seeded);
        items.add(category);
        return category;
    }

    /**
     * 분류를 지운다. 사용 여부 검사는 서비스 계층이 한다.
     *
     * @param id 지울 분류 id
     */
    public void remove(int id) {
        items.removeIf(c -> c.id() == id);
    }

    /**
     * 파일에서 읽는다. 파일이 없거나 비어 있으면 기본 분류 11개(지출 8개, 수입 3개)를 만든다.
     *
     * @throws IOException 파일을 읽지 못한 경우
     */
    public void load() throws IOException {
        items.clear();
        for (String[] row : CsvStore.read(file)) {
            if (row.length < 4) {
                continue;
            }
            int id = Integer.parseInt(row[0]);
            items.add(new Category(id, row[1], TransactionType.valueOf(row[2]),
                    Boolean.parseBoolean(row[3])));
            nextId = Math.max(nextId, id + 1);
        }
        if (items.isEmpty()) {
            seedDefaults();
        }
    }

    /**
     * 모든 항목을 파일에 쓴다.
     *
     * @throws IOException 파일을 쓰지 못한 경우
     */
    public void save() throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (Category c : items) {
            rows.add(new String[] {
                String.valueOf(c.id()), c.name(), c.type().name(), String.valueOf(c.seeded())
            });
        }
        CsvStore.write(file, HEADER, rows);
    }

    /** 사용자가 빈 화면에서 시작하지 않도록 기본 분류를 만들어 둔다. */
    private void seedDefaults() {
        for (String name : DEFAULT_EXPENSE) {
            add(name, TransactionType.EXPENSE, true);
        }
        for (String name : DEFAULT_INCOME) {
            add(name, TransactionType.INCOME, true);
        }
    }
}
