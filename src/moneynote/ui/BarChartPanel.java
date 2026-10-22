package moneynote.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import moneynote.service.CategoryShare;
import moneynote.util.MoneyFormat;

/**
 * 분류별 지출 막대그래프. (F7)
 *
 * <p>외부 차트 라이브러리를 쓰지 않고 paintComponent에서 직접 그린다. 창 크기가 바뀌면
 * 막대 폭과 간격을 그때그때 다시 계산하므로 별도의 리사이즈 처리가 필요 없다.
 */
public class BarChartPanel extends JPanel {

    private static final int PADDING = 24;
    private static final int LABEL_HEIGHT = 20;
    private static final Color BAR_COLOR = new Color(0x4A90D9);
    private static final Color TOP_BAR_COLOR = new Color(0xC0392B);

    private List<CategoryShare> shares = new ArrayList<>();

    /** 빈 그래프를 만든다. {@link #setShares(List)} 로 값을 넣는다. */
    public BarChartPanel() {
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(480, 260));
    }

    /**
     * 그릴 값을 바꾸고 다시 그린다. 첫 항목(가장 큰 분류)은 빨간 막대로 강조한다.
     *
     * @param shares 금액 내림차순의 분류별 지출, 비어 있으면 안내 문구를 그린다
     */
    public void setShares(List<CategoryShare> shares) {
        this.shares = new ArrayList<>(shares);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            if (shares.isEmpty()) {
                drawEmptyMessage(g2);
                return;
            }
            drawBars(g2);
        } finally {
            g2.dispose();
        }
    }

    private void drawEmptyMessage(Graphics2D g2) {
        String message = "표시할 지출 내역이 없습니다.";
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        g2.setColor(Color.GRAY);
        g2.drawString(message, Math.max(x, PADDING), y);
    }

    private void drawBars(Graphics2D g2) {
        int chartWidth = getWidth() - PADDING * 2;
        int chartHeight = getHeight() - PADDING * 2 - LABEL_HEIGHT;
        if (chartWidth <= 0 || chartHeight <= 0) {
            return;
        }

        long max = shares.stream().mapToLong(CategoryShare::amount).max().orElse(1L);
        int count = shares.size();
        int slot = chartWidth / count;
        int barWidth = Math.max(8, (int) (slot * 0.6));
        int baseY = PADDING + chartHeight;

        g2.setColor(new Color(0xDDDDDD));
        g2.drawLine(PADDING, baseY, PADDING + chartWidth, baseY);

        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < count; i++) {
            CategoryShare share = shares.get(i);
            int barHeight = (int) Math.round(share.amount() * 1.0 / max * chartHeight);
            int x = PADDING + i * slot + (slot - barWidth) / 2;
            int y = baseY - barHeight;

            // 최다 지출 분류만 다른 색으로 강조한다. 목록은 금액 내림차순이라 첫 항목이다.
            g2.setColor(i == 0 ? TOP_BAR_COLOR : BAR_COLOR);
            g2.fillRect(x, y, barWidth, barHeight);

            g2.setColor(Color.DARK_GRAY);
            String amount = MoneyFormat.plain(share.amount());
            g2.drawString(amount, centerText(fm, amount, x, barWidth), Math.max(y - 4, PADDING));

            String label = fit(fm, share.categoryName(), slot);
            g2.drawString(label, centerText(fm, label, x, barWidth), baseY + LABEL_HEIGHT - 4);
        }
    }

    private int centerText(FontMetrics fm, String text, int barX, int barWidth) {
        return barX + (barWidth - fm.stringWidth(text)) / 2;
    }

    /** 칸보다 긴 이름은 잘라내 옆 막대의 라벨과 겹치지 않게 한다. */
    private String fit(FontMetrics fm, String text, int available) {
        if (fm.stringWidth(text) <= available) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1 && fm.stringWidth(trimmed + "..") > available) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "..";
    }
}
