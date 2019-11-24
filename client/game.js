 
/*******************************************
 *  Created On : Sat Oct 26 2019
 *  File : game.js
 *******************************************/

"use strict";


const UNIT = 40;  // size of the unit rectangle
const DELTA = 10; // fps ( 16 is roughly 60fps )
const SPEED = 6; // speed of the movement
const PROP = 0.2; // proportion of the shape frame 

const OFFSET = UNIT * PROP;

// global variable to store if mouse is inside screen
let is_inside = true;


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
 * Draws the field based on the provided `location`
 * (screen middle) coordinates.
 */
const draw = (location, ctx, canvas, field) => {
    const n_hor = Math.ceil(canvas.width / UNIT);
    const n_ver = Math.ceil(canvas.height / UNIT);
    
    // these values store the cell which is
    // currently targeted by the camera 
    const x = location.x > 0 ?
        Math.floor(location.x / UNIT) :
        Math.ceil(location.x / UNIT);

    const y = location.y > 0 ?
        Math.floor(location.y / UNIT) :
        Math.ceil(location.y / UNIT);
          
    // offsets are the value by which the grid is shifted
    // in a direction for continuous movement 
    const x_offset = UNIT - location.x % UNIT;
    const y_offset = UNIT - location.y % UNIT;

    ctx.beginPath();
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.stroke();

    // offset to place the 0, 0 point in the middle of
    // the screen 
    const x_middle = Math.floor(n_hor / 2) + x + 1 
    const y_middle = Math.floor(n_ver / 2) + y + 1

    for (let i = 0; i <= n_hor + 1; i++) {
        for (let j = 0; j <= n_ver + 1; j++) {
            const top = (j * UNIT) - y_offset; 
            const left = (i  * UNIT) - x_offset;
           
            ctx.beginPath();
            ctx.rect(left, top, UNIT, UNIT);
            ctx.stroke();

            const coord = [i - x_middle , -j + y_middle];

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
const createUpdate = (mouse, ctx, canvas, field) => {
    let prev_time = new Date().getTime();
    let time = 0;
    
    // *_move variables determine the bounding box
    // in which the mouse will cause the screen
    // to move in that direction
    const is_moving_right = () => {
        return (mouse.x > canvas.width * 0.8);
    };

    const is_moving_left = () => {
        return (mouse.x < canvas.width * 0.2);
    };

    const is_moving_top = () => {
        return (mouse.y < canvas.height * 0.2);
    };

    const is_moving_bottom = () => {
        return (mouse.y > canvas.height * 0.8);
    };

    let location = {x: 0, y: 0};

    const update = () => {
        canvas.width = window.innerWidth - 30;
        canvas.height = window.innerHeight - 50;

        const curr_time = new Date().getTime();
        const dt = curr_time - prev_time; 
        prev_time = curr_time;
            
        time += dt;
        // updating at every 16th milisecond is
        // roughly equal to 60 frames / second
        if (time > DELTA) {
            time = 0;

            if (is_moving_right() && is_inside)
                location.x -= SPEED;

            else if (is_moving_left() && is_inside)
                location.x += SPEED; 

            if (is_moving_top() && is_inside)
                location.y += SPEED;

            else if (is_moving_bottom() && is_inside)
                location.y -= SPEED;
           
            draw(location, ctx, canvas, field);
        }

        window.requestAnimationFrame(update);
    };

    return update;
};


const as_string = (coordinates) => {
    return `${coordinates[0]}:${coordinates[1]}`;
};


window.addEventListener('load', () => {
    const canvas = document.getElementById('canvas');
    const ctx = canvas.getContext('2d');
    const socket = new WebSocket();

    let mouse = {x: 0, y: 0};

    canvas.onmousemove = (e) => {
        let rect = canvas.getBoundingClientRect();
        mouse.x = e.clientX - rect.left;
        mouse.y = e.clientY - rect.top;
    };
    
    canvas.onmouseout = () => {
        is_inside = false; 
    };

    canvas.onmouseenter = () => {
        is_inside = true; 
    };
  
    const field = new Map([
        [as_string([0, 0]), 'X'],
        [as_string([1, 2]), 'O'],
        [as_string([0, 4]), 'O']
    ]);

    const update = createUpdate(mouse, ctx, canvas, field);
    window.requestAnimationFrame(update);
});

