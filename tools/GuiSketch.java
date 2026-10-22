import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import moneynote.model.Category;
import moneynote.model.TransactionType;
import moneynote.repository.BudgetRepository;
import moneynote.repository.CategoryRepository;
import moneynote.repository.RecurringRepository;
import moneynote.repository.TransactionRepository;
import moneynote.service.BudgetService;
import moneynote.service.LedgerService;
import moneynote.service.RecurringService;
import moneynote.service.StatisticsService;
import moneynote.ui.CategoryDialog;
import moneynote.ui.MainFrame;
import moneynote.ui.RecurringDialog;
import moneynote.ui.TransactionDialog;

/**
 * 보고서의 GUI 설계 그림을 만든다.
 *
 *   javac -encoding UTF-8 -cp out -d out\tools tools\GuiSketch.java
 *   java -cp "out;out\tools" GuiSketch docs\cp4_figures
 *
 * 실제 Swing 창을 예시 데이터로 만들어 그린다. 각 그림에는 보고서 본문의 설명 번호와
 * 맞춘 빨간 번호표를 붙인다. 데이터 파일은 읽지도 쓰지도 않는다.
 * (체크포인트 3 때는 분류 관리·고정 지출 창이 없어 같은 컴포넌트로 설계안을 조립해 그렸다.)
 */
public class GuiSketch {

    private static final int SCALE = 2;
    private static final Color BADGE = new Color(0xD32F2F);
    private static final Color TITLE_BAR = new Color(0xF3F3F3);

    /**
     * 번호표 하나. 번호표 중심은 대상 영역 안의 비율 위치(fx, fy)에 놓는다.
     * 기본값은 오른쪽 위 모서리라 라벨 글자를 가리지 않는다.
     */
    private record Mark(Component target, Rectangle area, int number, double fx, double fy) {
        Mark(Component target, int number) {
            this(target, null, number, 1.0, 0.0);
        }

