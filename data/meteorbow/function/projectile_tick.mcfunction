scoreboard players add @s meteorbow.proj_life 1
execute if score @s meteorbow.proj_life matches 60.. run kill @s

execute unless entity @s run return 0

function meteorbow:util/particles_trail

execute unless block ^ ^ ^0.6 air run function meteorbow:projectile_hit
execute if entity @e[type=#minecraft:living,distance=..1.4] run function meteorbow:projectile_hit

tp @s ^ ^ ^0.9
