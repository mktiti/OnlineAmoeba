 
/*******************************************
 *  Created On : Sat Oct 26 2019
 *  File : game.js
 *******************************************/

"use strict";

const HOST = window.location.host || ("localhost" + (window.location.port || 80));

const UNIT = 50;  // size of the unit rectangle
const DELTA = 6; // fps ( 16 is roughly 60fps )
const SPEED = 6; // speed of the movement
const PROP = 0.2; // proportion of the shape frame 

const OFFSET = UNIT * PROP;

// global variable to store if mouse is inside screen
let is_inside = false;
let is_ingame = false;

const REST_URL = `http://${HOST}/api/matches`; 
const WS_URL = `ws://${HOST}/game`;
const STAT_URL = `http://${HOST}/api/matches/stats`;

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
    ERROR:  'error',
    PING: 'pong'
};


/**
 * Enumeration that stores the possible signs.
 */
const ClientMessage = {
    PUT: 'put',
    FULLSCAN: 'full-scan',
    PARTSCAN: 'part-scan',
    PING: 'ping'
};


/**
 * Enumeration that stores the used colors.
 */
const Colors = {
    LIGHTGREEN: '#99ff99',
    LIGHTRED: '#ffcccb',
    LIGHTGREY: '#d3d3d3',
    DARKGREY: '#777',
    BLACK: '#000000'
}


/**
 * Draws a circle around the provided coordinates. 
 */
const drawCircle = (left, top, ctx) => {
    ctx.beginPath();
    ctx.lineWidth = 5;
    ctx.arc(
        left + (UNIT / 2),
        top + (UNIT / 2),
        (UNIT - OFFSET) * 0.8 / 2,  0, 2 * Math.PI);
    ctx.stroke();
};


/**
 * Draws a cross in the box given by the coordinates.
 */
const drawCross = (left, top, ctx) => {
    ctx.beginPath();
    ctx.lineWidth = 5;
    ctx.moveTo(left + OFFSET, top + OFFSET);
    ctx.lineTo(left + UNIT - OFFSET, top + UNIT - OFFSET);
    
    ctx.moveTo(left + OFFSET, top + UNIT - OFFSET);
    ctx.lineTo(left + UNIT - OFFSET, top + OFFSET);
    ctx.stroke();
};


/**
 * Draws the sign at the provided location.
 */
const drawSign = (left, top, ctx, sign) => {
    switch (sign) {
        case Sign.CROSS:
            drawCross(left, top, ctx);
            break;

        case Sign.CIRCLE:
            drawCircle(left, top, ctx);
            break;
    }
};


/**
 * Draws a number on the provided location.
 */
const drawNumber = (left, top, num, ctx) => {
    ctx.beginPath();
    ctx.font = '20px Comic Sans MS';
    ctx.fillStyle = 'black';
    ctx.textAlign = 'center';
    ctx.fillText(num, left, top); 
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
        (mouse.x + offset.x) / UNIT);
    const y_mouse = Math.floor(
        (mouse.y + offset.y) / UNIT);
  
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
const draw = (
        pos, ctx, canvas, field, 
        selected, finished, game) => {
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
            const left = (i * UNIT) - offset.x;

            ctx.lineWidth = 1;

            const coord = {
                x: i - x_shift,
                y: -(j - y_shift)
            };
             
            // if the game is finished then the finished
            // argument is a map that stores the location
            // of the winning tile sequence 
            const c = finished.get(toString(coord));

            if (c && c.x == coord.x && c.y == coord.y) {
                ctx.beginPath();
                ctx.fillStyle = Colors.LIGHTGREEN;
                ctx.fillRect(left, top, UNIT, UNIT);
                ctx.stroke();
            } 

            // drawing the highlighted cell
            if (coord.x == selected.x && 
                    coord.y == selected.y) {
                ctx.beginPath();

                if (!is_ingame)
                    ctx.fillStyle = Colors.LIGHTGREY;
                else if (game.is_player_turn)
                    ctx.fillStyle = Colors.LIGHTGREEN;
                else
                    ctx.fillStyle = Colors.LIGHTRED;

                ctx.fillRect(left, top, UNIT, UNIT);
                ctx.stroke();
                
                ctx.strokeStyle = Colors.DARKGREY;
                drawSign(left, top, ctx, game.sign);
                ctx.strokeStyle = Colors.BLACK;
            } 

            ctx.lineWidth = 1; 
            
            ctx.beginPath();
            ctx.rect(left, top, UNIT, UNIT);
            ctx.stroke();

            drawSign(
                left, top, ctx, field.get(toString(coord)));
        }
    }

    // drawing the frame with the row and column numbers

    ctx.beginPath();
    ctx.clearRect(0, 0, canvas.width, UNIT);
    ctx.clearRect(0, 0, UNIT, canvas.height);
    ctx.stroke();
    
    // drawing the column numbers along horizontal axis
    for (let j = 0; j <= size.h + 1; j++) {
        let num_top = (j * UNIT) + 
            (UNIT / 1.5) - offset.y;
        let num_left = (UNIT / 2);
        let num = -(j - y_shift);
        
        drawNumber(num_left, num_top, num, ctx);
    } 
    
    // drawing the row numbers along vertical axis
    for (let i = 0; i <= size.w + 1; i++) {
        let num_top = (UNIT / 2);
        let num_left = (i * UNIT) + 
            (UNIT / 2) - offset.x;
        let num = i - x_shift;
        
        drawNumber(num_left, num_top, num, ctx);
    }

    ctx.beginPath();
    // clearing the upper left UNIT square where numbers
    // row and column numbers would overlap
    ctx.clearRect(0, 0, UNIT, UNIT);

    // clearing the left side line to give a framed
    // look to the canvas
    ctx.clearRect(canvas.width - UNIT, 0, 
        canvas.width, canvas.height);

    // clearing a small line from the buttom for the same
    // reason
    ctx.clearRect(0, canvas.height - UNIT / 8, 
        canvas.width, canvas.height);
    ctx.stroke();
};


