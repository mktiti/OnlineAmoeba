/******************************************
 *  Author : Patrik Purgai   
 *  Created On : Sat Oct 26 2019
 *  File : game.js
 *******************************************/

"use strict";


const WIDTH = 31;
const HEIGHT = 31;


/**
 * Possible states of a cell.
 * @enum {string}
 */
const CellState = {
    CROSS: 'X', 
    CIRCLE: 'O', 
    EMPTY: 'EMPTY'
};


/**
 * Possible messages from the server.
 * @enum {string}
 */
const MessageType = {
    FULLSCAN: 'fullscan', 
    NEWPOINT: 'newpoint'
};


/**
 * Stores an `x` and `y` NumberPair.
 */
class NumberPair {

    /**
     * @param {number} x
     * @param {number} y
     */
    constructor(x, y) {
        /** @public @const {number} */
        this.x = x;

        /** @public @const {number} */
        this.y = y;
    }

    /**
     * Computes whether the NumberPair is inside
     * the given rectangle.
     * @param {!NumberPair} top_left
     * @param {!NumberPair} dims
     */
    isInside(top_left, dims) {
        return (
            top_left.x <= this.x &&
            top_left.x - dims.x >= this.x &&
            top_left.y >= this.y &&
            top_left.y - dims.y <= this.y
        )
    }
}


class QuadTree {

}


/**
 * Stores the player's move.
 */
class Move {

    /**
     * @param {!NumberPair} coord Location of the move.
     * @param {!CellState} state State of the move.
     */
    constructor(coord, player) {
        /** @public @type {!NumberPair} */
        this.coord = coord;

        /** @public @type {!CellState} */
        this.player = player;
    }
}


/**
 * Stores the state of a cell.
 */
class Cell {

    /**
     * @param {!NumberPair} coord Coordinates of the cell.
     * @param {!NumberPair} dims Dimensions of the cell.
     */
    constructor(coord, dims) {
        /** @public @type {!NumberPair} */
        this.coord = coord;

        /** @public @type {!NumberPair} */
        this.dims = dims;

        /** @public @type {!CellState} */
        this.state = CellState.EMPTY;
    }

    /**
     * @param {HTMLElement} context
     * @param {number} row_idx Row index of the cell.
     * @param {number} col_idx Column index of the cell.
     */
    draw(context, row_idx, col_idx) {
        const top_left = new NumberPair(
            this.dims.x * col_idx,
            this.dims.y * row_idx
        );

        switch (this.state) {
            case CellState.EMPTY:
                draw_empty(context, top_left, this.dims);
                break;

            case CellState.CIRCLE:
                draw_cricle(context, top_left, this.dims);
                break;

            case CellState.CROSS:
                draw_cross(context, top_left, this.dims);
                break;
        }
    }
}


function draw_empty(context, top_left, dims) {
    context.lineWidth = '1';
    context.strokeStyle = 'black';
    context.rect(top_left.x, top_left.y, dims.x, dims.y);
}


function draw_cross(context, top_left, dims) {
    context.lineWidth = '1';
    context.strokeStyle = 'red';
    context.fillRect(top_left.x, top_left.y, dims.x, dims.y);
}


function draw_cricle(context, top_left, dims) {
    context.lineWidth = '1';
    context.strokeStyle = 'blue';
    context.fillRect(top_left.x, top_left.y, dims.x, dims.y);
}


/**
 * Represents the visible board.
 */
class Board {

    /**
     * @param {!number} rows Number of cells in a row.
     * @param {!number} cols Number of cells in a column.
     * @param {!HTMLElement} canvas
     */
    constructor(rows, cols, canvas) {
        /** @public @type {!number} */
        this.rows = rows;

        /** @public @type {!number} */
        this.cols = cols;

        /** @public @type {Array<Array<Cell>>} */
        this.cells = undefined;

        /** @public @const {HTMLElement}*/
        this.canvas = canvas;

        /** @public @const {object}*/
        this.context = canvas.getContext('2d');

        /** @public @type {NumberPair}*/
        this.cell_dims = new NumberPair(
            this.canvas.width / this.cols,
            this.canvas.height / this.rows
        );

        this.init(new NumberPair((rows - 1) / 2, (cols - 1) / 2));
    }

