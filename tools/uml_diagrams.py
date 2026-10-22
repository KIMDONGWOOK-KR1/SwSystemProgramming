r"""보고서의 UML 그림을 만든다.

    python tools\uml_diagrams.py docs\cp4_figures

유스케이스 다이어그램 1장, 시퀀스 다이어그램 7장, 클래스 다이어그램 5장을 PNG로 그린다.

- 양식이 2단이라 그림은 단 폭(약 8.5cm)에 들어간다. 그 폭에서 글자가 6pt 안팎으로 읽히도록
  그림 폭을 520px 안쪽으로 잡고, 긴 클래스 이름은 두 줄로 나누고 메시지 인자는 줄여 쓴다.
- 유스케이스 다이어그램은 액터와 유스케이스 사이의 연관(association)까지만 그린다.
  «include», «extend» 같은 의존(dependency) 관계는 넣지 않는다 (수업 지침).
- 시퀀스·클래스 다이어그램의 이름은 src 의 실제 클래스·메서드 이름을 따른다.
- 클래스 다이어그램은 연관, 합성, 의존, 다중도를 쓴다. 위의 연관만 그리기 지침은
  체크포인트 3 유스케이스 다이어그램에만 해당한다.
"""

import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

S = 2  # 해상도 배율. 좌표는 1배 기준으로 쓰고 그릴 때 곱한다.
FONT_DIR = Path(r"C:\Windows\Fonts")
INK = (33, 33, 33)
LINE = (66, 66, 66)
SOFT = (105, 105, 105)
BOX_FILL = (255, 248, 225)
ACT_FILL = (245, 245, 245)
FRAG = (90, 90, 90)
UC_FILL = (255, 249, 230)
UC_EDGE = (120, 100, 60)


def font(size, bold=False):
    name = "malgunbd.ttf" if bold else "malgun.ttf"
    return ImageFont.truetype(str(FONT_DIR / name), size * S)


F_TEXT = font(14)
F_SMALL = font(13)
F_BOLD = font(13, bold=True)
F_TITLE = font(14, bold=True)


def text_w(draw, text, f):
    return draw.textlength(text, font=f) / S


