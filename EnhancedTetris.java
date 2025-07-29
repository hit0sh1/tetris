import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.sound.sampled.*;

public class EnhancedTetris extends JPanel implements ActionListener {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int CELL_SIZE = 30;
    private static final int INITIAL_DELAY = 500;
    private static final int PREVIEW_SIZE = 4;
    
    private Timer timer;
    private Timer animationTimer;
    private boolean isFallingFinished = false;
    private boolean isStarted = false;
    private boolean isPaused = false;
    private int score = 0;
    private int level = 1;
    private int linesRemoved = 0;
    private int curX = 0;
    private int curY = 0;
    private int ghostY = 0;
    private int highScore = 0;
    
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private JLabel highScoreLabel;
    private JLabel linesLabel;
    
    private Tetromino curPiece;
    private Tetromino nextPiece;
    private Tetromino heldPiece;
    private boolean canHold = true;
    private Tetrominoes[] board;
    private ArrayList<Integer> linesToRemove = new ArrayList<>();
    private int animationStep = 0;
    
    public EnhancedTetris() {
        setFocusable(true);
        setBackground(new Color(20, 20, 20));
        setPreferredSize(new Dimension(500, 650));
        
        curPiece = new Tetromino();
        nextPiece = new Tetromino();
        heldPiece = new Tetromino();
        timer = new Timer(INITIAL_DELAY, this);
        animationTimer = new Timer(50, new AnimationListener());
        
        board = new Tetrominoes[BOARD_WIDTH * BOARD_HEIGHT];
        
        addKeyListener(new TAdapter());
        clearBoard();
        loadHighScore();
        
        nextPiece.setRandomShape();
    }
    
    public JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(new Color(30, 30, 30));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        Font labelFont = new Font("Arial", Font.BOLD, 16);
        Font valueFont = new Font("Arial", Font.PLAIN, 24);
        