/**
 * Creates the update callback function.
 */
const createUpdate = (
        pos, mouse, canvas, field, 
        selected, finished, game) => {

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
        let is_active = is_ingame && is_inside;

        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight - 100;

        if (is_active && isMovingRight())
            pos.x -= SPEED;

        else if (is_active && isMovingLeft())
            pos.x += SPEED; 

        if (is_active && isMovingTop())
            pos.y += SPEED;

        else if (is_active && isMovingBottom())
            pos.y -= SPEED;
        
        draw(pos, ctx, canvas, field, selected, finished, game);
        
        setTimeout(() => {
            window.requestAnimationFrame(update);
        }, DELTA);
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
 * Handles the result of the host's join and invite
 * code generation.
 */
const handleHostResponseResults = (result, game) => {
    const invite_label = document.getElementById('invite-label');
    invite_label.value = result['inviteCode'];
    game.id = result['id'];

    const host_label = document.getElementById('host-label');
    host_label.value = `${result['hostJoinCode']}:${result['id']}`;
};


/**
 * Handles the result join game request. 
 */
const handleJoinResponseResults = (result, game) => {
    const join_text = document.getElementById('join-text');
    join_text.value = `${result['clientJoinCode']}:${result['id']}`;
    game.id = result['id'];
};


/**
 * Fetches the join code from the join-text field.
 */
const getJoinCodeAndGameId = (game) => {
    const join_text = document.getElementById('join-text');
    const results = join_text.value.split(':');
    game.id = results[1];
    return results[0];
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
    const tile_slider = document.getElementById('tile-slider');
    return tile_slider.value;
};


window.addEventListener('load', () => {
    const canvas = document.getElementById('canvas');

    const field = new Map();
    const finished = new Map();

    const resetField = () => {
        field.clear();
        finished.clear();
    }; 

    let mouse = {x: 0, y: 0}; // location of mouse in pixel
    let pos = {x : 0, y: 0}; // location of the screen 
    let selected = {x: 0, y: 0}; // targeted cell

    let game = {
        id: undefined, 
        is_player_turn: false, 
        sign: undefined,
        socket: undefined
    }; 

    $('#tile-slider').slider({
        ticks: [3, 4, 5, 6, 7]
    });
    
    // preventing the dropdown menu from closing on click
    $(document).on(
           'click', '.dropdown-menu', (e) => {
        e.stopPropagation();
    });
   
    const finish_modal = document.getElementById('finish-modal');
    const stats_modal = document.getElementById('stats-modal');        
    
    // when the user clicks anywhere outside of 
    // the modal, close it 
    window.addEventListener('click', (e) => {
        if (e.target == finish_modal) {
            finish_modal.style.display = 'none';
        } else if (e.target == stats_modal) {
            stats_modal.style.display = 'none';
        }
    });

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
        
        startGame(result['hostJoinCode']); 
    };

    // setting up listeners for the join window
    // generating join code

    const generate_button = document.getElementById('gen-btn');
    generate_button.onclick = async () => {
        const invite_code = getInviteCode();

        const response = await fetch(
            `${REST_URL}?invite=${invite_code}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        handleJoinResponseResults(result, game);
    };

    const stat_button = document.getElementById('stat-btn');
    stat_button.onclick = async (e) => {
        stats_modal.style.display = 'block';
       
        const response = await fetch(
            STAT_URL, { 
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        });

        const result = await response.json();
        
        // plotting win lose ratio
        const pie_data = [{
            values: [result['xwins'], result['owins']],
            labels: ['X', 'O'],
            type: 'pie'
        }];

        const pie_layout = {title : 'Win / Lose ratio'};
        Plotly.plot('pie_div', pie_data, pie_layout);

        // plotting average run round distribution
        const keys = Object.keys(
            result['averageRoundsByTilesToWin']);
        
        const vals = keys.map((k) => {
            return result['averageRoundsByTilesToWin'][k];
        });

        const bar_data = [{
            x: keys,
            y: vals,
            type: 'bar',
            text: keys.map((k) => `${k}`) 
        }];
        
        let dist_layout = {title : 'Average rounds by tiles'};
        Plotly.plot('dist_div', bar_data, dist_layout);
                
    };

    // sets up websocket and its event listeners by
    // providing the `join_code` 
    const startGame = (join_code) => {
        is_ingame = true;

        game.socket = new WebSocket(
            `${WS_URL}/${game.id}/${join_code}`);

        game.socket.addEventListener('message', (e) => {
            // return if no data is returned
            if (e.data === undefined)
                return;
            
            const message = JSON.parse(e.data);

            switch (message['type']) {
                case ServerMessage.INFO:
                    handleInfo(message);
                    break;

                case ServerMessage.FULLSCAN:
                    handleFullscan(message);
                    break;

                case ServerMessage.PARTSCAN:
                    handlePartscan(message);
                    break;

                case ServerMessage.NEWPOINT:
                    handleNewpoint(message);
                    break;

                case ServerMessage.EVENT:
                    handleEvent(message);
                    break;

                case ServerMessage.GAMERESULT:
                    handleGameresult(message);
                    break;

                case ServerMessage.ERROR:
                    handleError(message);
                    break;

                case ServerMessage.PING:
                    break;
            };
        });

        game.socket.addEventListener('open', (e) => {
            sendFullscan();
            
            setInterval(sendPing, 15000);
        });
    }
    
    /**
     * Handles the initial info message and sets the
     * next player and sign for the game state.
     */    
    const handleInfo = (m) => {
        game.sign = m['sign']
        game.is_player_turn = game.sign == m['waitingFor'];
    };
    
    /**
     * Resets the field and populates it with elements
     * fetched from the server.
     */  
    const handleFullscan = (m) => {
        resetField();
        
        m['xs'].forEach((c) => {
            field.set(toString(c), Sign.CROSS);                   
        });

        m['os'].forEach((c) => {
            field.set(toString(c), Sign.CIRCLE);
        });
    };
    
    /**
     * Partscan request is not implemented.
     */  
    const handlePartscan = (m) => {
        // infore part scan message    
    };
    
    /**
     * Adds the new point to the field and sets the
     * next player in the game state.
     */  
    const handleNewpoint = (m) => {
        game.is_player_turn = game.sign != m['sign'];
        field.set(toString(m['position']), m['sign']);
    };
      
    /**
     * Event messages are irrelevant for the client.
     */  
    const handleEvent = (m) => {
        // ignore event message 
    };
    
    /**
     * Sets the finished map with the winning sequence
     * and opens the modal displaying the winner.
     */  
    const handleGameresult = (m) => {
        m['row'].forEach((c) => {
            finished.set(toString(c), c);
        });

        // creating a modal popup with winner info
        finish_modal.style.display = 'block';
        const modal_text = document.getElementById(
            'winner-label');
        modal_text.innerHTML = 
            `<h1><b>PLAYER ${m['sign']} WON!</b></h1>`;

        is_ingame = false;
    };
    
    /**
     * Errors are logged to the console for debugging.
     */  
    const handleError = (m) => {
        console.log(m);
    };
    
    /**
     * Sends a new point to the server.
     */  
    const sendNewpoint = (pos) => {
        const message = {
            'type': ClientMessage.PUT,
            'position': pos 
        };
        game.socket.send(JSON.stringify(message));
    };
      
    /**
     * Sends fullscan request to server.
     */  
    const sendFullscan = () => {
        const message = {
            'type': ClientMessage.FULLSCAN
        };
        game.socket.send(JSON.stringify(message));
    };
      
    /**
     * Sends a ping message to the server for keepalive. 
     */  
    const sendPing = () => {
        const message = {'type': ClientMessage.PING};
        game.socket.send(JSON.stringify(message));
    }

    const sendPartscan = () => {
        // partscan is not implemented
    };

    // setting up listeners for the join game button and
    // starting websocket communication with the server

    const join_button = document.getElementById('join-btn');
    join_button.onclick = () => {
        const join_code = getJoinCodeAndGameId(game);
        startGame(join_code);
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
            sendNewpoint(coord);
        }
    };
    
    canvas.onmouseout = () => {
        is_inside = false; 
    };

    canvas.onmouseenter = () => {
        is_inside = true; 
    };
    
    const update = createUpdate(
        pos, mouse, canvas, field, selected, finished, game);
    window.requestAnimationFrame(update);
});