class Canvas:
    """1배 좌표로 그리는 얇은 래퍼."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGB", (int(w * S), int(h * S)), "white")
        self.d = ImageDraw.Draw(self.img)

    def line(self, pts, fill=LINE, width=1.2, dash=None):
        pts = [(x * S, y * S) for x, y in pts]
        if not dash:
            self.d.line(pts, fill=fill, width=max(1, int(width * S)))
            return
        for (x1, y1), (x2, y2) in zip(pts, pts[1:]):
            length = math.hypot(x2 - x1, y2 - y1)
            if length == 0:
                continue
            on, off = dash[0] * S, dash[1] * S
            pos = 0.0
            while pos < length:
                end = min(pos + on, length)
                a = (x1 + (x2 - x1) * pos / length, y1 + (y2 - y1) * pos / length)
                b = (x1 + (x2 - x1) * end / length, y1 + (y2 - y1) * end / length)
                self.d.line([a, b], fill=fill, width=max(1, int(width * S)))
                pos += on + off

    def rect(self, x1, y1, x2, y2, fill=None, outline=LINE, width=1.2):
        self.d.rectangle([x1 * S, y1 * S, x2 * S, y2 * S], fill=fill, outline=outline,
                         width=max(1, int(width * S)))

    def ellipse(self, cx, cy, rx, ry, fill=UC_FILL, outline=UC_EDGE, width=1.3):
        self.d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S],
                       fill=fill, outline=outline, width=max(1, int(width * S)))

    def text(self, x, y, s, f=F_TEXT, fill=INK, anchor="la"):
        self.d.text((x * S, y * S), s, font=f, fill=fill, anchor=anchor)

    def polygon(self, pts, fill=LINE, outline=None):
        self.d.polygon([(x * S, y * S) for x, y in pts], fill=fill, outline=outline)

    def arrow_head(self, x1, y1, x2, y2, filled=True, size=8):
        ang = math.atan2(y2 - y1, x2 - x1)
        left = (x2 - size * math.cos(ang - 0.4), y2 - size * math.sin(ang - 0.4))
        right = (x2 - size * math.cos(ang + 0.4), y2 - size * math.sin(ang + 0.4))
        if filled:
            self.polygon([(x2, y2), left, right], fill=LINE)
        else:
            self.line([left, (x2, y2), right], width=1.2)

    def save(self, path):
        self.img.save(path, dpi=(96 * S, 96 * S))
        print(f"저장: {path} ({self.w:.0f}x{self.h:.0f})")


def stick_figure(c, cx, top, label, f=F_BOLD):
    """UML 액터. top 은 머리 위쪽 y. 이름 아래 끝의 y 를 돌려준다."""
    r = 8
    c.d.ellipse([(cx - r) * S, top * S, (cx + r) * S, (top + 2 * r) * S], outline=INK,
                width=int(1.4 * S))
    neck = top + 2 * r
    c.line([(cx, neck), (cx, neck + 20)], fill=INK, width=1.4)
    c.line([(cx - 14, neck + 7), (cx + 14, neck + 7)], fill=INK, width=1.4)
    c.line([(cx, neck + 20), (cx - 11, neck + 36)], fill=INK, width=1.4)
    c.line([(cx, neck + 20), (cx + 11, neck + 36)], fill=INK, width=1.4)
    c.text(cx, neck + 40, label, f, anchor="ma")
    return neck + 40 + 18


# --------------------------------------------------------------------------
# 시퀀스 다이어그램
# --------------------------------------------------------------------------

ROW = 28
HEAD_TOP = 30
LINE_H = 16  # 생명선 이름 한 줄 높이


def label_box(c, probe, x, y, text):
    """흰 바탕을 깔아 생명선 위에서도 읽히는 왼쪽 정렬 글자."""
    tw = text_w(probe, text, F_SMALL)
    c.rect(x - 2, y - 9, x + tw + 2, y + 9, fill="white", outline="white", width=0.5)
    c.text(x, y, text, F_SMALL, anchor="lm")


def draw_message(c, probe, x1, x2, y, dash, kind, label):
    c.line([(x1, y), (x2, y)], width=1.2, dash=dash)
    c.arrow_head(x1, y, x2, y, filled=(kind != "ret"))
    if not label:
        return
    mid = (x1 + x2) / 2
    tw = text_w(probe, label, F_SMALL)
    c.rect(mid - tw / 2 - 3, y - 20, mid + tw / 2 + 3, y - 3, fill="white", outline="white", width=0.5)
    c.text(mid, y - 4, label, F_SMALL, fill=INK if kind != "ret" else SOFT, anchor="md")


def sequence(title, participants, events, path):
    """participants: [(id, label, is_actor)]  label 의 "\n" 은 머리 상자 안 줄바꿈.
    events:
      ("call", a, b, text)   동기 메시지. b 에 활성 막대를 연다.
      ("send", a, b, text)   메시지. b 에 짧은 활성 막대만 그린다.
      ("ret", a, b, text)    응답(점선). a 의 가장 최근 활성 막대를 닫는다.
      ("self", a, text)      자기 호출.
      ("alt"|"opt"|"loop"|"break", guard) / ("else", guard) / ("end",)
      ("act", a) / ("deact", a)  활성 막대를 직접 열고 닫는다.
    """
    probe = ImageDraw.Draw(Image.new("RGB", (10, 10)))
    ids = [p[0] for p in participants]
    idx = {p: i for i, p in enumerate(ids)}

    def head_w(label):
        return max(text_w(probe, part, F_BOLD) for part in label.split("\n"))

    # 인접한 생명선 사이에 오가는 메시지 글자가 들어가도록 간격을 정한다.
    gaps = [0.0] * (len(ids) - 1)
    for i in range(len(ids) - 1):
        gaps[i] = max(78, (head_w(participants[i][1]) + head_w(participants[i + 1][1])) / 2 + 14)
    for ev in events:
        if ev[0] in ("call", "send", "ret"):
            a, b = idx[ev[1]], idx[ev[2]]
            lo, hi = min(a, b), max(a, b)
            need = text_w(probe, ev[3], F_SMALL) + 20
            have = sum(gaps[lo:hi])
            if have < need:
                extra = (need - have) / (hi - lo)
                for k in range(lo, hi):
                    gaps[k] += extra
        elif ev[0] == "self":
            a = idx[ev[1]]
            need = text_w(probe, ev[2], F_SMALL) + 40
            if a < len(gaps) and gaps[a] < need:
                gaps[a] = need

    left_pad = max(40, head_w(participants[0][1]) / 2 + 12)
    xs = [left_pad]
    for g in gaps:
        xs.append(xs[-1] + g)
    right_pad = max(30, head_w(participants[-1][1]) / 2 + 12)
    for ev in events:
        if ev[0] == "self" and ev[1] == ids[-1]:
            right_pad = max(right_pad, text_w(probe, ev[2], F_SMALL) + 46)
    width = xs[-1] + right_pad

    max_lines = max(len(p[1].split("\n")) for p in participants if not p[2])
    box_h = max_lines * LINE_H + 10
    head_h = max(box_h, 62) + 16
    rows = 0
    for ev in events:
        k = ev[0]
        rows += {"self": 1.6, "alt": 1.25, "opt": 1.25, "loop": 1.25, "break": 1.25, "else": 1.25,
                 "end": 0.5, "act": 0, "deact": 0}.get(k, 1)
    height = HEAD_TOP + head_h + rows * ROW + 40

    c = Canvas(width, height)
    c.text(6, 4, title, F_TITLE)
    body_top = HEAD_TOP + head_h

    for (pid, label, is_actor), x in zip(participants, xs):
        if is_actor:
            stick_figure(c, x, HEAD_TOP, label)
        else:
            lines = label.split("\n")
            w = head_w(label) + 12
            y1 = HEAD_TOP + head_h - 16 - box_h
            c.rect(x - w / 2, y1, x + w / 2, y1 + box_h, fill=BOX_FILL, outline=LINE)
            for k, part in enumerate(lines):
                ty = y1 + 5 + (box_h - 10 - len(lines) * LINE_H) / 2 + k * LINE_H + LINE_H / 2
                c.text(x, ty, part, F_BOLD, anchor="mm")
                # 객체 이름 밑줄 (UML 인스턴스 표기)
                tw = text_w(probe, part, F_BOLD)
                c.line([(x - tw / 2, ty + 8), (x + tw / 2, ty + 8)], fill=INK, width=0.7)
        c.line([(x, body_top - 6), (x, height - 16)], fill=SOFT, width=1, dash=(5, 4))

    act_stack = {p: [] for p in ids}
    acts = []
    frags = []
    done_frags = []
    ops = []
    y = body_top + 4

    def depth_of(pid):
        return len(act_stack[pid])

    def edge_x(pid, toward_right, at_depth=None):
        d = depth_of(pid) if at_depth is None else at_depth
        x = xs[idx[pid]]
        if d <= 0 or participants[idx[pid]][2]:
            return x
        return x + 5 + (d - 1) * 4 if toward_right else x - 5 + (d - 1) * 4

    def touch(*pids, right_edge=0.0):
        for f in frags:
            f["who"].update(pids)
            f["xmax"] = max(f["xmax"], right_edge)

    for ev in events:
        kind = ev[0]
        if kind in ("call", "send", "ret"):
            a, b, label = ev[1], ev[2], ev[3]
            touch(a, b)
            y += ROW
            right = xs[idx[b]] > xs[idx[a]]
            b_actor = participants[idx[b]][2]
            if kind == "call" and not b_actor:
                act_stack[b].append(y)
            if kind == "send" and not b_actor:
                acts.append((b, depth_of(b) + 1, y, y + 14))
            x1 = edge_x(a, right)
            if kind == "send" and not b_actor:
                x2 = edge_x(b, not right, depth_of(b) + 1)
            else:
                x2 = edge_x(b, not right)
            dash = (6, 4) if kind == "ret" else None
            ops.append(lambda x1=x1, x2=x2, y=y, dash=dash, kind=kind, label=label:
                       draw_message(c, probe, x1, x2, y, dash, kind, label))
            if kind == "ret" and act_stack[a]:
                start = act_stack[a].pop()
                acts.append((a, len(act_stack[a]) + 1, start, y))
        elif kind == "self":
            a, label = ev[1], ev[2]
            x0 = edge_x(a, True)
            touch(a, right_edge=x0 + 31 + text_w(probe, label, F_SMALL) + 8)
            y += ROW
            d = depth_of(a) + 1
            x_in = xs[idx[a]] + 5 + (d - 1) * 4

            def self_op(x0=x0, x_in=x_in, y=y, label=label):
                c.line([(x0, y - 8), (x0 + 26, y - 8), (x0 + 26, y + 10), (x_in + 5, y + 10)], width=1.2)
                c.arrow_head(x0 + 26, y + 10, x_in + 5, y + 10, filled=True)
                label_box(c, probe, x0 + 31, y - 1, label)
            ops.append(self_op)
            acts.append((a, d, y + 4, y + 20))
            y += 0.6 * ROW
        elif kind in ("alt", "opt", "loop", "break"):
            y += 0.75 * ROW
            frags.append({"kind": kind, "guard": ev[1], "y1": y - 12, "elses": [], "who": set(),
                          "depth": len(frags), "xmax": 0.0})
            y += 0.5 * ROW
        elif kind == "else":
            y += 0.75 * ROW
            frags[-1]["elses"].append((y - 6, ev[1]))
            y += 0.5 * ROW
        elif kind == "end":
            y += 0.5 * ROW
            f = frags.pop()
            f["y2"] = y
            done_frags.append(f)
            for outer in frags:
                outer["who"].update(f["who"])
                outer["xmax"] = max(outer["xmax"], f["xmax"])
        elif kind == "act":
            act_stack[ev[1]].append(y + 4)
        elif kind == "deact":
            if act_stack[ev[1]]:
                start = act_stack[ev[1]].pop()
                acts.append((ev[1], len(act_stack[ev[1]]) + 1, start, y + 6))

    for pid in ids:
        while act_stack[pid]:
            start = act_stack[pid].pop()
            acts.append((pid, len(act_stack[pid]) + 1, start, y + 8))

    for f in done_frags:
        who = [idx[p] for p in f["who"]] or [0]
        x1 = max(3, xs[min(who)] - 40 + f["depth"] * 5)
        x2 = min(width - 3, max(xs[max(who)] + 34, f["xmax"]) - f["depth"] * 5)
        c.rect(x1, f["y1"], x2, f["y2"], outline=FRAG, width=1.1)
        tag = f["kind"]
        tw = text_w(probe, tag, F_BOLD) + 12

        # 이름표는 활성 막대에 가려지지 않도록 맨 마지막에 그린다.
        def tag_op(x1=x1, y1=f["y1"], tw=tw, tag=tag):
            c.polygon([(x1, y1), (x1 + tw, y1), (x1 + tw, y1 + 12),
                       (x1 + tw - 6, y1 + 18), (x1, y1 + 18)], fill=(236, 236, 236))
            c.line([(x1, y1), (x1 + tw, y1), (x1 + tw, y1 + 12), (x1 + tw - 6, y1 + 18),
                    (x1, y1 + 18)], fill=FRAG, width=1.1)
            c.text(x1 + 5, y1 + 9, tag, F_BOLD, anchor="lm")
        ops.append(tag_op)
        g = f"[{f['guard']}]"
        ops.append(lambda x=x1 + tw + 6, yy=f["y1"] + 9, g=g: label_box(c, probe, x, yy, g))
        for ey, guard in f["elses"]:
            c.line([(x1, ey), (x2, ey)], fill=FRAG, width=1, dash=(5, 3))
            g = f"[{guard}]"
            ops.append(lambda x1=x1, ey=ey, g=g: label_box(c, probe, x1 + 6, ey + 10, g))

    for pid, depth, y1, y2 in sorted(acts, key=lambda a: a[1]):
        x = xs[idx[pid]] + (depth - 1) * 4
        c.rect(x - 5, y1 - 2, x + 5, y2 + 2, fill=ACT_FILL, outline=LINE, width=1)

    for op in ops:
        op()

    out_h = int(min(height, y + 30))
    c.img = c.img.crop((0, 0, int(width * S), out_h * S))
    c.h = out_h
    c.save(path)


U = ("U", "사용자", True)


def sd_register(path):
    P = [U, ("TP", ":Transaction\nPanel", False), ("TD", ":Transaction\nDialog", False),
         ("LS", ":Ledger\nService", False), ("TR", ":Transaction\nRepository", False)]
    E = [
        ("call", "U", "TP", "등록 클릭"),
        ("call", "TP", "TD", "new(owner, ledger, null)"),
        ("send", "U", "TD", "값 입력 후 저장"),
        ("self", "TD", "날짜·금액 변환"),
        ("opt", "날짜가 오늘 이후"),
        ("send", "TD", "U", "등록 확인 질문"),
        ("send", "U", "TD", "예"),
        ("end",),
        ("call", "TD", "LS", "register(...)"),
        ("self", "LS", "validate(...)"),
        ("alt", "검증 통과"),
        ("call", "LS", "TR", "add(...)"),
        ("ret", "TR", "LS", "Transaction"),
        ("ret", "LS", "TD", "Transaction"),
        ("ret", "TD", "TP", "saved = true"),
        ("self", "TP", "refresh()"),
        ("self", "TP", "onDataChanged"),
        ("else", "검증 실패"),
        ("act", "TD"),
        ("act", "LS"),
        ("ret", "LS", "TD", "예외(메시지)"),
        ("send", "TD", "U", "입력 확인 경고"),
        ("end",),
    ]
    sequence("SD1  거래 등록", P, E, path)


def sd_search(path):
    P = [U, ("TP", ":Transaction\nPanel", False), ("TF", ":Transaction\nFilter", False),
         ("LS", ":Ledger\nService", False), ("TR", ":Transaction\nRepository", False)]
    E = [
        ("call", "U", "TP", "조건 입력 후 조회"),
        ("self", "TP", "buildFilter()"),
        ("call", "TP", "TF", "period().type()..."),
        ("ret", "TF", "TP", "filter"),
        ("call", "TP", "LS", "find(filter)"),
        ("call", "LS", "TR", "findAll()"),
        ("ret", "TR", "LS", "거래 목록"),
        ("self", "LS", "matches(), 정렬"),
        ("ret", "LS", "TP", "List<Transaction>"),
        ("self", "TP", "setRows(found)"),
        ("self", "TP", "updateStatus()"),
        ("alt", "0건"),
        ("send", "TP", "U", "\"거래가 없습니다\""),
        ("else", "1건 이상"),
        ("send", "TP", "U", "건수·합계 표시"),
        ("end",),
    ]
    sequence("SD2  거래 조회 및 검색", P, E, path)


def sd_category_delete(path):
    P = [U, ("CD", ":Category\nDialog", False), ("RS", ":Recurring\nService", False),
         ("LS", ":Ledger\nService", False), ("CR", ":Category\nRepository", False)]
    E = [
        ("call", "U", "CD", "분류 선택, 삭제"),
        ("call", "CD", "RS", "countByCategory(id)"),
        ("ret", "RS", "CD", "rules"),
        ("break", "rules > 0"),
        ("send", "CD", "U", "\"규칙 N개 있음\""),
        ("end",),
        ("send", "CD", "U", "삭제 확인 질문"),
        ("send", "U", "CD", "예"),
        ("call", "CD", "LS", "deleteCategory(id)"),
        ("self", "LS", "usageCount(id)"),
        ("alt", "used > 0"),
        ("ret", "LS", "CD", "예외"),
        ("send", "CD", "U", "\"사용 중 N건\""),
        ("else", "used = 0"),
        ("act", "LS"),
        ("call", "LS", "CR", "remove(id)"),
        ("ret", "CR", "LS", ""),
        ("ret", "LS", "CD", "완료"),
        ("self", "CD", "reload()"),
        ("end",),
    ]
    sequence("SD3  분류 삭제", P, E, path)



def sd_statistics(path):
    P = [U, ("SP", ":Statistics\nPanel", False), ("SS", ":Statistics\nService", False),
         ("TR", ":Transaction\nRepository", False), ("BC", ":BarChart\nPanel", False)]
    E = [
        ("call", "U", "SP", "조회 월 입력"),
        ("self", "SP", "YearMonth.parse()"),
        ("call", "SP", "SS", "summarize(month)"),
        ("call", "SS", "TR", "findAll()"),
        ("ret", "TR", "SS", "거래 목록"),
        ("self", "SS", "월 합계"),
        ("ret", "SS", "SP", "MonthlySummary"),
        ("call", "SP", "SS", "expenseByCategory()"),
        ("self", "SS", "분류별 합산"),
        ("ret", "SS", "SP", "List<CategoryShare>"),
        ("call", "SP", "BC", "setShares(shares)"),
        ("self", "BC", "막대 그리기"),
        ("ret", "BC", "SP", ""),
        ("send", "SP", "U", "요약·표·그래프 표시"),
    ]
    sequence("SD4  수지 요약과 지출 통계", P, E, path)


def sd_budget(path):
    P = [U, ("BP", ":Budget\nPanel", False), ("BS", ":Budget\nService", False),
         ("BR", ":Budget\nRepository", False), ("SS", ":Statistics\nService", False)]
    E = [
        ("call", "U", "BP", "분류·한도 입력"),
        ("call", "BP", "BS", "setLimit(...)"),
        ("call", "BS", "BR", "setLimit(...)"),
        ("ret", "BR", "BS", "Budget"),
        ("ret", "BS", "BP", ""),
        ("call", "BP", "BS", "statuses(month)"),
        ("loop", "지출 분류마다"),
        ("call", "BS", "SS", "expenseOf(...)"),
        ("ret", "SS", "BS", "used"),
        ("call", "BS", "BR", "find(...)"),
        ("ret", "BR", "BS", "limit"),
        ("self", "BS", "levelOf()"),
        ("end",),
        ("ret", "BS", "BP", "List<BudgetStatus>"),
        ("send", "BP", "U", "정상·주의·초과 표시"),
    ]
    sequence("SD5  예산 설정과 현황 판정", P, E, path)


def sd_startup(path):
    P = [U, ("M", ":Main", False), ("RS", ":Recurring\nService", False),
         ("LS", ":Ledger\nService", False), ("TR", ":Transaction\nRepository", False)]
    E = [
        ("call", "U", "M", "프로그램 실행"),
        ("call", "M", "TR", "load()"),
        ("ret", "TR", "M", "skippedRows"),
        ("self", "M", "규칙·분류·예산 load()"),
        ("call", "M", "RS", "generateFor(month)"),
        ("loop", "활성, lastGenerated < month"),
        ("self", "RS", "dayInMonth()"),
        ("call", "RS", "LS", "register(...)"),
        ("call", "LS", "TR", "add(...)"),
        ("ret", "TR", "LS", ""),
        ("ret", "LS", "RS", "Transaction"),
        ("self", "RS", "setLastGenerated(month)"),
        ("end",),
        ("ret", "RS", "M", "created"),
        ("self", "M", "MainFrame 표시"),
        ("opt", "created > 0"),
        ("send", "M", "U", "\"N건 추가\""),
        ("end",),
    ]
    sequence("SD6  시작: 불러오기와 고정 지출 생성", P, E, path)



def sd_shutdown(path):
    P = [U, ("MF", ":MainFrame", False), ("LS", ":Ledger\nService", False),
         ("BR", ":Budget\nRepository", False), ("RS", ":Recurring\nService", False)]
    E = [
        ("call", "U", "MF", "창 닫기"),
        ("call", "MF", "LS", "save()"),
        ("self", "LS", "거래·분류 save()"),
        ("ret", "LS", "MF", ""),
        ("call", "MF", "BR", "save()"),
        ("ret", "BR", "MF", ""),
        ("call", "MF", "RS", "save()"),
        ("ret", "RS", "MF", ""),
        ("alt", "IOException"),
        ("send", "MF", "U", "\"저장 없이 종료?\""),
        ("self", "MF", "예: 종료, 아니오: 보류"),
        ("else", "저장 성공"),
        ("self", "MF", "dispose()"),
        ("end",),
    ]
    sequence("SD7  종료: 데이터 저장", P, E, path)



# --------------------------------------------------------------------------
# 유스케이스 다이어그램
# --------------------------------------------------------------------------

def usecase_diagram(path):
    """단 폭에 들어가는 세로형 배치. 연관(실선)만 그린다."""
    W, H = 500, 800
    c = Canvas(W, H)
    c.text(6, 4, "MoneyNote 유스케이스 다이어그램", F_TITLE)

    box = (112, 34, 404, H - 10)
    c.rect(*box, outline=INK, width=1.4)
    c.text((box[0] + box[2]) / 2, box[1] + 6, "MoneyNote", F_BOLD, anchor="ma")

    # 사용자 쪽으로 오목한 초승달 모양으로 늘어놓아 연관 선이 타원을 가로지르지 않게 한다.
    cases = [
        ("UC15", "데이터 저장"),
        ("UC14", "데이터 불러오기"),
        ("UC13", "고정 지출 자동 생성"),
        ("UC1", "거래 등록"),
        ("UC2", "거래 수정"),
        ("UC3", "거래 삭제"),
        ("UC4", "거래 조회 및 검색"),
        ("UC5", "분류 추가"),
        ("UC6", "분류 이름 변경"),
        ("UC7", "분류 삭제"),
        ("UC8", "월별 수지 요약 조회"),
        ("UC9", "분류별 지출 통계 조회"),
        ("UC10", "예산 한도 설정"),
        ("UC11", "예산 사용 현황 확인"),
        ("UC12", "고정 지출 규칙 관리"),
    ]
    rx, ry = 86, 21
    top, bottom = 88, H - 36
    n = len(cases)
    step = (bottom - top) / (n - 1)
    pos = {}
    for i, (uc, _) in enumerate(cases):
        cy = top + i * step
        t = (cy - (top + bottom) / 2) / ((bottom - top) / 2 + 40)
        cx = 228 + 62 * math.sqrt(max(0.0, 1 - t * t))
        pos[uc] = (cx, cy)

    actor_x, actor_top = 52, H / 2 - 52
    ax, ay = actor_x + 14, actor_top + 23
    for uc, _ in cases:
        cx, cy = pos[uc]
        ang = math.atan2(cy - ay, cx - ax)
        ex = cx - rx * math.cos(ang) * 0.99
        ey = cy - ry * math.sin(ang) * 0.99
        c.line([(ax, ay), (ex, ey)], fill=LINE, width=1.1)

    fs_x, fs_top = 456, 52
    for uc in ("UC14", "UC15"):
        cx, cy = pos[uc]
        c.line([(fs_x - 14, fs_top + 23), (cx + rx * 0.97, cy)], fill=LINE, width=1.1)

    for uc, name in cases:
        cx, cy = pos[uc]
        c.ellipse(cx, cy, rx, ry)
        c.text(cx, cy - 8, uc, F_SMALL, fill=SOFT, anchor="mm")
        c.text(cx, cy + 8, name, F_TEXT, anchor="mm")

    stick_figure(c, actor_x, actor_top, "사용자")
    end = stick_figure(c, fs_x, fs_top, "파일")
    c.text(fs_x, end - 2, "시스템", F_BOLD, anchor="ma")
    c.save(path)


# --------------------------------------------------------------------------
# 클래스 다이어그램
# --------------------------------------------------------------------------

CLS_FILL = (232, 241, 252)
CLS_HEAD = (208, 225, 246)
CLS_EDGE = (60, 84, 120)
PKG_EDGE = (150, 150, 150)
CLS_LH = 16   # 클래스 상자 한 줄 높이
CLS_PAD = 5
PROBE = ImageDraw.Draw(Image.new("RGB", (10, 10)))


class Cls:
    """클래스 상자. ops 의 "_" 로 시작하는 항목은 static 이라 밑줄을 긋는다.

    attrs 와 ops 가 모두 비어 있으면 이름 칸만 그린다(다른 그림에서 자세히 보인 클래스).
    """

    def __init__(self, name, x, y, w, attrs=(), ops=(), stereo=None, detail=None):
        self.name, self.x, self.y, self.w = name, x, y, w
        self.attrs, self.ops, self.stereo = list(attrs), list(ops), stereo
        self.detail = bool(attrs or ops) if detail is None else detail
        lines = len(name.split("\n")) + (1 if stereo else 0)
        self.head_h = CLS_PAD * 2 + CLS_LH * lines
        self.attr_h = CLS_PAD * 2 + CLS_LH * len(self.attrs) if self.detail else 0
        self.op_h = CLS_PAD * 2 + CLS_LH * len(self.ops) if self.detail else 0
        self.h = self.head_h + self.attr_h + self.op_h
        for t in self.attrs + self.ops:
            if text_w(PROBE, t.lstrip("_"), F_SMALL) > w - 2 * CLS_PAD:
                print(f"  경고: '{name}' 상자에 글자가 넘친다: {t}")
        for t in name.split("\n"):
            if text_w(PROBE, t, F_BOLD) > w - 2 * CLS_PAD:
                print(f"  경고: '{name}' 이름이 넘친다")

    @property
    def bottom(self):
        return self.y + self.h

    def anchor(self, side, f=0.5):
        return {
            "top": (self.x + self.w * f, self.y),
            "bottom": (self.x + self.w * f, self.y + self.h),
            "left": (self.x, self.y + self.h * f),
            "right": (self.x + self.w, self.y + self.h * f),
        }[side]

    def draw(self, c):
        x, y, w = self.x, self.y, self.w
        c.rect(x, y, x + w, y + self.h, fill=CLS_FILL, outline=CLS_EDGE, width=1.2)
        c.rect(x, y, x + w, y + self.head_h, fill=CLS_HEAD, outline=CLS_EDGE, width=1.2)
        ty = y + CLS_PAD
        if self.stereo:
            c.text(x + w / 2, ty, f"«{self.stereo}»", F_SMALL, fill=SOFT, anchor="ma")
            ty += CLS_LH
        for part in self.name.split("\n"):
            c.text(x + w / 2, ty, part, F_BOLD, anchor="ma")
            ty += CLS_LH
        if not self.detail:
            return
        ty = y + self.head_h
        c.line([(x, ty + self.attr_h), (x + w, ty + self.attr_h)], fill=CLS_EDGE, width=1.0)
        ty += CLS_PAD
        for a in self.attrs:
            c.text(x + CLS_PAD, ty, a, F_SMALL)
            ty += CLS_LH
        ty = y + self.head_h + self.attr_h + CLS_PAD
        for o in self.ops:
            static = o.startswith("_")
            text = o.lstrip("_")
            c.text(x + CLS_PAD, ty, text, F_SMALL)
            if static:
                tw = text_w(PROBE, text, F_SMALL)
                c.line([(x + CLS_PAD, ty + 14), (x + CLS_PAD + tw, ty + 14)], fill=INK, width=0.8)
            ty += CLS_LH


def _route(p1, sa, p2, sb):
    (x1, y1), (x2, y2) = p1, p2
    vert = ("top", "bottom")
    if sa in vert and sb in vert:
        if abs(x1 - x2) < 0.5:
            return []
        ym = (y1 + y2) / 2
        return [(x1, ym), (x2, ym)]
    if sa not in vert and sb not in vert:
        if abs(y1 - y2) < 0.5:
            return []
        xm = (x1 + x2) / 2
        return [(xm, y1), (xm, y2)]
    if sa in vert:
        return [(x1, y2)]
    return [(x2, y1)]


def rel(c, a, sa, b, sb, kind="assoc", fa=0.5, fb=0.5, via=None, label=None,
        ma=None, mb=None, lpos=None):
    """a 에서 b 로 가는 관계선.

    kind: assoc(실선, b 쪽 열린 화살표), dep(점선, 열린 화살표), comp(a 쪽 채운 마름모),
          gen(b 쪽 빈 삼각형). ma, mb 는 양 끝의 다중도, label 은 선 이름이다.
    via 를 주면 그 꺾임점들을 지난다.
    """
    p1, p2 = a.anchor(sa, fa), b.anchor(sb, fb)
    pts = [p1] + (via if via is not None else _route(p1, sa, p2, sb)) + [p2]
    c.line(pts, fill=LINE, width=1.1, dash=(5, 3) if kind == "dep" else None)
    (qx, qy), (ex, ey) = pts[-2], pts[-1]
    if kind in ("assoc", "dep"):
        c.arrow_head(qx, qy, ex, ey, filled=False, size=9)
    elif kind == "gen":
        ang = math.atan2(ey - qy, ex - qx)
        base = (ex - 12 * math.cos(ang), ey - 12 * math.sin(ang))
        px, py = -math.sin(ang) * 7, math.cos(ang) * 7
        c.polygon([(ex, ey), (base[0] + px, base[1] + py), (base[0] - px, base[1] - py)],
                  fill="white", outline=LINE)
    if kind == "comp":
        (sx, sy), (nx, ny) = pts[0], pts[1]
        ang = math.atan2(ny - sy, nx - sx)
        dx, dy = math.cos(ang), math.sin(ang)
        px, py = -dy * 5, dx * 5
        c.polygon([(sx, sy), (sx + dx * 8 + px, sy + dy * 8 + py), (sx + dx * 16, sy + dy * 16),
                   (sx + dx * 8 - px, sy + dy * 8 - py)], fill=LINE)
        c.arrow_head(qx, qy, ex, ey, filled=False, size=9)

    def end_label(point, toward, text, offset):
        (x, y), (tx, ty) = point, toward
        horizontal = abs(tx - x) > abs(ty - y)
        if horizontal:
            sx = 1 if tx > x else -1
            c.text(x + sx * offset, y - 3, text, F_SMALL, fill=SOFT,
                   anchor="ld" if sx > 0 else "rd")
        else:
            sy = 1 if ty > y else -1
            c.text(x + 5, y + sy * (offset + 6), text, F_SMALL, fill=SOFT, anchor="lm")

    # 마름모나 화살촉을 피해서 다중도를 쓴다.
    if ma:
        end_label(pts[0], pts[1], ma, 20 if kind == "comp" else 5)
    if mb:
        end_label(pts[-1], pts[-2], mb, 13)
    if label:
        if lpos is None:
            seg = max(zip(pts, pts[1:]), key=lambda s_: math.hypot(s_[1][0] - s_[0][0],
                                                                    s_[1][1] - s_[0][1]))
            lpos = ((seg[0][0] + seg[1][0]) / 2 + 4, (seg[0][1] + seg[1][1]) / 2)
        c.text(lpos[0], lpos[1], label, F_SMALL, fill=SOFT, anchor="lm")


def package(c, x1, y1, x2, y2, name):
    tab_w = text_w(PROBE, name, F_BOLD) + 14
    c.rect(x1, y1, x1 + tab_w, y1 + 18, fill=(244, 244, 244), outline=PKG_EDGE, width=1.1)
    c.text(x1 + 7, y1 + 2, name, F_BOLD, fill=SOFT)
    c.rect(x1, y1 + 18, x2, y2, outline=PKG_EDGE, width=1.1)


def cd_overview(path):
    """패키지와 클래스 배치. 패키지 사이의 의존 방향만 보인다."""
    W = 520
    c = Canvas(W, 10)
    bw, gap, left = 118, 8, 14
    col = [left + i * (bw + gap) for i in range(4)]

    def grid(names, top):
        boxes = []
        for i, n in enumerate(names):
            stereo = None
            if isinstance(n, tuple):
                n, stereo = n
            boxes.append(Cls(n, col[i % 4], top + (i // 4) * 52, bw, stereo=stereo))
        return boxes

    groups = [
        ("moneynote", [], [("Main", None)]),
        ("moneynote.ui", ["MainFrame", "Transaction\nPanel", "Statistics\nPanel", "Budget\nPanel",
                          "Transaction\nDialog", "Category\nDialog", "Recurring\nDialog",
                          "BarChart\nPanel", "Transaction\nTableModel", "Roomy\nComboBox"], None),
        ("moneynote.service", ["Ledger\nService", "Recurring\nService", "Statistics\nService",
                               "Budget\nService", "Transaction\nFilter", "Monthly\nSummary",
                               "Category\nShare", "Budget\nStatus"], None),
        ("moneynote.repository", ["Transaction\nRepository", "Recurring\nRepository",
                                  "Category\nRepository", "Budget\nRepository", "CsvStore"], None),
        ("moneynote.model", ["Transaction", "RecurringRule", "Category", "Budget",
                             ("TransactionType", "enumeration")], None),
    ]
    y = 30
    frames = {}
    all_boxes = []
    for name, names, special in groups:
        if special:
            boxes = [Cls("Main", col[0], y + 26, bw)]
            util = [Cls("DateUtil", col[2], y + 26, bw), Cls("MoneyFormat", col[3], y + 26, bw)]
            frames["moneynote"] = (8, y, col[1] + bw + 6, y + 26 + 42 + 8)
            frames["moneynote.util"] = (col[2] - 6, y, W - 8, y + 26 + 42 + 8)
            all_boxes += boxes + util
            y += 26 + 42 + 8 + 26
            continue
        boxes = grid(names, y + 26)
        bottom = max(b.bottom for b in boxes)
        frames[name] = (8, y, W - 8, bottom + 8)
        all_boxes += boxes
        y = bottom + 8 + 26
    c = Canvas(W, y - 14)
    c.text(6, 4, "CD1  패키지와 클래스 개요", F_TITLE)
    for name, (x1, y1, x2, y2) in frames.items():
        package(c, x1, y1, x2, y2, name)
    for b in all_boxes:
        b.draw(c)

    # 패키지 사이 의존(점선 화살표). 위 패키지의 아래 변에서 아래 패키지의 탭으로 간다.
    order = ["moneynote", "moneynote.ui", "moneynote.service", "moneynote.repository",
             "moneynote.model"]
    for upper, lower in zip(order, order[1:]):
        ux = 60 if upper == "moneynote" else W / 2
        y1 = frames[upper][3]
        y2 = frames[lower][1]
        c.line([(ux, y1), (ux, y2)], fill=LINE, width=1.1, dash=(5, 3))
        c.arrow_head(ux, y1, ux, y2, filled=False, size=9)
        c.text(ux + 6, (y1 + y2) / 2, "«use»", F_SMALL, fill=SOFT, anchor="lm")
    # service 는 model 도 직접 쓴다. 오른쪽 바깥 통로로 돌린다.
    sx = W - 3
    sy = (frames["moneynote.service"][1] + frames["moneynote.service"][3]) / 2
    my = frames["moneynote.model"][1] + 30
    c.line([(frames["moneynote.service"][2], sy), (sx, sy), (sx, my),
            (frames["moneynote.model"][2], my)], fill=LINE, width=1.1, dash=(5, 3))
    c.arrow_head(sx, my, frames["moneynote.model"][2], my, filled=False, size=9)
    c.save(path)


def cd_model(path):
    W = 520
    tx = Cls("Transaction", 10, 34, 236,
             ["- id: int", "- date: LocalDate", "- type: TransactionType", "- amount: long",
              "- categoryId: int", "- payment: String", "- memo: String",
              "- autoGenerated: boolean"],
             ["+ signedAmount(): long", "+ setAutoGenerated(b): void"])
    cat = Cls("Category", 300, 34, 210,
              ["- id: int", "- name: String", "- type: TransactionType", "- seeded: boolean"],
              ["+ rename(name): void"])
    rr = Cls("RecurringRule", 10, tx.bottom + 40, 236,
             ["- id: int", "- categoryId: int", "- amount: long", "- dayOfMonth: int",
              "- memo: String", "- active: boolean", "- lastGenerated: YearMonth"],
             ["+ setActive(active): void", "+ setLastGenerated(m): void"])
    bud = Cls("Budget", 300, cat.bottom + 70, 210,
              ["- categoryId: int", "- month: YearMonth", "- limitAmount: long"],
              ["+ setLimitAmount(limit): void"])
    tt = Cls("TransactionType", 300, bud.bottom + 40, 210,
             ["INCOME", "EXPENSE"], ["+ label(): String"], stereo="enumeration")
    H = max(rr.bottom, tt.bottom) + 30
    c = Canvas(W, H)
    c.text(6, 4, "CD2  model 패키지", F_TITLE)
    for b in (tx, cat, rr, bud, tt):
        b.draw(c)
    link_y = 90
    rel(c, tx, "right", cat, "left", fa=(link_y - tx.y) / tx.h, fb=(link_y - cat.y) / cat.h,
        ma="0..*", mb="1")
    rel(c, rr, "right", cat, "left", fa=0.3, fb=0.8,
        via=[(282, rr.anchor("right", 0.3)[1]), (282, cat.anchor("left", 0.8)[1])],
        ma="0..*", mb="1")
    rel(c, bud, "top", cat, "bottom", ma="0..*", mb="1")
    c.text(10, H - 22, "categoryId 로 Category 를 참조한다. 접근자(getter·setter)는 생략.",
           F_SMALL, fill=SOFT)
    c.save(path)


def cd_repository(path):
    W = 520
    rw, mx, mw = 330, 402, 108
    y = 34
    repos = []
    specs = [
        ("TransactionRepository", ["- file: Path", "- skippedRows: List<Integer>"],
         ["+ findAll(): List<Transaction>", "+ findById(id): Optional<Transaction>",
          "+ countByCategory(categoryId): int", "+ add(date, type, amount, …): Transaction",
          "+ delete(id): void", "+ load(): void", "+ save(): void"], "Transaction"),
        ("CategoryRepository", ["- file: Path"],
         ["+ findByType(type): List<Category>", "+ findById(id): Optional<Category>",
          "+ nameOf(categoryId): String", "+ add(name, type, seeded): Category",
          "+ remove(id): void", "+ load(): void", "+ save(): void"],
         "Category"),
        ("BudgetRepository", ["- file: Path"],
         ["+ findByMonth(month): List<Budget>", "+ find(categoryId, month): Optional<Budget>",
          "+ setLimit(categoryId, month, limit): Budget", "+ load(): void", "+ save(): void"],
         "Budget"),
        ("RecurringRepository", ["- file: Path", "- skippedRows: List<Integer>"],
         ["+ findAll(): List<RecurringRule>", "+ countByCategory(categoryId): int",
          "+ add(categoryId, amount, …): RecurringRule", "+ remove(id): void",
          "+ load(): void", "+ save(): void"], "RecurringRule"),
    ]
    pairs = []
    for name, attrs, ops, model in specs:
        r = Cls(name, 24, y, rw, attrs, ops)
        m = Cls(model, mx, y + 20, mw)
        pairs.append((r, m))
        y = r.bottom + 22
    csv = Cls("CsvStore", 24, y + 8, rw, [],
              ["_+ read(file): List<String[]>", "_+ write(file, header, rows): void"])
    H = csv.bottom + 28
    c = Canvas(W, H)
    c.text(6, 4, "CD3  repository 패키지", F_TITLE)
    for r, m in pairs:
        r.draw(c)
        m.draw(c)
        my = m.anchor("left")[1]
        rel(c, r, "right", m, "left", kind="comp", fa=(my - r.y) / r.h, mb="0..*")
    csv.draw(c)
    # 저장소 네 개 모두 CsvStore 를 쓴다. 왼쪽 통로 하나로 모은다.
    lane = 10
    for r, _ in pairs:
        ry = r.anchor("left", 0.85)[1]
        c.line([(r.x, ry), (lane, ry)], fill=LINE, width=1.1, dash=(5, 3))
    top = pairs[0][0].anchor("left", 0.85)[1]
    end = csv.anchor("left", 0.5)
    c.line([(lane, top), (lane, end[1]), end], fill=LINE, width=1.1, dash=(5, 3))
    c.arrow_head(lane, end[1], end[0], end[1], filled=False, size=8)
    c.text(csv.x + csv.w + 8, end[1] - 8, "«use»: 네 저장소 모두", F_SMALL, fill=SOFT)
    c.text(csv.x + csv.w + 8, end[1] + 8, "CSV 읽기·쓰기를 맡긴다", F_SMALL, fill=SOFT)
    c.save(path)


def cd_service(path):
    W = 520
    sw, rx, rw = 328, 400, 112
    rs = Cls("RecurringService", 10, 34, sw, [],
             ["+ rules(): List<RecurringRule>",
              "+ addRule(categoryId, amount, …): RecurringRule",
              "+ setActive(ruleId, active): void", "+ deleteRule(ruleId): void",
              "+ countByCategory(categoryId): int",
              "+ generateFor(month): List<Transaction>", "+ save(): void"])
    ls = Cls("LedgerService", 10, rs.bottom + 40, sw, [],
             ["+ register(date, type, amount, …): Transaction",
              "+ update(id, date, type, …): void", "+ delete(id): void",
              "+ find(filter): List<Transaction>", "+ addCategory(name, type): Category",
              "+ renameCategory(id, newName): void", "+ deleteCategory(id): void",
              "+ usageCount(categoryId): int", "+ save(): void"])
    ss = Cls("StatisticsService", 10, ls.bottom + 40, sw, [],
             ["+ summarize(month): MonthlySummary", "+ sum(month, type): long",
              "+ expenseOf(categoryId, month): long",
              "+ expenseByCategory(month): List<CategoryShare>"])
    bs = Cls("BudgetService", 10, ss.bottom + 40, sw, [],
             ["+ setLimit(categoryId, month, limit): void",
              "+ statuses(month): List<BudgetStatus>",
              "+ statusOf(categoryId, month): BudgetStatus",
              "_+ levelOf(used, limit): Level"])
    rr = Cls("Recurring\nRepository", rx, rs.y + 30, rw)
    tr = Cls("Transaction\nRepository", rx, ls.y + 30, rw)
    cr = Cls("Category\nRepository", rx, ss.y - 20, rw)
    br = Cls("Budget\nRepository", rx, bs.y + 40, rw)
    H = bs.bottom + 28
    c = Canvas(W, H)
    c.text(6, 4, "CD4  service 패키지", F_TITLE)
    for b in (rs, ls, ss, bs, rr, tr, cr, br):
        b.draw(c)
    rel(c, rs, "bottom", ls, "top", fa=0.3, fb=0.3, label="ledger")
    rel(c, bs, "top", ss, "bottom", fa=0.3, fb=0.3, label="statistics")

    def to_repo(svc, f, repo, lane, fb=0.5):
        p1 = svc.anchor("right", f)
        p2 = repo.anchor("left", fb)
        rel(c, svc, "right", repo, "left", fa=f, fb=fb,
            via=[(lane, p1[1]), (lane, p2[1])] if abs(p1[1] - p2[1]) > 0.5 else [])

    to_repo(rs, (rr.anchor("left")[1] - rs.y) / rs.h, rr, 360)
    to_repo(ls, (tr.anchor("left")[1] - ls.y) / ls.h, tr, 360)
    to_repo(ls, 0.85, cr, 350, fb=0.25)
    to_repo(ss, 0.25, tr, 372, fb=0.8)
    to_repo(ss, 0.6, cr, 382, fb=0.75)
    to_repo(bs, (br.anchor("left")[1] - bs.y) / bs.h, br, 360)
    to_repo(bs, 0.2, cr, 390, fb=0.95)
    c.text(10, H - 22, "저장소로 가는 연관의 다중도는 모두 1. 반환값 클래스는 CD1 참고.",
           F_SMALL, fill=SOFT)
    c.save(path)


def cd_ui(path):
    W = 520
    mf = Cls("MainFrame", 135, 34, 250,
             ["- ledger: LedgerService", "- recurring: RecurringService",
              "- budgetRepository: BudgetRepository"],
             ["- saveAndExit(): void"])
    tp = Cls("TransactionPanel", 10, mf.bottom + 46, 250,
             ["- ledger: LedgerService", "- recurring: RecurringService",
              "- model: TransactionTableModel"],
             ["+ refresh(): void", "+ setOnDataChanged(listener): void",
              "- openDialog(editing): void", "- openCategoryDialog(): void",
              "- openRecurringDialog(): void"])
    sp = Cls("StatisticsPanel", 276, mf.bottom + 46, 234,
             ["- statistics: StatisticsService", "- chart: BarChartPanel"],
             ["+ refresh(): void"])
    bp = Cls("BudgetPanel", 276, sp.bottom + 16, 234,
             ["- budgets: BudgetService", "- categories: CategoryRepository"],
             ["+ refresh(): void"])
    dy = max(tp.bottom, bp.bottom) + 46
    td = Cls("TransactionDialog", 10, dy, 160,
             ["- ledger: LedgerService", "- editing: Transaction"],
             ["+ isSaved(): boolean"])
    cd = Cls("CategoryDialog", 180, dy, 160,
             ["- ledger: LedgerService", "- recurring:", "    RecurringService"],
             ["+ isChanged(): boolean", "- addCategory(): void", "- renameSelected(): void",
              "- deleteSelected(): void"])
    rd = Cls("RecurringDialog", 350, dy, 160,
             ["- recurring:", "    RecurringService", "- categories:",
              "    CategoryRepository"],
             ["- addRule(): void", "- toggleSelected(): void", "- deleteSelected(): void"],)
    H = max(td.bottom, cd.bottom, rd.bottom) + 48
    c = Canvas(W, H)
    c.text(6, 4, "CD5  ui 패키지", F_TITLE)
    for b in (mf, tp, sp, bp, td, cd, rd):
        b.draw(c)
    rel(c, mf, "bottom", tp, "top", kind="comp", fa=0.2, fb=0.5)
    rel(c, mf, "bottom", sp, "top", kind="comp", fa=0.8, fb=0.5)
    rel(c, mf, "right", bp, "right", kind="comp", fa=0.5, fb=0.4,
        via=[(W - 4, mf.anchor("right")[1]), (W - 4, bp.anchor("right", 0.4)[1])])
    lane = max(tp.bottom, bp.bottom) + 18
    for dlg, fx in ((td, 0.2), (cd, 0.5), (rd, 0.8)):
        p1 = tp.anchor("bottom", fx)
        p2 = dlg.anchor("top")
        c.line([p1, (p1[0], lane), (p2[0], lane), p2], fill=LINE, width=1.1, dash=(5, 3))
        c.arrow_head(p2[0], lane, p2[0], p2[1], filled=False, size=9)
    c.text(tp.anchor("bottom", 0.8)[0] + 6, lane - 9, "«create»", F_SMALL, fill=SOFT)
    c.text(10, H - 40, "MainFrame 은 JFrame, …Panel 은 JPanel, …Dialog 는 JDialog 를 상속한다.",
           F_SMALL, fill=SOFT)
    c.text(10, H - 22, "서비스·저장소와의 연관은 속성으로 표기했다.", F_SMALL, fill=SOFT)
    c.save(path)


def main():
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "docs/cp3_figures")
    out.mkdir(parents=True, exist_ok=True)
    # 이전 판의 파일 이름이 남아 있으면 헷갈리므로 지운다.
    for old in ("sd6_lifecycle.png",):
        (out / old).unlink(missing_ok=True)
    usecase_diagram(out / "usecase_diagram.png")
    sd_register(out / "sd1_register.png")
    sd_search(out / "sd2_search.png")
    sd_category_delete(out / "sd3_category_delete.png")
    sd_statistics(out / "sd4_statistics.png")
    sd_budget(out / "sd5_budget.png")
    sd_startup(out / "sd6_startup.png")
    sd_shutdown(out / "sd7_shutdown.png")
    cd_overview(out / "cd1_overview.png")
    cd_model(out / "cd2_model.png")
    cd_repository(out / "cd3_repository.png")
    cd_service(out / "cd4_service.png")
    cd_ui(out / "cd5_ui.png")


if __name__ == "__main__":
    main()
