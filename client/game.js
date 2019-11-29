 
/*******************************************
 *  Created On : Sat Oct 26 2019
 *  File : game.js
 *******************************************/

"use strict";


const UNIT = 50;  // size of the unit rectangle
const DELTA = 10; // fps ( 16 is roughly 60fps )
const SPEED = 6; // speed of the movement
const PROP = 0.2; // proportion of the shape frame 
const PORT = '8080'

const OFFSET = UNIT * PROP;

// global variable to store if mouse is inside screen
let is_inside = true;

const REST_URL = `http://localhost:${PORT}/api/matches`; 
const WS_URL = `ws://localhost:${PORT}/game`;


/**
 * Enumeration that stores the possible signs.
 */
const Sign = {
    CROSS: 'X',
    CIRCLE: 'O'
};


/**
 * Enumeration that stores the possible signs.
 */
const ServerMessage = {
    INFO: 'info',
    FULLSCAN: 'full-scan',
    PARTSCAN: 'part-scan',
    NEWPOINT: 'new-point',
    EVENT:  'event',
    GAMERESULT: 'game-result',
    ERROR:  'error'
};


/**
 * Enumeration that stores the possible signs.
 */
const ClientMessage = {
    PUT: 'put',
    FULLSCAN: 'full-scan',
    PARTSCAN: 'part-scan'
};


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
            
            switch (field.get(toString(coord))) {
                case Sign.CROSS:
                    draw_cross(left, top, ctx);
                    break;

                case Sign.CIRCLE:
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
    const isMovingRight = () => {
        return (mouse.x > canvas.width * 0.85);
    };

    const isMovingLeft = () => {
        return (mouse.x < canvas.width * 0.15);
    };

    const isMovingTop = () => {
        return (mouse.y < canvas.height * 0.15);
    };

    const isMovingBottom = () => {
        return (mouse.y > canvas.height * 0.85);
    };

    const update = () => {
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight - 100;

        const curr_time = new Date().getTime();
        const dt = curr_time - prev_time; 
        prev_time = curr_time;
            
        time += dt;
        // updating at every 16th milisecond is
        // roughly equal to 60 frames / second
        if (time > DELTA) {
            time = 0;

            if (isMovingRight() && is_inside)
                pos.x -= SPEED;

            else if (isMovingLeft() && is_inside)
                pos.x += SPEED; 

            if (isMovingTop() && is_inside)
                pos.y += SPEED;

            else if (isMovingBottom() && is_inside)
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
const toString = (coordinates) => {
    return `${coordinates.x}:${coordinates.y}`;
};


/**
 * Sets the label of the range slider.
 */
const setRangeLabel = () => {
    const label = document.getElementById('range-label');
    const range = document.getElementById('label-setter');

    label.textContent = range.value;
};


/**
 * Handles the result of the host's join and invite
 * code generation.
 */
const handleHostResponseResults = (result, game) => {
    const invite_label = document.getElementById('invite-label');
    invite_label.value = result['inviteCode']
    game.id = result['id'];
};


/**
 * Handles the result join game request. 
 */
const handleJoinResponseResults = (result, game) => {
    const join_text = document.getElementById('join-text');
    join_text.value = result['clientJoinCode'];
    console.log(result);
    game.id = result['id'];
};


/**
 * Fetches the join code from the join-text field.
 */
const getJoinCode = () => {
    const join_text = document.getElementById('join-text');
    return join_text.value;
};


/**
 * Fetches the invite code from the gen-text field.
 */
const getInviteCode = () => {
    const generate_text = document.getElementById('gen-text');
    return generate_text.value;
};


/**
 * Fetches the number of tiles to win from the range label. 
 */
const getTilesToWin = () => {
    const range_label = document.getElementById('range-label');
    return range_label.textContent;
};


window.addEventListener('load', () => {
    const canvas = document.getElementById('canvas');
    
    // preventing the dropdown menu from closing on click
    $(document).on(
           'click', '.dropdown-menu', (e) => {
        e.stopPropagation();
    });


    let mouse = {x: 0, y: 0}; // location of mouse in pixel
    let pos = {x : 0, y: 0}; // location of the screen 
    let selected = {x: 0, y: 0}; // targeted cell

    let game = {
        id: undefined, 
        is_player_turn: false, 
        sign: undefined,
        socket: undefined
    };

    // setting up listeners for the host window
    // quering the game id and the invite codes

    const host_button = document.getElementById('host-btn');
    host_button.onclick = async () => {
        const n_tiles = getTilesToWin();

        const response = await fetch(REST_URL, {
            method: 'POST',
            body: JSON.stringify({'tilesToWin': n_tiles}),
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        handleHostResponseResults(result, game);
        
        setup_websocket(result['hostJoinCode']); 
    };

    // setting up listeners for the join window
    // generating join code

    const generate_button = document.getElementById('gen-btn');
    generate_button.onclick = async () => {
        const invite_code = getInviteCode();

        const response = await fetch(
            `${REST_URL}/${invite_code}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        handleJoinResponseResults(result, game);
    };

    // sets up websocket and its event listeners by
    // providing the `join_code` 
    const setup_websocket = (join_code) => {
        game.socket = new WebSocket(
            `${WS_URL}/${game.id}/${join_code}`);

        game.socket.addEventListener('message', (e) => {
            // return if no data is returned
            if (e.data === undefined)
                return;
            
            const message = JSON.parse(e.data);

            switch (message['type']) {
                case ServerMessage.INFO:
                    handle_info(message);
                    break;

                case ServerMessage.FULLSCAN:
                    handle_fullscan(message);
                    break;

                case ServerMessage.PARTSCAN:
                    handle_partscan(message);
                    break;

                case ServerMessage.NEWPOINT:
                    handle_newpoint(message);
                    break;

                case ServerMessage.EVENT:
                    handle_event(message);
                    break;

                case ServerMessage.GAMERESULT:
                    handle_gameresult(message);
                    break;

                case ServerMessage.ERROR:
                    handle_error(message);
                    break;
            };
        });
    }
    
    // implementing callback function for the websocket
    // server side communication

    const handle_info = (m) => {
        game.sign = m['sign']
        game.is_player_turn = game.sign === m['waitingFor'];
    };

    const handle_fullscan = (m) => {
        
    };

    const handle_partscan = (m) => {
        
    };

    const handle_newpoint = (m) => {
        console.log(m);
        game.is_player_turn = game.sign !== m['sign'];
        field.set(toString(m['position']), m['sign']);
    };

    const handle_event = (m) => {
        console.log(m);
    };

    const handle_gameresult = (m) => {
        console.log(m);
    };

    const handle_error = (m) => {
        console.log(m);
    };

    // implementing the client side message functions
    
    const send_newpoint = (pos) => {
        const message = {
            'type': ClientMessage.PUT,
            'position': pos 
        };
        game.socket.send(JSON.stringify(message));
    };

    const send_fullscan = () => {
        const message = {
            'type': ClientMessage.FULLSCAN
        };
        game.socket.send(JSON.stringify());
    };

    const send_partscan = () => {

    };

    // setting up listeners for the join game button and
    // starting websocket communication with the server

    const join_button = document.getElementById('join-btn');
    join_button.onclick = () => {
        const join_code = getJoinCode();
        setup_websocket(join_code);
    };
    
    // setting up client movement controls and listeners

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
        if (game.is_player_turn) {
            send_newpoint(coord);
        }
    };
    
    canvas.onmouseout = () => {
        is_inside = false; 
    };

    canvas.onmouseenter = () => {
        is_inside = true; 
    };
  
    const field = new Map();

    const update = createUpdate(
        pos, mouse, canvas, field, selected);
    window.requestAnimationFrame(update);
});