        JLabel scoreTitleLabel = new JLabel("SCORE");
        scoreTitleLabel.setForeground(Color.WHITE);
        scoreTitleLabel.setFont(labelFont);
        scoreTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        scoreLabel = new JLabel("0");
        scoreLabel.setForeground(Color.CYAN);
        scoreLabel.setFont(valueFont);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel levelTitleLabel = new JLabel("LEVEL");
        levelTitleLabel.setForeground(Color.WHITE);
        levelTitleLabel.setFont(labelFont);
        levelTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        levelLabel = new JLabel("1");
        levelLabel.setForeground(Color.GREEN);
        levelLabel.setFont(valueFont);
        levelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel linesTitleLabel = new JLabel("LINES");
        linesTitleLabel.setForeground(Color.WHITE);
        linesTitleLabel.setFont(labelFont);
        linesTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        linesLabel = new JLabel("0");
        linesLabel.setForeground(Color.YELLOW);
        linesLabel.setFont(valueFont);
        linesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel highScoreTitleLabel = new JLabel("HIGH SCORE");
        highScoreTitleLabel.setForeground(Color.WHITE);
        highScoreTitleLabel.setFont(labelFont);
        highScoreTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        highScoreLabel = new JLabel(String.valueOf(highScore));
        highScoreLabel.setForeground(Color.MAGENTA);
        highScoreLabel.setFont(valueFont);
        highScoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JPanel nextPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (nextPiece != null && nextPiece.getShape() != Tetrominoes.NoShape) {
                    drawPreviewPiece(g, nextPiece, 50, 30);
                }
            }
        };
        nextPanel.setBackground(new Color(40, 40, 40));
        nextPanel.setPreferredSize(new Dimension(150, 100));
        nextPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE), "NEXT", 
            0, 0, labelFont, Color.WHITE));
        
        JPanel holdPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (heldPiece != null && heldPiece.getShape() != Tetrominoes.NoShape) {
                    drawPreviewPiece(g, heldPiece, 50, 30);
                }
            }
        };
        holdPanel.setBackground(new Color(40, 40, 40));
        holdPanel.setPreferredSize(new Dimension(150, 100));
        holdPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.WHITE), "HOLD", 
            0, 0, labelFont, Color.WHITE));
        
        sidePanel.add(scoreTitleLabel);
        sidePanel.add(scoreLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(levelTitleLabel);
        sidePanel.add(levelLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(linesTitleLabel);
        sidePanel.add(linesLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(highScoreTitleLabel);
        sidePanel.add(highScoreLabel);
        sidePanel.add(Box.createVerticalStrut(30));
        sidePanel.add(nextPanel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(holdPanel);
        
        return sidePanel;
    }
    
    private void drawPreviewPiece(Graphics g, Tetromino piece, int offsetX, int offsetY) {
        for (int i = 0; i < 4; i++) {
            int x = offsetX + piece.x(i) * 20;
            int y = offsetY + piece.y(i) * 20;
            drawSquare(g, x, y, piece.getShape(), 20);
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isFallingFinished) {
            isFallingFinished = false;
            newPiece();
        } else {
            oneLineDown();
        }
    }
    
    class AnimationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            animationStep++;
            if (animationStep > 5) {
                animationTimer.stop();
                for (int line : linesToRemove) {
                    removeLine(line);
                }
                linesToRemove.clear();
                animationStep = 0;
            }
            repaint();
        }
    }
    
    private Tetrominoes shapeAt(int x, int y) {
        return board[(y * BOARD_WIDTH) + x];
    }
    
    public void start() {
        if (isPaused) {
            return;
        }
        
        isStarted = true;
        isFallingFinished = false;
        score = 0;
        level = 1;
        linesRemoved = 0;
        clearBoard();
        newPiece();
        timer.setDelay(INITIAL_DELAY);
        timer.start();
        updateLabels();
    }
    
    private void pause() {
        if (!isStarted) {
            return;
        }
        
        isPaused = !isPaused;
        if (isPaused) {
            timer.stop();
        } else {
            timer.start();
        }
        repaint();
    }
    
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw board background
        g.setColor(new Color(10, 10, 10));
        g.fillRect(50, 50, BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE);
        
        // Draw grid
        g.setColor(new Color(30, 30, 30));
        for (int i = 0; i <= BOARD_WIDTH; i++) {
            g.drawLine(50 + i * CELL_SIZE, 50, 50 + i * CELL_SIZE, 50 + BOARD_HEIGHT * CELL_SIZE);
        }
        for (int i = 0; i <= BOARD_HEIGHT; i++) {
            g.drawLine(50, 50 + i * CELL_SIZE, 50 + BOARD_WIDTH * CELL_SIZE, 50 + i * CELL_SIZE);
        }
        
        // Draw board pieces
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                Tetrominoes shape = shapeAt(j, BOARD_HEIGHT - i - 1);
                if (shape != Tetrominoes.NoShape) {
                    boolean isAnimating = linesToRemove.contains(BOARD_HEIGHT - i - 1);
                    if (!isAnimating || animationStep % 2 == 0) {
                        drawSquare(g, 50 + j * CELL_SIZE, 50 + i * CELL_SIZE, shape, CELL_SIZE);
                    }
                }
            }
        }
        
        // Draw ghost piece
        if (curPiece.getShape() != Tetrominoes.NoShape) {
            updateGhostPosition();
            g.setColor(new Color(100, 100, 100, 80));
            for (int i = 0; i < 4; i++) {
                int x = curX + curPiece.x(i);
                int y = ghostY - curPiece.y(i);
                g.fillRect(50 + x * CELL_SIZE + 1, 
                          50 + (BOARD_HEIGHT - y - 1) * CELL_SIZE + 1,
                          CELL_SIZE - 2, CELL_SIZE - 2);
            }
        }
        
        // Draw current piece
        if (curPiece.getShape() != Tetrominoes.NoShape) {
            for (int i = 0; i < 4; i++) {
                int x = curX + curPiece.x(i);
                int y = curY - curPiece.y(i);
                drawSquare(g, 50 + x * CELL_SIZE, 
                          50 + (BOARD_HEIGHT - y - 1) * CELL_SIZE, 
                          curPiece.getShape(), CELL_SIZE);
            }
        }
        
        // Draw pause overlay
        if (isPaused) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("PAUSED", getWidth() / 2 - 100, getHeight() / 2);
        }
        
        // Draw game over overlay
        if (!isStarted && score > 0) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 48));
            g.drawString("GAME OVER", getWidth() / 2 - 150, getHeight() / 2);
        }
    }
    
    private void updateGhostPosition() {
        ghostY = curY;
        while (ghostY > 0) {
            if (!tryMove(curPiece, curX, ghostY - 1, false)) {
                break;
            }
            ghostY--;
        }
    }
    
    private void hold() {
        if (!canHold) {
            return;
        }
        
        if (heldPiece.getShape() == Tetrominoes.NoShape) {
            heldPiece = curPiece;
            newPiece();
        } else {
            Tetromino temp = curPiece;
            curPiece = heldPiece;
            heldPiece = temp;
            curX = BOARD_WIDTH / 2 + 1;
            curY = BOARD_HEIGHT - 1 + curPiece.minY();
        }
        canHold = false;
        repaint();
    }
    
    private void dropDown() {
        int newY = curY;
        while (newY > 0) {
            if (!tryMove(curPiece, curX, newY - 1, true)) {
                break;
            }
            newY--;
        }
        pieceDropped();
    }
    
    private void oneLineDown() {
        if (!tryMove(curPiece, curX, curY - 1, true)) {
            pieceDropped();
        }
    }
    
    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT * BOARD_WIDTH; i++) {
            board[i] = Tetrominoes.NoShape;
        }
    }
    
    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY - curPiece.y(i);
            board[(y * BOARD_WIDTH) + x] = curPiece.getShape();
        }
        
        removeFullLines();
        
        if (!isFallingFinished) {
            newPiece();
        }
    }
    
    private void newPiece() {
        curPiece = nextPiece;
        nextPiece = new Tetromino();
        nextPiece.setRandomShape();
        
        curX = BOARD_WIDTH / 2 + 1;
        curY = BOARD_HEIGHT - 1 + curPiece.minY();
        
        canHold = true;
        
        if (!tryMove(curPiece, curX, curY, true)) {
            curPiece.setShape(Tetrominoes.NoShape);
            timer.stop();
            isStarted = false;
            saveHighScore();
            repaint();
        }
    }
    
    private boolean tryMove(Tetromino newPiece, int newX, int newY, boolean updateCurrent) {
        for (int i = 0; i < 4; i++) {
            int x = newX + newPiece.x(i);
            int y = newY - newPiece.y(i);
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) {
                return false;
            }
            if (shapeAt(x, y) != Tetrominoes.NoShape) {
                return false;
            }
        }
        
        if (updateCurrent) {
            curPiece = newPiece;
            curX = newX;
            curY = newY;
            repaint();
        }
        return true;
    }
    
    private void removeFullLines() {
        int numFullLines = 0;
        linesToRemove.clear();
        
        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean lineIsFull = true;
            
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (shapeAt(j, i) == Tetrominoes.NoShape) {
                    lineIsFull = false;
                    break;
                }
            }
            
            if (lineIsFull) {
                numFullLines++;
                linesToRemove.add(i);
            }
        }
        
        if (numFullLines > 0) {
            animationTimer.start();
            
            linesRemoved += numFullLines;
            score += numFullLines * 100 * level;
            
            if (numFullLines == 4) {
                score += 400 * level; // Tetris bonus
            }
            
            // Level up every 10 lines
            int newLevel = linesRemoved / 10 + 1;
            if (newLevel != level) {
                level = newLevel;
                int newDelay = Math.max(100, INITIAL_DELAY - (level - 1) * 50);
                timer.setDelay(newDelay);
            }
            
            if (score > highScore) {
                highScore = score;
            }
            
            updateLabels();
            isFallingFinished = true;
            curPiece.setShape(Tetrominoes.NoShape);
        }
    }
    
    private void removeLine(int line) {
        for (int k = line; k < BOARD_HEIGHT - 1; k++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                board[(k * BOARD_WIDTH) + j] = shapeAt(j, k + 1);
            }
        }
        for (int j = 0; j < BOARD_WIDTH; j++) {
            board[((BOARD_HEIGHT - 1) * BOARD_WIDTH) + j] = Tetrominoes.NoShape;
        }
    }
    
    private void updateLabels() {
        scoreLabel.setText(String.valueOf(score));
        levelLabel.setText(String.valueOf(level));
        linesLabel.setText(String.valueOf(linesRemoved));
        highScoreLabel.setText(String.valueOf(highScore));
    }
    
    private void loadHighScore() {
        try {
            File file = new File("tetris_highscore.txt");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                highScore = Integer.parseInt(reader.readLine());
                reader.close();
            }
        } catch (IOException | NumberFormatException e) {
            highScore = 0;
        }
    }
    
    private void saveHighScore() {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("tetris_highscore.txt"));
            writer.write(String.valueOf(highScore));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void drawSquare(Graphics g, int x, int y, Tetrominoes shape, int size) {
        Color colors[] = { 
            new Color(0, 0, 0), 
            new Color(204, 102, 102),
            new Color(102, 204, 102), 
            new Color(102, 102, 204),
            new Color(204, 204, 102), 
            new Color(204, 102, 204),
            new Color(102, 204, 204), 
            new Color(218, 170, 0)
        };
        
        Color color = colors[shape.ordinal()];
        
        g.setColor(color);
        g.fillRect(x + 1, y + 1, size - 2, size - 2);
        
        g.setColor(color.brighter());
        g.drawLine(x, y + size - 1, x, y);
        g.drawLine(x, y, x + size - 1, y);
        
        g.setColor(color.darker());
        g.drawLine(x + 1, y + size - 1, x + size - 1, y + size - 1);
        g.drawLine(x + size - 1, y + size - 1, x + size - 1, y + 1);
    }
    
    class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!isStarted || curPiece.getShape() == Tetrominoes.NoShape) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    start();
                }
                return;
            }
            
            int keycode = e.getKeyCode();
            
            if (keycode == 'P' || keycode == 'p' || keycode == KeyEvent.VK_ESCAPE) {
                pause();
                return;
            }
            
            if (isPaused) {
                return;
            }
            
            switch (keycode) {
                case KeyEvent.VK_LEFT:
                case 'A':
                case 'a':
                    tryMove(curPiece, curX - 1, curY, true);
                    break;
                case KeyEvent.VK_RIGHT:
                case 'D':
                case 'd':
                    tryMove(curPiece, curX + 1, curY, true);
                    break;
                case KeyEvent.VK_DOWN:
                case 'S':
                case 's':
                    oneLineDown();
                    break;
                case KeyEvent.VK_UP:
                case 'W':
                case 'w':
                    tryMove(curPiece.rotateRight(), curX, curY, true);
                    break;
                case 'Z':
                case 'z':
                    tryMove(curPiece.rotateLeft(), curX, curY, true);
                    break;
                case KeyEvent.VK_SPACE:
                    dropDown();
                    break;
                case KeyEvent.VK_SHIFT:
                case 'C':
                case 'c':
                    hold();
                    break;
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Enhanced Tetris");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            
            EnhancedTetris game = new EnhancedTetris();
            frame.add(game, BorderLayout.CENTER);
            frame.add(game.createSidePanel(), BorderLayout.EAST);
            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.setVisible(true);
            
            game.start();
        });
    }
}

