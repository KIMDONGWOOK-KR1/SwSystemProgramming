package moneynote.ui;

import java.awt.Dimension;
import javax.swing.JComboBox;

/**
 * 폭에 여유를 둔 콤보 상자.
 *
 * <p>Windows 모양에서는 선호 폭을 글꼴 너비로 계산하지만 선택 항목을 그릴 때는 안쪽 여백을 더 둔다.
 * 그래서 FlowLayout 처럼 선호 폭만 받는 자리에서는 "전체", "식비" 같은 짧은 한글 항목도
 * "..."으로 잘린다. 선호 폭에 여백만큼 더해 항목이 끝까지 보이게 한다.
 *
 * @param <E> 항목 타입
 */
public class RoomyComboBox<E> extends JComboBox<E> {

    /** 그릴 때 더해지는 여백보다 넉넉한 값. */
    private static final int EXTRA_WIDTH = 24;

    /** 빈 콤보 상자를 만든다. */
    public RoomyComboBox() {
        super();
    }

    /**
     * 항목을 채운 콤보 상자를 만든다.
     *
     * @param items 처음 채울 항목
     */
    public RoomyComboBox(E[] items) {
        super(items);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + EXTRA_WIDTH, d.height);
    }
}
