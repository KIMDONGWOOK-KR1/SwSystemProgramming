import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import moneynote.Main;
import moneynote.model.TransactionType;
import moneynote.repository.BudgetRepository;
import moneynote.repository.CategoryRepository;
import moneynote.repository.RecurringRepository;
import moneynote.repository.TransactionRepository;

/**
 * 보고서의 베타 릴리스 스크린샷을 만든다. 실제 프로그램을 화면에 띄워 운영체제 창 그대로 찍는다.
 *
 *   javac -encoding UTF-8 -cp out -d out\tools tools\BetaScreenshots.java
 *   java -cp "out;out\tools" BetaScreenshots docs\cp4_figures
 *
 * 임시 폴더에 예시 CSV를 쓰고 {@link Main#start} 로 사용자와 같은 시작 경로를 거친다.
 * 고정 지출 규칙의 마지막 생성 월을 지난달로 두었으므로 시작하면 이번 달 거래 2건이 실제로 생성된다.
 * 실행 중에는 창이 맨 위에 떠 있어야 하므로 다른 창으로 가리지 않는다. 실제 data 폴더는 건드리지 않는다.
 */
public class BetaScreenshots {

    private static Robot robot;
    private static Path outDir;

    public static void main(String[] args) throws Exception {
        outDir = Path.of(args.length > 0 ? args[0] : "docs/cp4_figures");
        Files.createDirectories(outDir);
        robot = new Robot();

        Path dataDir = Files.createTempDirectory("moneynote-beta");
        writeSampleData(dataDir);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        Main.start(dataDir);

        Frame frame = waitFor(() -> visibleFrame());
        onEdt(() -> {
            frame.setAlwaysOnTop(true);
            frame.setSize(1000, 600);
            frame.setLocation(40, 40);
        });

        // 1. 시작 알림: 이번 달 고정 지출 자동 생성 (UC13)
        JDialog notice = waitFor(() -> dialog("고정 지출"));
        onEdt(() -> {
            notice.setLocationRelativeTo(frame);
            notice.toFront();
        });
        capture(frame, "beta_startup_notice.png");
        onEdt(notice::dispose);

        // 2~4. 세 탭
        JTabbedPane tabs = find(frame, JTabbedPane.class::isInstance);
        String[] tabFiles = {"beta_transactions.png", "beta_statistics.png", "beta_budget.png"};
        for (int i = 0; i < tabFiles.length; i++) {
            int index = i;
            onEdt(() -> tabs.setSelectedIndex(index));
            capture(frame, tabFiles[i]);
        }
        onEdt(() -> tabs.setSelectedIndex(0));

        // 5. 분류 관리: 기타수입을 부수입으로 이름 변경한 뒤 (UC6)
        clickLater(frame, "분류 관리");
        JDialog categories = waitFor(() -> dialog("분류 관리"));
        JTable categoryTable = find(categories, JTable.class::isInstance);
        onEdt(() -> {
            int row = rowOf(categoryTable, "기타수입");
            categoryTable.setRowSelectionInterval(row, row);
            ((JTextField) find(categories, JTextField.class::isInstance)).setText("부수입");
            button(categories, "이름 변경").doClick();
            int renamed = rowOf(categoryTable, "부수입");
            categoryTable.setRowSelectionInterval(renamed, renamed);
            categoryTable.scrollRectToVisible(categoryTable.getCellRect(renamed, 0, true));
        });
        capture(categories, "beta_category_dialog.png");

        // 6. 사용 중인 분류 삭제 거부 (UC7 예외 흐름)
        onEdt(() -> {
            int row = rowOf(categoryTable, "식비");
            categoryTable.setRowSelectionInterval(row, row);
            categoryTable.scrollRectToVisible(categoryTable.getCellRect(row, 0, true));
        });
        clickLater(categories, "삭제");
        JDialog confirm = waitFor(() -> dialog("삭제 확인"));
        // 확인 창의 "예" 버튼 글자는 로캘마다 달라서 답을 직접 넣는다.
        onEdt(() -> ((JOptionPane) find(confirm, JOptionPane.class::isInstance))
                .setValue(JOptionPane.YES_OPTION));
        JDialog refused = waitFor(() -> dialog("입력 확인"));
        captureUnion(categories, refused, "beta_category_delete_refused.png");
        onEdt(refused::dispose);
        onEdt(categories::dispose);

        // 7. 고정 지출 관리: 시작할 때 생성된 규칙은 마지막 생성 월이 이번 달이다. (UC12)
        clickLater(frame, "고정 지출");
        JDialog rules = waitFor(() -> dialog("고정 지출 관리"));
        capture(rules, "beta_recurring_dialog.png");
        onEdt(rules::dispose);

        // 이름 변경이 거래 목록에도 보이는지 마지막으로 한 번 더 찍는다.
        capture(frame, "beta_transactions_after.png");

        // 창 닫기(저장)를 거치지 않고 끝낸다. 예시 데이터는 임시 폴더에만 있다.
        System.exit(0);
    }

