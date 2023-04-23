import random
from decimal import *

def preamble(num_states: int):
    print("emdp")
    print()
    print("global s : [0.."+str(num_states-1)+"] init "+str(num_states-1)+";")
    print()
    print("player s sys endplayer")
    print("environment e env endenvironment")
    print()

def generate_commands_line(length: int):

    sys_commands = []
    env_commands = []
    for state in range(0, length):
        command = []
        command.append("[] s="+str(state)+" -> ")

        if state == 0: continue

        if (state % 2) == 0: #environment
            chance = round(Decimal(random.uniform(0, 1)), 3)
            command.append(
                str(chance)+":(s'="+str(state-1)+")" + 
                " + " +
                str(1-chance)+":(s'="+str(min(state+2, length-1))+");"
            )
            env_commands.append("".join(command))
        else:
            cost_l = random.randint(0,5)
            cost_r = random.randint(0,5)

            command.append(
                str(cost_l)+":(s'="+str(state-1)+")" + 
                " + " +
                str(cost_r)+":(s'="+str(min(state+2, length-1))+");"
            )
            sys_commands.append("".join(command))

    return sys_commands, env_commands


def body(sys_commands, env_commands):
    print("module sys")
    for command in sys_commands:
        print("    "+command)
    print("endmodule")
    print()
    print("module env")
    for command in env_commands:
        print("    "+command)
    print("endmodule")
    print()

def generate_line(length: int):
    preamble(length)
    sys, env = generate_commands_line(length)
    body(sys, env)

generate_line(4)
print("=========================")
generate_line(16)
print("=========================")
generate_line(32)
print("=========================")
generate_line(64)
print("=========================")
generate_line(100)
