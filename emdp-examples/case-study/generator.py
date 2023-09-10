from decimal import *
from io import TextIOWrapper
from sys import argv

def preamble(num_states: int, out: TextIOWrapper):
    out.write("""emdp
          
global pow : [0..3] init 0;
global com : [0..{n}] init 0; // target state {n}

player client c endplayer
environment server s endenvironment

const int cost_lo = 2;
const int cost_hi = 5;
const int charge_rate = 1;

const double p_poweroff_failure = 0.2;
const double p_timeout_lo = 0.3;
const double p_timeout_hi = 0.1;
const double p_poweroff_success = 1 - p_poweroff_failure;
const double p_msg_lo = 1 - p_timeout_lo;
const double p_msg_hi = 1 - p_timeout_hi;
""".format(n = num_states - 1))

def body(commands: (list, list), out: TextIOWrapper):
    (sys_commands, env_commands) = commands

    out.write("""
module c
    // power consumption state
    [] pow=0 & mod(com, 2)=0 -> 0:(pow'=1) + -charge_rate:(pow'=2);
    [] pow=1 & mod(com, 2)=0 -> 0:(pow'=0) + -charge_rate:(pow'=2);

    // communication state\n""")
    for i, command in enumerate(sys_commands):
        spacer = "" if i % 2 == 0 else "\n"
        out.write("    {command}\n{spacer}".format(command=command, spacer=spacer))
    out.write("endmodule\n")

    out.write("""
module s
    // power consumption state
    [] pow=2 -> p_poweroff_success:(pow'=0) + p_poweroff_failure:(pow'=3);
    
    // communication state\n""")
    for i, command in enumerate(env_commands):
        spacer = "" if i % 2 == 0 else "\n"
        out.write("    {command}\n{spacer}".format(command=command, spacer=spacer))
    out.write("endmodule\n")

def make_commands(num_states: int):
    sys_commands = []
    env_commands = []
    for n in range(0, num_states):
        if (n % 2) == 0: # client
            sys_commands.append("[] com={n} & pow=0 -> cost_lo:(com'={n_next});".format(n=n, n_next=n+1))
            sys_commands.append("[] com={n} & pow=1 -> cost_hi:(com'={n_next});".format(n=n, n_next=n+1))
        else: # server
            env_commands.append("[] com={n} & pow=0 -> p_timeout_lo:(com'=0) + p_msg_lo:(com'={n_next});".format(n=n, n_next=n+1))
            env_commands.append("[] com={n} & pow=1 -> p_timeout_hi:(com'=0) + p_msg_hi:(com'={n_next});".format(n=n, n_next=n+1))

    return (sys_commands, env_commands)

def render_model(num_states: int):
    # model
    out = open("out.emdp", "w")
    preamble(num_states, out)
    body(make_commands(num_states), out)
    out.close()

    # props
    out = open("out.props", "w")
    out.write("P|E=0.0 [com={target} & (pow=0 | pow=1)]".format(target=num_states-1))
    out.close()

render_model(int(argv[1]))
