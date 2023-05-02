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

def generate_grid(width: int):
    preamble(width*width)

    sys_commands = []
    env_commands = []
    for state in range(0, width*width):
        command = []

        if state == 0: continue # 0 is our target state

        command.append("[] s="+str(state)+" -> ")
        if (state % 2) == 0: # environment
            
            if state % width == 0:
                command.append("1.0:(s'="+str(state-width)+");")
            elif state < width:
                command.append("1.0:(s'="+str(state-1)+");")
            else:
                chance = round(Decimal(random.uniform(0, 1)), 3)
                command.append(
                    str(chance)+":(s'="+str(state-1)+")" + 
                    " + " +
                    str(1-chance)+":(s'="+str(state-width)+");"
                )
            
            env_commands.append("".join(command))
        
        else: # system
            cost_l = random.randint(0,5)
            cost_r = random.randint(0,5)

            if state % width == 0:
                command.append(str(cost_l)+":(s'="+str(state-width)+");")
            elif state < width:
                command.append(str(cost_l)+":(s'="+str(state-1)+");")
            else:
                command.append(
                    str(cost_l)+":(s'="+str(state-1)+")" + 
                    " + " +
                    str(cost_r)+":(s'="+str(state-width)+");"
                )

            sys_commands.append("".join(command))

    body(sys_commands, env_commands)

# print("=====================================================")
# generate_grid(2)
# print("=====================================================")
# generate_grid(4)
# print("=====================================================")
# generate_grid(6)
# print("=====================================================")
# generate_grid(8)
# print("=====================================================")
# generate_grid(10)
generate_grid(9)