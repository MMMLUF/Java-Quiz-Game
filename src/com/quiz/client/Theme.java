package com.quiz.client;

import java.awt.Color;
import java.awt.Font;

class Theme {
    static final Color BG      = new Color(248, 250, 252);
    static final Color SURFACE = Color.WHITE;
    static final Color PRIMARY = new Color(37, 99, 235);
    static final Color TEXT    = new Color(30, 41, 59);
    static final Color MUTED   = new Color(100, 116, 139);
    static final Color SUCCESS = new Color(22, 163, 74);
    static final Color DANGER  = new Color(220, 38, 38);
    static final Color WARNING = new Color(217, 119, 6);
    static final Color BORDER  = new Color(226, 232, 240);

    static final Color CAT_JAVA     = new Color(245, 158,  11);
    static final Color CAT_SKKU     = new Color( 37,  99, 235);
    static final Color CAT_GENERAL  = new Color( 16, 185, 129);
    static final Color CAT_NONSENSE = new Color(139,  92, 246);

    static Color categoryColor(String category) {
        switch (category) {
            case "Java 코드": return CAT_JAVA;
            case "SKKU 퀴즈": return CAT_SKKU;
            case "일반상식":  return CAT_GENERAL;
            default:          return CAT_NONSENSE;
        }
    }

    static final Font TITLE   = new Font("맑은 고딕", Font.BOLD, 22);
    static final Font HEADING = new Font("맑은 고딕", Font.BOLD, 16);
    static final Font BODY    = new Font("맑은 고딕", Font.PLAIN, 15);
    static final Font BUTTON  = new Font("맑은 고딕", Font.BOLD, 14);
    static final Font SMALL   = new Font("맑은 고딕", Font.PLAIN, 13);
}