        Mark(Component target, int number, double fx, double fy) {
            this(target, null, number, fx, fy);
        }
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of(args.length > 0 ? args[0] : "docs/cp3_figures");
        Files.createDirectories(outDir);
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        SwingUtilities.invokeAndWait(() -> {
            try {
                renderAll(outDir);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.exit(0);
    }

    private static void renderAll(Path outDir) throws Exception {
        // 존재하지 않는 경로를 넘겨 실제 data 폴더와 분리한다. load()는 기본 분류만 시드한다.
        Path nowhere = Files.createTempDirectory("moneynote-sketch");
        CategoryRepository categories = new CategoryRepository(nowhere.resolve("categories.csv"));
        TransactionRepository transactions = new TransactionRepository(nowhere.resolve("transactions.csv"));
        BudgetRepository budgets = new BudgetRepository(nowhere.resolve("budgets.csv"));
        categories.load();
        seed(transactions, budgets);

        LedgerService ledger = new LedgerService(transactions, categories);
        StatisticsService statistics = new StatisticsService(transactions, categories);
        BudgetService budgetService = new BudgetService(budgets, categories, statistics);
        RecurringService recurring = new RecurringService(
                new RecurringRepository(nowhere.resolve("recurring.csv")), ledger);
        seedRules(recurring);

        MainFrame frame = new MainFrame(ledger, statistics, budgetService, recurring);
        frame.setSize(1000, 520);
        frame.validate();
        JTabbedPane tabs = find(frame.getContentPane(), JTabbedPane.class::isInstance);
        String title = frame.getTitle();

        // 1. 거래내역 탭.
        tabs.setSelectedIndex(0);
        JButton categoryButton = button(frame, "분류 관리");
        JTable txTable = find(tabs.getComponentAt(0), JTable.class::isInstance);
        Container filters = find(tabs.getComponentAt(0),
                c -> c instanceof JLabel l && "기간".equals(l.getText())).getParent();
        save(frame.getRootPane(), title, outDir.resolve("gui_main_transactions.png"), List.of(
                new Mark(tabs, tabs.getBoundsAt(2), 1, 1.35, 0.5),
                new Mark(button(frame, "등록"), 2),
                new Mark(categoryButton, 3),
                new Mark(filters.getComponent(1), 4),
                new Mark(button(frame, "이번 달"), 5),
                new Mark(filters.getComponent(10), 6, 0.8, 0.0),
                new Mark(button(frame, "조회"), 7),
                new Mark(txTable.getTableHeader(), 8, 0.12, 0.5),
                new Mark(find(tabs.getComponentAt(0),
                        c -> c instanceof JLabel l && l.getText().contains("건    수입")), 9, 0.40, 0.5)));

        // 2. 거래 등록 대화상자.
        TransactionDialog dialog = new TransactionDialog(frame, ledger, null);
        JRootPane dialogRoot = dialog.getRootPane();
        List<JTextField> fields = findAll(dialogRoot, JTextField.class::isInstance);
        fields.get(1).setText("12,000");
        fields.get(2).setText("점심 - 학생식당");
        dialog.validate();
        save(dialogRoot, dialog.getTitle(), outDir.resolve("gui_transaction_dialog.png"), List.of(
                new Mark(fields.get(0), 1),
                new Mark(find(dialogRoot, c -> c instanceof JRadioButton r && "지출".equals(r.getText())), 2, -0.3, 0.5),
                new Mark(fields.get(1), 3),
                new Mark(find(dialogRoot, JComboBox.class::isInstance), 4, 0.8, 0.0),
                new Mark(fields.get(2), 5),
                new Mark(button(dialogRoot, "저장"), 6)));
        dialog.dispose();

        // 3. 통계 탭.
        tabs.setSelectedIndex(1);
        frame.validate();
        Component stats = tabs.getComponentAt(1);
        save(frame.getRootPane(), title, outDir.resolve("gui_statistics.png"), List.of(
                new Mark(find(stats, c -> c instanceof JTextField), 1),
                new Mark(find(stats, c -> c instanceof JLabel l && "총수입".equals(l.getText())), 2, 0.42, 0.5),
                new Mark(find(stats, c -> c instanceof JLabel l && "전월 대비 지출".equals(l.getText())), 3, 0.75, 0.5),
                new Mark(find(stats, JTable.class::isInstance), 4, 0.98, 0.15),
                new Mark(find(stats, c -> c.getClass().getSimpleName().equals("BarChartPanel")), 5, 0.98, 0.08)));

        // 4. 예산 탭.
        tabs.setSelectedIndex(2);
        frame.validate();
        Component budget = tabs.getComponentAt(2);
        JTable budgetTable = find(budget, JTable.class::isInstance);
        save(frame.getRootPane(), title, outDir.resolve("gui_budget.png"), List.of(
                new Mark(find(budget, JTextField.class::isInstance), 1),
                new Mark(find(budget, JComboBox.class::isInstance), 2, 0.8, 0.0),
                new Mark(button(budget, "예산 설정"), 3),
                new Mark(budgetTable, 4, 0.98, 0.15),
                new Mark(budgetTable.getTableHeader(), 5, 0.9, 0.5)));

        // 5. 분류 관리 대화상자.
        CategoryDialog categoryDialog = new CategoryDialog(frame, ledger, recurring);
        JRootPane categoryRoot = categoryDialog.getRootPane();
        JTable categoryTable = find(categoryRoot, JTable.class::isInstance);
        JTextField nameField = find(categoryRoot, JTextField.class::isInstance);
        nameField.setText("구독");
        save(categoryRoot, categoryDialog.getTitle(), outDir.resolve("gui_category_dialog.png"), List.of(
                new Mark(categoryTable, 1, 0.5, 0.25),
                new Mark(categoryTable.getTableHeader(), 2, 0.62, 0.5),
                new Mark(nameField, 3),
                new Mark(find(categoryRoot, c -> c instanceof JRadioButton r && "지출".equals(r.getText())), 4),
                new Mark(button(categoryRoot, "추가"), 5),
                new Mark(button(categoryRoot, "이름 변경"), 6),
                new Mark(button(categoryRoot, "삭제"), 7)));
        categoryDialog.dispose();

        // 6. 고정 지출 관리 대화상자.
        RecurringDialog recurringDialog = new RecurringDialog(frame, recurring, categories);
        JRootPane recurringRoot = recurringDialog.getRootPane();
        JTable ruleTable = find(recurringRoot, JTable.class::isInstance);
        ruleTable.setRowSelectionInterval(1, 1);
        // 결제일 스피너 안의 편집 칸은 빼고 금액, 메모 칸만 고른다.
        List<JTextField> ruleFields = findAll(recurringRoot, c -> c instanceof JTextField
                && SwingUtilities.getAncestorOfClass(JSpinner.class, c) == null);
        ruleFields.get(0).setText("17,000");
        ruleFields.get(1).setText("영상 구독");
        JComboBox<?> ruleCategory = find(recurringRoot, JComboBox.class::isInstance);
        ruleCategory.setSelectedIndex(4);
        save(recurringRoot, recurringDialog.getTitle(), outDir.resolve("gui_recurring_dialog.png"), List.of(
                new Mark(ruleTable, 1, 0.97, 0.3),
                new Mark(ruleCategory, 2, 0.8, 0.0),
                new Mark(find(recurringRoot, JSpinner.class::isInstance), 3),
                new Mark(button(recurringRoot, "규칙 추가"), 4),
                new Mark(find(recurringRoot, JCheckBox.class::isInstance), 5),
                new Mark(button(recurringRoot, "규칙 삭제"), 6)));
        recurringDialog.dispose();

        frame.dispose();
    }

    /** 2026년 9월과 10월 초의 예시 거래. 오늘(10월 6일) 기준으로 예산 3단계가 모두 보이게 맞췄다. */
    private static void seed(TransactionRepository tx, BudgetRepository budgets) {
        TransactionType out = TransactionType.EXPENSE;
        TransactionType in = TransactionType.INCOME;
        // 분류 id: 1 식비, 2 교통, 3 주거, 4 통신, 5 문화, 7 쇼핑, 9 급여, 10 용돈
        tx.add(LocalDate.of(2026, 9, 1), in, 2_500_000, 9, "계좌이체", "9월 급여");
        tx.add(LocalDate.of(2026, 9, 5), out, 450_000, 3, "계좌이체", "9월 월세");
        tx.add(LocalDate.of(2026, 9, 12), out, 280_000, 1, "카드", "9월 식비 합계");
        tx.add(LocalDate.of(2026, 9, 20), out, 65_000, 4, "카드", "휴대폰 요금");
        tx.add(LocalDate.of(2026, 9, 25), out, 90_000, 2, "카드", "교통카드 충전");

        tx.add(LocalDate.of(2026, 10, 1), in, 2_500_000, 9, "계좌이체", "10월 급여");
        tx.add(LocalDate.of(2026, 10, 1), out, 450_000, 3, "계좌이체", "10월 월세 (자동)")
                .setAutoGenerated(true);
        tx.add(LocalDate.of(2026, 10, 2), out, 18_000, 2, "카드", "교통카드 충전");
        tx.add(LocalDate.of(2026, 10, 3), out, 62_000, 5, "카드", "콘서트 예매");
        tx.add(LocalDate.of(2026, 10, 4), out, 210_000, 1, "카드", "장보기");
        tx.add(LocalDate.of(2026, 10, 4), in, 100_000, 10, "현금", "용돈");
        tx.add(LocalDate.of(2026, 10, 5), out, 130_000, 1, "카드", "가족 외식");
        tx.add(LocalDate.of(2026, 10, 5), out, 39_000, 7, "카드", "운동화 할인");
        tx.add(LocalDate.of(2026, 10, 6), out, 12_000, 1, "현금", "점심 - 학생식당");

        YearMonth october = YearMonth.of(2026, 10);
        budgets.setLimit(1, october, 400_000);   // 식비 352,000 -> 88% 주의
        budgets.setLimit(2, october, 100_000);   // 교통 18,000 -> 정상
        budgets.setLimit(3, october, 500_000);   // 주거 450,000 -> 90% 주의
        budgets.setLimit(5, october, 50_000);    // 문화 62,000 -> 초과
    }

    /** 고정 지출 규칙 예시. 월세·휴대폰 요금은 이번 달 생성을 마쳤고 음악 구독은 꺼 두었다. */
    static void seedRules(RecurringService recurring) {
        // 분류 id: 3 주거, 4 통신, 5 문화
        YearMonth october = YearMonth.of(2026, 10);
        recurring.addRule(3, 450_000, 1, "월세").setLastGenerated(october);
        recurring.addRule(4, 65_000, 20, "휴대폰 요금").setLastGenerated(october);
        var music = recurring.addRule(5, 17_000, 31, "음악 구독");
        music.setLastGenerated(YearMonth.of(2026, 8));
        recurring.setActive(music.id(), false);
    }

    /** 컴포넌트를 2배율로 그리고 제목 표시줄과 번호표를 덧붙여 PNG로 저장한다. */
    private static void save(JRootPane root, String title, Path file, List<Mark> marks)
            throws Exception {
        int titleHeight = 30;
        int w = root.getWidth();
        int h = root.getHeight() + titleHeight;
        BufferedImage image = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.scale(SCALE, SCALE);

        // 운영체제 제목 표시줄은 그려지지 않으므로 비슷한 모양으로 대신 그린다.
        g.setColor(TITLE_BAR);
        g.fillRect(0, 0, w, titleHeight);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        g.drawString(title, 12, 20);
        g.drawString("—     ☐     ✕", w - 96, 20);

        Graphics2D body = (Graphics2D) g.create(0, titleHeight, w, h - titleHeight);
        root.printAll(body);
        body.dispose();

        g.setColor(new Color(0x9E9E9E));
        g.setStroke(new BasicStroke(1f));
        g.drawRect(0, 0, w - 1, h - 1);

        for (Mark mark : marks) {
            Rectangle r = mark.area() != null
                    ? SwingUtilities.convertRectangle(mark.target(), mark.area(), root)
                    : SwingUtilities.convertRectangle(
                            mark.target().getParent(), mark.target().getBounds(), root);
            int cx = r.x + (int) Math.round(r.width * mark.fx());
            int cy = r.y + (int) Math.round(r.height * mark.fy()) + titleHeight;
            drawBadge(g, cx, cy, w, h, mark.number());
        }
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
        System.out.println("저장: " + file + " (" + w + "x" + h + ")");
    }

    /** 중심이 (cx, cy)인 번호표를 그린다. 그림 밖으로 나가지 않게 안쪽으로 당긴다. */
    private static void drawBadge(Graphics2D g, int cx, int cy, int w, int h, int number) {
        // 보고서 양식이 2단이라 그림이 단 폭(약 8.5cm)으로 줄어도 번호가 읽히는 크기로 둔다.
        int size = 26;
        int x = Math.min(Math.max(1, cx - size / 2), w - size - 1);
        int y = Math.min(Math.max(1, cy - size / 2), h - size - 1);
        g.setColor(BADGE);
        g.fillRect(x, y, size, size);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 19));
        FontMetrics fm = g.getFontMetrics();
        String text = String.valueOf(number);
        g.drawString(text, x + (size - fm.stringWidth(text)) / 2, y + 20);
    }

    private static JButton button(Component root, String text) {
        return find(root, c -> c instanceof JButton b && text.equals(b.getText()));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T find(Component root, Predicate<Component> test) {
        List<Component> found = findAll(root, test);
        if (found.isEmpty()) {
            throw new IllegalStateException("컴포넌트를 찾지 못했습니다");
        }
        return (T) found.get(0);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> List<T> findAll(Component root, Predicate<Component> test) {
        List<T> result = new ArrayList<>();
        if (test.test(root)) {
            result.add((T) root);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                result.addAll(findAll(child, test));
            }
        }
        return result;
    }
}