    /**
     * Returns the board to initial state.
     * @param {!NumberPair} offset The offset of the board.
     */
    init(offset) {
        this.cells = [];

        for (let x = offset.x; x > offset.x - this.cols; x--) {
            let row = new Array();

            for (let y = offset.y; y > offset.y - this.rows; y--)
                row.push(new Cell(new NumberPair(x, y), this.cell_dims));

            this.cells.push(row);
        }
    }

    /**
     * @param {!NumberPair} offset Upper left coord of the board.
     * @param {!Array<Move>} moves Array of the player moves.
     */
    update(offset, moves) {
        this.init(offset);

        let bottom_right = new NumberPair(
            offset.x - this.cols,
            offset.y - this.rows
        );

        moves.forEach((move) => {
            const x = move.x - offset.x;
            const y = move.y - offset.y;

            if (coord.isInsideBoundary(offset))
                this.cells[x][y].state = move.state;
        });
    }

    /**
     * Draws the whole board.
     */
    draw() {
        this.context.beginPath();

        this.cells.forEach((rows, row_idx) => {
            rows.forEach((cell, col_idx) => {
                cell.draw(this.context, row_idx, col_idx);
            });
        });

        this.context.stroke();
    }

    /**
     * Updates and draws a single cell.
     * @param {NumberPair} coord Coordinate of the cell.
     * @param {CellState} state New state of the cell.
     */
    updateCell(coord, state) {
        let cell = this.cells[coord.x][coord.y];
        cell.state = state;

        this.context.beginPath();
        cell.draw(this.canvas, this.context);
        this.context.stroke();
    }

    /**
     * Converts click coordinates to board coordinates.
     * @param {NumberPair} click Coordinate of the click.
     * @return {NumberPair} Coordinate of the cell on board.
     */
    convertClick(click) {
        return new NumberPair(
            Math.floor(click.x / this.cell_dims.x),
            Math.floor(click.y / this.cell_dims.y)
        );
    }
}


class Game {

    /**
     * @param {string} address Address of the ws service.
     */
    constructor(address) {
        /** @public @const {HTMLElement}*/
        this.canvas = document.getElementById('canvas');

        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;

        /** @public @const {WebSocket}*/
        // this.socket = new WebSocket(address);
        
        /** @public @const {Board}*/
        this.board = new Board(HEIGHT, WIDTH, this.canvas);
        this.board.draw();

        /** @public @type {Player}*/
        this.player = undefined;

        /** @public @type {Player}*/
        this.opponent = undefined;

        /** @public @type {boolean}*/
        this.is_active_turn = false;
    }

    /** 
     * @return {!Iterator<Move>} 
     */
    * generateMoves() {
        for (const move of this.player.moves)
            yield move;

        for (const move of this.opponent.moves)
            yield move;
    }

    /**
     * @param {object} players_data
     */
    initializePlayers(players_data) {
        this.player = new Player()
    }

    registerCanvasEventHandlers() {
        this.canvas.onmousedown = (event) => {
            const click = new NumberPair(
                event.clientX, 
                event.clientY
            );

            console.log(this.board.convertClick(click));
        };

        this.canvas.onmousemove = (event) => {

        };

        this.canvas.onmouseout = (event) => {

        };

        this.canvas.onmouseup = (event) => {

        };
    }

    registerSocketEventHandlers() {
        this.socket.onmessage = (message) => {
            let json_message = JSON.parse(message);
        
            switch (json_message['type']) {
                case MessageType.FULLSCAN:
                    break;

                case MessageType.NEWPOINT:
                    break;

                default:
                    break;
            }
        };
    }
}


window.addEventListener('load', function() {
    const game = new Game();

    game.registerCanvasEventHandlers();
    game.registerSocketEventHandlers();
});
