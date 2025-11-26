package game.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class MainScreen extends JFrame {

    // ===== 커스텀 버튼 클래스 =====
    static class GameButton extends JButton implements MouseListener {

        private boolean hover = false;

        public GameButton(String text) {
            super(text);

            setFocusPainted(false);
            setContentAreaFilled(false);  // 우리가 직접 배경 그림
            setBorderPainted(false);
            setOpaque(false);

            setForeground(new Color(30, 30, 30));
            setFont(new Font("맑은 고딕", Font.BOLD, 26));

            addMouseListener(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int arc = 24;

            // 배경 그라데이션
            Color top, bottom;
            if (hover) {
                top = new Color(255, 255, 255, 230);
                bottom = new Color(230, 230, 230, 230);
            } else {
                top = new Color(245, 245, 245, 220);
                bottom = new Color(220, 220, 220, 220);
            }
            GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
            g2.setPaint(gp);
            Shape rect = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);
            g2.fill(rect);

            // 외곽선
            g2.setColor(new Color(180, 180, 180, 220));
            g2.draw(rect);

            // 텍스트
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();
            int tx = (w - tw) / 2;
            int ty = (h + th) / 2 - 3;

            g2.setColor(getForeground());
            g2.drawString(text, tx, ty);

            g2.dispose();
        }

        // MouseListener
        @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
        @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
    }

    // ===== 배경 패널 =====
    private class BackgroundPanel extends JPanel {
        private Image bgImage;

        public BackgroundPanel() {
            bgImage = new ImageIcon(
                    getClass().getResource("/images/main_background.png")
            ).getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // ====== 애니메이션 타이틀 패널 ======
    private static class AnimatedTitlePanel extends JPanel {

        private final String text = "SURVIVAL";
        private double phase = 0.0;              // 애니메이션 진행도
        private final float baseSize = 60f;      // 기본 폰트 크기
        private final Timer timer;               // Swing Timer

        // 🔥 파티클(불꽃) 데이터
        private static class FlameParticle {
            int x, y;
            float alpha;
            float size;
            float vy;
            Color color;
        }

        private java.util.List<FlameParticle> particles = new java.util.ArrayList<>();
        private final java.util.Random rand = new java.util.Random();

        public AnimatedTitlePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(900, 120));

            // 타이틀 애니메이션 + 파티클 생성
            timer = new Timer(33, e -> {
                phase += 0.08;  
                updateParticles();
                generateParticles();
                repaint();
            });
            timer.start();
        }

        // 🔥 불꽃 파티클 생성
        private void generateParticles() {
            if (particles.size() > 80) return; // 과부하 방지

            FlameParticle p = new FlameParticle();
            p.x = rand.nextInt(getWidth());
            p.y = getHeight() / 2 + 20;

            p.size = 5 + rand.nextFloat() * 6;
            p.alpha = 1.0f;
            p.vy = 1.0f + rand.nextFloat() * 1.5f;

            // 랜덤한 불꽃 색상
            p.color = new Color(
                    255,
                    100 + rand.nextInt(100),
                    0,
                    180 + rand.nextInt(75)
            );

            particles.add(p);
        }

        // 🔥 파티클 움직임 업데이트
        private void updateParticles() {
            for (int i = particles.size() - 1; i >= 0; i--) {
                FlameParticle p = particles.get(i);
                p.y -= p.vy;
                p.alpha -= 0.04f;

                if (p.alpha <= 0) {
                    particles.remove(i);
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 크기 펄스 + 위아래 흔들림
            double scale = 1.0 + 0.06 * Math.sin(phase);
            int yOffset = (int)(5 * Math.sin(phase * 2));

            float fontSize = (float)(baseSize * scale);
            Font baseFont = new Font("Serif", Font.BOLD, 60);
            Font font = baseFont.deriveFont(fontSize);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getAscent();

            int x = (w - textWidth) / 2;
            int y = h / 2 + textHeight / 2 + yOffset;

            // 1. 텍스트 뒤의 불꽃 글로우
            for (int i = 6; i > 0; i--) {
                float alpha = 0.07f * i;
                g2.setColor(new Color(255, 140, 0, (int)(alpha * 255)));
                g2.drawString(text, x, y - i * 2);
            }

            // 2. 텍스트 그림자
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(text, x + 4, y + 4);

            // 3. 본 텍스트 (불꽃 그라데이션)
            GradientPaint gp = new GradientPaint(
                    x, y - textHeight,
                    new Color(255, 120, 0),
                    x, y,
                    new Color(255, 30, 0)
            );
            g2.setPaint(gp);
            g2.drawString(text, x, y);

            // 4. 파티클 불꽃 렌더링
            for (FlameParticle p : particles) {
                g2.setColor(new Color(
                        p.color.getRed(),
                        p.color.getGreen(),
                        p.color.getBlue(),
                        (int)(p.alpha * 255)
                ));

                int size = (int)p.size;
                g2.fillOval(p.x, p.y, size, size);
            }

            g2.dispose();
        }
    }

    // ===== 메인 스크린 =====
    public MainScreen() {
        setTitle("Vam sur - Main Screen");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setResizable(true);

        buildMainMenu();

        setVisible(true);
    }

    /** 메인 메뉴 UI 생성 */
    private void buildMainMenu() {
        BackgroundPanel bgPanel = new BackgroundPanel();
        bgPanel.setLayout(new BorderLayout());
        setContentPane(bgPanel);

        // 애니메이션 타이틀을 상단에 배치
        AnimatedTitlePanel titlePanel = new AnimatedTitlePanel();
        bgPanel.add(titlePanel, BorderLayout.NORTH);

        // ===== 하단 버튼 패널 =====
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 40, 30));

        Dimension btnSize = new Dimension(200, 70);

        GameButton startBtn    = new GameButton("시작");
        GameButton continueBtn = new GameButton("이어하기");
        GameButton exitBtn     = new GameButton("종료");

        GameButton[] btnArr = {startBtn, continueBtn, exitBtn};
        for (GameButton btn : btnArr) {
            btn.setPreferredSize(btnSize);
        }

        // 시작 → 게임 화면으로 전환
        startBtn.addActionListener(e -> startGame());

        // 이어하기 (추후 구현)
        continueBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(
                        MainScreen.this,
                        "이어하기 기능은 추후 추가 예정입니다."
                )
        );

        // 종료
        exitBtn.addActionListener(e -> System.exit(0));

        buttonPanel.add(startBtn);
        buttonPanel.add(continueBtn);
        buttonPanel.add(exitBtn);

        bgPanel.add(buttonPanel, BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    /** 게임 화면으로 전환 */
    public void startGame() {
        GamePanel gamePanel = new GamePanel(this);
        setContentPane(gamePanel);

        setTitle("Vamsur - game");
        revalidate();
        repaint();

        gamePanel.requestFocusInWindow();
        gamePanel.startGameLoop();
    }

    /** 게임 도중 메인 메뉴로 복귀 */
    public void returnToMainMenu() {
        setTitle("Vam sur - Main Screen");
        buildMainMenu();
    }
}