    /** 이번 달(2026-10)이 22일까지 진행된 상태의 예시 데이터. */
    private static void writeSampleData(Path dir) throws Exception {
        CategoryRepository categories = new CategoryRepository(dir.resolve("categories.csv"));
        TransactionRepository tx = new TransactionRepository(dir.resolve("transactions.csv"));
        BudgetRepository budgets = new BudgetRepository(dir.resolve("budgets.csv"));
        RecurringRepository rules = new RecurringRepository(dir.resolve("recurring.csv"));
        categories.load();

        TransactionType out = TransactionType.EXPENSE;
        TransactionType in = TransactionType.INCOME;
        // 분류 id: 1 식비, 2 교통, 3 주거, 4 통신, 5 문화, 7 쇼핑, 9 급여, 10 용돈, 11 기타수입
        tx.add(LocalDate.of(2026, 9, 1), in, 2_500_000, 9, "계좌이체", "9월 급여");
        tx.add(LocalDate.of(2026, 9, 1), out, 450_000, 3, "계좌이체", "월세").setAutoGenerated(true);
        tx.add(LocalDate.of(2026, 9, 12), out, 280_000, 1, "카드", "9월 장보기");
        tx.add(LocalDate.of(2026, 9, 20), out, 65_000, 4, "계좌이체", "휴대폰 요금").setAutoGenerated(true);
        tx.add(LocalDate.of(2026, 9, 25), out, 90_000, 2, "카드", "교통카드 충전");

        tx.add(LocalDate.of(2026, 10, 1), in, 2_500_000, 9, "계좌이체", "10월 급여");
        tx.add(LocalDate.of(2026, 10, 2), out, 18_000, 2, "카드", "교통카드 충전");
        tx.add(LocalDate.of(2026, 10, 3), out, 62_000, 5, "카드", "콘서트 예매");
        tx.add(LocalDate.of(2026, 10, 4), out, 210_000, 1, "카드", "장보기");
        tx.add(LocalDate.of(2026, 10, 4), in, 100_000, 10, "현금", "용돈");
        tx.add(LocalDate.of(2026, 10, 9), out, 130_000, 1, "카드", "가족 외식");
        tx.add(LocalDate.of(2026, 10, 14), out, 39_000, 7, "카드", "운동화 할인");
        tx.add(LocalDate.of(2026, 10, 17), in, 150_000, 11, "계좌이체", "과외비");
        tx.add(LocalDate.of(2026, 10, 21), out, 12_000, 1, "현금", "점심 - 학생식당");
        tx.add(LocalDate.of(2026, 10, 22), out, 4_500, 1, "카드", "커피, \"아아\" 한 잔");

        YearMonth october = YearMonth.of(2026, 10);
        budgets.setLimit(1, october, 400_000);
        budgets.setLimit(2, october, 100_000);
        budgets.setLimit(3, october, 500_000);
        budgets.setLimit(5, october, 50_000);

        YearMonth september = YearMonth.of(2026, 9);
        rules.add(3, 450_000, 1, "월세").setLastGenerated(september);
        rules.add(4, 65_000, 20, "휴대폰 요금").setLastGenerated(september);
        var music = rules.add(5, 17_000, 31, "음악 구독");
        music.setActive(false);
        music.setLastGenerated(YearMonth.of(2026, 8));

        categories.save();
        tx.save();
        budgets.save();
        rules.save();
    }

