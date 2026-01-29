execute at @s run particle minecraft:explosion ~ ~ ~ 0 0 0 0.01 1
execute at @s run particle minecraft:flame ~ ~ ~ 0.5 0.5 0.5 0.02 20
execute at @s run playsound minecraft:entity.generic.explode player @a[distance=..24] ~ ~ ~ 0.8 1.1

execute at @s as @e[type=!player,distance=..3] run damage @s 8 minecraft:fireball
execute at @s as @e[type=!player,distance=..3] run data merge entity @s {Fire:80s}

kill @e[type=item_display,tag=meteor_visual,distance=..2]
kill @s
