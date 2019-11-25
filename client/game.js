 
/*******************************************
 *  Created On : Sat Oct 26 2019
 *  File : game.js
 *******************************************/

"use strict";


const UNIT = 50;  // size of the unit rectangle
const DELTA = 10; // fps ( 16 is roughly 60fps )
const SPEED = 6; // speed of the movement
const PROP = 0.2; // proportion of the shape frame 

const OFFSET = UNIT * PROP;

// global variable to store if mouse is inside screen
let is_inside = true;

const REST_URL = 'localhost:port/api/matches'; 
const WS_URL = 'ws://localhost:port/game/{id}/{joinCode}'

// {"type": "put", "position": {"x": 10, "y": 5}} 
// 
//
//
//
//
//


/**
 * Draws a circle around the provided coordinates. 
 */
const draw_circle = (left, top, ctx) => {
    ctx.beginPath();
    ctx.arc(
        left + (UNIT / 2),
        top + (UNIT / 2),
        (UNIT - OFFSET) * 0.8 / 2,  0, 2 * Math.PI);
    ctx.stroke();
};


/**
 * Draws a cross in the box given by the coordinates.
 */
const draw_cross = (left, top, ctx) => {
    ctx.beginPath();
    ctx.moveTo(left + OFFSET, top + OFFSET);
    ctx.lineTo(left + UNIT - OFFSET, top + UNIT - OFFSET);
    
    ctx.moveTo(left + OFFSET, top + UNIT - OFFSET);
    ctx.lineTo(left + UNIT - OFFSET, top + OFFSET);
    ctx.stroke();
};

/**
 * Computes the offset of the field.
 */
const computeOffset = (pos) => {
    return {
        x: UNIT - pos.x % UNIT,
        y: UNIT - pos.y % UNIT
    };
};


/**
 * Computes the the coordinate of targeted cell.
 */
const computeAbsoluteCoord = (pos) => {
    // these values store the cell which is
    // currently targeted by the camera 
    const x = pos.x > 0 ?
        Math.floor(pos.x / UNIT) :
        Math.ceil(pos.x / UNIT);

    const y = pos.y > 0 ?
        Math.floor(pos.y / UNIT) :
        Math.ceil(pos.y / UNIT); 
    
    return {x: x, y: y};
};


/**
 * Computes the number of cell horizontaly and verticaly.
 */
const computeSize = (canvas) => {
    return {
        w: Math.ceil(canvas.width / UNIT),
        h: Math.ceil(canvas.height / UNIT)
    };
};


/**
 * Computes the coordinates of the currently targeted
 * cell by the mouse.
 */
const computeRelativeCoord = (mouse, pos, canvas) => {
    const offset = computeOffset(pos);
    const size = computeSize(canvas);
    const abs = computeAbsoluteCoord(pos);

    // shifting back the location of the mouse by the
    // offset of the cells 
    const x_mouse = Math.floor(
        (mouse.x + offset.x - (UNIT * 0.25)) / UNIT);
    const y_mouse = Math.floor(
        (mouse.y + offset.y - (UNIT * 0.1)) / UNIT);
  
    // shifting the coordinate to the middle
    const x_shift = Math.floor(size.w / 2) + abs.x + 1;
    const y_shift = Math.floor(size.h / 2) + abs.y + 1;
    return {
        x: x_mouse - x_shift, 
        y: -(y_mouse - y_shift)
    };
};
    

/**
 * Draws the field based on the provided `location`
 * (screen middle) coordinates.
 */
