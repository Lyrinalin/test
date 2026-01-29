execute as @e[type=arrow,tag=meteor_carrier,tag=!meteor_init] at @s run summon item_display ~ ~ ~ {Tags:["meteor_visual","meteor_temp"],item:{id:"minecraft:magma_block",Count:1b},transformation:{scale:[0.6f,0.6f,0.6f]},brightness:{sky:15,block:15}}
execute as @e[type=arrow,tag=meteor_carrier,tag=!meteor_init] at @s run ride @e[type=item_display,tag=meteor_temp,limit=1,sort=nearest,distance=..0.6] mount @s
execute as @e[type=arrow,tag=meteor_carrier,tag=!meteor_init] run tag @s add meteor_init
execute as @e[type=item_display,tag=meteor_temp] run tag @s remove meteor_temp

execute as @e[type=arrow,tag=meteor_carrier] at @s run particle minecraft:flame ~ ~ ~ 0 0 0 0 2
execute as @e[type=arrow,tag=meteor_carrier] at @s run particle minecraft:smoke ~ ~ ~ 0 0 0 0.02 1

execute as @e[type=arrow,tag=meteor_carrier,nbt={inGround:1b}] run function firebow:meteor_hit