    /** 창 하나를 찍는다. 화면 배율이 있으면 원래 해상도로 저장한다. */
    private static void capture(Window window, String fileName) throws Exception {
        settle();
        save(visibleBounds(window), fileName);
    }

    /** 부모 창과 그 위에 뜬 메시지 창을 함께 찍는다. */
    private static void captureUnion(Window back, Window front, String fileName) throws Exception {
        settle();
        save(visibleBounds(back).union(visibleBounds(front)), fileName);
    }

    private static void save(Rectangle area, String fileName) throws Exception {
        MultiResolutionImage shot = robot.createMultiResolutionScreenCapture(area);
        List<Image> variants = shot.getResolutionVariants();
        Image largest = variants.get(variants.size() - 1);
        BufferedImage image = new BufferedImage(largest.getWidth(null), largest.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        image.getGraphics().drawImage(largest, 0, 0, null);
        Path file = outDir.resolve(fileName);
        ImageIO.write(image, "png", file.toFile());
        System.out.println("saved " + file + " (" + image.getWidth() + "x" + image.getHeight() + ")");
    }

    /**
     * Windows 10 이후의 창 경계에는 보이지 않는 크기 조절 테두리가 포함된다.
     * 그 부분(insets 에서 실제 1px 테두리를 뺀 만큼)을 잘라 바탕화면이 찍히지 않게 한다.
     */
    private static Rectangle visibleBounds(Window window) {
        Rectangle r = window.getBounds();
        Insets in = window.getInsets();
        int side = Math.max(0, in.left - 1);
        int bottom = Math.max(0, in.bottom - 1);
        return new Rectangle(r.x + side, r.y, r.width - 2 * side, r.height - bottom);
    }

    /** 다시 그리기와 창 애니메이션이 끝날 때까지 기다린다. */
    private static void settle() throws Exception {
        onEdt(() -> { });
        robot.waitForIdle();
        Thread.sleep(700);
    }

    private static void onEdt(Runnable action) throws Exception {
        EventQueue.invokeAndWait(action);
    }

    /** 모달 창을 여는 버튼은 누른 쪽이 막히므로 나중에 누르도록 넘긴다. */
    private static void clickLater(Component root, String text) {
        EventQueue.invokeLater(() -> button(root, text).doClick());
    }

    private static <T> T waitFor(Supplier<T> lookup) throws Exception {
        for (int i = 0; i < 100; i++) {
            T[] holder = newHolder();
            EventQueue.invokeAndWait(() -> holder[0] = lookup.get());
            if (holder[0] != null) {
                return holder[0];
            }
            Thread.sleep(100);
        }
        throw new AWTException("창이 나타나지 않았습니다");
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] newHolder() {
        return (T[]) new Object[1];
    }

    private static Frame visibleFrame() {
        for (Frame f : Frame.getFrames()) {
            if (f.isShowing()) {
                return f;
            }
        }
        return null;
    }

    private static JDialog dialog(String title) {
        for (Window w : Window.getWindows()) {
            if (w instanceof JDialog d && d.isShowing() && title.equals(d.getTitle())) {
                return d;
            }
        }
        return null;
    }

    private static int rowOf(JTable table, String firstColumn) {
        for (int row = 0; row < table.getRowCount(); row++) {
            if (firstColumn.equals(table.getValueAt(row, 0))) {
                return row;
            }
        }
        throw new IllegalStateException("행을 찾지 못했습니다: " + firstColumn);
    }

    private static JButton button(Component root, String text) {
        return find(root, c -> c instanceof JButton b && text.equals(b.getText()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Component root, Predicate<Component> test) {
        List<Component> found = new ArrayList<>();
        collect(root, test, found);
        if (found.isEmpty()) {
            throw new IllegalStateException("컴포넌트를 찾지 못했습니다");
        }
        return (T) found.get(0);
    }

    private static void collect(Component root, Predicate<Component> test, List<Component> found) {
        if (test.test(root)) {
            found.add(root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, test, found);
            }
        }
    }
}
