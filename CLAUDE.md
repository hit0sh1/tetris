# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based Tetris game implementation with two versions:
- `Tetris.java` - Basic implementation with core gameplay
- `EnhancedTetris.java` - Feature-rich version with advanced gameplay elements

## Build and Run Commands

### Basic Tetris
```bash
javac Tetris.java
java Tetris
```

### Enhanced Tetris
```bash
javac EnhancedTetris.java
java EnhancedTetris
```

## Architecture Overview

### Core Components

1. **Game Board**: 10x20 grid represented as a one-dimensional array of `Tetrominoes` enum values
2. **Tetromino System**: 
   - `Tetrominoes` enum defines 7 piece types plus NoShape
   - `Tetromino` class handles piece rotation and coordinate management using a coordinate table system
   - Rotation is implemented by coordinate transformation (90-degree rotations)

3. **Game Loop**: 
   - Timer-based with configurable delay (speed increases with level in Enhanced version)
   - Handles piece falling, collision detection, line clearing, and game over conditions

4. **Key Features in Enhanced Version**:
   - Level progression system (speed increases every 10 lines)
   - Hold functionality with swap limitation
   - Ghost piece preview showing drop location
   - Next piece preview panel
   - Score persistence via `tetris_highscore.txt`
   - Line clear animation using a separate timer
   - Multiple scoring tiers with Tetris bonus

### Input Handling

- Uses KeyAdapter for keyboard input
- Supports both arrow keys and WASD controls
- Special keys: Space (hard drop), Shift/C (hold), P/ESC (pause)

### Rendering

- Custom painting using Graphics2D
- Grid overlay for visual clarity
- Color-coded pieces with 3D shading effects
- Separate panels for game info and piece previews