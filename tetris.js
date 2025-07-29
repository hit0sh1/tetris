class TetrisGame {
    constructor() {
        this.canvas = document.getElementById('gameCanvas');
        this.ctx = this.canvas.getContext('2d');
        this.nextCanvas = document.getElementById('nextCanvas');
        this.nextCtx = this.nextCanvas.getContext('2d');
        this.holdCanvas = document.getElementById('holdCanvas');
        this.holdCtx = this.holdCanvas.getContext('2d');
        
        this.BOARD_WIDTH = 10;
        this.BOARD_HEIGHT = 20;
        this.CELL_SIZE = 30;
        this.INITIAL_DELAY = 500;
        
        this.canvas.width = this.BOARD_WIDTH * this.CELL_SIZE;
        this.canvas.height = this.BOARD_HEIGHT * this.CELL_SIZE;
        
        this.board = [];
        this.currentPiece = null;
        this.nextPiece = null;
        this.heldPiece = null;
        this.canHold = true;
        this.currentX = 0;
        this.currentY = 0;
        this.ghostY = 0;
        
        this.score = 0;
        this.level = 1;
        this.lines = 0;
        this.highScore = parseInt(localStorage.getItem('tetrisHighScore') || '0');
        
        this.isPaused = false;
        this.isGameOver = false;
        this.dropTimer = null;
        this.animationTimer = null;
        this.linesToClear = [];
        this.animationStep = 0;
        
        this.tetrominoes = {
            I: {
                shape: [[0,0,0,0], [1,1,1,1], [0,0,0,0], [0,0,0,0]],
                color: '#00ffff'
            },
            O: {
                shape: [[1,1], [1,1]],
                color: '#ffff00'
            },
            T: {
                shape: [[0,1,0], [1,1,1], [0,0,0]],
                color: '#ff00ff'
            },
            S: {
                shape: [[0,1,1], [1,1,0], [0,0,0]],
                color: '#00ff00'
            },
            Z: {
                shape: [[1,1,0], [0,1,1], [0,0,0]],
                color: '#ff0000'
            },
            J: {
                shape: [[1,0,0], [1,1,1], [0,0,0]],
                color: '#0000ff'
            },
            L: {
                shape: [[0,0,1], [1,1,1], [0,0,0]],
                color: '#ff8800'
            }
        };
        
        this.init();
    }
    
    init() {
        this.clearBoard();
        this.updateUI();
        this.setupEventListeners();
        this.spawnPiece();
        this.startGameLoop();
    }
    
    clearBoard() {
        this.board = Array(this.BOARD_HEIGHT).fill(null).map(() => 
            Array(this.BOARD_WIDTH).fill(0)
        );
    }
    
    setupEventListeners() {
        document.addEventListener('keydown', (e) => this.handleKeyPress(e));
        
        // モバイルコントロール
        document.getElementById('btnLeft').addEventListener('click', () => this.move(-1));
        document.getElementById('btnRight').addEventListener('click', () => this.move(1));
        document.getElementById('btnDown').addEventListener('click', () => this.softDrop());
        document.getElementById('btnRotate').addEventListener('click', () => this.rotate());
        document.getElementById('btnDrop').addEventListener('click', () => this.hardDrop());
        
        // タッチイベント
        let touchStartX = 0;
        let touchStartY = 0;
        
        this.canvas.addEventListener('touchstart', (e) => {
            touchStartX = e.touches[0].clientX;
            touchStartY = e.touches[0].clientY;
        });
        
        this.canvas.addEventListener('touchend', (e) => {
            const touchEndX = e.changedTouches[0].clientX;
            const touchEndY = e.changedTouches[0].clientY;
            const deltaX = touchEndX - touchStartX;
            const deltaY = touchEndY - touchStartY;
            
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                if (deltaX > 50) this.move(1);
                else if (deltaX < -50) this.move(-1);
            } else {
                if (deltaY > 50) this.softDrop();
                else if (deltaY < -50) this.rotate();
            }
        });
    }
    
    handleKeyPress(e) {
        if (this.isGameOver && e.key === 'Enter') {
            this.restart();
            return;
        }
        
        if (this.isGameOver) return;
        
        switch(e.key) {
            case 'ArrowLeft':
            case 'a':
            case 'A':
                e.preventDefault();
                this.move(-1);
                break;
            case 'ArrowRight':
            case 'd':
            case 'D':
                e.preventDefault();
                this.move(1);
                break;
            case 'ArrowDown':
            case 's':
            case 'S':
                e.preventDefault();
                this.softDrop();
                break;
            case 'ArrowUp':
            case 'w':
            case 'W':
                e.preventDefault();
                this.rotate();
                break;
            case 'z':
            case 'Z':
                e.preventDefault();
                this.rotate(true);
                break;
            case ' ':
                e.preventDefault();
                this.hardDrop();
                break;
            case 'c':
            case 'C':
            case 'Shift':
                e.preventDefault();
                this.hold();
                break;
            case 'p':
            case 'P':
            case 'Escape':
                e.preventDefault();
                this.togglePause();
                break;
        }
    }
    
    spawnPiece() {
        if (!this.nextPiece) {
            this.nextPiece = this.getRandomPiece();
        }
        
        this.currentPiece = this.nextPiece;
        this.nextPiece = this.getRandomPiece();
        
        this.currentX = Math.floor((this.BOARD_WIDTH - this.currentPiece.shape[0].length) / 2);
        this.currentY = 0;
        
        this.canHold = true;
        
        if (!this.isValidPosition(this.currentPiece.shape, this.currentX, this.currentY)) {
            this.gameOver();
        }
        
        this.drawNext();
    }
    
    getRandomPiece() {
        const pieces = Object.keys(this.tetrominoes);
        const randomPiece = pieces[Math.floor(Math.random() * pieces.length)];
        return {
            shape: this.tetrominoes[randomPiece].shape.map(row => [...row]),
            color: this.tetrominoes[randomPiece].color,
            type: randomPiece
        };
    }
    
    isValidPosition(shape, x, y) {
        for (let row = 0; row < shape.length; row++) {
            for (let col = 0; col < shape[row].length; col++) {
                if (shape[row][col]) {
                    const newX = x + col;
                    const newY = y + row;
                    
                    if (newX < 0 || newX >= this.BOARD_WIDTH || 
                        newY >= this.BOARD_HEIGHT || 
                        (newY >= 0 && this.board[newY][newX])) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    move(direction) {
        if (this.isPaused || this.isGameOver) return;
        
        const newX = this.currentX + direction;
        if (this.isValidPosition(this.currentPiece.shape, newX, this.currentY)) {
            this.currentX = newX;
        }
    }
    
    rotate(counterClockwise = false) {
        if (this.isPaused || this.isGameOver) return;
        
        const rotated = this.getRotatedShape(this.currentPiece.shape, counterClockwise);
        
        // Wall kick
        let kickX = 0;
        if (this.isValidPosition(rotated, this.currentX, this.currentY)) {
            this.currentPiece.shape = rotated;
        } else if (this.isValidPosition(rotated, this.currentX - 1, this.currentY)) {
            this.currentPiece.shape = rotated;
            this.currentX -= 1;
        } else if (this.isValidPosition(rotated, this.currentX + 1, this.currentY)) {
            this.currentPiece.shape = rotated;
            this.currentX += 1;
        } else if (this.isValidPosition(rotated, this.currentX - 2, this.currentY)) {
            this.currentPiece.shape = rotated;
            this.currentX -= 2;
        } else if (this.isValidPosition(rotated, this.currentX + 2, this.currentY)) {
            this.currentPiece.shape = rotated;
            this.currentX += 2;
        }
    }
    
    getRotatedShape(shape, counterClockwise = false) {
        const size = shape.length;
        const rotated = Array(size).fill(null).map(() => Array(size).fill(0));
        
        for (let row = 0; row < size; row++) {
            for (let col = 0; col < size; col++) {
                if (counterClockwise) {
                    rotated[size - 1 - col][row] = shape[row][col];
                } else {
                    rotated[col][size - 1 - row] = shape[row][col];
                }
            }
        }
        
        return rotated;
    }
    
    softDrop() {
        if (this.isPaused || this.isGameOver) return;
        
        if (this.isValidPosition(this.currentPiece.shape, this.currentX, this.currentY + 1)) {
            this.currentY++;
            this.score += 1;
        } else {
            this.lockPiece();
        }
    }
    
    hardDrop() {
        if (this.isPaused || this.isGameOver) return;
        
        let dropDistance = 0;
        while (this.isValidPosition(this.currentPiece.shape, this.currentX, this.currentY + 1)) {
            this.currentY++;
            dropDistance++;
        }
        this.score += dropDistance * 2;
        this.lockPiece();
    }
    
    hold() {
        if (!this.canHold || this.isPaused || this.isGameOver) return;
        
        this.canHold = false;
        
        if (!this.heldPiece) {
            this.heldPiece = {
                shape: this.currentPiece.shape.map(row => [...row]),
                color: this.currentPiece.color,
                type: this.currentPiece.type
            };
            this.spawnPiece();
        } else {
            const temp = this.currentPiece;
            this.currentPiece = this.heldPiece;
            this.heldPiece = {
                shape: temp.shape.map(row => [...row]),
                color: temp.color,
                type: temp.type
            };
            this.currentX = Math.floor((this.BOARD_WIDTH - this.currentPiece.shape[0].length) / 2);
            this.currentY = 0;
        }
        
        this.drawHold();
    }
    
    lockPiece() {
        for (let row = 0; row < this.currentPiece.shape.length; row++) {
            for (let col = 0; col < this.currentPiece.shape[row].length; col++) {
                if (this.currentPiece.shape[row][col]) {
                    const boardY = this.currentY + row;
                    const boardX = this.currentX + col;
                    if (boardY >= 0) {
                        this.board[boardY][boardX] = this.currentPiece.color;
                    }
                }
            }
        }
        
        this.clearLines();
        this.spawnPiece();
    }
    
    clearLines() {
        this.linesToClear = [];
        
        for (let row = this.BOARD_HEIGHT - 1; row >= 0; row--) {
            if (this.board[row].every(cell => cell !== 0)) {
                this.linesToClear.push(row);
            }
        }
        
        if (this.linesToClear.length > 0) {
            this.animateLineClear();
            this.updateScore(this.linesToClear.length);
        }
    }
    
    animateLineClear() {
        this.animationStep = 0;
        this.animationTimer = setInterval(() => {
            this.animationStep++;
            if (this.animationStep > 5) {
                clearInterval(this.animationTimer);
                this.removeLines();
                this.animationStep = 0;
            }
        }, 50);
    }
    
    removeLines() {
        for (let line of this.linesToClear) {
            this.board.splice(line, 1);
            this.board.unshift(Array(this.BOARD_WIDTH).fill(0));
        }
        this.linesToClear = [];
    }
    
    updateScore(linesCleared) {
        const points = [0, 100, 300, 500, 800];
        this.score += points[linesCleared] * this.level;
        this.lines += linesCleared;
        
        // Level up every 10 lines
        const newLevel = Math.floor(this.lines / 10) + 1;
        if (newLevel !== this.level) {
            this.level = newLevel;
            clearInterval(this.dropTimer);
            const speed = Math.max(100, this.INITIAL_DELAY - (this.level - 1) * 50);
            this.dropTimer = setInterval(() => this.drop(), speed);
        }
        
        if (this.score > this.highScore) {
            this.highScore = this.score;
            localStorage.setItem('tetrisHighScore', this.highScore.toString());
        }
        
        this.updateUI();
    }
    
    updateGhostPosition() {
        this.ghostY = this.currentY;
        while (this.isValidPosition(this.currentPiece.shape, this.currentX, this.ghostY + 1)) {
            this.ghostY++;
        }
    }
    
    drop() {
        if (this.isPaused || this.isGameOver) return;
        
        if (this.isValidPosition(this.currentPiece.shape, this.currentX, this.currentY + 1)) {
            this.currentY++;
        } else {
            this.lockPiece();
        }
    }
    
    startGameLoop() {
        this.dropTimer = setInterval(() => this.drop(), this.INITIAL_DELAY);
        this.gameLoop();
    }
    
    gameLoop() {
        this.draw();
        requestAnimationFrame(() => this.gameLoop());
    }
    
    draw() {
        // Clear canvas
        this.ctx.fillStyle = '#111';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
        
        // Draw grid
        this.ctx.strokeStyle = '#333';
        this.ctx.lineWidth = 0.5;
        for (let i = 0; i <= this.BOARD_WIDTH; i++) {
            this.ctx.beginPath();
            this.ctx.moveTo(i * this.CELL_SIZE, 0);
            this.ctx.lineTo(i * this.CELL_SIZE, this.canvas.height);
            this.ctx.stroke();
        }
        for (let i = 0; i <= this.BOARD_HEIGHT; i++) {
            this.ctx.beginPath();
            this.ctx.moveTo(0, i * this.CELL_SIZE);
            this.ctx.lineTo(this.canvas.width, i * this.CELL_SIZE);
            this.ctx.stroke();
        }
        
        // Draw board
        for (let row = 0; row < this.BOARD_HEIGHT; row++) {
            for (let col = 0; col < this.BOARD_WIDTH; col++) {
                if (this.board[row][col]) {
                    const isAnimating = this.linesToClear.includes(row) && this.animationStep % 2 === 0;
                    if (!isAnimating) {
                        this.drawCell(col, row, this.board[row][col]);
                    }
                }
            }
        }
        
        // Draw ghost piece
        if (this.currentPiece && !this.isPaused && !this.isGameOver) {
            this.updateGhostPosition();
            this.ctx.globalAlpha = 0.3;
            for (let row = 0; row < this.currentPiece.shape.length; row++) {
                for (let col = 0; col < this.currentPiece.shape[row].length; col++) {
                    if (this.currentPiece.shape[row][col]) {
                        this.drawCell(
                            this.currentX + col,
                            this.ghostY + row,
                            this.currentPiece.color
                        );
                    }
                }
            }
            this.ctx.globalAlpha = 1;
        }
        
        // Draw current piece
        if (this.currentPiece && !this.isPaused && !this.isGameOver) {
            for (let row = 0; row < this.currentPiece.shape.length; row++) {
                for (let col = 0; col < this.currentPiece.shape[row].length; col++) {
                    if (this.currentPiece.shape[row][col]) {
                        this.drawCell(
                            this.currentX + col,
                            this.currentY + row,
                            this.currentPiece.color
                        );
                    }
                }
            }
        }
    }
    
    drawCell(x, y, color) {
        const pixelX = x * this.CELL_SIZE;
        const pixelY = y * this.CELL_SIZE;
        
        // Main color
        this.ctx.fillStyle = color;
        this.ctx.fillRect(pixelX + 1, pixelY + 1, this.CELL_SIZE - 2, this.CELL_SIZE - 2);
        
        // Highlight
        this.ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
        this.ctx.fillRect(pixelX + 1, pixelY + 1, this.CELL_SIZE - 2, 2);
        this.ctx.fillRect(pixelX + 1, pixelY + 1, 2, this.CELL_SIZE - 2);
        
        // Shadow
        this.ctx.fillStyle = 'rgba(0, 0, 0, 0.3)';
        this.ctx.fillRect(pixelX + 1, pixelY + this.CELL_SIZE - 3, this.CELL_SIZE - 2, 2);
        this.ctx.fillRect(pixelX + this.CELL_SIZE - 3, pixelY + 1, 2, this.CELL_SIZE - 2);
    }
    
    drawNext() {
        this.nextCtx.fillStyle = '#333';
        this.nextCtx.fillRect(0, 0, this.nextCanvas.width, this.nextCanvas.height);
        
        if (this.nextPiece) {
            const cellSize = 20;
            const offsetX = (this.nextCanvas.width - this.nextPiece.shape[0].length * cellSize) / 2;
            const offsetY = (this.nextCanvas.height - this.nextPiece.shape.length * cellSize) / 2;
            
            for (let row = 0; row < this.nextPiece.shape.length; row++) {
                for (let col = 0; col < this.nextPiece.shape[row].length; col++) {
                    if (this.nextPiece.shape[row][col]) {
                        this.nextCtx.fillStyle = this.nextPiece.color;
                        this.nextCtx.fillRect(
                            offsetX + col * cellSize + 1,
                            offsetY + row * cellSize + 1,
                            cellSize - 2,
                            cellSize - 2
                        );
                    }
                }
            }
        }
    }
    
    drawHold() {
        this.holdCtx.fillStyle = '#333';
        this.holdCtx.fillRect(0, 0, this.holdCanvas.width, this.holdCanvas.height);
        
        if (this.heldPiece) {
            const cellSize = 20;
            const offsetX = (this.holdCanvas.width - this.heldPiece.shape[0].length * cellSize) / 2;
            const offsetY = (this.holdCanvas.height - this.heldPiece.shape.length * cellSize) / 2;
            
            this.holdCtx.globalAlpha = this.canHold ? 1 : 0.5;
            
            for (let row = 0; row < this.heldPiece.shape.length; row++) {
                for (let col = 0; col < this.heldPiece.shape[row].length; col++) {
                    if (this.heldPiece.shape[row][col]) {
                        this.holdCtx.fillStyle = this.heldPiece.color;
                        this.holdCtx.fillRect(
                            offsetX + col * cellSize + 1,
                            offsetY + row * cellSize + 1,
                            cellSize - 2,
                            cellSize - 2
                        );
                    }
                }
            }
            
            this.holdCtx.globalAlpha = 1;
        }
    }
    
    updateUI() {
        document.querySelector('#score .value').textContent = this.score;
        document.querySelector('#level .value').textContent = this.level;
        document.querySelector('#lines .value').textContent = this.lines;
        document.querySelector('#highScore .value').textContent = this.highScore;
    }
    
    togglePause() {
        if (this.isGameOver) return;
        
        this.isPaused = !this.isPaused;
        document.getElementById('pauseOverlay').style.display = this.isPaused ? 'block' : 'none';
    }
    
    gameOver() {
        this.isGameOver = true;
        clearInterval(this.dropTimer);
        document.getElementById('gameOverOverlay').style.display = 'block';
    }
    
    restart() {
        this.isGameOver = false;
        this.isPaused = false;
        this.score = 0;
        this.level = 1;
        this.lines = 0;
        this.currentPiece = null;
        this.nextPiece = null;
        this.heldPiece = null;
        this.canHold = true;
        
        document.getElementById('gameOverOverlay').style.display = 'none';
        document.getElementById('pauseOverlay').style.display = 'none';
        
        this.clearBoard();
        this.updateUI();
        this.spawnPiece();
        this.startGameLoop();
    }
}

// ゲームの開始
const game = new TetrisGame();