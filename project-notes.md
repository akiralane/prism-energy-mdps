# temp notes

Either:
- it's two-player, with one controller and one environment
  - this means that they'll have to be declared at the start and 
    be described in separate modules. something like 
            
        player controller m1 endplayer
        env environment m2 endenv
    and then the "env" module will be interpreted as using probabilities 
    while the player module will be interpreted as using energy

- it's one-player
  - which will mean I'll probably have to denote the difference 
    between probability and energy _inside_ each action. something like
  ```
      [] s=0 -!> 5:(s'=1) // "transition to s'=1 using 5 energy"
  ```

neither approach seems trivial, but I think the second one might 
be easier?