enum Tetrominoes {
    NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape
}

class Tetromino {
    private Tetrominoes pieceShape;
    private int coords[][];
    private int[][][] coordsTable;
    
    public Tetromino() {
        coords = new int[4][2];
        setShape(Tetrominoes.NoShape);
    }
    
    public void setShape(Tetrominoes shape) {
        coordsTable = new int[][][] {
            { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } },
            { { 0, -1 }, { 0, 0 }, { -1, 0 }, { -1, 1 } },
            { { 0, -1 }, { 0, 0 }, { 1, 0 }, { 1, 1 } },
            { { 0, -1 }, { 0, 0 }, { 0, 1 }, { 0, 2 } },
            { { -1, 0 }, { 0, 0 }, { 1, 0 }, { 0, 1 } },
            { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } },
            { { -1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } },
            { { 1, -1 }, { 0, -1 }, { 0, 0 }, { 0, 1 } }
        };
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; j++) {
                coords[i][j] = coordsTable[shape.ordinal()][i][j];
            }
        }
        pieceShape = shape;
    }
    
    private void setX(int index, int x) {
        coords[index][0] = x;
    }
    
    private void setY(int index, int y) {
        coords[index][1] = y;
    }
    
    public int x(int index) {
        return coords[index][0];
    }
    
    public int y(int index) {
        return coords[index][1];
    }
    
    public Tetrominoes getShape() {
        return pieceShape;
    }
    
    public void setRandomShape() {
        int x = (int) (Math.random() * 7) + 1;
        Tetrominoes[] values = Tetrominoes.values();
        setShape(values[x]);
    }
    
    public int minX() {
        int m = coords[0][0];
        for (int i = 0; i < 4; i++) {
            m = Math.min(m, coords[i][0]);
        }
        return m;
    }
    
    public int minY() {
        int m = coords[0][1];
        for (int i = 0; i < 4; i++) {
            m = Math.min(m, coords[i][1]);
        }
        return m;
    }
    
    public Tetromino rotateLeft() {
        if (pieceShape == Tetrominoes.SquareShape) {
            return this;
        }
        
        Tetromino result = new Tetromino();
        result.pieceShape = pieceShape;
        
        for (int i = 0; i < 4; i++) {
            result.setX(i, y(i));
            result.setY(i, -x(i));
        }
        return result;
    }
    
    public Tetromino rotateRight() {
        if (pieceShape == Tetrominoes.SquareShape) {
            return this;
        }
        
        Tetromino result = new Tetromino();
        result.pieceShape = pieceShape;
        
        for (int i = 0; i < 4; i++) {
            result.setX(i, -y(i));
            result.setY(i, x(i));
        }
        return result;
    }
}