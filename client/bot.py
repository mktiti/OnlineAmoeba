"""
@author:    Patrik Purgai
@copyright: Copyright 2019, OnlineAmoeba 
@license:   MIT
@email:     purgai.patrik@gmail.com
@date:      2019.06.25.
"""

import json
import asyncio
import random
import websockets
import argparse
import enum
import copy
import itertools
import functools


def parse_bot_args():
    """
    Parses the command line arguments for the bot.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument(
        '--join_code',
        type=str,
        required=True,
        help='Invite code to the match.')
    parser.add_argument(
        '--url',
        type=str,
        default='ws://online-amoeba.herokuapp.com/game/{game_id}/{join_code}',
        help='URL for the websocket.')
    parser.add_argument(
        '--agent',
        type=str,
        default='weighted',
        help='Type of the agent to use.')

    return parser.parse_args()


class ServerMessage(enum.Enum):
    """
    Enumeration for the server responses.
    """
    INFO = 'info'
    FULLSCAN = 'full-scan'
    NEWPOINT = 'new-point'
    GAMERESULT = 'game-result'


class ClientMessage(enum.Enum):
    """
    Enumeration for the client messages.
    """
    FULLSCAN = 'full-scan'
    PUT = 'put'


def create_agent(args):
    """
    Factory method for creating an agent.
    """
    agents = {
        a.__name__.lower()[:-5]: a for a in 
        Agent.__subclasses__()
    }

    return agents[args.agent](**vars(args))


class Agent:
    """
    Base class for the amoeba AIs.
    """

    def __init__(self, **kwargs):
        self.sign = None
        self.waitingFor = None
        self.moves = {}

    def is_agents_turn(self):
        return self.sign != self.waitingFor

    def observe(self, sign, action):
        self.moves[sign].update({
            to_string(action): action
        })
        self.waitingFor = 'X' if sign == 'O' else 'O'
         
    def act(self):
       raise NotImplementedError('Abstract method.') 


def to_string(pos):
    """
    Convenience function to convert a position
    to string.
    """
    return f'{pos["x"]}:{pos["y"]}'


def generate_neighbours(pos):
    """
    Generates neighbours of a given position.
    """
    for x in range(pos['x'] - 1, pos['x'] + 2):
        for y in range(pos['y'] - 1, pos['y'] + 2):
            if x == pos['x'] and y == pos['y']:
                continue
            yield {'x': x, 'y': y}


def find_nearby_legal_moves(moves):
    """
    Returns a set of moves, which are near the
    existing moves.
    """
    found = {}
 
    def generate_all_neighbours(m):
        """
        Generates neighbours while gradualy filling
        the found dict.
        """
        for move in m.values():
            for n in generate_neighbours(move):
                s = to_string(n)
                if s not in moves['X'] \
                        and s not in moves['O'] \
                        and s not in found:
                    yield s, n
    
    for key, neighbour in itertools.chain(
            generate_all_neighbours(moves['X']),
            generate_all_neighbours(moves['O'])):
        found[key] = neighbour 

    return found
            
    
class RandomAgent(Agent):
    """
    Agent that selects the next move randomly.
    """

    def act(self):
        moves = find_nearby_legal_moves(self.moves)
        selected = random.choice(moves.values())

        return selected 


def check_direction(direction, pos, moves):
    """
    Steps along the provided directions and
    returns the size of the longest sequence.
    """
    def add(c1, c2):
        return {
            'x': c1['x'] + c2['x'], 
            'y': c1['y'] + c2['y']
        }

    def generate_seq():
        """
        Generates a sequence of moves from pos
        in `direction`.
        """
        curr = add(pos, direction)
        while to_string(curr) in moves:
            yield 1
            curr = add(curr, direction)

    return sum(generate_seq())


def find_longest_sequence(pos, moves):
    """
    Finds the longest the sequence.
    """
    check_dir = functools.partial(
        check_direction, pos=pos, moves=moves)

    def neg(c):
        return {'x': -c['x'], 'y': -c['y']}

    directions = [
        {'x': 0, 'y': 1}, # vert
        {'x': 1, 'y': 0}, # hor
        {'x': 1, 'y': 1}, # ne - sw
        {'x': 1, 'y': -1} # nw - se
    ]

    longest = max(
        check_dir(d) + check_dir(neg(d))
        for d in directions)

    return longest


class WeightedAgent(Agent):
    """
    Agent that selects the next move by weighting
    the candidates.
    """

    def act(self):
        moves = find_nearby_legal_moves(self.moves)
        pos_list = list(moves.values()) 

        if len(pos_list) == 0:
            return {'x': 0, 'y': 0}
        
        opponent = 'X' if self.sign == 'O' else 'O'

        def find_best(p):
            """
            Looks at the best opponent and player
            moves and chooses the best.
            """
            agent_best = find_longest_sequence(
                p, self.moves[self.sign])
            opp_best = find_longest_sequence(
                p, self.moves[opponent])

            return max(agent_best, opp_best)
        
        selected = max(pos_list, key=find_best)
        
        return selected 


async def send_fullscan(websocket):
    """
    Sends fullscan message to the server.
    """
    message = json.dumps({
        'type': ClientMessage.FULLSCAN.value
    })

    await websocket.send(message)


async def send_newpoint(pos, websocket):
    """
    Sends a new point to the server.
    """
    message = json.dumps({
        'type': ClientMessage.PUT.value,
        'position': pos 
    })

    await websocket.send(message)


async def handle_info(
        sign, waitingFor, agent, websocket, **kwargs):
    """
    Handles the initial info message by setting up
    the state of the bot.
    """
    agent.sign = sign
    agent.waitingFor = waitingFor

    await send_fullscan(websocket) 

    return False 


async def handle_fullscan(
        xs, os, agent, websocket, **kwargs):
    """
    Handles the initial fullscan message.
    """
    agent.moves['X'] = {to_string(x):x for x in xs}
    agent.moves['O'] = {to_string(o):o for o in os}

    if agent.is_agents_turn:
        pos = agent.act()
        await send_newpoint(pos, websocket)

    return False


async def handle_newpoint(
        sign, position, agent, websocket, **kwargs):
    agent.observe(sign, position)

    if agent.is_agents_turn:
        pos = agent.act()
        await send_newpoint(pos, websocket)

    return False 


async def handle_gameresult(**kwargs):
    return True 


async def handle_default(*args, **kwargs):
    return False


HANDLERS = {
    ServerMessage.INFO.value: handle_info,
    ServerMessage.FULLSCAN.value: handle_fullscan,
    ServerMessage.NEWPOINT.value: handle_newpoint,
    ServerMessage.GAMERESULT.value: handle_gameresult,
}


async def handle_message(message, agent, websocket):
    """
    Parses the received message.
    """
    message = json.loads(message)

    handler_fn = HANDLERS.get(
        message.pop('type'), handle_default)

    is_finished = await handler_fn(
        **message, agent=agent, websocket=websocket)
    
    return is_finished


async def play(url, agent):
    """
    Main loop for the bot.
    """
    async with websockets.connect(url) as websocket:
        while True:
            message = await websocket.recv()
            is_finished = await handle_message(
                message, agent, websocket)

            if is_finished:
                break


def main():
    args = parse_bot_args()
    
    join_code, game_id = args.join_code.split(':')

    url = args.url.format(
        game_id=game_id, 
        join_code=join_code)

    agent = create_agent(args)

    asyncio.get_event_loop().run_until_complete(
       play(url, agent))

if __name__ == '__main__':
    main()