const draw = (pos, ctx, canvas, field, selected) => {
    const size = computeSize(canvas);

    // offsets are the value by which the grid is shifted
    // in a direction for continuous movement 
    const offset = computeOffset(pos);
    const abs = computeAbsoluteCoord(pos);

    ctx.beginPath();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.stroke();

    // offset to place the 0, 0 point in the middle of
    // the screen 
    const x_shift = Math.floor(size.w / 2) + abs.x + 1;
    const y_shift = Math.floor(size.h / 2) + abs.y + 1;
    
    for (let i = 0; i <= size.w + 1; i++) {
        for (let j = 0; j <= size.h + 1; j++) {
            const top = (j * UNIT) - offset.y;
            const left = (i  * UNIT) - offset.x;
            
          const coord = {x: i - x_shift ,y: -j + y_shift};
            
            // drawing the highlighted cell
            if (coord.x == selected.x && 
                    coord.y == selected.y) {
                ctx.beginPath();
                ctx.fillStyle = '#d3d3d3';
                ctx.fillRect(left, top, UNIT, UNIT);
                ctx.stroke();
            } 
            
            ctx.beginPath();
            ctx.rect(left, top, UNIT, UNIT);
            ctx.stroke();
            
            switch (field.get(as_string(coord))) {
                case 'X':
                    draw_cross(left, top, ctx);
                    break;

                case 'O':
                    draw_circle(left, top, ctx);
                    break;
            }
        }
    }
};


/**
 * Creates the update callback function.
 */
const createUpdate = (
        pos, mouse, canvas, field, selected) => {
    let prev_time = new Date().getTime();
    let time = 0;

    const ctx = canvas.getContext('2d');
    
    // *_move variables determine the bounding box
    // in which the mouse will cause the screen
    // to move in that direction
    const is_moving_right = () => {
        return (mouse.x > canvas.width * 0.85);
    };

    const is_moving_left = () => {
        return (mouse.x < canvas.width * 0.15);
    };

    const is_moving_top = () => {
        return (mouse.y < canvas.height * 0.15);
    };

    const is_moving_bottom = () => {
        return (mouse.y > canvas.height * 0.85);
    };

    const update = () => {
        canvas.width = window.innerWidth - 30;
        canvas.height = window.innerHeight - 60;

        const curr_time = new Date().getTime();
        const dt = curr_time - prev_time; 
        prev_time = curr_time;
            
        time += dt;
        // updating at every 16th milisecond is
        // roughly equal to 60 frames / second
        if (time > DELTA) {
            time = 0;

            if (is_moving_right() && is_inside)
                pos.x -= SPEED;

            else if (is_moving_left() && is_inside)
                pos.x += SPEED; 

            if (is_moving_top() && is_inside)
                pos.y += SPEED;

            else if (is_moving_bottom() && is_inside)
                pos.y -= SPEED;
           
            draw(pos, ctx, canvas, field, selected);
        }

        window.requestAnimationFrame(update);
    };

    return update;
};


/**
 * Creates the update callback function.
 */
const as_string = (coordinates) => {
    return `${coordinates.x}:${coordinates.y}`;
};


window.addEventListener('load', () => {
    const canvas = document.getElementById('canvas');
    const host_btn_open = document.getElementById(
        'host-btn-open');
    
    host_btn_open.onclick = () => {
        document.getElementById('host-form')
            .style.display = 'block';
    };
    
    const host_btn_cancel = document.getElementById(
        'host-btn-cancel');

    host_btn_cancel.onclick = () => {
        document.getElementById('host-form')
            .style.display = 'none';
    }
  
    const join_btn_cancel = document.getElementById(
        'host-btn-cancel');

    let mouse = {x: 0, y: 0}; // location of mouse in pixel
    let pos = {x : 0, y: 0}; // location of the screen 
    let selected = {x: 0, y: 0}; // targeted cell

    canvas.onmousemove = (e) => {
        let rect = canvas.getBoundingClientRect();
        mouse.x = e.clientX - rect.left;
        mouse.y = e.clientY - rect.top;
        let coord = computeRelativeCoord(mouse, pos, canvas);
        selected.x = coord.x;
        selected.y = coord.y;
    };

    canvas.onclick = (e) => {
        const coord = computeRelativeCoord(mouse, pos, canvas);
        field.set(as_string(coord), 'X');
    };
    
    canvas.onmouseout = () => {
        is_inside = false; 
    };

    canvas.onmouseenter = () => {
        is_inside = true; 
    };
  
    const field = new Map([
        [as_string({x: 0, y: 0}), 'X'],
        [as_string({x: 1, y: 2}), 'O'],
        [as_string({x: 0, y: 4}), 'O']
    ]);

    const update = createUpdate(
        pos, mouse, canvas, field, selected);
    window.requestAnimationFrame(update);
});

